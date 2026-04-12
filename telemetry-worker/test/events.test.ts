import { describe, it, expect, beforeEach } from "vitest";
import { app } from "../src/index.js";
import { createMockD1 } from "./helpers/mock-d1.js";
import { createMockKV } from "./helpers/mock-kv.js";

function makeEnv() {
  return {
    DB: createMockD1() as unknown as D1Database,
    RL_KV: createMockKV() as unknown as KVNamespace,
    ENVIRONMENT: "test",
  };
}

function validPayload(overrides: Record<string, unknown> = {}) {
  return {
    timestamp: "2026-04-12T10:00:00.000Z",
    event: "tool",
    client: "claude-desktop",
    clientVersion: "1.2.3",
    mcpVersion: "4.0.0-rc.1",
    tier: "free",
    tool: "get_node_info",
    ...overrides,
  };
}

function post(
  path: string,
  body: unknown,
  env: ReturnType<typeof makeEnv>,
  headers: Record<string, string> = {},
) {
  return app.request(
    path,
    {
      method: "POST",
      headers: { "content-type": "application/json", ...headers },
      body: JSON.stringify(body),
    },
    env,
  );
}

describe("POST /v1/events", () => {
  let env: ReturnType<typeof makeEnv>;

  beforeEach(() => {
    env = makeEnv();
  });

  it("accepts a valid tool event", async () => {
    const res = await post("/v1/events", validPayload(), env);
    expect(res.status).toBe(202);

    const json = await res.json();
    expect(json).toEqual({ ok: true });

    // Verify D1 insert
    const db = env.DB as unknown as ReturnType<typeof createMockD1>;
    expect(db._statements).toHaveLength(1);
    expect(db._statements[0].params).toEqual([
      "2026-04-12T10:00:00.000Z",
      "tool",
      "claude-desktop",
      "1.2.3",
      "4.0.0-rc.1",
      "free",
      "get_node_info",
    ]);
  });

  it("accepts a valid init event (no tool field)", async () => {
    const res = await post(
      "/v1/events",
      validPayload({ event: "init", tool: undefined }),
      env,
    );
    expect(res.status).toBe(202);

    const db = env.DB as unknown as ReturnType<typeof createMockD1>;
    expect(db._statements[0].params[6]).toBeNull(); // tool = null
  });

  it("rejects invalid JSON", async () => {
    const res = await app.request("/v1/events", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "not json",
    }, env);
    expect(res.status).toBe(400);
    expect(await res.json()).toEqual({ error: "invalid_json" });
  });

  it("rejects unknown event type", async () => {
    const res = await post("/v1/events", validPayload({ event: "crash" }), env);
    expect(res.status).toBe(400);
    expect(await res.json()).toEqual({ error: "invalid_payload" });
  });

  it("rejects unknown tier", async () => {
    const res = await post("/v1/events", validPayload({ tier: "enterprise" }), env);
    expect(res.status).toBe(400);
  });

  it("rejects oversized strings", async () => {
    const res = await post(
      "/v1/events",
      validPayload({ client: "x".repeat(200) }),
      env,
    );
    expect(res.status).toBe(400);
  });

  it("rejects missing required fields", async () => {
    const { client, ...incomplete } = validPayload();
    const res = await post("/v1/events", incomplete, env);
    expect(res.status).toBe(400);
  });

  it("returns 202 with stored:false on D1 failure", async () => {
    const db = env.DB as unknown as ReturnType<typeof createMockD1>;
    db._shouldFail = true;

    const res = await post("/v1/events", validPayload(), env);
    expect(res.status).toBe(202);
    expect(await res.json()).toEqual({ ok: true, stored: false });
  });
});

describe("POST /v1/batch", () => {
  let env: ReturnType<typeof makeEnv>;

  beforeEach(() => {
    env = makeEnv();
  });

  it("accepts a batch of valid events", async () => {
    const batch = [
      validPayload({ tool: "get_node_info" }),
      validPayload({ tool: "search_models", event: "tool" }),
      validPayload({ event: "init", tool: undefined }),
    ];

    const res = await post("/v1/batch", batch, env);
    expect(res.status).toBe(202);
    expect(await res.json()).toEqual({ ok: true, accepted: 3 });
  });

  it("filters out invalid events in batch", async () => {
    const batch = [
      validPayload(),
      { garbage: true }, // invalid
      validPayload({ tool: "other_tool" }),
    ];

    const res = await post("/v1/batch", batch, env);
    expect(res.status).toBe(202);
    expect(await res.json()).toEqual({ ok: true, accepted: 2 });
  });

  it("rejects empty batch", async () => {
    const res = await post("/v1/batch", [], env);
    expect(res.status).toBe(400);
  });

  it("rejects oversized batch (>50)", async () => {
    const batch = Array.from({ length: 51 }, () => validPayload());
    const res = await post("/v1/batch", batch, env);
    expect(res.status).toBe(400);
  });

  it("rejects batch with all invalid events", async () => {
    const batch = [{ garbage: true }, { also: "garbage" }];
    const res = await post("/v1/batch", batch, env);
    expect(res.status).toBe(400);
    expect(await res.json()).toEqual({ error: "no_valid_events" });
  });
});

describe("Rate limiting", () => {
  it("returns 429 after exceeding limit", async () => {
    const env = makeEnv();

    // Simulate 30 prior requests in the KV bucket
    const kv = env.RL_KV as unknown as ReturnType<typeof createMockKV>;
    // We need to fill any matching key — the rate limiter uses hashed IP + minute bucket.
    // Easier: just send 31 requests.
    for (let i = 0; i < 30; i++) {
      const res = await post("/v1/events", validPayload(), env);
      expect(res.status).toBe(202);
    }

    // 31st should be rate-limited
    const res = await post("/v1/events", validPayload(), env);
    expect(res.status).toBe(429);
    expect(await res.json()).toEqual({ error: "rate_limited" });
  });
});

describe("GET /health", () => {
  it("returns 200 with service info", async () => {
    const env = makeEnv();
    const res = await app.request("/health", { method: "GET" }, env);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({
      ok: true,
      service: "sceneview-telemetry",
      version: "1.0.0",
    });
  });
});

describe("GET /v1/stats", () => {
  it("returns stats structure", async () => {
    const env = makeEnv();
    const db = env.DB as unknown as ReturnType<typeof createMockD1>;
    // Mock the expected row shapes
    db._rows = [{ total: 42, inits: 10, tools: 32, unique_clients: 3 }];

    const res = await app.request("/v1/stats", { method: "GET" }, env);
    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.period).toBe("24h");
    expect(json.totals).toBeDefined();
  });
});

describe("CORS", () => {
  it("responds to OPTIONS preflight", async () => {
    const env = makeEnv();
    const res = await app.request("/v1/events", {
      method: "OPTIONS",
      headers: {
        Origin: "http://localhost:3000",
        "Access-Control-Request-Method": "POST",
      },
    }, env);
    expect(res.status).toBe(204);
    expect(res.headers.get("access-control-allow-origin")).toBe("*");
  });
});

describe("404 catch-all", () => {
  it("returns 404 for unknown routes", async () => {
    const env = makeEnv();
    const res = await app.request("/unknown", { method: "GET" }, env);
    expect(res.status).toBe(404);
  });
});
