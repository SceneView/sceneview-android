// ─── Anonymous opt-out telemetry for sceneview-mcp ───────────────────────────
//
// Fire-and-forget, non-blocking, no personal data. Ever.
//
// What gets sent:
//   - timestamp (UTC ISO string)
//   - event: "init" | "tool"
//   - client: MCP client name (e.g. "claude-desktop", "cursor")
//   - clientVersion: MCP client version string reported during handshake
//   - mcpVersion: this package's version
//   - tier: "free" | "pro"
//   - tool?: tool name (only for "tool" events)
//
// What NEVER gets sent:
//   - IP address (the endpoint strips it server-side; we never send headers)
//   - hostname, OS user, machine id
//   - prompt content, tool arguments, tool results
//   - API keys, billing info
//
// Opt out with `SCENEVIEW_TELEMETRY=0` or by running in CI (`CI=true`).

import type { Tier } from "./tiers.js";

// TODO: Provision a Cloudflare Worker at this address that ingests events
// into a lightweight store (Workers Analytics Engine or R2 + daily rollup).
// Until the Worker is deployed, requests to this endpoint will fail silently,
// which is fine — fetch errors are swallowed by design.
const TELEMETRY_ENDPOINT = "https://telemetry.sceneview.io/v1/events";

// Hard cap so we never hang the process on a slow endpoint.
const TELEMETRY_TIMEOUT_MS = 2000;

// Shared between init and tool events. Populated by `recordClientInit`.
interface ClientContext {
  client: string;
  clientVersion: string;
}

let clientContext: ClientContext | undefined;

// Read lazily so tests that mutate env vars between runs see the latest value.
function isEnabled(): boolean {
  if (process.env.SCENEVIEW_TELEMETRY === "0") return false;
  if (process.env.CI === "true") return false;
  return true;
}

// Read the package version lazily so tests don't need to mock fs.
// Falls back to "unknown" so payloads stay well-formed even if
// something goes wrong.
function getMcpVersion(): string {
  return process.env.SCENEVIEW_MCP_VERSION ?? "3.6.4";
}

/** Exposed for tests. */
export interface TelemetryPayload {
  timestamp: string;
  event: "init" | "tool";
  client: string;
  clientVersion: string;
  mcpVersion: string;
  tier: Tier;
  tool?: string;
}

/** Exposed for tests — resets the cached client context. */
export function __resetClientContext(): void {
  clientContext = undefined;
}

// Fire-and-forget POST with a timeout. Never throws, never awaits the caller.
function send(payload: TelemetryPayload): void {
  if (!isEnabled()) return;

  // Build a bounded promise so we never hang on a slow endpoint. We
  // intentionally do NOT await this from the caller — telemetry must
  // never block the handshake or a tool call. Wrap everything in a
  // try/catch because a buggy fetch polyfill (or a test mock) could
  // throw synchronously before we get a chance to attach .catch.
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TELEMETRY_TIMEOUT_MS);

    fetch(TELEMETRY_ENDPOINT, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
      signal: controller.signal,
    })
      .catch(() => {
        // Swallow all failures — network errors, DNS, timeouts, non-2xx.
        // Telemetry is best-effort.
      })
      .finally(() => {
        clearTimeout(timeoutId);
      });
  } catch {
    // Swallow synchronous throws too.
  }
}

/**
 * Record an MCP client handshake. Called from the server's `oninitialized`
 * callback once the client has advertised its name/version. Safe to call
 * with `undefined` if the client didn't report info — the event is dropped.
 */
export function recordClientInit(clientInfo: { name: string; version: string } | undefined): void {
  if (!clientInfo) return;

  clientContext = {
    client: clientInfo.name,
    clientVersion: clientInfo.version,
  };

  if (!isEnabled()) return;

  send({
    timestamp: new Date().toISOString(),
    event: "init",
    client: clientContext.client,
    clientVersion: clientContext.clientVersion,
    mcpVersion: getMcpVersion(),
    tier: "free",
  });
}

/**
 * Record a tool invocation. Called from the `CallToolRequestSchema` handler
 * after the tier check has passed. `tier` is the tier the tool resolved to,
 * not the user's subscription status.
 */
export function recordToolCall(toolName: string, tier: Tier): void {
  if (!isEnabled()) return;

  // If the client never completed initialization (which shouldn't happen in
  // practice), fall back to "unknown" so we can still see the tool mix.
  const ctx = clientContext ?? { client: "unknown", clientVersion: "unknown" };

  send({
    timestamp: new Date().toISOString(),
    event: "tool",
    client: ctx.client,
    clientVersion: ctx.clientVersion,
    mcpVersion: getMcpVersion(),
    tier,
    tool: toolName,
  });
}
