/**
 * Mapping from Stripe price ids (as read from `env`) to our internal
 * `UserTier` enum.
 *
 * Price ids are deployment-specific and set via wrangler env vars:
 *
 *   STRIPE_PRICE_PRO_MONTHLY
 *   STRIPE_PRICE_PRO_YEARLY
 *   STRIPE_PRICE_TEAM_MONTHLY
 *   STRIPE_PRICE_TEAM_YEARLY
 *
 * Unknown ids fall back to `null` so the webhook handler can log and
 * keep the user on their previous tier (conservative).
 */

import type { Env } from "../env.js";
import type { UserTier } from "../db/schema.js";

/** Logical plan identifier surfaced to the /billing/checkout route. */
export type PlanId = "pro-monthly" | "pro-yearly" | "team-monthly" | "team-yearly";

/** Looks up the Stripe price id configured for a given plan. */
export function getPriceIdForPlan(
  env: Env,
  plan: PlanId,
): string | undefined {
  switch (plan) {
    case "pro-monthly":
      return env.STRIPE_PRICE_PRO_MONTHLY;
    case "pro-yearly":
      return env.STRIPE_PRICE_PRO_YEARLY;
    case "team-monthly":
      return env.STRIPE_PRICE_TEAM_MONTHLY;
    case "team-yearly":
      return env.STRIPE_PRICE_TEAM_YEARLY;
  }
}

/** Maps a Stripe price id back to an internal tier. */
export function getTierForPriceId(
  env: Env,
  priceId: string,
): UserTier | null {
  if (!priceId) return null;
  if (
    priceId === env.STRIPE_PRICE_PRO_MONTHLY ||
    priceId === env.STRIPE_PRICE_PRO_YEARLY
  ) {
    return "pro";
  }
  if (
    priceId === env.STRIPE_PRICE_TEAM_MONTHLY ||
    priceId === env.STRIPE_PRICE_TEAM_YEARLY
  ) {
    return "team";
  }
  return null;
}

/** All plan ids accepted by the /billing/checkout route. */
export const ALL_PLANS: readonly PlanId[] = [
  "pro-monthly",
  "pro-yearly",
  "team-monthly",
  "team-yearly",
] as const;

/** Narrow a user-supplied string down to a {@link PlanId}, or null. */
export function parsePlanId(value: unknown): PlanId | null {
  if (typeof value !== "string") return null;
  return (ALL_PLANS as readonly string[]).includes(value)
    ? (value as PlanId)
    : null;
}
