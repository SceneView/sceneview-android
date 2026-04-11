/**
 * Builds a Stripe Checkout Session for a given user + plan.
 *
 * The user's row id is passed as `client_reference_id` so the webhook
 * handler can identify the buyer without relying on the email (which
 * might not match their account email yet).
 */

import type { Env } from "../env.js";
import type { UserRow } from "../db/schema.js";
import {
  createCheckoutSession as stripeCreateCheckoutSession,
  type StripeCheckoutSession,
} from "./stripe-client.js";
import { getPriceIdForPlan, type PlanId } from "./tiers.js";

/** Arguments to {@link startCheckout}. */
export interface StartCheckoutArgs {
  env: Env;
  user: UserRow;
  plan: PlanId;
  baseUrl: string;
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
  const successUrl = `${cleanBase}/billing?success=1`;
  const cancelUrl = `${cleanBase}/pricing?canceled=1`;

  const session = await stripeCreateCheckoutSession(args.env.STRIPE_SECRET_KEY, {
    priceId,
    clientReferenceId: args.user.id,
    customerEmail: args.user.email,
    customerId: args.user.stripe_customer_id ?? undefined,
    successUrl,
    cancelUrl,
  });
  return { session };
}
