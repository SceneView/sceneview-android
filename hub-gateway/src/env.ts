/**
 * Environment bindings exposed to the Hub Gateway worker at runtime.
 *
 * Shape MUST match the bindings declared in `wrangler.toml`. Any new
 * binding needs to be added in both places for typesafety.
 *
 * The D1 database and KV namespace are SHARED with Gateway #1
 * (sceneview-mcp-gateway). This is deliberate: one sub = access to
 * both gateways. Do not create a new database here.
 */
export interface Env {
  // ── Vars ──────────────────────────────────────────────────────────────────
  ENVIRONMENT: string;
  /**
   * Absolute public URL of the hub gateway, without trailing slash.
   * Used to build Stripe checkout success/cancel URLs.
   * Example: `https://hub-mcp.mcp-tools-lab.workers.dev`.
   */
  GATEWAY_BASE_URL?: string;

  // ── D1 (shared with Gateway #1) ───────────────────────────────────────────
  DB: D1Database;

  // ── KV (shared with Gateway #1) ───────────────────────────────────────────
  /** Rate limiting counters + auth cache. */
  RL_KV: KVNamespace;

  // ── Secrets (wrangler secret put <NAME>) ─────────────────────────────────
  /** Stripe API secret key. */
  STRIPE_SECRET_KEY?: string;
  /** Stripe webhook signing secret (whsec_...). */
  STRIPE_WEBHOOK_SECRET?: string;

  // ── Stripe price identifiers (vars, not secrets) ─────────────────────────
  /** Portfolio Access monthly (29 EUR/mo). */
  STRIPE_PRICE_PORTFOLIO_MONTHLY?: string;
  /** Portfolio Access yearly (290 EUR/yr — 2 months free). */
  STRIPE_PRICE_PORTFOLIO_YEARLY?: string;
  /** Team monthly (79 EUR/mo). */
  STRIPE_PRICE_TEAM_MONTHLY?: string;
  /** Team yearly (790 EUR/yr — 2 months free). */
  STRIPE_PRICE_TEAM_YEARLY?: string;
}
