/**
 * healthcare-3d-mcp library — REAL handlers vendored from the
 * sceneview monorepo (`mcp/packages/healthcare/src/tools.ts`).
 *
 * Second library to graduate from pilot stub to production code.
 * Same pattern as automotive-3d.ts — thin re-export of the
 * upstream `TOOL_DEFINITIONS` + `dispatchTool` so the hub always
 * runs the canonical version of the healthcare tools.
 *
 * IMPORTANT — contract preservation:
 *
 * healthcare-3d-mcp is VISUALISATION ONLY. Every upstream tool
 * generates 3D viewer composables (anatomy, molecules, medical
 * imaging, surgical planning demos, dental). The package NEVER
 * diagnoses, prescribes, or interprets real patient data. That
 * contract is enforced in the tool descriptions themselves — see
 * the upstream file for the exact wording. No hub-side
 * modification of that wording is allowed (doing so would let
 * the hub silently drift away from the educational framing).
 *
 * Tier mapping: `list_medical_models` and
 * `validate_medical_code` are the free-tier discovery tools
 * (see src/mcp/access.ts FREE_TOOLS whitelist). The five `get_*`
 * generators are Pro.
 */

export {
  TOOL_DEFINITIONS,
  dispatchTool,
} from "../../../mcp/packages/healthcare/src/tools.js";
