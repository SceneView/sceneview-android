// ===== DARK MODE =====
(function initTheme() {
  const saved = localStorage.getItem('sceneview-theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const theme = saved || (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', theme);
})();

document.getElementById('themeToggle').addEventListener('click', function () {
  const current = document.documentElement.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('sceneview-theme', next);
});

// ===== TAB SWITCHING =====
document.querySelectorAll('.tabs').forEach(function (tabGroup) {
  var buttons = tabGroup.querySelectorAll('.tabs__btn');
  var panels = tabGroup.querySelectorAll('.tabs__panel');

  buttons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      var target = btn.getAttribute('data-tab');

      buttons.forEach(function (b) { b.classList.remove('tabs__btn--active'); });
      panels.forEach(function (p) { p.classList.remove('tabs__panel--active'); });

      btn.classList.add('tabs__btn--active');
      tabGroup.querySelector('[data-panel="' + target + '"]').classList.add('tabs__panel--active');
    });
  });
});

// ===== MOBILE HAMBURGER =====
var hamburger = document.getElementById('hamburger');
var navLinks = document.getElementById('navLinks');
var overlay = document.createElement('div');
overlay.className = 'nav-overlay';
document.body.appendChild(overlay);

function toggleMenu() {
  hamburger.classList.toggle('active');
  navLinks.classList.toggle('open');
  overlay.classList.toggle('active');
  document.body.style.overflow = navLinks.classList.contains('open') ? 'hidden' : '';
}

hamburger.addEventListener('click', toggleMenu);
overlay.addEventListener('click', toggleMenu);

// Close mobile menu on link click
navLinks.querySelectorAll('.nav__link').forEach(function (link) {
  link.addEventListener('click', function () {
    if (navLinks.classList.contains('open')) {
      toggleMenu();
    }
  });
});

// ===== SMOOTH SCROLL =====
document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
  anchor.addEventListener('click', function (e) {
    var target = document.querySelector(this.getAttribute('href'));
    if (target) {
      e.preventDefault();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
});
