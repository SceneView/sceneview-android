/**
 * `/mcp` route for the hub gateway (MVP).
 *
 * Thin wrapper around `handleMcpRequest`. Auth, rate-limiting, and
 * usage logging are DELIBERATELY not wired yet — they will be ported
 * from mcp-gateway/src/routes/mcp.ts in the follow-up session once
 * the scaffold is validated on `*.workers.dev`.
 *
 * The auth bypass is fine for MVP smoke-tests because the pilot
 * dispatcher only returns a hard-coded "not yet wired" message. When
 * the real tool handlers land, the auth middleware MUST land with
 * them.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { handleMcpRequest } from "../mcp/transport.js";

export function mcpRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.post("/", async (c) => {
    return handleMcpRequest(c.req.raw, {});
  });

  app.get("/", (c) =>
    c.json(
      {
        error: "Streamable HTTP MCP only accepts POST in the MVP (SSE not implemented)",
      },
      405,
    ),
  );

  return app;
}
