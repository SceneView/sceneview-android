/**
 * french-admin-mcp pilot library.
 *
 * Upstream: `french-admin-mcp` (#3 DL/mo, ~1 268), owned by the
 * `thomasgorisse` GitHub org. Bridge to French administrative APIs:
 * impôts, CAF, Ameli, Pôle Emploi, démarches administratives.
 *
 * This is a bridge-API MCP, which historically outperforms pure
 * offline utilities in the portfolio by ~50x on downloads (see
 * project memory: "Bridge API >> utility"). High priority for the
 * hub gateway value prop.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "french_admin__list_democraties",
    description:
      "List supported French administrative demarches (impots, caf, ameli, pole_emploi, urssaf, service_public).",
    inputSchema: {
      type: "object",
      properties: {},
      additionalProperties: false,
    },
  },
  {
    name: "french_admin__search_form",
    description:
      "Search for an official French administrative form (Cerfa) by keyword and get the direct PDF link on service-public.fr.",
    inputSchema: {
      type: "object",
      properties: {
        query: { type: "string" },
        administration: {
          type: "string",
          description: "impots, caf, ameli, pole_emploi, urssaf, prefecture.",
        },
      },
      required: ["query"],
      additionalProperties: false,
    },
  },
  {
    name: "french_admin__calculate_impots",
    description:
      "Estimate French income tax (impot sur le revenu) from taxable income, parts, and year. Returns net due + marginal rate.",
    inputSchema: {
      type: "object",
      properties: {
        revenuNet: { type: "number", description: "Annual taxable income in EUR." },
        parts: { type: "number", description: "Number of fiscal parts." },
        year: { type: "number", description: "Tax year (e.g. 2026)." },
      },
      required: ["revenuNet", "parts"],
      additionalProperties: false,
    },
  },
  {
    name: "french_admin__caf_eligibility",
    description:
      "Check eligibility for CAF benefits (APL, prime d'activite, AAH, RSA) given household situation.",
    inputSchema: {
      type: "object",
      properties: {
        situation: {
          type: "string",
          description: "salarie, independant, etudiant, chomeur, retraite.",
        },
        nbEnfants: { type: "number" },
        revenuMensuel: { type: "number" },
        loyer: { type: "number" },
        codePostal: { type: "string" },
      },
      required: ["situation", "revenuMensuel"],
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
          `french-admin-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `See hub-gateway/src/libraries/french-admin.ts for the wiring checklist.`,
      },
    ],
  };
}
