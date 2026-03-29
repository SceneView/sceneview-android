import { describe, it, expect } from "vitest";
import { optimizeScene, formatOptimizationReport } from "./optimize-scene.js";

describe("optimizeScene", () => {
  it("detects multiple engine instances", () => {
    const code = `
val engine1 = rememberEngine()
val engine2 = rememberEngine()
Scene(engine = engine1) { }
Scene(engine = engine2) { }
`;
    const report = optimizeScene(code);
    expect(report.suggestions.some(s => s.category === "Memory" && s.issue.includes("Engine"))).toBe(true);
  });

  it("detects large HDR files", () => {
    const report = optimizeScene('environmentLoader.createHDREnvironment("environments/sky_4k.hdr")');
    expect(report.suggestions.some(s => s.issue.includes("4K"))).toBe(true);
  });

  it("detects missing post-processing flag", () => {
    const report = optimizeScene('Scene(engine = engine) { ModelNode(...) }');
    expect(report.suggestions.some(s => s.issue.includes("Post-processing"))).toBe(true);
  });

  it("detects threading issues", () => {
    const code = 'withContext(Dispatchers.IO) { modelLoader.createModelInstance("a.glb") }';
    const report = optimizeScene(code);
    expect(report.suggestions.some(s => s.category === "Correctness")).toBe(true);
  });

  it("gives perfect score for clean code", () => {
    const code = "// nothing problematic here";
    const report = optimizeScene(code);
    expect(report.score).toBe(100);
    expect(report.suggestions.length).toBe(0);
  });

  it("formatOptimizationReport produces markdown", () => {
    const report = optimizeScene('rememberEngine()\nrememberEngine()\nScene(engine = engine) { }');
    const text = formatOptimizationReport(report);
    expect(text).toContain("## Scene Optimization Report");
    expect(text).toContain("**Score:**");
  });
});
