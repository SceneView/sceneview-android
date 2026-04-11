/**
 * Minimal JSON-RPC MCP transport for the hub gateway (MVP).
 *
 * This is a deliberately stripped-down version of
 * mcp-gateway/src/mcp/transport.ts (490 lines) — only the three
 * methods needed for the pilot are implemented:
 *
 *   - `initialize`   → returns protocol version + server info
 *   - `tools/list`   → returns the merged registry
 *   - `tools/call`   → dispatches to the owning library
 *
 * No streaming, no SSE, no session ids. When the MVP needs parity
 * with Gateway #1 we will port the full transport — until then,
 * keep the code surface small enough to audit in one sitting.
 */

import { dispatch, getAllTools } from "./registry.js";
import { canCallTool, getToolTier } from "./access.js";
import type { DispatchContext, ToolResult } from "./types.js";

/** Canonical JSON-RPC error codes reused from Gateway #1. */
export const JSON_RPC_ERRORS = {
  PARSE_ERROR: -32700,
  INVALID_REQUEST: -32600,
  METHOD_NOT_FOUND: -32601,
  INVALID_PARAMS: -32602,
  INTERNAL_ERROR: -32603,
  UNAUTHORIZED: -32001,
  RATE_LIMITED: -32002,
  ACCESS_DENIED: -32003,
} as const;

/** Protocol version advertised on `initialize`. */
const PROTOCOL_VERSION = "2025-03-26";

interface JsonRpcRequest {
  jsonrpc: "2.0";
  id?: string | number | null;
  method?: string;
  params?: unknown;
}

interface JsonRpcSuccess {
  jsonrpc: "2.0";
  id: string | number | null;
  result: unknown;
}

interface JsonRpcError {
  jsonrpc: "2.0";
  id: string | number | null;
  error: { code: number; message: string; data?: unknown };
}

type JsonRpcResponse = JsonRpcSuccess | JsonRpcError;

function jsonResponse(payload: JsonRpcResponse, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function rpcError(
  id: string | number | null,
  code: number,
  message: string,
  data?: unknown,
  status = 200,
): Response {
  return jsonResponse({ jsonrpc: "2.0", id, error: { code, message, data } }, status);
}

/**
 * Entry point called by the /mcp route. Returns an HTTP response
 * containing the JSON-RPC envelope. Status is always 200 unless the
 * request is unparseable (400) or produces a server error (500).
 */
export async function handleMcpRequest(
  request: Request,
  opts: { dispatchContext?: DispatchContext } = {},
): Promise<Response> {
  if (request.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405 });
  }

  let rpc: JsonRpcRequest;
  try {
    rpc = (await request.json()) as JsonRpcRequest;
  } catch {
    return rpcError(
      null,
      JSON_RPC_ERRORS.PARSE_ERROR,
      "Invalid JSON body",
      undefined,
      400,
    );
  }

  if (rpc.jsonrpc !== "2.0" || typeof rpc.method !== "string") {
    return rpcError(
      rpc.id ?? null,
      JSON_RPC_ERRORS.INVALID_REQUEST,
      "Not a valid JSON-RPC 2.0 request",
      undefined,
      400,
    );
  }

  const id = rpc.id ?? null;
  const dispatchContext = opts.dispatchContext ?? {};

  switch (rpc.method) {
    case "initialize":
      return jsonResponse({
        jsonrpc: "2.0",
        id,
        result: {
          protocolVersion: PROTOCOL_VERSION,
          capabilities: { tools: {} },
          serverInfo: {
            name: "hub-mcp-gateway",
            version: "0.0.1",
          },
        },
      });

    case "tools/list":
      return jsonResponse({
        jsonrpc: "2.0",
        id,
        result: { tools: getAllTools() },
      });

    case "tools/call": {
      const params = rpc.params as
        | { name?: unknown; arguments?: Record<string, unknown> }
        | undefined;
      if (!params || typeof params.name !== "string") {
        return rpcError(
          id,
          JSON_RPC_ERRORS.INVALID_PARAMS,
          "tools/call requires params.name (string)",
        );
      }

      // Tier gate: reject Pro tools for free users with -32003.
      // The JSON-RPC envelope stays HTTP 200 so the /mcp route's
      // post-dispatch observer can pick up the ACCESS_DENIED code
      // and log the usage row with status="denied".
      if (!canCallTool(params.name, dispatchContext)) {
        return rpcError(
          id,
          JSON_RPC_ERRORS.ACCESS_DENIED,
          `Tool "${params.name}" requires a Portfolio Access subscription`,
          {
            tool: params.name,
            requiredTier: getToolTier(params.name),
            currentTier: dispatchContext.tier ?? "unknown",
            upgradeUrl: "https://hub-mcp.mcp-tools-lab.workers.dev/pricing",
          },
        );
      }

      let result: ToolResult;
      try {
        result = await dispatch(params.name, params.arguments, dispatchContext);
      } catch (err) {
        return rpcError(
          id,
          JSON_RPC_ERRORS.INTERNAL_ERROR,
          err instanceof Error ? err.message : "Dispatch failure",
          undefined,
          500,
        );
      }

      return jsonResponse({ jsonrpc: "2.0", id, result });
    }

    default:
      return rpcError(
        id,
        JSON_RPC_ERRORS.METHOD_NOT_FOUND,
        `Method not found: ${rpc.method}`,
      );
  }
}
