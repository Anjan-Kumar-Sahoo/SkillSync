import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import './LandingPage.css';

const DEFAULT_PROD_API_URL = 'https://skillsync.mraks.dev';

type DocLink = {
  title: string;
  description: string;
  href: string;
};

type MonitoringLink = {
  name: string;
  description: string;
  status: string;
  href: string;
};

const uiDocs: DocLink[] = [
  {
    title: 'Backend Architecture',
    description: 'Service topology, data boundaries, and domain ownership.',
    href: '/ui-docs/BE-ARCHITECTURE.html',
  },
  {
    title: 'Frontend Architecture',
    description: 'Component tree, state flow, and API integration model.',
    href: '/ui-docs/FE-ARCHITECTURE.html',
  },
  {
    title: 'Payment Saga',
    description: 'Outbox, compensation, retries, and consistency strategy.',
    href: '/ui-docs/PAYMENT_SAGA.html',
  },
  {
    title: 'Deployment',
    description: 'Container runtime, ingress, CI/CD and observability setup.',
    href: '/ui-docs/DEPLOYMENT.html',
  },
];

const resolveMonitoringLinks = (): MonitoringLink[] => {
  const apiUrl = import.meta.env.VITE_API_URL || DEFAULT_PROD_API_URL;

  try {
    const parsed = new URL(apiUrl);
    const protocol = parsed.protocol;
    const host = parsed.hostname;
    const gatewayUrl = apiUrl.replace(/\/$/, '');
    const onPort = (port: number, suffix = '') => `${protocol}//${host}:${port}${suffix}`;

    return [
      {
        name: 'API Gateway',
        description: 'Traffic routing and filtering',
        status: 'ACTIVE',
        href: gatewayUrl,
      },
      {
        name: 'Eureka',
        description: 'Service discovery dashboard',
        status: 'ACTIVE',
        href: onPort(8761),
      },
      {
        name: 'RabbitMQ',
        description: 'Message broker management',
        status: 'ACTIVE',
        href: onPort(15672),
      },
      {
        name: 'Prometheus',
        description: 'Metrics collection and queries',
        status: 'ACTIVE',
        href: onPort(9090),
      },
      {
        name: 'Grafana',
        description: 'Dashboards and alerting',
        status: 'ACTIVE',
        href: onPort(3000),
      },
      {
        name: 'Loki Ready',
        description: 'Log aggregation health endpoint',
        status: 'CHECK',
        href: onPort(3100, '/ready'),
      },
      {
        name: 'Zipkin',
        description: 'Distributed tracing UI',
        status: 'ACTIVE',
        href: onPort(9411),
      },
      {
        name: 'Nginx Entry',
        description: 'Public reverse proxy entrypoint',
        status: 'ACTIVE',
        href: `${protocol}//${host}`,
      },
      {
        name: 'Gateway Swagger',
        description: 'Gateway API contract explorer',
        status: 'DOCS',
        href: `${protocol}//${host}/swagger-ui.html`,
      },
    ];
  } catch {
    return [];
  }
};

const monitoringLinks = resolveMonitoringLinks();

const LandingPage = () => {
  return (
    <div className="landing-page">
      <div className="landing-bg landing-bg-1" />
      <div className="landing-bg landing-bg-2" />

      <header className="landing-nav">
        <a className="landing-brand" href="#top" aria-label="SkillSync Home">
          <img src={logo} alt="SkillSync logo" className="landing-logo" />
          <span>SkillSync</span>
        </a>
        <nav className="landing-actions" aria-label="Landing actions">
          <a
            className="landing-btn landing-btn-ghost"
            href="https://github.com/Anjan-Kumar-Sahoo/SkillSync"
            target="_blank"
            rel="noreferrer"
          >
            GitHub
          </a>
          <Link className="landing-btn landing-btn-solid" to="/dashboard">
            Use App
          </Link>
        </nav>
      </header>

      <main className="landing-content" id="top">
        <section className="hero-card">
          <div className="hero-aura hero-aura-one" aria-hidden="true" />
          <div className="hero-aura hero-aura-two" aria-hidden="true" />
          <div className="hero-aura hero-aura-three" aria-hidden="true" />
          <div className="brand-stage" aria-hidden="true">
            <div className="gravity-orb orb-a" />
            <div className="gravity-orb orb-b" />
            <div className="gravity-orb orb-c" />
            <img src={logo} alt="" className="hero-logo" />
          </div>
          <h2 className="hero-brand-title">SkillSync</h2>
          <p className="hero-tagline">Peer To Peer Learning Platform</p>
          <div className="hero-cta-row">
            <Link className="landing-btn landing-btn-solid" to="/dashboard">
              Get Started
            </Link>
            <a className="landing-btn landing-btn-ghost" href="#ui-docs">
              View Docs
            </a>
          </div>
        </section>

        <section className="section-wrap" id="ui-docs">
          <div className="section-head">
            <p className="section-label">Infrastructure</p>
            <h2>UI Documentation</h2>
            <p>Explore comprehensive technical blueprints that power the SkillSync ecosystem.</p>
          </div>
          <div className="docs-grid">
            {uiDocs.map((doc, index) => (
              <a
                key={doc.title}
                href={doc.href}
                className="doc-card"
                target="_blank"
                rel="noreferrer"
                style={{ animationDelay: `${index * 120}ms` }}
              >
                <h3>{doc.title}</h3>
                <p>{doc.description}</p>
                <span>View Specs</span>
              </a>
            ))}
          </div>
        </section>

        <section className="section-wrap" id="system-health">
          <div className="section-head">
            <p className="section-label">System Health</p>
            <h2>Monitoring Quick Access</h2>
            <p>All production monitoring links are mapped from your EC2 backend target.</p>
          </div>
          <div className="monitor-grid">
            {monitoringLinks.map((item, index) => (
              <a
                key={item.name}
                className="monitor-link"
                href={item.href}
                target="_blank"
                rel="noreferrer"
                style={{ animationDelay: `${index * 80}ms` }}
              >
                <div className="monitor-top">
                  <span>{item.name}</span>
                  <b>{item.status}</b>
                </div>
                <small>{item.description}</small>
              </a>
            ))}
          </div>
        </section>
      </main>
      <p className="footer-description">
        This page is for presentation purpose only. It gives quick navigation to your
        platform walkthrough and operational dashboards.
      </p>
      <footer className="landing-footer">
        <h3>SkillSync</h3>
        <p>© 2026 SkillSync. Peer To Peer Learning Platform.</p>
      </footer>
    </div>
  );
};

export default LandingPage;