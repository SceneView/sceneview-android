/**
 * Builds a Stripe Checkout Session for a given plan.
 *
 * No dashboard auth in the MVP — the buyer arrives at `/pricing`,
 * picks a plan, and is redirected straight into Stripe. The webhook
 * handler creates the user row once the payment succeeds (see
 * `billing/events/checkout-completed.ts`).
 *
 * The `success_url` always carries `session_id={CHECKOUT_SESSION_ID}`
 * which Stripe substitutes server-side; the `/checkout/success`
 * handler uses it to look up the plaintext API key stashed in KV.
 */

import type { Env } from "../env.js";
import {
  createCheckoutSession as stripeCreateCheckoutSession,
  type StripeCheckoutSession,
} from "./stripe-client.js";
import { getPriceIdForPlan, type PlanId } from "./tiers.js";

/** Arguments to {@link startCheckout}. */
export interface StartCheckoutArgs {
  env: Env;
  plan: PlanId;
  baseUrl: string;
  /** Optional email to prefill the Stripe form with. */
  email?: string;
}

/** Result returned to the route handler. */
export interface StartCheckoutResult {
  session: StripeCheckoutSession;
}

/**
 * Creates a Stripe Checkout Session and returns its redirect URL.
 *
 * Validates:
 *   - The plan resolves to a configured Stripe price id.
 *   - `STRIPE_SECRET_KEY` is set.
 */
export async function startCheckout(
  args: StartCheckoutArgs,
): Promise<StartCheckoutResult> {
  if (!args.env.STRIPE_SECRET_KEY) {
    throw new Error("STRIPE_SECRET_KEY is not set");
  }
  const priceId = getPriceIdForPlan(args.env, args.plan);
  if (!priceId) {
    throw new Error(`Stripe price id for plan "${args.plan}" is not configured`);
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
    },
  });
  return { session };
}
