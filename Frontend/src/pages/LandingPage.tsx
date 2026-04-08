import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import ThemeToggleButton from '../components/ui/ThemeToggleButton';
import './LandingPage.css';

type LiveStep = {
  time: string;
  title: string;
  detail: string;
  state: 'done' | 'active' | 'queued';
};

type ExperienceLane = {
  role: string;
  headline: string;
  summary: string;
  bullets: string[];
  metric: string;
  pulse: string;
  accentClass: string;
};

type PlatformMetric = {
  label: string;
  value: string;
  helper: string;
};

type ReliabilitySignal = {
  label: string;
  value: string;
  helper: string;
};

type HeroSignal = {
  label: string;
  value: string;
};

const livePipeline: LiveStep[] = [
  {
    time: '09:10',
    title: 'Learner books a session',
    detail: 'Topic, slot, and mentor are selected with timezone-safe scheduling.',
    state: 'done',
  },
  {
    time: '09:11',
    title: 'Payment verification lock',
    detail: 'Mentor notification is held until transaction success is confirmed.',
    state: 'active',
  },
  {
    time: '09:13',
    title: 'Mentor response window',
    detail: 'Accept/reject action updates learner status in real time.',
    state: 'queued',
  },
  {
    time: '10:00',
    title: 'Live session execution',
    detail: 'Join details, reminders, and completion flow stay aligned for both users.',
    state: 'queued',
  },
];

const experienceLanes: ExperienceLane[] = [
  {
    role: 'Learner Workspace',
    headline: 'Book confidently, cancel consciously.',
    summary: 'Learners can discover mentors quickly, but every cancellation is explicit and policy-aware.',
    bullets: [
      'Clear mentor profile cards with ratings and trust cues',
      'Payment-first booking to prevent ghost confirmations',
      'Cancellation confirmation with compensation disclaimer',
    ],
    metric: '3-step booking flow',
    pulse: 'High confidence onboarding',
    accentClass: 'lane-cyan',
  },
  {
    role: 'Mentor Console',
    headline: 'Only valid bookings reach the mentor.',
    summary: 'Mentors receive requests after payment success, reducing noise and protecting time.',
    bullets: [
      'No premature notifications from failed transactions',
      'Accept, reject, and complete actions from one queue',
      'Earnings, ratings, and session history stay synchronized',
    ],
    metric: 'Zero ghost bookings',
    pulse: 'Cleaner decision queue',
    accentClass: 'lane-orange',
  },
  {
    role: 'Platform Operations',
    headline: 'Runtime visibility without guesswork.',
    summary: 'Events, retries, and service-level monitoring are designed for production behavior.',
    bullets: [
      'Event contracts drive booking, payment, and notification flow',
      'Rollback paths preserve data consistency on payment failure',
      'Observability stack supports fast issue diagnosis',
    ],
    metric: 'Monitoring-first topology',
    pulse: 'Operational reliability',
    accentClass: 'lane-blue',
  },
];

const metrics: PlatformMetric[] = [
  { label: 'Services', value: '9', helper: 'Microservices under one platform graph' },
  { label: 'Critical Journeys', value: '25+', helper: 'Auth, booking, payment, review, notifications' },
  { label: 'Runtime Goal', value: '99.9%', helper: 'Monitoring and fail-safe flow design' },
];

const reliabilitySignals: ReliabilitySignal[] = [
  {
    label: 'Mentor alert after payment success',
    value: 'Strictly enforced',
    helper: 'Prevents invalid request notifications',
  },
  {
    label: 'Failed payment rollback',
    value: 'Automatic',
    helper: 'Session state stays consistent across services',
  },
  {
    label: 'Rating and review propagation',
    value: 'Realtime',
    helper: 'Dashboard and profile credibility stay in sync',
  },
];

const heroSignals: HeroSignal[] = [
  { label: 'Queue Health', value: 'Stable' },
  { label: 'Payment Guard', value: 'Enabled' },
  { label: 'Reminder Jobs', value: 'On Schedule' },
];

const finaleTags = [
  'Mentor Match Engine',
  'Realtime Session Flow',
  'Payment-First Reliability',
  'Trust-Driven Reviews',
  'Career Growth Pathways',
  'Community Learning Loops',
];

const LandingPage = () => {
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
              Built For Real Sessions,
              <span> Not Just Pretty Screens.</span>
            </h1>
            <p className="ppt-subtext">
              SkillSync maps the full mentorship lifecycle: learner request, payment verification, mentor response,
              live session delivery, and post-session trust signals, all in a single reliable flow.
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

          <div className="ppt-hero-stage" aria-label="Live session pipeline preview">
            <div className="ppt-live-console">
              <div className="live-console-head">
                <p>Live Session Pipeline</p>
                <span className="live-indicator">
                  <i aria-hidden="true" />
                  Active now
                </span>
              </div>

              <div className="live-flow">
                {livePipeline.map((step) => (
                  <article key={`${step.time}-${step.title}`} className={`live-step ${step.state}`}>
                    <span className="live-time">{step.time}</span>
                    <div className="live-step-body">
                      <h3>{step.title}</h3>
                      <p>{step.detail}</p>
                    </div>
                  </article>
                ))}
              </div>

              <p className="live-console-foot">
                State transitions remain traceable across booking, payment, notification, and completion events.
              </p>
            </div>

            <div className="ppt-live-signals" aria-hidden="true">
              <div className="signal-brand">
                <img src={logo} alt="" />
                <span>SkillSync Runtime</span>
              </div>
              {heroSignals.map((signal) => (
                <article key={signal.label} className="ppt-live-signal-card">
                  <p>{signal.label}</p>
                  <h3>{signal.value}</h3>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="ppt-section" id="platform-story">
          <div className="ppt-section-head">
            <p>Real Workflow, Real Context</p>
            <h2>Every role sees the right state, at the right moment.</h2>
          </div>

          <div className="ppt-lane-grid">
            {experienceLanes.map((lane) => (
              <article key={lane.role} className={`ppt-lane-card ${lane.accentClass}`}>
                <div className="lane-top">
                  <span className="lane-role">{lane.role}</span>
                  <span className="lane-metric">{lane.metric}</span>
                </div>
                <h3>{lane.headline}</h3>
                <p>{lane.summary}</p>
                <ul>
                  {lane.bullets.map((bullet) => (
                    <li key={bullet}>{bullet}</li>
                  ))}
                </ul>
                <div className="lane-pulse">{lane.pulse}</div>
              </article>
            ))}
          </div>
        </section>

        <section className="ppt-section">
          <div className="ppt-section-head">
            <p>Operational Reliability</p>
            <h2>Production-safe behavior is part of the UX, not an afterthought.</h2>
          </div>

          <div className="ppt-signal-grid">
            {reliabilitySignals.map((signal) => (
              <article key={signal.label} className="ppt-signal-card">
                <p>{signal.label}</p>
                <h3>{signal.value}</h3>
                <small>{signal.helper}</small>
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
