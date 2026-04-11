/**
 * Minimal fake D1 + KV bindings for unit tests.
 *
 * Implements just enough of the Workers runtime API to drive the
 * hub auth middleware without spinning up Miniflare. The real
 * bindings live on Cloudflare and are exercised by the separate
 * integration suite.
 *
 * Usage:
 *
 *   const db = new FakeD1({
 *     api_keys: [{ id, user_id, key_hash, key_prefix, revoked_at }],
 *     users: [{ id, email, tier }],
 *   });
 *   const kv = new FakeKV();
 *   const env: Env = { ENVIRONMENT: "test", DB: db as any, RL_KV: kv as any };
 */

import type { Env } from "../../src/env.js";

interface ApiKeyRow {
  id: string;
  user_id: string;
  key_hash: string;
  key_prefix: string;
  revoked_at: number | null;
}

interface UserRow {
  id: string;
  email: string;
  tier: "free" | "pro" | "team";
}

interface UsageRecordRow {
  api_key_id: string;
  user_id: string;
  tool_name: string;
  tier_required: string;
  status: string;
  bucket_month: string;
  created_at: number;
}

export interface FakeSeed {
  api_keys: ApiKeyRow[];
  users: UserRow[];
  /** Optional: pre-seeded usage records (mostly starts empty). */
  usage_records?: UsageRecordRow[];
}

/** Fake D1Database covering the SELECTs + the usage_records INSERT / COUNT. */
export class FakeD1 {
  constructor(private seed: FakeSeed) {
    if (!this.seed.usage_records) this.seed.usage_records = [];
  }

  prepare(sql: string) {
    return new FakeStatement(sql, this.seed);
  }

  /** Direct read accessor for tests. */
  getUsageRecords(): UsageRecordRow[] {
    return this.seed.usage_records ?? [];
  }

  /** For tests that need to mutate after construction. */
  upsert(table: "api_keys" | "users", row: ApiKeyRow | UserRow): void {
    const arr = this.seed[table] as Array<ApiKeyRow | UserRow>;
    const idx = arr.findIndex((r) => r.id === row.id);
    if (idx >= 0) arr[idx] = row;
    else arr.push(row);
  }
}

class FakeStatement {
  private params: unknown[] = [];
  constructor(private sql: string, private seed: FakeSeed) {}

  bind(...params: unknown[]): this {
    this.params = params;
    return this;
  }

  async first<T>(): Promise<T | null> {
    const s = this.sql.toLowerCase();
    if (s.includes("from api_keys where key_hash")) {
      const hash = this.params[0] as string;
      const row = this.seed.api_keys.find((k) => k.key_hash === hash);
      return (row as unknown as T) ?? null;
    }
    if (s.includes("from users where id")) {
      const id = this.params[0] as string;
      const row = this.seed.users.find((u) => u.id === id);
      return (row as unknown as T) ?? null;
    }
    if (s.includes("count(*) as count") && s.includes("from usage_records")) {
      // countSuccessfulUsageInMonth: WHERE api_key_id = ?1 AND
      // bucket_month = ?2 AND status = 'ok'.
      const apiKeyId = this.params[0] as string;
      const bucket = this.params[1] as string;
      const rows = this.seed.usage_records ?? [];
      const n = rows.filter(
        (r) =>
          r.api_key_id === apiKeyId &&
          r.bucket_month === bucket &&
          r.status === "ok",
      ).length;
      return { count: n } as unknown as T;
    }
    throw new Error(`FakeD1: unsupported SQL in first(): ${this.sql}`);
  }

  async run(): Promise<{ success: boolean }> {
    const s = this.sql.toLowerCase();
    if (s.includes("insert into usage_records")) {
      const [apiKeyId, userId, toolName, tierRequired, status, bucketMonth, createdAt] =
        this.params as [string, string, string, string, string, string, number];
      // FakeD1 constructor guarantees usage_records is initialised,
      // so the cast is safe here.
      (this.seed.usage_records as UsageRecordRow[]).push({
        api_key_id: apiKeyId,
        user_id: userId,
        tool_name: toolName,
        tier_required: tierRequired,
        status,
        bucket_month: bucketMonth,
        created_at: createdAt,
      });
      return { success: true };
    }
    throw new Error(`FakeD1: unsupported SQL in run(): ${this.sql}`);
  }
}

/** Fake KVNamespace with in-memory Map storage and TTL awareness. */
export class FakeKV {
  private store = new Map<string, { value: string; expires: number | null }>();

  async get(key: string): Promise<string | null> {
    const entry = this.store.get(key);
    if (!entry) return null;
    if (entry.expires !== null && entry.expires < Date.now()) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async put(
    key: string,
    value: string,
    opts?: { expirationTtl?: number },
  ): Promise<void> {
    const expires =
      opts?.expirationTtl !== undefined
        ? Date.now() + opts.expirationTtl * 1000
        : null;
    this.store.set(key, { value, expires });
  }

  async delete(key: string): Promise<void> {
    this.store.delete(key);
  }

  /** Test-only: inspect the current store. */
  get size(): number {
    return this.store.size;
  }

  has(key: string): boolean {
    return this.store.has(key);
  }
}

/** Convenience: build a full hub Env from a seed. */
export function makeEnv(seed: FakeSeed): {
  env: Env;
  db: FakeD1;
  kv: FakeKV;
} {
  const db = new FakeD1(seed);
  const kv = new FakeKV();
  return {
    db,
    kv,
    env: {
      ENVIRONMENT: "test",
      GATEWAY_BASE_URL: "https://hub-mcp.test",
      DB: db as unknown as D1Database,
      RL_KV: kv as unknown as KVNamespace,
    },
  };
}
