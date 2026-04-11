/**
 * Per-tool tier gate for the multiplexed registry.
 *
 * The sceneview-mcp package already ships a `tiers.ts` module that
 * distinguishes free vs pro tools. We import it via a relative path
 * the same way `registry.ts` imports the upstream tool libraries —
 * the gateway does not use npm workspaces.
 *
 * The gate is intentionally permissive on the `pro` tier entry: any
 * authenticated user on `pro` or `team` can call every tool. Unknown
 * tools fall back to `pro` (conservative default) and are therefore
 * blocked on the free tier. This mirrors the behavior of the stdio
 * implementation so migrations between transports are transparent.
 */

import type { DispatchContext } from "./types.js";
import { getToolTier } from "../../../mcp/src/tiers.js";

/**
 * Returns true if the given dispatch context is allowed to call the
 * tool. This is the signature that `handleMcpRequest` passes in:
 * `(toolName, ctx: DispatchContext | undefined)`.
 */
export function canCallTool(
  toolName: string,
  ctx: DispatchContext | undefined,
): boolean {
  const required = getToolTier(toolName);
  if (required === "free") return true;
  const tier = ctx?.tier;
  // required === "pro" — pro and team users pass, free is blocked.
  return tier === "pro" || tier === "team";
}

/** Re-exports the upstream tier resolver for callers that want the raw value. */
export { getToolTier };
