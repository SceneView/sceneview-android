/**
 * Billing route tests for the hub gateway.
 *
 * Covers the /billing/checkout and /stripe/webhook surface WITHOUT
 * hitting the real Stripe API — the Stripe SDK is not a dependency,
 * and the client module only uses `fetch()` which we monkey-patch
 * per-test. The webhook signature verification uses the real
 * Web Crypto HMAC (available in Node 22).
 */

import { describe, it, expect } from "vitest";
import app from "../src/index.js";
import { makeEnv } from "./helpers/fake-bindings.js";
import { parsePlanId, ALL_PLANS } from "../src/billing/tiers.js";
import { verifyWebhookSignature } from "../src/billing/stripe-client.js";

const FAKE_ENV_NO_STRIPE = makeEnv({
  api_keys: [],
  users: [],
  usage_records: [],
}).env;

describe("billing — parsePlanId", () => {
  it("accepts every plan in ALL_PLANS", () => {
    for (const p of ALL_PLANS) expect(parsePlanId(p)).toBe(p);
  });
  it("rejects unknown and non-string values", () => {
    expect(parsePlanId("pro-monthly")).toBeNull();
    expect(parsePlanId("team-weekly")).toBeNull();
    expect(parsePlanId(undefined)).toBeNull();
    expect(parsePlanId(42)).toBeNull();
    expect(parsePlanId(null)).toBeNull();
  });
});

describe("GET /billing/checkout", () => {
  it("returns 400 on missing plan", async () => {
    const res = await app.request("/billing/checkout", {}, FAKE_ENV_NO_STRIPE);
    expect(res.status).toBe(400);
    const body = await res.text();
    expect(body).toContain("Unknown plan");
  });

  it("returns 400 on unknown plan", async () => {
    const res = await app.request(
      "/billing/checkout?plan=team-weekly",
      {},
      FAKE_ENV_NO_STRIPE,
    );
    expect(res.status).toBe(400);
  });

  it("returns 503 when STRIPE_SECRET_KEY is not set", async () => {
    const res = await app.request(
      "/billing/checkout?plan=portfolio-monthly",
      {},
      FAKE_ENV_NO_STRIPE,
    );
    expect(res.status).toBe(503);
    const body = await res.text();
    expect(body).toContain("Billing is not yet configured");
  });

  it("redirects to Stripe when fully configured", async () => {
    const realFetch = globalThis.fetch;
    const calls: Array<{ url: string; init?: RequestInit }> = [];
    globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
      calls.push({
        url: typeof input === "string" ? input : input.toString(),
        init,
      });
      return new Response(
        JSON.stringify({
          id: "cs_test_abc",
          url: "https://checkout.stripe.com/c/pay/cs_test_abc",
          metadata: { plan: "portfolio-monthly" },
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      );
    }) as typeof fetch;

    try {
      const env = {
        ...FAKE_ENV_NO_STRIPE,
        STRIPE_SECRET_KEY: "sk_test_FAKE",
        STRIPE_PRICE_PORTFOLIO_MONTHLY: "price_test_FAKE",
        GATEWAY_BASE_URL: "https://hub-mcp.test",
      };
      const res = await app.request(
        "/billing/checkout?plan=portfolio-monthly",
        {},
        env,
      );
      expect(res.status).toBe(303);
      expect(res.headers.get("location")).toBe(
        "https://checkout.stripe.com/c/pay/cs_test_abc",
      );
      expect(calls).toHaveLength(1);
      const body = calls[0].init?.body as string;
      expect(body).toContain("mode=subscription");
      expect(body).toContain(
        encodeURIComponent("line_items[0][price]") + "=price_test_FAKE",
      );
      // CRITICAL: never re-introduce customer_creation in subscription mode.
      expect(body).not.toContain("customer_creation");
      // Metadata is propagated.
      expect(body).toContain("metadata%5Bplan%5D=portfolio-monthly");
      expect(body).toContain("metadata%5Btier%5D=pro");
      expect(body).toContain("metadata%5Bgateway%5D=hub");
    } finally {
      globalThis.fetch = realFetch;
    }
  });
});

