/**
 * convert-platform.ts
 *
 * Convert SceneView code between Android (Kotlin/Compose) and iOS (Swift/SwiftUI).
 * Also generates multiplatform code from a scene description.
 */

export interface ConversionResult {
  code: string;
  sourceplatform: "android" | "ios";
  targetPlatform: "android" | "ios";
  changes: string[];
  warnings: string[];
}

export interface MultiplatformResult {
  androidCode: string;
  iosCode: string;
  description: string;
  notes: string[];
}

// ── Android → iOS conversion rules ──────────────────────────────────────────

interface ConversionRule {
  pattern: RegExp;
  androidToIos: string | ((match: string, ...groups: string[]) => string);
  iosToAndroid: string | ((match: string, ...groups: string[]) => string);
  description: string;
}

const CONVERSION_RULES: ConversionRule[] = [
  {
    pattern: /\bScene\s*\(/g,
    androidToIos: "SceneView {",
    iosToAndroid: "Scene(",
    description: "Scene composable → SceneView SwiftUI view",
  },
  {
    pattern: /\bARScene\s*\(/g,
    androidToIos: "ARSceneView(",
    iosToAndroid: "ARScene(",
    description: "ARScene composable → ARSceneView SwiftUI view",
  },
  {
    pattern: /rememberModelInstance\s*\(\s*modelLoader\s*,\s*"([^"]+)\.glb"\s*\)/g,
    androidToIos: 'try await ModelNode.load("$1.usdz")',
    iosToAndroid: 'rememberModelInstance(modelLoader, "$1.glb")',
    description: "Model loading: rememberModelInstance → ModelNode.load, GLB → USDZ",
  },
  {
    pattern: /rememberEngine\(\)/g,
    androidToIos: "// Engine managed by RealityKit automatically",
    iosToAndroid: "rememberEngine()",
    description: "Engine: explicit in Android, implicit in iOS/RealityKit",
  },
  {
    pattern: /rememberModelLoader\(engine\)/g,
    androidToIos: "// ModelLoader not needed — RealityKit loads models directly",
    iosToAndroid: "rememberModelLoader(engine)",
    description: "ModelLoader: Android-specific, not needed on iOS",
  },
  {
    pattern: /rememberEnvironmentLoader\(engine\)/g,
    androidToIos: "// Environment managed by RealityKit automatically",
    iosToAndroid: "rememberEnvironmentLoader(engine)",
    description: "EnvironmentLoader: Android-specific",
  },
  {
    pattern: /Modifier\.fillMaxSize\(\)/g,
    androidToIos: ".edgesIgnoringSafeArea(.all)",
    iosToAndroid: "Modifier.fillMaxSize()",
    description: "Full-screen modifier",
  },
  {
    pattern: /Position\(([^)]+)\)/g,
    androidToIos: "SIMD3<Float>($1)",
    iosToAndroid: "Position($1)",
    description: "Position type: Position → SIMD3<Float>",
  },
  {
    pattern: /\.glb\b/g,
    androidToIos: ".usdz",
    iosToAndroid: ".glb",
    description: "Model format: GLB (Android) ↔ USDZ (iOS)",
  },
];

export function convertAndroidToIos(code: string): ConversionResult {
  let result = code;
  const changes: string[] = [];
  const warnings: string[] = [];

  for (const rule of CONVERSION_RULES) {
    if (rule.pattern.test(result)) {
      const replacement = typeof rule.androidToIos === "string" ? rule.androidToIos : rule.androidToIos;
      result = result.replace(new RegExp(rule.pattern.source, rule.pattern.flags), replacement as string);
      changes.push(rule.description);
    }
  }

  // Add Swift-specific warnings
  warnings.push("RealityKit uses USDZ models, not GLB/glTF. Convert models using Apple's Reality Converter or Blender.");
  warnings.push("RealityKit handles engine, material, and environment lifecycle automatically — no manual management needed.");
  warnings.push("Swift async/await: model loading must use `try await` inside `.task { }` block.");
  if (code.includes("LightNode")) {
    warnings.push("LightNode API differs: RealityKit uses DirectionalLightComponent, PointLightComponent, SpotLightComponent on Entity.");
  }
  if (code.includes("materialLoader")) {
    warnings.push("Material API differs: RealityKit uses SimpleMaterial, PhysicallyBasedMaterial, or UnlitMaterial.");
  }

  return { code: result, sourceplatform: "android", targetPlatform: "ios", changes, warnings };
}

