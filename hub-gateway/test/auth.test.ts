/**
 * Auth middleware tests: header extraction, D1 validation, KV cache
 * roundtrip, and 401 paths.
 *
 * Uses the in-memory FakeD1 + FakeKV helpers — no Miniflare.
 */

import { describe, it, expect, beforeEach } from "vitest";
import app from "../src/index.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { JSON_RPC_ERRORS } from "../src/mcp/transport.js";
import { makeEnv, FakeD1, FakeKV } from "./helpers/fake-bindings.js";

const VALID_KEY = "sv_live_AUTHKEYTEST00000000000000000000";
const REVOKED_KEY = "sv_live_REVOKEDKEY00000000000000000000";
const ORPHAN_KEY = "sv_live_ORPHANKEY0000000000000000000000";

interface Ctx {
  env: ReturnType<typeof makeEnv>["env"];
  db: FakeD1;
  kv: FakeKV;
}

let ctx: Ctx;

beforeEach(async () => {
  const validHash = await hashApiKey(VALID_KEY);
  const revokedHash = await hashApiKey(REVOKED_KEY);
  const orphanHash = await hashApiKey(ORPHAN_KEY);

  ctx = makeEnv({
    api_keys: [
      {
        id: "key_valid",
        user_id: "usr_valid",
        key_hash: validHash,
        key_prefix: "sv_live_AUTHKE",
        revoked_at: null,
      },
      {
        id: "key_revoked",
        user_id: "usr_valid",
        key_hash: revokedHash,
        key_prefix: "sv_live_REVOKE",
        revoked_at: Date.now() - 1000,
      },
      {
        id: "key_orphan",
        user_id: "usr_missing",
        key_hash: orphanHash,
        key_prefix: "sv_live_ORPHAN",
        revoked_at: null,
      },
    ],
    users: [{ id: "usr_valid", email: "valid@hub.test", tier: "pro" }],
  });
});

function rpc(headers: Record<string, string>, method = "tools/list"): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: { "content-type": "application/json", ...headers },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method }),
  });
}

describe("authMiddleware", () => {
  it("returns 401 when no Authorization header is present", async () => {
    const res = await app.request(rpc({}), {}, ctx.env);
    expect(res.status).toBe(401);
    const body = (await res.json()) as {
      error: { code: number; message: string; data?: { detail?: string } };
    };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.UNAUTHORIZED);
    expect(body.error.message).toBe("Unauthorized");
    expect(body.error.data?.detail).toBe("Missing API key");
  });

  it("returns 401 when the Bearer token is unknown to D1", async () => {
    const res = await app.request(
      rpc({ authorization: "Bearer sv_live_DOESNOTEXIST000000000000000000" }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(401);
    const body = (await res.json()) as {
      error: { data?: { detail?: string } };
    };
    expect(body.error.data?.detail).toBe("Invalid or revoked API key");
  });

  it("returns 401 when the key has been revoked", async () => {
    const res = await app.request(
      rpc({ authorization: `Bearer ${REVOKED_KEY}` }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(401);
  });

  it("returns 401 when the owning user no longer exists", async () => {
    const res = await app.request(
      rpc({ authorization: `Bearer ${ORPHAN_KEY}` }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(401);
  });

  it("returns 401 when the token does not start with sv_live_", async () => {
    const res = await app.request(
      rpc({ authorization: "Bearer pk_test_somethingelse" }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(401);
  });

  it("accepts a valid Bearer token and reaches the dispatch layer", async () => {
    const res = await app.request(
      rpc({ authorization: `Bearer ${VALID_KEY}` }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      result: { tools: Array<{ name: string }> };
    };
    expect(body.result.tools.length).toBeGreaterThan(0);
  });

  it("accepts a key passed via the ?key= query param fallback", async () => {
    const req = new Request(
      `https://hub-mcp.test/mcp?key=${VALID_KEY}`,
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }),
      },
    );
    const res = await app.request(req, {}, ctx.env);
    expect(res.status).toBe(200);
  });

  it("warms the KV cache on a D1 hit and reuses it on a second call", async () => {
    expect(ctx.kv.size).toBe(0);

    const res1 = await app.request(
      rpc({ authorization: `Bearer ${VALID_KEY}` }),
      {},
      ctx.env,
    );
    expect(res1.status).toBe(200);
    // Expected KV entries after one valid tools/list request:
    //   1. hub-auth:{hash}         — auth cache entry
    //   2. hub-quota:{hash}:{mon}  — monthly quota cache (warm from D1)
    //   3. hub-rl:{hash}:h:...     — hourly rate-limit counter
    // tools/list itself is NOT a tool call and doesn't trigger
    // usage_records insertion or quota increment; only the 3
    // middleware-side writes land here.
    expect(ctx.kv.size).toBe(3);

    // Revoke the key directly in D1. If the middleware is hitting the
    // cache, the next request still succeeds — that's the 5 min TTL
    // revocation trade-off documented in the module header.
    ctx.db.upsert("api_keys", {
      id: "key_valid",
      user_id: "usr_valid",
      key_hash: await hashApiKey(VALID_KEY),
      key_prefix: "sv_live_AUTHKE",
      revoked_at: Date.now(),
    });

    const res2 = await app.request(
      rpc({ authorization: `Bearer ${VALID_KEY}` }),
      {},
      ctx.env,
    );
    expect(res2.status).toBe(200);
  });

  it("does not warm the KV cache when the key is invalid", async () => {
    expect(ctx.kv.size).toBe(0);
    const res = await app.request(
      rpc({ authorization: "Bearer sv_live_DOESNOTEXIST000000000000000000" }),
      {},
      ctx.env,
    );
    expect(res.status).toBe(401);
    // Auth failed at step 1 — neither the auth cache nor the
    // rate-limit counter should have been written.
    expect(ctx.kv.size).toBe(0);
  });
});
