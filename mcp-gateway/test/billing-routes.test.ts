/**
 * Tests for `POST /billing/checkout` and the tier mapping helpers in
 * `src/billing/tiers.ts`.
 *
 * No dashboard auth in the MVP — any visitor can POST a plan id and
 * be redirected into Stripe Checkout.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Env } from "../src/env.js";
import { billingRoutes } from "../src/routes/billing.js";
import {
  getPriceIdForPlan,
  parsePlanId,
  getTierForPriceId,
} from "../src/billing/tiers.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

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
    STRIPE_SECRET_KEY: STRIPE_SECRET,
    STRIPE_PRICE_PRO_MONTHLY: PRO_MONTHLY_PRICE,
    STRIPE_PRICE_PRO_YEARLY: PRO_YEARLY_PRICE,
    STRIPE_PRICE_TEAM_MONTHLY: TEAM_MONTHLY_PRICE,
    STRIPE_PRICE_TEAM_YEARLY: TEAM_YEARLY_PRICE,
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
    ...overrides,
  } as Env;
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
  it("rejects unknown plan", async () => {
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "plan=free-forever",
      },
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("creates a checkout session and redirects (no session required)", async () => {
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
        headers: { "content-type": "application/x-www-form-urlencoded" },
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
    // success_url carries the CHECKOUT_SESSION_ID placeholder.
    expect(body).toContain(
      "success_url=https%3A%2F%2Fsceneview-mcp.workers.dev%2Fcheckout%2Fsuccess%3Fsession_id%3D%7BCHECKOUT_SESSION_ID%7D",
    );
    // metadata carries plan + tier + billing_period.
    expect(body).toContain("metadata%5Bplan%5D=pro-monthly");
    expect(body).toContain("metadata%5Btier%5D=pro");
    expect(body).toContain("metadata%5Bbilling_period%5D=monthly");
    // No client_reference_id (no user account exists yet).
    expect(body).not.toContain("client_reference_id");
    // In subscription mode Stripe auto-creates the Customer. The
    // explicit `customer_creation=always` form field only applies to
    // `payment` mode and returns a 400 from Stripe if sent here.
    expect(body).not.toContain("customer_creation");
    fetchMock.mockRestore();
  });

  it("passes the buyer email as customer_email when provided", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: "cs_2",
            url: "https://checkout.stripe.com/cs_2",
          }),
          { status: 200 },
        ),
      );
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "plan=team-yearly&email=alice%40example.com",
      },
      env(),
    );
    expect(res.status).toBe(303);
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const body = init.body as string;
    expect(body).toContain("customer_email=alice%40example.com");
    expect(body).toContain(`line_items%5B0%5D%5Bprice%5D=${TEAM_YEARLY_PRICE}`);
    expect(body).toContain("metadata%5Btier%5D=team");
    expect(body).toContain("metadata%5Bbilling_period%5D=yearly");
    fetchMock.mockRestore();
  });

  it("returns 500 when STRIPE_SECRET_KEY is missing", async () => {
    const app = makeApp();
    const res = await app.request(
      "/billing/checkout",
      {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "plan=pro-monthly",
      },
      env({ STRIPE_SECRET_KEY: undefined }),
    );
    expect(res.status).toBe(500);
  });

  it("surfaces Stripe API errors as 502", async () => {
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
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: "plan=pro-monthly",
      },
      env(),
    );
    expect(res.status).toBe(502);
    fetchMock.mockRestore();
  });
});
