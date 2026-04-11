// ─── Shared deprecated-API checks for SceneView MCP validators ───────────────
//
// This module is the single source of truth for the "Deprecated APIs" and
// "Missing imports" validation blocks that used to be copy-pasted across the
// 4 vertical MCP packages (interior, gaming, healthcare, automotive).
//
// Importing flow:
//   - Each sub-package's tsconfig.json declares `rootDirs: ["src", "../shared/src"]`
//     so this file is treated as a sibling of the package's own src files.
//   - Validators import via:  `import { ... } from "./deprecated-api-check.js"`
//   - tsc compiles this file into each sub-package's `dist/` alongside the
//     validator, so the published npm package is fully self-contained.
//
// IMPORTANT: do not change the error messages without coordinating across
// packages. They ship on npm and downstream consumers grep their content.
/**
 * Detect usage of pre-v3.6 deprecated composable names and pre-3.0 Sceneform imports.
 *
 * The `Scene { }` / `ARScene { }` composables were renamed to `SceneView { }` /
 * `ARSceneView { }` in v3.6 for cross-platform consistency with SceneViewSwift.
 * The old names are kept as @Deprecated aliases in Scene.kt but should not
 * appear in new code.
 *
 * Returns an array of `error`-severity issues to merge into the caller's list.
 */
export function checkDeprecatedApi(code) {
    const issues = [];
    const deprecated = [
        [/(?<![\w.])Scene\s*\(/, "`Scene { }` is deprecated since v3.6. Use `SceneView { }`."],
        [/(?<![\w.])ARScene\s*\(/, "`ARScene { }` is deprecated since v3.6. Use `ARSceneView { }`."],
        [/ArSceneView\s*\(/, "`ArSceneView()` is Sceneform 1.x. Use `ARSceneView { }` from io.github.sceneview."],
        [/loadModelAsync/, "`loadModelAsync` is Sceneform. Use `rememberModelInstance` in composables."],
        [/Engine\.create/, "`Engine.create` is imperative. Use `rememberEngine()` in composables."],
        [/import\s+.*sceneform/, "Sceneform imports are obsolete. SceneView 3.x does not use Sceneform."],
    ];
    for (const [pattern, msg] of deprecated) {
        if (pattern.test(code)) {
            issues.push({ severity: "error", message: msg });
        }
    }
    return issues;
}
/**
 * Detect usage of `SceneView { }` or `ARSceneView { }` without the corresponding
 * `import io.github.sceneview.SceneView` / `io.github.sceneview.ar.ARSceneView` import.
 *
 * Returns an array of `warning`-severity issues to merge into the caller's list.
 */
export function checkMissingSceneViewImports(code) {
    const issues = [];
    // Detect usage of the `SceneView { }` composable without a corresponding import.
    if (/(?<![\w.])SceneView\s*[\(\{]/.test(code) &&
        !/import\s+io\.github\.sceneview\.SceneView\b/.test(code)) {
        issues.push({
            severity: "warning",
            message: "Missing SceneView import. Add: import io.github.sceneview.SceneView",
        });
    }
    // Detect usage of the `ARSceneView { }` composable without a corresponding import.
    if (/(?<![\w.])ARSceneView\s*[\(\{]/.test(code) &&
        !/import\s+io\.github\.sceneview\.ar\.ARSceneView\b/.test(code)) {
        issues.push({
            severity: "warning",
            message: "Missing ARSceneView import. Add: import io.github.sceneview.ar.ARSceneView",
        });
    }
    return issues;
}
