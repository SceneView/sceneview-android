/**
 * Stripe webhook receiver.
 *
 *   POST /stripe/webhook — public, signed with the shared webhook secret.
 *
 * We reply 200 immediately on a valid signature and dispatch the
 * event asynchronously via `ctx.waitUntil` so Stripe never hits a
 * timeout. Failures inside the handler are logged but never fail the
 * webhook — Stripe retries on its own schedule if we 5xx.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { dispatchWebhookEvent, verifyWebhookSignature } from "../billing/webhook.js";

/** Mounts `/stripe/webhook`. */
export function webhookRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.post("/stripe/webhook", async (c) => {
    if (!c.env.STRIPE_WEBHOOK_SECRET) {
      return c.text("Webhook secret not configured", 500);
    }
    const rawBody = await c.req.raw.clone().text();
    const header = c.req.header("stripe-signature");
    const event = await verifyWebhookSignature({
      secret: c.env.STRIPE_WEBHOOK_SECRET,
      header: header ?? null,
      rawBody,
    });
    if (!event) {
      return c.text("Invalid signature", 400);
    }

    const promise = dispatchWebhookEvent(c.env, event).catch(() => {
      // Swallow — Stripe retries 5xx, but failing silently avoids
      // flapping a dashboard that cannot observe the error inline.
    });

    // Hono throws on `c.executionCtx` when no execution context is attached
    // (i.e. `app.request(...)` in tests). Probe via the raw descriptor.
    let exec:
      | { waitUntil?: (p: Promise<unknown>) => void }
      | undefined;
    try {
      exec = c.executionCtx as unknown as {
        waitUntil?: (p: Promise<unknown>) => void;
      };
    } catch {
      exec = undefined;
    }
    if (exec?.waitUntil) {
      exec.waitUntil(promise);
    } else {
      await promise;
    }
    return c.json({ received: true });
  });

  return app;
}
