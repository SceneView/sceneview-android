import { describe, it, expect } from "vitest";
import { migrateCode, formatMigrationResult } from "./migrate-code.js";

describe("migrateCode", () => {
  it("renames SceneView() to Scene()", () => {
    const result = migrateCode(`SceneView(modifier = Modifier.fillMaxSize())`);
    expect(result.migratedCode).toContain("Scene(");
    expect(result.migratedCode).not.toContain("SceneView(");
    expect(result.changes.length).toBeGreaterThan(0);
    expect(result.changes[0].rule).toBe("rename-sceneview-to-scene");
  });

  it("renames ArSceneView() to ARScene()", () => {
    const result = migrateCode(`ArSceneView(modifier = Modifier.fillMaxSize())`);
    expect(result.migratedCode).toContain("ARScene(");
    expect(result.changes.some((c) => c.rule === "rename-arsceneview-to-arscene")).toBe(true);
  });

  it("replaces loadModelAsync with rememberModelInstance", () => {
    const result = migrateCode(`modelLoader.loadModelAsync("models/chair.glb")`);
    expect(result.migratedCode).toContain("rememberModelInstance");
    expect(result.migratedCode).toContain("models/chair.glb");
  });

  it("replaces Engine.create() with rememberEngine()", () => {
    const result = migrateCode(`val engine = Engine.create()`);
    expect(result.migratedCode).toContain("rememberEngine()");
    expect(result.migratedCode).not.toContain("Engine.create()");
  });

  it("removes manual engine.destroy()", () => {
    const result = migrateCode(`engine.destroy()`);
    expect(result.migratedCode).toContain("// engine.destroy() removed");
  });

  it("fixes LightNode trailing lambda to apply = {", () => {
    const result = migrateCode(`LightNode(engine = engine, type = LightManager.Type.DIRECTIONAL) {`);
    expect(result.migratedCode).toContain("apply = {");
  });

  it("replaces Sceneform imports", () => {
    const result = migrateCode(`import com.google.ar.sceneform.Node`);
    expect(result.migratedCode).toContain("import io.github.sceneview.Node");
    expect(result.migratedCode).not.toContain("sceneform");
  });

  it("replaces loadEnvironment with createHDREnvironment", () => {
    const result = migrateCode(`environmentLoader.loadEnvironment("environments/sky.hdr")`);
    expect(result.migratedCode).toContain("createHDREnvironment");
  });

  it("marks TransformableNode as removed", () => {
    const result = migrateCode(`val node = TransformableNode(system)`);
    expect(result.migratedCode).toContain("TransformableNode removed");
    expect(result.migratedCode).toContain("isEditable = true");
  });

  it("marks PlacementNode as removed", () => {
    const result = migrateCode(`val node = PlacementNode()`);
    expect(result.migratedCode).toContain("PlacementNode removed");
    expect(result.migratedCode).toContain("AnchorNode");
  });

  it("marks ViewRenderable as removed", () => {
    const result = migrateCode(`ViewRenderable.builder()`);
    expect(result.migratedCode).toContain("ViewRenderable removed");
    expect(result.migratedCode).toContain("ViewNode");
  });

  it("returns no changes for 3.x code", () => {
    const code = `
val engine = rememberEngine()
Scene(engine = engine) {
    rememberModelInstance(modelLoader, "models/chair.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
    }
}`;
    const result = migrateCode(code);
    expect(result.changes).toHaveLength(0);
  });

  it("adds warning for ModelRenderable.builder", () => {
    const result = migrateCode(`ModelRenderable.builder().setSource(context, uri)`);
    expect(result.warnings.length).toBeGreaterThan(0);
    expect(result.warnings[0]).toContain("ModelRenderable.builder");
  });

  it("applies multiple rules in one pass", () => {
    const code = `
import com.google.ar.sceneform.Node
val engine = Engine.create()
SceneView(modifier = Modifier.fillMaxSize())
modelLoader.loadModelAsync("models/chair.glb")
`;
    const result = migrateCode(code);
    expect(result.changes.length).toBeGreaterThanOrEqual(4);
    expect(result.migratedCode).toContain("io.github.sceneview");
    expect(result.migratedCode).toContain("rememberEngine");
    expect(result.migratedCode).toContain("Scene(");
    expect(result.migratedCode).toContain("rememberModelInstance");
  });
});

describe("formatMigrationResult", () => {
  it("reports no changes for clean code", () => {
    const result = migrateCode(`Scene(engine = engine) { }`);
    const formatted = formatMigrationResult(result);
    expect(formatted).toContain("No 2.x patterns detected");
  });

  it("includes migrated code block", () => {
    const result = migrateCode(`SceneView(modifier = Modifier.fillMaxSize())`);
    const formatted = formatMigrationResult(result);
    expect(formatted).toContain("```kotlin");
    expect(formatted).toContain("Scene(");
    expect(formatted).toContain("Changes Applied");
  });

  it("includes migration checklist", () => {
    const result = migrateCode(`SceneView(modifier = Modifier.fillMaxSize())`);
    const formatted = formatMigrationResult(result);
    expect(formatted).toContain("Migration Checklist");
    expect(formatted).toContain("rememberEngine");
  });
});
