/**
 * Billing routes for the hub gateway.
 *
 *   GET  /billing/checkout?plan=portfolio-monthly   — 303 → Stripe
 *   POST /billing/checkout (form data)              — same, for /pricing form
 *
 * No dashboard auth in the MVP — same as Gateway #1. Buyers land on
 * /pricing, click Subscribe, get redirected to Stripe Checkout,
 * finish the payment, get their API key provisioned by the webhook
 * handler (`/stripe/webhook`).
 *
 * Error shape:
 *   503 — STRIPE_SECRET_KEY not set (pre-go-live)
 *   400 — plan missing/invalid
 *   502 — Stripe API returned an error (passed through)
 *   500 — other unexpected failures
 */

import { Hono, type Context } from "hono";
import type { Env } from "../env.js";
import { startCheckout } from "../billing/checkout.js";
import { parsePlanId } from "../billing/tiers.js";
import { StripeError } from "../billing/stripe-client.js";

type BillingCtx = Context<{ Bindings: Env }>;

export function billingRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  async function handleCheckout(c: BillingCtx): Promise<Response> {
    // Accept plan via query (GET) or form body (POST).
    let planRaw: unknown = c.req.query("plan");
    let email: string | undefined;
    if (!planRaw && c.req.method === "POST") {
      const form = await c.req.parseBody().catch(() => null);
      if (form) {
        planRaw = form.plan;
        const e = typeof form.email === "string" ? form.email.trim() : "";
        email = e ? e.toLowerCase() : undefined;
      }
    }
    const plan = parsePlanId(planRaw);
    if (!plan) {
      return c.text(
        "Unknown plan — expected one of: portfolio-monthly, portfolio-yearly, team-monthly, team-yearly",
        400,
      );
    }

    if (!c.env.STRIPE_SECRET_KEY) {
      return c.text(
        "Billing is not yet configured for the hub gateway. Come back once Portfolio Access and Team are live in Stripe.",
        503,
      );
    }

    const baseUrl =
      c.env.GATEWAY_BASE_URL ?? new URL(c.req.url).origin;
    try {
      const { session } = await startCheckout({
        env: c.env,
        plan,
        baseUrl,
        email,
      });
      return c.redirect(session.url, 303);
    } catch (err) {
      if (err instanceof StripeError) {
        return c.text(`Stripe error: ${err.message}`, 502);
      }
      const message = err instanceof Error ? err.message : "Unknown error";
      return c.text(message, 500);
    }
  }

  app.get("/billing/checkout", (c) => handleCheckout(c));
  app.post("/billing/checkout", (c) => handleCheckout(c));

  return app;
}
