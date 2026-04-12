/**
 * Stripe webhook receiver for the hub gateway.
 *
 * Flow on a successful `checkout.session.completed` event:
 *   1. Verify the Stripe-Signature header against STRIPE_WEBHOOK_SECRET.
 *   2. Parse the event body.
 *   3. Extract the plan metadata written in startCheckout().
 *   4. Resolve the tier (pro | team) from the plan metadata.
 *   5. Upsert the user row in the shared D1 `users` table.
 *
 * Compared to Gateway #1's full webhook (179 lines), this is a
 * minimal subset: no API key provisioning, no email sending, no
 * subscription state tracking beyond the initial upsert. A follow-up
 * session will wire the full key-handoff flow once the hub has its
 * own `/checkout/success` page that reads from KV (same pattern as
 * Gateway #1's commit c7d957f3 anonymous checkout).
 *
 * Any signature mismatch → 400 with a generic error (never leak
 * which check failed, to avoid helping attackers). Unknown event
 * types → 200 (silently ignored). Real errors during processing
 * → 500 so Stripe retries.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { verifyWebhookSignature } from "../billing/stripe-client.js";
import { provisionApiKey } from "../billing/key-provisioning.js";
import type { UserTier } from "../db/schema.js";

/** Minimal subset of Stripe event shapes the hub actually reads. */
interface StripeEvent {
  id: string;
  type: string;
  data: { object: Record<string, unknown> };
}

interface CheckoutSessionCompleted {
  id: string;
  customer?: string | null;
  customer_details?: { email?: string | null; name?: string | null } | null;
  customer_email?: string | null;
  subscription?: string | null;
  metadata?: Record<string, string> | null;
  amount_total?: number | null;
  currency?: string | null;
}

export function webhookRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.post("/stripe/webhook", async (c) => {
    const secret = c.env.STRIPE_WEBHOOK_SECRET;
    if (!secret) {
      return c.text("Webhook not configured", 503);
    }

    // Read the raw body BEFORE parsing — signature verification
    // runs against the exact bytes Stripe sent, not the parsed JSON.
    const rawBody = await c.req.raw.clone().text();
    const signature = c.req.header("stripe-signature") ?? null;
    const valid = await verifyWebhookSignature(rawBody, signature, secret);
    if (!valid) {
      return c.text("Invalid signature", 400);
    }

    let event: StripeEvent;
    try {
      event = JSON.parse(rawBody) as StripeEvent;
    } catch {
      return c.text("Invalid JSON body", 400);
    }

    if (event.type !== "checkout.session.completed") {
      // Known event we don't handle yet — 200 so Stripe stops retrying.
      return c.json({ received: true, handled: false });
    }

    const session = event.data.object as unknown as CheckoutSessionCompleted;
    const planMeta = session.metadata?.plan;
    const email =
      session.customer_details?.email ?? session.customer_email ?? null;

    // Tier resolution: trust the metadata written by startCheckout(),
    // fall back to price id lookup if the metadata is missing (e.g.
    // hand-crafted checkout session).
    let tier: UserTier | null = null;
    if (planMeta?.startsWith("team")) tier = "team";
    else if (planMeta?.startsWith("portfolio")) tier = "pro";
    if (!tier && typeof session.metadata?.tier === "string") {
      const raw = session.metadata.tier;
      if (raw === "pro" || raw === "team") tier = raw;
    }

    if (!tier || !email) {
      // Log and acknowledge — never 5xx on a "valid but unexpected"
      // event shape; Stripe would retry forever.
      return c.json({
        received: true,
        handled: false,
        reason: !tier ? "tier unresolved" : "email missing",
      });
    }

    // Provision the user + API key. provisionApiKey handles:
    //   1. Upsert user row in D1
    //   2. Generate sv_live_ API key + store hash in D1 api_keys
    //   3. Stash plaintext in KV under checkout_key:{session.id}
    //      for /checkout/success to display once
    //   4. Invalidate auth cache
    // Returns null on failure (logged internally, never throws).
    const result = await provisionApiKey(
      c.env,
      email,
      tier,
      session.id,
      session.customer,
    );

    if (!result) {
      // 500 so Stripe retries on the next backoff cycle.
      return c.text("Webhook processing failed — will retry", 500);
    }

    return c.json({
      received: true,
      handled: true,
      tier,
      keyPrefix: result.prefix,
    });
  });

  return app;
}

