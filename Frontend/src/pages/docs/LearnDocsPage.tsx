import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import DOMPurify from 'dompurify';
import { marked } from 'marked';

import { LEARN_DOCS, LEARN_DOC_CATEGORIES, type LearnDocItem } from './learnDocsManifest';
import './LearnDocsPage.css';

interface DocHeading {
  id: string;
  text: string;
  level: 1 | 2 | 3;
}

interface MammothResult {
  value: string;
}

interface MammothApi {
  convertToHtml: (input: { arrayBuffer: ArrayBuffer }) => Promise<MammothResult>;
}

marked.setOptions({
  gfm: true,
  breaks: false,
});

let mammothApiPromise: Promise<MammothApi> | null = null;

function slugify(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
}

async function getMammothApi(): Promise<MammothApi> {
  if (!mammothApiPromise) {
    mammothApiPromise = import('mammoth/mammoth.browser').then((module) => {
      const candidate = module as {
        convertToHtml?: MammothApi['convertToHtml'];
        default?: { convertToHtml?: MammothApi['convertToHtml'] };
      };

      const convertToHtml = candidate.convertToHtml ?? candidate.default?.convertToHtml;

      if (!convertToHtml) {
        throw new Error('DOCX conversion is not available in this build.');
      }

      return { convertToHtml };
    });
  }

  return mammothApiPromise;
}

function extractBodyFromHtmlDocument(rawHtml: string): string {
  const parsedDocument = new DOMParser().parseFromString(rawHtml, 'text/html');
  const bodyHtml = parsedDocument.body.innerHTML.trim();

  return bodyHtml.length > 0 ? bodyHtml : rawHtml;
}

function sanitizeAndMapHeadings(rawHtml: string): { html: string; headings: DocHeading[] } {
  const sanitizedHtml = DOMPurify.sanitize(rawHtml, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['style', 'script', 'iframe'],
    FORBID_ATTR: ['style', 'onerror', 'onclick'],
  });

  const parsedDocument = new DOMParser().parseFromString(sanitizedHtml, 'text/html');
  const headingNodes = Array.from(parsedDocument.body.querySelectorAll('h1, h2, h3'));
  const slugCounts = new Map<string, number>();
  const headings: DocHeading[] = [];

  headingNodes.forEach((headingNode, index) => {
    const headingText = headingNode.textContent?.trim() ?? `Section ${index + 1}`;
    const headingLevel = Number(headingNode.tagName.slice(1));
    const baseSlug = slugify(headingText) || `section-${index + 1}`;
    const duplicateCount = slugCounts.get(baseSlug) ?? 0;
    const uniqueSlug = duplicateCount === 0 ? baseSlug : `${baseSlug}-${duplicateCount + 1}`;

    slugCounts.set(baseSlug, duplicateCount + 1);
    headingNode.id = uniqueSlug;

    if (headingLevel >= 1 && headingLevel <= 3) {
      headings.push({
        id: uniqueSlug,
        text: headingText,
        level: headingLevel as 1 | 2 | 3,
      });
    }
  });

  parsedDocument.body.querySelectorAll('a[href]').forEach((linkNode) => {
    const href = linkNode.getAttribute('href') ?? '';

    if (/^https?:\/\//i.test(href)) {
      linkNode.setAttribute('target', '_blank');
      linkNode.setAttribute('rel', 'noreferrer noopener');
    }
  });

  return {
    html: parsedDocument.body.innerHTML,
    headings,
  };
}

async function renderDocumentContent(doc: LearnDocItem): Promise<string> {
  const fileUrl = `/project-docs/${doc.fileName}`;
  const response = await fetch(fileUrl);

  if (!response.ok) {
    throw new Error(`Unable to load ${doc.fileName}. HTTP ${response.status}.`);
  }

  if (doc.format === 'markdown') {
    const markdownSource = await response.text();
    return (await marked.parse(markdownSource)) as string;
  }

  if (doc.format === 'html') {
    const htmlSource = await response.text();
    return extractBodyFromHtmlDocument(htmlSource);
  }

  const mammothApi = await getMammothApi();
  const docxSource = await response.arrayBuffer();
  const convertedDocument = await mammothApi.convertToHtml({ arrayBuffer: docxSource });

  return convertedDocument.value;
}

function LearnDocsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [query, setQuery] = useState('');
  const [topicsOpen, setTopicsOpen] = useState(true);
  const [docHtml, setDocHtml] = useState('');
  const [docHeadings, setDocHeadings] = useState<DocHeading[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const selectedDocId = searchParams.get('doc') ?? LEARN_DOCS[0].id;

  const selectedDoc = useMemo(
    () => LEARN_DOCS.find((doc) => doc.id === selectedDocId) ?? LEARN_DOCS[0],
    [selectedDocId],
  );

  const normalizedQuery = query.trim().toLowerCase();

  const filteredDocs = useMemo(() => {
    if (!normalizedQuery) {
      return LEARN_DOCS;
    }

    return LEARN_DOCS.filter((doc) => {
      return (
        doc.title.toLowerCase().includes(normalizedQuery) ||
        doc.fileName.toLowerCase().includes(normalizedQuery) ||
        doc.category.toLowerCase().includes(normalizedQuery)
      );
    });
  }, [normalizedQuery]);

  const groupedDocs = useMemo(() => {
    return LEARN_DOC_CATEGORIES.map((category) => {
      return {
        category,
        docs: filteredDocs.filter((doc) => doc.category === category),
      };
    }).filter((section) => section.docs.length > 0);
  }, [filteredDocs]);

  useEffect(() => {
    if (searchParams.has('doc')) {
      return;
    }

    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('doc', LEARN_DOCS[0].id);
    setSearchParams(nextParams, { replace: true });
  }, [searchParams, setSearchParams]);

  useEffect(() => {
    const mobileMedia = window.matchMedia('(max-width: 980px)');

    const syncTopicsPanel = (mediaQuery: MediaQueryList | MediaQueryListEvent) => {
      setTopicsOpen(!mediaQuery.matches);
    };

    syncTopicsPanel(mobileMedia);
    mobileMedia.addEventListener('change', syncTopicsPanel);

    return () => {
      mobileMedia.removeEventListener('change', syncTopicsPanel);
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadDocument = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const renderedHtml = await renderDocumentContent(selectedDoc);
        const { html, headings } = sanitizeAndMapHeadings(renderedHtml);

        if (!cancelled) {
          setDocHtml(html);
          setDocHeadings(headings);
        }
      } catch (loadError) {
        if (!cancelled) {
          setDocHtml('');
          setDocHeadings([]);
          setError(loadError instanceof Error ? loadError.message : 'Unable to load this document right now.');
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void loadDocument();

    return () => {
      cancelled = true;
    };
  }, [selectedDoc]);

  const selectDocument = (doc: LearnDocItem) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('doc', doc.id);
    setSearchParams(nextParams, { replace: true });

    if (window.matchMedia('(max-width: 980px)').matches) {
      setTopicsOpen(false);
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const jumpToHeading = (headingId: string) => {
    const headingElement = document.getElementById(headingId);
    if (headingElement) {
      headingElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <div className="learn-page">
      <div className="learn-atmosphere" aria-hidden="true" />

      <header className="learn-header">
        <p className="learn-kicker">SkillSync Documentation Hub</p>
        <h1>Learn Every Part of the Platform from One Place</h1>
        <p>
          This page keeps your source documents unchanged and presents them with a cleaner reading experience.
          Choose any subtopic from the left sidebar and read it instantly.
        </p>
        <div className="learn-header-actions">
          <Link className="learn-chip" to="/ppt">
            Presentation View
          </Link>
          <a className="learn-chip learn-chip-muted" href={`/project-docs/${selectedDoc.fileName}`} target="_blank" rel="noreferrer noopener">
            Open Original File
          </a>
        </div>
      </header>

      <div className="learn-layout">
        <aside className="learn-sidebar" aria-label="Documentation topics">
          <button
            className="learn-mobile-toggle"
            type="button"
            onClick={() => setTopicsOpen((previous) => !previous)}
            aria-expanded={topicsOpen}
            aria-controls="learn-sidebar-content"
          >
            <span>Topics ({LEARN_DOCS.length})</span>
            <strong>{topicsOpen ? 'Hide' : 'Show'}</strong>
          </button>

          <div id="learn-sidebar-content" className={`learn-sidebar-content ${topicsOpen ? 'is-open' : ''}`}>
            <label className="learn-search-label" htmlFor="learn-doc-search">
              Find a topic
            </label>
            <input
              id="learn-doc-search"
              className="learn-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by topic, keyword, or file name"
              type="search"
            />

            <div className="learn-sidebar-scroll">
              {groupedDocs.map((section) => (
                <section key={section.category} className="learn-sidebar-section">
                  <h2>{section.category}</h2>
                  <div className="learn-topic-list">
                    {section.docs.map((doc) => {
                      const isActive = doc.id === selectedDoc.id;
                      return (
                        <button
                          key={doc.id}
                          className={`learn-topic ${isActive ? 'is-active' : ''}`}
                          onClick={() => selectDocument(doc)}
                          type="button"
                        >
                          <span>{doc.title}</span>
                          <small>{doc.format.toUpperCase()}</small>
                        </button>
                      );
                    })}
                  </div>
                </section>
              ))}
            </div>
          </div>
        </aside>

        <main className="learn-main" aria-live="polite">
          <div className="learn-doc-meta">
            <p>{selectedDoc.category}</p>
            <h2>{selectedDoc.title}</h2>
            <span>{selectedDoc.fileName}</span>
          </div>

          {isLoading && <div className="learn-state">Loading document...</div>}

          {!isLoading && error && <div className="learn-state learn-error">{error}</div>}

          {!isLoading && !error && (
            <article className="learn-article" dangerouslySetInnerHTML={{ __html: docHtml }} />
          )}
        </main>

        <aside className="learn-outline" aria-label="Current document sections">
          <h2>On This Document</h2>
          {docHeadings.length === 0 && <p>No headings found in this file.</p>}
          {docHeadings.length > 0 && (
            <div className="learn-outline-list">
              {docHeadings.map((heading) => (
                <button
                  key={heading.id}
                  className={`learn-outline-item level-${heading.level}`}
                  onClick={() => jumpToHeading(heading.id)}
                  type="button"
                >
                  {heading.text}
                </button>
              ))}
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}

export default LearnDocsPage;
