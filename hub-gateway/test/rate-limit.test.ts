/**
 * Rate limit middleware tests.
 *
 * Drives the hub /mcp endpoint with a valid API key and walks the
 * hourly counter up to the free-tier limit (5/h) to verify the
 * transition from 200 → 429. Uses the in-memory FakeD1 + FakeKV
 * helpers — no Miniflare, no real clock manipulation.
 */

import { describe, it, expect, beforeEach } from "vitest";
import app from "../src/index.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { JSON_RPC_ERRORS } from "../src/mcp/transport.js";
import { TIER_LIMITS } from "../src/rate-limit/limits.js";
import {
  hourlyBucketKey,
  isoHourBucket,
} from "../src/rate-limit/kv-counter.js";
import { makeEnv, FakeKV } from "./helpers/fake-bindings.js";

const FREE_KEY = "sv_live_FREERATE00000000000000000000000";
const PRO_KEY = "sv_live_PRORATE000000000000000000000000";

interface Ctx {
  env: ReturnType<typeof makeEnv>["env"];
  kv: FakeKV;
}

let ctx: Ctx;

beforeEach(async () => {
  const freeHash = await hashApiKey(FREE_KEY);
  const proHash = await hashApiKey(PRO_KEY);
  ctx = makeEnv({
    api_keys: [
      {
        id: "key_free",
        user_id: "usr_free",
        key_hash: freeHash,
        key_prefix: "sv_live_FREERA",
        revoked_at: null,
      },
      {
        id: "key_pro",
        user_id: "usr_pro",
        key_hash: proHash,
        key_prefix: "sv_live_PRORAT",
        revoked_at: null,
      },
    ],
    users: [
      { id: "usr_free", email: "free@hub.test", tier: "free" },
      { id: "usr_pro", email: "pro@hub.test", tier: "pro" },
    ],
  });
});

function rpc(key: string): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${key}`,
    },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }),
  });
}

describe("rateLimitMiddleware", () => {
  it("exposes X-RateLimit-* headers on a successful request", async () => {
    const res = await app.request(rpc(PRO_KEY), {}, ctx.env);
    expect(res.status).toBe(200);
    expect(res.headers.get("X-RateLimit-Limit")).toBe(
      String(TIER_LIMITS.pro.hourly),
    );
    // First request — remaining should be `limit - 1`.
    expect(res.headers.get("X-RateLimit-Remaining")).toBe(
      String(TIER_LIMITS.pro.hourly - 1),
    );
    expect(res.headers.get("X-RateLimit-Reset")).toMatch(/^\d+$/);
  });

  it("counts down the remaining budget across multiple calls", async () => {
    const r1 = await app.request(rpc(PRO_KEY), {}, ctx.env);
    const r2 = await app.request(rpc(PRO_KEY), {}, ctx.env);
    const r3 = await app.request(rpc(PRO_KEY), {}, ctx.env);
    expect(r1.status).toBe(200);
    expect(r2.status).toBe(200);
    expect(r3.status).toBe(200);
    // Hourly limit is 700 for Pro; after 3 calls remaining should
    // be somewhere in [696, 697]. The sliding window weight on the
    // previous bucket (empty here) means the math is deterministic.
    const remaining = Number(r3.headers.get("X-RateLimit-Remaining"));
    expect(remaining).toBeGreaterThanOrEqual(696);
    expect(remaining).toBeLessThanOrEqual(698);
  });

  it("returns 429 + JSON-RPC -32002 when the free tier runs out", async () => {
    // Free tier = 5/h. Walk up to the limit.
    const limit = TIER_LIMITS.free.hourly;
    for (let i = 0; i < limit; i++) {
      const res = await app.request(rpc(FREE_KEY), {}, ctx.env);
      expect(res.status, `call ${i + 1}`).toBe(200);
    }
    // The (limit+1)th call must be rate-limited.
    const res = await app.request(rpc(FREE_KEY), {}, ctx.env);
    expect(res.status).toBe(429);
    expect(res.headers.get("X-RateLimit-Limit")).toBe(String(limit));
    const body = (await res.json()) as {
      error: {
        code: number;
        message: string;
        data: {
          tier: string;
          scope: string;
          limit: number;
          remaining: number;
          resetAt: number;
        };
      };
    };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.RATE_LIMITED);
    expect(body.error.message).toBe("Rate limit exceeded");
    expect(body.error.data.tier).toBe("free");
    expect(body.error.data.scope).toBe("hourly");
    expect(body.error.data.limit).toBe(limit);
    expect(body.error.data.resetAt).toBeGreaterThan(Date.now());
  });

  it("keeps per-user KV counters isolated", async () => {
    // Drain the free key entirely.
    for (let i = 0; i < TIER_LIMITS.free.hourly; i++) {
      await app.request(rpc(FREE_KEY), {}, ctx.env);
    }
    const blocked = await app.request(rpc(FREE_KEY), {}, ctx.env);
    expect(blocked.status).toBe(429);

    // The Pro key must NOT be affected.
    const still = await app.request(rpc(PRO_KEY), {}, ctx.env);
    expect(still.status).toBe(200);
  });

  it("uses the hub-rl: KV prefix so Gateway #1 cannot clash", async () => {
    await app.request(rpc(PRO_KEY), {}, ctx.env);
    const proHash = await hashApiKey(PRO_KEY);
    const bucket = isoHourBucket();
    const key = hourlyBucketKey(proHash, bucket);
    expect(key.startsWith("hub-rl:")).toBe(true);
    expect(ctx.kv.has(key)).toBe(true);
  });
});
