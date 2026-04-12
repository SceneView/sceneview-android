/**
 * automotive-3d-mcp library — REAL handlers vendored from the
 * sceneview monorepo (`mcp/packages/automotive/src/tools.ts`).
 *
 * Thin re-export that keeps hub in sync with the canonical package.
 * Exports are a thin re-export of the upstream `TOOL_DEFINITIONS`
 * + `dispatchTool` so a bug fix in the upstream package lands on
 * the hub the next time `wrangler deploy` runs, with no
 * duplication and no drift.
 *
 * Path rationale: relative `../../../mcp/packages/automotive/src`
 * because the repo has no npm workspaces — Gateway #1 uses the
 * same pattern in `mcp-gateway/src/mcp/registry.ts`. TypeScript's
 * `bundler` moduleResolution rewrites the `.js` extensions at
 * build time and Hono's bundled Worker at runtime.
 *
 * Tool name scheme: upstream tools are NOT prefixed
 * (`get_car_configurator`, `list_car_models`, etc.). That's by
 * design — the stdio version of this package uses the same names,
 * so an MCP client configured for automotive-3d-mcp's lite proxy
 * will see identical tool names on the hosted gateway. No
 * migration pain.
 *
 * Tier mapping: `list_car_models` and `validate_automotive_code`
 * are the free-tier catalog discovery tools (see src/mcp/access.ts
 * FREE_TOOLS whitelist). All six `get_*` generators are Pro — they
 * produce real Kotlin composables ready to paste into a project.
 */

export {
  TOOL_DEFINITIONS,
  dispatchTool,
} from "../../../mcp/packages/automotive/src/tools.js";
