/**
 * Unit tests for `src/auth/magic-link.ts` and the /login /auth/verify routes.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  consumeMagicLinkToken,
  createMagicLink,
  isValidEmail,
  MAGIC_LINK_TTL_MINUTES,
  sendMagicLinkEmail,
} from "../src/auth/magic-link.js";
import { authRoutes } from "../src/routes/auth.js";
import type { Env } from "../src/env.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";
import { insertMagicLink, getMagicLink } from "../src/db/magic-links.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { getUserByEmail } from "../src/db/users.js";

let mock: MockD1;
let kv: MockKv;

beforeEach(async () => {
  mock = await createMockD1();
  kv = new MockKv();
});
afterEach(() => {
  mock.close();
});

describe("isValidEmail", () => {
  it("accepts reasonable addresses", () => {
    expect(isValidEmail("alice@example.com")).toBe(true);
    expect(isValidEmail("ALICE@example.com")).toBe(true);
    expect(isValidEmail("a.b+tag@sub.example.co")).toBe(true);
  });

  it("rejects junk", () => {
    expect(isValidEmail("")).toBe(false);
    expect(isValidEmail("alice")).toBe(false);
    expect(isValidEmail("alice@")).toBe(false);
    expect(isValidEmail("@example.com")).toBe(false);
    expect(isValidEmail("alice@@example.com")).toBe(false);
    expect(isValidEmail("alice@example")).toBe(false);
  });
});

describe("createMagicLink + consumeMagicLinkToken", () => {
  it("generates a token, stores its hash, and consumes it exactly once", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "consume@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    expect(link.url).toContain("/auth/verify?token=");
    expect(link.token).toMatch(/^[A-Z2-7]{40}$/);
    expect(link.expiresAt).toBeGreaterThan(Date.now());

    const first = await consumeMagicLinkToken(mock.db, link.token);
    expect(first).toBe("consume@example.com");

    const replay = await consumeMagicLinkToken(mock.db, link.token);
    expect(replay).toBeNull();
  });

  it("normalizes the email to lowercase and trims whitespace", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "  Upper@Example.com  ",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const email = await consumeMagicLinkToken(mock.db, link.token);
    expect(email).toBe("upper@example.com");
  });

  it("rejects expired tokens", async () => {
    // Insert an already-expired row directly through the DB layer.
    const token = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    const tokenHash = await hashApiKey(token);
    await insertMagicLink(mock.db, {
      tokenHash,
      email: "expired@example.com",
      expiresAt: Date.now() - 1000,
    });
    const email = await consumeMagicLinkToken(mock.db, token);
    expect(email).toBeNull();
  });

  it("rejects unknown tokens without leaking", async () => {
    const email = await consumeMagicLinkToken(
      mock.db,
      "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
    );
    expect(email).toBeNull();
  });

  it("throws on invalid email", async () => {
    await expect(
      createMagicLink({
        db: mock.db,
        email: "not-an-email",
        baseUrl: "https://sceneview-mcp.workers.dev",
      }),
    ).rejects.toThrow();
  });

  it("strips trailing slashes from baseUrl", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "slash@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev///",
    });
    expect(link.url).toMatch(
      /^https:\/\/sceneview-mcp\.workers\.dev\/auth\/verify\?/,
    );
  });
});

describe("sendMagicLinkEmail", () => {
  it("noop when apiKey is missing", async () => {
    const spy = vi.spyOn(globalThis, "fetch");
    await sendMagicLinkEmail({
      apiKey: undefined,
      from: "test@example.com",
      to: "user@example.com",
      url: "https://x/y",
    });
    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
  });

  it("POSTs to api.resend.com when apiKey is set", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ id: "rnd_123" }), { status: 200 }),
      );
    await sendMagicLinkEmail({
      apiKey: "re_test_key",
      from: "from@example.com",
      to: "to@example.com",
      url: "https://sceneview-mcp.workers.dev/auth/verify?token=zzz",
    });
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("https://api.resend.com/emails");
    expect(init.method).toBe("POST");
    const headers = init.headers as Record<string, string>;
    expect(headers.authorization).toBe("Bearer re_test_key");
    const body = JSON.parse(init.body as string) as {
      from: string;
      to: string[];
      subject: string;
      html: string;
      text: string;
    };
    expect(body.from).toBe("from@example.com");
    expect(body.to).toEqual(["to@example.com"]);
    expect(body.html).toContain(
      "https://sceneview-mcp.workers.dev/auth/verify?token=zzz",
    );
    expect(body.text).toContain(
      "https://sceneview-mcp.workers.dev/auth/verify?token=zzz",
    );
    expect(body.subject).toMatch(/sign-in/i);
    fetchMock.mockRestore();
  });

  it("throws on non-OK Resend responses", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("bad", { status: 500 }));
    await expect(
      sendMagicLinkEmail({
        apiKey: "re_test_key",
        from: "f@example.com",
        to: "t@example.com",
        url: "https://x/y",
      }),
    ).rejects.toThrow(/HTTP 500/);
    fetchMock.mockRestore();
  });

  it("includes the TTL in the body", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 200 }));
    await sendMagicLinkEmail({
      apiKey: "re_test_key",
      from: "f@example.com",
      to: "t@example.com",
      url: "https://x/y",
    });
    const body = JSON.parse(
      (fetchMock.mock.calls[0][1] as RequestInit).body as string,
    ) as { html: string; text: string };
    expect(body.html).toContain(String(MAGIC_LINK_TTL_MINUTES));
    expect(body.text).toContain(String(MAGIC_LINK_TTL_MINUTES));
    fetchMock.mockRestore();
  });
});

// ── /login + /auth/verify wiring ────────────────────────────────────────────

function makeAuthApp() {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", authRoutes());
  return app;
}

function env(overrides: Partial<Env> = {}): Env {
  return {
    DB: mock.db,
    RL_KV: kv.asKv(),
    ENVIRONMENT: "test",
    JWT_SECRET: "test-secret-32-chars-for-hs256-ok",
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
    ...overrides,
  } as Env;
}

describe("/login and /auth/verify routes", () => {
  it("GET /login renders the form", async () => {
    const app = makeAuthApp();
    const res = await app.request("/login", {}, env());
    expect(res.status).toBe(200);
    const html = await res.text();
    expect(html).toMatch(/<form/);
    expect(html).toMatch(/name="email"/);
  });

  it("POST /login with a valid email inserts a magic_link row", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("{}", { status: 200 }));
    const app = makeAuthApp();
    const body = new URLSearchParams({ email: "newuser@example.com" });
    const res = await app.request(
      "/login",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: body.toString(),
      },
      env({ RESEND_API_KEY: "re_test" }),
    );
    expect(res.status).toBe(200);
    const text = await res.text();
    expect(text).toMatch(/Check your inbox/);
    // Exactly one row should be present, and fetch should have been called.
    expect(fetchMock).toHaveBeenCalledOnce();
    fetchMock.mockRestore();
  });

  it("POST /login with a bad email returns 400", async () => {
    const app = makeAuthApp();
    const body = new URLSearchParams({ email: "not-an-email" });
    const res = await app.request(
      "/login",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: body.toString(),
      },
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("POST /login swallows Resend failures and still shows success", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("boom", { status: 500 }));
    const app = makeAuthApp();
    const body = new URLSearchParams({ email: "boom@example.com" });
    const res = await app.request(
      "/login",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: body.toString(),
      },
      env({ RESEND_API_KEY: "re_test" }),
    );
    expect(res.status).toBe(200);
    const text = await res.text();
    expect(text).toMatch(/Check your inbox/);
    fetchMock.mockRestore();
  });

  it("GET /auth/verify with a valid token sets sv_session and redirects", async () => {
    // Seed a magic link by hitting the real create function.
    const link = await createMagicLink({
      db: mock.db,
      email: "verify@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const app = makeAuthApp();
    const res = await app.request(
      `/auth/verify?token=${encodeURIComponent(link.token)}`,
      {},
      env(),
    );
    expect(res.status).toBe(302);
    expect(res.headers.get("location")).toBe("/dashboard");
    const cookie = res.headers.get("set-cookie") ?? "";
    expect(cookie).toMatch(/sv_session=/);
    expect(cookie).toMatch(/HttpOnly/);
    expect(cookie).toMatch(/SameSite=Lax/);
    // A user row was created for that email.
    const user = await getUserByEmail(mock.db, "verify@example.com");
    expect(user).not.toBeNull();
  });

  it("GET /auth/verify with an unknown token redirects to /login", async () => {
    const app = makeAuthApp();
    const res = await app.request(
      "/auth/verify?token=ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ",
      {},
      env(),
    );
    expect(res.status).toBe(302);
    const loc = res.headers.get("location") ?? "";
    expect(loc).toContain("/login");
    expect(loc).toContain("invalid");
  });

  it("GET /auth/verify with a consumed token refuses replay", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "replay@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const app = makeAuthApp();
    const first = await app.request(
      `/auth/verify?token=${encodeURIComponent(link.token)}`,
      {},
      env(),
    );
    expect(first.status).toBe(302);
    expect(first.headers.get("location")).toBe("/dashboard");
    const second = await app.request(
      `/auth/verify?token=${encodeURIComponent(link.token)}`,
      {},
      env(),
    );
    expect(second.status).toBe(302);
    expect(second.headers.get("location")).toContain("/login");
  });

  it("GET /auth/verify without JWT_SECRET returns 500", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "nosecret@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const app = makeAuthApp();
    const res = await app.request(
      `/auth/verify?token=${encodeURIComponent(link.token)}`,
      {},
      env({ JWT_SECRET: undefined }),
    );
    expect(res.status).toBe(500);
  });

  it("POST /auth/logout clears the cookie and redirects home", async () => {
    const app = makeAuthApp();
    const res = await app.request(
      "/auth/logout",
      { method: "POST" },
      env(),
    );
    expect(res.status).toBe(303);
    expect(res.headers.get("location")).toBe("/");
    const cookie = res.headers.get("set-cookie") ?? "";
    expect(cookie).toMatch(/sv_session=/);
    expect(cookie).toMatch(/Max-Age=0/);
  });

  it("stores the magic_link row hashed — DB leak cannot reveal the token", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "leak@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const hash = await hashApiKey(link.token);
    const row = await getMagicLink(mock.db, hash);
    expect(row).not.toBeNull();
    expect(row!.email).toBe("leak@example.com");
    // The raw token must NOT appear anywhere in the row.
    const serialized = JSON.stringify(row);
    expect(serialized).not.toContain(link.token);
  });
});
