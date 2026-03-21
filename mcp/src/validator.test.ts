import { describe, it, expect } from "vitest";
import { validateCode, formatValidationReport } from "./validator.js";

// ─── Helpers ─────────────────────────────────────────────────────────────────

function ruleIds(code: string) {
  return validateCode(code).map((i) => i.rule);
}

function hasRule(code: string, rule: string) {
  return ruleIds(code).includes(rule);
}

// ─── threading/filament-off-main-thread ──────────────────────────────────────

describe("threading/filament-off-main-thread", () => {
  const RULE = "threading/filament-off-main-thread";

  it("fires when modelLoader.createModel* used near Dispatchers.IO", () => {
    const code = `
      withContext(Dispatchers.IO) {
        val model = modelLoader.createModelInstance("models/chair.glb")
      }
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("fires when Texture.Builder used near Dispatchers.Default", () => {
    const code = `
      launch(Dispatchers.Default) {
        val texture = Texture.Builder().width(4).build(engine)
      }
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire without a background dispatcher", () => {
    const code = `val model = modelLoader.createModelInstance("models/chair.glb")`;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when Dispatchers.Main is used", () => {
    const code = `
      withContext(Dispatchers.Main) {
        val model = modelLoader.createModelInstance("models/chair.glb")
      }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── ar/node-not-anchor ───────────────────────────────────────────────────────

describe("ar/node-not-anchor", () => {
  const RULE = "ar/node-not-anchor";

  it("fires when worldPosition is set inside an ARScene", () => {
    const code = `
      ARScene(engine = engine) {
        node.worldPosition = Position(0f, 0f, -1f)
      }
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire for worldPosition outside ARScene", () => {
    const code = `node.worldPosition = Position(0f, 0f, -1f)`;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when AnchorNode is used correctly", () => {
    const code = `
      ARScene(engine = engine) {
        AnchorNode(anchor = hitResult.createAnchor()) { }
      }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── composable/model-instance-null-check ────────────────────────────────────

describe("composable/model-instance-null-check", () => {
  const RULE = "composable/model-instance-null-check";

  it("fires when rememberModelInstance result used without null guard", () => {
    const code = `
      val instance = rememberModelInstance(modelLoader, "models/chair.glb")
      ModelNode(modelInstance = instance, scaleToUnits = 1f)
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when guarded with ?.", () => {
    const code = `
      val instance = rememberModelInstance(modelLoader, "models/chair.glb")
      instance?.let { ModelNode(modelInstance = it) }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when guarded with !!", () => {
    const code = `
      val instance = rememberModelInstance(modelLoader, "models/chair.glb")
      ModelNode(modelInstance = instance!!, scaleToUnits = 1f)
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when rememberModelInstance is not assigned to a var", () => {
    const code = `ModelNode(modelInstance = someOtherInstance, scaleToUnits = 1f)`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/light-node-trailing-lambda ──────────────────────────────────────────

describe("api/light-node-trailing-lambda", () => {
  const RULE = "api/light-node-trailing-lambda";

  it("fires on trailing lambda syntax", () => {
    const code = `LightNode(engine = engine, type = LightManager.Type.SUN) { intensity(100_000f) }`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when apply = { } is used correctly", () => {
    const code = `LightNode(engine = engine, type = LightManager.Type.SUN, apply = { intensity(100_000f) })`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── lifecycle/manual-engine-create ──────────────────────────────────────────

describe("lifecycle/manual-engine-create", () => {
  const RULE = "lifecycle/manual-engine-create";

  it("fires on Engine.create()", () => {
    const code = `val engine = Engine.create(eglContext)`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when rememberEngine() is used", () => {
    const code = `val engine = rememberEngine()`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── lifecycle/manual-engine-destroy ─────────────────────────────────────────

describe("lifecycle/manual-engine-destroy", () => {
  const RULE = "lifecycle/manual-engine-destroy";

  it("fires when engine.destroy() called alongside rememberEngine", () => {
    const code = `
      val engine = rememberEngine()
      DisposableEffect(Unit) { onDispose { engine.destroy() } }
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when rememberEngine is absent (imperative code)", () => {
    const code = `engine.destroy()`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── lifecycle/texture-destroy-order ─────────────────────────────────────────

describe("lifecycle/texture-destroy-order", () => {
  const RULE = "lifecycle/texture-destroy-order";

  it("fires when texture is destroyed before material instance", () => {
    const code = `
      engine.safeDestroyTexture(texture)
      materialLoader.destroyMaterialInstance(instance)
    `;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when material instance destroyed first", () => {
    const code = `
      materialLoader.destroyMaterialInstance(instance)
      engine.safeDestroyTexture(texture)
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when only one of the two is present", () => {
    expect(hasRule(`engine.safeDestroyTexture(texture)`, RULE)).toBe(false);
    expect(hasRule(`materialLoader.destroyMaterialInstance(instance)`, RULE)).toBe(false);
  });
});

// ─── composable/prefer-remember-model-instance ────────────────────────────────

describe("composable/prefer-remember-model-instance", () => {
  const RULE = "composable/prefer-remember-model-instance";

  it("fires when createModelInstance is used directly", () => {
    const code = `val m = modelLoader.createModelInstance("models/chair.glb")`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire for rememberModelInstance", () => {
    const code = `val m = rememberModelInstance(modelLoader, "models/chair.glb")`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── ar/anchor-node-missing-anchor ───────────────────────────────────────────

describe("ar/anchor-node-missing-anchor", () => {
  const RULE = "ar/anchor-node-missing-anchor";

  it("fires on empty AnchorNode()", () => {
    const code = `val node = AnchorNode()`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when anchor param is provided", () => {
    const code = `AnchorNode(anchor = hitResult.createAnchor())`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── migration/old-api ────────────────────────────────────────────────────────

describe("migration/old-api", () => {
  const RULE = "migration/old-api";

  it("fires on SceneView composable", () => {
    expect(hasRule(`SceneView(modifier = Modifier.fillMaxSize())`, RULE)).toBe(true);
  });

  it("fires on ArSceneView composable", () => {
    expect(hasRule(`ArSceneView(modifier = Modifier.fillMaxSize())`, RULE)).toBe(true);
  });

  it("fires on TransformableNode", () => {
    expect(hasRule(`val node = TransformableNode(system)`, RULE)).toBe(true);
  });

  it("fires on PlacementNode", () => {
    expect(hasRule(`val node = PlacementNode()`, RULE)).toBe(true);
  });

  it("fires on ViewRenderable", () => {
    expect(hasRule(`ViewRenderable.builder()`, RULE)).toBe(true);
  });

  it("fires on loadModelAsync", () => {
    expect(hasRule(`modelLoader.loadModelAsync("models/x.glb")`, RULE)).toBe(true);
  });

  it("does NOT fire on modern 3.0 APIs", () => {
    const code = `
      val engine = rememberEngine()
      Scene(engine = engine) {
        rememberModelInstance(modelLoader, "models/x.glb")?.let { ModelNode(modelInstance = it) }
      }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/scene-missing-engine ─────────────────────────────────────────────────

describe("api/scene-missing-engine", () => {
  const RULE = "api/scene-missing-engine";

  it("fires when Scene() has no engine nearby", () => {
    const code = `Scene(modifier = Modifier.fillMaxSize()) { }`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when engine is provided", () => {
    const code = `
      val engine = rememberEngine()
      Scene(modifier = Modifier.fillMaxSize(), engine = engine) { }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/fog-node-missing-view ────────────────────────────────────────────────

describe("api/fog-node-missing-view", () => {
  const RULE = "api/fog-node-missing-view";

  it("fires when FogNode has no view parameter", () => {
    const code = `FogNode(density = 0.1f, enabled = true)`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when view is provided", () => {
    const code = `FogNode(view = view, density = 0.1f)`;
    expect(hasRule(code, RULE)).toBe(false);
  });

  it("does NOT fire when FogNode is not used", () => {
    const code = `Scene(engine = engine) { }`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/reflection-probe-missing-camera ─────────────────────────────────────

describe("api/reflection-probe-missing-camera", () => {
  const RULE = "api/reflection-probe-missing-camera";

  it("fires when ReflectionProbeNode has no cameraPosition", () => {
    const code = `ReflectionProbeNode(filamentScene = scene, environment = env)`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when cameraPosition is provided", () => {
    const code = `ReflectionProbeNode(
      filamentScene = scene,
      environment = env,
      cameraPosition = cameraPos
    )`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/physics-node-missing-radius ─────────────────────────────────────────

describe("api/physics-node-missing-radius", () => {
  const RULE = "api/physics-node-missing-radius";

  it("fires info when PhysicsNode has no radius", () => {
    const code = `PhysicsNode(node = sphere, mass = 1f, restitution = 0.7f)`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when radius is provided", () => {
    const code = `PhysicsNode(node = sphere, mass = 1f, radius = 0.15f)`;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── api/dynamic-sky-outside-scene ───────────────────────────────────────────

describe("api/dynamic-sky-outside-scene", () => {
  const RULE = "api/dynamic-sky-outside-scene";

  it("fires when DynamicSkyNode is used without a Scene", () => {
    const code = `DynamicSkyNode(timeOfDay = 12f)`;
    expect(hasRule(code, RULE)).toBe(true);
  });

  it("does NOT fire when inside a Scene", () => {
    const code = `
      Scene(engine = engine) {
        DynamicSkyNode(timeOfDay = 12f)
      }
    `;
    expect(hasRule(code, RULE)).toBe(false);
  });
});

// ─── formatValidationReport ───────────────────────────────────────────────────

describe("formatValidationReport", () => {
  it("returns success message when no issues", () => {
    expect(formatValidationReport([])).toContain("No issues found");
  });

  it("includes severity counts in the header", () => {
    const issues = validateCode(`
      val instance = rememberModelInstance(modelLoader, "x.glb")
      ModelNode(modelInstance = instance, scaleToUnits = 1f)
      LightNode(engine = engine) { intensity(1f) }
    `);
    const report = formatValidationReport(issues);
    expect(report).toMatch(/error/);
    expect(report).toContain("🔴");
  });

  it("includes line numbers when available", () => {
    const code = `\nval instance = rememberModelInstance(modelLoader, "x.glb")\nModelNode(modelInstance = instance, scaleToUnits = 1f)`;
    const report = formatValidationReport(validateCode(code));
    expect(report).toMatch(/line \d+/);
  });
});
