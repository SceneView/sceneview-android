/**
 * JSON-RPC MCP transport smoke tests.
 *
 * Exercises the three methods the MVP handles: initialize, tools/list,
 * tools/call. Dispatch goes against the architecture-mcp stub library,
 * which returns a "not yet wired" text payload — the test asserts that
 * the envelope shape is correct, not the upstream behavior.
 */

import { describe, it, expect } from "vitest";
import app from "../src/index.js";
import type { Env } from "../src/env.js";
import { JSON_RPC_ERRORS } from "../src/mcp/transport.js";

const FAKE_ENV: Env = {
  ENVIRONMENT: "test",
  GATEWAY_BASE_URL: "https://hub-mcp.test",
  DB: {} as unknown as D1Database,
  RL_KV: {} as unknown as KVNamespace,
};

function rpc(body: unknown): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

describe("hub-gateway /mcp", () => {
  it("handles initialize", async () => {
    const res = await app.request(
      rpc({ jsonrpc: "2.0", id: 1, method: "initialize" }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      id: number;
      result: { serverInfo: { name: string }; capabilities: unknown };
    };
    expect(body.id).toBe(1);
    expect(body.result.serverInfo.name).toBe("hub-mcp-gateway");
    expect(body.result.capabilities).toBeDefined();
  });

  it("handles tools/list across every stubbed library", async () => {
    const res = await app.request(
      rpc({ jsonrpc: "2.0", id: 2, method: "tools/list" }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      id: number;
      result: { tools: Array<{ name: string }> };
    };
    expect(body.id).toBe(2);
    expect(body.result.tools.length).toBeGreaterThanOrEqual(14);
    const names = body.result.tools.map((t) => t.name);
    // One tool from each stubbed library — canary that the registry
    // wiring is complete.
    expect(names).toContain("architecture__list_building_types");
    expect(names).toContain("realestate__search_listings");
    expect(names).toContain("french_admin__calculate_impots");
    expect(names).toContain("ecommerce3d__search_products");
    expect(names).toContain("finance__market_quote");
    // Every hub tool must be package-prefixed. Single-underscore
    // package ids (like `french_admin`) are allowed, the delimiter
    // between package and tool is always `__`.
    for (const n of names) expect(n).toMatch(/^[a-z0-9_]+__[a-z0-9_]+$/);
  });

  it("dispatches tools/call to the architecture stub library", async () => {
    const res = await app.request(
      rpc({
        jsonrpc: "2.0",
        id: 3,
        method: "tools/call",
        params: { name: "architecture__list_building_types", arguments: {} },
      }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      id: number;
      result: { content: Array<{ type: string; text: string }>; isError?: boolean };
    };
    expect(body.id).toBe(3);
    expect(body.result.isError).toBeFalsy();
    expect(body.result.content[0].type).toBe("text");
    expect(body.result.content[0].text).toContain("architecture-mcp pilot stub");
  });

  it("returns METHOD_NOT_FOUND for an unknown JSON-RPC method", async () => {
    const res = await app.request(
      rpc({ jsonrpc: "2.0", id: 4, method: "nope/unknown" }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      error: { code: number; message: string };
    };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.METHOD_NOT_FOUND);
    expect(body.error.message).toMatch(/nope\/unknown/);
  });

  it("returns an error result for an unknown tool", async () => {
    const res = await app.request(
      rpc({
        jsonrpc: "2.0",
        id: 5,
        method: "tools/call",
        params: { name: "architecture__does_not_exist", arguments: {} },
      }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      result: { content: Array<{ text: string }>; isError?: boolean };
    };
    expect(body.result.isError).toBe(true);
    expect(body.result.content[0].text).toContain("Unknown tool");
  });

  it("dispatches each stubbed library through its own dispatcher", async () => {
    const targets: Array<{ tool: string; marker: string }> = [
      { tool: "realestate__search_listings", marker: "realestate-mcp pilot stub" },
      { tool: "french_admin__calculate_impots", marker: "french-admin-mcp pilot stub" },
      { tool: "ecommerce3d__search_products", marker: "ecommerce-3d-mcp pilot stub" },
      { tool: "finance__market_quote", marker: "finance-mcp pilot stub" },
    ];
    for (const { tool, marker } of targets) {
      const res = await app.request(
        rpc({
          jsonrpc: "2.0",
          id: 10,
          method: "tools/call",
          params: { name: tool, arguments: {} },
        }),
        {},
        FAKE_ENV,
      );
      expect(res.status, `status for ${tool}`).toBe(200);
      const body = (await res.json()) as {
        result: { content: Array<{ text: string }>; isError?: boolean };
      };
      expect(body.result.isError, `isError for ${tool}`).toBeFalsy();
      expect(body.result.content[0].text).toContain(marker);
    }
  });

  it("rejects non-JSON bodies with PARSE_ERROR", async () => {
    const req = new Request("https://hub-mcp.test/mcp", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "not json",
    });
    const res = await app.request(req, {}, FAKE_ENV);
    expect(res.status).toBe(400);
    const body = (await res.json()) as { error: { code: number } };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.PARSE_ERROR);
  });
});
