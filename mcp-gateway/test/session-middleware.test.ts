/**
 * Unit tests for `src/auth/session-middleware.ts`.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  buildClearSessionCookie,
  buildSessionCookie,
  optionalSession,
  readSessionCookie,
  requireSession,
  SESSION_COOKIE,
  shouldUseSecureCookies,
  type SessionVariables,
} from "../src/auth/session-middleware.js";
import { signJwt } from "../src/auth/jwt.js";
import type { Env } from "../src/env.js";
import { insertUser } from "../src/db/users.js";
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

function baseEnv(overrides: Partial<Env> = {}): Env {
  return {
    DB: mock.db,
    RL_KV: kv.asKv(),
    ENVIRONMENT: "test",
    JWT_SECRET: SECRET,
    ...overrides,
  } as Env;
}

function makeApp() {
  const app = new Hono<{ Bindings: Env; Variables: SessionVariables }>();
  app.use("/optional", optionalSession());
  app.get("/optional", (c) => {
    const s = c.get("session");
    return c.json({ authenticated: !!s, userId: s?.user.id ?? null });
  });
  app.use("/protected", requireSession());
  app.get("/protected", (c) => {
    const s = c.get("session")!;
    return c.json({ userId: s.user.id });
  });
  return app;
}

async function seedUser(id = "usr_session1", email = "session@example.com") {
  await insertUser(mock.db, { id, email });
}

describe("session middleware", () => {
  it("optionalSession exposes no session on missing cookie", async () => {
    const app = makeApp();
    const res = await app.request("/optional", {}, baseEnv());
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      authenticated: boolean;
      userId: string | null;
    };
    expect(body.authenticated).toBe(false);
    expect(body.userId).toBeNull();
  });

  it("optionalSession ignores a cookie signed with a different secret", async () => {
    await seedUser();
    const token = await signJwt("a-different-secret-0123456789", {
      sub: "usr_session1",
    });
    const app = makeApp();
    const res = await app.request(
      "/optional",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      baseEnv(),
    );
    const body = (await res.json()) as { authenticated: boolean };
    expect(body.authenticated).toBe(false);
  });

  it("optionalSession rejects a valid cookie when the user was deleted", async () => {
    const token = await signJwt(SECRET, { sub: "usr_gone" });
    const app = makeApp();
    const res = await app.request(
      "/optional",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      baseEnv(),
    );
    const body = (await res.json()) as { authenticated: boolean };
    expect(body.authenticated).toBe(false);
  });

  it("optionalSession accepts a valid session cookie", async () => {
    await seedUser();
    const token = await signJwt(SECRET, { sub: "usr_session1" });
    const app = makeApp();
    const res = await app.request(
      "/optional",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      baseEnv(),
    );
    const body = (await res.json()) as {
      authenticated: boolean;
      userId: string | null;
    };
    expect(body.authenticated).toBe(true);
    expect(body.userId).toBe("usr_session1");
  });

  it("requireSession redirects to /login on missing cookie", async () => {
    const app = makeApp();
    const res = await app.request("/protected", {}, baseEnv());
    expect(res.status).toBe(302);
    const loc = res.headers.get("location") ?? "";
    expect(loc).toMatch(/^\/login/);
    expect(loc).toMatch(/next=%2Fprotected/);
  });

  it("requireSession allows access when the cookie is valid", async () => {
    await seedUser();
    const token = await signJwt(SECRET, { sub: "usr_session1" });
    const app = makeApp();
    const res = await app.request(
      "/protected",
      { headers: { cookie: `${SESSION_COOKIE}=${token}` } },
      baseEnv(),
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as { userId: string };
    expect(body.userId).toBe("usr_session1");
  });

  it("requireSession on POST returns a 303 so forms switch to GET", async () => {
    const app = new Hono<{ Bindings: Env; Variables: SessionVariables }>();
    app.use("/post", requireSession());
    app.post("/post", (c) => c.text("ok"));
    const res = await app.request("/post", { method: "POST" }, baseEnv());
    expect(res.status).toBe(303);
  });

  it("readSessionCookie extracts the value from a messy cookie header", async () => {
    const app = new Hono();
    app.get("/extract", (c) => {
      const value = readSessionCookie(c);
      return c.text(value ?? "none");
    });
    const res1 = await app.request("/extract", {
      headers: {
        cookie: `foo=bar; ${SESSION_COOKIE}=my-token; baz=qux`,
      },
    });
    expect(await res1.text()).toBe("my-token");
    const res2 = await app.request("/extract");
    expect(await res2.text()).toBe("none");
  });

  it("buildSessionCookie sets the expected attributes", () => {
    const c = buildSessionCookie({
      value: "tok",
      maxAgeSeconds: 60,
      secure: true,
    });
    expect(c).toMatch(/^sv_session=tok;/);
    expect(c).toMatch(/Path=\//);
    expect(c).toMatch(/HttpOnly/);
    expect(c).toMatch(/SameSite=Lax/);
    expect(c).toMatch(/Max-Age=60/);
    expect(c).toMatch(/Secure/);
  });

  it("buildSessionCookie omits Secure when secure=false", () => {
    const c = buildSessionCookie({ value: "", maxAgeSeconds: 0, secure: false });
    expect(c).not.toMatch(/Secure/);
  });

  it("buildClearSessionCookie returns a zero-length cookie", () => {
    const c = buildClearSessionCookie(false);
    expect(c).toMatch(/^sv_session=;/);
    expect(c).toMatch(/Max-Age=0/);
  });

  it("shouldUseSecureCookies is false in development", () => {
    expect(
      shouldUseSecureCookies({ ENVIRONMENT: "development" } as Env),
    ).toBe(false);
    expect(
      shouldUseSecureCookies({ ENVIRONMENT: "production" } as Env),
    ).toBe(true);
    expect(shouldUseSecureCookies({} as Env)).toBe(true);
  });
});
