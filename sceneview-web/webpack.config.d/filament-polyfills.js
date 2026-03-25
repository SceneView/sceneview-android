// Filament npm package uses Node.js modules (path, fs, crypto)
// These are not available in browser — provide empty fallbacks
config.resolve = config.resolve || {};
config.resolve.fallback = {
    ...config.resolve.fallback,
    "path": false,
    "fs": false,
    "crypto": false
};
