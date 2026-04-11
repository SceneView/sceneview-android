/**
 * Hono middleware — enforces the per-tier hourly sliding window AFTER
 * the auth middleware has populated `c.var.auth`.
 *
 * Ported from mcp-gateway/src/middleware/rate-limit.ts with three
 * differences:
 *   - Monthly quota is NOT enforced yet — that path needs D1 usage
 *     logging which is scheduled for the next sprint.
 *   - KV prefix is `hub-rl:` so the two gateways keep independent
 *     hourly budgets in the shared KV namespace.
 *   - KV transient errors fail OPEN (see kv-counter.ts) so a KV
 *     outage doesn't shut the hub down.
 *
 * On rejection, returns a JSON-RPC 2.0 body carrying error code
 * -32002 (RATE_LIMITED) with data `{ tier, scope, limit, remaining,
 * resetAt }`, HTTP status 429, plus `X-RateLimit-*` response headers
 * on every response so clients can build their own backoff logic.
 *
 * Sequencing: auth → rate-limit → /mcp dispatch. Does NOT log usage.
 */

import type { Context, MiddlewareHandler } from "hono";
import type { Env } from "../env.js";
import type { AuthVariables } from "../auth/middleware.js";
import { getLimitsForTier } from "../rate-limit/limits.js";
import { checkAndIncrementHourly } from "../rate-limit/kv-counter.js";
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
