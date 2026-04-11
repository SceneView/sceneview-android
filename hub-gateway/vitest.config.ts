import { defineConfig } from "vitest/config";

/**
 * Vitest config for the hub MCP gateway (Gateway #2).
 *
 * Mirrors the config used by the sceneview-mcp gateway: default Node
 * pool, no Workers runtime — Hono's `app.request()` is pure JS and
 * runs fine in Node for unit and dispatch tests. Integration tests
 * that need real D1/KV bindings should live in a separate suite
 * running under `wrangler dev`.
 */
export default defineConfig({
  test: {
    environment: "node",
    include: ["test/**/*.test.ts"],
  },
});
