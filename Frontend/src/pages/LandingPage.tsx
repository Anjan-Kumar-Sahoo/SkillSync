import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import './LandingPage.css';

const DEFAULT_BACKEND_BASE_URL = 'https://api.skillsync.mraks.dev';
const DEFAULT_SWAGGER_PATH = '/swagger-ui/index.html';
const DEFAULT_EUREKA_PATH = '/eureka-ui/';
const DEFAULT_SONAR_URL = 'https://sonarcloud.io/organizations/skillsync-github/projects';

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
  const backendBaseUrl = import.meta.env.VITE_BACKEND_BASE_URL || DEFAULT_BACKEND_BASE_URL;
  const monitoringBaseUrl = import.meta.env.VITE_MONITORING_BASE_URL || backendBaseUrl;
  const swaggerUrl = import.meta.env.VITE_SWAGGER_URL;
  const eurekaUrl = import.meta.env.VITE_EUREKA_URL;
  const sonarUrl = import.meta.env.VITE_SONAR_URL || DEFAULT_SONAR_URL;

  try {
    const backendParsed = new URL(backendBaseUrl);
    const monitoringParsed = new URL(monitoringBaseUrl);

    const backendProtocol = backendParsed.protocol;
    const backendHost = backendParsed.hostname;
    const backendOrigin = backendParsed.port
      ? `${backendProtocol}//${backendHost}:${backendParsed.port}`
      : `${backendProtocol}//${backendHost}`;

    const monitoringProtocol = monitoringParsed.protocol;
    const monitoringHost = monitoringParsed.hostname;
    const monitoringOrigin = monitoringParsed.port
      ? `${monitoringProtocol}//${monitoringHost}:${monitoringParsed.port}`
      : `${monitoringProtocol}//${monitoringHost}`;

    const onMonitoringPort = (port: number, suffix = '') =>
      `${monitoringProtocol}//${monitoringHost}:${port}${suffix}`;

    return [
      {
        name: 'Eureka',
        description: 'Service discovery dashboard',
        status: 'ACTIVE',
        href: eurekaUrl || `${monitoringOrigin}${DEFAULT_EUREKA_PATH}`,
      },
      {
        name: 'RabbitMQ',
        description: 'Message broker management',
        status: 'ACTIVE',
        href: onMonitoringPort(15672),
      },
      {
        name: 'Prometheus',
        description: 'Metrics collection and queries',
        status: 'ACTIVE',
        href: onMonitoringPort(9090),
      },
      {
        name: 'Grafana',
        description: 'Dashboards and alerting',
        status: 'ACTIVE',
        href: onMonitoringPort(3000),
      },
      {
        name: 'SonarCloud',
        description: 'Code quality and security analysis',
        status: 'QA',
        href: sonarUrl,
      },
      {
        name: 'Loki Ready',
        description: 'Log aggregation health endpoint',
        status: 'CHECK',
        href: onMonitoringPort(3100, '/ready'),
      },
      {
        name: 'Zipkin',
        description: 'Distributed tracing UI',
        status: 'ACTIVE',
        href: onMonitoringPort(9411),
      },
      {
        name: 'Gateway Swagger',
        description: 'Gateway API contract explorer',
        status: 'DOCS',
        href: swaggerUrl || `${backendOrigin}${DEFAULT_SWAGGER_PATH}`,
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
            <p>All production monitoring links are mapped from your direct gateway domain.</p>
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