/**
 * Simple sliding-window rate limiter using KV.
 *
 * Key: `rl:<ip-hash>:<minute-bucket>`
 * Value: counter (stringified integer)
 * TTL: 120s (2 minutes — covers current + previous bucket)
 *
 * We hash the IP with a salted prefix so we never store raw IPs.
 *
 * NOTE — TOCTOU / non-atomic: the KV get and put are two separate operations.
 * A burst of concurrent requests can each read count=0 and all increment to 1,
 * effectively bypassing the limit for that race window. This is intentional —
 * KV is eventually consistent by design, and for telemetry rate limiting an
 * approximate limit is acceptable. Use Durable Objects if you need strict
 * atomicity.
 */

const MAX_REQUESTS_PER_MINUTE = 30;
const BUCKET_TTL_SECONDS = 120;

/**
 * Fixed salt prefix prepended before hashing. Not cryptographic — the goal is
 * simply to make keys non-reversible at a glance in KV storage. A random salt
 * baked at build-time is sufficient for this purpose; a per-request secret
 * would only matter if we needed anonymization guarantees (we don't here).
 */
const HASH_SALT = "sv-rl-2024:";

function hashIp(ip: string): string {
  const salted = HASH_SALT + ip;
  let hash = 0x811c9dc5;
  for (let i = 0; i < salted.length; i++) {
    hash ^= salted.charCodeAt(i);
    hash = (hash * 0x01000193) >>> 0;
  }
  return hash.toString(36);
}

function minuteBucket(): string {
  return Math.floor(Date.now() / 60_000).toString(36);
}

export async function isRateLimited(
  ip: string,
  kv: KVNamespace,
): Promise<boolean> {
  const key = `rl:${hashIp(ip)}:${minuteBucket()}`;

  const current = await kv.get(key);
  const count = current ? parseInt(current, 10) : 0;

  if (count >= MAX_REQUESTS_PER_MINUTE) {
    return true;
  }

  // Increment — fire-and-forget (waitUntil would be ideal but KV put is fast)
  await kv.put(key, String(count + 1), {
    expirationTtl: BUCKET_TTL_SECONDS,
  });

  return false;
}
