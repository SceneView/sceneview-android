/**
 * End-to-end tests for the wired POST /mcp route.
 *
 * These tests exercise the full middleware chain:
 *   authMiddleware → rateLimitMiddleware → handleMcpRequest →
 *   registry dispatch → usage logging.
 *
 * Bindings are mocked with the in-memory D1 and KV helpers so the
 * suite runs in the default Node vitest pool.
 */

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import app from "../src/index.js";
import type { Env } from "../src/env.js";
import { insertUser, updateUserTier } from "../src/db/users.js";
import { createApiKey } from "../src/auth/api-keys.js";
import { countUsageInMonth, monthBucket } from "../src/db/usage.js";
import { hourlyBucketKey, isoHourBucket } from "../src/rate-limit/kv-counter.js";
import { quotaKey } from "../src/rate-limit/quotas.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

let mock: MockD1;
let kv: MockKv;

beforeEach(async () => {
  mock = await createMockD1();
  kv = new MockKv();
});
afterEach(() => {
  mock.close();
});

function env(): Env {
  return {
    DB: mock.db,
    RL_KV: kv.asKv(),
    ENVIRONMENT: "test",
  } as unknown as Env;
}

function postMcp(body: unknown, headers: Record<string, string> = {}) {
  return app.request(
    "/mcp",
    {
      method: "POST",
      headers: { "content-type": "application/json", ...headers },
      body: JSON.stringify(body),
    },
    env(),
  );
}

async function seedUserKey(tier: "free" | "pro" = "free", userId = "usr_e") {
  await insertUser(mock.db, { id: userId, email: `${userId}@example.com` });
  if (tier !== "free") await updateUserTier(mock.db, userId, tier);
  return createApiKey(mock.db, userId, "e2e");
}

describe("POST /mcp: auth failures", () => {
  it("returns 401 JSON-RPC when no credentials are present", async () => {
    const res = await postMcp({
      jsonrpc: "2.0",
      id: 1,
      method: "initialize",
    });
    expect(res.status).toBe(401);
    const body = (await res.json()) as {
      error: { code: number; message: string };
    };
    expect(body.error.code).toBe(-32001);
  });

  it("returns 401 for an unknown key", async () => {
    const res = await postMcp(
      { jsonrpc: "2.0", id: 1, method: "initialize" },
      { authorization: "Bearer sv_live_DOESNOTEXIST12345678901234567890" },
    );
    expect(res.status).toBe(401);
  });
});

describe("POST /mcp: happy path", () => {
  it("initialize handshake succeeds and echoes Mcp-Session-Id", async () => {
    const { plaintext } = await seedUserKey("free");
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 1,
        method: "initialize",
        params: { protocolVersion: "2025-03-26" },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(200);
    expect(res.headers.get("mcp-session-id")).toBeTruthy();
    const body = (await res.json()) as {
      result: { protocolVersion: string; serverInfo: { name: string } };
    };
    expect(body.result.protocolVersion).toBe("2025-03-26");
    expect(body.result.serverInfo.name).toBe("sceneview-mcp-gateway");
  });

  it("tools/list returns the multiplexed tool list", async () => {
    const { plaintext } = await seedUserKey("free");
    const res = await postMcp(
      { jsonrpc: "2.0", id: 2, method: "tools/list" },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      result: { tools: Array<{ name: string }> };
    };
    const names = new Set(body.result.tools.map((t) => t.name));
    expect(names.has("list_samples")).toBe(true);
    expect(names.size).toBeGreaterThan(20);
  });

  it("tools/call on a free tool returns the handler output and logs usage", async () => {
    const { plaintext, row } = await seedUserKey("free");
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 3,
        method: "tools/call",
        params: { name: "list_samples", arguments: {} },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      result: { content: Array<{ type: string; text: string }> };
    };
    expect(body.result.content[0].type).toBe("text");

    // Usage row was written.
    const used = await countUsageInMonth(mock.db, row.id, monthBucket());
    expect(used).toBe(1);
  });
});

describe("POST /mcp: tier enforcement", () => {
  it("returns ACCESS_DENIED (-32003) when a free user hits a pro tool", async () => {
    const { plaintext } = await seedUserKey("free", "usr_free");
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 4,
        method: "tools/call",
        params: { name: "generate_scene", arguments: {} },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      error: { code: number; message: string };
    };
    expect(body.error?.code).toBe(-32003);
  });

  it("allows a pro user to call pro tools", async () => {
    const { plaintext } = await seedUserKey("pro", "usr_pro_e2e");
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 5,
        method: "tools/call",
        params: { name: "generate_scene", arguments: {} },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    const body = (await res.json()) as { result?: unknown; error?: unknown };
    // The upstream handler may or may not produce a structured error
    // for an empty argument bag, but the middleware chain must NOT
    // reject the call with an access-denied code.
    expect((body as { error?: { code?: number } }).error?.code).not.toBe(-32003);
  });
});

describe("POST /mcp: rate limiting", () => {
  it("returns 429 when the hourly limit is exhausted", async () => {
    const { plaintext, row } = await seedUserKey("free", "usr_rl_e2e");
    await kv.put(
      hourlyBucketKey(row.key_hash, isoHourBucket()),
      "60",
    );
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 6,
        method: "tools/call",
        params: { name: "list_samples", arguments: {} },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(429);
    const body = (await res.json()) as {
      error: { code: number; data: { scope: string } };
    };
    expect(body.error.code).toBe(-32002);
    expect(body.error.data.scope).toBe("hourly");
  });

  it("returns 429 when the monthly quota is exhausted", async () => {
    const { plaintext, row } = await seedUserKey("free", "usr_qt_e2e");
    await kv.put(quotaKey(row.key_hash, monthBucket()), "1000");
    const res = await postMcp(
      {
        jsonrpc: "2.0",
        id: 7,
        method: "tools/call",
        params: { name: "list_samples", arguments: {} },
      },
      { authorization: `Bearer ${plaintext}` },
    );
    expect(res.status).toBe(429);
  });
});

describe("POST /mcp: health + GET fallback", () => {
  it("GET /mcp returns 501 (SSE placeholder)", async () => {
    const { plaintext } = await seedUserKey();
    const res = await app.request(
      "/mcp",
      {
        method: "GET",
        headers: { authorization: `Bearer ${plaintext}` },
      },
      env(),
    );
    expect(res.status).toBe(501);
  });

  it("GET /health still works (unprotected)", async () => {
    const res = await app.request("/health", undefined, env());
    expect(res.status).toBe(200);
  });
});
