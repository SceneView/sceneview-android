/**
 * Tests for `/billing/checkout`, `/billing/portal`, and the tier
 * mapping helpers in `src/billing/tiers.ts`.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Env } from "../src/env.js";
import { billingRoutes } from "../src/routes/billing.js";
import { signJwt } from "../src/auth/jwt.js";
import { SESSION_COOKIE } from "../src/auth/session-middleware.js";
import { insertUser, updateUserStripeCustomer } from "../src/db/users.js";
import { getPriceIdForPlan, parsePlanId, getTierForPriceId } from "../src/billing/tiers.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

const SECRET = "test-secret-32-chars-for-hs256-ok";
const STRIPE_SECRET = "sk_test_PLACEHOLDER";
const PRO_MONTHLY_PRICE = "price_pro_monthly_test";
const PRO_YEARLY_PRICE = "price_pro_yearly_test";
const TEAM_MONTHLY_PRICE = "price_team_monthly_test";
const TEAM_YEARLY_PRICE = "price_team_yearly_test";

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
    JWT_SECRET: SECRET,
    STRIPE_SECRET_KEY: STRIPE_SECRET,
    STRIPE_PRICE_PRO_MONTHLY: PRO_MONTHLY_PRICE,
    STRIPE_PRICE_PRO_YEARLY: PRO_YEARLY_PRICE,
    STRIPE_PRICE_TEAM_MONTHLY: TEAM_MONTHLY_PRICE,
    STRIPE_PRICE_TEAM_YEARLY: TEAM_YEARLY_PRICE,
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
    ...overrides,
  } as Env;
}

async function seedUser(id = "usr_bill", email = "bill@example.com") {
  await insertUser(mock.db, { id, email, tier: "free" });
  const token = await signJwt(SECRET, { sub: id });
  return { id, token };
}

function makeApp() {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", billingRoutes());
  return app;
}

describe("tier helpers", () => {
  it("parsePlanId accepts only known plans", () => {
    expect(parsePlanId("pro-monthly")).toBe("pro-monthly");
    expect(parsePlanId("pro-yearly")).toBe("pro-yearly");
    expect(parsePlanId("team-monthly")).toBe("team-monthly");
    expect(parsePlanId("team-yearly")).toBe("team-yearly");
    expect(parsePlanId("free-forever")).toBeNull();
    expect(parsePlanId(null)).toBeNull();
    expect(parsePlanId(42)).toBeNull();
  });

  it("getPriceIdForPlan reads the env vars", () => {
    expect(getPriceIdForPlan(env(), "pro-monthly")).toBe(PRO_MONTHLY_PRICE);
    expect(getPriceIdForPlan(env(), "pro-yearly")).toBe(PRO_YEARLY_PRICE);
    expect(getPriceIdForPlan(env(), "team-monthly")).toBe(TEAM_MONTHLY_PRICE);
    expect(getPriceIdForPlan(env(), "team-yearly")).toBe(TEAM_YEARLY_PRICE);
  });

  it("getTierForPriceId maps price id to tier", () => {
    expect(getTierForPriceId(env(), PRO_MONTHLY_PRICE)).toBe("pro");
    expect(getTierForPriceId(env(), PRO_YEARLY_PRICE)).toBe("pro");
    expect(getTierForPriceId(env(), TEAM_MONTHLY_PRICE)).toBe("team");
    expect(getTierForPriceId(env(), TEAM_YEARLY_PRICE)).toBe("team");
    expect(getTierForPriceId(env(), "price_unknown")).toBeNull();
  });
});

describe("POST /billing/checkout", () => {
  it("requires a session", async () => {
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "plan=pro-monthly",
      },
      env(),
    );
    expect(res.status).toBe(303);
    expect(res.headers.get("location")).toMatch(/^\/login/);
  });

  it("rejects unknown plan", async () => {
    const { token } = await seedUser();
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: {
          cookie: `${SESSION_COOKIE}=${token}`,
          "content-type": "application/x-www-form-urlencoded",
        },
        body: "plan=free-forever",
      },
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("creates a checkout session and redirects", async () => {
    const { token } = await seedUser();
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: "cs_1",
            url: "https://checkout.stripe.com/cs_1",
            customer: null,
          }),
          { status: 200 },
        ),
      );
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: {
          cookie: `${SESSION_COOKIE}=${token}`,
          "content-type": "application/x-www-form-urlencoded",
        },
        body: "plan=pro-monthly",
      },
      env(),
    );
    expect(res.status).toBe(303);
    expect(res.headers.get("location")).toBe(
      "https://checkout.stripe.com/cs_1",
    );
    // Verify the Stripe call shape.
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("https://api.stripe.com/v1/checkout/sessions");
    expect(init.method).toBe("POST");
    const headers = init.headers as Record<string, string>;
    expect(headers.authorization).toMatch(/^Basic /);
    const body = init.body as string;
    expect(body).toContain(`line_items%5B0%5D%5Bprice%5D=${PRO_MONTHLY_PRICE}`);
    expect(body).toContain("mode=subscription");
    expect(body).toContain("client_reference_id=usr_bill");
    fetchMock.mockRestore();
  });

  it("returns 500 when STRIPE_SECRET_KEY is missing", async () => {
    const { token } = await seedUser();
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: {
          cookie: `${SESSION_COOKIE}=${token}`,
          "content-type": "application/x-www-form-urlencoded",
        },
        body: "plan=pro-monthly",
      },
      env({ STRIPE_SECRET_KEY: undefined }),
    );
    expect(res.status).toBe(500);
  });

  it("surfaces Stripe API errors as 502", async () => {
    const { token } = await seedUser();
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            error: { message: "You must provide a valid price id." },
          }),
          { status: 400 },
        ),
      );
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: {
          cookie: `${SESSION_COOKIE}=${token}`,
          "content-type": "application/x-www-form-urlencoded",
        },
        body: "plan=pro-monthly",
      },
      env(),
    );
    expect(res.status).toBe(502);
    fetchMock.mockRestore();
  });
});

describe("POST /billing/portal", () => {
  it("returns 400 when the user has no stripe_customer_id", async () => {
    const { token } = await seedUser();
    const app = makeApp();
    const res = await app.request(
      "/billing/portal",
      {
        method: "POST",
        headers: { cookie: `${SESSION_COOKIE}=${token}` },
      },
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("opens a portal session when the user has a customer id", async () => {
    const { id, token } = await seedUser();
    await updateUserStripeCustomer(mock.db, id, "cus_abc");
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: "bps_1",
            url: "https://billing.stripe.com/session/123",
          }),
          { status: 200 },
        ),
      );
    const app = makeApp();
    const res = await app.request(
      "/billing/portal",
      {
        method: "POST",
        headers: { cookie: `${SESSION_COOKIE}=${token}` },
      },
      env(),
    );
    expect(res.status).toBe(303);
    expect(res.headers.get("location")).toBe(
      "https://billing.stripe.com/session/123",
    );
    const [url] = fetchMock.mock.calls[0] as [string];
    expect(url).toBe("https://api.stripe.com/v1/billing_portal/sessions");
    fetchMock.mockRestore();
  });
});
