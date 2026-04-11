/**
 * Billing action routes (public, no dashboard auth in the MVP):
 *
 *   POST /billing/checkout — starts a Stripe Checkout Session
 *
 * The `/billing/portal` route is intentionally NOT mounted in the MVP:
 * without a user-auth flow we cannot identify who is asking to manage
 * their subscription. Buyers use the Stripe email receipts to reach
 * the portal directly until we reintroduce a dashboard.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { startCheckout } from "../billing/checkout.js";
import { parsePlanId } from "../billing/tiers.js";
import { StripeError } from "../billing/stripe-client.js";

/** Mounts the billing action routes on a Hono router. */
export function billingRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.post("/billing/checkout", async (c) => {
    const form = await c.req.parseBody();
    const plan = parsePlanId(form.plan);
    if (!plan) {
      return c.text("Unknown plan", 400);
    }
    const rawEmail = typeof form.email === "string" ? form.email.trim() : "";
    const email = rawEmail ? rawEmail.toLowerCase() : undefined;

    const baseUrl =
      c.env.DASHBOARD_BASE_URL ?? new URL(c.req.url).origin;
    try {
      const { session: stripeSession } = await startCheckout({
        env: c.env,
        plan,
        baseUrl,
        email,
      });
      return c.redirect(stripeSession.url, 303);
    } catch (err) {
      if (err instanceof StripeError) {
        return c.text(`Stripe error: ${err.message}`, 502);
      }
      const message = err instanceof Error ? err.message : "Unknown error";
      return c.text(message, 500);
    }
  });

  return app;
}
