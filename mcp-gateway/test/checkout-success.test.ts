/**
 * Tests for `GET /checkout/success?session_id=cs_...`.
 *
 * Covers the single-use KV handoff: seed a plaintext API key in KV
 * under `checkout_key:{session_id}`, GET the route, and verify that
 * the key is rendered once and removed from KV immediately after.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { Env } from "../src/env.js";
import { checkoutSuccessRoutes } from "../src/routes/checkout-success.js";
import { CHECKOUT_KEY_KV_PREFIX } from "../src/billing/events/checkout-completed.js";
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

function env(overrides: Partial<Env> = {}): Env {
  return {
    DB: mock.db,
    RL_KV: kv.asKv(),
    ENVIRONMENT: "test",
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
    ...overrides,
  } as Env;
}

function makeApp() {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", checkoutSuccessRoutes());
  return app;
}

async function seedKv(sessionId: string, payload: Record<string, unknown>) {
  await kv.put(
    `${CHECKOUT_KEY_KV_PREFIX}${sessionId}`,
    JSON.stringify(payload),
    { expirationTtl: 86_400 },
  );
}

describe("GET /checkout/success", () => {
  it("renders the API key once and wipes it from KV", async () => {
    const sessionId = "cs_test_happy";
    const plaintext = "sv_live_ABCDEFGHIJKLMNOPQRSTUVWXYZ23456789";
    await seedKv(sessionId, {
      plaintext,
      prefix: "sv_live_ABCDEF",
      name: "Checkout",
      tier: "pro",
      email: "buyer@example.com",
      createdAt: Date.now(),
    });

    const app = makeApp();
    const res = await app.request(
      `/checkout/success?session_id=${sessionId}`,
      {},
      env(),
    );
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toMatch(/text\/html/);
    const body = await res.text();
    expect(body).toContain("<!doctype html>");
    expect(body).toContain(plaintext);
    expect(body).toContain("buyer@example.com");
    expect(body).toMatch(/Welcome to SceneView MCP Pro/);
    // Install snippets bake the key into both Claude Desktop and Cursor
    // configs. Hono JSX HTML-escapes the quotes so we match on the
    // escaped form.
    expect(body).toContain("&quot;sceneview&quot;");
    expect(body).toContain("claude_desktop_config.json");

    // The KV entry has been consumed — a refresh must not reveal it again.
    const stillThere = await kv.get(`${CHECKOUT_KEY_KV_PREFIX}${sessionId}`);
    expect(stillThere).toBeNull();
  });

  it("shows the 'already consumed' page on the second request", async () => {
    const sessionId = "cs_test_replay";
    await seedKv(sessionId, {
      plaintext: "sv_live_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
      prefix: "sv_live_XXXXXX",
      name: "Checkout",
      tier: "team",
      email: "team@example.com",
      createdAt: Date.now(),
    });
    const app = makeApp();
    const first = await app.request(
      `/checkout/success?session_id=${sessionId}`,
      {},
      env(),
    );
    expect(first.status).toBe(200);
    const second = await app.request(
      `/checkout/success?session_id=${sessionId}`,
      {},
      env(),
    );
    expect(second.status).toBe(410);
    const body = await second.text();
    expect(body).toMatch(/already been used/);
    expect(body).toContain(sessionId);
  });

  it("returns 410 when the session_id is unknown", async () => {
    const app = makeApp();
    const res = await app.request(
      "/checkout/success?session_id=cs_unknown",
      {},
      env(),
    );
    expect(res.status).toBe(410);
    const body = await res.text();
    expect(body).toMatch(/already been used/);
  });

  it("returns 400 when the session_id query parameter is missing", async () => {
    const app = makeApp();
    const res = await app.request("/checkout/success", {}, env());
    expect(res.status).toBe(400);
  });

  it("returns 400 when the session_id does not start with cs_", async () => {
    const app = makeApp();
    const res = await app.request(
      "/checkout/success?session_id=invalid",
      {},
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("handles a Team tier entry with the Team welcome headline", async () => {
    const sessionId = "cs_test_team";
    await seedKv(sessionId, {
      plaintext: "sv_live_YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY",
      prefix: "sv_live_YYYYYY",
      name: "Checkout",
      tier: "team",
      email: "teambuyer@example.com",
      createdAt: Date.now(),
    });
    const app = makeApp();
    const res = await app.request(
      `/checkout/success?session_id=${sessionId}`,
      {},
      env(),
    );
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toMatch(/Welcome to SceneView MCP Team/);
  });

  it("falls through to the consumed page when KV holds invalid JSON", async () => {
    const sessionId = "cs_test_corrupt";
    await kv.put(`${CHECKOUT_KEY_KV_PREFIX}${sessionId}`, "not json", {
      expirationTtl: 86_400,
    });
    const app = makeApp();
    const res = await app.request(
      `/checkout/success?session_id=${sessionId}`,
      {},
      env(),
    );
    expect(res.status).toBe(410);
    // And the bad entry is wiped so a retry does not loop forever.
    const still = await kv.get(`${CHECKOUT_KEY_KV_PREFIX}${sessionId}`);
    expect(still).toBeNull();
  });
});
