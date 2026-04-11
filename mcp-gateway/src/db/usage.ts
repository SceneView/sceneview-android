/**
 * Typed queries against the `usage_records` table.
 *
 * The single hot query is `countUsageInMonth`, which is called on
 * every monthly quota check. The `idx_usage_key_month` composite
 * index on `(api_key_id, bucket_month)` makes that query an O(1)
 * lookup regardless of table size.
 */

import { count, execute } from "./client.js";
import type { UsageRecordRow } from "./schema.js";

/** Returns the `YYYY-MM` bucket string for a unix epoch ms. */
export function monthBucket(nowMs: number = Date.now()): string {
  const d = new Date(nowMs);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}

/** Inserts a new usage record. Returns nothing on purpose (fire-and-forget friendly). */
export async function insertUsageRecord(
  db: D1Database,
  row: {
    apiKeyId: string;
    userId: string;
    toolName: string;
    tierRequired: UsageRecordRow["tier_required"];
    status: UsageRecordRow["status"];
    bucketMonth?: string;
  },
): Promise<void> {
  await execute(
    db,
    `INSERT INTO usage_records
       (api_key_id, user_id, tool_name, tier_required, status, bucket_month, created_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)`,
    row.apiKeyId,
    row.userId,
    row.toolName,
    row.tierRequired,
    row.status,
    row.bucketMonth ?? monthBucket(),
    Date.now(),
  );
}

/** Counts usage records for a given API key in the given month bucket. */
export function countUsageInMonth(
  db: D1Database,
  apiKeyId: string,
  bucketMonth: string,
): Promise<number> {
  return count(
    db,
    `SELECT COUNT(*) AS count
       FROM usage_records
      WHERE api_key_id = ?1
        AND bucket_month = ?2`,
    apiKeyId,
    bucketMonth,
  );
}

/** Counts only the `ok` records. Useful to exclude denied / rate-limited from the monthly quota. */
export function countSuccessfulUsageInMonth(
  db: D1Database,
  apiKeyId: string,
  bucketMonth: string,
): Promise<number> {
  return count(
    db,
    `SELECT COUNT(*) AS count
       FROM usage_records
      WHERE api_key_id = ?1
        AND bucket_month = ?2
        AND status = 'ok'`,
    apiKeyId,
    bucketMonth,
  );
}
