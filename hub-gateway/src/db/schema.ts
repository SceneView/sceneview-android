/**
 * TypeScript row types mirroring the D1 schema shared with Gateway #1.
 *
 * D1 is owned by mcp-gateway/ — the authoritative migrations live in
 * `mcp-gateway/migrations/`. These types are a **subset** of
 * mcp-gateway/src/db/schema.ts, limited to what the hub gateway
 * actually reads today (users + api_keys). Keep them in sync with
 * the owning module; if a column changes there, update this file too.
 *
 * Subscription tier semantics used by the hub:
 *
 *   - `free`        — no paid access, hub denies Pro tools
 *   - `pro`         — paid access, hub treats this as "Portfolio
 *                     Access" regardless of which Stripe product
 *                     paid for it (Gateway #1 sceneview-mcp Pro and
 *                     Gateway #2 Portfolio Access both set tier=pro
 *                     in the shared users table)
 *   - `team`        — enterprise tier, unlocks everything
 */

/** Subscription tier stored in users.tier. */
export type UserTier = "free" | "pro" | "team";

/** Row in the `users` table (subset actually read by the hub). */
export interface UserRow {
  id: string;
  email: string;
  tier: UserTier;
}

/** Row in the `api_keys` table (subset actually read by the hub). */
export interface ApiKeyRow {
  id: string;
  user_id: string;
  key_hash: string;
  key_prefix: string;
  revoked_at: number | null;
}
