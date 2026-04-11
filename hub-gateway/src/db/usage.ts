/**
 * Typed queries against the shared `usage_records` table.
 *
 * The hub is a READ-WRITE consumer of `usage_records`: it inserts a
 * row per tool call and runs an aggregate `COUNT(*)` for monthly
 * quota enforcement. Gateway #1 owns the table schema and migrations
 * (see mcp-gateway/migrations/) — this module is a subset adapter.
 *
 * Hot paths:
 *   - `insertUsageRecord` runs on every /mcp call (fire-and-forget
 *     friendly, never throws into the response path).
 *   - `countSuccessfulUsageInMonth` runs on the first call of the
 *     month for a given key (then the KV cache serves until next
 *     month). The `idx_usage_key_month` composite index on
 *     `(api_key_id, bucket_month)` makes it O(1).
 *
 * All DB calls are wrapped in try/catch at the boundary so a
 * transient D1 outage degrades to "no quota / no log" instead of
 * 500-ing paying customers. The contract with the billing team is
 * explicit: under-count in emergencies, never over-count.
 */

/** Monthly bucket string `YYYY-MM` for a unix epoch ms (UTC). */
export function monthBucket(nowMs: number = Date.now()): string {
  const d = new Date(nowMs);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}

/** Usage record row subset the hub reads from / writes to. */
export type UsageStatus = "ok" | "denied" | "rate_limited" | "error";
export type UsageTierRequired = "free" | "pro";

/** Insert a usage record into the shared D1 table (fire-and-forget). */
export async function insertUsageRecord(
  db: D1Database,
  row: {
    apiKeyId: string;
    userId: string;
    toolName: string;
    tierRequired: UsageTierRequired;
    status: UsageStatus;
    bucketMonth?: string;
  },
): Promise<void> {
  try {
    await db
      .prepare(
        `INSERT INTO usage_records
           (api_key_id, user_id, tool_name, tier_required, status, bucket_month, created_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)`,
      )
      .bind(
        row.apiKeyId,
        row.userId,
        row.toolName,
        row.tierRequired,
        row.status,
        row.bucketMonth ?? monthBucket(),
        Date.now(),
      )
      .run();
  } catch {
    // Swallow — logging must never break the user response.
  }
}

/** Count `status = 'ok'` records for a key in a given month bucket. */
export async function countSuccessfulUsageInMonth(
  db: D1Database,
  apiKeyId: string,
  bucketMonth: string,
): Promise<number> {
  try {
    const row = await db
      .prepare(
        `SELECT COUNT(*) AS count
           FROM usage_records
          WHERE api_key_id = ?1
            AND bucket_month = ?2
            AND status = 'ok'`,
      )
      .bind(apiKeyId, bucketMonth)
      .first<{ count: number }>();
    const n = Number(row?.count ?? 0);
    return Number.isFinite(n) ? n : 0;
  } catch {
    // Fail open: an outage should let the user through, not 429 them.
    return 0;
  }
}
