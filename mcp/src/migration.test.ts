import { describe, it, expect } from "vitest";
import { MIGRATION_GUIDE } from "./migration.js";

describe("MIGRATION_GUIDE", () => {
  it("is a non-empty string", () => {
    expect(typeof MIGRATION_GUIDE).toBe("string");
    expect(MIGRATION_GUIDE.length).toBeGreaterThan(500);
  });

  it("covers composable renames (SceneView → Scene, ArSceneView → ARScene)", () => {
    expect(MIGRATION_GUIDE).toContain("SceneView");
    expect(MIGRATION_GUIDE).toContain("Scene");
    expect(MIGRATION_GUIDE).toContain("ArSceneView");
    expect(MIGRATION_GUIDE).toContain("ARScene");
  });

  it("covers model loading migration (loadModelAsync → rememberModelInstance)", () => {
    expect(MIGRATION_GUIDE).toContain("loadModelAsync");
    expect(MIGRATION_GUIDE).toContain("rememberModelInstance");
  });

  it("covers removed nodes", () => {
    expect(MIGRATION_GUIDE).toContain("TransformableNode");
    expect(MIGRATION_GUIDE).toContain("PlacementNode");
    expect(MIGRATION_GUIDE).toContain("ViewRenderable");
  });

  it("covers LightNode named apply parameter gotcha", () => {
    expect(MIGRATION_GUIDE).toContain("apply");
    expect(MIGRATION_GUIDE).toContain("LightNode");
    expect(MIGRATION_GUIDE).toContain("trailing lambda");
  });

  it("covers engine lifecycle (rememberEngine)", () => {
    expect(MIGRATION_GUIDE).toContain("rememberEngine");
    expect(MIGRATION_GUIDE).toContain("engine.destroy");
  });

  it("covers AR anchor drift (worldPosition → AnchorNode)", () => {
    expect(MIGRATION_GUIDE).toContain("worldPosition");
    expect(MIGRATION_GUIDE).toContain("AnchorNode");
    expect(MIGRATION_GUIDE).toContain("drift");
  });

  it("covers gradle dependency changes", () => {
    expect(MIGRATION_GUIDE).toContain("io.github.sceneview:sceneview:3.3.0");
    expect(MIGRATION_GUIDE).toContain("io.github.sceneview:arsceneview:3.3.0");
  });

  it("includes a migration checklist", () => {
    expect(MIGRATION_GUIDE).toContain("Checklist");
    expect(MIGRATION_GUIDE).toContain("- [ ]");
  });

  it("includes before/after code examples", () => {
    expect(MIGRATION_GUIDE).toContain("Before");
    expect(MIGRATION_GUIDE).toContain("After");
    expect(MIGRATION_GUIDE).toContain("```kotlin");
  });
});
