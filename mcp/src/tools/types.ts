/**
 * Public types for the SceneView MCP tool library.
 *
 * These are the contract shared with the `mcp-gateway` package. Any change here
 * MUST be reflected in `mcp-gateway/src/mcp/types.ts` (or that file should
 * import from this one once workspaces are set up).
 */

/** An MCP content block — always text for SceneView tools. */
export interface ToolTextContent {
  type: "text";
  text: string;
}

/** The shape returned by every SceneView MCP tool handler. */
export interface ToolResult {
  content: ToolTextContent[];
  isError?: boolean;
}

/**
 * JSONSchema-ish input schema attached to a tool definition.
 *
 * We intentionally type this loosely (`unknown` arguments) because the real
 * validation happens inside each handler. The MCP SDK exposes these schemas
 * verbatim to clients via `ListToolsRequestSchema`.
 */
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
 * Per-request context passed to handlers by the dispatcher.
 *
 * Handlers running in the stdio npm package get an empty context. The gateway
 * populates fields like `userId`, `apiKeyId`, and `tier` once it has
 * authenticated the caller.
 */
export interface DispatchContext {
  /** Authenticated user id. Set by the gateway, `undefined` in stdio. */
  userId?: string;
  /** API key row id. Set by the gateway, `undefined` in stdio. */
  apiKeyId?: string;
  /** Resolved subscription tier. Defaults to `"free"` in stdio. */
  tier?: "free" | "pro" | "team";
  /** Free-form key/value bag for future extensibility (request id, headers). */
  extras?: Record<string, unknown>;
}
