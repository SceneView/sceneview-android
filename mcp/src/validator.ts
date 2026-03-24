export interface ValidationIssue {
  severity: "error" | "warning" | "info";
  rule: string;
  message: string;
  line?: number;
}

interface Rule {
  id: string;
  severity: ValidationIssue["severity"];
  check: (code: string, lines: string[]) => ValidationIssue[];
}

function findLines(lines: string[], pattern: RegExp): number[] {
  return lines
    .map((l, i) => (pattern.test(l) ? i + 1 : -1))
    .filter((n) => n !== -1);
}

const RULES: Rule[] = [
  // ─── Threading ────────────────────────────────────────────────────────────
  {
    id: "threading/filament-off-main-thread",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!/Dispatchers\.(IO|Default)/.test(code)) return issues;
      const filamentCallPatterns: [RegExp, string][] = [
        [/modelLoader\.createModel/, "modelLoader.createModel*"],
        [/modelLoader\.loadModel/, "modelLoader.loadModel*"],
        [/materialLoader\./, "materialLoader.*"],
        [/Texture\.Builder/, "Texture.Builder"],
        [/engine\.createTexture/, "engine.createTexture"],
      ];
      for (const [pat, name] of filamentCallPatterns) {
        if (pat.test(code)) {
          findLines(lines, pat).forEach((line) =>
            issues.push({
              severity: "error",
              rule: "threading/filament-off-main-thread",
              message: `\`${name}\` detected alongside a background dispatcher. Filament JNI calls must run on the **main thread**. Use \`rememberModelInstance\` in composables, or wrap imperative code in \`withContext(Dispatchers.Main)\`.`,
              line,
            })
          );
        }
      }
      return issues;
    },
  },

  // ─── AR: plain Node worldPosition instead of AnchorNode ──────────────────
  {
    id: "ar/node-not-anchor",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("ARScene") && !code.includes("AnchorNode")) return issues;
      if (/\.worldPosition\s*=/.test(code)) {
        findLines(lines, /\.worldPosition\s*=/).forEach((line) =>
          issues.push({
            severity: "warning",
            rule: "ar/node-not-anchor",
            message:
              "Manually setting `worldPosition` inside an AR scene causes drift — ARCore remaps coordinates during tracking and plain nodes don't compensate. Use `AnchorNode(anchor = hitResult.createAnchor())` instead.",
            line,
          })
        );
      }
      return issues;
    },
  },

  // ─── Missing null-check on rememberModelInstance ──────────────────────────
  {
    id: "composable/model-instance-null-check",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      const assignMatch = code.match(/val\s+(\w+)\s*=\s*rememberModelInstance\(/);
      if (!assignMatch) return issues;
      const varName = assignMatch[1];
      // Flag ModelNode uses where the variable is passed without null-guard (no ?. !! or ?: )
      const unsafeUse = new RegExp(`ModelNode\\s*\\([^)]*modelInstance\\s*=\\s*${varName}(?![?!])`);
      if (unsafeUse.test(code)) {
        findLines(lines, new RegExp(`modelInstance\\s*=\\s*${varName}(?![?!])`)).forEach((line) =>
          issues.push({
            severity: "error",
            rule: "composable/model-instance-null-check",
            message: `\`${varName}\` from \`rememberModelInstance\` is \`null\` while the asset loads. Guard it: \`${varName}?.let { ModelNode(modelInstance = it, ...) }\`.`,
            line,
          })
        );
      }
      return issues;
    },
  },

  // ─── LightNode trailing lambda instead of named `apply =` ────────────────
  {
    id: "api/light-node-trailing-lambda",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // Matches: LightNode(...) { — trailing lambda, not apply = { inside parens
      if (/LightNode\s*\([^)]*\)\s*\{/.test(code)) {
        findLines(lines, /LightNode\s*\([^)]*\)\s*\{/).forEach((line) =>
          issues.push({
            severity: "error",
            rule: "api/light-node-trailing-lambda",
            message:
              "`LightNode`'s configuration block is a **named parameter** `apply`, not a trailing lambda. Write `LightNode(engine = engine, type = ..., apply = { intensity(100_000f) })`. Without `apply =` the block is silently ignored and your light has default (zero) settings.",
            line,
          })
        );
      }
      return issues;
    },
  },

  // ─── Engine created manually in composable ────────────────────────────────
  {
    id: "lifecycle/manual-engine-create",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      findLines(lines, /Engine\.create\(/).forEach((line) =>
        issues.push({
          severity: "error",
          rule: "lifecycle/manual-engine-create",
          message:
            "`Engine.create()` called directly. In composables use `rememberEngine()` — it ties the Engine to the composition lifecycle and destroys it automatically on disposal, preventing leaks and double-destroy SIGABRTs.",
          line,
        })
      );
      return issues;
    },
  },

  // ─── Manual engine.destroy() alongside rememberEngine ────────────────────
  {
    id: "lifecycle/manual-engine-destroy",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("rememberEngine")) return issues;
      findLines(lines, /engine\.(safeD|d)estroy\(\)/).forEach((line) =>
        issues.push({
          severity: "warning",
          rule: "lifecycle/manual-engine-destroy",
          message:
            "`engine.destroy()` called manually alongside `rememberEngine()`. The engine is already destroyed on composition disposal — calling it again triggers a SIGABRT. Remove the manual call.",
          line,
        })
      );
      return issues;
    },
  },

  // ─── Texture destroy order (issue #630) ──────────────────────────────────
  {
    id: "lifecycle/texture-destroy-order",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      const texIdx = code.indexOf("safeDestroyTexture");
      const matIdx = code.indexOf("destroyMaterialInstance");
      if (texIdx !== -1 && matIdx !== -1 && texIdx < matIdx) {
        findLines(lines, /safeDestroyTexture|engine\.destroyTexture/).forEach((line) =>
          issues.push({
            severity: "error",
            rule: "lifecycle/texture-destroy-order",
            message:
              "Texture destroyed **before** the MaterialInstance that references it → SIGABRT (\"Invalid texture still bound to MaterialInstance\"). Destroy the MaterialInstance first: `materialLoader.destroyMaterialInstance(instance)`, then `engine.safeDestroyTexture(texture)`.",
            line,
          })
        );
      }
      return issues;
    },
  },

  // ─── createModelInstance in composable (should be rememberModelInstance) ──
  {
    id: "composable/prefer-remember-model-instance",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      findLines(lines, /modelLoader\.createModelInstance\(/).forEach((line) =>
        issues.push({
          severity: "warning",
          rule: "composable/prefer-remember-model-instance",
          message:
            "`modelLoader.createModelInstance()` blocks the main thread. In composables, use `rememberModelInstance(modelLoader, path)` — it loads asynchronously, returns `null` while loading, and recomposes when ready.",
          line,
        })
      );
      return issues;
    },
  },

  // ─── Empty AnchorNode() ────────────────────────────────────────────────────
  {
    id: "ar/anchor-node-missing-anchor",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      findLines(lines, /AnchorNode\s*\(\s*\)/).forEach((line) =>
        issues.push({
          severity: "error",
          rule: "ar/anchor-node-missing-anchor",
          message:
            "`AnchorNode()` requires an `anchor` from a hit result. Use `AnchorNode(anchor = hitResult.createAnchor())` inside `onTouchEvent` or `onSessionUpdated`.",
          line,
        })
      );
      return issues;
    },
  },

  // ─── 2.x → 3.0 renamed/removed APIs ─────────────────────────────────────
  {
    id: "migration/old-api",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      const renames: Array<[RegExp, string]> = [
        [/\bSceneView\s*\(/, "`SceneView(…)` → renamed to `Scene(…)` in 3.0"],
        [/\bArSceneView\s*\(/, "`ArSceneView(…)` → renamed to `ARScene(…)` in 3.0"],
        [/\bPlacementNode\b/, "`PlacementNode` removed → use `AnchorNode` + `HitResultNode` in 3.0"],
        [/\bTransformableNode\b/, "`TransformableNode` removed → set `isEditable = true` on `ModelNode` in 3.0"],
        [/\bViewRenderable\b/, "`ViewRenderable` removed → use `ViewNode` with a `@Composable` content lambda in 3.0"],
        [/\bmodelLoader\.loadModelAsync\b/, "`loadModelAsync` removed → use `rememberModelInstance` in composables (3.0)"],
        [/\bmodelLoader\.loadModel\b(?!Instance)/, "`loadModel` → use `rememberModelInstance` or `loadModelInstanceAsync` (3.0)"],
      ];
      for (const [pat, msg] of renames) {
        if (pat.test(code)) {
          findLines(lines, pat).forEach((line) =>
            issues.push({ severity: "error", rule: "migration/old-api", message: msg, line })
          );
        }
      }
      return issues;
    },
  },

  // ─── FogNode missing view parameter ──────────────────────────────────────
  {
    id: "api/fog-node-missing-view",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("FogNode")) return issues;
      const fogLines = findLines(lines, /FogNode\s*\(/);
      fogLines.forEach((line) => {
        const block = lines.slice(line - 1, line + 5).join("\n");
        if (!block.includes("view")) {
          issues.push({
            severity: "error",
            rule: "api/fog-node-missing-view",
            message:
              "`FogNode` requires a `view` parameter — the same Filament View passed to `Scene(view = view)`. Create one with `val view = rememberView(engine)` and pass it to both `Scene` and `FogNode`.",
            line,
          });
        }
      });
      return issues;
    },
  },

  // ─── ReflectionProbeNode missing cameraPosition ────────────────────────
  {
    id: "api/reflection-probe-missing-camera",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("ReflectionProbeNode")) return issues;
      const probeLines = findLines(lines, /ReflectionProbeNode\s*\(/);
      probeLines.forEach((line) => {
        const block = lines.slice(line - 1, line + 8).join("\n");
        if (!block.includes("cameraPosition")) {
          issues.push({
            severity: "warning",
            rule: "api/reflection-probe-missing-camera",
            message:
              "`ReflectionProbeNode` needs `cameraPosition` to detect when the camera enters its zone. Track it in `onFrame`: `onFrame = { cameraPosition = cameraNode.worldPosition }` and pass it to the probe.",
            line,
          });
        }
      });
      return issues;
    },
  },

  // ─── PhysicsNode without radius offset ──────────────────────────────────
  {
    id: "api/physics-node-missing-radius",
    severity: "info",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("PhysicsNode")) return issues;
      const physicsLines = findLines(lines, /PhysicsNode\s*\(/);
      physicsLines.forEach((line) => {
        const block = lines.slice(line - 1, line + 8).join("\n");
        if (!block.includes("radius")) {
          issues.push({
            severity: "info",
            rule: "api/physics-node-missing-radius",
            message:
              "`PhysicsNode` defaults to `radius = 0f` — the collision point is the node centre, not the surface. For spheres, set `radius` to match the sphere radius so the surface touches the floor.",
            line,
          });
        }
      });
      return issues;
    },
  },

  // ─── DynamicSkyNode outside SceneScope ──────────────────────────────────
  {
    id: "api/dynamic-sky-outside-scene",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("DynamicSkyNode")) return issues;
      // If DynamicSkyNode appears but Scene { } doesn't, it's likely wrong
      if (!code.includes("Scene(") && !code.includes("Scene {")) {
        findLines(lines, /DynamicSkyNode\s*\(/).forEach((line) =>
          issues.push({
            severity: "warning",
            rule: "api/dynamic-sky-outside-scene",
            message:
              "`DynamicSkyNode` is a `SceneScope` extension composable — it must be declared inside a `Scene { }` content block, not at the top level.",
            line,
          })
        );
      }
      return issues;
    },
  },

  // ─── Deprecated Sceneform imports ───────────────────────────────────────
  {
    id: "migration/sceneform-import",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      findLines(lines, /import\s+com\.google\.ar\.sceneform/).forEach((line) =>
        issues.push({
          severity: "error",
          rule: "migration/sceneform-import",
          message:
            "Sceneform imports detected (`com.google.ar.sceneform.*`). Sceneform was deprecated by Google in 2021. Use `io.github.sceneview.*` imports instead — SceneView is the official successor.",
          line,
        })
      );
      return issues;
    },
  },

  // ─── Node.destroy() called manually ────────────────────────────────────
  {
    id: "lifecycle/manual-node-destroy",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (!code.includes("Scene(") && !code.includes("Scene {")) return issues;
      findLines(lines, /\bnode\w*\.destroy\(\)|\.\bdestroy\(\)/).forEach((line) => {
        const lineContent = lines[line - 1];
        if (lineContent && !lineContent.includes("engine") && !lineContent.includes("Engine")) {
          issues.push({
            severity: "warning",
            rule: "lifecycle/manual-node-destroy",
            message:
              "Manual `destroy()` on a node inside a composable Scene. Compose manages node lifecycle automatically — nodes are destroyed when they leave composition. Remove the manual call to avoid double-destroy crashes.",
            line,
          });
        }
      });
      return issues;
    },
  },

  // ─── Scene missing engine param ───────────────────────────────────────────
  {
    id: "api/scene-missing-engine",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // Scene( or ARScene( without engine = somewhere nearby
      const sceneCallLines = findLines(lines, /\b(AR)?Scene\s*\(/);
      sceneCallLines.forEach((line) => {
        // Look at the next 10 lines for engine =
        const block = lines.slice(line - 1, line + 10).join("\n");
        if (!block.includes("engine")) {
          issues.push({
            severity: "error",
            rule: "api/scene-missing-engine",
            message:
              "`Scene` / `ARScene` requires an `engine` parameter. Create one with `val engine = rememberEngine()` and pass it: `Scene(engine = engine, …)`.",
            line,
          });
        }
      });
      return issues;
    },
  },
];

