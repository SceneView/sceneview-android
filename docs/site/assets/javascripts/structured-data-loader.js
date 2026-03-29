/**
 * Injects structured-data.json as LD+JSON into the page <head>.
 * Only runs on the homepage to avoid duplicate structured data on subpages.
 */
(function () {
  'use strict';
  var path = window.location.pathname;
  if (path !== '/' && path !== '/index.html' && !path.endsWith('/sceneview/')) return;

  fetch('/structured-data.json')
    .then(function (r) { return r.ok ? r.json() : Promise.reject(); })
    .then(function (data) {
      var script = document.createElement('script');
      script.type = 'application/ld+json';
      script.textContent = JSON.stringify(data);
      document.head.appendChild(script);
    })
    .catch(function () { /* structured data is optional — fail silently */ });
})();
