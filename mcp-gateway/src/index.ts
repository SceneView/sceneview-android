import { Hono } from "hono";
import type { Env } from "./env.js";
import type { AuthVariables } from "./auth/middleware.js";
import type { SessionVariables } from "./auth/session-middleware.js";
import { mcpRoutes } from "./routes/mcp.js";
import { authRoutes } from "./routes/auth.js";

type AppVariables = AuthVariables & SessionVariables;

const app = new Hono<{ Bindings: Env; Variables: AppVariables }>();

// ── Root ────────────────────────────────────────────────────────────────────

app.get("/", (c) => c.text("SceneView MCP Gateway"));

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

// ── Dashboard auth routes (login, verify, logout) ───────────────────────────

app.route("/", authRoutes());

// ── Fallback 404 ────────────────────────────────────────────────────────────

app.notFound((c) => c.json({ error: "Not Found" }, 404));

export default app;
