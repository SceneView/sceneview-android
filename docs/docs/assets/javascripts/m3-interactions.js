/**
 * m3-interactions.js — Premium M3-style interactions for SceneView homepage
 * Vanilla JS, performance-first: IntersectionObserver, rAF, passive listeners.
 */
document.addEventListener('DOMContentLoaded', () => {
  'use strict';

  // ---------------------------------------------------------------------------
  // 1. Scroll-triggered reveal animations
  // ---------------------------------------------------------------------------
  const REVEAL_SELECTORS = [
    '.hero-tagline', '.platform-badges', '.stat-row', '.device-section',
    '.showcase-gallery', '.industry-card', '.visual-card', '.bottom-cta',
    '.grid.cards > ul > li', '.demo-container'
  ];

  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      const el = entry.target;
      el.style.opacity = '1';
      el.style.transform = 'translateY(0)';
      Array.from(el.children).forEach((child, i) => {
        child.style.transition = `opacity 0.5s ease ${i * 0.07}s, transform 0.5s ease ${i * 0.07}s`;
        child.style.opacity = '1';
        child.style.transform = 'translateY(0)';
      });
      revealObserver.unobserve(el);
    });
  }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });

  document.querySelectorAll(REVEAL_SELECTORS.join(',')).forEach((el) => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(24px)';
    el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
    Array.from(el.children).forEach((child) => {
      child.style.opacity = '0';
      child.style.transform = 'translateY(16px)';
    });
    revealObserver.observe(el);
  });

  // ---------------------------------------------------------------------------
  // 2. Animated counter for stat-pills
  // ---------------------------------------------------------------------------
  function animateCounter(el) {
    const text = el.textContent.trim();
    const match = text.match(/^([\d,]+)(\+?)$/);
    if (!match) return;
    const target = parseInt(match[1].replace(/,/g, ''), 10);
    const suffix = match[2] || '';
    const duration = 1200;
    const start = performance.now();
    function tick(now) {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(eased * target);
      el.textContent = current.toLocaleString() + suffix;
      if (progress < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  const statObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.querySelectorAll('.stat-pill').forEach((pill) => {
        const numEl = pill.querySelector('strong, b, .stat-value') || pill;
        animateCounter(numEl);
      });
      statObserver.unobserve(entry.target);
    });
  }, { threshold: 0.3 });

  document.querySelectorAll('.stat-row').forEach((row) => statObserver.observe(row));

  // ---------------------------------------------------------------------------
  // 3. Parallax hero background
  // ---------------------------------------------------------------------------
  const hero = document.querySelector('.hero-tagline')?.closest('section')
             || document.querySelector('.hero-tagline')?.parentElement;
  if (hero) {
    let lastScroll = 0;
    let ticking = false;
    const applyParallax = () => {
      const offset = lastScroll * 0.35;
      hero.style.backgroundPositionY = `${offset}px`;
      ticking = false;
    };
    window.addEventListener('scroll', () => {
      lastScroll = window.scrollY;
      if (!ticking) { requestAnimationFrame(applyParallax); ticking = true; }
    }, { passive: true });
  }

  // ---------------------------------------------------------------------------
  // 4. Tilt effect on cards
  // ---------------------------------------------------------------------------
  const TILT_MAX = 4;
  function handleTiltMove(e) {
    const rect = this.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    this.style.transform =
      `perspective(800px) rotateY(${x * TILT_MAX}deg) rotateX(${-y * TILT_MAX}deg) scale(1.02)`;
  }
  function handleTiltLeave() {
    this.style.transform = 'perspective(800px) rotateY(0) rotateX(0) scale(1)';
  }
  document.querySelectorAll('.visual-card, .industry-card').forEach((card) => {
    card.style.transition = 'transform 0.3s ease';
    card.style.willChange = 'transform';
    card.addEventListener('mousemove', handleTiltMove, { passive: true });
    card.addEventListener('mouseleave', handleTiltLeave, { passive: true });
  });

  // ---------------------------------------------------------------------------
  // 4b. Mouse tracking for CSS ripple effect (--mouse-x, --mouse-y)
  // ---------------------------------------------------------------------------
  const RIPPLE_SELECTORS = '.grid.cards > ul > li, .platform-card, .device-section--clickable, .industry-card';
  document.querySelectorAll(RIPPLE_SELECTORS).forEach((el) => {
    el.addEventListener('mousemove', (e) => {
      const rect = el.getBoundingClientRect();
      el.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`);
      el.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`);
    }, { passive: true });
  });

  // ---------------------------------------------------------------------------
  // 5. Smooth scroll for anchor links
  // ---------------------------------------------------------------------------
  document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
    anchor.addEventListener('click', (e) => {
      const id = anchor.getAttribute('href');
      if (!id || id === '#') return;
      const target = document.querySelector(id);
      if (!target) return;
      e.preventDefault();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      history.pushState(null, '', id);
    });
  });

  // ---------------------------------------------------------------------------
  // 6. Auto-cycling showcase (marquee-style)
  // ---------------------------------------------------------------------------
  const gallery = document.querySelector('.showcase-gallery');
  if (gallery) {
    let autoScrollId = null;
    const SCROLL_SPEED = 0.8;
    const autoScroll = () => {
      gallery.scrollLeft += SCROLL_SPEED;
      if (gallery.scrollLeft >= gallery.scrollWidth - gallery.clientWidth - 1) {
        gallery.scrollLeft = 0;
      }
      autoScrollId = requestAnimationFrame(autoScroll);
    };
    const galleryObserver = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        autoScrollId = requestAnimationFrame(autoScroll);
      } else if (autoScrollId) {
        cancelAnimationFrame(autoScrollId);
        autoScrollId = null;
      }
    }, { threshold: 0.1 });
    galleryObserver.observe(gallery);
    gallery.addEventListener('mouseenter', () => {
      if (autoScrollId) { cancelAnimationFrame(autoScrollId); autoScrollId = null; }
    }, { passive: true });
    gallery.addEventListener('mouseleave', () => {
      autoScrollId = requestAnimationFrame(autoScroll);
    }, { passive: true });
    gallery.addEventListener('touchstart', () => {
      if (autoScrollId) { cancelAnimationFrame(autoScrollId); autoScrollId = null; }
    }, { passive: true });
    gallery.addEventListener('touchend', () => {
      autoScrollId = requestAnimationFrame(autoScroll);
    }, { passive: true });

    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        if (autoScrollId) { cancelAnimationFrame(autoScrollId); autoScrollId = null; }
      } else {
        if (!autoScrollId) { autoScrollId = requestAnimationFrame(autoScroll); }
      }
    }, { passive: true });
  }

  // ---------------------------------------------------------------------------
  // 7. Dark mode transition smoothing
  // ---------------------------------------------------------------------------
  const style = document.createElement('style');
  style.textContent = `
    body.m3-theme-transitioning,
    body.m3-theme-transitioning * {
      transition: background-color 0.35s ease, color 0.35s ease,
                  border-color 0.35s ease, box-shadow 0.35s ease !important;
    }`;
  document.head.appendChild(style);
  const schemeObserver = new MutationObserver(() => {
    document.body.classList.add('m3-theme-transitioning');
    setTimeout(() => document.body.classList.remove('m3-theme-transitioning'), 400);
  });
  schemeObserver.observe(document.body, {
    attributes: true,
    attributeFilter: ['data-md-color-scheme']
  });

  // ---------------------------------------------------------------------------
  // 8. Copy code button — "Copied!" toast feedback
  // ---------------------------------------------------------------------------
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('.md-clipboard, .copy-button, button[data-clipboard-target]');
    if (!btn) return;
    const toast = document.createElement('span');
    toast.textContent = 'Copied!';
    Object.assign(toast.style, {
      position: 'absolute', top: '-2rem', left: '50%',
      transform: 'translateX(-50%)', padding: '4px 12px',
      borderRadius: '8px', fontSize: '0.75rem', fontWeight: '600',
      background: 'var(--md-primary-fg-color, #6750a4)',
      color: '#fff', opacity: '0', transition: 'opacity 0.25s ease, top 0.25s ease',
      pointerEvents: 'none', zIndex: '10', whiteSpace: 'nowrap'
    });
    btn.style.position = btn.style.position || 'relative';
    btn.appendChild(toast);
    requestAnimationFrame(() => {
      toast.style.opacity = '1';
      toast.style.top = '-2.5rem';
    });
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 1500);
  });

  // ---------------------------------------------------------------------------
  // 9. Platform badge interaction — scroll to matching device-section
  // ---------------------------------------------------------------------------
  document.querySelectorAll('.platform-badge').forEach((badge) => {
    badge.style.cursor = 'pointer';
    if (!badge.hasAttribute('tabindex')) badge.setAttribute('tabindex', '0');
    if (!badge.hasAttribute('role')) badge.setAttribute('role', 'button');
    const activateBadge = () => {
      const label = badge.textContent.trim().toLowerCase();
      const sections = document.querySelectorAll('.device-section');
      for (const section of sections) {
        const heading = section.querySelector('h2, h3, h4, [class*="title"]');
        if (heading && heading.textContent.toLowerCase().includes(label)) {
          section.scrollIntoView({ behavior: 'smooth', block: 'start' });
          section.style.transition = 'box-shadow 0.3s ease';
          section.style.boxShadow = '0 0 0 3px var(--md-primary-fg-color, #6750a4)';
          setTimeout(() => { section.style.boxShadow = 'none'; }, 1200);
          return;
        }
      }
      if (sections.length) sections[0].scrollIntoView({ behavior: 'smooth', block: 'start' });
    };
    badge.addEventListener('click', activateBadge);
    badge.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        activateBadge();
      }
    });
  });

});
