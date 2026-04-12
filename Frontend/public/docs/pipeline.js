/**
 * SkillSync Architecture Pipeline — Scroll-Driven Animations
 * 
 * Controls:
 *  1. Spine glow line that fills as you scroll
 *  2. Section reveal on viewport intersection
 *  3. SVG flow-path drawing on scroll
 *  4. Active nav pill highlighting
 *  5. Child-element staggered reveals
 */
(function () {
  'use strict';

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ─── Spine Glow ─── */
  const spineGlow = document.querySelector('.spine-glow');

  function updateSpine() {
    if (!spineGlow) return;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    const progress = docHeight > 0 ? (window.scrollY / docHeight) * 100 : 0;
    spineGlow.style.height = progress + '%';
  }

  /* ─── Reveal Observer ─── */
  const revealEls = document.querySelectorAll('.reveal');

  function initReveals() {
    if (reduceMotion) {
      revealEls.forEach(el => el.classList.add('is-visible'));
      return;
    }

    const observer = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach(entry => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add('is-visible');
          obs.unobserve(entry.target);
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -6% 0px' }
    );

    revealEls.forEach(el => observer.observe(el));
  }

  /* ─── Flow Path Drawing ─── */
  const flowPaths = document.querySelectorAll('.draw-on-scroll');

  function initFlowPaths() {
    flowPaths.forEach(path => {
      try {
        const len = path.getTotalLength();
        path.style.strokeDasharray = len;
        path.style.strokeDashoffset = len;
      } catch (e) {
        path.style.strokeDasharray = '600';
        path.style.strokeDashoffset = '600';
      }
    });

    if (reduceMotion) {
      flowPaths.forEach(p => {
        p.style.strokeDashoffset = '0';
        p.classList.add('is-drawn');
      });
      return;
    }

    const pathObserver = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach(entry => {
          if (!entry.isIntersecting) return;
          const paths = entry.target.querySelectorAll('.draw-on-scroll');
          paths.forEach((p, i) => {
            setTimeout(() => {
              p.style.strokeDashoffset = '0';
              p.classList.add('is-drawn');
            }, i * 150);
          });
          obs.unobserve(entry.target);
        });
      },
      { threshold: 0.3 }
    );

    document.querySelectorAll('.flow-connector-section').forEach(section => {
      pathObserver.observe(section);
    });
  }

  /* ─── Active Nav Pill ─── */
  const navPills = document.querySelectorAll('.nav-pills a');
  const sections = [];

  function initNavTracking() {
    navPills.forEach(pill => {
      const href = pill.getAttribute('href');
      if (href && href.startsWith('#')) {
        const target = document.getElementById(href.slice(1));
        if (target) sections.push({ el: target, pill: pill });
      }
    });

    if (sections.length === 0) return;

    const sectionObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const match = sections.find(s => s.el === entry.target);
            if (match) {
              navPills.forEach(p => p.classList.remove('active-pill'));
              match.pill.classList.add('active-pill');
            }
          }
        });
      },
      { threshold: 0.2, rootMargin: '-20% 0px -60% 0px' }
    );

    sections.forEach(s => sectionObserver.observe(s.el));
  }

  /* ─── Smooth Scroll for Nav ─── */
  function initSmoothScroll() {
    navPills.forEach(pill => {
      pill.addEventListener('click', (e) => {
        const href = pill.getAttribute('href');
        if (href && href.startsWith('#')) {
          e.preventDefault();
          const target = document.getElementById(href.slice(1));
          if (target) {
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        }
      });
    });
  }

  /* ─── Topbar Background on Scroll ─── */
  const topbar = document.getElementById('topbar');
  let topbarProgress = document.getElementById('topbar-progress');

  // Inject a progress line element into the topbar if it doesn't exist
  if (topbar && !topbarProgress) {
    topbarProgress = document.createElement('div');
    topbarProgress.id = 'topbar-progress';
    topbarProgress.style.position = 'absolute';
    topbarProgress.style.bottom = '0';
    topbarProgress.style.left = '0';
    topbarProgress.style.height = '2px';
    topbarProgress.style.background = 'linear-gradient(90deg, #3b82f6, #ec4899, #f59e0b)';
    topbarProgress.style.width = '0%';
    topbarProgress.style.transition = 'width 0.1s ease-out';
    topbarProgress.style.zIndex = '101';
    topbarProgress.style.boxShadow = '0 0 10px rgba(236,72,153,0.5)';
    topbar.appendChild(topbarProgress);
  }

  function updateTopbar() {
    if (!topbar) return;
    
    // Toggle glassy background vs deeply solid
    if (window.scrollY > 50) {
      topbar.style.background = 'rgba(12, 17, 32, 0.95)';
    } else {
      topbar.style.background = 'rgba(12, 17, 32, 0.8)';
    }

    // Update progress line width based on scroll percentage
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (docHeight > 0 && topbarProgress) {
      const scrollPercent = (window.scrollY / docHeight) * 100;
      topbarProgress.style.width = scrollPercent + '%';
    }
  }

  /* ─── Scroll Handler ─── */
  let ticking = false;
  function onScroll() {
    if (!ticking) {
      requestAnimationFrame(() => {
        updateSpine();
        updateTopbar();
        ticking = false;
      });
      ticking = true;
    }
  }

  /* ─── Init ─── */
  function init() {
    initReveals();
    initFlowPaths();
    initNavTracking();
    initSmoothScroll();
    updateSpine();
    updateTopbar();

    window.addEventListener('scroll', onScroll, { passive: true });

    // Initial spine update after layout
    requestAnimationFrame(() => {
      updateSpine();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
