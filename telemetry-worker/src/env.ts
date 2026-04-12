/** Type-safe Cloudflare bindings for the telemetry worker. */
export interface Env {
  DB: D1Database;
  RL_KV: KVNamespace;
  ENVIRONMENT: string;
}
