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
  /**
   * Absolute public URL of the gateway, without trailing slash.
   * Used to build Stripe success/cancel URLs (and, historically,
   * magic-link redirects — the dashboard auth flow is not wired in
   * the MVP). Example: `https://sceneview-mcp.mcp-tools-lab.workers.dev`.
   */
  DASHBOARD_BASE_URL?: string;
  /**
   * "from" email shown in magic-link emails. NOT USED IN THE MVP —
   * kept for a future dashboard auth sprint.
   */
  MAGIC_LINK_FROM_EMAIL?: string;

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
  /**
   * Resend API key for magic-link emails. NOT USED IN THE MVP —
   * kept for a future dashboard auth sprint.
   */
  RESEND_API_KEY?: string;
  /**
   * HMAC secret for dashboard session JWTs. NOT USED IN THE MVP —
   * kept for a future dashboard auth sprint.
   */
  JWT_SECRET?: string;

  // ── Stripe price identifiers (vars, not secrets) ─────────────────────────
  /** Stripe price id for Pro monthly (19 EUR/mo). */
  STRIPE_PRICE_PRO_MONTHLY?: string;
  /** Stripe price id for Pro yearly (190 EUR/yr). */
  STRIPE_PRICE_PRO_YEARLY?: string;
  /** Stripe price id for Team monthly (49 EUR/mo). */
  STRIPE_PRICE_TEAM_MONTHLY?: string;
  /** Stripe price id for Team yearly (490 EUR/yr). */
  STRIPE_PRICE_TEAM_YEARLY?: string;
}
