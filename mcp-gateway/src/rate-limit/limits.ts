/**
 * Per-tier rate limit constants.
 *
 * Numbers come from section 2.2 of the monetisation plan:
 *   Free: 60/h, 1 000/mo
 *   Pro : 600/h, 10 000/mo
 *   Team: 3 000/h, 50 000/mo
 *
 * Keeping them in one place (and exporting them) lets the dashboard,
 * the middleware, and the tests all read the same source of truth.
 */

import type { UserTier } from "../db/schema.js";

/** Shape of the limit bag used by the rate limiter and quota checker. */
export interface TierLimits {
  /** Max requests in any 1-hour sliding window. */
  hourly: number;
  /** Max requests in the current calendar month. */
  monthly: number;
  /** Max number of active API keys a user on this tier can own. */
  maxKeys: number;
}

/** Full per-tier table. */
export const TIER_LIMITS: Record<UserTier, TierLimits> = {
  free: { hourly: 60, monthly: 1000, maxKeys: 1 },
  pro: { hourly: 600, monthly: 10_000, maxKeys: 3 },
  team: { hourly: 3000, monthly: 50_000, maxKeys: 10 },
};

/** Safe accessor that falls back to the free tier on unknown input. */
export function getLimitsForTier(tier: string | UserTier): TierLimits {
  return TIER_LIMITS[tier as UserTier] ?? TIER_LIMITS.free;
}
