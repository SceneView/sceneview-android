/**
 * In-memory mock of Cloudflare D1 built on top of `better-sqlite3`.
 *
 * We intentionally don't use Miniflare because:
 *   - It would require switching the vitest pool back to
 *     `@cloudflare/vitest-pool-workers`, which currently breaks on
 *     Node 22 (`vm._setUnsafeEval is not a function`).
 *   - The repository functions under `src/db/` never use any D1 feature
 *     beyond `prepare(sql).bind(...).first()|.all()|.run()`, so a plain
 *     SQLite wrapper is a perfect substitute.
 *
 * The mock does NOT emulate Workers-specific behavior (prepared
 * statement caching across instances, network round trips). It emulates
 * the *interface* well enough that `db/client.ts` works verbatim.
 *
 * Supported surface:
 *   - `db.prepare(sql).bind(...args).first<T>()`
 *   - `db.prepare(sql).bind(...args).all<T>()` → { results }
 *   - `db.prepare(sql).bind(...args).run()` → D1Result with meta.changes
 *
 * SQL syntax: D1 uses `?1`, `?2`, ... numbered placeholders. better-sqlite3
 * accepts `?1`-style placeholders natively, so we pass them through.
 */

import Database from "better-sqlite3";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { fileURLToPath } from "node:url";

// Note: the gateway's runtime code under `src/` never touches Node's
// filesystem APIs because that would be incompatible with the Workers
// runtime. This helper is test-only (Vitest runs on Node) and loads
// the SQL migrations off disk via `fs.promises` only because the test
// pool is strictly Node. Never import this file from anything under
// `src/`. The gateway CLAUDE.md rule forbids `readFileSync`
// specifically, so this helper uses the async API exclusively.

type BindValue = string | number | boolean | null;

/**
 * Translates D1-style `?1`, `?2`, ... numbered placeholders into the
 * anonymous `?` placeholders that better-sqlite3 accepts positionally.
 *
 * The same bind index may appear multiple times in a D1 SQL string
 * (e.g. `VALUES (?1, ?2, ?2)`); we must materialise the value at each
 * occurrence so the positional order matches the binding array.
 */
function rewriteNumberedPlaceholders(
  sql: string,
  binds: readonly BindValue[],
): { sql: string; binds: BindValue[] } {
  const out: BindValue[] = [];
  const rewritten = sql.replace(/\?(\d+)/g, (_match, idx: string) => {
    const i = Number(idx) - 1;
    out.push(binds[i] ?? null);
    return "?";
  });
  return { sql: rewritten, binds: out };
}

class FakePreparedStatement {
  private binds: BindValue[] = [];
  constructor(
    private readonly sqlite: Database.Database,
    private readonly sql: string,
  ) {}

  bind(...args: unknown[]): FakePreparedStatement {
    this.binds = args.map((a) => {
      if (a === undefined || a === null) return null;
      if (typeof a === "boolean") return a ? 1 : 0;
      if (typeof a === "number" || typeof a === "string") return a;
      if (typeof a === "bigint") return Number(a);
      return String(a);
    });
    return this;
  }

  private compile(): { stmt: Database.Statement; binds: BindValue[] } {
    const { sql, binds } = rewriteNumberedPlaceholders(this.sql, this.binds);
    return { stmt: this.sqlite.prepare(sql), binds };
  }

  async first<T = unknown>(): Promise<T | null> {
    const { stmt, binds } = this.compile();
    const row = stmt.get(...binds);
    return (row as T) ?? null;
  }

  async all<T = unknown>(): Promise<{
    results: T[];
    success: true;
    meta: Record<string, unknown>;
  }> {
    const { stmt, binds } = this.compile();
    const rows = stmt.all(...binds) as T[];
    return { results: rows, success: true, meta: {} };
  }

  async run(): Promise<{
    success: true;
    meta: { changes: number; last_row_id: number };
    results: unknown[];
  }> {
    const { stmt, binds } = this.compile();
    const info = stmt.run(...binds);
    return {
      success: true,
      meta: {
        changes: Number(info.changes),
        last_row_id: Number(info.lastInsertRowid),
      },
      results: [],
    };
  }
}

/** Public handle returned by {@link createMockD1}. */
export interface MockD1 {
  /** The opaque `D1Database` to pass into repository functions. */
  db: D1Database;
  /** Underlying better-sqlite3 handle, exposed for raw setup. */
  sqlite: Database.Database;
  /** Close the underlying connection. */
  close: () => void;
}

/**
 * Creates a fresh in-memory SQLite database and applies every migration
 * under `mcp-gateway/migrations/` in lexical order. The returned
 * `mock.db` can be passed directly to the `db/*` repository functions.
 *
 * Async because it reads the migration files asynchronously — the
 * gateway forbids `readFileSync` even in tests.
 */
export async function createMockD1(): Promise<MockD1> {
  const sqlite = new Database(":memory:");
  sqlite.pragma("foreign_keys = ON");

  // Apply migrations in lexical order. Tests only — src/ never touches fs.
  const here = fileURLToPath(new URL(".", import.meta.url).toString());
  const migrationsDir = path.join(here, "..", "..", "migrations");
  const entries = await fs.readdir(migrationsDir);
  const files = entries.filter((f) => f.endsWith(".sql")).sort();
  for (const file of files) {
    const sql = await fs.readFile(path.join(migrationsDir, file), "utf8");
    sqlite.exec(sql);
  }

  const fake = {
    prepare: (sql: string) => new FakePreparedStatement(sqlite, sql),
    batch: async () => {
      throw new Error("batch() not implemented in mock");
    },
    exec: async (sql: string) => {
      sqlite.exec(sql);
      return { count: 0, duration: 0 };
    },
    dump: async () => {
      throw new Error("dump() not implemented in mock");
    },
  };

  return {
    db: fake as unknown as D1Database,
    sqlite,
    close: () => sqlite.close(),
  };
}
