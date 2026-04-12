/**
 * Proxy helper for the hub-mcp lite package.
 *
 * Same pattern as mcp/src/proxy.ts (Gateway #1) — free-tier tools run
 * locally, Pro-tier tools are forwarded as JSON-RPC `tools/call` requests
 * to the hosted hub gateway.
 *
 * The gateway lives at
 *   https://hub-mcp.mcp-tools-lab.workers.dev/mcp
 * Override with `HUB_MCP_URL` if you run a self-hosted fork or staging.
 *
 * This module is intentionally dependency-free: it relies on the global
 * `fetch` that Node 18+ exposes and on nothing else.
 */

/** Default URL of the hosted hub gateway. */
export const DEFAULT_GATEWAY_URL =
  "https://hub-mcp.mcp-tools-lab.workers.dev/mcp";

/** Public pricing/signup page shown in stubs when no API key is set. */
export const DEFAULT_PRICING_URL =
  "https://hub-mcp.mcp-tools-lab.workers.dev/pricing";

/** Response shape returned by `dispatchProxyToolCall`. */
export interface ProxyToolResult {
  content: Array<{ type: "text"; text: string }>;
  isError?: boolean;
}

/** Options for {@link dispatchProxyToolCall}. */
export interface ProxyOptions {
  apiKey?: string;
  gatewayUrl?: string;
  fetchImpl?: typeof fetch;
}

let nextRpcId = 1;

/**
 * Forwards a Pro-tier tool call to the hosted hub gateway and returns
 * its `ToolResult`. Network, HTTP, and JSON-RPC errors are converted
 * into a user-visible error block.
 *
 * When no API key is configured, returns a helpful stub that points at
 * the pricing page.
 */
export async function dispatchProxyToolCall(
  toolName: string,
  args: Record<string, unknown> | undefined,
  options: ProxyOptions = {},
): Promise<ProxyToolResult> {
  const apiKey = options.apiKey ?? process.env.HUB_MCP_API_KEY;
  if (!apiKey) {
    return {
      content: [
        {
          type: "text",
          text:
            `## Pro feature\n\n` +
            `\`${toolName}\` is a Hub MCP Pro tool. ` +
            `Set \`HUB_MCP_API_KEY\` to an API key from ` +
            `${DEFAULT_PRICING_URL} to unlock it.\n\n` +
            `Pro unlocks 39+ premium tools across architecture, real estate, ` +
            `finance, education, legal, healthcare, automotive, and more.`,
        },
      ],
      isError: true,
    };
  }

  const gatewayUrl =
    options.gatewayUrl ?? process.env.HUB_MCP_GATEWAY_URL ?? DEFAULT_GATEWAY_URL;
  const fetchImpl = options.fetchImpl ?? fetch;

  const requestBody = {
    jsonrpc: "2.0" as const,
    id: nextRpcId++,
    method: "tools/call",
    params: { name: toolName, arguments: args ?? {} },
  };

  let response: Response;
  try {
    response = await fetchImpl(gatewayUrl, {
      method: "POST",
      headers: {
        authorization: `Bearer ${apiKey}`,
        "content-type": "application/json",
        accept: "application/json",
      },
      body: JSON.stringify(requestBody),
    });
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err);
    return {
      content: [
        {
          type: "text",
          text:
            `Failed to reach Hub MCP gateway (${gatewayUrl}): ${detail}.\n\n` +
            `The gateway may be temporarily down. Try again in a few seconds, ` +
            `or check status at ${DEFAULT_PRICING_URL}.`,
        },
      ],
      isError: true,
    };
  }

  const text = await response.text().catch(() => "");

  if (response.status === 401 || response.status === 403) {
    return {
      content: [
        {
          type: "text",
          text:
            `## Invalid or expired API key\n\n` +
            `The gateway rejected your \`HUB_MCP_API_KEY\` (HTTP ${response.status}).\n\n` +
            `- If you just subscribed, make sure you copied the full key from ` +
            `the Stripe success page.\n` +
            `- If your subscription was cancelled, reactivate it at ${DEFAULT_PRICING_URL}.\n\n` +
            (text ? `Gateway response: ${text}` : ""),
        },
      ],
      isError: true,
    };
  }

  if (response.status === 429) {
    return {
      content: [
        {
          type: "text",
          text:
            `## Rate limited\n\n` +
            `You've hit the rate limit for \`${toolName}\` (HTTP 429). ` +
            `Wait a few seconds and retry, or upgrade your tier at ` +
            `${DEFAULT_PRICING_URL}.\n\n` +
            (text ? `Gateway response: ${text}` : ""),
        },
      ],
      isError: true,
    };
  }

  if (!response.ok) {
    return {
      content: [
        {
          type: "text",
          text:
            `Gateway HTTP ${response.status} while calling ${toolName}. ` +
            (text || "No response body."),
        },
      ],
      isError: true,
    };
  }

  let parsed: {
    result?: {
      content?: Array<{ type: "text"; text: string }>;
      isError?: boolean;
    };
    error?: { message?: string; data?: unknown };
  };
  try {
    parsed = JSON.parse(text) as typeof parsed;
  } catch {
    return {
      content: [
        {
          type: "text",
          text: `Gateway returned non-JSON response: ${text}`,
        },
      ],
      isError: true,
    };
  }

  if (parsed.error) {
    return {
      content: [
        {
          type: "text",
          text:
            parsed.error.message ??
            `Gateway error while calling ${toolName}.`,
        },
      ],
      isError: true,
    };
  }

  const result = parsed.result;
  return {
    content: result?.content ?? [{ type: "text", text: "" }],
    isError: result?.isError ?? false,
  };
}

/**
 * Returns true when proxy mode is active: an API key is configured
 * (either via `HUB_MCP_API_KEY` env var or an explicit override).
 */
export function isProxyConfigured(apiKey?: string): boolean {
  if (apiKey && apiKey.length > 0) return true;
  return !!process.env.HUB_MCP_API_KEY;
}
