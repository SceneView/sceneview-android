/**
 * Hourly sliding-window rate limiter backed by Workers KV.
 *
 * Ported verbatim from mcp-gateway/src/rate-limit/kv-counter.ts with
 * one difference: a distinct KV prefix (`hub-rl:` instead of `rl:`)
 * so the two gateways keep separate counters in the shared KV
 * namespace. A user who has exhausted their Gateway #1 hourly quota
 * can still make requests on the hub — we treat the two gateways as
 * independent budgets per hour, sharing only the monthly aggregate
 * (which is the billed amount).
 *
 * Algorithm: weighted previous-bucket sliding window (Cloudflare
 * reference implementation). At time `t` inside the current hour,
 *
 *   effective = current + previous * (1 - elapsedFraction)
 *
 * Two KV reads + one write per check, ~5% accuracy vs true sliding
 * window — which is explicitly called out as acceptable in the
 * monetisation plan.
 *
 * KV key layout:
 *   hub-rl:{keyHash}:h:{YYYY-MM-DDTHH}  → counter integer (UTC)
 * TTL: 7200 s so the previous bucket stays readable for the first
 *      hour of the following window.
 */

/** KV key prefix used for hub hourly sliding-window buckets. */
export const KV_HOURLY_PREFIX = "hub-rl:";

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
  /** Weighted count after this request, for observability and tests. */
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
 * current bucket. Combines read + write in one call because every
 * hot path wants the same two operations.
 *
 * Tests inject a fixed `nowMs` to make time-travel assertions
 * deterministic.
 *
 * Wrapped in try/catch: KV transient errors degrade to "allowed"
 * rather than blocking every request when the KV namespace is
 * unreachable. A 429 false-negative is strictly worse than a brief
 * rate-limit bypass in an outage scenario.
 */
export async function checkAndIncrementHourly(
  kv: KVNamespace,
  keyHash: string,
  limit: number,
  nowMs: number = Date.now(),
): Promise<RateLimitDecision> {
  const hourMs = 3_600_000;
  const resetAt = Math.floor(nowMs / hourMs) * hourMs + hourMs;

  try {
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

    const elapsedFraction = (nowMs % hourMs) / hourMs;
    const effective = current + previous * (1 - elapsedFraction);

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
  } catch {
    // Fail open: one KV outage should not shut down the hub.
    return { allowed: true, limit, remaining: limit, resetAt, effective: 0 };
  }
}

/** Parses a KV string to integer, returning 0 on null / NaN. */
function parseIntSafe(value: string | null | undefined): number {
  if (!value) return 0;
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}
