/**
 * Monthly quota check, backed by D1 aggregation with a KV cache.
 *
 * Hot path:
 *   1. Read `quota:{keyHash}:{YYYY-MM}` from KV.
 *   2. On hit, compare to limit. Done.
 *   3. On miss, run `SELECT COUNT(*) FROM usage_records WHERE ...`
 *      against D1 and cache the value with a 40-day TTL.
 *
 * Mutation path (on every usage log):
 *   - `incrementQuotaCache(kv, keyHash, month)` bumps the cached
 *     counter so the next check sees the new value without re-hitting
 *     D1. Callers should call this AFTER inserting a usage record.
 *
 * We count `status = 'ok'` only so denied/rate-limited attempts don't
 * burn a user's monthly budget. The `monthBucket` helper lives in
 * `src/db/usage.ts` — tests can inject it to time-travel.
 */

import type { UsageRecordRow } from "../db/schema.js";
import { countSuccessfulUsageInMonth } from "../db/usage.js";

/** KV key prefix for monthly quota cache. */
export const QUOTA_CACHE_PREFIX = "quota:";

/** TTL applied to quota cache entries (40 days — slightly longer than a month). */
export const QUOTA_CACHE_TTL_SECONDS = 40 * 24 * 3600;

/** Result of a monthly quota check. */
export interface QuotaDecision {
  /** Whether the call is allowed under the monthly limit. */
  allowed: boolean;
  /** Monthly limit for this tier. */
  limit: number;
  /** Number of requests used so far in the current month. */
  used: number;
  /** Number of requests remaining before hitting the cap. */
  remaining: number;
}

/** Builds the KV key for a monthly counter. */
export function quotaKey(keyHash: string, monthBucket: string): string {
  return `${QUOTA_CACHE_PREFIX}${keyHash}:${monthBucket}`;
}

/** Reads the current monthly usage for a key, consulting KV first and D1 on miss. */
export async function getMonthlyUsage(
  kv: KVNamespace,
  db: D1Database,
  keyId: string,
  keyHash: string,
  monthBucket: string,
): Promise<number> {
  const cached = await kv.get(quotaKey(keyHash, monthBucket));
  if (cached !== null) {
    const n = Number(cached);
    if (Number.isFinite(n)) return n;
  }
  const fresh = await countSuccessfulUsageInMonth(db, keyId, monthBucket);
  await kv.put(quotaKey(keyHash, monthBucket), String(fresh), {
    expirationTtl: QUOTA_CACHE_TTL_SECONDS,
  });
  return fresh;
}

/**
 * Returns a decision object for the current monthly quota.
 *
 * Does not mutate the cache — call {@link incrementQuotaCache} from
 * your usage-logging path after a successful tool call.
 */
export async function checkMonthlyQuota(
  kv: KVNamespace,
  db: D1Database,
  keyId: string,
  keyHash: string,
  monthBucket: string,
  limit: number,
): Promise<QuotaDecision> {
  const used = await getMonthlyUsage(kv, db, keyId, keyHash, monthBucket);
  return {
    allowed: used < limit,
    limit,
    used,
    remaining: Math.max(0, limit - used),
  };
}

/** Bumps the quota cache counter by 1 (fire-and-forget from usage logging). */
export async function incrementQuotaCache(
  kv: KVNamespace,
  keyHash: string,
  monthBucket: string,
): Promise<void> {
  const key = quotaKey(keyHash, monthBucket);
  const raw = await kv.get(key);
  const current = raw ? Number(raw) : 0;
  const next = Number.isFinite(current) ? current + 1 : 1;
  await kv.put(key, String(next), {
    expirationTtl: QUOTA_CACHE_TTL_SECONDS,
  });
}

/** Re-exports the tier row types so tests don't need deep imports. */
export type { UsageRecordRow };
