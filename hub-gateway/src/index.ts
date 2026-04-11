/**
 * Hub MCP Gateway — entry point.
 *
 * Gateway #2 of the Cloudflare Workers MCP stack. Consolidates the
 * 15+ non-SceneView MCPs from the portfolio behind a single hosted
 * endpoint with unified Portfolio Access billing.
 *
 * Sibling of mcp-gateway/src/index.ts (Gateway #1, sceneview-mcp).
 * The two workers share a D1 database and a KV namespace so one
 * subscription covers both. See wrangler.toml for the wiring.
 *
 * MVP scope (this commit):
 *   - /health JSON endpoint
 *   - /, /pricing, /docs HTML routes
 *   - POST /mcp JSON-RPC endpoint with `initialize`, `tools/list`,
 *     `tools/call` — auth and rate-limit TODO, see routes/mcp.ts
 *   - 1 pilot library: architecture-mcp (stub dispatcher)
 *
 * Out of MVP (follow-up sessions):
 *   - Auth middleware (ported from mcp-gateway/src/auth/middleware.ts)
 *   - Rate limit middleware
 *   - Stripe checkout + webhook routes
 *   - Usage logging into D1
 *   - Wiring the remaining 14 portfolio MCPs
 */

import { Hono } from "hono";
import type { Env } from "./env.js";
import type { AuthVariables } from "./auth/middleware.js";
import { landingRoutes } from "./routes/landing.js";
import { mcpRoutes } from "./routes/mcp.js";
import { getRegistrySummary } from "./mcp/registry.js";

const app = new Hono<{ Bindings: Env; Variables: AuthVariables }>();

// ── Health check ────────────────────────────────────────────────────────────

app.get("/health", (c) => {
  const summary = getRegistrySummary();
  return c.json({
    ok: true,
    service: "hub-mcp-gateway",
    version: "0.0.1",
    environment: c.env.ENVIRONMENT ?? "unknown",
    registry: summary,
  });
});

// ── MCP JSON-RPC endpoint ───────────────────────────────────────────────────

app.route("/mcp", mcpRoutes());

// ── HTML routes: landing, pricing, docs ────────────────────────────────────

app.route("/", landingRoutes());

// ── Fallback 404 ────────────────────────────────────────────────────────────

app.notFound((c) => c.json({ error: "Not Found" }, 404));

export default app;
