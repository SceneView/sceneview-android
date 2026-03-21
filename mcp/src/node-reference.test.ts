import { describe, it, expect } from "vitest";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { parseNodeSections, findNodeSection, listNodeTypes } from "./node-reference.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

// Load the real llms.txt (copied alongside the compiled output or at repo root during dev)
let LLMS_TXT: string;
try {
  // Running from dist/ (after build): llms.txt is one level up
  LLMS_TXT = readFileSync(resolve(__dirname, "../llms.txt"), "utf-8");
} catch {
  // Running via tsx from src/ in dev: llms.txt is two levels up (repo root)
  LLMS_TXT = readFileSync(resolve(__dirname, "../../llms.txt"), "utf-8");
}

const SECTIONS = parseNodeSections(LLMS_TXT);

// ─── parseNodeSections ────────────────────────────────────────────────────────

describe("parseNodeSections", () => {
  it("returns a non-empty map", () => {
    expect(SECTIONS.size).toBeGreaterThan(0);
  });

  it("contains ModelNode", () => {
    expect(SECTIONS.has("modelnode")).toBe(true);
  });

  it("contains LightNode", () => {
    expect(SECTIONS.has("lightnode")).toBe(true);
  });

  it("contains Scene", () => {
    expect(SECTIONS.has("scene")).toBe(true);
  });

  it("contains ARScene", () => {
    expect(SECTIONS.has("arscene")).toBe(true);
  });

  it("contains AnchorNode", () => {
    expect(SECTIONS.has("anchornode")).toBe(true);
  });

  it("each section has a non-empty content string", () => {
    for (const [, section] of SECTIONS) {
      expect(section.content.length).toBeGreaterThan(0);
    }
  });

  it("section content starts with ### heading", () => {
    for (const [, section] of SECTIONS) {
      expect(section.content).toMatch(/^###\s/);
    }
  });

  it("ModelNode content includes scaleToUnits parameter", () => {
    const section = SECTIONS.get("modelnode")!;
    expect(section.content).toContain("scaleToUnits");
  });

  it("LightNode content mentions apply named parameter", () => {
    const section = SECTIONS.get("lightnode")!;
    expect(section.content).toContain("apply");
  });
});

// ─── findNodeSection ──────────────────────────────────────────────────────────

describe("findNodeSection", () => {
  it("finds ModelNode case-insensitively", () => {
    expect(findNodeSection(SECTIONS, "ModelNode")).toBeDefined();
    expect(findNodeSection(SECTIONS, "modelnode")).toBeDefined();
    expect(findNodeSection(SECTIONS, "MODELNODE")).toBeDefined();
  });

  it("finds ARScene case-insensitively", () => {
    expect(findNodeSection(SECTIONS, "ARScene")).toBeDefined();
    expect(findNodeSection(SECTIONS, "arscene")).toBeDefined();
  });

  it("returns undefined for an unknown type", () => {
    expect(findNodeSection(SECTIONS, "NonExistentNode")).toBeUndefined();
  });

  it("returned section has the correct canonical name", () => {
    const s = findNodeSection(SECTIONS, "lightnode");
    expect(s!.name).toBe("LightNode");
  });

  it("returned section content is non-empty markdown", () => {
    const s = findNodeSection(SECTIONS, "ModelNode");
    expect(s!.content.length).toBeGreaterThan(10);
  });
});

// ─── listNodeTypes ────────────────────────────────────────────────────────────

describe("listNodeTypes", () => {
  it("returns a sorted array", () => {
    const types = listNodeTypes(SECTIONS);
    const sorted = [...types].sort();
    expect(types).toEqual(sorted);
  });

  it("includes key node types", () => {
    const types = listNodeTypes(SECTIONS);
    expect(types).toContain("ModelNode");
    expect(types).toContain("LightNode");
    expect(types).toContain("Scene");
    expect(types).toContain("ARScene");
    expect(types).toContain("AnchorNode");
  });

  it("includes 3.2.0 node types", () => {
    const types = listNodeTypes(SECTIONS);
    expect(types).toContain("PhysicsNode");
    expect(types).toContain("DynamicSkyNode");
    expect(types).toContain("FogNode");
    expect(types).toContain("ReflectionProbeNode");
    expect(types).toContain("TextNode");
    expect(types).toContain("LineNode");
    expect(types).toContain("PathNode");
    expect(types).toContain("BillboardNode");
  });

  it("includes geometry node types", () => {
    const types = listNodeTypes(SECTIONS);
    expect(types).toContain("CubeNode");
    expect(types).toContain("SphereNode");
    expect(types).toContain("CylinderNode");
    expect(types).toContain("PlaneNode");
    expect(types).toContain("MeshNode");
  });

  it("returns at least 10 entries (enough node types in llms.txt)", () => {
    expect(listNodeTypes(SECTIONS).length).toBeGreaterThanOrEqual(10);
  });
});
