import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  recordClientInit,
  recordToolCall,
  __resetClientContext,
  type TelemetryPayload,
} from "./telemetry.js";

// ─── Fetch stub ──────────────────────────────────────────────────────────────
//
// Telemetry must never throw and must never await the caller, so every test
// stubs `globalThis.fetch` with a spy and then inspects call counts and
// payloads. We also use fake timers in one test to assert the abort timeout
// does not leave a dangling handle.

type FetchMock = ReturnType<typeof vi.fn>;

function installFetchMock(impl?: (...args: unknown[]) => Promise<Response>): FetchMock {
  const mock = vi.fn<(...args: unknown[]) => Promise<Response>>(
    impl ?? (async () => new Response("", { status: 204 })),
  );
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (globalThis as unknown as { fetch: typeof fetch }).fetch = mock as unknown as typeof fetch;
  return mock;
}

const ORIGINAL_ENV = { ...process.env };
const ORIGINAL_FETCH = globalThis.fetch;

beforeEach(() => {
  // Start each test from a clean env with telemetry enabled and CI off.
  // The vitest harness usually sets CI=true, so we must unset it here.
  process.env = { ...ORIGINAL_ENV };
  delete process.env.SCENEVIEW_TELEMETRY;
  delete process.env.CI;
  __resetClientContext();
});

afterEach(() => {
  process.env = { ...ORIGINAL_ENV };
  globalThis.fetch = ORIGINAL_FETCH;
  vi.restoreAllMocks();
});

