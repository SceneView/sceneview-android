/**
 * Plan → Stripe price id mapping for the hub gateway.
 *
 * The hub advertises FOUR plans on /pricing:
 *
 *   Portfolio Access Monthly  — 29 EUR/mo
 *   Portfolio Access Yearly   — 290 EUR/yr  (monthly × 10, "save 2 months")
 *   Team Monthly              — 79 EUR/mo
 *   Team Yearly               — 790 EUR/yr
 *
 * Portfolio Access maps to `users.tier = 'pro'` in the shared D1
 * (Gateway #1 also uses `pro` — the D1 column is unified, not
 * product-specific). Team maps to `users.tier = 'team'`. See
 * src/db/schema.ts for the full rationale.
 *
 * Price ids come from wrangler env vars:
 *
 *   STRIPE_PRICE_PORTFOLIO_MONTHLY
 *   STRIPE_PRICE_PORTFOLIO_YEARLY
 *   STRIPE_PRICE_TEAM_MONTHLY
 *   STRIPE_PRICE_TEAM_YEARLY
 *
 * Unknown ids fall back to `null` so the webhook handler can log a
 * warning and keep the user on their previous tier (conservative
 * default — never downgrade on a malformed webhook).
 */

import type { Env } from "../env.js";
import type { UserTier } from "../db/schema.js";

/** Logical plan identifier surfaced to the /billing/checkout route. */
export type PlanId =
  | "portfolio-monthly"
  | "portfolio-yearly"
  | "team-monthly"
  | "team-yearly";

/** All plan ids accepted by /billing/checkout. */
export const ALL_PLANS: readonly PlanId[] = [
  "portfolio-monthly",
  "portfolio-yearly",
  "team-monthly",
  "team-yearly",
] as const;

/** Narrow a user-supplied value down to a PlanId, or null. */
export function parsePlanId(value: unknown): PlanId | null {
  if (typeof value !== "string") return null;
  return (ALL_PLANS as readonly string[]).includes(value)
    ? (value as PlanId)
    : null;
}

/** Looks up the configured Stripe price id for a plan. */
export function getPriceIdForPlan(
  env: Env,
  plan: PlanId,
): string | undefined {
  switch (plan) {
    case "portfolio-monthly":
      return env.STRIPE_PRICE_PORTFOLIO_MONTHLY;
    case "portfolio-yearly":
      return env.STRIPE_PRICE_PORTFOLIO_YEARLY;
    case "team-monthly":
      return env.STRIPE_PRICE_TEAM_MONTHLY;
    case "team-yearly":
      return env.STRIPE_PRICE_TEAM_YEARLY;
  }
}

/** Maps a Stripe price id back to the D1 users.tier value. */
export function getTierForPriceId(
  env: Env,
  priceId: string,
): UserTier | null {
  if (!priceId) return null;
  if (
    priceId === env.STRIPE_PRICE_PORTFOLIO_MONTHLY ||
    priceId === env.STRIPE_PRICE_PORTFOLIO_YEARLY
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
