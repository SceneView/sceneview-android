/**
 * migrate-code.ts
 *
 * Automatic migration of SceneView 2.x Kotlin code to 3.x.
 * Applies known renames, API changes, and pattern transformations.
 */

export interface MigrationChange {
  line: number;
  before: string;
  after: string;
  rule: string;
  explanation: string;
}

export interface MigrationResult {
  originalCode: string;
  migratedCode: string;
  changes: MigrationChange[];
  warnings: string[];
}

interface MigrationRule {
  id: string;
  pattern: RegExp;
  replacement: string | ((match: string, ...groups: string[]) => string);
  explanation: string;
  /** If true, apply only once (not globally) */
  once?: boolean;
}

const MIGRATION_RULES: MigrationRule[] = [
  // ── Composable renames ──────────────────────────────────────────────────
  {
    id: "rename-sceneview-to-scene",
    pattern: /\bSceneView\s*\(/g,
    replacement: "Scene(",
    explanation: "`SceneView(...)` renamed to `Scene(...)` in 3.0.",
  },
  {
    id: "rename-arsceneview-to-arscene",
    pattern: /\bArSceneView\s*\(/g,
    replacement: "ARScene(",
    explanation: "`ArSceneView(...)` renamed to `ARScene(...)` in 3.0.",
  },

  // ── Model loading ──────────────────────────────────────────────────────
  {
    id: "replace-loadModelAsync",
    pattern: /modelLoader\.loadModelAsync\s*\(\s*"([^"]+)"\s*\)/g,
    replacement: 'rememberModelInstance(modelLoader, "$1")',
    explanation: "`loadModelAsync` removed. Use `rememberModelInstance(modelLoader, path)` which returns null while loading.",
  },
  {
    id: "replace-loadModel",
    pattern: /modelLoader\.loadModel\s*\(\s*"([^"]+)"\s*\)(?!Instance)/g,
    replacement: 'rememberModelInstance(modelLoader, "$1")',
    explanation: "`loadModel` replaced by `rememberModelInstance` in composables or `loadModelInstanceAsync` for imperative code.",
  },

  // ── Removed nodes ──────────────────────────────────────────────────────
  {
    id: "replace-transformable-node",
    pattern: /\bTransformableNode\s*\([^)]*\)\s*\.apply\s*\{[^}]*setParent\([^)]*\)[^}]*\}/g,
    replacement: "// TransformableNode removed in 3.0 — use `isEditable = true` on ModelNode instead",
    explanation: "`TransformableNode` removed. Set `isEditable = true` on `ModelNode` for pinch-to-scale + drag-to-rotate.",
  },
  {
    id: "replace-transformable-node-simple",
    pattern: /\bTransformableNode\b/g,
    replacement: "/* TransformableNode removed — use ModelNode(isEditable = true) */",
    explanation: "`TransformableNode` removed. Set `isEditable = true` on `ModelNode`.",
  },
  {
    id: "replace-placement-node",
    pattern: /\bPlacementNode\b/g,
    replacement: "/* PlacementNode removed — use AnchorNode + HitResultNode */",
    explanation: "`PlacementNode` removed. Use `AnchorNode(anchor = hitResult.createAnchor())` + `HitResultNode`.",
  },
  {
    id: "replace-view-renderable",
    pattern: /ViewRenderable\.builder\(\)/g,
    replacement: "/* ViewRenderable removed — use ViewNode with @Composable lambda */",
    explanation: "`ViewRenderable` removed. Use `ViewNode(windowManager = rememberViewNodeManager()) { /* Compose content */ }`.",
  },

  // ── LightNode trailing lambda fix ──────────────────────────────────────
  {
    id: "fix-lightnode-trailing-lambda",
    pattern: /LightNode\s*\(([^)]*)\)\s*\{/g,
    replacement: "LightNode($1, apply = {",
    explanation: "`LightNode`'s configuration is `apply = { }` (named param), not a trailing lambda. Without `apply =` the block is silently ignored.",
  },

  // ── Engine lifecycle ──────────────────────────────────────────────────
  {
    id: "replace-engine-create",
    pattern: /val\s+(\w+)\s*=\s*Engine\.create\(\)/g,
    replacement: "val $1 = rememberEngine()",
    explanation: "`Engine.create()` replaced by `rememberEngine()` which ties lifecycle to composition.",
  },
  {
    id: "remove-engine-destroy",
    pattern: /\bengine\.(safeD|d)estroy\(\)/g,
    replacement: "// engine.destroy() removed — rememberEngine() handles cleanup automatically",
    explanation: "Manual `engine.destroy()` removed. `rememberEngine()` destroys on composition disposal.",
  },

  // ── Environment loading ────────────────────────────────────────────────
  {
    id: "replace-loadEnvironment",
    pattern: /environmentLoader\.loadEnvironment\s*\(\s*"([^"]+)"\s*\)/g,
    replacement: 'environmentLoader.createHDREnvironment("$1")',
    explanation: "`loadEnvironment` renamed to `createHDREnvironment` in 3.0.",
  },

  // ── Import renames ─────────────────────────────────────────────────────
  {
    id: "replace-sceneform-import",
    pattern: /import\s+com\.google\.ar\.sceneform\./g,
    replacement: "import io.github.sceneview.",
    explanation: "Sceneform imports (`com.google.ar.sceneform.*`) replaced by `io.github.sceneview.*`. SceneView is the official Sceneform successor.",
  },

  // ── addChildNode → composable DSL ──────────────────────────────────────
  {
    id: "replace-addChildNode",
    pattern: /(\w+)\.addChildNode\((\w+)\)/g,
    replacement: "// $1.addChildNode($2) — in 3.0, declare nodes as composables inside Scene { } instead of adding imperatively",
    explanation: "Imperative `addChildNode` replaced by declarative composable DSL. Declare nodes inside `Scene { }` content block.",
  },

  // ── AR worldPosition → AnchorNode ──────────────────────────────────────
  {
    id: "replace-worldPosition-ar",
    pattern: /(\w+)\.worldPosition\s*=\s*(.+)/g,
    replacement: "// $1.worldPosition = $2  ← replaced by AnchorNode(anchor = hitResult.createAnchor()) in 3.0",
    explanation: "Manual `worldPosition` in AR causes drift. Use `AnchorNode(anchor = hitResult.createAnchor())` instead.",
  },

  // ── Camera manipulator ─────────────────────────────────────────────────
  {
    id: "replace-camera-manipulator",
    pattern: /setCameraManipulator\(([^)]*)\)/g,
    replacement: "// setCameraManipulator removed — use cameraManipulator = rememberCameraManipulator() on Scene",
    explanation: "`setCameraManipulator` replaced by `cameraManipulator = rememberCameraManipulator()` parameter on `Scene`.",
  },

  // ── Material loading ──────────────────────────────────────────────────
  {
    id: "replace-materialLoader-loadMaterial",
    pattern: /materialLoader\.loadMaterial\s*\(\s*"([^"]+)"\s*\)/g,
    replacement: 'materialLoader.createMaterial("$1")',
    explanation: "`loadMaterial` renamed to `createMaterial` in 3.0.",
  },

  // ── Scene background ──────────────────────────────────────────────────
  {
    id: "replace-setBackground",
    pattern: /\bsetBackground\s*\(\s*([^)]+)\s*\)/g,
    replacement: "// setBackground($1) removed — use Scene(environment = ...) for background or skybox",
    explanation: "`setBackground` removed. Use the `environment` parameter on `Scene` for backgrounds and skyboxes.",
  },

  // ── Node.setRenderable → ModelNode ──────────────────────────────────
  {
    id: "replace-setRenderable",
    pattern: /(\w+)\.setRenderable\(([^)]*)\)/g,
    replacement: "// $1.setRenderable($2) → in 3.0, pass modelInstance directly to ModelNode() constructor",
    explanation: "`setRenderable()` removed. In 3.0, pass the model directly: `ModelNode(modelInstance = instance)`.",
  },

  // ── onUpdate → onFrame ──────────────────────────────────────────────
  {
    id: "replace-onUpdate",
    pattern: /\.onUpdate\s*=\s*\{/g,
    replacement: "// .onUpdate removed — use Scene(onFrame = {",
    explanation: "`.onUpdate` removed. Use the `onFrame` callback parameter on `Scene(onFrame = { ... })`.",
  },

  // ── Node.setParent → composable DSL ──────────────────────────────────
  {
    id: "replace-setParent",
    pattern: /(\w+)\.setParent\((\w+)\)/g,
    replacement: "// $1.setParent($2) — in 3.0, nest nodes as composables inside parent's content block",
    explanation: "`setParent()` removed. In 3.0, declare child nodes as composables inside the parent's trailing content block.",
  },

  // ── ArFragment → ARScene composable ─────────────────────────────────
  {
    id: "replace-arfragment",
    pattern: /\bArFragment\b/g,
    replacement: "/* ArFragment removed — use ARScene composable in Jetpack Compose */",
    explanation: "`ArFragment` (Android View-based) removed. Use `ARScene(...)` composable in Jetpack Compose.",
  },

  // ── onTapArPlane → onTouchEvent ─────────────────────────────────────
  {
    id: "replace-onTapArPlane",
    pattern: /\.onTapArPlane\s*\{/g,
    replacement: "// .onTapArPlane removed — use ARScene(onTouchEvent = { hitResult, motionEvent ->",
    explanation: "`.onTapArPlane` removed. Use `ARScene(onTouchEvent = { hitResult, motionEvent -> ... })` and call `hitResult.createAnchor()` yourself.",
  },
];

export function migrateCode(code: string): MigrationResult {
  const changes: MigrationChange[] = [];
  const warnings: string[] = [];
  let result = code;

  // Apply each rule
  for (const rule of MIGRATION_RULES) {
    const pattern = new RegExp(rule.pattern.source, rule.pattern.flags);
    let match: RegExpExecArray | null;
    const localMatches: { index: number; match: string }[] = [];

    // Collect matches first
    while ((match = pattern.exec(result)) !== null) {
      localMatches.push({ index: match.index, match: match[0] });
      if (rule.once) break;
    }

    if (localMatches.length > 0) {
      // Compute line numbers from original positions
      for (const m of localMatches) {
        const lineNum = result.substring(0, m.index).split("\n").length;
        const replaced = typeof rule.replacement === "string"
          ? m.match.replace(
              new RegExp(rule.pattern.source, rule.pattern.flags.replace("g", "")),
              rule.replacement
            )
          : m.match.replace(
              new RegExp(rule.pattern.source, rule.pattern.flags.replace("g", "")),
              rule.replacement as (substring: string, ...args: string[]) => string
            );
        changes.push({
          line: lineNum,
          before: m.match.trim(),
          after: replaced.trim(),
          rule: rule.id,
          explanation: rule.explanation,
        });
      }

      // Apply replacement
      if (typeof rule.replacement === "string") {
        result = result.replace(
          new RegExp(rule.pattern.source, rule.pattern.flags),
          rule.replacement
        );
      } else {
        result = result.replace(
          new RegExp(rule.pattern.source, rule.pattern.flags),
          rule.replacement as (substring: string, ...args: string[]) => string
        );
      }
    }
  }

  // Add warnings for things that need manual attention
  if (code.includes("ModelRenderable.builder")) {
    warnings.push("`ModelRenderable.builder()` is completely removed in 3.0 — it has no direct equivalent. Use GLB/glTF assets with `rememberModelInstance(modelLoader, path)` instead.");
  }
  if (code.includes("rememberEngine") && !result.includes("rememberEngine")) {
    // Already using 3.0 style
  }
  if (code.includes("Scene(") && !code.includes("engine")) {
    warnings.push("`Scene(...)` requires an explicit `engine` parameter in 3.0. Add `val engine = rememberEngine()` and pass it.");
  }
  if (/node\w*\.destroy\(\)/.test(code) && /Scene\s*[({]/.test(code)) {
    warnings.push("Manual `node.destroy()` calls inside composable Scenes should be removed — Compose manages node lifecycle automatically.");
  }
  if (code.includes("ArFragment")) {
    warnings.push("`ArFragment` is the old Android View-based AR component. In 3.0, use the `ARScene` composable in Jetpack Compose instead.");
  }
  if (code.includes("Dispatchers.IO") && (code.includes("modelLoader") || code.includes("materialLoader"))) {
    warnings.push("**CRITICAL**: Filament calls (modelLoader, materialLoader) on `Dispatchers.IO` will cause SIGABRT. Use `Dispatchers.Main` or `rememberModelInstance` in composables.");
  }
  if (code.includes("setRenderable")) {
    warnings.push("`setRenderable()` removed in 3.0. Pass model instances directly to `ModelNode(modelInstance = instance)` constructor.");
  }
  if (code.includes(".setParent(")) {
    warnings.push("`.setParent()` removed in 3.0. Declare child nodes as composables inside the parent's content block instead.");
  }
  if (code.includes("onTapArPlane")) {
    warnings.push("`onTapArPlane` callback removed. In 3.0, use `onTouchEvent` on `ARScene` and call `hitResult.createAnchor()` yourself.");
  }

  return {
    originalCode: code,
    migratedCode: result,
    changes,
    warnings,
  };
}

export function formatMigrationResult(result: MigrationResult): string {
  if (result.changes.length === 0 && result.warnings.length === 0) {
    return "No 2.x patterns detected. This code appears to already use SceneView 3.x APIs.\n\nIf you're still having issues, try `get_migration_guide` for the full migration reference.";
  }

  const parts: string[] = [
    `## Migration Result\n`,
    `**${result.changes.length} change(s) applied**, ${result.warnings.length} warning(s).\n`,
  ];

  if (result.changes.length > 0) {
    parts.push(`### Changes Applied\n`);
    for (let i = 0; i < result.changes.length; i++) {
      const c = result.changes[i];
      parts.push(
        `**${i + 1}. \`${c.rule}\`** (line ${c.line})`,
        `- Before: \`${c.before}\``,
        `- After: \`${c.after}\``,
        `- ${c.explanation}`,
        ``
      );
    }
  }

  if (result.warnings.length > 0) {
    parts.push(`### Manual Attention Required\n`);
    result.warnings.forEach((w, i) => {
      parts.push(`${i + 1}. ${w}`);
    });
    parts.push("");
  }

  parts.push(`### Migrated Code\n`);
  parts.push("```kotlin");
  parts.push(result.migratedCode);
  parts.push("```");

  parts.push(`\n### Migration Checklist\n`);
  parts.push("- [ ] Review all `// removed` comments and refactor manually");
  parts.push("- [ ] Add `val engine = rememberEngine()` if not present");
  parts.push("- [ ] Add `val modelLoader = rememberModelLoader(engine)` if loading models");
  parts.push("- [ ] Null-check every `rememberModelInstance` result");
  parts.push("- [ ] Test compilation and runtime");

  return parts.join("\n");
}
