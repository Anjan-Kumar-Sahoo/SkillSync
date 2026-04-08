import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import ThemeToggleButton from '../components/ui/ThemeToggleButton';
import './LandingPage.css';

type ValueItem = {
  title: string;
  body: string;
  stat: string;
};

type FeatureItem = {
  icon: string;
  title: string;
  description: string;
};

type PlatformMetric = {
  label: string;
  value: string;
  helper: string;
};

const values: ValueItem[] = [
  {
    title: 'Mentor Marketplace',
    body: 'Discover verified mentors across coding, cloud, system design, and interview prep with clear pricing and trust signals.',
    stat: '12+ domains',
  },
  {
    title: 'Secure Session Flow',
    body: 'Booking is payment-aware and event-driven, so mentors are notified only after successful payment with automatic failure rollback.',
    stat: 'Zero ghost bookings',
  },
  {
    title: 'Smart Feedback Loop',
    body: 'Reviews, ratings, and mentor credibility update consistently across notifications, profile cards, and dashboards.',
    stat: 'Realtime sync',
  },
];

const features: FeatureItem[] = [
  {
    icon: 'rocket_launch',
    title: 'Fast Start, No Friction',
    description: 'Inline OTP verification, OAuth onboarding, and role-aware dashboard routing get users productive in minutes.',
  },
  {
    icon: 'shield_lock',
    title: 'Production-Grade Safety',
    description: 'JWT cookie auth, guarded routes, rollback-aware session booking, and clean compensating flows across services.',
  },
  {
    icon: 'monitoring',
    title: 'Observability Built In',
    description: 'Prometheus, Grafana, Loki, Zipkin, and Eureka are integrated from day one for resilient operations.',
  },
  {
    icon: 'groups_2',
    title: 'Community + Mentoring',
    description: 'Learners can level up through 1:1 mentorship and collaborative group learning in a single platform.',
  },
  {
    icon: 'bolt',
    title: 'Event-Driven Backbone',
    description: 'Microservices communicate via robust event contracts, making the system scalable and failure-aware.',
  },
  {
    icon: 'insights',
    title: 'Clear Growth Signals',
    description: 'Track sessions, earnings, ratings, and learner progress with purpose-built dashboards per role.',
  },
];

const metrics: PlatformMetric[] = [
  { label: 'Services', value: '9', helper: 'Microservices in production topology' },
  { label: 'Core Flows', value: '25+', helper: 'Auth, booking, review, payment, notifications' },
  { label: 'Uptime Focus', value: '99.9%', helper: 'Designed with monitoring + fail-safe behavior' },
];

const finaleTags = [
  'Mentor Match Engine',
  'Realtime Session Flow',
  'Payment-First Reliability',
  'Trust-Driven Reviews',
  'Career Growth Pathways',
  'Community Learning Loops',
];

const cardClassNames = [
  'ppt-surface-card accent-cyan',
  'ppt-surface-card accent-orange',
  'ppt-surface-card accent-blue',
] as const;

const LandingPage = () => {
  const surfaceCards = useMemo(
    () =>
      values.map((item, index) => ({
        ...item,
        className: cardClassNames[index % cardClassNames.length],
      })),
    [],
  );

  return (
    <div className="ppt-page" id="top">
      <div className="ppt-grid-overlay" aria-hidden="true" />
      <div className="ppt-aura aura-one" aria-hidden="true" />
      <div className="ppt-aura aura-two" aria-hidden="true" />
      <div className="ppt-aura aura-three" aria-hidden="true" />

      <header className="ppt-nav">
        <a className="ppt-brand" href="#top" aria-label="SkillSync Presentation Home">
          <img src={logo} alt="SkillSync logo" className="ppt-logo" />
          <span>SkillSync</span>
        </a>

        <div className="ppt-nav-actions">
          <ThemeToggleButton className="ppt-theme-toggle" showLabel={false} />
          <Link className="ppt-btn solid" to="/login">
            Sign In
          </Link>
        </div>
      </header>

      <main className="ppt-main">
        <section className="ppt-hero">
          <div className="ppt-hero-copy">
            <p className="ppt-kicker">Peer To Peer Learning Platform</p>
            <h1>
              Mentorship That
              <span> Scales Trust, Not Noise.</span>
            </h1>
            <p className="ppt-subtext">
              SkillSync connects serious learners with verified mentors through a resilient, payment-safe,
              event-driven platform engineered for real growth and real outcomes.
            </p>

            <div className="ppt-cta-row">
              <Link className="ppt-btn solid" to="/dashboard">
                Explore Product
              </Link>
              <a className="ppt-btn ghost" href="#platform-story">
                Why SkillSync
              </a>
            </div>

            <div className="ppt-metrics">
              {metrics.map((metric) => (
                <article key={metric.label} className="ppt-metric-card">
                  <p>{metric.label}</p>
                  <h3>{metric.value}</h3>
                  <small>{metric.helper}</small>
                </article>
              ))}
            </div>
          </div>

          <div className="ppt-hero-stage" aria-hidden="true">
            <div className="stage-ring ring-outer" />
            <div className="stage-ring ring-mid" />
            <div className="stage-ring ring-inner" />
            <div className="stage-core">
              <img src={logo} alt="" />
            </div>
            <div className="stage-chip chip-a">Event Driven</div>
            <div className="stage-chip chip-b">Payment Safe</div>
            <div className="stage-chip chip-c">Realtime UX</div>
          </div>
        </section>

        <section className="ppt-section" id="platform-story">
          <div className="ppt-section-head">
            <p>What Makes It Different</p>
            <h2>Built Like a Product, Not Just a Demo.</h2>
          </div>

          <div className="ppt-surface-grid">
            {surfaceCards.map((item) => (
              <article key={item.title} className={item.className}>
                <div className="surface-glow" aria-hidden="true" />
                <span className="surface-stat">{item.stat}</span>
                <h3>{item.title}</h3>
                <p>{item.body}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="ppt-section">
          <div className="ppt-section-head">
            <p>Product Story</p>
            <h2>A polished learning ecosystem for learners, mentors, and teams.</h2>
          </div>

          <div className="ppt-feature-grid">
            {features.map((feature) => (
              <article key={feature.title} className="ppt-feature-card">
                <div className="feature-sheen" aria-hidden="true" />
                <span className="material-symbols-outlined">{feature.icon}</span>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="ppt-final-cta">
          <p className="final-kicker">Ready To Scale Learning?</p>
          <h2>Give every learner a premium mentorship experience.</h2>
          <p>
            SkillSync blends trust, velocity, and clarity into one polished platform where users discover mentors,
            book confidently, and improve continuously with feedback that actually matters.
          </p>
          <div className="ppt-cta-row">
            <Link className="ppt-btn solid" to="/dashboard">
              Enter Application
            </Link>
            <Link className="ppt-btn ghost" to="/register">
              Create Account
            </Link>
          </div>

          <div className="ppt-finale-ribbon" aria-label="Platform highlights">
            {finaleTags.map((tag) => (
              <span key={tag} className="ribbon-chip">
                {tag}
              </span>
            ))}
          </div>

          <div className="ppt-finale-orbit" aria-hidden="true">
            <div className="orbit-ring ring-a" />
            <div className="orbit-ring ring-b" />
            <div className="orbit-ring ring-c" />
            <div className="orbit-pulse pulse-a" />
            <div className="orbit-pulse pulse-b" />
            <div className="orbit-pulse pulse-c" />
            <div className="orbit-core">SkillSync</div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default LandingPage;