// ─── Swift Validation Rules ──────────────────────────────────────────────────

const SWIFT_RULES: Rule[] = [
  // ─── Missing @MainActor for async model loading ────────────────────────────
  {
    id: "swift/missing-main-actor",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // If there's an async function that loads models but no @MainActor annotation
      if (/func\s+\w+.*async/.test(code) && /ModelNode\.load\(/.test(code)) {
        if (!code.includes("@MainActor")) {
          findLines(lines, /func\s+\w+.*async/).forEach((line) =>
            issues.push({
              severity: "warning",
              rule: "swift/missing-main-actor",
              message:
                "Async function that loads RealityKit models should be annotated with `@MainActor` or called from a `@MainActor` context. RealityKit entity operations are main-thread-bound. Using `.task { }` in SwiftUI is already `@MainActor`-isolated, but standalone functions should be annotated.",
              line,
            })
          );
        }
      }
      return issues;
    },
  },

  // ─── Missing async/await for ModelNode.load ────────────────────────────────
  {
    id: "swift/model-load-not-async",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // ModelNode.load is async throws — must use try await
      if (/ModelNode\.load\(/.test(code)) {
        findLines(lines, /ModelNode\.load\(/).forEach((line) => {
          const lineContent = lines[line - 1];
          if (lineContent && !lineContent.includes("await") && !lineContent.includes("try")) {
            issues.push({
              severity: "error",
              rule: "swift/model-load-not-async",
              message:
                "`ModelNode.load()` is `async throws` — you must call it with `try await ModelNode.load(\"model.usdz\")` inside a `.task { }` block or async function.",
              line,
            });
          }
        });
      }
      return issues;
    },
  },

  // ─── Missing import statements ─────────────────────────────────────────────
  {
    id: "swift/missing-imports",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // Check if SceneViewSwift types are used but not imported
      const svTypes = ["SceneView", "ARSceneView", "ModelNode", "GeometryNode", "LightNode", "TextNode", "VideoNode", "PhysicsNode", "BillboardNode", "AnchorNode", "AugmentedImageNode", "ImageNode", "CameraNode", "PathNode", "LineNode", "MeshNode", "FogNode", "DynamicSkyNode", "ReflectionProbeNode"];
      const usesSceneView = svTypes.some((t) => code.includes(t));
      if (usesSceneView && !code.includes("import SceneViewSwift")) {
        issues.push({
          severity: "warning",
          rule: "swift/missing-imports",
          message:
            "SceneViewSwift types used but `import SceneViewSwift` not found. Add `import SceneViewSwift` at the top of the file.",
          line: 1,
        });
      }
      // Check for RealityKit types without import
      const rkTypes = ["Entity", "ModelEntity", "AnchorEntity", "RealityView", "MeshResource", "SimpleMaterial"];
      const usesRK = rkTypes.some((t) => new RegExp(`\\b${t}\\b`).test(code));
      if (usesRK && !code.includes("import RealityKit")) {
        issues.push({
          severity: "warning",
          rule: "swift/missing-imports",
          message:
            "RealityKit types used but `import RealityKit` not found. Add `import RealityKit` at the top of the file.",
          line: 1,
        });
      }
      return issues;
    },
  },

  // ─── Common RealityKit mistakes: using addChild on non-Entity ──────────────
  {
    id: "swift/add-child-wrong-type",
    severity: "error",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // Common mistake: passing ModelNode instead of ModelNode.entity
      const patterns: [RegExp, string][] = [
        [/addChild\(\s*model\s*\)/, "model"],
        [/addChild\(\s*cube\s*\)/, "cube"],
        [/addChild\(\s*sphere\s*\)/, "sphere"],
        [/addChild\(\s*light\s*\)/, "light"],
        [/addChild\(\s*text\s*\)/, "text"],
      ];
      for (const [pat, name] of patterns) {
        // Only flag if the variable is likely a SceneViewSwift node wrapper, not a raw Entity
        if (pat.test(code) && new RegExp(`(ModelNode|GeometryNode|LightNode|TextNode|VideoNode|BillboardNode).*\\b${name}\\b`).test(code)) {
          findLines(lines, pat).forEach((line) =>
            issues.push({
              severity: "error",
              rule: "swift/add-child-wrong-type",
              message:
                `\`addChild(${name})\` — SceneViewSwift node wrappers are not \`Entity\` subclasses. Use \`addChild(${name}.entity)\` to add the underlying RealityKit entity.`,
              line,
            })
          );
        }
      }
      return issues;
    },
  },

  // ─── ARSceneView used on macOS or visionOS ─────────────────────────────────
  {
    id: "swift/ar-platform-check",
    severity: "info",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      if (code.includes("ARSceneView")) {
        if (code.includes("macOS") || code.includes("visionOS")) {
          findLines(lines, /ARSceneView/).forEach((line) =>
            issues.push({
              severity: "info",
              rule: "swift/ar-platform-check",
              message:
                "`ARSceneView` uses `ARView` which is only available on iOS. For macOS use `SceneView` (3D only). For visionOS, use RealityKit's `RealityView` with `ARKitSession` directly.",
              line,
            })
          );
        }
      }
      return issues;
    },
  },

  // ─── Missing error handling on model load ──────────────────────────────────
  {
    id: "swift/unhandled-model-load-error",
    severity: "warning",
    check(code, lines) {
      const issues: ValidationIssue[] = [];
      // Using try? silently swallows errors — warning but not error
      if (/try\?\s+await\s+ModelNode\.load/.test(code)) {
        findLines(lines, /try\?\s+await\s+ModelNode\.load/).forEach((line) =>
          issues.push({
            severity: "warning",
            rule: "swift/unhandled-model-load-error",
            message:
              "`try?` on `ModelNode.load()` silently swallows load failures. Consider using `do { try await ... } catch { print(error) }` to at least log failures, or show a loading indicator.",
            line,
          })
        );
      }
      return issues;
    },
  },
];

