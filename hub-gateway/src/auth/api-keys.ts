/**
 * API key validation against the shared D1 database.
 *
 * The hub gateway does NOT create or revoke API keys — that
 * responsibility lives entirely on Gateway #1 (`mcp-gateway/`) which
 * owns the Stripe webhook pipeline and the D1 migrations. The hub
 * only READS the `api_keys` and `users` tables to authenticate
 * incoming requests.
 *
 * Flow:
 *   1. Client sends `Authorization: Bearer sv_live_...`
 *   2. Hub hashes the plaintext with SHA-256 (Web Crypto, available
 *      in Workers + Node 20+)
 *   3. Hub queries D1 `api_keys WHERE key_hash = ?`
 *   4. Hub joins against `users` for the current tier
 *   5. Response cached in KV under `hub-auth:{hash}` for 5 minutes
 *      (revocation propagation is bounded by that TTL — same trade
 *      off as Gateway #1)
 *
 * The `hub-auth:` KV prefix is deliberately distinct from Gateway #1's
 * `auth:` prefix so the two caches can evolve independently without
 * contaminating each other.
 */

import type { ApiKeyRow, UserRow, UserTier } from "../db/schema.js";

/** Successful validation result returned by validateApiKey. */
export interface ValidatedApiKey {
  key: ApiKeyRow;
  userId: string;
  tier: UserTier;
}

/** SHA-256 hex digest of a plaintext API key — used as D1 lookup and KV cache key. */
export async function hashApiKey(plaintext: string): Promise<string> {
  const data = new TextEncoder().encode(plaintext);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return toHex(new Uint8Array(digest));
}

/** Valid Stripe-era hub/Gateway #1 keys use this prefix. */
const KEY_PREFIX = "sv_live_";

/**
 * Validates a plaintext API key against D1.
 *
 * Returns `null` if the key is malformed, unknown, revoked, or the
 * owning user has been deleted. The function intentionally does NOT
 * cache — the KV caching layer lives in the middleware so tests of
 * both halves stay isolated.
 */
export async function validateApiKey(
  db: D1Database,
  plaintext: string,
): Promise<ValidatedApiKey | null> {
  if (!plaintext || !plaintext.startsWith(KEY_PREFIX)) return null;
  const hash = await hashApiKey(plaintext);

  // Wrap D1 calls in try/catch so transient errors (D1 outage,
  // uninitialised local sandbox) become 401 "invalid key" instead
  // of leaking 500s to the client. The JSON-RPC contract on /mcp
  // stays clean: a client gets "Unauthorized" and retries with a
  // fresh token.
  try {
    const keyRow = await db
      .prepare(
        "SELECT id, user_id, key_hash, key_prefix, revoked_at FROM api_keys WHERE key_hash = ?1 LIMIT 1",
      )
      .bind(hash)
      .first<ApiKeyRow>();
    if (!keyRow) return null;
    if (keyRow.revoked_at !== null && keyRow.revoked_at !== undefined) {
      return null;
    }

    const userRow = await db
      .prepare("SELECT id, email, tier FROM users WHERE id = ?1 LIMIT 1")
      .bind(keyRow.user_id)
      .first<UserRow>();
    if (!userRow) return null;

    return {
      key: keyRow,
      userId: userRow.id,
      tier: userRow.tier,
    };
  } catch {
    return null;
  }
}

/** Lowercase hex encoder (used by hashApiKey). */
function toHex(bytes: Uint8Array): string {
  const out: string[] = [];
  for (const b of bytes) out.push(b.toString(16).padStart(2, "0"));
  return out.join("");
}
