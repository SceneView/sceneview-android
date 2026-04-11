/**
 * ecommerce-3d-mcp pilot library.
 *
 * Upstream: `ecommerce-3d-mcp` (#4 DL/mo, ~1 153), owned by the
 * `sceneview-tools` GitHub org. Product configurators, 3D model
 * search, try-on previews and shoppable scenes.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "ecommerce3d__list_categories",
    description:
      "List product categories supported by the ecommerce-3d catalogue (furniture, apparel, eyewear, jewelry, footwear, home_decor).",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
  },
  {
    name: "ecommerce3d__search_products",
    description:
      "Search the ecommerce-3d catalogue and return 3D models (glTF/USDZ) ready for SceneView rendering or AR try-on.",
    inputSchema: {
      type: "object",
      properties: {
        category: { type: "string" },
        query: { type: "string" },
        maxPriceUSD: { type: "number" },
        hasAR: { type: "boolean" },
        limit: { type: "number" },
      },
      additionalProperties: false,
    },
  },
  {
    name: "ecommerce3d__configurator_options",
    description:
      "Return the configurable options (material, color, size) for a given product SKU so an AI agent can build a configurator UI.",
    inputSchema: {
      type: "object",
      properties: {
        sku: { type: "string" },
      },
      required: ["sku"],
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
          `ecommerce-3d-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `See hub-gateway/src/libraries/ecommerce-3d.ts for the wiring checklist.`,
      },
    ],
  };
}
