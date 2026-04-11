/**
 * TypeScript row types mirroring the D1 migrations under `mcp-gateway/migrations/`.
 *
 * D1 rows come back as `Record<string, unknown>`; these types are the
 * runtime shape we project them into. If you edit a migration, edit the
 * matching interface here too — there is no runtime validation.
 */

/** Tier granted to a user (mirrors the string stored in `users.tier`). */
export type UserTier = "free" | "pro" | "team";

/** Row in the `users` table. */
export interface UserRow {
  id: string;
  email: string;
  stripe_customer_id: string | null;
  tier: UserTier;
  created_at: number;
  updated_at: number;
}

/** Row in the `subscriptions` table. */
export interface SubscriptionRow {
  id: string;
  user_id: string;
  stripe_subscription_id: string;
  stripe_price_id: string;
  tier: UserTier;
  status: "active" | "trialing" | "past_due" | "canceled" | string;
  current_period_end: number;
  cancel_at_period_end: number;
  created_at: number;
  updated_at: number;
}

/** Row in the `api_keys` table. */
export interface ApiKeyRow {
  id: string;
  user_id: string;
  name: string;
  key_hash: string;
  key_prefix: string;
  last_used_at: number | null;
  revoked_at: number | null;
  created_at: number;
}

/** Row in the `usage_records` table. */
export interface UsageRecordRow {
  id: number;
  api_key_id: string;
  user_id: string;
  tool_name: string;
  tier_required: "free" | "pro";
  status: "ok" | "denied" | "rate_limited" | "error" | string;
  bucket_month: string;
  created_at: number;
}

/** Row in the `magic_links` table. */
export interface MagicLinkRow {
  token_hash: string;
  email: string;
  expires_at: number;
  consumed_at: number | null;
}
