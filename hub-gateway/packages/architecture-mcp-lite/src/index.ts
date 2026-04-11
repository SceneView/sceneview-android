#!/usr/bin/env node
/**
 * architecture-mcp 2.0.0-beta.1 — lite proxy mode.
 *
 * Historically, `architecture-mcp` was a standalone MCP server shipped
 * with its own tool logic. As of 2.x it is a thin proxy that forwards
 * every tool call over HTTP to the hub-mcp hosted gateway, which runs
 * the real handlers:
 *
 *     https://hub-mcp.mcp-tools-lab.workers.dev/mcp
 *
 * Why: one subscription (Portfolio Access, €29/mo) covers the whole
 * non-SceneView MCP portfolio instead of a per-package paywall. Free
 * tier users still get 100 tool calls/month without an API key —
 * `tools/list` works anonymously, `tools/call` will be metered by
 * the gateway (auth middleware ships in a follow-up).
 *
 * Environment variables:
 *   HUB_MCP_API_KEY — optional Bearer token. If set, forwarded to the
 *                     hub gateway in the `Authorization` header.
 *   HUB_MCP_URL     — override the gateway URL (for local dev / tests).
 *                     Default: https://hub-mcp.mcp-tools-lab.workers.dev/mcp
 *
 * The banner on stderr makes the lite mode obvious so users know
 * why they need a `HUB_MCP_API_KEY` for Pro tools.
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  type Tool,
} from "@modelcontextprotocol/sdk/types.js";

const DEFAULT_URL = "https://hub-mcp.mcp-tools-lab.workers.dev/mcp";
const GATEWAY_URL = process.env.HUB_MCP_URL ?? DEFAULT_URL;
const API_KEY = process.env.HUB_MCP_API_KEY;
const PACKAGE_ID = "architecture";
const VERSION = "2.0.0-beta.1";

/** Write a one-time banner to stderr on process start. */
function printBanner(): void {
  process.stderr.write(
    [
      "",
      `  architecture-mcp v${VERSION} (lite proxy mode)`,
      `  Forwarding tool calls to ${GATEWAY_URL}`,
      API_KEY
        ? "  Authenticated: yes (HUB_MCP_API_KEY is set)"
        : "  Authenticated: no  (set HUB_MCP_API_KEY to unlock Pro tools)",
      "  https://hub-mcp.mcp-tools-lab.workers.dev/pricing",
      "",
    ].join("\n"),
  );
}

interface JsonRpcEnvelope<T = unknown> {
  jsonrpc: "2.0";
  id: number;
  method: string;
  params?: T;
}

let nextId = 1;

/** POST a JSON-RPC envelope to the hub gateway and return the parsed response. */
async function callGateway(
  method: string,
  params?: unknown,
): Promise<{ result?: unknown; error?: { code: number; message: string } }> {
  const envelope: JsonRpcEnvelope = {
    jsonrpc: "2.0",
    id: nextId++,
    method,
    params,
  };

  const headers: Record<string, string> = {
    "content-type": "application/json",
    "user-agent": `architecture-mcp/${VERSION} (lite)`,
  };
  if (API_KEY) headers.authorization = `Bearer ${API_KEY}`;

  const res = await fetch(GATEWAY_URL, {
    method: "POST",
    headers,
    body: JSON.stringify(envelope),
  });

  if (!res.ok && res.status !== 200) {
    const text = await res.text().catch(() => "");
    throw new Error(
      `Hub gateway returned HTTP ${res.status}: ${text.slice(0, 200)}`,
    );
  }

  return (await res.json()) as {
    result?: unknown;
    error?: { code: number; message: string };
  };
}

/**
 * Restrict the tools this lite package exposes to its own package
 * prefix. The hub gateway may host many libraries — we only want the
 * architecture ones to show up in a client that installed
 * `architecture-mcp`.
 */
function isOurTool(name: string): boolean {
  return name.startsWith(`${PACKAGE_ID}__`);
}

async function main(): Promise<void> {
  printBanner();

  const server = new Server(
    { name: "architecture-mcp", version: VERSION },
    { capabilities: { tools: {} } },
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => {
    const response = await callGateway("tools/list");
    if (response.error) {
      throw new Error(
        `tools/list failed on hub gateway: ${response.error.message}`,
      );
    }
    const result = response.result as { tools?: Tool[] } | undefined;
    const allTools = result?.tools ?? [];
    return { tools: allTools.filter((t) => isOurTool(t.name)) };
  });

  server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const { name, arguments: args } = req.params;
    if (!isOurTool(name)) {
      throw new Error(
        `Tool "${name}" is not exposed by architecture-mcp. Install the matching lite package or use the hub gateway directly.`,
      );
    }
    const response = await callGateway("tools/call", { name, arguments: args });
    if (response.error) {
      throw new Error(
        `tools/call failed on hub gateway: ${response.error.message}`,
      );
    }
    return response.result as {
      content: Array<{ type: "text"; text: string }>;
      isError?: boolean;
    };
  });

  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  process.stderr.write(`architecture-mcp fatal: ${String(err)}\n`);
  process.exit(1);
});
