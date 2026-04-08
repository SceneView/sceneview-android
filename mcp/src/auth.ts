/**
 * Authentication middleware for the SceneView MCP server.
 *
 * Provides tool-level access control based on free/pro tiers,
 * API key validation, and MCP-formatted denial responses.
 */

import { isProTool, PRO_UPGRADE_MESSAGE } from "./tiers.js";
import { getConfiguredApiKey, validateApiKey } from "./billing.js";

// ─── Types ───────────────────────────────────────────────────────────────────

export interface AccessResult {
  allowed: boolean;
  tier: "free" | "pro";
  /** Only set when `allowed` is false — contains an upgrade prompt or error detail. */
  message?: string;
}

// ─── Main middleware ─────────────────────────────────────────────────────────

/**
 * Checks whether the current user is allowed to invoke the given tool.
 *
 * - Free-tier tools are always allowed.
 * - Pro-tier tools require a valid API key with an active subscription.
 */
export async function checkToolAccess(
  toolName: string
): Promise<AccessResult> {
  // Free-tier tools are always accessible
  if (!isProTool(toolName)) {
    return { allowed: true, tier: "free" };
  }

  // Pro tool — check for API key
  const apiKey = getConfiguredApiKey();

  if (!apiKey) {
    return {
      allowed: false,
      tier: "free",
      message: PRO_UPGRADE_MESSAGE,
    };
  }

  // Validate the key against the billing service
  const validation = await validateApiKey(apiKey);

  if (validation.valid) {
    return { allowed: true, tier: "pro" };
  }

  // Key exists but is invalid or subscription expired
  return {
    allowed: false,
    tier: "free",
    message: validation.error ?? "Your API key is invalid or your Pro subscription has expired.",
  };
}

// ─── Tool list filtering ─────────────────────────────────────────────────────

/**
 * Annotates the tool list based on the caller's tier.
 *
 * - Free users see every tool, but pro-only tools get a "[PRO]" prefix on
 *   their description so the AI (and the human) know an upgrade is needed.
 * - Pro users see the list unmodified.
 */
export async function filterToolsForTier(
  tools: Array<{ name: string; [key: string]: unknown }>
): Promise<Array<{ name: string; [key: string]: unknown }>> {
  const apiKey = getConfiguredApiKey();
  let isPro = false;

  if (apiKey) {
    const validation = await validateApiKey(apiKey);
    isPro = validation.valid;
  }

  if (isPro) {
    return tools;
  }

  // Free tier — prefix pro tool descriptions so users can discover them
  return tools.map((tool) => {
    if (!isProTool(tool.name)) {
      return tool;
    }

    const description =
      typeof tool.description === "string" ? tool.description : "";

    return {
      ...tool,
      description: `[PRO] ${description}`,
    };
  });
}

// ─── MCP response helpers ────────────────────────────────────────────────────

/**
 * Builds an MCP-formatted error response for access-denied scenarios.
 *
 * The returned object can be used directly as the handler return value
 * for a `CallToolRequestSchema` handler.
 */
export function createAccessDeniedResponse(
  toolName: string,
  message: string
) {
  return {
    content: [{ type: "text" as const, text: message }],
    isError: true,
  };
}
