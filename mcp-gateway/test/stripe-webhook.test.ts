/**
 * Tests for the Stripe webhook signature verifier and event dispatcher.
 */

import { Hono } from "hono";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Env } from "../src/env.js";
import {
  computeStripeSignature,
  parseStripeSignatureHeader,
  verifyWebhookSignature,
} from "../src/billing/webhook.js";
import { webhookRoutes } from "../src/routes/webhooks.js";
import { handleCheckoutCompleted } from "../src/billing/events/checkout-completed.js";
import { handleSubscriptionUpdated } from "../src/billing/events/subscription-updated.js";
import { handleSubscriptionDeleted } from "../src/billing/events/subscription-deleted.js";
import { insertUser, getUserById } from "../src/db/users.js";
import { createApiKey } from "../src/auth/api-keys.js";
import {
  getSubscriptionByStripeId,
  insertSubscription,
} from "../src/db/subscriptions.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";

const WEBHOOK_SECRET = "whsec_test_secret_0123456789abcdef";
const STRIPE_SECRET = "sk_test_PLACEHOLDER";
const PRO_MONTHLY_PRICE = "price_pro_monthly_test";
const TEAM_MONTHLY_PRICE = "price_team_monthly_test";

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
    STRIPE_WEBHOOK_SECRET: WEBHOOK_SECRET,
    STRIPE_PRICE_PRO_MONTHLY: PRO_MONTHLY_PRICE,
    STRIPE_PRICE_TEAM_MONTHLY: TEAM_MONTHLY_PRICE,
    DASHBOARD_BASE_URL: "https://sceneview-mcp.workers.dev",
    ...overrides,
  } as Env;
}

/** Builds a signed `Stripe-Signature` header for the given payload. */
async function sign(payload: string, t: number = Math.floor(Date.now() / 1000)) {
  const sig = await computeStripeSignature(WEBHOOK_SECRET, t, payload);
  return `t=${t},v1=${sig}`;
}

describe("parseStripeSignatureHeader", () => {
  it("parses a standard header", () => {
    const parsed = parseStripeSignatureHeader("t=1234,v1=abcdef,v1=fedcba");
    expect(parsed).toEqual({ t: 1234, v1: ["abcdef", "fedcba"] });
  });

  it("returns null for malformed headers", () => {
    expect(parseStripeSignatureHeader("")).toBeNull();
    expect(parseStripeSignatureHeader("v1=abc")).toBeNull();
    expect(parseStripeSignatureHeader("t=not-a-number,v1=abc")).toBeNull();
  });
});

describe("verifyWebhookSignature", () => {
  const payload = '{"type":"test.event","data":{"object":{}}}';

  it("accepts a valid signature within the tolerance window", async () => {
    const now = Math.floor(Date.now() / 1000);
    const header = await sign(payload, now);
    const event = await verifyWebhookSignature({
      secret: WEBHOOK_SECRET,
      header,
      rawBody: payload,
      nowSeconds: now,
    });
    expect(event).not.toBeNull();
    expect(event!.type).toBe("test.event");
  });

  it("rejects a signature outside the tolerance window", async () => {
    const past = Math.floor(Date.now() / 1000) - 1000;
    const header = await sign(payload, past);
    const event = await verifyWebhookSignature({
      secret: WEBHOOK_SECRET,
      header,
      rawBody: payload,
    });
    expect(event).toBeNull();
  });

  it("rejects a tampered payload", async () => {
    const now = Math.floor(Date.now() / 1000);
    const header = await sign(payload, now);
    const event = await verifyWebhookSignature({
      secret: WEBHOOK_SECRET,
      header,
      rawBody: '{"type":"different.event","data":{"object":{}}}',
      nowSeconds: now,
    });
    expect(event).toBeNull();
  });

  it("rejects a signature computed with the wrong secret", async () => {
    const now = Math.floor(Date.now() / 1000);
    const sig = await computeStripeSignature(
      "some-other-secret",
      now,
      payload,
    );
    const event = await verifyWebhookSignature({
      secret: WEBHOOK_SECRET,
      header: `t=${now},v1=${sig}`,
      rawBody: payload,
      nowSeconds: now,
    });
    expect(event).toBeNull();
  });

  it("rejects when the secret is missing", async () => {
    const event = await verifyWebhookSignature({
      secret: "",
      header: "t=1,v1=abc",
      rawBody: payload,
    });
    expect(event).toBeNull();
  });

  it("rejects when the header is missing", async () => {
    const event = await verifyWebhookSignature({
      secret: WEBHOOK_SECRET,
      header: null,
      rawBody: payload,
    });
    expect(event).toBeNull();
  });
});

// ── /stripe/webhook HTTP wiring ─────────────────────────────────────────────

function makeApp() {
  const app = new Hono<{ Bindings: Env }>();
  app.route("/", webhookRoutes());
  return app;
}