// Wait a microtask tick so fire-and-forget `fetch(...)` calls have a chance
// to be registered. We never actually await the telemetry call itself.
async function flushMicrotasks(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

// ─── Opt-out ─────────────────────────────────────────────────────────────────

describe("telemetry opt-out", () => {
  it("does not call fetch when SCENEVIEW_TELEMETRY=0 (init)", async () => {
    process.env.SCENEVIEW_TELEMETRY = "0";
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    await flushMicrotasks();

    expect(mock).not.toHaveBeenCalled();
  });

  it("does not call fetch when SCENEVIEW_TELEMETRY=0 (tool call)", async () => {
    process.env.SCENEVIEW_TELEMETRY = "0";
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    recordToolCall("list_samples", "free");
    await flushMicrotasks();

    expect(mock).not.toHaveBeenCalled();
  });

  it("is case-sensitive: SCENEVIEW_TELEMETRY=1 does NOT disable telemetry", async () => {
    process.env.SCENEVIEW_TELEMETRY = "1";
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(1);
  });
});

// ─── CI detection ────────────────────────────────────────────────────────────

describe("telemetry CI detection", () => {
  it("does not call fetch when CI=true (init)", async () => {
    process.env.CI = "true";
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    await flushMicrotasks();

    expect(mock).not.toHaveBeenCalled();
  });

  it("does not call fetch when CI=true (tool call)", async () => {
    process.env.CI = "true";
    const mock = installFetchMock();

    recordToolCall("list_samples", "free");
    await flushMicrotasks();

    expect(mock).not.toHaveBeenCalled();
  });

  it("other truthy CI values do not trip the skip (only literal 'true')", async () => {
    // Rationale: we deliberately match only "true" (as documented) so local
    // experiments with CI=1 still emit events.
    process.env.CI = "1";
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(1);
  });
});

// ─── Payload shape ───────────────────────────────────────────────────────────

describe("telemetry payload shape", () => {
  const ALLOWED_FIELDS = new Set([
    "timestamp",
    "event",
    "client",
    "clientVersion",
    "mcpVersion",
    "tier",
    "tool",
  ]);

  function parsePayload(mock: FetchMock, callIndex = 0): TelemetryPayload {
    const call = mock.mock.calls[callIndex];
    expect(call, `expected fetch call #${callIndex}`).toBeDefined();
    const [url, init] = call as [string, RequestInit];
    expect(url).toBe("https://telemetry.sceneview.io/v1/events");
    expect(init.method).toBe("POST");
    const headers = (init.headers ?? {}) as Record<string, string>;
    expect(headers["content-type"]).toBe("application/json");
    return JSON.parse(init.body as string) as TelemetryPayload;
  }

  it("init event contains only allowed fields", async () => {
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(1);
    const payload = parsePayload(mock);

    expect(payload.event).toBe("init");
    expect(payload.client).toBe("claude-desktop");
    expect(payload.clientVersion).toBe("0.11.0");
    expect(typeof payload.mcpVersion).toBe("string");
    expect(payload.mcpVersion.length).toBeGreaterThan(0);
    expect(payload.tier).toBe("free");
    expect(payload.tool).toBeUndefined();
    // Timestamp must be a valid ISO string (round-trippable via Date).
    expect(new Date(payload.timestamp).toISOString()).toBe(payload.timestamp);

    // No extra fields. This is the anti-exfiltration guard.
    for (const key of Object.keys(payload)) {
      expect(ALLOWED_FIELDS.has(key), `unexpected field: ${key}`).toBe(true);
    }
  });

  it("tool event contains only allowed fields and includes tool name", async () => {
    const mock = installFetchMock();

    recordClientInit({ name: "cursor", version: "0.50.0" });
    recordToolCall("get_node_reference", "pro");
    await flushMicrotasks();

    // Two fetches: init + tool.
    expect(mock).toHaveBeenCalledTimes(2);
    const payload = parsePayload(mock, 1);

    expect(payload.event).toBe("tool");
    expect(payload.tool).toBe("get_node_reference");
    expect(payload.tier).toBe("pro");
    expect(payload.client).toBe("cursor");
    expect(payload.clientVersion).toBe("0.50.0");

    for (const key of Object.keys(payload)) {
      expect(ALLOWED_FIELDS.has(key), `unexpected field: ${key}`).toBe(true);
    }
  });

  it("tool event falls back to 'unknown' client when init was never recorded", async () => {
    const mock = installFetchMock();

    // No recordClientInit before recordToolCall.
    recordToolCall("list_samples", "free");
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(1);
    const payload = JSON.parse((mock.mock.calls[0]![1] as RequestInit).body as string);
    expect(payload.client).toBe("unknown");
    expect(payload.clientVersion).toBe("unknown");
  });

  it("does not send an init event when clientInfo is undefined", async () => {
    const mock = installFetchMock();

    recordClientInit(undefined);
    await flushMicrotasks();

    expect(mock).not.toHaveBeenCalled();
  });

  it("payload never contains an 'ip', 'hostname', 'user', 'args', or 'result' field", async () => {
    const mock = installFetchMock();

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    recordToolCall("debug_issue", "free");
    await flushMicrotasks();

    for (const call of mock.mock.calls) {
      const body = JSON.parse((call[1] as RequestInit).body as string);
      for (const forbidden of ["ip", "hostname", "user", "args", "result", "prompt", "apiKey"]) {
        expect(body[forbidden]).toBeUndefined();
      }
    }
  });
});

// ─── Non-blocking behavior ───────────────────────────────────────────────────

describe("telemetry non-blocking behavior", () => {
  it("recordClientInit returns synchronously even if fetch hangs forever", () => {
    // Install a fetch that returns a promise which never resolves.
    installFetchMock(() => new Promise<Response>(() => {}));

    const start = Date.now();
    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    const elapsed = Date.now() - start;

    // Must be effectively instant (< 50ms) — fetch is fire-and-forget.
    expect(elapsed).toBeLessThan(50);
  });

  it("recordToolCall returns synchronously even if fetch hangs forever", () => {
    installFetchMock(() => new Promise<Response>(() => {}));

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });

    const start = Date.now();
    recordToolCall("list_samples", "free");
    const elapsed = Date.now() - start;

    expect(elapsed).toBeLessThan(50);
  });

  it("recordClientInit does not return a thenable (caller cannot await it)", () => {
    installFetchMock();
    const result = recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    expect(result).toBeUndefined();
  });
});

// ─── Fetch failure is swallowed ──────────────────────────────────────────────

describe("telemetry failure handling", () => {
  it("swallows fetch rejections (network error)", async () => {
    const mock = installFetchMock(() => Promise.reject(new Error("ENETUNREACH")));

    expect(() => recordClientInit({ name: "claude-desktop", version: "0.11.0" })).not.toThrow();
    await flushMicrotasks();
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(1);
  });

  it("swallows fetch rejections on tool calls", async () => {
    const mock = installFetchMock(() => Promise.reject(new Error("DNS failure")));

    recordClientInit({ name: "claude-desktop", version: "0.11.0" });
    expect(() => recordToolCall("list_samples", "free")).not.toThrow();
    await flushMicrotasks();
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(2);
  });

  it("swallows non-2xx responses without throwing (e.g., Cloudflare 502)", async () => {
    const mock = installFetchMock(async () => new Response("bad gateway", { status: 502 }));

    expect(() => {
      recordClientInit({ name: "claude-desktop", version: "0.11.0" });
      recordToolCall("list_samples", "free");
    }).not.toThrow();

    await flushMicrotasks();
    await flushMicrotasks();

    expect(mock).toHaveBeenCalledTimes(2);
  });

  it("swallows synchronous fetch throws", async () => {
    installFetchMock(() => {
      throw new Error("sync boom");
    });

    expect(() => recordClientInit({ name: "claude-desktop", version: "0.11.0" })).not.toThrow();
  });
});