function detectLanguage(code: string): "kotlin" | "swift" {
  // Heuristic: if the code has Swift markers, validate as Swift
  if (
    code.includes("import SwiftUI") ||
    code.includes("import RealityKit") ||
    code.includes("import SceneViewSwift") ||
    code.includes("struct ") && code.includes(": View") ||
    code.includes("@State private var") ||
    /SceneView\s*\{.*\bin\b/.test(code)
  ) {
    return "swift";
  }
  return "kotlin";
}

export function validateCode(code: string): ValidationIssue[] {
  const lines = code.split("\n");
  const lang = detectLanguage(code);
  if (lang === "swift") {
    return SWIFT_RULES.flatMap((rule) => rule.check(code, lines));
  }
  return RULES.flatMap((rule) => rule.check(code, lines));
}

export function formatValidationReport(issues: ValidationIssue[]): string {
  if (issues.length === 0) {
    return "✅ No issues found. The snippet follows SceneView best practices.";
  }

  const errors = issues.filter((i) => i.severity === "error");
  const warnings = issues.filter((i) => i.severity === "warning");
  const infos = issues.filter((i) => i.severity === "info");

  const icon: Record<ValidationIssue["severity"], string> = {
    error: "🔴",
    warning: "🟡",
    info: "🔵",
  };

  const header = `Found **${issues.length} issue(s)**: ${errors.length} error(s), ${warnings.length} warning(s), ${infos.length} info(s).\n`;
  const body = issues
    .map((issue, i) => {
      const loc = issue.line ? ` (line ${issue.line})` : "";
      return [
        `### ${i + 1}. ${icon[issue.severity]} ${issue.severity.toUpperCase()}${loc}`,
        `**Rule:** \`${issue.rule}\``,
        issue.message,
      ].join("\n");
    })
    .join("\n\n");

  return header + "\n" + body;
}
