/**
 * Multiplexed MCP tool registry for the hub gateway (Gateway #2).
 *
 * Structurally identical to mcp-gateway/src/mcp/registry.ts — the two
 * files are intentionally copy-paste siblings so a future refactor can
 * extract them into a shared workspace package. Do not diverge.
 *
 * MVP scope: the registry contains exactly ONE pilot library
 * (`architecture-mcp`). The other 15+ portfolio MCPs will be wired in
 * follow-up sessions, one package at a time:
 *
 *   - realestate-mcp        (1 276 DL/mo, sceneview-tools)
 *   - french-admin-mcp      (1 268 DL/mo, thomasgorisse)
 *   - ecommerce-3d-mcp      (1 153 DL/mo, sceneview-tools)
 *   - legal-docs-mcp          (789 DL/mo, orphan — needs repo)
 *   - finance-mcp             (585 DL/mo, mcp-tools-lab)
 *   - education-mcp           (566 DL/mo, mcp-tools-lab)
 *   - social-media-mcp        (341 DL/mo, thomasgorisse)
 *   - health-fitness-mcp      (335 DL/mo, thomasgorisse — needs repo)
 *   - automotive-3d-mcp     (sceneview monorepo)
 *   - healthcare-3d-mcp     (sceneview monorepo)
 *
 * sceneview-mcp is intentionally EXCLUDED — it has its own gateway.
 *
 * When adding a new library:
 *   1. Drop a module in `../libraries/<id>.ts` that exports
 *      `TOOL_DEFINITIONS` and `dispatchTool`.
 *   2. Add an entry to the LIBRARIES array below.
 *   3. Prefix every tool name with `<id>__` to avoid cross-package
 *      collisions. Collisions are detected at import time and throw
 *      a descriptive error, so the Worker fails fast on startup.
 */

import * as ArchitectureTools from "../libraries/architecture.js";

import type {
  DispatchContext,
  ToolDefinition,
  ToolLibrary,
  ToolResult,
} from "./types.js";

// ── Raw library list ────────────────────────────────────────────────────────

const LIBRARIES: ToolLibrary[] = [
  {
    id: "architecture",
    label: "architecture-mcp",
    definitions: ArchitectureTools.TOOL_DEFINITIONS,
    dispatch: (name, args, ctx) => ArchitectureTools.dispatchTool(name, args, ctx),
  },
];

// ── Build a name → library lookup and fail fast on collisions ──────────────

const OWNERS: Map<string, ToolLibrary> = (() => {
  const owners = new Map<string, ToolLibrary>();
  const collisions: Array<{ name: string; a: string; b: string }> = [];

  for (const lib of LIBRARIES) {
    for (const def of lib.definitions) {
      const existing = owners.get(def.name);
      if (existing) {
        collisions.push({ name: def.name, a: existing.label, b: lib.label });
        continue;
      }
      owners.set(def.name, lib);
    }
  }

  if (collisions.length > 0) {
    const details = collisions
      .map((c) => `  - "${c.name}" is defined by both ${c.a} and ${c.b}`)
      .join("\n");
    throw new Error(
      `Hub MCP registry: tool name collision detected between packages.\n${details}\n` +
        `Rename the offending tool in one of the packages before redeploying the gateway.`,
    );
  }

  return owners;
})();

// ── Public API ──────────────────────────────────────────────────────────────

/** Merged list of tool definitions from every registered library. */
export function getAllTools(): ToolDefinition[] {
  const out: ToolDefinition[] = [];
  for (const lib of LIBRARIES) {
    for (const def of lib.definitions) out.push(def);
  }
  return out;
}

/** Read-only view of the registry — used by the /health endpoint. */
export function getRegistrySummary(): {
  libraries: Array<{ id: string; label: string; toolCount: number }>;
  totalTools: number;
} {
  const libraries = LIBRARIES.map((lib) => ({
    id: lib.id,
    label: lib.label,
    toolCount: lib.definitions.length,
  }));
  return {
    libraries,
    totalTools: libraries.reduce((n, l) => n + l.toolCount, 0),
  };
}

/** Returns the tool definition for a given name, or `undefined`. */
export function getToolDefinition(name: string): ToolDefinition | undefined {
  const owner = OWNERS.get(name);
  if (!owner) return undefined;
  return owner.definitions.find((d) => d.name === name);
}

/** Routes a tool call to the owning library. */
export async function dispatch(
  toolName: string,
  args: Record<string, unknown> | undefined,
  ctx: DispatchContext = {},
): Promise<ToolResult> {
  const owner = OWNERS.get(toolName);
  if (!owner) {
    return {
      content: [{ type: "text", text: `Unknown tool: ${toolName}` }],
      isError: true,
    };
  }
  return owner.dispatch(toolName, args, ctx);
}

/** Exposed for tests and diagnostics. */
export const __internals = { LIBRARIES, OWNERS };
