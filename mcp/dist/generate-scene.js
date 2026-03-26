/**
 * generate-scene.ts
 *
 * Generates a complete Scene{} or ARScene{} composable from a text description.
 * Maps common objects/concepts to SceneView node types and builds compilable code.
 */
const OBJECT_MAPPINGS = [
    // Furniture
    { keywords: ["table"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 1.0, defaultPosition: [0, 0.4, 0], comment: "Table (flat cube)" },
    { keywords: ["chair"], nodeType: "ModelNode", defaultScale: 0.8, defaultPosition: [0, 0, -1], comment: "Chair (use GLB model)" },
    { keywords: ["desk"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 1.2, defaultPosition: [0, 0.35, 0], comment: "Desk" },
    { keywords: ["shelf", "bookshelf"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 1.5, defaultPosition: [2, 0.75, 0], comment: "Shelf" },
    { keywords: ["bed"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 2.0, defaultPosition: [0, 0.25, 0], comment: "Bed" },
    { keywords: ["sofa", "couch"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 1.8, defaultPosition: [0, 0.35, 0], comment: "Sofa" },
    // Basic shapes
    { keywords: ["box", "cube", "crate"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 0.5, defaultPosition: [0, 0.25, 0], comment: "Box" },
    { keywords: ["sphere", "ball", "globe", "orb"], nodeType: "SphereNode", geometryType: "sphere", defaultScale: 0.5, defaultPosition: [0, 0.5, 0], comment: "Sphere" },
    { keywords: ["cylinder", "pillar", "column"], nodeType: "CylinderNode", geometryType: "cylinder", defaultScale: 0.5, defaultPosition: [0, 0.5, 0], comment: "Cylinder" },
    { keywords: ["plane", "floor", "ground"], nodeType: "PlaneNode", geometryType: "plane", defaultScale: 5.0, defaultPosition: [0, 0, 0], comment: "Ground plane" },
    { keywords: ["wall"], nodeType: "CubeNode", geometryType: "cube", defaultScale: 3.0, defaultPosition: [0, 1.5, -3], comment: "Wall" },
    // Environment
    { keywords: ["tree", "plant"], nodeType: "ModelNode", defaultScale: 2.0, defaultPosition: [2, 0, 2], comment: "Tree (use GLB model)" },
    { keywords: ["car", "vehicle"], nodeType: "ModelNode", defaultScale: 2.0, defaultPosition: [0, 0, 0], comment: "Car (use GLB model)" },
    { keywords: ["house", "building"], nodeType: "ModelNode", defaultScale: 5.0, defaultPosition: [0, 0, -5], comment: "Building (use GLB model)" },
    // People / characters
    { keywords: ["person", "character", "human", "avatar", "robot"], nodeType: "ModelNode", defaultScale: 1.7, defaultPosition: [0, 0, 0], comment: "Character (use GLB model)" },
];
const LIGHT_KEYWORDS = ["sun", "sunlight", "daylight", "bright", "lit", "sunny", "well-lit"];
const INDOOR_KEYWORDS = ["room", "indoor", "inside", "interior"];
const OUTDOOR_KEYWORDS = ["outdoor", "outside", "garden", "park", "street"];
const AR_KEYWORDS = ["ar", "augmented", "real world", "camera", "place in room", "place on"];
const DARK_KEYWORDS = ["night", "dark", "dim", "mood", "candlelight"];
function parseDescription(description) {
    const lower = description.toLowerCase();
    const objects = [];
    // Parse quantity + object patterns
    for (const mapping of OBJECT_MAPPINGS) {
        for (const keyword of mapping.keywords) {
            // Match "two chairs", "3 tables", "a chair", "chairs"
            const countPatterns = [
                new RegExp(`(\\d+)\\s+${keyword}s?`, "i"),
                new RegExp(`(one|two|three|four|five|six|seven|eight|nine|ten)\\s+${keyword}s?`, "i"),
                new RegExp(`(a|an)\\s+${keyword}\\b`, "i"),
                new RegExp(`\\b${keyword}s\\b`, "i"), // plural without number = 2
                new RegExp(`\\b${keyword}\\b`, "i"), // singular without article = 1
            ];
            for (const pat of countPatterns) {
                const match = lower.match(pat);
                if (match) {
                    let count = 1;
                    if (match[1]) {
                        const numWords = {
                            one: 1, two: 2, three: 3, four: 4, five: 5,
                            six: 6, seven: 7, eight: 8, nine: 9, ten: 10,
                            a: 1, an: 1,
                        };
                        count = numWords[match[1].toLowerCase()] || parseInt(match[1]) || 1;
                    }
                    if (pat.source.includes("s\\b") && !match[1])
                        count = 2; // plural default
                    objects.push({ mapping, count });
                    break;
                }
            }
        }
    }
    // Deduplicate
    const seen = new Set();
    const unique = objects.filter((o) => {
        const key = o.mapping.keywords[0];
        if (seen.has(key))
            return false;
        seen.add(key);
        return true;
    });
    return {
        objects: unique,
        isAR: AR_KEYWORDS.some((k) => lower.includes(k)),
        isIndoor: INDOOR_KEYWORDS.some((k) => lower.includes(k)),
        isOutdoor: OUTDOOR_KEYWORDS.some((k) => lower.includes(k)),
        needsSky: OUTDOOR_KEYWORDS.some((k) => lower.includes(k)) || lower.includes("sky"),
        isDark: DARK_KEYWORDS.some((k) => lower.includes(k)),
    };
}
function spreadPositions(count, basePos, spacing) {
    if (count === 1)
        return [basePos];
    const positions = [];
    const halfSpan = ((count - 1) * spacing) / 2;
    for (let i = 0; i < count; i++) {
        positions.push([
            basePos[0] + i * spacing - halfSpan,
            basePos[1],
            basePos[2],
        ]);
    }
    return positions;
}
export function generateScene(description) {
    const parsed = parseDescription(description);
    const elements = [];
    const notes = [];
    const dependencies = [];
    let hasModel = false;
    // Track geometry node counters for naming
    let nodeCounter = 0;
    // Generate elements for each parsed object
    for (const obj of parsed.objects) {
        const positions = spreadPositions(obj.count, obj.mapping.defaultPosition, 1.5);
        for (let i = 0; i < obj.count; i++) {
            const pos = positions[i];
            if (obj.mapping.nodeType === "ModelNode") {
                hasModel = true;
                elements.push({
                    type: "model",
                    nodeType: "ModelNode",
                    properties: {
                        modelPath: `models/${obj.mapping.keywords[0]}.glb`,
                        scaleToUnits: String(obj.mapping.defaultScale),
                        position: `Position(${pos[0].toFixed(1)}f, ${pos[1].toFixed(1)}f, ${pos[2].toFixed(1)}f)`,
                    },
                    comment: obj.mapping.comment,
                });
            }
            else {
                elements.push({
                    type: "geometry",
                    nodeType: obj.mapping.nodeType,
                    properties: {
                        size: String(obj.mapping.defaultScale),
                        position: `Position(${pos[0].toFixed(1)}f, ${pos[1].toFixed(1)}f, ${pos[2].toFixed(1)}f)`,
                    },
                    comment: obj.mapping.comment,
                });
            }
            nodeCounter++;
        }
    }
    // Add light
    if (!parsed.isDark) {
        elements.push({
            type: "light",
            nodeType: "LightNode",
            properties: {
                type: "LightManager.Type.DIRECTIONAL",
                intensity: parsed.isOutdoor ? "110_000f" : "100_000f",
                castShadows: "true",
            },
            comment: parsed.isOutdoor ? "Sunlight" : "Main directional light",
        });
    }
    else {
        elements.push({
            type: "light",
            nodeType: "LightNode",
            properties: {
                type: "LightManager.Type.POINT",
                intensity: "50_000f",
                position: "Position(0f, 2f, 0f)",
                castShadows: "false",
            },
            comment: "Dim ambient light",
        });
    }
    // Add ground plane if not already present
    const hasGround = elements.some((e) => e.nodeType === "PlaneNode" || (e.comment && e.comment.toLowerCase().includes("ground")));
    if (!hasGround && !parsed.isAR) {
        elements.push({
            type: "geometry",
            nodeType: "PlaneNode",
            properties: {
                size: "10.0f",
                position: "Position(0f, 0f, 0f)",
            },
            comment: "Ground plane",
        });
    }
    // Build the code
    const isAR = parsed.isAR;
    dependencies.push(isAR ? "io.github.sceneview:arsceneview:3.3.0" : "io.github.sceneview:sceneview:3.3.0");
    // Build model instance declarations
    const modelElements = elements.filter((e) => e.type === "model");
    const uniqueModels = new Map();
    modelElements.forEach((e) => {
        const path = e.properties.modelPath;
        if (!uniqueModels.has(path)) {
            const varName = path.replace("models/", "").replace(".glb", "").replace(/[^a-zA-Z0-9]/g, "") + "Instance";
            uniqueModels.set(path, varName);
        }
    });
    // Generate Kotlin code
    const lines = [];
    lines.push("@Composable");
    lines.push(`fun GeneratedScene() {`);
    lines.push("    val engine = rememberEngine()");
    lines.push("    val modelLoader = rememberModelLoader(engine)");
    if (!isAR) {
        lines.push("    val environmentLoader = rememberEnvironmentLoader(engine)");
    }
    lines.push("");
    // Model instance declarations
    for (const [path, varName] of uniqueModels) {
        lines.push(`    val ${varName} = rememberModelInstance(modelLoader, "${path}")`);
    }
    if (uniqueModels.size > 0)
        lines.push("");
    // Scene or ARScene
    if (isAR) {
        lines.push("    var anchor by remember { mutableStateOf<Anchor?>(null) }");
        lines.push("");
        lines.push("    ARScene(");
        lines.push("        modifier = Modifier.fillMaxSize(),");
        lines.push("        engine = engine,");
        lines.push("        modelLoader = modelLoader,");
        lines.push("        planeRenderer = true,");
        lines.push("        onTouchEvent = { event, hitResult ->");
        lines.push("            if (event.action == MotionEvent.ACTION_UP && hitResult != null) {");
        lines.push("                anchor = hitResult.createAnchor()");
        lines.push("            }");
        lines.push("            true");
        lines.push("        }");
        lines.push("    ) {");
        lines.push("        anchor?.let { a ->");
        lines.push("            AnchorNode(anchor = a) {");
    }
    else {
        lines.push("    Scene(");
        lines.push("        modifier = Modifier.fillMaxSize(),");
        lines.push("        engine = engine,");
        lines.push("        modelLoader = modelLoader,");
        if (!parsed.isDark) {
            lines.push('        environment = rememberEnvironment(environmentLoader) {');
            lines.push('            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")');
            lines.push('                ?: createEnvironment(environmentLoader)');
            lines.push("        }");
        }
        lines.push("    ) {");
    }
    const indent = isAR ? "                " : "        ";
    // Generate nodes
    for (const elem of elements) {
        lines.push(`${indent}// ${elem.comment}`);
        switch (elem.type) {
            case "model": {
                const varName = uniqueModels.get(elem.properties.modelPath);
                lines.push(`${indent}${varName}?.let { instance ->`);
                lines.push(`${indent}    ModelNode(`);
                lines.push(`${indent}        modelInstance = instance,`);
                lines.push(`${indent}        scaleToUnits = ${elem.properties.scaleToUnits}f,`);
                if (elem.properties.position !== "Position(0.0f, 0.0f, 0.0f)") {
                    lines.push(`${indent}        centerOrigin = ${elem.properties.position}`);
                }
                lines.push(`${indent}    )`);
                lines.push(`${indent}}`);
                break;
            }
            case "geometry": {
                const nodeType = elem.nodeType;
                lines.push(`${indent}${nodeType}(`);
                lines.push(`${indent}    engine = engine,`);
                lines.push(`${indent}    size = ${elem.properties.size}`);
                lines.push(`${indent})`);
                break;
            }
            case "light": {
                lines.push(`${indent}LightNode(`);
                lines.push(`${indent}    engine = engine,`);
                lines.push(`${indent}    type = ${elem.properties.type},`);
                lines.push(`${indent}    apply = {`);
                lines.push(`${indent}        intensity(${elem.properties.intensity})`);
                lines.push(`${indent}        castShadows(${elem.properties.castShadows})`);
                lines.push(`${indent}    }`);
                lines.push(`${indent})`);
                break;
            }
        }
        lines.push("");
    }
    // Close braces
    if (isAR) {
        lines.push("            }");
        lines.push("        }");
    }
    lines.push("    }");
    lines.push("}");
    // Notes
    if (hasModel) {
        notes.push("This scene references GLB model files. You need to provide actual .glb files in `src/main/assets/models/`.");
        notes.push("Free 3D models: https://sketchfab.com/features/free-3d-models (download as GLB)");
    }
    if (isAR) {
        notes.push("AR requires camera permission at runtime and ARCore on the device. See `get_platform_setup(\"android\", \"ar\")` for full setup.");
    }
    if (elements.length === 1) {
        notes.push("Only a light was generated. The description didn't match any known objects. Try mentioning specific objects like 'table', 'chair', 'sphere', 'cube', etc.");
    }
    return {
        code: lines.join("\n"),
        description,
        elements,
        isAR,
        dependencies,
        notes,
    };
}
export function formatGeneratedScene(result) {
    const parts = [];
    parts.push(`## Generated Scene\n`);
    parts.push(`**Description:** "${result.description}"`);
    parts.push(`**Mode:** ${result.isAR ? "AR (ARScene)" : "3D (Scene)"}`);
    parts.push(`**Elements:** ${result.elements.length} nodes\n`);
    parts.push(`### Dependency\n`);
    parts.push("```kotlin");
    result.dependencies.forEach((d) => parts.push(`implementation("${d}")`));
    parts.push("```\n");
    parts.push(`### Code\n`);
    parts.push("```kotlin");
    parts.push(result.code);
    parts.push("```\n");
    if (result.notes.length > 0) {
        parts.push(`### Notes\n`);
        result.notes.forEach((n, i) => parts.push(`${i + 1}. ${n}`));
    }
    return parts.join("\n");
}
