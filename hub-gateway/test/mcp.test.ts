/**
 * JSON-RPC MCP transport smoke tests.
 *
 * Exercises the three methods the MVP handles: initialize, tools/list,
 * tools/call. Dispatch goes against the architecture-mcp stub library,
 * which returns a "not yet wired" text payload — the test asserts that
 * the envelope shape is correct, not the upstream behavior.
 */

import { describe, it, expect, beforeEach } from "vitest";
import app from "../src/index.js";
import { JSON_RPC_ERRORS } from "../src/mcp/transport.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { makeEnv } from "./helpers/fake-bindings.js";

// Every /mcp request now requires an API key. We seed a valid key
// for the whole suite and send it via the Authorization header on
// each call.
const VALID_KEY = "sv_live_TESTKEY000000000000000000000000";
const VALID_HEADER = `Bearer ${VALID_KEY}`;

let FAKE_ENV: ReturnType<typeof makeEnv>["env"];

beforeEach(async () => {
  const hash = await hashApiKey(VALID_KEY);
  const ctx = makeEnv({
    api_keys: [
      {
        id: "key_test0000",
        user_id: "usr_test0000",
        key_hash: hash,
        key_prefix: "sv_live_TESTKE",
        revoked_at: null,
      },
    ],
    users: [{ id: "usr_test0000", email: "test@hub.local", tier: "pro" }],
  });
  FAKE_ENV = ctx.env;
});

function rpc(body: unknown): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: VALID_HEADER,
    },
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
    expect(body.result.tools.length).toBeGreaterThanOrEqual(33);
    const names = body.result.tools.map((t) => t.name);
    // One canary from each of the 11 stubbed libraries — if the
    // registry wiring regresses, exactly one assertion below fails
    // so the offending package is easy to spot.
    expect(names).toContain("architecture__list_building_types");
    expect(names).toContain("realestate__search_listings");
    expect(names).toContain("french_admin__calculate_impots");
    expect(names).toContain("ecommerce3d__search_products");
    expect(names).toContain("legal_docs__list_templates");
    expect(names).toContain("finance__market_quote");
    expect(names).toContain("education__generate_lesson_plan");
    expect(names).toContain("social_media__suggest_hashtags");
    expect(names).toContain("health_fitness__workout_plan");
    // Real vendored libraries — upstream tool names (no prefix).
    expect(names).toContain("get_car_configurator");
    expect(names).toContain("get_anatomy_viewer");
    // Every hub tool must be a valid MCP tool name (lowercase,
    // underscores, digits). Stubbed libraries use the
    // `{package}__tool` prefix scheme; the real vendored libraries
    // (automotive-3d, healthcare-3d) keep their upstream names for
    // parity with their stdio counterparts, which are NOT prefixed.
    for (const n of names) expect(n).toMatch(/^[a-z0-9_]+$/);
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
    // Only covers the still-stubbed libraries. automotive-3d and
    // healthcare-3d were graduated to real upstream handlers in a
    // later commit — they have their own tests in the vendored
    // packages.
    const targets: Array<{ tool: string; marker: string }> = [
      { tool: "realestate__search_listings", marker: "realestate-mcp pilot stub" },
      { tool: "french_admin__calculate_impots", marker: "french-admin-mcp pilot stub" },
      { tool: "ecommerce3d__search_products", marker: "ecommerce-3d-mcp pilot stub" },
      { tool: "legal_docs__list_templates", marker: "legal-docs-mcp pilot stub" },
      { tool: "finance__market_quote", marker: "finance-mcp pilot stub" },
      { tool: "education__generate_lesson_plan", marker: "education-mcp pilot stub" },
      { tool: "social_media__suggest_hashtags", marker: "social-media-mcp pilot stub" },
      { tool: "health_fitness__workout_plan", marker: "health-fitness-mcp pilot stub" },
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

  it("dispatches a real vendored tool (automotive list_car_models)", async () => {
    const res = await app.request(
      rpc({
        jsonrpc: "2.0",
        id: 11,
        method: "tools/call",
        params: { name: "list_car_models", arguments: {} },
      }),
      {},
      FAKE_ENV,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      result: { content: Array<{ text: string }>; isError?: boolean };
    };
    // The upstream handler returns a real catalogue listing, NOT
    // a pilot stub marker. A drift test: if anyone replaces the
    // thin re-export with a fresh stub, this assertion breaks.
    expect(body.result.isError).toBeFalsy();
    expect(body.result.content[0].text).not.toContain("pilot stub");
  });

  it("rejects non-JSON bodies with PARSE_ERROR", async () => {
    const req = new Request("https://hub-mcp.test/mcp", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        authorization: VALID_HEADER,
      },
      body: "not json",
    });
    const res = await app.request(req, {}, FAKE_ENV);
    expect(res.status).toBe(400);
    const body = (await res.json()) as { error: { code: number } };
    expect(body.error.code).toBe(JSON_RPC_ERRORS.PARSE_ERROR);
  });
});
