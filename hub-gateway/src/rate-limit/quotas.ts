/**
 * Monthly quota check, backed by D1 aggregation with a KV cache.
 *
 * Ported from mcp-gateway/src/rate-limit/quotas.ts with:
 *   - Distinct KV prefix (`hub-quota:` vs `quota:`) so the two
 *     gateways keep independent counters in the shared KV namespace.
 *     This is intentional: the hourly budgets are separate, and the
 *     monthly aggregate is reported per-gateway until a follow-up
 *     sprint unifies it under a single "portfolio" counter.
 *   - `fail open` on transient KV errors — the hub never blocks a
 *     paying user on a caching outage.
 *
 * Hot path:
 *   1. Read `hub-quota:{hash}:{YYYY-MM}` from KV.
 *   2. On hit, compare to limit. Done.
 *   3. On miss, run the aggregate against D1 and cache with 40d TTL.
 *
 * Mutation path (from usage logger):
 *   - `incrementQuotaCache(kv, hash, month)` bumps the cached
 *     counter by 1 after a SUCCESSFUL tool call. Called after the
 *     usage record is inserted so the next check doesn't re-hit D1.
 *
 * We count `status = 'ok'` only — denied / rate_limited / error
 * calls do NOT burn monthly budget. Same semantic as Gateway #1.
 */

import { countSuccessfulUsageInMonth } from "../db/usage.js";

/** KV prefix for hub monthly quota cache entries. */
export const HUB_QUOTA_CACHE_PREFIX = "hub-quota:";

/** TTL applied to quota cache entries (40 days — longer than a month). */
export const HUB_QUOTA_CACHE_TTL_SECONDS = 40 * 24 * 3600;

/** Result of a monthly quota check. */
export interface QuotaDecision {
  /** Whether the call is allowed under the monthly limit. */
  allowed: boolean;
  /** Monthly limit for this tier. */
  limit: number;
  /** Number of successful tool calls so far in the current month. */
  used: number;
  /** Number of calls remaining before hitting the cap. */
  remaining: number;
}

/** Builds the KV key for a monthly counter. */
export function quotaKey(keyHash: string, monthBucket: string): string {
  return `${HUB_QUOTA_CACHE_PREFIX}${keyHash}:${monthBucket}`;
}

/** Reads the current monthly usage, consulting KV first and D1 on miss. */
export async function getMonthlyUsage(
  kv: KVNamespace,
  db: D1Database,
  keyId: string,
  keyHash: string,
  monthBucket: string,
): Promise<number> {
  try {
    const cached = await kv.get(quotaKey(keyHash, monthBucket));
    if (cached !== null) {
      const n = Number(cached);
      if (Number.isFinite(n)) return n;
    }
  } catch {
    // KV read failure → fall through to D1.
  }

  const fresh = await countSuccessfulUsageInMonth(db, keyId, monthBucket);
  try {
    await kv.put(quotaKey(keyHash, monthBucket), String(fresh), {
      expirationTtl: HUB_QUOTA_CACHE_TTL_SECONDS,
    });
  } catch {
    // KV write failure → the next request will re-count from D1, fine.
  }
  return fresh;
}

/** Returns a decision object for the current monthly quota. */
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

/** Bumps the cached quota counter by 1 (fire-and-forget from /mcp). */
export async function incrementQuotaCache(
  kv: KVNamespace,
  keyHash: string,
  monthBucket: string,
): Promise<void> {
  try {
    const key = quotaKey(keyHash, monthBucket);
    const raw = await kv.get(key);
    const current = raw ? Number(raw) : 0;
    const next = Number.isFinite(current) ? current + 1 : 1;
    await kv.put(key, String(next), {
      expirationTtl: HUB_QUOTA_CACHE_TTL_SECONDS,
    });
  } catch {
    // Degrade silently — the next check will re-aggregate from D1.
  }
}
