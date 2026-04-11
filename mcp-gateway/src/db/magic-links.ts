/**
 * Typed queries against the `magic_links` table.
 *
 * Magic link tokens are generated as random base32 strings and stored
 * hashed so a DB dump never grants account access. `consumed_at` is
 * written atomically on login to prevent replay.
 */

import { execute, firstRow } from "./client.js";
import type { MagicLinkRow } from "./schema.js";

/** Inserts a new magic link row. */
export async function insertMagicLink(
  db: D1Database,
  row: { tokenHash: string; email: string; expiresAt: number },
): Promise<void> {
  await execute(
    db,
    `INSERT INTO magic_links (token_hash, email, expires_at)
     VALUES (?1, ?2, ?3)`,
    row.tokenHash,
    row.email,
    row.expiresAt,
  );
}

/** Returns the magic link row matching a hashed token, or null. */
export function getMagicLink(
  db: D1Database,
  tokenHash: string,
): Promise<MagicLinkRow | null> {
  return firstRow<MagicLinkRow>(
    db,
    "SELECT * FROM magic_links WHERE token_hash = ?1",
    tokenHash,
  );
}

/**
 * Marks a magic link as consumed. Returns the number of rows updated,
 * which is 1 on first consumption and 0 on replay.
 */
export async function consumeMagicLink(
  db: D1Database,
  tokenHash: string,
): Promise<number> {
  return execute(
    db,
    `UPDATE magic_links
        SET consumed_at = ?1
      WHERE token_hash = ?2
        AND consumed_at IS NULL`,
    Date.now(),
    tokenHash,
  );
}

/** Deletes expired, consumed magic links older than the given ts. */
export async function deleteExpiredMagicLinks(
  db: D1Database,
  beforeMs: number,
): Promise<number> {
  return execute(
    db,
    `DELETE FROM magic_links WHERE expires_at < ?1 OR consumed_at IS NOT NULL`,
    beforeMs,
  );
}
