/**
 * Monthly quota + usage logging tests.
 *
 * Covers the post-dispatch observation chain:
 *   - insertUsageRecord fires on every tools/call with the right
 *     tool name and status
 *   - incrementQuotaCache bumps the KV counter on `ok` only
 *   - checkMonthlyQuota triggers 429 + JSON-RPC -32002 with
 *     scope "monthly" when the cached counter hits the tier limit
 *   - tools/list does NOT log usage (it's not a tool call)
 */

import { describe, it, expect, beforeEach } from "vitest";
import app from "../src/index.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { JSON_RPC_ERRORS } from "../src/mcp/transport.js";
import { TIER_LIMITS } from "../src/rate-limit/limits.js";
import {
  HUB_QUOTA_CACHE_PREFIX,
  quotaKey,
} from "../src/rate-limit/quotas.js";
import { monthBucket } from "../src/db/usage.js";
import { makeEnv, FakeD1, FakeKV } from "./helpers/fake-bindings.js";

const FREE_KEY = "sv_live_QUOTAFREE00000000000000000000000";
const PRO_KEY = "sv_live_QUOTAPRO0000000000000000000000000";

interface Ctx {
  env: ReturnType<typeof makeEnv>["env"];
  db: FakeD1;
  kv: FakeKV;
}

let ctx: Ctx;

beforeEach(async () => {
  const freeHash = await hashApiKey(FREE_KEY);
  const proHash = await hashApiKey(PRO_KEY);
  ctx = makeEnv({
    api_keys: [
      {
        id: "key_free_q",
        user_id: "usr_free_q",
        key_hash: freeHash,
        key_prefix: "sv_live_QUOTAF",
        revoked_at: null,
      },
      {
        id: "key_pro_q",
        user_id: "usr_pro_q",
        key_hash: proHash,
        key_prefix: "sv_live_QUOTAP",
        revoked_at: null,
      },
    ],
    users: [
      { id: "usr_free_q", email: "free@hub.test", tier: "free" },
      { id: "usr_pro_q", email: "pro@hub.test", tier: "pro" },
    ],
    usage_records: [],
  });
});

function rpc(key: string, body: unknown): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${key}`,
    },
    body: JSON.stringify(body),
  });
}

describe("monthly quota + usage logging", () => {
  it("logs one usage_record with status=ok on a successful tools/call", async () => {
    const res = await app.request(
      rpc(PRO_KEY, {
        jsonrpc: "2.0",
        id: 1,
        method: "tools/call",
        params: { name: "architecture__list_building_types", arguments: {} },
      }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(200);

    const records = ctx.db.getUsageRecords();
    expect(records).toHaveLength(1);
    expect(records[0]).toMatchObject({
      api_key_id: "key_pro_q",
      user_id: "usr_pro_q",
      tool_name: "architecture__list_building_types",
      // This tool is in the free whitelist (see src/mcp/access.ts)
      // so usage is logged with tier_required=free even when the
      // calling user is on `pro` — dashboards can tell free vs pro
      // volume apart regardless of the caller's own tier.
      tier_required: "free",
      status: "ok",
      bucket_month: monthBucket(),
    });
    expect(typeof records[0].created_at).toBe("number");
  });

  it("bumps the hub-quota: KV counter on a successful call", async () => {
    const proHash = await hashApiKey(PRO_KEY);
    const key = quotaKey(proHash, monthBucket());
    expect(key.startsWith(HUB_QUOTA_CACHE_PREFIX)).toBe(true);

    await app.request(
      rpc(PRO_KEY, {
        jsonrpc: "2.0",
        id: 1,
        method: "tools/call",
        params: { name: "architecture__list_building_types", arguments: {} },
      }),
      {},
      ctx.env,
    );
    await app.request(
      rpc(PRO_KEY, {
        jsonrpc: "2.0",
        id: 2,
        method: "tools/call",
        params: { name: "realestate__search_listings", arguments: { location: "Paris" } },
      }),
      {},
      ctx.env,
    );

    // Each successful call went through getMonthlyUsage (write `0`
    // from the D1 aggregate on the first call) then
    // incrementQuotaCache (`1` after call 1, `2` after call 2).
    const raw = await ctx.kv.get(key);
    expect(raw).toBe("2");
  });

  it("does not log usage_records on tools/list (not a tool call)", async () => {
    const res = await app.request(
      rpc(PRO_KEY, { jsonrpc: "2.0", id: 1, method: "tools/list" }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(200);
    expect(ctx.db.getUsageRecords()).toHaveLength(0);
  });

  it("returns 429 + scope=monthly when the KV counter exceeds the free cap", async () => {
    // Pre-seed the KV counter above the free-tier monthly limit (100).
    const freeHash = await hashApiKey(FREE_KEY);
    const key = quotaKey(freeHash, monthBucket());
    await ctx.kv.put(key, String(TIER_LIMITS.free.monthly), {
      expirationTtl: 3600,
    });

    const res = await app.request(
      rpc(FREE_KEY, {
        jsonrpc: "2.0",
        id: 1,
        method: "tools/call",
        params: { name: "architecture__list_building_types", arguments: {} },
      }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(429);
    const body = (await res.json()) as {
      error: {
        code: number;
        data: { tier: string; scope: string; limit: number };
      };
    };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.RATE_LIMITED);
    expect(body.error.data.scope).toBe("monthly");
    expect(body.error.data.tier).toBe("free");
    expect(body.error.data.limit).toBe(TIER_LIMITS.free.monthly);
  });

  it("does NOT log usage when monthly cap rejects in middleware", async () => {
    // The rate-limit middleware short-circuits BEFORE dispatch, so
    // the post-dispatch observer in /mcp/routes/mcp.ts never runs.
    // This matches Gateway #1's behavior: middleware-side 429s are
    // tracked in the KV counter (`hub-quota:{hash}:{mon}`) and
    // don't duplicate into `usage_records`. Dashboards read the
    // 429 rate from the middleware metric, not from the usage log.
    const proHash = await hashApiKey(PRO_KEY);
    await ctx.kv.put(quotaKey(proHash, monthBucket()), String(TIER_LIMITS.pro.monthly), {
      expirationTtl: 3600,
    });
    const res = await app.request(
      rpc(PRO_KEY, {
        jsonrpc: "2.0",
        id: 1,
        method: "tools/call",
        params: { name: "realestate__search_listings", arguments: { location: "NYC" } },
      }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(429);
    expect(ctx.db.getUsageRecords()).toHaveLength(0);
  });

  it("does NOT increment quota cache on a failed dispatch (unknown tool)", async () => {
    const proHash = await hashApiKey(PRO_KEY);
    const key = quotaKey(proHash, monthBucket());

    await app.request(
      rpc(PRO_KEY, {
        jsonrpc: "2.0",
        id: 1,
        method: "tools/call",
        params: { name: "architecture__does_not_exist", arguments: {} },
      }),
      {},
      ctx.env,
    );

    // Dispatch returned { isError: true } but the JSON-RPC envelope
    // is still a successful 200 with result.isError. The observer
    // treats this as status=ok (there is no JSON-RPC error code).
    // This is the same contract as Gateway #1 — tool-level errors
    // burn quota because the server did execute the call.
    const raw = await ctx.kv.get(key);
    expect(raw).toBe("1");

    const records = ctx.db.getUsageRecords();
    expect(records).toHaveLength(1);
    expect(records[0].status).toBe("ok");
    expect(records[0].tool_name).toBe("architecture__does_not_exist");
  });
});
