import { Hono } from "hono";
import { cors } from "hono/cors";
import type { Env } from "./env.js";
import { isRateLimited } from "./rate-limit.js";
import { rollupYesterday } from "./rollup.js";

const app = new Hono<{ Bindings: Env }>();

// ── CORS — allow calls from any origin (npx, browser, etc.) ─────────────────
app.use(
  "/v1/*",
  cors({
    origin: "*",
    allowMethods: ["POST", "OPTIONS"],
    allowHeaders: ["content-type"],
    maxAge: 86400,
  }),
);

// ── Health check ─────────────────────────────────────────────────────────────
app.get("/health", (c) =>
  c.json({ ok: true, service: "sceneview-telemetry", version: "1.0.0" }),
);

// ── Payload validation ───────────────────────────────────────────────────────
const VALID_EVENTS = new Set(["init", "tool"]);
const VALID_TIERS = new Set(["free", "pro"]);
const MAX_STRING_LEN = 128;

interface EventPayload {
  timestamp: string;
  event: "init" | "tool";
  client: string;
  clientVersion: string;
  mcpVersion: string;
  tier: "free" | "pro";
  tool?: string;
}

function validatePayload(body: unknown): EventPayload | null {
  if (!body || typeof body !== "object") return null;

  const b = body as Record<string, unknown>;

  if (typeof b.timestamp !== "string" || b.timestamp.length > MAX_STRING_LEN)
    return null;
  if (typeof b.event !== "string" || !VALID_EVENTS.has(b.event)) return null;
  if (typeof b.client !== "string" || b.client.length > MAX_STRING_LEN)
    return null;
  if (
    typeof b.clientVersion !== "string" ||
    b.clientVersion.length > MAX_STRING_LEN
  )
    return null;
  if (typeof b.mcpVersion !== "string" || b.mcpVersion.length > MAX_STRING_LEN)
    return null;
  if (typeof b.tier !== "string" || !VALID_TIERS.has(b.tier)) return null;

  // tool is optional but must be a bounded string if present
  if (b.tool !== undefined) {
    if (typeof b.tool !== "string" || b.tool.length > MAX_STRING_LEN)
      return null;
  }

  return {
    timestamp: b.timestamp,
    event: b.event as "init" | "tool",
    client: b.client,
    clientVersion: b.clientVersion,
    mcpVersion: b.mcpVersion,
    tier: b.tier as "free" | "pro",
    tool: typeof b.tool === "string" ? b.tool : undefined,
  };
}

// ── POST /v1/events ──────────────────────────────────────────────────────────
app.post("/v1/events", async (c) => {
  // Rate limit by IP (hashed — never stored raw)
  const ip = c.req.header("cf-connecting-ip") ?? "unknown";
  if (await isRateLimited(ip, c.env.RL_KV)) {
    return c.json({ error: "rate_limited" }, 429);
  }

  // Parse & validate
  let body: unknown;
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "invalid_json" }, 400);
  }

  const payload = validatePayload(body);
  if (!payload) {
    return c.json({ error: "invalid_payload" }, 400);
  }

  // Insert into D1 — fire-and-forget style but we await for correctness
  try {
    await c.env.DB.prepare(
      `INSERT INTO events (timestamp, event, client, client_ver, mcp_ver, tier, tool)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    )
      .bind(
        payload.timestamp,
        payload.event,
        payload.client,
        payload.clientVersion,
        payload.mcpVersion,
        payload.tier,
        payload.tool ?? null,
      )
      .run();
  } catch {
    // D1 write failed — log but don't fail the client.
    // In production, this would go to Workers Logpush.
    console.error("D1 insert failed");
    return c.json({ ok: true, stored: false }, 202);
  }

  return c.json({ ok: true }, 202);
});

// ── Batch endpoint (future-proof for client-side batching) ───────────────────
app.post("/v1/batch", async (c) => {
  const ip = c.req.header("cf-connecting-ip") ?? "unknown";
  if (await isRateLimited(ip, c.env.RL_KV)) {
    return c.json({ error: "rate_limited" }, 429);
  }

  let body: unknown;
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "invalid_json" }, 400);
  }

  if (!Array.isArray(body) || body.length === 0 || body.length > 50) {
    return c.json({ error: "invalid_batch", max: 50 }, 400);
  }

  const validated = body.map(validatePayload).filter(Boolean) as EventPayload[];
  if (validated.length === 0) {
    return c.json({ error: "no_valid_events" }, 400);
  }

  // Batch insert with D1 batch API
  try {
    const stmt = c.env.DB.prepare(
      `INSERT INTO events (timestamp, event, client, client_ver, mcp_ver, tier, tool)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    );

    await c.env.DB.batch(
      validated.map((p) =>
        stmt.bind(
          p.timestamp,
          p.event,
          p.client,
          p.clientVersion,
          p.mcpVersion,
          p.tier,
          p.tool ?? null,
        ),
      ),
    );
  } catch {
    console.error("D1 batch insert failed");
    return c.json({ ok: true, accepted: validated.length, stored: false }, 202);
  }

  return c.json({ ok: true, accepted: validated.length }, 202);
});

// ── Stats endpoint (simple read-only dashboard data) ─────────────────────────
app.get("/v1/stats", async (c) => {
  // Bearer token guard — only enforced when STATS_TOKEN is configured
  const token = c.env.STATS_TOKEN;
  if (token) {
    const auth = c.req.header("Authorization") ?? "";
    if (auth !== `Bearer ${token}`) {
      return c.json({ error: "unauthorized" }, 401);
    }
  }

  try {
    const [totals, topTools, versions] = await Promise.all([
      c.env.DB.prepare(
        `SELECT
           COUNT(*) as total,
           COUNT(CASE WHEN event = 'init' THEN 1 END) as inits,
           COUNT(CASE WHEN event = 'tool' THEN 1 END) as tools,
           COUNT(DISTINCT client) as unique_clients
         FROM events
         WHERE ingested > datetime('now', '-24 hours')`,
      ).first(),
      c.env.DB.prepare(
        `SELECT tool, COUNT(*) as count
         FROM events
         WHERE event = 'tool' AND ingested > datetime('now', '-24 hours')
         GROUP BY tool
         ORDER BY count DESC
         LIMIT 20`,
      ).all(),
      c.env.DB.prepare(
        `SELECT mcp_ver, COUNT(*) as count
         FROM events
         WHERE ingested > datetime('now', '-24 hours')
         GROUP BY mcp_ver
         ORDER BY count DESC`,
      ).all(),
    ]);

    return c.json({
      period: "24h",
      totals,
      topTools: topTools.results,
      versions: versions.results,
    });
  } catch {
    return c.json({ error: "query_failed" }, 500);
  }
});

// ── Catch-all ────────────────────────────────────────────────────────────────
app.all("*", (c) => c.json({ error: "not_found" }, 404));

export { app };

export default {
  fetch: app.fetch,
  async scheduled(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
    ctx.waitUntil(rollupYesterday(env.DB));
  },
};
