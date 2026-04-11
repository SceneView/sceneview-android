/**
 * Hourly sliding-window rate limiter backed by Workers KV.
 *
 * The implementation follows the weighted-previous-bucket algorithm
 * commonly used by Cloudflare reference implementations: at time `t`
 * inside the current hour bucket, the effective count is
 *
 *   current + previous * (1 - elapsedFraction)
 *
 * where `elapsedFraction` is the share of the current hour that has
 * already passed. It is accurate to within ~5 % of a true sliding
 * window and requires only two KV reads per check.
 *
 * KV key layout:
 *   rl:{keyHash}:h:{YYYY-MM-DDTHH}  → counter integer
 * TTL: 7200 s so the previous bucket is still readable for the first
 *      hour of the following window.
 *
 * We deliberately do NOT use compare-and-swap or locks: a few extra
 * requests during a tight burst are acceptable (the plan calls out
 * ±5 % tolerance explicitly) and the alternative would cost an extra
 * KV round trip per request.
 *
 * The API is designed to return a rich decision object so the
 * middleware can populate `X-RateLimit-*` response headers.
 */

/** KV key prefix used for hourly sliding-window buckets. */
export const KV_HOURLY_PREFIX = "rl:";

/** TTL applied to hourly bucket writes (2 hours). */
export const KV_HOURLY_TTL_SECONDS = 7200;

/** Result of a rate limit check. */
export interface RateLimitDecision {
  /** Whether the request should be allowed. */
  allowed: boolean;
  /** Hourly limit used to make the decision. */
  limit: number;
  /** Approximate number of requests remaining in the current window. */
  remaining: number;
  /** Unix epoch ms at which the current window resets (top of next hour). */
  resetAt: number;
  /** The weighted count after this request, for observability/testing. */
  effective: number;
}

/** Builds the KV key for a given key hash and ISO hour string. */
export function hourlyBucketKey(keyHash: string, isoHour: string): string {
  return `${KV_HOURLY_PREFIX}${keyHash}:h:${isoHour}`;
}

/** Returns the ISO hour bucket string `YYYY-MM-DDTHH` in UTC. */
export function isoHourBucket(nowMs: number = Date.now()): string {
  return new Date(nowMs).toISOString().slice(0, 13);
}

/** Returns the ISO hour bucket for (now - 1 hour) in UTC. */
export function previousIsoHourBucket(nowMs: number = Date.now()): string {
  return isoHourBucket(nowMs - 3_600_000);
}

/**
 * Checks the hourly sliding window and, if allowed, increments the
 * current bucket. This combines read + write in one call because
 * every hot path wants the same two operations.
 *
 * Tests can inject a fixed `nowMs` to make time-travel assertions
 * deterministic.
 */
export async function checkAndIncrementHourly(
  kv: KVNamespace,
  keyHash: string,
  limit: number,
  nowMs: number = Date.now(),
): Promise<RateLimitDecision> {
  const curBucket = isoHourBucket(nowMs);
  const prevBucket = previousIsoHourBucket(nowMs);
  const curKey = hourlyBucketKey(keyHash, curBucket);
  const prevKey = hourlyBucketKey(keyHash, prevBucket);

  const [curRaw, prevRaw] = await Promise.all([
    kv.get(curKey),
    kv.get(prevKey),
  ]);
  const current = parseIntSafe(curRaw);
  const previous = parseIntSafe(prevRaw);

  // Fraction of the current hour that has elapsed so far.
  const hourMs = 3_600_000;
  const elapsedFraction = (nowMs % hourMs) / hourMs;
  const effective = current + previous * (1 - elapsedFraction);

  const resetAt = Math.floor(nowMs / hourMs) * hourMs + hourMs;

  if (effective + 1 > limit) {
    return {
      allowed: false,
      limit,
      remaining: Math.max(0, Math.floor(limit - effective)),
      resetAt,
      effective,
    };
  }

  const newCurrent = current + 1;
  await kv.put(curKey, String(newCurrent), {
    expirationTtl: KV_HOURLY_TTL_SECONDS,
  });

  return {
    allowed: true,
    limit,
    remaining: Math.max(0, Math.floor(limit - (effective + 1))),
    resetAt,
    effective: effective + 1,
  };
}

// ── Internal helpers ───────────────────────────────────────────────────────

/** Parses a KV string to integer, returning 0 on null / NaN. */
function parseIntSafe(value: string | null | undefined): number {
  if (!value) return 0;
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}
