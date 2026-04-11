/**
 * Real estate MCP pilot library.
 *
 * Upstream: `realestate-mcp` (#2 DL/mo of the non-SceneView portfolio,
 * ~1 276), owned by the `sceneview-tools` GitHub org. Like
 * `architecture.ts`, this module ships STUB definitions + a "not yet
 * wired" dispatcher so the hub's `tools/list` surface is realistic
 * while the real handlers get vendored in a follow-up session.
 *
 * Wiring checklist (identical for every hub library):
 *   1. Publish `@sceneview-tools/realestate-mcp-core` exposing
 *      `TOOL_DEFINITIONS` + `dispatchTool`.
 *   2. Replace this file's body with a thin re-export.
 *   3. Bump `realestate-mcp` on npm to a 2.x lite proxy release.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "realestate__search_listings",
    description:
      "Search real estate listings by location, price range, beds/baths, and property type. Returns structured records with photos and listing URLs.",
    inputSchema: {
      type: "object",
      properties: {
        location: { type: "string", description: "City, ZIP, or neighborhood." },
        minPrice: { type: "number" },
        maxPrice: { type: "number" },
        minBeds: { type: "number" },
        propertyType: {
          type: "string",
          description: "house, apartment, condo, townhouse, land.",
        },
        limit: { type: "number" },
      },
      required: ["location"],
      additionalProperties: false,
    },
  },
  {
    name: "realestate__get_listing",
    description:
      "Retrieve full details for a single listing by id, including price history, disclosures, and HOA data.",
    inputSchema: {
      type: "object",
      properties: {
        listingId: { type: "string" },
      },
      required: ["listingId"],
      additionalProperties: false,
    },
  },
  {
    name: "realestate__estimate_value",
    description:
      "Automated valuation model (AVM) estimate for a property address, with confidence interval and comparables.",
    inputSchema: {
      type: "object",
      properties: {
        address: { type: "string" },
      },
      required: ["address"],
      additionalProperties: false,
    },
  },
  {
    name: "realestate__staging_assets",
    description:
      "List 3D staging assets (furniture, fixtures, decor) compatible with SceneView's AR preview.",
    inputSchema: {
      type: "object",
      properties: {
        roomType: {
          type: "string",
          description: "living_room, kitchen, bedroom, office, outdoor.",
        },
        style: {
          type: "string",
          description: "scandinavian, mid_century, industrial, farmhouse.",
        },
      },
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
          `realestate-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `See hub-gateway/src/libraries/realestate.ts for the wiring checklist.`,
      },
    ],
  };
}
