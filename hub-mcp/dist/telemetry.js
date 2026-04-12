// ─── Anonymous opt-out telemetry for hub-mcp ────────────────────────────────
//
// Fire-and-forget, non-blocking, no personal data. Ever.
//
// What gets sent:
//   - timestamp (UTC ISO string)
//   - event: "init" | "tool"
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
// Opt out with `HUB_TELEMETRY=0` or by running in CI (`CI=true`).
const TELEMETRY_ENDPOINT = "https://sceneview-telemetry.mcp-tools-lab.workers.dev/v1/events";
const TELEMETRY_BATCH_ENDPOINT = "https://sceneview-telemetry.mcp-tools-lab.workers.dev/v1/batch";
// Hard cap so we never hang the process on a slow endpoint.
const TELEMETRY_TIMEOUT_MS = 2000;
// Client-side batching: flush when buffer reaches this size or after this delay.
const BATCH_MAX_SIZE = 10;
const BATCH_FLUSH_INTERVAL_MS = 30_000;
// ─── Batch buffer ─────────────────────────────────────────────────────────────
let buffer = [];
let flushTimer;
// Read lazily so tests that mutate env vars between runs see the latest value.
function isEnabled() {
    if (process.env.HUB_TELEMETRY === "0")
        return false;
    if (process.env.CI === "true")
        return false;
    return true;
}
function getMcpVersion() {
    return process.env.HUB_MCP_VERSION ?? "0.1.0";
}
// Fire-and-forget POST of a single payload.
function sendSingle(payload) {
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
            // Swallow all failures — telemetry is best-effort.
        })
            .finally(() => {
            clearTimeout(timeoutId);
        });
    }
    catch {
        // Swallow synchronous throws too.
    }
}
// Fire-and-forget POST of a batch. Falls back to individual sends if the batch
// request fails.
function sendBatch(events) {
    if (events.length === 0)
        return;
    if (events.length === 1) {
        sendSingle(events[0]);
        return;
    }
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TELEMETRY_TIMEOUT_MS);
        fetch(TELEMETRY_BATCH_ENDPOINT, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(events),
            signal: controller.signal,
        })
            .catch(() => {
            clearTimeout(timeoutId);
            for (const payload of events) {
                sendSingle(payload);
            }
        })
            .finally(() => {
            clearTimeout(timeoutId);
        });
    }
    catch {
        for (const payload of events) {
            sendSingle(payload);
        }
    }
}
// Schedule the auto-flush timer (idempotent — only one timer at a time).
function scheduleFlushTimer() {
    if (flushTimer !== undefined)
        return;
    flushTimer = setTimeout(() => {
        flushTimer = undefined;
        flushTelemetry();
    }, BATCH_FLUSH_INTERVAL_MS);
    // Allow Node.js to exit even if the timer is still pending.
    if (typeof flushTimer === "object" && flushTimer !== null && "unref" in flushTimer) {
        flushTimer.unref();
    }
}
/**
 * Flush all buffered telemetry events immediately. Safe to call at any time.
 * Useful for graceful shutdown. Fire-and-forget — never throws.
 */
export function flushTelemetry() {
    if (flushTimer !== undefined) {
        clearTimeout(flushTimer);
        flushTimer = undefined;
    }
    if (buffer.length === 0)
        return;
    const snapshot = buffer.splice(0);
    sendBatch(snapshot);
}
// Push a payload into the buffer and flush when the batch is full.
function send(payload) {
    if (!isEnabled())
        return;
    buffer.push(payload);
    scheduleFlushTimer();
    if (buffer.length >= BATCH_MAX_SIZE) {
        flushTelemetry();
    }
}
/**
 * Record server initialization. Call once after the MCP server starts.
 */
export function recordInit() {
    send({
        timestamp: new Date().toISOString(),
        event: "init",
        mcpVersion: getMcpVersion(),
        tier: "free",
    });
}
/**
 * Record a tool invocation. Call from the `CallToolRequestSchema` handler
 * after the tier check. `tier` reflects whether the call was a free-local or
 * pro-proxied dispatch.
 */
export function recordToolCall(toolName, tier) {
    send({
        timestamp: new Date().toISOString(),
        event: "tool",
        mcpVersion: getMcpVersion(),
        tier,
        tool: toolName,
    });
}
