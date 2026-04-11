/**
 * Orchestrates `/billing/checkout?plan=...` for the hub gateway.
 *
 * Flow:
 *   1. Parse the plan id from the request.
 *   2. Validate the Stripe secret key + price id are configured.
 *   3. POST to Stripe to create a Checkout Session with the right
 *      price, success URL, cancel URL, and metadata.
 *   4. Return the session — the route handler redirects 303 to
 *      session.url (Stripe-hosted checkout page).
 *
 * The success URL carries `session_id={CHECKOUT_SESSION_ID}` which
 * Stripe substitutes server-side; the /checkout/success route can
 * later use it to provision an API key (handled by the webhook,
 * same as Gateway #1).
 */

import type { Env } from "../env.js";
import {
  createCheckoutSession as stripeCreateCheckoutSession,
  type StripeCheckoutSession,
} from "./stripe-client.js";
import { getPriceIdForPlan, type PlanId } from "./tiers.js";

export interface StartCheckoutArgs {
  env: Env;
  plan: PlanId;
  baseUrl: string;
  email?: string;
}

export interface StartCheckoutResult {
  session: StripeCheckoutSession;
}

/** Creates a Stripe Checkout Session for a given plan. */
export async function startCheckout(
  args: StartCheckoutArgs,
): Promise<StartCheckoutResult> {
  if (!args.env.STRIPE_SECRET_KEY) {
    throw new Error("STRIPE_SECRET_KEY is not set");
  }
  const priceId = getPriceIdForPlan(args.env, args.plan);
  if (!priceId) {
    throw new Error(
      `Stripe price id for plan "${args.plan}" is not configured`,
    );
  }

  const cleanBase = args.baseUrl.replace(/\/+$/, "");
  const successUrl = `${cleanBase}/checkout/success?session_id={CHECKOUT_SESSION_ID}`;
  const cancelUrl = `${cleanBase}/pricing?canceled=1`;

  const tier = args.plan.startsWith("team") ? "team" : "pro";
  const billingPeriod = args.plan.endsWith("yearly") ? "yearly" : "monthly";

  const session = await stripeCreateCheckoutSession(args.env.STRIPE_SECRET_KEY, {
    priceId,
    customerEmail: args.email,
    successUrl,
    cancelUrl,
    metadata: {
      plan: args.plan,
      tier,
      billing_period: billingPeriod,
      gateway: "hub",
    },
  });
  return { session };
}
