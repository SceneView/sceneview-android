// Scheduled rollup — call this from a Cloudflare Cron Trigger.
//
// Recommended cron schedule in wrangler.toml:
//   [[triggers.crons]]
//   crons = ["0 2 * * *"]   # 02:00 UTC daily, after midnight UTC rolls over
//
// Wire up in the default export by handling the ScheduledEvent:
//   export default {
//     fetch: app.fetch,
//     async scheduled(_event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
//       ctx.waitUntil(rollupYesterday(env.DB));
//     },
//   };

import type { Env } from "./env.js";

/** Date string for yesterday in YYYY-MM-DD format (UTC). */
function yesterday(): string {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - 1);
  return d.toISOString().slice(0, 10);
}

/**
 * Aggregates yesterday's raw events into `daily_rollups` and then deletes raw
 * events older than 90 days from the `events` table.
 *
 * Safe to call multiple times — INSERT OR REPLACE overwrites any existing
 * rollup row for the same (date, event, client, mcp_ver, tier, tool) key,
 * so a retry after a partial failure is idempotent.
 */
export async function rollupYesterday(db: D1Database): Promise<void> {
  const date = yesterday();

  // ── Step 1: aggregate yesterday's raw events ─────────────────────────────
  // Group by every dimension that matters for downstream analysis.
  const rows = await db
    .prepare(
      `SELECT
         date(ingested)  AS date,
         event,
         client,
         mcp_ver,
         tier,
         COALESCE(tool, '') AS tool,
         COUNT(*)           AS count
       FROM events
       WHERE date(ingested) = ?
       GROUP BY date(ingested), event, client, mcp_ver, tier, COALESCE(tool, '')`,
    )
    .bind(date)
    .all<{
      date: string;
      event: string;
      client: string;
      mcp_ver: string;
      tier: string;
      tool: string;
      count: number;
    }>();

  // ── Step 2: upsert each aggregated row into daily_rollups ─────────────────
  if (rows.results.length > 0) {
    const upsert = db.prepare(
      `INSERT OR REPLACE INTO daily_rollups
         (date, event, client, mcp_ver, tier, tool, count)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    );

    await db.batch(
      rows.results.map((r) =>
        upsert.bind(r.date, r.event, r.client, r.mcp_ver, r.tier, r.tool, r.count),
      ),
    );
  }

  // ── Step 3: purge raw events older than 90 days ───────────────────────────
  await db
    .prepare(`DELETE FROM events WHERE ingested < datetime('now', '-90 days')`)
    .run();
}
