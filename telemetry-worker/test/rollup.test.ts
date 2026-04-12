import { describe, it, expect, beforeEach } from "vitest";
import { rollupYesterday } from "../src/rollup.js";
import { createMockD1 } from "./helpers/mock-d1.js";

// Helper: build a grouped row as returned by the SELECT aggregate query.
function makeRow(overrides: Record<string, unknown> = {}) {
  return {
    date: "2026-04-11",
    event: "tool",
    client: "claude-desktop",
    mcp_ver: "4.0.0-rc.1",
    tier: "free",
    tool: "get_node_info",
    count: 5,
    ...overrides,
  };
}

describe("rollupYesterday", () => {
  let db: ReturnType<typeof createMockD1>;

  beforeEach(() => {
    db = createMockD1();
  });

  // ── Test 1: aggregates events correctly ──────────────────────────────────

  it("aggregates events — inserts one row per grouped result", async () => {
    db._rows = [
      makeRow({ count: 12 }),
      makeRow({ event: "init", tool: null, count: 3 }),
    ];

    await rollupYesterday(db as unknown as D1Database);

    // Statement 0: the SELECT aggregate query
    // Statements 1 & 2: the two INSERT OR REPLACE rows (via batch)
    // Statement 3: the DELETE purge
    const inserts = db._statements.filter((s) =>
      s.sql.includes("INSERT OR REPLACE INTO daily_rollups"),
    );
    // Two rows → two batch INSERT statements recorded
    expect(inserts).toHaveLength(2);

    // Both must use the INSERT OR REPLACE SQL
    for (const ins of inserts) {
      expect(ins.sql).toContain("INSERT OR REPLACE INTO daily_rollups");
      expect(ins.params).toHaveLength(7);
    }
  });

  it("passes yesterday's date to the SELECT query", async () => {
    db._rows = [];
    await rollupYesterday(db as unknown as D1Database);

    const select = db._statements.find((s) =>
      s.sql.includes("FROM events") && s.sql.includes("GROUP BY"),
    );
    expect(select).toBeDefined();
    // The bound param must be a YYYY-MM-DD string
    expect(typeof select!.params[0]).toBe("string");
    expect(select!.params[0]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  // ── Test 2: deletes events older than 90 days ────────────────────────────

  it("always issues a DELETE for events older than 90 days", async () => {
    db._rows = [makeRow()];

    await rollupYesterday(db as unknown as D1Database);

    const del = db._statements.find((s) =>
      s.sql.includes("DELETE FROM events") && s.sql.includes("-90 days"),
    );
    expect(del).toBeDefined();
  });

  it("deletes old events even when there are no rows to aggregate", async () => {
    db._rows = [];

    await rollupYesterday(db as unknown as D1Database);

    const del = db._statements.find((s) => s.sql.includes("DELETE FROM events"));
    expect(del).toBeDefined();
  });

  // ── Test 3: handles empty result gracefully ──────────────────────────────

  it("does not call batch when there are no events yesterday", async () => {
    db._rows = [];

    await expect(rollupYesterday(db as unknown as D1Database)).resolves.toBeUndefined();

    const inserts = db._statements.filter((s) =>
      s.sql.includes("INSERT OR REPLACE INTO daily_rollups"),
    );
    expect(inserts).toHaveLength(0);
  });

  it("still runs the DELETE purge when there are no events yesterday", async () => {
    db._rows = [];

    await rollupYesterday(db as unknown as D1Database);

    // Only two statements: SELECT + DELETE (no batch inserts)
    expect(db._statements).toHaveLength(2);
    expect(db._statements[0].sql).toContain("FROM events");
    expect(db._statements[1].sql).toContain("DELETE FROM events");
  });

  // ── Test 4: idempotent — INSERT OR REPLACE ───────────────────────────────

  it("uses INSERT OR REPLACE so repeated calls are idempotent", async () => {
    db._rows = [makeRow({ count: 7 })];

    await rollupYesterday(db as unknown as D1Database);

    const insert = db._statements.find((s) =>
      s.sql.includes("INSERT OR REPLACE INTO daily_rollups"),
    );
    expect(insert).toBeDefined();
    expect(insert!.sql).toMatch(/INSERT OR REPLACE/i);
  });

  it("can be called twice without error (simulated idempotency)", async () => {
    db._rows = [makeRow({ count: 4 })];

    // First call
    await rollupYesterday(db as unknown as D1Database);
    const firstCount = db._statements.length;

    // Reset and call again — simulates a cron retry
    db._statements = [];
    await rollupYesterday(db as unknown as D1Database);
    const secondCount = db._statements.length;

    // Same number of statements on both runs
    expect(secondCount).toBe(firstCount);
  });

  // ── Test 5: D1 failure handling ──────────────────────────────────────────

  it("throws when the SELECT aggregate query fails", async () => {
    db._shouldFail = true;

    await expect(rollupYesterday(db as unknown as D1Database)).rejects.toThrow(
      "D1 mock failure",
    );
  });

  it("throws when the DELETE purge fails", async () => {
    // Let the SELECT succeed (no rows so no batch), then fail on DELETE
    let callIndex = 0;
    const originalPrepare = db.prepare.bind(db);
    db.prepare = (sql: string) => {
      const stmt = originalPrepare(sql);
      if (sql.includes("DELETE FROM events")) {
        const originalRun = stmt.run.bind(stmt);
        stmt.run = async () => {
          throw new Error("D1 delete failure");
        };
      }
      return stmt;
    };
    db._rows = [];

    await expect(rollupYesterday(db as unknown as D1Database)).rejects.toThrow(
      "D1 delete failure",
    );
  });

  it("throws when the batch upsert fails", async () => {
    db._rows = [makeRow()];

    // Let the SELECT succeed, then fail on batch
    const originalBatch = db.batch.bind(db);
    db.batch = async (_stmts) => {
      throw new Error("D1 batch failure");
    };

    await expect(rollupYesterday(db as unknown as D1Database)).rejects.toThrow(
      "D1 batch failure",
    );
  });

  // ── Edge cases ───────────────────────────────────────────────────────────

  it("maps tool: undefined/null from grouped row to null in INSERT params", async () => {
    db._rows = [makeRow({ tool: null })];

    await rollupYesterday(db as unknown as D1Database);

    const insert = db._statements.find((s) =>
      s.sql.includes("INSERT OR REPLACE INTO daily_rollups"),
    );
    expect(insert).toBeDefined();
    // 7th param (index 5) is tool
    expect(insert!.params[5]).toBeNull();
  });

  it("handles multiple distinct dimension combinations", async () => {
    db._rows = [
      makeRow({ client: "cursor", tier: "pro", count: 20 }),
      makeRow({ client: "claude-desktop", tier: "free", count: 5 }),
      makeRow({ client: "vscode", tier: "free", tool: null, event: "init", count: 1 }),
    ];

    await rollupYesterday(db as unknown as D1Database);

    const inserts = db._statements.filter((s) =>
      s.sql.includes("INSERT OR REPLACE INTO daily_rollups"),
    );
    // Three grouped rows → three INSERT statements recorded by the batch
    expect(inserts).toHaveLength(3);
    // All have 7 parameters: date, event, client, mcp_ver, tier, tool, count
    for (const ins of inserts) {
      expect(ins.params).toHaveLength(7);
    }
  });
});