export function convertIosToAndroid(code: string): ConversionResult {
  let result = code;
  const changes: string[] = [];
  const warnings: string[] = [];

  // iOS → Android specific replacements
  const iosPatterns: Array<{ pattern: RegExp; replacement: string; description: string }> = [
    { pattern: /SceneView\s*\{/g, replacement: "Scene(engine = engine) {", description: "SceneView → Scene with engine" },
    { pattern: /ARSceneView\s*\(/g, replacement: "ARScene(engine = engine, ", description: "ARSceneView → ARScene with engine" },
    { pattern: /try\s+await\s+ModelNode\.load\s*\(\s*"([^"]+)\.usdz"\s*\)/g, replacement: 'rememberModelInstance(modelLoader, "$1.glb")', description: "ModelNode.load → rememberModelInstance, USDZ → GLB" },
    { pattern: /import\s+SwiftUI/g, replacement: "// SwiftUI → Jetpack Compose", description: "Import replacement" },
    { pattern: /import\s+SceneViewSwift/g, replacement: "import io.github.sceneview.*", description: "Import replacement" },
    { pattern: /import\s+RealityKit/g, replacement: "// RealityKit → Filament (included in SceneView)", description: "Import replacement" },
    { pattern: /\.task\s*\{/g, replacement: "LaunchedEffect(Unit) {", description: ".task → LaunchedEffect" },
    { pattern: /\.edgesIgnoringSafeArea\(.all\)/g, replacement: "", description: "Remove iOS-specific modifier" },
    { pattern: /\.usdz\b/g, replacement: ".glb", description: "Model format: USDZ → GLB" },
    { pattern: /@State\s+private\s+var/g, replacement: "var /* @State */ ", description: "@State → Compose state" },
    { pattern: /SIMD3<Float>\(([^)]+)\)/g, replacement: "Position($1)", description: "SIMD3 → Position" },
  ];

  for (const rule of iosPatterns) {
    if (rule.pattern.test(result)) {
      result = result.replace(rule.pattern, rule.replacement);
      changes.push(rule.description);
    }
  }

  warnings.push("Android uses GLB/glTF models, not USDZ. Convert models using Blender or gltf-transform.");
  warnings.push("Android requires explicit engine, modelLoader, and environmentLoader management.");
  warnings.push("Filament JNI calls must run on the main thread. Use rememberModelInstance in composables.");
  if (code.includes("Entity") || code.includes("entity")) {
    warnings.push("RealityKit Entity → SceneView Node types (ModelNode, LightNode, etc.).");
  }

  return { code: result, sourceplatform: "ios", targetPlatform: "android", changes, warnings };
}

export function generateMultiplatformCode(description: string): MultiplatformResult {
  const lower = description.toLowerCase();
  const isAR = lower.includes("ar") || lower.includes("augmented") || lower.includes("camera");
  const hasModel = lower.includes("model") || lower.includes("object") || lower.includes("3d") || !isAR;

  const modelName = "scene_object";

  const androidCode = isAR
    ? `@Composable
fun MultiplatformARScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/${modelName}.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null) {
                anchor = hitResult.createAnchor()
            }
            true
        }
    ) {
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                modelInstance?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 0.5f,
                        isEditable = true
                    )
                }
            }
        }
    }
}`
    : `@Composable
fun MultiplatformSceneScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/${modelName}.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f },
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f),
                isEditable = true
            )
        }
    }
}`;

  const iosCode = isAR
    ? `import SwiftUI
import SceneViewSwift
import RealityKit

struct MultiplatformARView: View {
    @State private var model: ModelNode?

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            showCoachingOverlay: true,
            onTapOnPlane: { position, arView in
                guard let model else { return }
                let anchor = AnchorNode.world(position: position)
                let clone = model.entity.clone(recursive: true)
                clone.scale = .init(repeating: 0.5)
                anchor.add(clone)
                arView.scene.addAnchor(anchor.entity)
            }
        )
        .edgesIgnoringSafeArea(.all)
        .task {
            do {
                model = try await ModelNode.load("models/${modelName}.usdz")
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`
    : `import SwiftUI
import SceneViewSwift
import RealityKit

struct MultiplatformSceneView: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { root in
            if let model {
                root.addChild(model.entity)
            }
        }
        .cameraControls(.orbit)
        .task {
            do {
                model = try await ModelNode.load("models/${modelName}.usdz")
                model?.scaleToUnits(1.0)
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`;

  return {
    androidCode,
    iosCode,
    description,
    notes: [
      "Android uses GLB/glTF models, iOS uses USDZ format.",
      "Both platforms render with PBR materials but use different engines (Filament vs RealityKit).",
      "Android requires explicit engine/loader management; iOS/RealityKit handles this automatically.",
      isAR ? "AR features: Android uses ARCore, iOS uses ARKit. Both support plane detection and anchoring." : "",
      `Replace 'models/${modelName}.glb' and 'models/${modelName}.usdz' with your actual model files.`,
    ].filter(Boolean),
  };
}

export function formatConversionResult(result: ConversionResult): string {
  const targetLabel = result.targetPlatform === "ios" ? "iOS (SwiftUI + RealityKit)" : "Android (Jetpack Compose + Filament)";
  const lang = result.targetPlatform === "ios" ? "swift" : "kotlin";
  const parts: string[] = [
    `## Code Converted to ${targetLabel}`,
    ``,
    `**${result.changes.length} conversion(s) applied.**`,
    ``,
    `### Converted Code`,
    ``,
    "```" + lang,
    result.code,
    "```",
    ``,
  ];

  if (result.changes.length > 0) {
    parts.push(`### Changes`);
    result.changes.forEach((c, i) => parts.push(`${i + 1}. ${c}`));
    parts.push(``);
  }

  if (result.warnings.length > 0) {
    parts.push(`### Manual Attention Required`);
    result.warnings.forEach((w, i) => parts.push(`${i + 1}. ${w}`));
  }

  return parts.join("\n");
}

export function formatMultiplatformResult(result: MultiplatformResult): string {
  return [
    `## Multiplatform Scene Code`,
    `**Description:** "${result.description}"`,
    ``,
    `### Android (Kotlin / Jetpack Compose)`,
    ``,
    "```kotlin",
    result.androidCode,
    "```",
    ``,
    `### iOS (Swift / SwiftUI)`,
    ``,
    "```swift",
    result.iosCode,
    "```",
    ``,
    `### Notes`,
    ...result.notes.map((n, i) => `${i + 1}. ${n}`),
  ].join("\n");
}
