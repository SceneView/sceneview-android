/**
 * Builds a Stripe Customer Portal session for a user who already has a
 * `stripe_customer_id` on file.
 */

import type { Env } from "../env.js";
import type { UserRow } from "../db/schema.js";
import {
  createPortalSession as stripeCreatePortalSession,
  type StripePortalSession,
} from "./stripe-client.js";

/** Arguments to {@link openPortal}. */
export interface OpenPortalArgs {
  env: Env;
  user: UserRow;
  baseUrl: string;
}

/** Creates a Stripe portal session and returns its redirect URL. */
export async function openPortal(
  args: OpenPortalArgs,
): Promise<StripePortalSession> {
  if (!args.env.STRIPE_SECRET_KEY) {
    throw new Error("STRIPE_SECRET_KEY is not set");
  }
  if (!args.user.stripe_customer_id) {
    throw new Error(
      "This account is not linked to a Stripe customer yet. Complete a checkout first.",
    );
  }
  const cleanBase = args.baseUrl.replace(/\/+$/, "");
  return stripeCreatePortalSession(args.env.STRIPE_SECRET_KEY, {
    customerId: args.user.stripe_customer_id,
    returnUrl: `${cleanBase}/billing`,
  });
}
