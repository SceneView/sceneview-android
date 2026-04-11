/**
 * Hono middleware — enforces per-tier monthly quota + hourly sliding
 * window AFTER the auth middleware has populated `c.var.auth`.
 *
 * Order matters: the monthly quota runs FIRST because it's the
 * cheapest path (typically a single KV hit) and tells the client
 * "you're out of budget for the month" rather than "you're bursting
 * too fast". Hourly runs second and is the defence against runaway
 * loops even when plenty of monthly quota is still available.
 *
 * Ported from mcp-gateway/src/middleware/rate-limit.ts with:
 *   - Monthly quota cache uses the `hub-quota:` KV prefix so both
 *     gateways keep independent monthly counters in the shared KV
 *     namespace.
 *   - Hourly counter uses the `hub-rl:` KV prefix for the same
 *     reason.
 *   - Every KV/D1 path fails OPEN on transient errors (see
 *     quotas.ts and kv-counter.ts). A caching outage never blocks
 *     a paying user.
 *
 * On rejection, returns a JSON-RPC 2.0 body carrying error code
 * -32002 (RATE_LIMITED) with data `{ tier, scope, limit, remaining,
 * resetAt }`, HTTP status 429, plus `X-RateLimit-*` response headers
 * on every response so clients can build their own backoff logic.
 *
 * Sequencing: auth → rate-limit (monthly → hourly) → /mcp dispatch.
 * This middleware does NOT log usage; that happens in the route
 * AFTER dispatch so the status field (ok/denied/error) is known.
 */

import type { Context, MiddlewareHandler } from "hono";
import type { Env } from "../env.js";
import type { AuthVariables } from "../auth/middleware.js";
import { getLimitsForTier } from "../rate-limit/limits.js";
import { checkAndIncrementHourly } from "../rate-limit/kv-counter.js";
import { checkMonthlyQuota } from "../rate-limit/quotas.js";
import { monthBucket } from "../db/usage.js";
import { JSON_RPC_ERRORS } from "../mcp/transport.js";

/** Hono middleware factory — enforces hourly limits on every call. */
export function rateLimitMiddleware(): MiddlewareHandler<{
  Bindings: Env;
  Variables: AuthVariables;
}> {
  return async (c, next) => {
    const auth = c.get("auth");
    if (!auth) {
      // Defensive: the auth middleware should have rejected earlier.
      return c.json(
        {
          jsonrpc: "2.0",
          id: null,
          error: {
            code: JSON_RPC_ERRORS.INTERNAL_ERROR,
            message: "Auth middleware missing",
          },
        },
        500,
      );
    }

    const limits = getLimitsForTier(auth.tier);
    const bucket = monthBucket();

    // 1. Monthly quota — cheapest path (often a single KV hit).
    const quota = await checkMonthlyQuota(
      c.env.RL_KV,
      c.env.DB,
      auth.keyId,
      auth.keyHash,
      bucket,
      limits.monthly,
    );
    if (!quota.allowed) {
      return rateLimitedResponse(c, {
        tier: auth.tier,
        scope: "monthly",
        limit: limits.monthly,
        remaining: 0,
        resetAt: monthResetMs(),
      });
    }

    // 2. Hourly sliding window — defence against runaway loops.
    const hourly = await checkAndIncrementHourly(
      c.env.RL_KV,
      auth.keyHash,
      limits.hourly,
    );

    if (!hourly.allowed) {
      return rateLimitedResponse(c, {
        tier: auth.tier,
        scope: "hourly",
        limit: limits.hourly,
        remaining: hourly.remaining,
        resetAt: hourly.resetAt,
      });
    }

    await next();

    // Attach X-RateLimit headers to the downstream response AFTER
    // it's been generated. We can't use `c.header()` before
    // `await next()` because downstream handlers that return a
    // brand new `Response` object (like `handleMcpRequest`) don't
    // pick up pending headers set on the context. Writing directly
    // to `c.res.headers` mutates the final response.
    if (c.res) {
      c.res.headers.set("X-RateLimit-Limit", String(limits.hourly));
      c.res.headers.set("X-RateLimit-Remaining", String(hourly.remaining));
      c.res.headers.set(
        "X-RateLimit-Reset",
        String(Math.floor(hourly.resetAt / 1000)),
      );
    }
    return;
  };
}

// ── Helpers ────────────────────────────────────────────────────────────────

interface RateLimitedDetail {
  tier: string;
  scope: "hourly" | "monthly";
  limit: number;
  remaining: number;
  resetAt: number;
}

type RateLimitContext = Context<{
  Bindings: Env;
  Variables: AuthVariables;
}>;

/** Builds a JSON-RPC 2.0 rate-limit response (-32002) with HTTP 429. */
function rateLimitedResponse(
  c: RateLimitContext,
  detail: RateLimitedDetail,
): Response {
  c.header("X-RateLimit-Limit", String(detail.limit));
  c.header("X-RateLimit-Remaining", String(detail.remaining));
  c.header("X-RateLimit-Reset", String(Math.floor(detail.resetAt / 1000)));
  return c.json(
    {
      jsonrpc: "2.0",
      id: null,
      error: {
        code: JSON_RPC_ERRORS.RATE_LIMITED,
        message: "Rate limit exceeded",
        data: detail,
      },
    },
    429,
  );
}

/** Epoch ms of the first day of next month, UTC 00:00. */
function monthResetMs(now = new Date()): number {
  const y = now.getUTCFullYear();
  const m = now.getUTCMonth();
  return Date.UTC(y, m + 1, 1, 0, 0, 0, 0);
}
