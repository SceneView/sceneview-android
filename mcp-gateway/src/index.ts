import { Hono } from "hono";
import type { Env } from "./env.js";
import type { AuthVariables } from "./auth/middleware.js";
import type { SessionVariables } from "./auth/session-middleware.js";
import { mcpRoutes } from "./routes/mcp.js";
import { authRoutes } from "./routes/auth.js";
import { dashboardRoutes } from "./routes/dashboard.js";
import { billingRoutes } from "./routes/billing.js";
import { webhookRoutes } from "./routes/webhooks.js";
import { renderLoginPage } from "./dashboard/login-page.js";

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

// ── Dashboard auth routes (login, verify, logout) ───────────────────────────

app.route("/", authRoutes({ renderLoginPage }));

// ── Billing actions (Stripe checkout, portal) ──────────────────────────────

app.route("/", billingRoutes());

// ── Stripe webhook receiver ────────────────────────────────────────────────

app.route("/", webhookRoutes());

// ── Fallback 404 ────────────────────────────────────────────────────────────

app.notFound((c) => c.json({ error: "Not Found" }, 404));

export default app;
