/**
 * Hono middleware — enforces the per-tier hourly and monthly limits
 * AFTER the auth middleware has populated `c.var.auth`.
 *
 * On rejection it returns a JSON-RPC 2.0 body carrying error code
 * -32002 (RATE_LIMITED) with data `{ tier, limit, reset }`. It also
 * sets the `X-RateLimit-*` response headers on every response so
 * clients can build their own backoff logic.
 *
 * Sequencing note: this middleware runs AFTER authentication and
 * BEFORE the MCP handler. It does NOT log usage — usage logging
 * happens in the route once the dispatch result is known, because
 * we want to distinguish `ok` / `denied` / `error` outcomes.
 */

import type { Context, MiddlewareHandler } from "hono";
import type { Env } from "../env.js";
import type { AuthVariables } from "../auth/middleware.js";
import { getLimitsForTier } from "../rate-limit/limits.js";
import {
  checkAndIncrementHourly,
  type RateLimitDecision,
} from "../rate-limit/kv-counter.js";
import { checkMonthlyQuota } from "../rate-limit/quotas.js";
import { monthBucket } from "../db/usage.js";
import { JSON_RPC_ERRORS, type JsonRpcResponse } from "../mcp/transport.js";

/** Returns a Hono middleware that enforces hourly + monthly limits. */
export function rateLimitMiddleware(): MiddlewareHandler<{
  Bindings: Env;
  Variables: AuthVariables;
}> {
  return async (c, next) => {
    const auth = c.get("auth");
    if (!auth) {
      // Defensive: the auth middleware should have rejected earlier.
      return c.json(
        { error: "Auth middleware missing" } as unknown as JsonRpcResponse,
        500,
      );
    }

    const limits = getLimitsForTier(auth.tier);
    const bucket = monthBucket();

    // 1. Monthly quota (cheapest path — often a KV hit).
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
        // Monthly reset is computed as the first of next month 00:00 UTC.
        resetAt: monthResetMs(),
      });
    }

    // 2. Hourly sliding window (two KV reads + one write on ok).
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

    // Attach the X-RateLimit headers to the success response.
    c.header("X-RateLimit-Limit", String(limits.hourly));
    c.header("X-RateLimit-Remaining", String(hourly.remaining));
    c.header(
      "X-RateLimit-Reset",
      String(Math.floor(hourly.resetAt / 1000)),
    );

    await next();
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
  const body: JsonRpcResponse = {
    jsonrpc: "2.0",
    id: null,
    error: {
      code: JSON_RPC_ERRORS.RATE_LIMITED,
      message: "Rate limit exceeded",
      data: detail,
    },
  };
  c.header("X-RateLimit-Limit", String(detail.limit));
  c.header("X-RateLimit-Remaining", String(detail.remaining));
  c.header("X-RateLimit-Reset", String(Math.floor(detail.resetAt / 1000)));
  return c.json(body, 429);
}

/** Epoch ms of the first day of next month, UTC 00:00. */
function monthResetMs(now = new Date()): number {
  const y = now.getUTCFullYear();
  const m = now.getUTCMonth();
  return Date.UTC(y, m + 1, 1, 0, 0, 0, 0);
}

/** Re-export so tests can reference the decision type. */
export type { RateLimitDecision };
