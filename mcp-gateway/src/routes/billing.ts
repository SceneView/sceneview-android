/**
 * Billing action routes (auth-gated):
 *
 *   POST /billing/checkout — starts a Stripe Checkout Session
 *   POST /billing/portal   — opens the Stripe Customer Portal
 *
 * The GET /billing HTML page is served by `routes/dashboard.tsx`.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import {
  requireSession,
  type SessionVariables,
} from "../auth/session-middleware.js";
import { startCheckout } from "../billing/checkout.js";
import { openPortal } from "../billing/portal.js";
import { parsePlanId } from "../billing/tiers.js";
import { StripeError } from "../billing/stripe-client.js";

/** Mounts the billing action routes on a Hono router. */
export function billingRoutes(): Hono<{
  Bindings: Env;
  Variables: SessionVariables;
}> {
  const app = new Hono<{ Bindings: Env; Variables: SessionVariables }>();

  app.post("/billing/checkout", requireSession(), async (c) => {
    const session = c.get("session")!;
    const form = await c.req.parseBody();
    const plan = parsePlanId(form.plan);
    if (!plan) {
      return c.text("Unknown plan", 400);
    }
    const baseUrl =
      c.env.DASHBOARD_BASE_URL ?? new URL(c.req.url).origin;
    try {
      const { session: stripeSession } = await startCheckout({
        env: c.env,
        user: session.user,
        plan,
        baseUrl,
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

  app.post("/billing/portal", requireSession(), async (c) => {
    const session = c.get("session")!;
    const baseUrl =
      c.env.DASHBOARD_BASE_URL ?? new URL(c.req.url).origin;
    try {
      const portalSession = await openPortal({
        env: c.env,
        user: session.user,
        baseUrl,
      });
      return c.redirect(portalSession.url, 303);
    } catch (err) {
      if (err instanceof StripeError) {
        return c.text(`Stripe error: ${err.message}`, 502);
      }
      const message = err instanceof Error ? err.message : "Unknown error";
      return c.text(message, 400);
    }
  });

  return app;
}
