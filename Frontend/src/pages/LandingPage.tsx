import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import logo from '../assets/skillsync-logo.png';
import ThemeToggleButton from '../components/ui/ThemeToggleButton';
import './LandingPage.css';

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

const finaleTags = [
  'Mentor Match Engine',
  'Realtime Session Flow',
  'Payment-First Reliability',
  'Trust-Driven Reviews',
  'Career Growth Pathways',
  'Community Learning Loops',
];

/** Intersection Observer hook — adds `.revealed` when element scrolls into view */
const useRevealOnScroll = () => {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('revealed');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );

    // Observe the container and all stagger-children inside it
    observer.observe(el);
    el.querySelectorAll('.stagger-child').forEach((child) => observer.observe(child));

    return () => observer.disconnect();
  }, []);

  return ref;
};

const LandingPage = () => {
  const heroTextRef = useRevealOnScroll();
  const lanesRef = useRevealOnScroll();
  const signalsRef = useRevealOnScroll();
  const finaleRef = useRevealOnScroll();

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
          <Link className="ppt-btn ghost" to="/register">
            Register
          </Link>
          <Link className="ppt-btn solid" to="/login">
            Sign In
          </Link>
        </div>
      </header>

      <main className="ppt-main">
        {/* ── Hero 1: Brand showcase with logo, orbs ── */}
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
            <a className="landing-btn landing-btn-ghost" href="#platform-story">
              Why SkillSync
            </a>
          </div>
        </section>

        {/* ── Hero 2: Full-screen text headline + metrics ── */}
        <section className="ppt-hero reveal-section" ref={heroTextRef}>
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



            <div className="ppt-metrics">
              {metrics.map((metric, i) => (
                <article key={metric.label} className="ppt-metric-card stagger-child" style={{ transitionDelay: `${i * 120}ms` }}>
                  <p>{metric.label}</p>
                  <h3>{metric.value}</h3>
                  <small>{metric.helper}</small>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* ── Experience Lanes ── */}
        <section className="ppt-section reveal-section" id="platform-story" ref={lanesRef}>
          <div className="ppt-section-head">
            <p>Real Workflow, Real Context</p>
            <h2>Every role sees the right state, at the right moment.</h2>
          </div>

          <div className="ppt-lane-grid">
            {experienceLanes.map((lane, i) => (
              <article key={lane.role} className={`ppt-lane-card ${lane.accentClass} stagger-child`} style={{ transitionDelay: `${i * 150}ms` }}>
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

        {/* ── Reliability Signals ── */}
        <section className="ppt-section reveal-section" ref={signalsRef}>
          <div className="ppt-section-head">
            <p>Operational Reliability</p>
            <h2>Production-safe behavior is part of the UX, not an afterthought.</h2>
          </div>

          <div className="ppt-signal-grid">
            {reliabilitySignals.map((signal, i) => (
              <article key={signal.label} className="ppt-signal-card stagger-child" style={{ transitionDelay: `${i * 150}ms` }}>
                <p>{signal.label}</p>
                <h3>{signal.value}</h3>
                <small>{signal.helper}</small>
              </article>
            ))}
          </div>
        </section>

        {/* ── Final CTA ── */}
        <section className="ppt-final-cta reveal-section" ref={finaleRef}>
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
            <Link className="ppt-btn outline" to="/learn" style={{ border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(255,255,255,0.05)' }}>
              Watch Docs
            </Link>
          </div>

          <div className="ppt-finale-ribbon" aria-label="Platform highlights">
            {finaleTags.map((tag, i) => (
              <span key={tag} className="ribbon-chip stagger-child" style={{ transitionDelay: `${i * 80}ms` }}>
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
            <div className="orbit-core">
              <img src={logo} alt="" className="orbit-core-logo" />
              <span>SkillSync</span>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default LandingPage;
