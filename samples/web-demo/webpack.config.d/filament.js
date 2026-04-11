// Kotlin/JS merges every file in this directory into Webpack's config before
// the build runs. We disable Node polyfills for modules that filament.js
// imports unconditionally but never uses at runtime in the browser.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    path: false,
    fs: false,
    crypto: false,
});
