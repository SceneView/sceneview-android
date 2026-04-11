import { Hono } from "hono";
import type { Env } from "./env.js";
import type { AuthVariables } from "./auth/middleware.js";
import type { SessionVariables } from "./auth/session-middleware.js";
import { mcpRoutes } from "./routes/mcp.js";
import { authRoutes } from "./routes/auth.js";
import { dashboardRoutes } from "./routes/dashboard.js";
import { billingRoutes } from "./routes/billing.js";
import { checkoutSuccessRoutes } from "./routes/checkout-success.js";
import { webhookRoutes } from "./routes/webhooks.js";

type AppVariables = AuthVariables & SessionVariables;

const app = new Hono<{ Bindings: Env; Variables: AppVariables }>();

// ── Health check ────────────────────────────────────────────────────────────

app.get("/health", (c) =>
  c.json({
    ok: true,
    service: "sceneview-mcp-gateway",
    version: "0.0.1",
  }),
);

// ── MCP endpoint ────────────────────────────────────────────────────────────

app.route("/mcp", mcpRoutes());

// ── Dashboard HTML routes (landing, pricing, docs, dashboard, billing) ─────

app.route("/", dashboardRoutes());

// ── Dashboard auth stubs — magic-link is disabled in the MVP ──────────────
//
// The /login, /auth/verify and /auth/logout routes currently return
// 503 Service Unavailable. They are kept in the router so that bots and
// old links get a clean HTTP response instead of a 404. See
// `routes/auth.ts` for the full MVP-disabled stub.

app.route("/", authRoutes());

// ── Billing actions (Stripe checkout) ──────────────────────────────────────

app.route("/", billingRoutes());

// ── Checkout success page (reads the KV handoff) ───────────────────────────

app.route("/", checkoutSuccessRoutes());

// ── Stripe webhook receiver ────────────────────────────────────────────────

app.route("/", webhookRoutes());

// ── Fallback 404 ────────────────────────────────────────────────────────────

app.notFound((c) => c.json({ error: "Not Found" }, 404));

export default app;
