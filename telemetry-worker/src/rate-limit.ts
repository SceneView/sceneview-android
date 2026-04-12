import type { Env } from "./env.js";

/**
 * Simple sliding-window rate limiter using KV.
 *
 * Key: `rl:<ip-hash>:<minute-bucket>`
 * Value: counter (stringified integer)
 * TTL: 120s (2 minutes — covers current + previous bucket)
 *
 * We hash the IP with a simple FNV-1a so we never store raw IPs.
 */

const MAX_REQUESTS_PER_MINUTE = 30;
const BUCKET_TTL_SECONDS = 120;

function fnv1aHash(str: string): string {
  let hash = 0x811c9dc5;
  for (let i = 0; i < str.length; i++) {
    hash ^= str.charCodeAt(i);
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
  const key = `rl:${fnv1aHash(ip)}:${minuteBucket()}`;

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