describe("verifyWebhookSignature", () => {
  const secret = "whsec_test_FAKE";
  const body = '{"id":"evt_1","type":"checkout.session.completed"}';

  async function sign(ts: number, payload: string): Promise<string> {
    const key = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"],
    );
    const mac = await crypto.subtle.sign(
      "HMAC",
      key,
      new TextEncoder().encode(`${ts}.${payload}`),
    );
    return Array.from(new Uint8Array(mac))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");
  }

  it("accepts a well-formed signature within the tolerance window", async () => {
    const ts = Math.floor(Date.now() / 1000);
    const sig = await sign(ts, body);
    const header = `t=${ts},v1=${sig}`;
    expect(await verifyWebhookSignature(body, header, secret, ts)).toBe(true);
  });

  it("rejects a signature with a wrong secret", async () => {
    const ts = Math.floor(Date.now() / 1000);
    const sig = await sign(ts, body);
    const header = `t=${ts},v1=${sig}`;
    expect(
      await verifyWebhookSignature(body, header, "whsec_WRONG", ts),
    ).toBe(false);
  });

  it("rejects an expired timestamp", async () => {
    const old = Math.floor(Date.now() / 1000) - 1000;
    const sig = await sign(old, body);
    const header = `t=${old},v1=${sig}`;
    expect(
      await verifyWebhookSignature(body, header, secret, old + 1000),
    ).toBe(false);
  });

  it("rejects a missing header", async () => {
    expect(await verifyWebhookSignature(body, null, secret)).toBe(false);
    expect(await verifyWebhookSignature(body, "", secret)).toBe(false);
  });

  it("rejects a body tampered after signing", async () => {
    const ts = Math.floor(Date.now() / 1000);
    const sig = await sign(ts, body);
    const header = `t=${ts},v1=${sig}`;
    expect(
      await verifyWebhookSignature(
        body + "tamper",
        header,
        secret,
        ts,
      ),
    ).toBe(false);
  });
});

describe("POST /stripe/webhook", () => {
  const secret = "whsec_test_FAKE";

  async function signedRequest(
    body: string,
    ts: number = Math.floor(Date.now() / 1000),
  ): Promise<Request> {
    const key = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"],
    );
    const mac = await crypto.subtle.sign(
      "HMAC",
      key,
      new TextEncoder().encode(`${ts}.${body}`),
    );
    const sig = Array.from(new Uint8Array(mac))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");
    return new Request("https://hub-mcp.test/stripe/webhook", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "stripe-signature": `t=${ts},v1=${sig}`,
      },
      body,
    });
  }

  it("returns 503 when STRIPE_WEBHOOK_SECRET is not set", async () => {
    const res = await app.request(
      "https://hub-mcp.test/stripe/webhook",
      { method: "POST", body: "{}" },
      FAKE_ENV_NO_STRIPE,
    );
    expect(res.status).toBe(503);
  });

  it("returns 400 on invalid signature", async () => {
    const env = { ...FAKE_ENV_NO_STRIPE, STRIPE_WEBHOOK_SECRET: secret };
    const req = new Request("https://hub-mcp.test/stripe/webhook", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "stripe-signature": "t=1,v1=nope",
      },
      body: '{"id":"evt_1"}',
    });
    const res = await app.request(req, {}, env);
    expect(res.status).toBe(400);
  });

  it("ignores unknown event types with 200 handled=false", async () => {
    const env = { ...FAKE_ENV_NO_STRIPE, STRIPE_WEBHOOK_SECRET: secret };
    const body = JSON.stringify({
      id: "evt_ignored",
      type: "invoice.payment_failed",
      data: { object: {} },
    });
    const req = await signedRequest(body);
    const res = await app.request(req, {}, env);
    expect(res.status).toBe(200);
    const json = (await res.json()) as { received: boolean; handled: boolean };
    expect(json.received).toBe(true);
    expect(json.handled).toBe(false);
  });
});
