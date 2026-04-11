/**
 * healthcare-3d-mcp pilot library.
 *
 * Upstream: `healthcare-3d-mcp`, published from the sceneview
 * monorepo under `mcp/packages/healthcare/`. Same vendoring story
 * as automotive — re-export in the next session.
 *
 * IMPORTANT: healthcare-3d-mcp is VISUALISATION ONLY (anatomy,
 * medical equipment, procedure assets). It NEVER diagnoses, never
 * prescribes, never interprets medical data. The dispatcher must
 * enforce that contract even when the real handlers ship.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "healthcare3d__anatomy_search",
    description:
      "Search 3D anatomy assets by body system (skeletal, muscular, cardiovascular, nervous, digestive, respiratory). Returns glTF/USDZ models for SceneView.",
    inputSchema: {
      type: "object",
      properties: {
        system: { type: "string" },
        detailLevel: {
          type: "string",
          description: "overview, intermediate, detailed.",
        },
      },
      required: ["system"],
      additionalProperties: false,
    },
  },
  {
    name: "healthcare3d__procedure_assets",
    description:
      "List 3D assets supporting a medical procedure or training scenario (OR setup, surgical tools, ultrasound, MRI). Educational use only — never a substitute for medical training.",
    inputSchema: {
      type: "object",
      properties: {
        procedure: { type: "string" },
      },
      required: ["procedure"],
      additionalProperties: false,
    },
  },
  {
    name: "healthcare3d__medical_icons",
    description:
      "List 3D medical icons (pill, syringe, stethoscope, cross, wheelchair, ambulance) for SceneView-rendered UI or AR overlays.",
    inputSchema: {
      type: "object",
      properties: {},
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
          `healthcare-3d-mcp pilot stub: ${toolName} is registered on the ` +
          `hub gateway. Visualisation only — never diagnoses or prescribes. ` +
          `Upstream ships from sceneview monorepo (mcp/packages/healthcare/); ` +
          `vendor its TOOL_DEFINITIONS + dispatchTool here in the next session.`,
      },
    ],
  };
}
