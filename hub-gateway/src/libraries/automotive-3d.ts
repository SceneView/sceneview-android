/**
 * automotive-3d-mcp pilot library.
 *
 * Upstream: `automotive-3d-mcp`, published from the sceneview
 * monorepo under `mcp/packages/automotive/`. When vendoring lands,
 * the dispatcher here will re-export the same `TOOL_DEFINITIONS` +
 * `dispatchTool` already shipped on npm — no code duplication.
 *
 * Caveat: the `files[]` glob in that package was hardened in
 * f38339d8 (the base of this worktree); before re-publishing on
 * the hub, re-run `npm pack --dry-run` to verify the tarball ships
 * every dist module.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "automotive3d__list_brands",
    description:
      "List automotive brands with 3D models available in the catalogue (Tesla, BMW, Ford, Toyota, etc.).",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
  },
  {
    name: "automotive3d__search_vehicles",
    description:
      "Search the vehicle catalogue by brand, body style, fuel, year. Returns glTF/USDZ model URLs for SceneView.",
    inputSchema: {
      type: "object",
      properties: {
        brand: { type: "string" },
        bodyStyle: {
          type: "string",
          description: "sedan, suv, coupe, truck, van, convertible, hatchback.",
        },
        fuel: {
          type: "string",
          description: "gasoline, diesel, hybrid, electric.",
        },
        yearMin: { type: "number" },
        yearMax: { type: "number" },
      },
      additionalProperties: false,
    },
  },
  {
    name: "automotive3d__configure_variant",
    description:
      "Return the configurable options (colour, wheels, trim, interior) for a given vehicle model so an AI agent can build a configurator UI.",
    inputSchema: {
      type: "object",
      properties: {
        modelId: { type: "string" },
      },
      required: ["modelId"],
      additionalProperties: false,
    },
  },
];

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
          `automotive-3d-mcp pilot stub: ${toolName} is registered on the ` +
          `hub gateway. Upstream package already ships on npm from the ` +
          `sceneview monorepo (mcp/packages/automotive/); vendor its ` +
          `TOOL_DEFINITIONS + dispatchTool here in the next session.`,
      },
    ],
  };
}
