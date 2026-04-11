import { defineConfig } from "vitest/config";

/**
 * Vitest config for the MCP gateway.
 *
 * We deliberately use the default Node pool (not `@cloudflare/vitest-pool-workers`)
 * for unit tests because:
 *   - Hono's `app.request()` is pure JS and runs fine in Node.
 *   - The workers pool currently breaks on Node 22 with
 *     `TypeError: vm._setUnsafeEval is not a function`.
 *   - Unit tests for pure dispatch / registry logic don't need a real Workers runtime.
 *
 * Integration tests that require actual D1/KV bindings will use `wrangler dev`
 * or an explicit Miniflare instance in a separate suite.
 */
export default defineConfig({
  test: {
    environment: "node",
    include: ["test/**/*.test.ts"],
  },
});
