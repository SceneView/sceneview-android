/**
 * Shared MCP types for the hub gateway.
 *
 * Mirrors mcp-gateway/src/mcp/types.ts exactly — the two gateways use
 * structurally compatible types so a future refactor can extract them
 * into a shared workspace package. Do not diverge without updating
 * both sides.
 */

/** An MCP content block — always text for portfolio tools. */
export interface ToolTextContent {
  type: "text";
  text: string;
}

/** Result returned by every tool handler. */
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
 *
 * The tier type matches the shared D1 `users.tier` column
 * (`free | pro | team`). The hub treats `pro` as "Portfolio Access"
 * regardless of which Stripe product paid for it — see
 * src/db/schema.ts for the rationale.
 */
export interface DispatchContext {
  /** Authenticated user id from the D1 `users` table (shared with Gateway #1). */
  userId?: string;
  /** API key row id from the D1 `api_keys` table. */
  apiKeyId?: string;
  /** Resolved subscription tier as stored in users.tier. */
  tier?: "free" | "pro" | "team";
  /** Opaque extension bag (request id, headers, ...) reserved for future use. */
  extras?: Record<string, unknown>;
}

/**
 * Contract every upstream tool library in the hub registry must satisfy.
 * Structurally identical to Gateway #1's ToolLibrary so that the
 * registry module is copy-pasteable between the two gateways.
 */
export interface ToolLibrary {
  /** Identifier used for logging, error messages, and duplicate detection. */
  id: string;
  /** Human name shown in error messages when tool collisions are reported. */
  label: string;
  /** Static tool metadata (schemas + descriptions). */
  definitions: readonly ToolDefinition[];
  /** Pure dispatcher — NEVER enforces auth/rate-limit/billing itself. */
  dispatch: (
    toolName: string,
    args: Record<string, unknown> | undefined,
    ctx?: DispatchContext,
  ) => Promise<ToolResult>;
}
