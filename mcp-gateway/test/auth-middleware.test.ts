/**
 * Tests for the Hono auth middleware (`src/auth/middleware.ts`).
 *
 * The middleware is exercised by mounting it on a tiny throwaway Hono
 * app. D1 is backed by the in-memory mock from step 6 and KV by the
 * in-memory mock from step 5.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  authMiddleware,
  AUTH_CACHE_TTL_SECONDS,
  __testing,
  type AuthVariables,
} from "../src/auth/middleware.js";
import { createApiKey } from "../src/auth/api-keys.js";
import { insertUser, updateUserTier } from "../src/db/users.js";
import { revokeApiKeyRow } from "../src/db/api-keys.js";
import type { Env } from "../src/env.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

const { AUTH_CACHE_PREFIX } = __testing;

let mock: MockD1;
let kv: MockKv;

beforeEach(async () => {
  mock = await createMockD1();
  kv = new MockKv();
});
afterEach(() => {
  mock.close();
});

/** Builds a tiny Hono app with one protected `/mcp` endpoint. */
function makeApp() {
  const app = new Hono<{ Bindings: Env; Variables: AuthVariables }>();
  app.use("/mcp", authMiddleware());
  app.post("/mcp", (c) => {
    const auth = c.get("auth");
    return c.json({
      ok: true,
      userId: auth.userId,
      tier: auth.tier,
      keyId: auth.keyId,
    });
  });
  return app;
}

async function seedPlusKey(tier: "free" | "pro" | "team" = "free") {
  await insertUser(mock.db, { id: "usr_m1", email: "m1@example.com" });
  if (tier !== "free") await updateUserTier(mock.db, "usr_m1", tier);
  const { row, plaintext } = await createApiKey(
    mock.db,
    "usr_m1",
    "middleware-test",
  );
  return { row, plaintext };
}


// Hono's `app.request(url, init, env)` overload is the only way to
// inject Workers bindings into tests, so every case below goes through
// this helper.

type TestApp = Hono<{ Bindings: Env; Variables: AuthVariables }>;

function exec(
  app: TestApp,
  plaintextOrOpts: string | { query?: string; header?: string },
) {
  const headers: Record<string, string> = {
    "content-type": "application/json",
  };
  let url = "/mcp";
  if (typeof plaintextOrOpts === "string") {
    headers["authorization"] = `Bearer ${plaintextOrOpts}`;
  } else {
    if (plaintextOrOpts.header) {
      headers["authorization"] = `Bearer ${plaintextOrOpts.header}`;
    }
    if (plaintextOrOpts.query) {
      url += `?key=${encodeURIComponent(plaintextOrOpts.query)}`;
    }
  }
  return app.request(
    url,
    { method: "POST", headers, body: "{}" },
    { DB: mock.db, RL_KV: kv.asKv(), ENVIRONMENT: "test" } as unknown as Env,
  );
}

describe("auth middleware (real env)", () => {
  it("accepts a valid bearer token", async () => {
    const { plaintext, row } = await seedPlusKey("pro");
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      ok: boolean;
      userId: string;
      tier: string;
      keyId: string;
    };
    expect(body.ok).toBe(true);
    expect(body.userId).toBe("usr_m1");
    expect(body.tier).toBe("pro");
    expect(body.keyId).toBe(row.id);
  });

  it("accepts a valid key via ?key= query parameter", async () => {
    const { plaintext } = await seedPlusKey();
    const app = makeApp();
    const res = await exec(app, { query: plaintext });
    expect(res.status).toBe(200);
  });

  it("returns 401 JSON-RPC when no credentials are present", async () => {
    const app = makeApp();
    const res = await exec(app, { });
    expect(res.status).toBe(401);
    const body = (await res.json()) as {
      jsonrpc: string;
      error: { code: number; message: string };
    };
    expect(body.jsonrpc).toBe("2.0");
    expect(body.error.code).toBe(-32001);
    expect(body.error.message).toBe("Unauthorized");
  });

  it("returns 401 for an unknown key", async () => {
    await seedPlusKey();
    const app = makeApp();
    const res = await exec(
      app,
      "sv_live_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    );
    expect(res.status).toBe(401);
  });

  it("returns 401 for a revoked key", async () => {
    const { plaintext, row } = await seedPlusKey();
    await revokeApiKeyRow(mock.db, row.id, "usr_m1");
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(401);
  });

  it("writes the auth entry to KV on first hit and reads it on second hit", async () => {
    const { plaintext } = await seedPlusKey("pro");
    const app = makeApp();

    const r1 = await exec(app, plaintext);
    expect(r1.status).toBe(200);
    // One KV entry under auth:{hash}.
    const writtenKeys = [...kv.store.keys()].filter((k) =>
      k.startsWith(AUTH_CACHE_PREFIX),
    );
    expect(writtenKeys).toHaveLength(1);

    // Second hit — wipe the underlying rows to prove the cache is used.
    // If the middleware went back to D1 we would now get a 401.
    // Disable FK enforcement for the teardown so we can delete both
    // tables in any order.
    mock.sqlite.pragma("foreign_keys = OFF");
    mock.sqlite.exec("DELETE FROM api_keys");
    mock.sqlite.exec("DELETE FROM users");
    mock.sqlite.pragma("foreign_keys = ON");
    const r2 = await exec(app, plaintext);
    expect(r2.status).toBe(200);
  });

  it("honors a pre-populated cache entry without consulting D1", async () => {
    const { plaintext } = await seedPlusKey();
    // Put a hand-crafted entry for an arbitrary tier.
    const hash = await (
      await import("../src/auth/api-keys.js")
    ).hashApiKey(plaintext);
    await kv.put(
      `auth:${hash}`,
      JSON.stringify({
        keyId: "key_cached",
        keyPrefix: "sv_live_cached",
        userId: "usr_m1",
        tier: "team",
      }),
      { expirationTtl: AUTH_CACHE_TTL_SECONDS },
    );
    const app = makeApp();
    const res = await exec(app, plaintext);
    expect(res.status).toBe(200);
    const body = (await res.json()) as { tier: string; keyId: string };
    expect(body.tier).toBe("team");
    expect(body.keyId).toBe("key_cached");
  });
});
