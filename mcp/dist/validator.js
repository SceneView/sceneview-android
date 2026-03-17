function findLines(lines, pattern) {
    return lines
        .map((l, i) => (pattern.test(l) ? i + 1 : -1))
        .filter((n) => n !== -1);
}
const RULES = [
    // ─── Threading ────────────────────────────────────────────────────────────
    {
        id: "threading/filament-off-main-thread",
        severity: "error",
        check(code, lines) {
            const issues = [];
            if (!/Dispatchers\.(IO|Default)/.test(code))
                return issues;
            const filamentCallPatterns = [
                [/modelLoader\.createModel/, "modelLoader.createModel*"],
                [/modelLoader\.loadModel/, "modelLoader.loadModel*"],
                [/materialLoader\./, "materialLoader.*"],
                [/Texture\.Builder/, "Texture.Builder"],
                [/engine\.createTexture/, "engine.createTexture"],
            ];
            for (const [pat, name] of filamentCallPatterns) {
                if (pat.test(code)) {
                    findLines(lines, pat).forEach((line) => issues.push({
                        severity: "error",
                        rule: "threading/filament-off-main-thread",
                        message: `\`${name}\` detected alongside a background dispatcher. Filament JNI calls must run on the **main thread**. Use \`rememberModelInstance\` in composables, or wrap imperative code in \`withContext(Dispatchers.Main)\`.`,
                        line,
                    }));
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
            const issues = [];
            if (!code.includes("ARScene") && !code.includes("AnchorNode"))
                return issues;
            if (/\.worldPosition\s*=/.test(code)) {
                findLines(lines, /\.worldPosition\s*=/).forEach((line) => issues.push({
                    severity: "warning",
                    rule: "ar/node-not-anchor",
                    message: "Manually setting `worldPosition` inside an AR scene causes drift — ARCore remaps coordinates during tracking and plain nodes don't compensate. Use `AnchorNode(anchor = hitResult.createAnchor())` instead.",
                    line,
                }));
            }
            return issues;
        },
    },
    // ─── Missing null-check on rememberModelInstance ──────────────────────────
    {
        id: "composable/model-instance-null-check",
        severity: "error",
        check(code, lines) {
            const issues = [];
            const assignMatch = code.match(/val\s+(\w+)\s*=\s*rememberModelInstance\(/);
            if (!assignMatch)
                return issues;
            const varName = assignMatch[1];
            // Flag ModelNode uses where the variable is passed without null-guard (no ?. !! or ?: )
            const unsafeUse = new RegExp(`ModelNode\\s*\\([^)]*modelInstance\\s*=\\s*${varName}(?![?!])`);
            if (unsafeUse.test(code)) {
                findLines(lines, new RegExp(`modelInstance\\s*=\\s*${varName}(?![?!])`)).forEach((line) => issues.push({
                    severity: "error",
                    rule: "composable/model-instance-null-check",
                    message: `\`${varName}\` from \`rememberModelInstance\` is \`null\` while the asset loads. Guard it: \`${varName}?.let { ModelNode(modelInstance = it, ...) }\`.`,
                    line,
                }));
            }
            return issues;
        },
    },
    // ─── LightNode trailing lambda instead of named `apply =` ────────────────
    {
        id: "api/light-node-trailing-lambda",
        severity: "error",
        check(code, lines) {
            const issues = [];
            // Matches: LightNode(...) { — trailing lambda, not apply = { inside parens
            if (/LightNode\s*\([^)]*\)\s*\{/.test(code)) {
                findLines(lines, /LightNode\s*\([^)]*\)\s*\{/).forEach((line) => issues.push({
                    severity: "error",
                    rule: "api/light-node-trailing-lambda",
                    message: "`LightNode`'s configuration block is a **named parameter** `apply`, not a trailing lambda. Write `LightNode(engine = engine, type = ..., apply = { intensity(100_000f) })`. Without `apply =` the block is silently ignored and your light has default (zero) settings.",
                    line,
                }));
            }
            return issues;
        },
    },
    // ─── Engine created manually in composable ────────────────────────────────
    {
        id: "lifecycle/manual-engine-create",
        severity: "error",
        check(code, lines) {
            const issues = [];
            findLines(lines, /Engine\.create\(/).forEach((line) => issues.push({
                severity: "error",
                rule: "lifecycle/manual-engine-create",
                message: "`Engine.create()` called directly. In composables use `rememberEngine()` — it ties the Engine to the composition lifecycle and destroys it automatically on disposal, preventing leaks and double-destroy SIGABRTs.",
                line,
            }));
            return issues;
        },
    },
    // ─── Manual engine.destroy() alongside rememberEngine ────────────────────
    {
        id: "lifecycle/manual-engine-destroy",
        severity: "warning",
        check(code, lines) {
            const issues = [];
            if (!code.includes("rememberEngine"))
                return issues;
            findLines(lines, /engine\.(safeD|d)estroy\(\)/).forEach((line) => issues.push({
                severity: "warning",
                rule: "lifecycle/manual-engine-destroy",
                message: "`engine.destroy()` called manually alongside `rememberEngine()`. The engine is already destroyed on composition disposal — calling it again triggers a SIGABRT. Remove the manual call.",
                line,
            }));
            return issues;
        },
    },
    // ─── Texture destroy order (issue #630) ──────────────────────────────────
    {
        id: "lifecycle/texture-destroy-order",
        severity: "error",
        check(code, lines) {
            const issues = [];
            const texIdx = code.indexOf("safeDestroyTexture");
            const matIdx = code.indexOf("destroyMaterialInstance");
            if (texIdx !== -1 && matIdx !== -1 && texIdx < matIdx) {
                findLines(lines, /safeDestroyTexture|engine\.destroyTexture/).forEach((line) => issues.push({
                    severity: "error",
                    rule: "lifecycle/texture-destroy-order",
                    message: "Texture destroyed **before** the MaterialInstance that references it → SIGABRT (\"Invalid texture still bound to MaterialInstance\"). Destroy the MaterialInstance first: `materialLoader.destroyMaterialInstance(instance)`, then `engine.safeDestroyTexture(texture)`.",
                    line,
                }));
            }
            return issues;
        },
    },
    // ─── createModelInstance in composable (should be rememberModelInstance) ──
    {
        id: "composable/prefer-remember-model-instance",
        severity: "warning",
        check(code, lines) {
            const issues = [];
            findLines(lines, /modelLoader\.createModelInstance\(/).forEach((line) => issues.push({
                severity: "warning",
                rule: "composable/prefer-remember-model-instance",
                message: "`modelLoader.createModelInstance()` blocks the main thread. In composables, use `rememberModelInstance(modelLoader, path)` — it loads asynchronously, returns `null` while loading, and recomposes when ready.",
                line,
            }));
            return issues;
        },
    },
    // ─── Empty AnchorNode() ────────────────────────────────────────────────────
    {
        id: "ar/anchor-node-missing-anchor",
        severity: "error",
        check(code, lines) {
            const issues = [];
            findLines(lines, /AnchorNode\s*\(\s*\)/).forEach((line) => issues.push({
                severity: "error",
                rule: "ar/anchor-node-missing-anchor",
                message: "`AnchorNode()` requires an `anchor` from a hit result. Use `AnchorNode(anchor = hitResult.createAnchor())` inside `onTouchEvent` or `onSessionUpdated`.",
                line,
            }));
            return issues;
        },
    },
    // ─── 2.x → 3.0 renamed/removed APIs ─────────────────────────────────────
    {
        id: "migration/old-api",
        severity: "error",
        check(code, lines) {
            const issues = [];
            const renames = [
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
                    findLines(lines, pat).forEach((line) => issues.push({ severity: "error", rule: "migration/old-api", message: msg, line }));
                }
            }
            return issues;
        },
    },
    // ─── Scene missing engine param ───────────────────────────────────────────
    {
        id: "api/scene-missing-engine",
        severity: "error",
        check(code, lines) {
            const issues = [];
            // Scene( or ARScene( without engine = somewhere nearby
            const sceneCallLines = findLines(lines, /\b(AR)?Scene\s*\(/);
            sceneCallLines.forEach((line) => {
                // Look at the next 10 lines for engine =
                const block = lines.slice(line - 1, line + 10).join("\n");
                if (!block.includes("engine")) {
                    issues.push({
                        severity: "error",
                        rule: "api/scene-missing-engine",
                        message: "`Scene` / `ARScene` requires an `engine` parameter. Create one with `val engine = rememberEngine()` and pass it: `Scene(engine = engine, …)`.",
                        line,
                    });
                }
            });
            return issues;
        },
    },
];
export function validateCode(kotlinCode) {
    const lines = kotlinCode.split("\n");
    return RULES.flatMap((rule) => rule.check(kotlinCode, lines));
}
export function formatValidationReport(issues) {
    if (issues.length === 0) {
        return "✅ No issues found. The snippet follows SceneView best practices.";
    }
    const errors = issues.filter((i) => i.severity === "error");
    const warnings = issues.filter((i) => i.severity === "warning");
    const infos = issues.filter((i) => i.severity === "info");
    const icon = {
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
