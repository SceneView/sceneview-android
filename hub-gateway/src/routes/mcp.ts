/**
 * `/mcp` route for the hub gateway.
 *
 * Middleware chain:
 *   1. authMiddleware       — 401 on missing/invalid Bearer
 *   2. rateLimitMiddleware  — 429 on monthly or hourly overuse
 *   3. dispatch via transport — JSON-RPC parse + tool execution
 *   4. Post-dispatch observer — inserts usage_records row, bumps
 *      the monthly quota cache on success, attaches request ctx
 *
 * Usage logging intentionally runs AFTER dispatch so the outcome
 * (ok / denied / rate_limited / error) is known. It is wrapped in a
 * try/catch so a logging failure never bubbles into the user
 * response — matches Gateway #1's semantics exactly (see
 * mcp-gateway/src/routes/mcp.ts).
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { authMiddleware, type AuthVariables } from "../auth/middleware.js";
import { rateLimitMiddleware } from "../middleware/rate-limit.js";
import { handleMcpRequest, JSON_RPC_ERRORS } from "../mcp/transport.js";
import { insertUsageRecord, monthBucket } from "../db/usage.js";
import { incrementQuotaCache } from "../rate-limit/quotas.js";

type McpBindings = { Bindings: Env; Variables: AuthVariables };

export function mcpRoutes(): Hono<McpBindings> {
  const app = new Hono<McpBindings>();

  // Chain: auth (401) → rate-limit (429) → dispatch + usage log.
  app.use("*", authMiddleware(), rateLimitMiddleware());

  app.post("/", async (c) => {
    const auth = c.get("auth");
    const dispatchContext = {
      userId: auth.userId,
      apiKeyId: auth.keyId,
      tier: auth.tier,
    };

    // Read the body once so we can inspect it for usage logging.
    const rawBody = await c.req.raw.clone().text();

    const response = await handleMcpRequest(c.req.raw, {
      dispatchContext,
    });

    // Observe the call: figure out which tool was invoked and with
    // what outcome, then insert a usage record + bump the monthly
    // quota cache if it succeeded. Parse errors are swallowed so
    // the observer never interferes with the response path.
    try {
      const parsed = JSON.parse(rawBody) as {
        method?: string;
        params?: { name?: string };
      };
      if (
        parsed.method === "tools/call" &&
        typeof parsed.params?.name === "string"
      ) {
        const toolName = parsed.params.name;

        // Inspect the JSON-RPC error code on the response body to
        // distinguish denied vs rate-limited vs internal errors.
        let jsonRpcErrorCode: number | null = null;
        try {
          const bodyText = await response.clone().text();
          if (bodyText) {
            const payload = JSON.parse(bodyText) as {
              error?: { code?: number };
            };
            if (typeof payload?.error?.code === "number") {
              jsonRpcErrorCode = payload.error.code;
            }
          }
        } catch {
          // Non-JSON or unreadable — treat as ok if the HTTP code says so.
        }

        const status: "ok" | "denied" | "rate_limited" | "error" =
          response.status >= 500
            ? "error"
            : response.status === 429 ||
                jsonRpcErrorCode === JSON_RPC_ERRORS.RATE_LIMITED
              ? "rate_limited"
              : response.status === 401 ||
                  jsonRpcErrorCode === JSON_RPC_ERRORS.UNAUTHORIZED ||
                  jsonRpcErrorCode === JSON_RPC_ERRORS.ACCESS_DENIED
                ? "denied"
                : jsonRpcErrorCode !== null
                  ? "error"
                  : "ok";

        // Fire-and-forget the observer chain. Failures here are
        // swallowed inside each helper — nothing is allowed to
        // reach the response path.
        await insertUsageRecord(c.env.DB, {
          apiKeyId: auth.keyId,
          userId: auth.userId,
          toolName,
          // Every hub tool is treated as `pro` for billing semantics
          // until the tier table lands (next sprint). Free-tier
          // calls still count against the free monthly cap because
          // the middleware checks `limits.monthly` per-tier already.
          tierRequired: "pro",
          status,
          bucketMonth: monthBucket(),
        });
        if (status === "ok") {
          await incrementQuotaCache(
            c.env.RL_KV,
            auth.keyHash,
            monthBucket(),
          );
        }
      }
    } catch {
      // Observation failed — never bubble into the user response.
    }

    return response;
  });

  app.get("/", (c) =>
    c.json(
      {
        error:
          "Streamable HTTP MCP only accepts POST in the MVP (SSE not implemented)",
      },
      405,
    ),
  );

  return app;
}
