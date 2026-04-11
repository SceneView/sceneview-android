#!/usr/bin/env node

/**
 * stdio entrypoint for the `sceneview-mcp` npm package.
 *
 * This file used to be a 1 200+ line monolith that defined every tool,
 * its handler, and the stdio transport all at once. The tool definitions
 * and handler logic now live in `./tools/`, so this file is a thin
 * adapter that wires the library into the MCP stdio server plus the
 * two MCP resources (`sceneview://api`, `sceneview://known-issues`) and
 * the pro-tier access / billing checks.
 *
 * IMPORTANT: the runtime behaviour must stay identical to v3.6.2 for
 * existing npm consumers. Do not reorder checks, do not change content
 * strings, do not touch disclaimers.
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { fetchKnownIssues } from "./issues.js";
import { checkToolAccess, filterToolsForTier, createAccessDeniedResponse } from "./auth.js";
import { recordUsage, getConfiguredApiKey } from "./billing.js";
import { recordClientInit, recordToolCall } from "./telemetry.js";
import { getToolTier } from "./tiers.js";
import {
  API_DOCS,
  TOOL_DEFINITIONS,
  dispatchTool,
} from "./tools/index.js";

const server = new Server(
  { name: "sceneview-mcp", version: "4.0.0" },
  { capabilities: { resources: {}, tools: {} } }
);

// ─── Telemetry (anonymous, opt-out via SCENEVIEW_TELEMETRY=0) ────────────────
//
// Fire once when the client finishes the handshake. See `telemetry.ts` and
// `PRIVACY.md` for what's collected and how to opt out.
server.oninitialized = () => {
  recordClientInit(server.getClientVersion());
};

// ─── Resources ───────────────────────────────────────────────────────────────

server.setRequestHandler(ListResourcesRequestSchema, async () => ({
  resources: [
    {
      uri: "sceneview://api",
      name: "SceneView API Reference",
      description:
        "Complete SceneView 3.6.2 API — SceneView, ARSceneView, SceneScope DSL, ARSceneScope DSL, node types, resource loading, camera, gestures, math types, threading rules, and common patterns. Read this before writing any SceneView code.",
      mimeType: "text/markdown",
    },
    {
      uri: "sceneview://known-issues",
      name: "SceneView Open GitHub Issues",
      description:
        "Live list of open issues from the SceneView GitHub repository. Check this before reporting a bug or when something isn't working — there may already be a known workaround.",
      mimeType: "text/markdown",
    },
  ],
}));

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  switch (request.params.uri) {
    case "sceneview://api":
      return {
        contents: [{ uri: "sceneview://api", mimeType: "text/markdown", text: API_DOCS }],
      };

    case "sceneview://known-issues": {
      const issues = await fetchKnownIssues();
      return {
        contents: [{ uri: "sceneview://known-issues", mimeType: "text/markdown", text: issues }],
      };
    }

    default:
      throw new Error(`Unknown resource: ${request.params.uri}`);
  }
});

// ─── Tools ───────────────────────────────────────────────────────────────────

server.setRequestHandler(ListToolsRequestSchema, async () => {
  // filterToolsForTier has a looser parameter type (index signature) than
  // our strict ToolDefinition. The cast is safe: ToolDefinition is a
  // superset of { name, description, inputSchema } and filterToolsForTier
  // only reads `name` and `description`.
  const tools = await filterToolsForTier(
    TOOL_DEFINITIONS as unknown as Array<{ name: string; [key: string]: unknown }>,
  );
  return { tools };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const toolName = request.params.name;

  // ── Pro tier access check ──────────────────────────────────────────────────
  const access = await checkToolAccess(toolName);
  if (!access.allowed) {
    return createAccessDeniedResponse(toolName, access.message!);
  }

  // Record anonymous telemetry (fire-and-forget, non-blocking, opt-out via
  // SCENEVIEW_TELEMETRY=0). See `telemetry.ts` and `PRIVACY.md`.
  recordToolCall(toolName, getToolTier(toolName));

  // Record usage for billing (async, fire-and-forget)
  const apiKey = getConfiguredApiKey();
  if (apiKey) {
    recordUsage(apiKey, toolName).catch(() => {});
  }

  // The dispatcher returns the narrower SceneView `ToolResult` shape, which
  // structurally matches the MCP SDK's `CallToolResult` but TS can't prove
  // it (the SDK's zod-derived type has additional optional members).
  const result = await dispatchTool(
    toolName,
    request.params.arguments as Record<string, unknown> | undefined,
  );
  return result as unknown as { content: Array<{ type: "text"; text: string }>; isError?: boolean };
});

const transport = new StdioServerTransport();
await server.connect(transport);
