/**
 * Architecture MCP pilot library.
 *
 * The upstream package `architecture-mcp` (#6 DL/mo, ~1 134) lives in
 * its own repo (`github.com/sceneview-tools/architecture-mcp`) and is
 * not yet vendored into this monorepo. For the MVP we expose a small
 * set of STUB tool definitions that let the hub gateway:
 *
 *   - answer `tools/list` with real schemas,
 *   - dispatch `tools/call` without throwing,
 *   - return a clear "not yet wired" message so early pilot users
 *     understand the state of the integration.
 *
 * Next step (follow-up session):
 *   1. Publish `@sceneview-tools/architecture-mcp-core` as a pure-ESM
 *      package exporting `TOOL_DEFINITIONS` + `dispatchTool` (the same
 *      shape every library in mcp-gateway/src/mcp/registry.ts uses).
 *   2. Replace the body of this file with a thin re-export:
 *
 *        import * as Upstream from "@sceneview-tools/architecture-mcp-core";
 *        export const TOOL_DEFINITIONS = Upstream.TOOL_DEFINITIONS;
 *        export const dispatchTool = Upstream.dispatchTool;
 *
 *   3. Bump `architecture-mcp` on npm to a lite version (2.0.0-beta.1)
 *      that forwards tool calls through the hub gateway via fetch,
 *      mirroring the `sceneview-mcp@4.0.0-beta.1` lite design.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "architecture__list_building_types",
    description:
      "List architectural building typologies supported by architecture-mcp (residential, commercial, industrial, civic, mixed-use, landscape).",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
  },
  {
    name: "architecture__search_models",
    description:
      "Search the architecture-mcp model catalogue by typology, style, and material. Returns glTF/USDZ asset URLs ready to drop into a SceneView scene.",
    inputSchema: {
      type: "object",
      properties: {
        typology: {
          type: "string",
          description:
            "Building typology (residential, commercial, industrial, civic, mixed-use, landscape).",
        },
        style: {
          type: "string",
          description: "Architectural style (modern, brutalist, art-deco, etc.).",
        },
        material: {
          type: "string",
          description: "Primary material (concrete, glass, wood, brick, steel).",
        },
        limit: {
          type: "number",
          description: "Maximum number of results (1-50, default 10).",
        },
      },
      additionalProperties: false,
    },
  },
  {
    name: "architecture__get_floor_plan",
    description:
      "Generate a floor plan sketch as SVG for a given building typology and footprint.",
    inputSchema: {
      type: "object",
      properties: {
        typology: {
          type: "string",
          description: "Building typology.",
        },
        widthMeters: { type: "number" },
        depthMeters: { type: "number" },
        storeys: { type: "number" },
      },
      required: ["typology", "widthMeters", "depthMeters"],
      additionalProperties: false,
    },
  },
];

/**
 * Pilot dispatcher — returns a clear "not yet wired" message until the
 * upstream architecture-mcp core package is imported (see file header).
 */
export async function dispatchTool(
  toolName: string,
  _args: Record<string, unknown> | undefined,
  _ctx: DispatchContext = {},
): Promise<ToolResult> {
  return {
    content: [
      {
        type: "text",
        text:
          `architecture-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway (MVP scaffold) but the upstream implementation is not yet ` +
          `vendored. See hub-gateway/src/libraries/architecture.ts for the ` +
          `wiring checklist. Track progress in the multi-gateway-sprint worktree.`,
      },
    ],
    isError: false,
  };
}
