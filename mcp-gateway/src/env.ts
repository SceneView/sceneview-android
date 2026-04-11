/**
 * Environment bindings exposed to the Worker at runtime.
 *
 * The shape of this type matches the bindings declared in `wrangler.toml`.
 * Any new binding (D1 database, KV namespace, secret, var) MUST be added
 * both here and in `wrangler.toml` for typesafety.
 */
export interface Env {
  // ── Vars ──────────────────────────────────────────────────────────────────
  ENVIRONMENT: string;

  // ── D1 ────────────────────────────────────────────────────────────────────
  DB: D1Database;

  // ── KV ────────────────────────────────────────────────────────────────────
  /** Rate limiting counters + auth cache. */
  RL_KV: KVNamespace;

  // ── Secrets (wrangler secret put <NAME>) ─────────────────────────────────
  /** Stripe API secret key. */
  STRIPE_SECRET_KEY?: string;
  /** Stripe webhook signing secret (whsec_...). */
  STRIPE_WEBHOOK_SECRET?: string;
  /** Resend API key for magic-link emails. */
  RESEND_API_KEY?: string;
  /** HMAC secret for dashboard session JWTs. */
  JWT_SECRET?: string;
}
