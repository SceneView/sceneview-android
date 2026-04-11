/**
 * Per-tier rate limit constants for the hub gateway.
 *
 * The hub is more generous than Gateway #1 because the Portfolio
 * Access plan aggregates the whole non-SceneView portfolio under one
 * sub — users routinely hit several packages in a single
 * conversation (architecture + realestate + ecommerce-3d for a
 * staging workflow, for example). A stingy limit would break that
 * multi-package value proposition.
 *
 * Numbers match the /pricing landing page:
 *   Free:      100 calls/month = ~5/h  (anti-abuse, not usable in prod)
 *   Portfolio: 20 000/month = ~700/h   (29 EUR/mo)
 *   Team:      100 000/month = ~3500/h (79 EUR/mo, 5 seats)
 *
 * The monthly quota is enforced in a separate middleware once D1
 * usage logging lands. Until then, only the hourly sliding window
 * runs — which is still a strong defence against runaway loops.
 */

import type { UserTier } from "../db/schema.js";

/** Shape of the limit bag used by the rate limiter. */
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
  free: { hourly: 5, monthly: 100, maxKeys: 1 },
  // `pro` is the D1 users.tier value for Portfolio Access subscribers.
  // The hub treats it as "Portfolio 29 EUR/mo" regardless of which
  // Stripe product paid for it (Gateway #1 Pro and Gateway #2
  // Portfolio both set tier=pro in the shared users table).
  pro: { hourly: 700, monthly: 20_000, maxKeys: 3 },
  team: { hourly: 3500, monthly: 100_000, maxKeys: 10 },
};

/** Safe accessor that falls back to the free tier on unknown input. */
export function getLimitsForTier(tier: string | UserTier): TierLimits {
  return TIER_LIMITS[tier as UserTier] ?? TIER_LIMITS.free;
}
