// Filament.js is loaded from CDN via <script> tag before sceneview-web.js.
// Tell webpack to treat 'filament' as an external that resolves to the global 'Filament' object.
// This avoids bundling the Emscripten WASM loader (which needs runtime WASM fetch)
// and instead uses the CDN-hosted version that knows its own WASM URL.
config.externals = Object.assign(config.externals || {}, {
    "filament": "Filament"
});

// Also provide Node.js polyfill fallbacks (in case any transitive dep references them)
config.resolve = config.resolve || {};
config.resolve.fallback = {
    ...config.resolve.fallback,
    "path": false,
    "fs": false,
    "crypto": false
};