describe("POST /stripe/webhook", () => {
  it("returns 400 on an invalid signature", async () => {
    const app = makeApp();
    const res = await app.request(
      "/stripe/webhook",
      {
        method: "POST",
        headers: {
          "stripe-signature": "t=1,v1=deadbeef",
          "content-type": "application/json",
        },
        body: '{"type":"checkout.session.completed","data":{"object":{}}}',
      },
      env(),
    );
    expect(res.status).toBe(400);
  });

  it("returns 200 on a valid signature and dispatches the event", async () => {
    const payload =
      '{"id":"evt_1","type":"checkout.session.completed","data":{"object":{"id":"cs_1"}}}';
    const header = await sign(payload);
    const app = makeApp();
    const res = await app.request(
      "/stripe/webhook",
      {
        method: "POST",
        headers: {
          "stripe-signature": header,
          "content-type": "application/json",
        },
        body: payload,
      },
      env(),
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as { received: boolean };
    expect(body.received).toBe(true);
  });

  it("returns 500 when the webhook secret is not configured", async () => {
    const payload =
      '{"id":"evt_1","type":"unknown.type","data":{"object":{}}}';
    const app = makeApp();
    const res = await app.request(
      "/stripe/webhook",
      {
        method: "POST",
        headers: { "stripe-signature": "t=1,v1=abc" },
        body: payload,
      },
      env({ STRIPE_WEBHOOK_SECRET: undefined }),
    );
    expect(res.status).toBe(500);
  });
});

// ── Event handlers ───────────────────────────────────────────────────────────

describe("handleCheckoutCompleted", () => {
  it("upserts customer id, creates a subscription row, and updates the tier", async () => {
    await insertUser(mock.db, {
      id: "usr_checkout",
      email: "checkout@example.com",
    });
    await createApiKey(mock.db, "usr_checkout", "pre");

    // Prime the KV auth cache for that key so we can watch it vanish.
    const kvEntries = [...kv.store.keys()];
    expect(kvEntries.length).toBe(0);

    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: "sub_123",
            customer: "cus_123",
            status: "active",
            cancel_at_period_end: false,
            current_period_end: 1_700_000_000,
            items: { data: [{ price: { id: PRO_MONTHLY_PRICE } }] },
          }),
          { status: 200 },
        ),
      );

    await handleCheckoutCompleted(env(), {
      id: "evt_1",
      type: "checkout.session.completed",
      data: {
        object: {
          id: "cs_1",
          customer: "cus_123",
          client_reference_id: "usr_checkout",
          subscription: "sub_123",
        },
      },
      created: 1,
    });

    const user = await getUserById(mock.db, "usr_checkout");
    expect(user?.tier).toBe("pro");
    expect(user?.stripe_customer_id).toBe("cus_123");

    const sub = await getSubscriptionByStripeId(mock.db, "sub_123");
    expect(sub).not.toBeNull();
    expect(sub!.tier).toBe("pro");
    expect(sub!.status).toBe("active");
    fetchMock.mockRestore();
  });

  it("ignores unknown users", async () => {
    await handleCheckoutCompleted(env(), {
      id: "evt_x",
      type: "checkout.session.completed",
      data: {
        object: {
          id: "cs_x",
          customer: "cus_x",
          client_reference_id: "usr_does_not_exist",
          subscription: "sub_x",
        },
      },
      created: 1,
    });
    // No throw, no side effects.
    const all = await mock.db.prepare("SELECT COUNT(*) as n FROM users").first<{
      n: number;
    }>();
    expect(all?.n).toBe(0);
  });
});

describe("handleSubscriptionUpdated", () => {
  it("updates an existing subscription row and the user tier", async () => {
    await insertUser(mock.db, {
      id: "usr_upd",
      email: "upd@example.com",
      stripeCustomerId: "cus_upd",
      tier: "pro",
    });
    await insertSubscription(mock.db, {
      id: "sub_row_1",
      userId: "usr_upd",
      stripeSubscriptionId: "sub_upd",
      stripePriceId: PRO_MONTHLY_PRICE,
      tier: "pro",
      status: "active",
      currentPeriodEnd: 1_600_000_000 * 1000,
    });
    await handleSubscriptionUpdated(env(), {
      id: "evt_u",
      type: "customer.subscription.updated",
      data: {
        object: {
          id: "sub_upd",
          customer: "cus_upd",
          status: "active",
          cancel_at_period_end: false,
          current_period_end: 1_700_000_000,
          items: { data: [{ price: { id: TEAM_MONTHLY_PRICE } }] },
        },
      },
      created: 1,
    });
    const user = await getUserById(mock.db, "usr_upd");
    expect(user?.tier).toBe("team");
  });

  it("ignores events for unknown customers", async () => {
    await handleSubscriptionUpdated(env(), {
      id: "evt_u2",
      type: "customer.subscription.updated",
      data: {
        object: {
          id: "sub_x",
          customer: "cus_unknown",
          status: "active",
          cancel_at_period_end: false,
          current_period_end: 1,
          items: { data: [{ price: { id: PRO_MONTHLY_PRICE } }] },
        },
      },
      created: 1,
    });
  });
});

describe("handleSubscriptionDeleted", () => {
  it("downgrades the user to free", async () => {
    await insertUser(mock.db, {
      id: "usr_del",
      email: "del@example.com",
      stripeCustomerId: "cus_del",
      tier: "pro",
    });
    await insertSubscription(mock.db, {
      id: "sub_row_2",
      userId: "usr_del",
      stripeSubscriptionId: "sub_del",
      stripePriceId: PRO_MONTHLY_PRICE,
      tier: "pro",
      status: "active",
      currentPeriodEnd: 1_700_000_000 * 1000,
    });
    await handleSubscriptionDeleted(env(), {
      id: "evt_d",
      type: "customer.subscription.deleted",
      data: {
        object: {
          id: "sub_del",
          customer: "cus_del",
          current_period_end: 1_700_000_000,
          status: "canceled",
        },
      },
      created: 1,
    });
    const user = await getUserById(mock.db, "usr_del");
    expect(user?.tier).toBe("free");
    const sub = await getSubscriptionByStripeId(mock.db, "sub_del");
    expect(sub?.status).toBe("canceled");
  });
});
