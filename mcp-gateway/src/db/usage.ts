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

/**
 * Returns daily successful call counts for a user over the last N days
 * (inclusive of today). Emits a dense series — days with no calls are
 * returned with `count: 0` so the dashboard sparkline stays smooth.
 */
export async function listDailyUsage(
  db: D1Database,
  userId: string,
  days: number,
  nowMs: number = Date.now(),
): Promise<Array<{ day: string; count: number }>> {
  const end = nowMs;
  const start = end - days * 24 * 60 * 60 * 1000;
  const rows = await db
    .prepare(
      `SELECT strftime('%Y-%m-%d', created_at / 1000, 'unixepoch') AS day,
              COUNT(*) AS count
         FROM usage_records
        WHERE user_id = ?1
          AND status = 'ok'
          AND created_at >= ?2
        GROUP BY day
        ORDER BY day ASC`,
    )
    .bind(userId, start)
    .all<{ day: string; count: number }>();

  const map = new Map<string, number>();
  for (const r of rows.results ?? []) {
    map.set(r.day, Number(r.count));
  }
  const series: Array<{ day: string; count: number }> = [];
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(end - i * 24 * 60 * 60 * 1000);
    const iso = d.toISOString().slice(0, 10);
    series.push({ day: iso, count: map.get(iso) ?? 0 });
  }
  return series;
}
