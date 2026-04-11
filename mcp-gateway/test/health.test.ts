import { describe, it, expect } from "vitest";
import app from "../src/index.js";

describe("GET /health", () => {
  it("returns 200 with JSON body", async () => {
    const res = await app.request("/health");
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toContain("application/json");

    const body = (await res.json()) as {
      ok: boolean;
      service: string;
      version: string;
    };
    expect(body.ok).toBe(true);
    expect(body.service).toBe("sceneview-mcp-gateway");
    expect(typeof body.version).toBe("string");
  });
});

describe("GET /", () => {
  it("returns landing text", async () => {
    const res = await app.request("/");
    expect(res.status).toBe(200);
    const text = await res.text();
    expect(text).toBe("SceneView MCP Gateway");
  });
});

describe("GET /unknown", () => {
  it("returns 404 JSON", async () => {
    const res = await app.request("/does-not-exist");
    expect(res.status).toBe(404);
    const body = (await res.json()) as { error: string };
    expect(body.error).toBe("Not Found");
  });
});
