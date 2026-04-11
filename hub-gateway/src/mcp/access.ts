/**
 * Per-tool tier gate for the hub registry.
 *
 * Unlike Gateway #1 which delegates to `mcp/src/tiers.ts`, the hub
 * owns its own whitelist because the pilot libraries here are stubs
 * with no upstream tier module yet. The whitelist lives in this file
 * as a simple `Set<string>` — easy to audit, easy to grep, easy to
 * extend when a new library lands.
 *
 * Policy:
 *   - `getToolTier(name)` returns "free" if the tool is in the
 *     whitelist, "pro" otherwise (conservative default — unknown
 *     tools are billable).
 *   - `canCallTool(name, ctx)` returns true iff:
 *       * the tool is free, OR
 *       * the context carries tier `pro` or `team` (Portfolio Access
 *         and Team both pay — see src/db/schema.ts for why the D1
 *         `users.tier` column uses `pro` for both).
 *
 * Free whitelist rationale:
 *   - One discovery/list tool per library stays free so anonymous
 *     users can explore the catalogue without paying.
 *   - Any tool that produces expensive output (renders, API calls
 *     to third-party services, bulk searches) is pro-only.
 *
 * When wiring a new library:
 *   1. Add its discovery tool (`{pkg}__list_*`) to FREE_TOOLS.
 *   2. Leave everything else implicit-pro.
 *   3. Add one test case in test/access.test.ts.
 */

import type { DispatchContext } from "./types.js";

export type ToolTier = "free" | "pro";

/**
 * Explicit allowlist of hub tools that free-tier users can call.
 * Everything else defaults to `pro`. Keep this alphabetised for
 * readability — the runtime uses a Set so order doesn't matter.
 */
export const FREE_TOOLS: ReadonlySet<string> = new Set<string>([
  // Stubbed libraries — one discovery tool per library (the
  // catalogue entry point). Tool names use the `{package}__tool`
  // scheme because the stubs were scaffolded from scratch.
  "architecture__list_building_types",
  "ecommerce3d__list_categories",
  "education__build_quiz",
  "finance__compound_interest",
  "french_admin__list_democraties",
  "health_fitness__exercise_form_cues",
  "legal_docs__list_templates",
  "realestate__estimate_value",
  "social_media__suggest_hashtags",

  // REAL libraries vendored from the sceneview monorepo — tool
  // names are the upstream names, NOT prefixed. See
  // src/libraries/automotive-3d.ts and healthcare-3d.ts for the
  // rationale (parity with the stdio version of each package).
  "list_car_models",
  "validate_automotive_code",
  "list_medical_models",
  "validate_medical_code",
]);

/** Returns the tier required to call a tool (defaults to `pro`). */
export function getToolTier(toolName: string): ToolTier {
  return FREE_TOOLS.has(toolName) ? "free" : "pro";
}

/**
 * Returns true iff the given dispatch context is allowed to call
 * the tool. Signature matches what `handleMcpRequest` expects so
 * the transport can call it uniformly.
 */
export function canCallTool(
  toolName: string,
  ctx: DispatchContext | undefined,
): boolean {
  const required = getToolTier(toolName);
  if (required === "free") return true;
  const tier = ctx?.tier;
  return tier === "pro" || tier === "team";
}
