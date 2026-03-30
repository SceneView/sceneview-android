import { describe, it, expect } from "vitest";
import { explainAPI, listExplainableAPIs, formatAPIExplanation } from "./explain-api.js";

describe("explainAPI", () => {
  it("explains rememberEngine case-insensitively", () => {
    const result = explainAPI("rememberEngine");
    expect(result).not.toBeNull();
    expect(result!.name).toBe("rememberEngine()");
  });

  it("explains ModelNode", () => {
    const result = explainAPI("ModelNode");
    expect(result).not.toBeNull();
    expect(result!.name).toBe("ModelNode()");
    expect(result!.commonMistakes.length).toBeGreaterThan(0);
  });

  it("explains LightNode with trailing-lambda warning", () => {
    const result = explainAPI("LightNode");
    expect(result).not.toBeNull();
    expect(result!.commonMistakes.some(m => m.includes("trailing lambda") || m.includes("SILENTLY IGNORED"))).toBe(true);
  });

  it("explains ARScene", () => {
    const result = explainAPI("ARScene");
    expect(result).not.toBeNull();
    expect(result!.platform).toBe("Android");
  });

  it("returns null for unknown API", () => {
    const result = explainAPI("nonExistentAPI");
    expect(result).toBeNull();
  });

  it("listExplainableAPIs returns all known APIs", () => {
    const apis = listExplainableAPIs();
    expect(apis.length).toBeGreaterThanOrEqual(7);
    expect(apis).toContain("rememberEngine()");
    expect(apis).toContain("ModelNode()");
  });

  it("formatAPIExplanation produces markdown", () => {
    const result = explainAPI("Scene")!;
    const text = formatAPIExplanation(result);
    expect(text).toContain("## SceneView()");
    expect(text).toContain("### Signature");
    expect(text).toContain("### Example");
    expect(text).toContain("### Common Mistakes");
  });
});
