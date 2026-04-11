/**
 * Smoke tests for the hub gateway top-level routes.
 *
 * We use Hono's in-process `app.request()` so we don't need Miniflare
 * or a live Worker. D1/KV bindings are stubbed — the MVP routes don't
 * touch them yet, so the stubs can be empty objects cast through
 * `unknown`.
 */

import { describe, it, expect } from "vitest";
import app from "../src/index.js";
import type { Env } from "../src/env.js";

// Minimal fake env: the MVP routes don't touch D1/KV, so any object
// that satisfies the type for compile purposes is enough. Cast via
// `unknown` to silence the D1/KV binding types.
const FAKE_ENV: Env = {
  ENVIRONMENT: "test",
  GATEWAY_BASE_URL: "https://hub-mcp.test",
  DB: {} as unknown as D1Database,
  RL_KV: {} as unknown as KVNamespace,
};

describe("hub-gateway smoke", () => {
  it("GET /health returns 200 with registry summary", async () => {
    const res = await app.request("/health", {}, FAKE_ENV);
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      ok: boolean;
      service: string;
      registry: { totalTools: number; libraries: unknown[] };
    };
    expect(body.ok).toBe(true);
    expect(body.service).toBe("hub-mcp-gateway");
    expect(body.registry.totalTools).toBeGreaterThan(0);
    expect(body.registry.libraries.length).toBeGreaterThan(0);
  });

  it("GET / returns the landing HTML", async () => {
    const res = await app.request("/", {}, FAKE_ENV);
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type") ?? "").toMatch(/html/);
    const body = await res.text();
    expect(body).toContain("hub-mcp");
    expect(body).toContain("architecture-mcp");
  });

  it("GET /pricing advertises Portfolio 29€ + Team 79€", async () => {
    const res = await app.request("/pricing", {}, FAKE_ENV);
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toContain("Portfolio Access");
    expect(body).toContain("€29");
    expect(body).toContain("€79");
  });

  it("GET /docs shows a quick-start snippet", async () => {
    const res = await app.request("/docs", {}, FAKE_ENV);
    expect(res.status).toBe(200);
    const body = await res.text();
    expect(body).toContain("tools/list");
    expect(body).toContain("architecture__list_building_types");
  });

  it("GET /nope returns the 404 JSON fallback", async () => {
    const res = await app.request("/nope", {}, FAKE_ENV);
    expect(res.status).toBe(404);
    const body = (await res.json()) as { error: string };
    expect(body.error).toBe("Not Found");
  });
});
