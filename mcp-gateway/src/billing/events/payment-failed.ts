/**
 * Handler for `invoice.payment_failed`.
 *
 * Stripe handles the grace period automatically — they retry payments
 * and only fire `customer.subscription.deleted` once the dunning
 * period expires. We therefore intentionally do NOT downgrade the
 * user here: logging is enough.
 */

import type { Env } from "../../env.js";
import type { StripeEvent } from "../webhook.js";

export async function handlePaymentFailed(
  _env: Env,
  _event: StripeEvent,
): Promise<void> {
  // Intentional no-op. Grace period is owned by Stripe dunning.
  return;
}
