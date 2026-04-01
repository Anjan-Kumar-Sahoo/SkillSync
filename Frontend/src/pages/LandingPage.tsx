import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import './LandingPage.css';

const uiDocs = [
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

const monitoringLinks = [
  { name: 'API Gateway', href: 'http://localhost:8080' },
  { name: 'Eureka', href: 'http://localhost:8761' },
  { name: 'RabbitMQ', href: 'http://localhost:15672' },
  { name: 'Prometheus', href: 'http://localhost:9090' },
  { name: 'Grafana', href: 'http://localhost:3000' },
  { name: 'Loki', href: 'http://localhost:3100' },
  { name: 'Zipkin', href: 'http://localhost:9411' },
  { name: 'Nginx Entry', href: 'http://localhost:80' },
];

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
          <p className="hero-kicker">Presentation Edition</p>
          <h1>One click entry to architecture docs and live monitoring surfaces.</h1>
          <p>
            This page is for presentation purpose only. It gives quick navigation to your
            platform walkthrough and operational dashboards.
          </p>
          <div className="hero-cta-row">
            <Link className="landing-btn landing-btn-solid" to="/dashboard">
              Start SkillSync App
            </Link>
            <a
              className="landing-btn landing-btn-ghost"
              href="https://github.com/Anjan-Kumar-Sahoo/SkillSync"
              target="_blank"
              rel="noreferrer"
            >
              View Source
            </a>
          </div>
        </section>

        <section className="section-wrap">
          <div className="section-head">
            <h2>UI Documentation</h2>
            <p>Open each visual document directly in a new tab.</p>
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
                <span>Open document</span>
              </a>
            ))}
          </div>
        </section>

        <section className="section-wrap">
          <div className="section-head">
            <h2>Monitoring Quick Access</h2>
            <p>Jump to every operational endpoint from one place.</p>
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
                <span>{item.name}</span>
                <small>{item.href}</small>
              </a>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
};

export default LandingPage;