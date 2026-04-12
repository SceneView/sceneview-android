/**
 * architecture-mcp library — REAL handlers from the npm package.
 *
 * Wraps the 10 pure functions exported by `architecture-mcp` into the
 * hub's `ToolDefinition[]` + `dispatchTool` contract. The upstream
 * package uses `server.tool()` (MCP SDK) for its stdio entrypoint, but
 * also exports each tool handler as a named pure function + Zod schema
 * from `architecture-mcp/dist/tools/index.js`.
 *
 * Each tool below:
 *   1. Declares a JSON Schema matching the Zod shape (hand-translated
 *      because `zodToJsonSchema` is a heavy dep we don't want in the
 *      Worker bundle — the schemas are stable and verified by tests)
 *   2. Dispatches to the upstream pure function
 *   3. Returns the result as a JSON text content block
 *
 * Tier mapping (see src/mcp/access.ts FREE_TOOLS):
 *   Free: generate_3d_concept, cost_estimate (discovery hooks)
 *   Pro: everything else (detailed output, professional quality)
 */

import {
  generate3dConcept,
  createFloorPlan,
  interiorDesign,
  materialPalette,
  lightingAnalysis,
  renderWalkthrough,
  costEstimate,
  exportSpecs,
  generate3dWalkthrough,
  sustainabilityAnalysis,
} from "architecture-mcp/dist/tools/index.js";

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "architecture__generate_3d_concept",
    description:
      "Generate a 3D architectural concept from a text description. Returns style-specific materials, passive design strategies, massing recommendations, and color palette.",
    inputSchema: {
      type: "object",
      properties: {
        description: { type: "string" },
        style: { type: "string" },
        budget_range: { type: "string" },
        climate: { type: "string" },
        lot_size_sqm: { type: "number" },
      },
      required: ["description"],
    },
  },
  {
    name: "architecture__create_floor_plan",
    description:
      "Create a detailed floor plan from room descriptions with dimensions. Returns room layout, SVG visualization, compliance checks, and circulation notes.",
    inputSchema: {
      type: "object",
      properties: {
        rooms: { type: "array" },
        width_m: { type: "number" },
        depth_m: { type: "number" },
        stories: { type: "number" },
        style: { type: "string" },
      },
      required: ["rooms"],
    },
  },
  {
    name: "architecture__interior_design",
    description:
      "Generate interior design recommendations: mood board, color palette, furniture layout with placement rules, and budget estimate.",
    inputSchema: {
      type: "object",
      properties: {
        room_type: { type: "string" },
        style: { type: "string" },
        dimensions: { type: "object" },
        budget: { type: "number" },
      },
      required: ["room_type"],
    },
  },
  {
    name: "architecture__material_palette",
    description:
      "Generate specification-grade material suggestions for floors, walls, and ceilings with pricing, durability ratings, and sustainability scores.",
    inputSchema: {
      type: "object",
      properties: {
        room_type: { type: "string" },
        style: { type: "string" },
        budget_range: { type: "string" },
        climate: { type: "string" },
      },
      required: ["room_type"],
    },
  },
  {
    name: "architecture__lighting_analysis",
    description:
      "Analyze and design a layered lighting plan: ambient, task, and accent lighting with fixture specifications, lux calculations, and daylighting analysis.",
    inputSchema: {
      type: "object",
      properties: {
        room_type: { type: "string" },
        dimensions: { type: "object" },
        natural_light: { type: "string" },
        style: { type: "string" },
      },
      required: ["room_type"],
    },
  },
  {
    name: "architecture__render_walkthrough",
    description:
      "Generate a 3D walkthrough specification with camera path, render settings, and embeddable viewer code. Pro tier required.",
    inputSchema: {
      type: "object",
      properties: {
        rooms: { type: "array" },
        style: { type: "string" },
        quality: { type: "string" },
      },
      required: ["rooms"],
    },
  },
  {
    name: "architecture__cost_estimate",
    description:
      "Generate a rough cost estimate for renovation or new build: materials, labor, furniture, professional fees, contingency, and timeline.",
    inputSchema: {
      type: "object",
      properties: {
        project_type: { type: "string" },
        area_sqm: { type: "number" },
        quality_level: { type: "string" },
        location: { type: "string" },
      },
      required: ["project_type", "area_sqm"],
    },
  },
  {
    name: "architecture__export_specs",
    description:
      "Generate a technical specifications document for contractors: room-by-room finishes, electrical/plumbing points, HVAC sizing, code compliance, and required drawings. Pro tier required.",
    inputSchema: {
      type: "object",
      properties: {
        rooms: { type: "array" },
        building_type: { type: "string" },
        code_region: { type: "string" },
      },
      required: ["rooms"],
    },
  },
  {
    name: "architecture__generate_3d_walkthrough",
    description:
      "Generate an animated 3D walkthrough camera path through a building. Creates waypoints, camera movements, lighting, audio, and render settings. Pro tier required.",
    inputSchema: {
      type: "object",
      properties: {
        rooms: { type: "array" },
        duration_seconds: { type: "number" },
        style: { type: "string" },
      },
      required: ["rooms"],
    },
  },
  {
    name: "architecture__sustainability_analysis",
    description:
      "Analyze building energy efficiency, materials sustainability, water conservation, and carbon footprint. Provides certification gap analysis (LEED/BREEAM/Passive House) and improvement recommendations.",
    inputSchema: {
      type: "object",
      properties: {
        building_type: { type: "string" },
        area_sqm: { type: "number" },
        climate: { type: "string" },
        materials: { type: "array" },
        energy_source: { type: "string" },
      },
      required: ["building_type", "area_sqm"],
    },
  },
];

const HANDLERS: Record<string, (args: Record<string, unknown>) => unknown> = {
  architecture__generate_3d_concept: (a) => generate3dConcept(a as never),
  architecture__create_floor_plan: (a) => createFloorPlan(a as never),
  architecture__interior_design: (a) => interiorDesign(a as never),
  architecture__material_palette: (a) => materialPalette(a as never),
  architecture__lighting_analysis: (a) => lightingAnalysis(a as never),
  architecture__render_walkthrough: (a) => renderWalkthrough(a as never),
  architecture__cost_estimate: (a) => costEstimate(a as never),
  architecture__export_specs: (a) => exportSpecs(a as never),
  architecture__generate_3d_walkthrough: (a) => generate3dWalkthrough(a as never),
  architecture__sustainability_analysis: (a) => sustainabilityAnalysis(a as never),
};

export async function dispatchTool(
  toolName: string,
  args: Record<string, unknown> | undefined,
  _ctx: DispatchContext = {},
): Promise<ToolResult> {
  const handler = HANDLERS[toolName];
  if (!handler) {
    return {
      content: [{ type: "text", text: `Unknown architecture tool: ${toolName}` }],
      isError: true,
    };
  }
  try {
    const result = handler(args ?? {});
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2) +
            "\n\n---\n*Not professional architectural advice. Consult a licensed architect.*",
        },
      ],
    };
  } catch (err) {
    return {
      content: [
        {
          type: "text",
          text: `architecture-mcp error: ${err instanceof Error ? err.message : String(err)}`,
        },
      ],
      isError: true,
    };
  }
}
