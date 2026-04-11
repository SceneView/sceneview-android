/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import type { UserTier } from "../../db/schema.js";

/**
 * Small coloured pill showing the user's current tier.
 * Uses the shared CSS classes from `layout.tsx`.
 */
export const TierBadge: FC<{ tier: UserTier }> = ({ tier }) => (
  <span class={`tier-badge tier-badge--${tier}`}>{tier}</span>
);
