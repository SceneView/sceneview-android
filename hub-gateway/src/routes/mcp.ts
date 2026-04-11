/**
 * `/mcp` route for the hub gateway.
 *
 * Middleware chain:
 *   1. authMiddleware — populates c.var.auth, returns JSON-RPC 401
 *      on missing or invalid keys.
 *   2. handleMcpRequest — JSON-RPC parse + tool dispatch.
 *
 * Rate limiting, usage logging, and Stripe plumbing are still out of
 * scope — they'll port from mcp-gateway/src/middleware/rate-limit.ts
 * and mcp-gateway/src/routes/billing.ts in a follow-up session. The
 * minimal guarantee shipped here is: no D1-backed dispatch happens
 * unless a valid API key is present.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { authMiddleware, type AuthVariables } from "../auth/middleware.js";
import { rateLimitMiddleware } from "../middleware/rate-limit.js";
import { handleMcpRequest } from "../mcp/transport.js";

type McpBindings = { Bindings: Env; Variables: AuthVariables };

export function mcpRoutes(): Hono<McpBindings> {
  const app = new Hono<McpBindings>();

  // Chain: auth (401 on missing/invalid) → rate-limit (429 on overuse)
  // → JSON-RPC dispatch.
  app.use("*", authMiddleware(), rateLimitMiddleware());

  app.post("/", async (c) => {
    const auth = c.get("auth");
    return handleMcpRequest(c.req.raw, {
      dispatchContext: {
        userId: auth.userId,
        apiKeyId: auth.keyId,
        tier: auth.tier,
      },
    });
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
