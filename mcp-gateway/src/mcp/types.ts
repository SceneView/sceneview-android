/**
 * Shared MCP types for the gateway.
 *
 * These types are intentionally kept identical to the per-package
 * `ToolDefinition` / `ToolResult` / `DispatchContext` declarations so the
 * registry can treat every upstream lib uniformly without cross-package
 * type imports (the repo has no npm workspaces, so structural compatibility
 * is the glue).
 *
 * Keep this file in sync with:
 *   - mcp/src/tools/types.ts
 *   - mcp/packages/automotive/src/tools.ts (inline types)
 *   - mcp/packages/gaming/src/tools.ts
 *   - mcp/packages/healthcare/src/tools.ts
 *   - mcp/packages/interior/src/tools.ts
 */

/** An MCP content block — always text for SceneView tools. */
export interface ToolTextContent {
  type: "text";
  text: string;
}

/** Result returned by every SceneView MCP tool handler. */
export interface ToolResult {
  content: ToolTextContent[];
  isError?: boolean;
}

/** Tool metadata exposed to MCP clients via `tools/list`. */
export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: {
    type: "object";
    properties?: Record<string, unknown>;
    required?: string[];
    additionalProperties?: boolean;
  };
}

/**
 * Per-request context injected by the gateway before calling a handler.
 *
 * Handlers are pure — they read this bag but do not authenticate, rate
 * limit, or write billing events themselves.
 */
export interface DispatchContext {
  /** Authenticated user id from the D1 `users` table. */
  userId?: string;
  /** API key row id from the D1 `api_keys` table. */
  apiKeyId?: string;
  /** Resolved subscription tier. */
  tier?: "free" | "pro" | "team";
  /** Opaque extension bag (request id, headers, ...) reserved for future use. */
  extras?: Record<string, unknown>;
}

/**
 * The shape exposed by every upstream tool library. All five MCP packages
 * satisfy this structurally, which lets the registry import them without
 * any shared package or workspace.
 */
export interface ToolLibrary {
  /** Identifier used for logging / error messages and for duplicate detection. */
  id: string;
  /** Human name shown in error messages when collisions are reported. */
  label: string;
  /** Static tool metadata (schemas + descriptions). */
  definitions: readonly ToolDefinition[];
  /** Pure dispatcher — enforces NO auth/rate-limit/billing itself. */
  dispatch: (
    toolName: string,
    args: Record<string, unknown> | undefined,
    ctx?: DispatchContext,
  ) => Promise<ToolResult>;
}
