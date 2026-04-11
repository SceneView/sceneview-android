/**
 * Tests for the dashboard HTML routes and HTMX fragment endpoints.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { dashboardRoutes } from "../src/routes/dashboard.js";
import { authRoutes } from "../src/routes/auth.js";
import { renderLoginPage } from "../src/dashboard/login-page.js";
import type { Env } from "../src/env.js";
import { insertUser } from "../src/db/users.js";
import { createApiKey } from "../src/auth/api-keys.js";
import { signJwt } from "../src/auth/jwt.js";
import { SESSION_COOKIE } from "../src/auth/session-middleware.js";
import { insertUsageRecord, monthBucket } from "../src/db/usage.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

const SECRET = "test-secret-32-chars-for-hs256-ok";

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
    JWT_SECRET: SECRET,
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
  } as Env;
}

function makeFullApp() {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", dashboardRoutes());
  app.route("/", authRoutes({ renderLoginPage }));
  return app;
}

async function seedUser(
  id = "usr_dashtest",
  email = "dash@example.com",
  tier: "free" | "pro" | "team" = "pro",
) {
  await insertUser(mock.db, { id, email, tier });
  const token = await signJwt(SECRET, { sub: id });
  return { id, token };
}

describe("GET public pages", () => {
  it("GET / renders the landing page", async () => {
    const app = makeFullApp();
    const res = await app.request("/", {}, env());
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toMatch(/text\/html/);
    const body = await res.text();
    expect(body).toContain("<!doctype html>");
    expect(body).toContain("SceneView MCP");
    expect(body).toMatch(/Expert 3D and AR/);
    // Unauthenticated visitor sees "Sign in", not "Dashboard".
    expect(body).toContain("Sign in");
  });

  it("GET / shows Dashboard link when authenticated", async () => {
    const { token } = await seedUser();
    const app = makeFullApp();
    const res = await app.request(
      "/",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      env(),
    );
    const body = await res.text();
    expect(body).toContain("Dashboard");
    expect(body).not.toContain('href="/login"');
  });

  it("GET /pricing renders all three tiers", async () => {
    const app = makeFullApp();
    const res = await app.request("/pricing", {}, env());
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toMatch(/Free/);
    expect(body).toMatch(/Pro/);
    expect(body).toMatch(/Team/);
    expect(body).toMatch(/19 EUR/);
    expect(body).toMatch(/49 EUR/);
  });

  it("GET /docs shows install instructions", async () => {
    const app = makeFullApp();
    const res = await app.request("/docs", {}, env());
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toMatch(/Claude Desktop/);
    expect(body).toMatch(/Cursor/);
    expect(body).toContain("sceneview-mcp.workers.dev/mcp");
    expect(body).toContain("sv_live_");
    // Never leak an sk_live_ prefix — that is Stripe's.
    expect(body).not.toContain("sk_live_");
  });

  it("GET /login renders the email form", async () => {
    const app = makeFullApp();
    const res = await app.request("/login", {}, env());
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toMatch(/<form[^>]+method="post"[^>]+action="\/login"/);
    expect(body).toMatch(/type="email"/);
  });
});

describe("authenticated dashboard pages", () => {
  it("GET /dashboard redirects to /login when unauthenticated", async () => {
    const app = makeFullApp();
    const res = await app.request("/dashboard", {}, env());
    expect(res.status).toBe(302);
    expect(res.headers.get("location")).toMatch(/^\/login\?next=/);
  });

  it("GET /dashboard renders user data + empty usage on a fresh account", async () => {
    const { id, token } = await seedUser();
    const app = makeFullApp();
    const res = await app.request(
      "/dashboard",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      env(),
    );
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toContain("dash@example.com");
    expect(body).toMatch(/No keys yet/);
    expect(body).toMatch(/No usage yet/);
    // Sign out form is visible.
    expect(body).toContain('action="/auth/logout"');
    // Monthly quota number shows 0 out of the Pro monthly limit (10000).
    expect(body).toContain("10000");
    expect(id).toBe("usr_dashtest");
  });

  it("GET /dashboard lists existing API keys", async () => {
    const { token } = await seedUser();
    await createApiKey(mock.db, "usr_dashtest", "alpha");
    const app = makeFullApp();
    const res = await app.request(
      "/dashboard",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      env(),
    );
    const body = await res.text();
    expect(body).toMatch(/alpha/);
    expect(body).toContain("sv_live_");
    expect(body).toMatch(/Revoke/);
  });

  it("GET /dashboard shows the usage sparkline when records exist", async () => {
    const { token } = await seedUser();
    const { row } = await createApiKey(mock.db, "usr_dashtest", "beta");
    await insertUsageRecord(mock.db, {
      apiKeyId: row.id,
      userId: "usr_dashtest",
      toolName: "list_samples",
      tierRequired: "free",
      status: "ok",
      bucketMonth: monthBucket(),
    });
    const app = makeFullApp();
    const res = await app.request(
      "/dashboard",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      env(),
    );
    const body = await res.text();
    expect(body).toMatch(/Last 30 days/);
    // Should render an SVG sparkline instead of the empty state.
    expect(body).toContain("<svg");
    expect(body).not.toMatch(/No usage yet/);
  });

  it("GET /billing redirects unauthenticated visitors", async () => {
    const app = makeFullApp();
    const res = await app.request("/billing", {}, env());
    expect(res.status).toBe(302);
    expect(res.headers.get("location")).toMatch(/^\/login/);
  });

  it("GET /billing shows upgrade CTA for free users", async () => {
    await insertUser(mock.db, {
      id: "usr_free",
      email: "free@example.com",
      tier: "free",
    });
    const token = await signJwt(SECRET, { sub: "usr_free" });
    const app = makeFullApp();
    const res = await app.request(
      "/billing",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      env(),
    );
    const body = await res.text();
    expect(body).toMatch(/Upgrade to Pro/);
    expect(body).toContain('action="/billing/checkout"');
  });
});

describe("HTMX fragments", () => {
  it("POST /dashboard/keys creates a key and returns the plaintext fragment", async () => {
    const { token } = await seedUser();
    const app = makeFullApp();
    const form = new URLSearchParams({ name: "ci-key" });
    const res = await app.request(
      "/dashboard/keys",
      {
        method: "POST",
        headers: {
          cookie: `${SESSION_COOKIE}=${token}`,
          "content-type": "application/x-www-form-urlencoded",
        },
        body: form.toString(),
      },
      env(),
    );
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toContain("sv_live_");
    expect(body).toMatch(/ci-key/);
    // Fragment response — no <!doctype>.
    expect(body).not.toContain("<!doctype html>");
    // Must not leak the stripe prefix accidentally.
    expect(body).not.toContain("sk_live_");
  });

  it("POST /dashboard/keys/:id/revoke marks the key revoked and returns the row", async () => {
    const { token } = await seedUser();
    const { row } = await createApiKey(mock.db, "usr_dashtest", "to-revoke");
    const app = makeFullApp();
    const res = await app.request(
      `/dashboard/keys/${row.id}/revoke`,
      {
        method: "POST",
        headers: { cookie: `${SESSION_COOKIE}=${token}` },
      },
      env(),
    );
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toMatch(/revoked/);
    // Fragment response — no <!doctype>.
    expect(body).not.toContain("<!doctype html>");
  });

  it("HTMX endpoints refuse to act on keys owned by someone else", async () => {
    const { token } = await seedUser();
    await insertUser(mock.db, { id: "usr_other", email: "other@example.com" });
    const { row } = await createApiKey(mock.db, "usr_other", "not-mine");
    const app = makeFullApp();
    const res = await app.request(
      `/dashboard/keys/${row.id}/revoke`,
      {
        method: "POST",
        headers: { cookie: `${SESSION_COOKIE}=${token}` },
      },
      env(),
    );
    // Revoke is a no-op (rowcount 0) and we 404 on the lookup for
    // someone else's row.
    expect(res.status).toBe(404);
  });

  it("HTMX endpoints require a session", async () => {
    const app = makeFullApp();
    const res = await app.request(
      "/dashboard/keys",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "name=x",
      },
      env(),
    );
    expect(res.status).toBe(303);
  });
});
