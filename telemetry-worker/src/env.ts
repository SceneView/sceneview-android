/** Type-safe Cloudflare bindings for the telemetry worker. */
export interface Env {
  DB: D1Database;
  RL_KV: KVNamespace;
  ENVIRONMENT: string;
  /** Optional bearer token protecting GET /v1/stats. Set via `wrangler secret put STATS_TOKEN`. */
  STATS_TOKEN?: string;
}
