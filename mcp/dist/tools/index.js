/**
 * Public entrypoint for the SceneView MCP tool library.
 *
 * Consumers (stdio server in `../index.ts`, hosted gateway in
 * `mcp-gateway/src/mcp/registry.ts`) should import from this file and
 * never reach into `definitions.ts` / `handler.ts` directly.
 */
export { TOOL_DEFINITIONS } from "./definitions.js";
export { dispatchTool, API_DOCS, __resetSponsorCounter } from "./handler.js";
import { TOOL_DEFINITIONS } from "./definitions.js";
/** Returns the full tool definition list (read-only copy). */
export function getAllTools() {
    return [...TOOL_DEFINITIONS];
}
/** Returns the tool definition for a given name, or `undefined`. */
export function getToolDefinition(name) {
    return TOOL_DEFINITIONS.find((t) => t.name === name);
}
