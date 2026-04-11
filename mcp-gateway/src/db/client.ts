/**
 * Thin typed wrapper around Cloudflare D1.
 *
 * The goal is not to be an ORM — we just want a handful of tiny helpers
 * that narrow `D1Database`'s `unknown`-heavy return types down to our
 * domain row types, and make the repository functions under
 * `./users.ts`, `./api-keys.ts`, ... read like plain queries.
 *
 * All helpers are `async` and accept a `D1Database` directly rather
 * than wrapping it in a class: this keeps the surface friendly to
 * functional composition, test mocks, and `ctx.waitUntil` from Hono.
 */

/** Runs a SELECT statement and returns the first row or null. */
export async function firstRow<Row>(
  db: D1Database,
  sql: string,
  ...binds: unknown[]
): Promise<Row | null> {
  const stmt = db.prepare(sql).bind(...binds);
  const row = await stmt.first<Row>();
  return row ?? null;
}

/** Runs a SELECT statement and returns all rows as a typed array. */
export async function allRows<Row>(
  db: D1Database,
  sql: string,
  ...binds: unknown[]
): Promise<Row[]> {
  const stmt = db.prepare(sql).bind(...binds);
  const { results } = await stmt.all<Row>();
  return results ?? [];
}

/**
 * Runs an INSERT/UPDATE/DELETE statement.
 *
 * Returns the D1 meta row that contains `changes`, `last_row_id`, etc.
 * Prefer {@link execute} for read-only counts where you only need the
 * number of affected rows.
 */
export async function run(
  db: D1Database,
  sql: string,
  ...binds: unknown[]
): Promise<D1Result> {
  const stmt = db.prepare(sql).bind(...binds);
  return stmt.run();
}

/**
 * Runs a write statement and returns the `meta.changes` count directly
 * for call sites that don't care about the full meta payload.
 */
export async function execute(
  db: D1Database,
  sql: string,
  ...binds: unknown[]
): Promise<number> {
  const result = await run(db, sql, ...binds);
  const changes =
    (result.meta?.changes as number | undefined) ??
    (result.meta as unknown as { changes?: number })?.changes ??
    0;
  return changes;
}

/** Runs a COUNT(*) query and returns the integer value. */
export async function count(
  db: D1Database,
  sql: string,
  ...binds: unknown[]
): Promise<number> {
  const row = await firstRow<{ count: number }>(db, sql, ...binds);
  if (!row) return 0;
  // Some D1 drivers return the count column as bigint-ish; coerce.
  const raw = (row as unknown as Record<string, unknown>).count;
  return typeof raw === "number" ? raw : Number(raw ?? 0);
}
