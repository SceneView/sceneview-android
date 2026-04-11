/**
 * legal-docs-mcp pilot library.
 *
 * Upstream: `legal-docs-mcp` (#8 DL/mo, ~789), ORPHAN on npm — no
 * GitHub repo exists yet. Registered as a stub here so the hub
 * gateway surfaces it immediately; the real handler + repo creation
 * is tracked as a blocker in .claude/SESSION_PROMPT.md.
 *
 * IMPORTANT: legal-docs-mcp NEVER gives legal advice. It generates
 * contract templates and clause libraries, flagged as "informational,
 * not legal counsel" in every tool description. The dispatcher must
 * enforce this even when the upstream code ships.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "legal_docs__list_templates",
    description:
      "List contract templates available in the library (NDA, employment, freelance, SaaS ToS, privacy policy, licence agreement). Templates are informational, not legal counsel.",
    inputSchema: {
      type: "object",
      properties: {
        jurisdiction: {
          type: "string",
          description: "ISO country code (US, FR, UK, DE, ...).",
        },
      },
      additionalProperties: false,
    },
  },
  {
    name: "legal_docs__generate_clause",
    description:
      "Generate a contract clause from a structured intent (confidentiality, termination, IP assignment, arbitration, warranty). Returns markdown + plain text variants. Informational only.",
    inputSchema: {
      type: "object",
      properties: {
        clauseType: { type: "string" },
        tone: {
          type: "string",
          description: "formal, plain_language, bilingual.",
        },
        jurisdiction: { type: "string" },
      },
      required: ["clauseType"],
      additionalProperties: false,
    },
  },
  {
    name: "legal_docs__review_nda",
    description:
      "Review an NDA draft and return a structured checklist of missing elements (definition of confidential info, term, return of materials, exclusions, governing law). Informational only.",
    inputSchema: {
      type: "object",
      properties: {
        draftMarkdown: { type: "string" },
      },
      required: ["draftMarkdown"],
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
          `legal-docs-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `legal-docs-mcp is INFORMATIONAL ONLY and never provides legal ` +
          `advice. See hub-gateway/src/libraries/legal-docs.ts for the ` +
          `wiring checklist. Upstream repo creation still pending.`,
      },
    ],
  };
}
