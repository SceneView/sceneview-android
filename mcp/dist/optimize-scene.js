/**
 * optimize-scene.ts
 *
 * Analyzes SceneView code and suggests performance optimizations.
 */
export function optimizeScene(code) {
    const suggestions = [];
    const lines = code.split("\n");
    // ── Check for multiple engine instances ──
    const engineCount = (code.match(/rememberEngine\(\)/g) || []).length;
    if (engineCount > 1) {
        suggestions.push({
            severity: "high",
            category: "Memory",
            issue: `${engineCount} Engine instances detected. Each Engine allocates significant GPU memory.`,
            suggestion: "Create ONE engine at the top level and pass it down to all Scene composables.",
            codeExample: `// At top-level composable:\nval engine = rememberEngine()\n// Pass to all scenes:\nScene(engine = engine) { ... }`,
        });
    }
    // ── Check for per-frame allocations ──
    if (/onFrame\s*=\s*\{/.test(code)) {
        if (/Position\(/.test(code) && /onFrame/.test(code)) {
            suggestions.push({
                severity: "medium",
                category: "Performance",
                issue: "Potential per-frame Position allocation inside onFrame callback.",
                suggestion: "Reuse mutable position objects instead of creating new Position instances every frame.",
                codeExample: `// Before (bad): onFrame = { node.position = Position(x, y, z) }\n// After (good):\nval pos = remember { MutablePosition() }\nonFrame = { pos.set(x, y, z); node.position = pos }`,
            });
        }
    }
    // ── Check for large environment files ──
    if (/4k\.hdr|4K\.hdr|8k\.hdr|8K\.hdr/.test(code)) {
        suggestions.push({
            severity: "high",
            category: "Memory",
            issue: "Large HDR environment file detected (4K or 8K).",
            suggestion: "Use 2K HDR files for mobile. 4K+ wastes GPU memory with minimal visual improvement on small screens.",
            codeExample: `// Use 2K: "environments/sky_2k.hdr"\n// NOT: "environments/sky_4k.hdr"`,
        });
    }
    // ── Check for missing post-processing flag ──
    if (/Scene\s*\(/.test(code) && !/postProcessing/.test(code)) {
        suggestions.push({
            severity: "low",
            category: "Performance",
            issue: "Post-processing is enabled by default (bloom, SSAO, tone mapping).",
            suggestion: "If you don't need post-processing effects, disable them to save ~2ms per frame.",
            codeExample: `Scene(\n    engine = engine,\n    postProcessing = false // saves ~2ms/frame\n) { ... }`,
        });
    }
    // ── Check for too many concurrent model loads ──
    const modelLoadCount = (code.match(/rememberModelInstance/g) || []).length;
    if (modelLoadCount > 4) {
        suggestions.push({
            severity: "medium",
            category: "Memory",
            issue: `${modelLoadCount} concurrent model loads detected. Loading many models simultaneously spikes memory.`,
            suggestion: "Limit to 3-4 concurrent loads. Use pagination or lazy loading for large model lists.",
            codeExample: `// Load only visible models:\nval visibleItems = items.take(4)\nvisibleItems.forEach { item ->\n    rememberModelInstance(modelLoader, item.path)\n}`,
        });
    }
    // ── Check for missing scaleToUnits ──
    if (/ModelNode\s*\(/.test(code) && !/scaleToUnits/.test(code)) {
        suggestions.push({
            severity: "low",
            category: "Quality",
            issue: "ModelNode without scaleToUnits — model may render at unexpected size.",
            suggestion: "Use scaleToUnits to normalize model size. Without it, the model uses its original units which vary by source.",
            codeExample: `ModelNode(\n    modelInstance = instance,\n    scaleToUnits = 1.0f // normalizes to 1 meter\n)`,
        });
    }
    // ── Check for multiple shadow-casting lights ──
    const shadowLights = (code.match(/castShadows\s*\(\s*true\s*\)/g) || []).length;
    if (shadowLights > 2) {
        suggestions.push({
            severity: "high",
            category: "Performance",
            issue: `${shadowLights} shadow-casting lights detected. Each adds a depth render pass.`,
            suggestion: "Limit to 1-2 shadow-casting lights on mobile. Disable shadows on secondary lights.",
            codeExample: `// Primary light with shadows:\nLightNode(apply = { castShadows(true) })\n// Fill light without shadows:\nLightNode(apply = { castShadows(false) })`,
        });
    }
    // ── Check for texture compression hints ──
    if (/\.png|\.jpg|\.jpeg/.test(code) && /Texture|texture|bitmap/i.test(code)) {
        suggestions.push({
            severity: "medium",
            category: "Memory",
            issue: "Uncompressed texture format (PNG/JPG) detected.",
            suggestion: "Use KTX2 with Basis Universal compression for textures. 4-6x memory reduction on GPU.",
        });
    }
    // ── Check for Dispatchers.IO with Filament calls ──
    if (/Dispatchers\.(IO|Default)/.test(code) && /(modelLoader|materialLoader|engine\.)/.test(code)) {
        suggestions.push({
            severity: "high",
            category: "Correctness",
            issue: "Filament JNI calls detected near background dispatcher.",
            suggestion: "ALL Filament calls must run on the main thread. Use rememberModelInstance in composables or wrap in withContext(Dispatchers.Main).",
        });
    }
    // ── Check for missing environment/IBL ──
    if (/Scene\s*\(/.test(code) && !/environment|createHDR|rememberEnvironment/.test(code) && /ModelNode/.test(code)) {
        suggestions.push({
            severity: "medium",
            category: "Quality",
            issue: "No environment/IBL detected. Metallic surfaces will appear black without image-based lighting.",
            suggestion: "Add an HDR environment for physically-correct reflections on PBR materials.",
            codeExample: `Scene(\n    environment = rememberEnvironment(environmentLoader) {\n        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")\n            ?: createEnvironment(environmentLoader)\n    }\n)`,
        });
    }
    // ── Check for missing Modifier.fillMaxSize ──
    if (/Scene\s*\(/.test(code) && !/fillMaxSize/.test(code)) {
        suggestions.push({
            severity: "low",
            category: "Quality",
            issue: "Scene may have zero size without Modifier.fillMaxSize().",
            suggestion: "Add modifier = Modifier.fillMaxSize() to ensure the 3D view is visible.",
        });
    }
    // Calculate score
    const highCount = suggestions.filter((s) => s.severity === "high").length;
    const mediumCount = suggestions.filter((s) => s.severity === "medium").length;
    const lowCount = suggestions.filter((s) => s.severity === "low").length;
    const score = Math.max(0, 100 - highCount * 20 - mediumCount * 10 - lowCount * 5);
    let summary;
    if (score >= 90) {
        summary = "Excellent! This code follows SceneView best practices with minimal optimization opportunities.";
    }
    else if (score >= 70) {
        summary = "Good code with some optimization opportunities. Address the high-severity items for best performance.";
    }
    else if (score >= 50) {
        summary = "Several optimization opportunities found. Focus on high-severity items first for the biggest impact.";
    }
    else {
        summary = "Significant performance issues detected. Address the high-severity items immediately to avoid crashes and poor performance.";
    }
    return { suggestions, score, summary };
}
export function formatOptimizationReport(report) {
    const parts = [
        `## Scene Optimization Report`,
        ``,
        `**Score:** ${report.score}/100`,
        `**Summary:** ${report.summary}`,
        ``,
    ];
    if (report.suggestions.length === 0) {
        parts.push("No optimization suggestions. Your code looks well-optimized!");
        return parts.join("\n");
    }
    const severityIcon = { high: "!!!", medium: "!!", low: "!" };
    const severityLabel = { high: "HIGH", medium: "MEDIUM", low: "LOW" };
    // Group by severity
    for (const sev of ["high", "medium", "low"]) {
        const items = report.suggestions.filter((s) => s.severity === sev);
        if (items.length === 0)
            continue;
        parts.push(`### ${severityIcon[sev]} ${severityLabel[sev]} Priority (${items.length})\n`);
        for (const item of items) {
            parts.push(`**[${item.category}] ${item.issue}**`);
            parts.push(`> ${item.suggestion}`);
            if (item.codeExample) {
                parts.push("```kotlin");
                parts.push(item.codeExample);
                parts.push("```");
            }
            parts.push(``);
        }
    }
    return parts.join("\n");
}
