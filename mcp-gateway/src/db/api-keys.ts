/**
 * Typed queries against the `api_keys` table.
 *
 * These are the pure database operations. The higher-level
 * `auth/api-keys.ts` module (step 7) builds on top of them to handle
 * generation, hashing, and plaintext-return-on-creation semantics.
 */

import { allRows, execute, firstRow } from "./client.js";
import type { ApiKeyRow } from "./schema.js";

/** Returns the api_keys row matching the SHA-256 hash, or null. */
export function getApiKeyByHash(
  db: D1Database,
  hash: string,
): Promise<ApiKeyRow | null> {
  return firstRow<ApiKeyRow>(
    db,
    "SELECT * FROM api_keys WHERE key_hash = ?1",
    hash,
  );
}

/** Returns the api_keys row by primary key, or null. */
export function getApiKeyById(
  db: D1Database,
  id: string,
): Promise<ApiKeyRow | null> {
  return firstRow<ApiKeyRow>(db, "SELECT * FROM api_keys WHERE id = ?1", id);
}

/** Lists every API key owned by a user, most recent first. */
export function listApiKeysByUser(
  db: D1Database,
  userId: string,
): Promise<ApiKeyRow[]> {
  return allRows<ApiKeyRow>(
    db,
    "SELECT * FROM api_keys WHERE user_id = ?1 ORDER BY created_at DESC",
    userId,
  );
}

/** Inserts a new API key row. Caller is responsible for hashing. */
export async function insertApiKey(
  db: D1Database,
  row: {
    id: string;
    userId: string;
    name: string;
    keyHash: string;
    keyPrefix: string;
  },
): Promise<ApiKeyRow> {
  const now = Date.now();
  await execute(
    db,
    `INSERT INTO api_keys (id, user_id, name, key_hash, key_prefix, created_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6)`,
    row.id,
    row.userId,
    row.name,
    row.keyHash,
    row.keyPrefix,
    now,
  );
  return {
    id: row.id,
    user_id: row.userId,
    name: row.name,
    key_hash: row.keyHash,
    key_prefix: row.keyPrefix,
    last_used_at: null,
    revoked_at: null,
    created_at: now,
  };
}

/** Marks an API key as revoked. Does nothing if the key is already revoked. */
export async function revokeApiKeyRow(
  db: D1Database,
  id: string,
  userId: string,
): Promise<number> {
  return execute(
    db,
    `UPDATE api_keys
        SET revoked_at = ?1
      WHERE id = ?2
        AND user_id = ?3
        AND revoked_at IS NULL`,
    Date.now(),
    id,
    userId,
  );
}

/** Bumps `last_used_at` to now. Intended for async usage logging. */
export async function touchApiKey(
  db: D1Database,
  id: string,
): Promise<void> {
  await execute(
    db,
    "UPDATE api_keys SET last_used_at = ?1 WHERE id = ?2",
    Date.now(),
    id,
  );
}
