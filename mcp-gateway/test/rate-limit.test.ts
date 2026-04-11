/**
 * Rate limiting tests: both the pure KV counter primitives and the
 * end-to-end Hono middleware (auth + rate limit) against mocked
 * bindings.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  authMiddleware,
  type AuthVariables,
} from "../src/auth/middleware.js";
import { createApiKey } from "../src/auth/api-keys.js";
import { insertUser, updateUserTier } from "../src/db/users.js";
import { rateLimitMiddleware } from "../src/middleware/rate-limit.js";
import {
  checkAndIncrementHourly,
  hourlyBucketKey,
  isoHourBucket,
  KV_HOURLY_PREFIX,
  previousIsoHourBucket,
} from "../src/rate-limit/kv-counter.js";
import { getLimitsForTier, TIER_LIMITS } from "../src/rate-limit/limits.js";
import {
  checkMonthlyQuota,
  getMonthlyUsage,
  incrementQuotaCache,
  quotaKey,
} from "../src/rate-limit/quotas.js";
import { insertUsageRecord, monthBucket } from "../src/db/usage.js";
import type { Env } from "../src/env.js";
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

describe("limits table", () => {
  it("matches the monetisation plan (free/pro/team hourly + monthly)", () => {
    expect(TIER_LIMITS.free.hourly).toBe(60);
    expect(TIER_LIMITS.free.monthly).toBe(1000);
    expect(TIER_LIMITS.pro.hourly).toBe(600);
    expect(TIER_LIMITS.pro.monthly).toBe(10_000);
    expect(TIER_LIMITS.team.hourly).toBe(3000);
    expect(TIER_LIMITS.team.monthly).toBe(50_000);
  });
  it("falls back to free for unknown tiers", () => {
    expect(getLimitsForTier("enterprise")).toEqual(TIER_LIMITS.free);
  });
});

describe("kv-counter: hourly sliding window", () => {
  it("bucket helpers produce ISO hour strings", () => {
    // Middle of the hour 2026-04-11T03:00:00Z
    const now = Date.UTC(2026, 3, 11, 3, 30, 0);
    expect(isoHourBucket(now)).toBe("2026-04-11T03");
    expect(previousIsoHourBucket(now)).toBe("2026-04-11T02");
  });

  it("allows requests up to the limit and rejects the next one", async () => {
    const kvImpl = kv.asKv();
    const hash = "abc";
    const now = Date.UTC(2026, 3, 11, 3, 30, 0);
    for (let i = 0; i < 5; i++) {
      const d = await checkAndIncrementHourly(kvImpl, hash, 5, now);
      expect(d.allowed).toBe(true);
    }
    const sixth = await checkAndIncrementHourly(kvImpl, hash, 5, now);
    expect(sixth.allowed).toBe(false);
    expect(sixth.remaining).toBe(0);
  });

  it("writes the hourly bucket key with the expected shape", async () => {
    const kvImpl = kv.asKv();
    const hash = "xyz";
    const now = Date.UTC(2026, 3, 11, 3, 30, 0);
    await checkAndIncrementHourly(kvImpl, hash, 100, now);
    const key = hourlyBucketKey(hash, "2026-04-11T03");
    expect(key).toBe(`${KV_HOURLY_PREFIX}xyz:h:2026-04-11T03`);
    const stored = await kvImpl.get(key);
    expect(stored).toBe("1");
  });

  it("weights the previous bucket by remaining time in the hour", async () => {
    const kvImpl = kv.asKv();
    const hash = "w";
    // Pre-seed the previous bucket with 100 requests.
    const now = Date.UTC(2026, 3, 11, 3, 30, 0);
    const prevKey = hourlyBucketKey(hash, previousIsoHourBucket(now));
    await kvImpl.put(prevKey, "100");
    // 30 minutes into the current hour: previous weight = 0.5 → effective 50.
    // limit 60 → still allowed, but only 10 more until cutoff.
    const d = await checkAndIncrementHourly(kvImpl, hash, 60, now);
    expect(d.allowed).toBe(true);
    expect(Math.round(d.effective)).toBe(51);
  });
});

describe("quotas: monthly", () => {
  it("quotaKey format", () => {
    expect(quotaKey("hash", "2026-04")).toBe("quota:hash:2026-04");
  });

  it("returns used count from D1 on first call and caches in KV", async () => {
    await insertUser(mock.db, { id: "usr_q", email: "q@example.com" });
    const { row } = await createApiKey(mock.db, "usr_q", "q-key");
    await insertUsageRecord(mock.db, {
      apiKeyId: row.id,
      userId: "usr_q",
      toolName: "list_samples",
      tierRequired: "free",
      status: "ok",
    });
    const bucket = monthBucket();
    const hash = row.key_hash;
    const used = await getMonthlyUsage(kv.asKv(), mock.db, row.id, hash, bucket);
    expect(used).toBe(1);
    // Cache entry should now exist.
    expect(kv.store.has(quotaKey(hash, bucket))).toBe(true);
  });

  it("checkMonthlyQuota allows under limit and denies over", async () => {
    await insertUser(mock.db, { id: "usr_q2", email: "q2@example.com" });
    const { row } = await createApiKey(mock.db, "usr_q2", "k");
    const bucket = monthBucket();
    // Pre-seed the cache with 3 of 5.
    await kv.put(quotaKey(row.key_hash, bucket), "3");
    const under = await checkMonthlyQuota(
      kv.asKv(),
      mock.db,
      row.id,
      row.key_hash,
      bucket,
      5,
    );
    expect(under.allowed).toBe(true);
    expect(under.remaining).toBe(2);

    await kv.put(quotaKey(row.key_hash, bucket), "5");
    const over = await checkMonthlyQuota(
      kv.asKv(),
      mock.db,
      row.id,
      row.key_hash,
      bucket,
      5,
    );
    expect(over.allowed).toBe(false);
  });

  it("incrementQuotaCache bumps the cached counter", async () => {
    await kv.put("quota:h:2026-04", "4");
    await incrementQuotaCache(kv.asKv(), "h", "2026-04");
    const stored = await kv.get("quota:h:2026-04");
    expect(stored).toBe("5");
  });
});

// ── End-to-end middleware test ────────────────────────────────────────────

type TestApp = Hono<{ Bindings: Env; Variables: AuthVariables }>;

function makeApp(): TestApp {
  const app = new Hono<{ Bindings: Env; Variables: AuthVariables }>();
  app.use("/mcp", authMiddleware(), rateLimitMiddleware());
  app.post("/mcp", (c) =>
    c.json({ ok: true, tier: c.get("auth").tier }),
  );
  return app;
}

function exec(app: TestApp, plaintext: string) {
  return app.request(
    "/mcp",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
        authorization: `Bearer ${plaintext}`,
      },
      body: "{}",
    },
    { DB: mock.db, RL_KV: kv.asKv(), ENVIRONMENT: "test" } as unknown as Env,
  );
}

async function seedFreeUserWithKey() {
  await insertUser(mock.db, { id: "usr_rl", email: "rl@example.com" });
  const { plaintext, row } = await createApiKey(mock.db, "usr_rl", "k");
  return { plaintext, row };
}

describe("rate-limit middleware end-to-end", () => {
  it("allows the first call and sets X-RateLimit headers", async () => {
    const { plaintext } = await seedFreeUserWithKey();
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(200);
    expect(res.headers.get("x-ratelimit-limit")).toBe("60");
    expect(res.headers.get("x-ratelimit-remaining")).toBeTruthy();
    expect(res.headers.get("x-ratelimit-reset")).toBeTruthy();
  });

  it("returns 429 JSON-RPC when the hourly limit is exceeded", async () => {
    const { plaintext, row } = await seedFreeUserWithKey();
    // Pre-seed the current hourly bucket with 60 (the free limit).
    const hash = row.key_hash;
    const cur = isoHourBucket();
    await kv.put(hourlyBucketKey(hash, cur), "60");
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(429);
    const body = (await res.json()) as {
      jsonrpc: string;
      error: {
        code: number;
        message: string;
        data: { tier: string; scope: string; limit: number };
      };
    };
    expect(body.error.code).toBe(-32002);
    expect(body.error.data.tier).toBe("free");
    expect(body.error.data.scope).toBe("hourly");
  });

  it("returns 429 when the monthly quota is exhausted", async () => {
    const { plaintext, row } = await seedFreeUserWithKey();
    const bucket = monthBucket();
    await kv.put(quotaKey(row.key_hash, bucket), "1000");
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(429);
    const body = (await res.json()) as {
      error: { data: { scope: string } };
    };
    expect(body.error.data.scope).toBe("monthly");
  });

  it("applies the higher pro limits when the user is on the pro tier", async () => {
    await insertUser(mock.db, { id: "usr_pro", email: "pro@example.com" });
    await updateUserTier(mock.db, "usr_pro", "pro");
    const { plaintext } = await createApiKey(mock.db, "usr_pro", "pro-k");
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(200);
    expect(res.headers.get("x-ratelimit-limit")).toBe("600");
  });
});
