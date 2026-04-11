/**
 * High-level API key helpers: generation, hashing, and CRUD.
 *
 * Layered above `src/db/api-keys.ts` so the DB module stays a pure
 * query layer and this module owns the cryptographic details.
 *
 * Key format: `sv_live_` prefix + 32 characters of RFC 4648 base32
 * (no padding) drawn from 20 cryptographically random bytes.
 *   40 chars prefix + body
 *   200 bits of entropy in the body
 *   base32 alphabet is URL-safe and looks friendly in logs
 *
 * The `sv_live_` prefix is always returned to the user at creation
 * time and stored as `key_prefix` (first 14 chars) in D1 so dashboards
 * and logs can identify a key without exposing the secret.
 *
 * Runtime APIs used: Web Crypto (`crypto.getRandomValues`,
 * `crypto.subtle.digest`). Both are available in Workers, Node 20+,
 * and the Node 22 test runtime.
 */

import {
  getApiKeyByHash,
  insertApiKey,
  listApiKeysByUser,
  revokeApiKeyRow,
} from "../db/api-keys.js";
import { getUserById } from "../db/users.js";
import type { ApiKeyRow, UserTier } from "../db/schema.js";

/** Result of a fresh key generation — the only place plaintext ever exists. */
export interface GeneratedApiKey {
  /** Full plaintext secret (`sv_live_...`). Return to the user ONCE. */
  plaintext: string;
  /** First 14 characters (`sv_live_xxxxxx`) — safe to store and display. */
  prefix: string;
  /** SHA-256 of the plaintext, hex-encoded. Stored in D1. */
  hash: string;
}

/** Default key name used when the caller does not supply one. */
export const DEFAULT_KEY_NAME = "Default";

/** Length of the random body (in base32 characters) after the `sv_live_` prefix. */
const KEY_BODY_LENGTH = 32;

/** Length of the public prefix we store in D1 and show in UIs. */
const KEY_PREFIX_LENGTH = 14;

/** RFC 4648 base32 alphabet (uppercase, no padding). */
const BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

// ── Key generation ─────────────────────────────────────────────────────────

/**
 * Generates a fresh API key secret with cryptographic randomness.
 *
 * The body length ({@link KEY_BODY_LENGTH} = 32 base32 chars) gives us
 * 160 bits of entropy, which is well above the symbolic 128-bit
 * threshold used by industry-standard secret keys.
 */
export async function generateApiKey(): Promise<GeneratedApiKey> {
  // 20 random bytes → 32 base32 chars.
  const bytes = new Uint8Array(20);
  crypto.getRandomValues(bytes);
  const body = base32Encode(bytes).slice(0, KEY_BODY_LENGTH);
  const plaintext = `sv_live_${body}`;
  const prefix = plaintext.slice(0, KEY_PREFIX_LENGTH);
  const hash = await hashApiKey(plaintext);
  return { plaintext, prefix, hash };
}

/**
 * Returns the SHA-256 of an API key as a 64-character hex string.
 * Deterministic and safe to run on the hot auth path.
 */
export async function hashApiKey(plaintext: string): Promise<string> {
  const data = new TextEncoder().encode(plaintext);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return toHex(new Uint8Array(digest));
}

// ── CRUD operations ────────────────────────────────────────────────────────

/**
 * Creates a new API key for the given user.
 *
 * Returns an object containing the brand new row (without plaintext)
 * plus the plaintext value, which is the only moment it will ever be
 * exposed. Caller must ensure the user exists before calling.
 */
export async function createApiKey(
  db: D1Database,
  userId: string,
  name: string | undefined,
): Promise<{ row: ApiKeyRow; plaintext: string }> {
  const generated = await generateApiKey();
  const id = newKeyId();
  const row = await insertApiKey(db, {
    id,
    userId,
    name: name && name.trim() ? name.trim() : DEFAULT_KEY_NAME,
    keyHash: generated.hash,
    keyPrefix: generated.prefix,
  });
  return { row, plaintext: generated.plaintext };
}

/** Lists the API keys owned by a user. Never includes plaintext. */
export function listApiKeys(
  db: D1Database,
  userId: string,
): Promise<ApiKeyRow[]> {
  return listApiKeysByUser(db, userId);
}

/**
 * Marks an API key as revoked.
 *
 * Returns `true` if a row was updated (the key existed, belonged to
 * the user, and was not already revoked), `false` otherwise. Revoking
 * a key already revoked is a no-op but returns `false` so callers can
 * distinguish between the two.
 */
export async function revokeApiKey(
  db: D1Database,
  keyId: string,
  userId: string,
): Promise<boolean> {
  const changes = await revokeApiKeyRow(db, keyId, userId);
  return changes > 0;
}

// ── Validation ─────────────────────────────────────────────────────────────

/** Shape returned by {@link validateApiKey} on success. */
export interface ValidatedApiKey {
  /** The API key row (hash + prefix + id) as stored in D1. */
  key: ApiKeyRow;
  /** The owning user's id. */
  userId: string;
  /** The tier granted to that user right now (users.tier). */
  tier: UserTier;
}

/**
 * Validates a plaintext API key by hashing it and looking up the DB.
 * Returns `null` if the key is unknown or revoked.
 *
 * This function intentionally does NOT cache in KV — the caching layer
 * lives in the auth middleware (step 8) so tests of both halves stay
 * isolated.
 */
export async function validateApiKey(
  db: D1Database,
  plaintext: string,
): Promise<ValidatedApiKey | null> {
  if (!plaintext || !plaintext.startsWith("sv_live_")) return null;
  const hash = await hashApiKey(plaintext);
  const key = await getApiKeyByHash(db, hash);
  if (!key) return null;
  if (key.revoked_at !== null && key.revoked_at !== undefined) return null;

  const user = await getUserById(db, key.user_id);
  if (!user) return null;

  return {
    key,
    userId: user.id,
    tier: user.tier,
  };
}

// ── Helpers ────────────────────────────────────────────────────────────────

/** Encodes a byte array into an uppercase base32 string (no padding). */
function base32Encode(bytes: Uint8Array): string {
  let bits = 0;
  let value = 0;
  let out = "";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += BASE32_ALPHABET[(value >> bits) & 31];
    }
  }
  if (bits > 0) out += BASE32_ALPHABET[(value << (5 - bits)) & 31];
  return out;
}

/** Converts a byte array into a lowercase hex string. */
function toHex(bytes: Uint8Array): string {
  const hex: string[] = [];
  for (const b of bytes) hex.push(b.toString(16).padStart(2, "0"));
  return hex.join("");
}

/** Generates a new API key row id of the form `key_<12 base32 chars>`. */
function newKeyId(): string {
  const bytes = new Uint8Array(8);
  crypto.getRandomValues(bytes);
  return `key_${base32Encode(bytes).slice(0, 12).toLowerCase()}`;
}

/** Generates a new user row id of the form `usr_<12 base32 chars>`. */
export function newUserId(): string {
  const bytes = new Uint8Array(8);
  crypto.getRandomValues(bytes);
  return `usr_${base32Encode(bytes).slice(0, 12).toLowerCase()}`;
}
