(function () {
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const revealElements = Array.from(document.querySelectorAll('.reveal'));
  const pathElements = Array.from(document.querySelectorAll('.draw-path'));

  const setPathLengths = () => {
    pathElements.forEach((path) => {
      try {
        const length = path.getTotalLength();
        path.style.strokeDasharray = String(length);
        path.style.strokeDashoffset = String(length);
      } catch (error) {
        path.style.strokeDasharray = '1200';
        path.style.strokeDashoffset = '1200';
      }
    });
  };

  const revealDrawPaths = (container) => {
    const localPaths = Array.from(container.querySelectorAll('.draw-path'));
    localPaths.forEach((path, index) => {
      path.style.transitionDelay = `${index * 120}ms`;
      path.classList.add('is-visible');
      path.style.strokeDashoffset = '0';
    });
  };

  setPathLengths();

  if (reduceMotion) {
    revealElements.forEach((el) => {
      el.classList.add('is-visible');
      revealDrawPaths(el);
    });
    return;
  }

  const observer = new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return;
        }

        const target = entry.target;
        target.classList.add('is-visible');

        if (target.classList.contains('draw-when-visible')) {
          revealDrawPaths(target);
        }

        if (target.getAttribute('data-once') !== 'false') {
          obs.unobserve(target);
        }
      });
    },
    {
      threshold: 0.2,
      rootMargin: '0px 0px -8% 0px',
    },
  );

  revealElements.forEach((el) => observer.observe(el));

  const parallaxElements = Array.from(document.querySelectorAll('[data-parallax]'));
  if (parallaxElements.length > 0) {
    window.addEventListener(
      'scroll',
      () => {
        const currentY = window.scrollY;
        parallaxElements.forEach((el) => {
          const speed = Number(el.getAttribute('data-parallax') || 0);
          el.style.transform = `translate3d(0, ${currentY * speed}px, 0)`;
        });
      },
      { passive: true },
    );
  }
})();