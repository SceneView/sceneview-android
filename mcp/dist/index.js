#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListResourcesRequestSchema, ListToolsRequestSchema, ReadResourceRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { getSample, SAMPLE_IDS, SAMPLES } from "./samples.js";
import { validateCode, formatValidationReport } from "./validator.js";
import { MIGRATION_GUIDE } from "./migration.js";
import { fetchKnownIssues } from "./issues.js";
import { parseNodeSections, findNodeSection, listNodeTypes } from "./node-reference.js";
import { PLATFORM_ROADMAP, BEST_PRACTICES, AR_SETUP_GUIDE, TROUBLESHOOTING_GUIDE } from "./guides.js";
import { buildPreviewUrl, validatePreviewInput, formatPreviewResponse } from "./preview.js";
import { validateArtifactInput, generateArtifact, formatArtifactResponse } from "./artifact.js";
import { getPlatformSetup, PLATFORM_IDS } from "./platform-setup.js";
import { migrateCode, formatMigrationResult } from "./migrate-code.js";
import { getDebugGuide, autoDetectIssue, DEBUG_CATEGORIES } from "./debug-issue.js";
import { generateScene, formatGeneratedScene } from "./generate-scene.js";
const __dirname = dirname(fileURLToPath(import.meta.url));
// ─── Legal disclaimer ─────────────────────────────────────────────────────────
const DISCLAIMER = '\n\n---\n*Generated code suggestion. Review before use in production. See [TERMS.md](https://github.com/SceneView/sceneview/blob/main/mcp/TERMS.md).*';
function withDisclaimer(content) {
    if (content.length === 0)
        return content;
    const last = content[content.length - 1];
    return [
        ...content.slice(0, -1),
        { ...last, text: last.text + DISCLAIMER },
    ];
}
let API_DOCS;
try {
    API_DOCS = readFileSync(resolve(__dirname, "../llms.txt"), "utf-8");
}
catch {
    API_DOCS = "SceneView API docs not found. Run `npm run prepare` to bundle llms.txt.";
}
const NODE_SECTIONS = parseNodeSections(API_DOCS);
const server = new Server({ name: "@sceneview/mcp", version: "3.4.13" }, { capabilities: { resources: {}, tools: {} } });
// ─── Resources ───────────────────────────────────────────────────────────────
server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: [
        {
            uri: "sceneview://api",
            name: "SceneView API Reference",
            description: "Complete SceneView 3.3.0 API — Scene, ARScene, SceneScope DSL, ARSceneScope DSL, node types, resource loading, camera, gestures, math types, threading rules, and common patterns. Read this before writing any SceneView code.",
            mimeType: "text/markdown",
        },
        {
            uri: "sceneview://known-issues",
            name: "SceneView Open GitHub Issues",
            description: "Live list of open issues from the SceneView GitHub repository. Check this before reporting a bug or when something isn't working — there may already be a known workaround.",
            mimeType: "text/markdown",
        },
    ],
}));
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    switch (request.params.uri) {
        case "sceneview://api":
            return {
                contents: [{ uri: "sceneview://api", mimeType: "text/markdown", text: API_DOCS }],
            };
        case "sceneview://known-issues": {
            const issues = await fetchKnownIssues();
            return {
                contents: [{ uri: "sceneview://known-issues", mimeType: "text/markdown", text: issues }],
            };
        }
        default:
            throw new Error(`Unknown resource: ${request.params.uri}`);
    }
});
// ─── Tools ───────────────────────────────────────────────────────────────────
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "get_sample",
            description: "Returns a complete, compilable Kotlin sample for a given SceneView scenario. Use this to get a working starting point before customising. Call `list_samples` first if you are unsure which scenario fits.",
            inputSchema: {
                type: "object",
                properties: {
                    scenario: {
                        type: "string",
                        enum: SAMPLE_IDS,
                        description: `The scenario to fetch:\n${SAMPLE_IDS.map((id) => `- "${id}": ${SAMPLES[id].description}`).join("\n")}`,
                    },
                },
                required: ["scenario"],
            },
        },
        {
            name: "list_samples",
            description: "Lists all available SceneView code samples with their IDs, descriptions, and tags. Use this to find the right sample before calling `get_sample`, or to show the user what SceneView can do.",
            inputSchema: {
                type: "object",
                properties: {
                    tag: {
                        type: "string",
                        description: "Optional tag to filter by (e.g. \"ar\", \"3d\", \"ios\", \"swift\", \"anchor\", \"geometry\", \"animation\", \"video\", \"lighting\"). Omit to list all samples.",
                    },
                },
                required: [],
            },
        },
        {
            name: "get_setup",
            description: "Returns the Gradle dependency and AndroidManifest snippet required to use SceneView in an Android project.",
            inputSchema: {
                type: "object",
                properties: {
                    type: {
                        type: "string",
                        enum: ["3d", "ar"],
                        description: '"3d" for 3D-only scenes. "ar" for augmented reality (includes 3D).',
                    },
                },
                required: ["type"],
            },
        },
        {
            name: "validate_code",
            description: "Checks a Kotlin or Swift SceneView snippet for common mistakes. For Kotlin: threading violations, wrong destroy order, missing null-checks, LightNode trailing-lambda bug, deprecated 2.x APIs. For Swift: missing @MainActor, async/await patterns, missing imports, RealityKit mistakes. Language is auto-detected. Always call this before presenting generated SceneView code to the user.",
            inputSchema: {
                type: "object",
                properties: {
                    code: {
                        type: "string",
                        description: "The Kotlin or Swift source code to validate (composable function, SwiftUI view, class, or file). Language is auto-detected.",
                    },
                },
                required: ["code"],
            },
        },
        {
            name: "get_migration_guide",
            description: "Returns the full SceneView 2.x → 3.0 migration guide. Use this when a user reports code that worked in 2.x but breaks in 3.0, or when helping someone upgrade.",
            inputSchema: {
                type: "object",
                properties: {},
                required: [],
            },
        },
        {
            name: "get_node_reference",
            description: "Returns the full API reference for a specific SceneView node type or composable — parameters, types, and a usage example — parsed directly from the official llms.txt. Use this when you need the exact signature or options for a node (e.g. ModelNode, LightNode, ARScene). If the requested type is not found, the response lists all available types.",
            inputSchema: {
                type: "object",
                properties: {
                    nodeType: {
                        type: "string",
                        description: 'The node type or composable name to look up, e.g. "ModelNode", "LightNode", "ARScene", "AnchorNode". Case-insensitive.',
                    },
                },
                required: ["nodeType"],
            },
        },
        {
            name: "get_platform_roadmap",
            description: "Returns the SceneView multi-platform roadmap — current Android support status, planned iOS/KMP/web targets, and timeline. Use this when the user asks about cross-platform support, iOS, Kotlin Multiplatform, or future plans.",
            inputSchema: {
                type: "object",
                properties: {},
                required: [],
            },
        },
        {
            name: "get_best_practices",
            description: "Returns SceneView performance and architecture best practices — memory management, model optimization, threading rules, Compose integration patterns, and common anti-patterns. Use this when the user asks about performance, optimization, best practices, or architecture.",
            inputSchema: {
                type: "object",
                properties: {
                    category: {
                        type: "string",
                        enum: ["all", "performance", "architecture", "memory", "threading"],
                        description: 'Category to filter by. "all" returns everything. Defaults to "all" if omitted.',
                    },
                },
                required: [],
            },
        },
        {
            name: "get_ar_setup",
            description: "Returns detailed AR setup instructions — AndroidManifest permissions and features, Gradle dependencies, ARCore session configuration options (depth, light estimation, instant placement, plane detection, image tracking, cloud anchors), and a complete working AR starter template. More detailed than `get_setup` for AR-specific configuration.",
            inputSchema: {
                type: "object",
                properties: {},
                required: [],
            },
        },
        {
            name: "get_troubleshooting",
            description: "Returns the SceneView troubleshooting guide — common crashes (SIGABRT, model not showing), build failures, AR issues (drift, overexposure, image detection), and performance problems. Use this when a user reports something not working, a crash, or unexpected behavior.",
            inputSchema: {
                type: "object",
                properties: {},
                required: [],
            },
        },
        {
            name: "get_ios_setup",
            description: "Returns the complete iOS setup guide for SceneViewSwift — SPM dependency, Package.swift example, minimum platform versions, Info.plist entries for AR (camera permission), and basic SwiftUI integration code. Use this when a user wants to set up SceneView for iOS, macOS, or visionOS.",
            inputSchema: {
                type: "object",
                properties: {
                    type: {
                        type: "string",
                        enum: ["3d", "ar"],
                        description: '"3d" for 3D-only scenes. "ar" for augmented reality (requires iOS, not macOS/visionOS via this path).',
                    },
                },
                required: ["type"],
            },
        },
        {
            name: "get_web_setup",
            description: "Returns the complete Web setup guide for SceneView Web — npm install, Kotlin/JS Gradle config, HTML canvas setup, and basic Filament.js integration code. SceneView Web uses the same Filament rendering engine as Android, compiled to WebAssembly. Use this when a user wants to set up SceneView for browsers.",
            inputSchema: {
                type: "object",
                properties: {},
            },
        },
        {
            name: "render_3d_preview",
            description: "Generates an interactive 3D preview link. Accepts a model URL, a SceneView code snippet, or both. Returns a URL to sceneview.github.io/preview that renders the model in the browser with orbit controls, AR support, and sharing. For model URLs: embeds a model-viewer link directly. For code snippets: shows the 3D preview with the code in a companion panel. Use this when you want to show a 3D model to the user — paste the link in your response and they can click to see it live.",
            inputSchema: {
                type: "object",
                properties: {
                    modelUrl: {
                        type: "string",
                        description: "Public URL to a .glb or .gltf model file. Must be HTTPS and CORS-enabled. If omitted, a default model is used.",
                    },
                    codeSnippet: {
                        type: "string",
                        description: "SceneView code snippet (Kotlin or Swift) to display alongside the 3D preview in a companion panel. Useful when showing generated code together with a live preview.",
                    },
                    autoRotate: {
                        type: "boolean",
                        description: "Auto-rotate the model (default: true).",
                    },
                    ar: {
                        type: "boolean",
                        description: "Enable AR mode on supported devices (default: true).",
                    },
                    title: {
                        type: "string",
                        description: "Custom title shown above the preview.",
                    },
                },
                required: [],
            },
        },
        {
            name: "create_3d_artifact",
            description: 'Generates a complete, self-contained HTML page with interactive 3D content that Claude can render as an artifact. Returns valid HTML using model-viewer (Google\'s web component for 3D). Use this when the user asks to "show", "preview", "visualize" 3D models, create 3D charts/dashboards, or view products in 360°. The HTML works standalone in any browser, supports AR on mobile, and includes orbit controls. Types: "model-viewer" for 3D model viewing, "chart-3d" for 3D data visualization (bar charts with perspective), "scene" for rich 3D scenes with lighting, "product-360" for product turntables with hotspot annotations.',
            inputSchema: {
                type: "object",
                properties: {
                    type: {
                        type: "string",
                        enum: ["model-viewer", "chart-3d", "scene", "product-360", "geometry"],
                        description: '"model-viewer": interactive 3D model viewer with orbit + AR. "chart-3d": 3D bar chart for data visualization. "scene": rich 3D scene with lighting. "product-360": product turntable with hotspot annotations. "geometry": procedural 3D shapes (cubes, spheres, cylinders, planes, lines) — Claude can DRAW in 3D! Use this when the user asks to draw, build, or visualize 3D shapes.',
                    },
                    modelUrl: {
                        type: "string",
                        description: "Public HTTPS URL to a .glb or .gltf model. If omitted, a default model is used. Not needed for chart-3d type.",
                    },
                    title: {
                        type: "string",
                        description: "Title displayed on the artifact. Defaults to a sensible name per type.",
                    },
                    data: {
                        type: "array",
                        items: {
                            type: "object",
                            properties: {
                                label: { type: "string", description: "Data point label (e.g. 'Q1 2025')" },
                                value: { type: "number", description: "Numeric value" },
                                color: { type: "string", description: "Optional hex color (e.g. '#4285F4'). Auto-assigned if omitted." },
                            },
                            required: ["label", "value"],
                        },
                        description: 'Data array for chart-3d type. Each item has {label, value, color?}. Required for "chart-3d", ignored for other types.',
                    },
                    options: {
                        type: "object",
                        properties: {
                            autoRotate: { type: "boolean", description: "Auto-rotate the model (default: true)" },
                            ar: { type: "boolean", description: "Enable AR on mobile devices (default: true)" },
                            backgroundColor: { type: "string", description: "Background color as hex (default: '#1a1a2e')" },
                            cameraOrbit: { type: "string", description: "Camera orbit string (default: '0deg 75deg 105%')" },
                        },
                        description: "Visual options for the 3D artifact.",
                    },
                    hotspots: {
                        type: "array",
                        items: {
                            type: "object",
                            properties: {
                                position: { type: "string", description: 'Hotspot 3D position, e.g. "0.5 1.2 0.3"' },
                                normal: { type: "string", description: 'Hotspot surface normal, e.g. "0 1 0"' },
                                label: { type: "string", description: "Hotspot label" },
                                description: { type: "string", description: "Hotspot description" },
                            },
                            required: ["position", "normal", "label"],
                        },
                        description: "Annotation hotspots for product-360 type. Each has position, normal, label, and optional description.",
                    },
                    shapes: {
                        type: "array",
                        items: {
                            type: "object",
                            properties: {
                                type: {
                                    type: "string",
                                    enum: ["cube", "sphere", "cylinder", "plane", "line"],
                                    description: 'Shape type: "cube", "sphere", "cylinder", "plane" (flat surface), or "line" (thin cylinder connecting points).',
                                },
                                position: {
                                    type: "array",
                                    items: { type: "number" },
                                    description: "Position [x, y, z] in world space. Y is up. Default: [0, 0, 0].",
                                },
                                scale: {
                                    type: "array",
                                    items: { type: "number" },
                                    description: "Scale [x, y, z]. For cube: edge sizes. For sphere: diameters. For line: [length, thickness, thickness]. Default: [1, 1, 1].",
                                },
                                color: {
                                    type: "array",
                                    items: { type: "number" },
                                    description: "Color [r, g, b] in 0-1 range. E.g. [1, 0, 0] for red, [0.2, 0.5, 1] for sky blue. Default: [0.8, 0.8, 0.8].",
                                },
                                metallic: {
                                    type: "number",
                                    description: "Metallic factor 0-1. 0 = plastic/matte, 1 = metal. Default: 0.",
                                },
                                roughness: {
                                    type: "number",
                                    description: "Roughness factor 0-1. 0 = mirror/glossy, 1 = rough/matte. Default: 0.5.",
                                },
                            },
                            required: ["type"],
                        },
                        description: 'Array of procedural 3D shapes for "geometry" type. Each shape has type, position, scale, color, metallic, roughness. Required for "geometry" type. Example: [{type:"cube",position:[0,0.5,0],scale:[1,1,1],color:[1,0,0]},{type:"sphere",position:[0,1.8,0],scale:[0.6,0.6,0.6],color:[0,0,1]}]',
                    },
                },
                required: ["type"],
            },
        },
        {
            name: "get_platform_setup",
            description: "Returns the complete setup guide for any SceneView-supported platform: Android, iOS, Web, Flutter, React Native, Desktop, or Android TV. Includes dependencies, manifest/permissions, minimum SDK, and a working starter template. Replaces `get_setup`, `get_ios_setup`, and `get_web_setup` with a single unified tool. Use this when a user wants to set up SceneView on any platform.",
            inputSchema: {
                type: "object",
                properties: {
                    platform: {
                        type: "string",
                        enum: PLATFORM_IDS,
                        description: `Target platform:\n${PLATFORM_IDS.map((p) => `- "${p}"`).join("\n")}`,
                    },
                    type: {
                        type: "string",
                        enum: ["3d", "ar"],
                        description: '"3d" for 3D-only scenes. "ar" for augmented reality. Some platforms only support 3D.',
                    },
                },
                required: ["platform", "type"],
            },
        },
        {
            name: "migrate_code",
            description: "Automatically migrates SceneView 2.x Kotlin code to 3.x. Applies known renames (SceneView→Scene, ArSceneView→ARScene), replaces deprecated APIs (loadModelAsync→rememberModelInstance, Engine.create→rememberEngine), fixes LightNode trailing-lambda bug, removes Sceneform imports, and more. Returns the migrated code with a detailed changelog. Use this when a user has 2.x code that needs updating, or when you detect 2.x patterns in their code.",
            inputSchema: {
                type: "object",
                properties: {
                    code: {
                        type: "string",
                        description: "The SceneView 2.x Kotlin code to migrate. Can be a snippet, a function, or a full file.",
                    },
                },
                required: ["code"],
            },
        },
        {
            name: "debug_issue",
            description: 'Returns a targeted debugging guide for a specific SceneView issue. Categories: "model-not-showing" (invisible models), "ar-not-working" (AR camera/planes), "crash" (SIGABRT/native), "performance" (low FPS/memory), "build-error" (Gradle/dependency), "black-screen" (no rendering), "lighting" (dark/bright/shadows), "gestures" (touch/drag), "ios" (Swift/RealityKit). You can provide a category directly, or describe the problem and it will be auto-detected. Use this when a user reports something not working.',
            inputSchema: {
                type: "object",
                properties: {
                    category: {
                        type: "string",
                        enum: DEBUG_CATEGORIES,
                        description: `The issue category. If omitted, provide \`description\` for auto-detection.\n${DEBUG_CATEGORIES.map((c) => `- "${c}"`).join("\n")}`,
                    },
                    description: {
                        type: "string",
                        description: 'Free-text description of the issue (e.g., "my model is not showing", "app crashes on destroy"). Used for auto-detection when category is omitted.',
                    },
                },
                required: [],
            },
        },
        {
            name: "generate_scene",
            description: 'Generates a complete, compilable Scene{} or ARScene{} Kotlin composable from a natural language description. Parses objects (table, chair, sphere, car, etc.), quantities ("two chairs", "3 spheres"), environment (indoor/outdoor/dark), and mode (3D or AR). Returns working Kotlin code with proper engine setup, model loading, lighting, and ground plane. Use this when a user says "build me a scene with..." or describes a 3D scene they want to create.',
            inputSchema: {
                type: "object",
                properties: {
                    description: {
                        type: "string",
                        description: 'Natural language description of the desired 3D scene. Examples: "a room with a table and two chairs", "AR scene with a robot on the floor", "outdoor scene with three trees and a car", "dark room with a sphere and a cube".',
                    },
                },
                required: ["description"],
            },
        },
    ],
}));
// ─── Tool handlers ────────────────────────────────────────────────────────────
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    switch (request.params.name) {
        // ── get_sample ────────────────────────────────────────────────────────────
        case "get_sample": {
            const scenario = request.params.arguments?.scenario;
            const sample = getSample(scenario);
            if (!sample) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `Unknown scenario "${scenario}". Call \`list_samples\` to see available options.`,
                        },
                    ],
                    isError: true,
                };
            }
            const isIos = sample.language === "swift";
            const depBlock = isIos
                ? [
                    `**SPM dependency:**`,
                    `\`\`\`swift`,
                    `.package(url: "${sample.spmDependency ?? sample.dependency}", from: "3.3.0")`,
                    `\`\`\``,
                ]
                : [
                    `**Gradle dependency:**`,
                    `\`\`\`kotlin`,
                    `implementation("${sample.dependency}")`,
                    `\`\`\``,
                ];
            const codeLang = isIos ? "swift" : "kotlin";
            const codeLabel = isIos ? "**Swift (SwiftUI):**" : "**Kotlin (Jetpack Compose):**";
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## ${sample.title}`,
                            ``,
                            `**Tags:** ${sample.tags.join(", ")}`,
                            ``,
                            ...depBlock,
                            ``,
                            codeLabel,
                            `\`\`\`${codeLang}`,
                            sample.code,
                            `\`\`\``,
                            ``,
                            `**Prompt that generates this:**`,
                            `> ${sample.prompt}`,
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── list_samples ──────────────────────────────────────────────────────────
        case "list_samples": {
            const filterTag = request.params.arguments?.tag;
            const entries = Object.values(SAMPLES).filter((s) => !filterTag || s.tags.includes(filterTag));
            if (entries.length === 0) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `No samples found with tag "${filterTag}". Available tags: 3d, ar, model, geometry, animation, camera, environment, anchor, plane-detection, image-tracking, cloud-anchor, point-cloud, placement, gestures, physics, sky, fog, lines, text, reflection, post-processing, ios, swift, video, lighting`,
                        },
                    ],
                };
            }
            const header = filterTag
                ? `## SceneView samples tagged \`${filterTag}\` (${entries.length})\n`
                : `## All SceneView samples (${entries.length})\n`;
            const rows = entries
                .map((s) => {
                const depLabel = s.language === "swift" ? "*SPM:*" : "*Dependency:*";
                return `### \`${s.id}\`\n**${s.title}**${s.language === "swift" ? " (Swift/iOS)" : ""}\n${s.description}\n*Tags:* ${s.tags.join(", ")}\n${depLabel} \`${s.dependency}\`\n\nCall \`get_sample("${s.id}")\` for the full code.`;
            })
                .join("\n\n---\n\n");
            return { content: withDisclaimer([{ type: "text", text: header + rows }]) };
        }
        // ── get_setup ─────────────────────────────────────────────────────────────
        case "get_setup": {
            const type = request.params.arguments?.type;
            if (type === "3d") {
                return {
                    content: withDisclaimer([
                        {
                            type: "text",
                            text: [
                                `## SceneView — 3D setup`,
                                ``,
                                `### build.gradle.kts`,
                                `\`\`\`kotlin`,
                                `dependencies {`,
                                `    implementation("io.github.sceneview:sceneview:3.3.0")`,
                                `}`,
                                `\`\`\``,
                                ``,
                                `No manifest changes required for 3D-only scenes.`,
                            ].join("\n"),
                        },
                    ]),
                };
            }
            if (type === "ar") {
                return {
                    content: withDisclaimer([
                        {
                            type: "text",
                            text: [
                                `## SceneView — AR setup`,
                                ``,
                                `### build.gradle.kts`,
                                `\`\`\`kotlin`,
                                `dependencies {`,
                                `    implementation("io.github.sceneview:arsceneview:3.3.0")`,
                                `}`,
                                `\`\`\``,
                                ``,
                                `### AndroidManifest.xml`,
                                `\`\`\`xml`,
                                `<uses-permission android:name="android.permission.CAMERA" />`,
                                `<uses-feature android:name="android.hardware.camera.ar" android:required="true" />`,
                                `<application>`,
                                `    <meta-data android:name="com.google.ar.core" android:value="required" />`,
                                `</application>`,
                                `\`\`\``,
                            ].join("\n"),
                        },
                    ]),
                };
            }
            return {
                content: [{ type: "text", text: `Unknown type "${type}". Use "3d" or "ar".` }],
                isError: true,
            };
        }
        // ── validate_code ─────────────────────────────────────────────────────────
        case "validate_code": {
            const code = request.params.arguments?.code;
            if (!code || typeof code !== "string") {
                return {
                    content: [{ type: "text", text: "Missing required parameter: `code`" }],
                    isError: true,
                };
            }
            const issues = validateCode(code);
            const report = formatValidationReport(issues);
            return { content: withDisclaimer([{ type: "text", text: report }]) };
        }
        // ── get_migration_guide ───────────────────────────────────────────────────
        case "get_migration_guide": {
            return { content: withDisclaimer([{ type: "text", text: MIGRATION_GUIDE }]) };
        }
        // ── get_node_reference ────────────────────────────────────────────────────
        case "get_node_reference": {
            const nodeType = request.params.arguments?.nodeType;
            if (!nodeType || typeof nodeType !== "string") {
                return {
                    content: [{ type: "text", text: "Missing required parameter: `nodeType`" }],
                    isError: true,
                };
            }
            const section = findNodeSection(NODE_SECTIONS, nodeType);
            if (!section) {
                const available = listNodeTypes(NODE_SECTIONS).join(", ");
                return {
                    content: [
                        {
                            type: "text",
                            text: [
                                `No reference found for node type \`${nodeType}\`.`,
                                ``,
                                `**Available node types:**`,
                                available,
                            ].join("\n"),
                        },
                    ],
                };
            }
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## \`${section.name}\` — API Reference`,
                            ``,
                            section.content,
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── get_platform_roadmap ────────────────────────────────────────────────
        case "get_platform_roadmap": {
            return { content: withDisclaimer([{ type: "text", text: PLATFORM_ROADMAP }]) };
        }
        // ── get_best_practices ───────────────────────────────────────────────────
        case "get_best_practices": {
            const category = request.params.arguments?.category || "all";
            const text = BEST_PRACTICES[category] ?? BEST_PRACTICES["all"];
            return { content: withDisclaimer([{ type: "text", text }]) };
        }
        // ── get_ar_setup ─────────────────────────────────────────────────────────
        case "get_ar_setup": {
            return { content: withDisclaimer([{ type: "text", text: AR_SETUP_GUIDE }]) };
        }
        // ── get_troubleshooting ──────────────────────────────────────────────────
        case "get_troubleshooting": {
            return { content: withDisclaimer([{ type: "text", text: TROUBLESHOOTING_GUIDE }]) };
        }
        // ── get_ios_setup ─────────────────────────────────────────────────────────
        case "get_ios_setup": {
            const iosType = request.params.arguments?.type;
            if (iosType === "3d") {
                return {
                    content: withDisclaimer([
                        {
                            type: "text",
                            text: [
                                `## SceneViewSwift — iOS/macOS/visionOS 3D Setup`,
                                ``,
                                `### 1. Add SPM Dependency`,
                                ``,
                                `In Xcode: **File → Add Package Dependencies** → paste:`,
                                `\`\`\``,
                                `https://github.com/SceneView/sceneview`,
                                `\`\`\``,
                                `Set version rule to **"from: 3.3.0"**.`,
                                ``,
                                `Or in Package.swift:`,
                                `\`\`\`swift`,
                                `// swift-tools-version: 5.10`,
                                `import PackageDescription`,
                                ``,
                                `let package = Package(`,
                                `    name: "MyApp",`,
                                `    platforms: [.iOS(.v17), .macOS(.v14), .visionOS(.v1)],`,
                                `    dependencies: [`,
                                `        .package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")`,
                                `    ],`,
                                `    targets: [`,
                                `        .executableTarget(`,
                                `            name: "MyApp",`,
                                `            dependencies: [`,
                                `                .product(name: "SceneViewSwift", package: "sceneview")`,
                                `            ]`,
                                `        )`,
                                `    ]`,
                                `)`,
                                `\`\`\``,
                                ``,
                                `### 2. Minimum Platform Versions`,
                                ``,
                                `| Platform | Minimum Version |`,
                                `|----------|-----------------|`,
                                `| iOS      | 17.0            |`,
                                `| macOS    | 14.0            |`,
                                `| visionOS | 1.0             |`,
                                ``,
                                `### 3. Basic SwiftUI Integration`,
                                ``,
                                `\`\`\`swift`,
                                `import SwiftUI`,
                                `import SceneViewSwift`,
                                `import RealityKit`,
                                ``,
                                `struct ContentView: View {`,
                                `    @State private var model: ModelNode?`,
                                ``,
                                `    var body: some View {`,
                                `        SceneView { root in`,
                                `            if let model {`,
                                `                root.addChild(model.entity)`,
                                `            }`,
                                `        }`,
                                `        .cameraControls(.orbit)`,
                                `        .task {`,
                                `            model = try? await ModelNode.load("models/car.usdz")`,
                                `            model?.scaleToUnits(1.0)`,
                                `        }`,
                                `    }`,
                                `}`,
                                `\`\`\``,
                                ``,
                                `### 4. Model Formats`,
                                ``,
                                `| Format | Support |`,
                                `|--------|---------|`,
                                `| USDZ   | Native — recommended for iOS |`,
                                `| .reality | Native — RealityKit format |`,
                                `| glTF/GLB | Planned via GLTFKit2 |`,
                                ``,
                                `No manifest or permission changes needed for 3D-only scenes.`,
                            ].join("\n"),
                        },
                    ]),
                };
            }
            if (iosType === "ar") {
                return {
                    content: withDisclaimer([
                        {
                            type: "text",
                            text: [
                                `## SceneViewSwift — iOS AR Setup`,
                                ``,
                                `### 1. Add SPM Dependency`,
                                ``,
                                `\`\`\`swift`,
                                `.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")`,
                                `\`\`\``,
                                ``,
                                `### 2. Minimum Platform`,
                                ``,
                                `AR requires **iOS 17.0+** (ARKit + RealityKit). macOS and visionOS use different AR APIs.`,
                                ``,
                                `### 3. Info.plist — Camera Permission`,
                                ``,
                                `Add to your Info.plist (required for AR camera access):`,
                                `\`\`\`xml`,
                                `<key>NSCameraUsageDescription</key>`,
                                `<string>This app uses the camera for augmented reality.</string>`,
                                `\`\`\``,
                                ``,
                                `### 4. Basic AR Integration`,
                                ``,
                                `\`\`\`swift`,
                                `import SwiftUI`,
                                `import SceneViewSwift`,
                                `import RealityKit`,
                                ``,
                                `struct ARContentView: View {`,
                                `    @State private var model: ModelNode?`,
                                ``,
                                `    var body: some View {`,
                                `        ARSceneView(`,
                                `            planeDetection: .horizontal,`,
                                `            showCoachingOverlay: true,`,
                                `            onTapOnPlane: { position, arView in`,
                                `                guard let model else { return }`,
                                `                let anchor = AnchorNode.world(position: position)`,
                                `                let clone = model.entity.clone(recursive: true)`,
                                `                clone.scale = .init(repeating: 0.3)`,
                                `                anchor.add(clone)`,
                                `                arView.scene.addAnchor(anchor.entity)`,
                                `            }`,
                                `        )`,
                                `        .edgesIgnoringSafeArea(.all)`,
                                `        .task {`,
                                `            model = try? await ModelNode.load("models/robot.usdz")`,
                                `        }`,
                                `    }`,
                                `}`,
                                `\`\`\``,
                                ``,
                                `### 5. AR Configuration Options`,
                                ``,
                                `| Parameter | Options | Default |`,
                                `|-----------|---------|---------|`,
                                `| \`planeDetection\` | \`.none\`, \`.horizontal\`, \`.vertical\`, \`.both\` | \`.horizontal\` |`,
                                `| \`showPlaneOverlay\` | \`true\` / \`false\` | \`true\` |`,
                                `| \`showCoachingOverlay\` | \`true\` / \`false\` | \`true\` |`,
                                `| \`imageTrackingDatabase\` | \`Set<ARReferenceImage>\` | \`nil\` |`,
                                ``,
                                `### 6. Image Tracking`,
                                ``,
                                `\`\`\`swift`,
                                `let images = AugmentedImageNode.createImageDatabase([`,
                                `    AugmentedImageNode.ReferenceImage(`,
                                `        name: "poster",`,
                                `        image: UIImage(named: "poster_ref")!,`,
                                `        physicalWidth: 0.3  // meters`,
                                `    )`,
                                `])`,
                                ``,
                                `ARSceneView(`,
                                `    imageTrackingDatabase: images,`,
                                `    onImageDetected: { name, anchor, arView in`,
                                `        let cube = GeometryNode.cube(size: 0.1, color: .blue)`,
                                `        anchor.add(cube.entity)`,
                                `        arView.scene.addAnchor(anchor.entity)`,
                                `    }`,
                                `)`,
                                `\`\`\``,
                            ].join("\n"),
                        },
                    ]),
                };
            }
            return {
                content: [{ type: "text", text: `Unknown type "${iosType}". Use "3d" or "ar".` }],
                isError: true,
            };
        }
        // ── get_web_setup ────────────────────────────────────────────────────────
        case "get_web_setup": {
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## SceneView Web — Browser 3D Setup`,
                            ``,
                            `SceneView Web uses **Filament.js** — the same rendering engine as Android, compiled to WebAssembly (WebGL2).`,
                            ``,
                            `### 1. Install`,
                            ``,
                            `\`\`\`bash`,
                            `npm install @sceneview/sceneview-web`,
                            `\`\`\``,
                            ``,
                            `Or in a Kotlin/JS Gradle project:`,
                            `\`\`\`kotlin`,
                            `// build.gradle.kts`,
                            `kotlin {`,
                            `    js(IR) { browser(); binaries.executable() }`,
                            `    sourceSets {`,
                            `        jsMain.dependencies {`,
                            `            implementation("@sceneview/sceneview-web")`,
                            `            // or: implementation(project(":sceneview-web"))`,
                            `        }`,
                            `    }`,
                            `}`,
                            `\`\`\``,
                            ``,
                            `### 2. HTML`,
                            ``,
                            `\`\`\`html`,
                            `<canvas id="scene-canvas" style="width:100%;height:100vh"></canvas>`,
                            `<script src="your-app.js"></script>`,
                            `\`\`\``,
                            ``,
                            `### 3. Kotlin/JS Code`,
                            ``,
                            `\`\`\`kotlin`,
                            `import io.github.sceneview.web.SceneView`,
                            `import kotlinx.browser.document`,
                            `import org.w3c.dom.HTMLCanvasElement`,
                            ``,
                            `fun main() {`,
                            `    val canvas = document.getElementById("scene-canvas") as HTMLCanvasElement`,
                            `    canvas.width = canvas.clientWidth`,
                            `    canvas.height = canvas.clientHeight`,
                            ``,
                            `    SceneView.create(`,
                            `        canvas = canvas,`,
                            `        configure = {`,
                            `            camera {`,
                            `                eye(0.0, 1.5, 5.0)`,
                            `                target(0.0, 0.0, 0.0)`,
                            `                fov(45.0)`,
                            `            }`,
                            `            light {`,
                            `                directional()`,
                            `                intensity(100_000.0)`,
                            `            }`,
                            `            model("models/DamagedHelmet.glb")`,
                            `            autoRotate()`,
                            `        },`,
                            `        onReady = { sceneView ->`,
                            `            sceneView.startRendering()`,
                            `        }`,
                            `    )`,
                            `}`,
                            `\`\`\``,
                            ``,
                            `### 4. Features`,
                            ``,
                            `- Same Filament PBR renderer as Android (WASM)`,
                            `- glTF 2.0 / GLB model loading`,
                            `- IBL environment lighting (KTX)`,
                            `- Orbit camera with mouse/touch/pinch controls`,
                            `- Auto-rotation`,
                            `- Directional, point, and spot lights`,
                            ``,
                            `### 5. Limitations`,
                            ``,
                            `- No AR (requires native sensors)`,
                            `- WebGL2 required (~95% of browsers)`,
                            `- glTF/GLB format only (same as Android)`,
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── render_3d_preview ──────────────────────────────────────────────────
        case "render_3d_preview": {
            const modelUrl = request.params.arguments?.modelUrl;
            const codeSnippet = request.params.arguments?.codeSnippet;
            const autoRotate = request.params.arguments?.autoRotate;
            const ar = request.params.arguments?.ar;
            const title = request.params.arguments?.title;
            const validationError = validatePreviewInput(modelUrl, codeSnippet);
            if (validationError) {
                return {
                    content: [{ type: "text", text: `Error: ${validationError}` }],
                    isError: true,
                };
            }
            const result = buildPreviewUrl({ modelUrl, codeSnippet, autoRotate, ar, title });
            const text = formatPreviewResponse(result);
            return { content: withDisclaimer([{ type: "text", text }]) };
        }
        // ── create_3d_artifact ───────────────────────────────────────────────────
        case "create_3d_artifact": {
            const artifactInput = {
                type: request.params.arguments?.type,
                modelUrl: request.params.arguments?.modelUrl,
                title: request.params.arguments?.title,
                data: request.params.arguments?.data,
                options: request.params.arguments?.options,
                hotspots: request.params.arguments?.hotspots,
                shapes: request.params.arguments?.shapes,
            };
            const validationError = validateArtifactInput(artifactInput);
            if (validationError) {
                return {
                    content: [{ type: "text", text: `Error: ${validationError}` }],
                    isError: true,
                };
            }
            const result = generateArtifact(artifactInput);
            const text = formatArtifactResponse(result);
            return { content: withDisclaimer([{ type: "text", text }]) };
        }
        // ── get_platform_setup ─────────────────────────────────────────────────
        case "get_platform_setup": {
            const platform = request.params.arguments?.platform;
            const setupType = request.params.arguments?.type;
            if (!platform || !setupType) {
                return {
                    content: [{ type: "text", text: "Missing required parameters: `platform` and `type`." }],
                    isError: true,
                };
            }
            const guide = getPlatformSetup(platform, setupType);
            return { content: withDisclaimer([{ type: "text", text: guide }]) };
        }
        // ── migrate_code ─────────────────────────────────────────────────────────
        case "migrate_code": {
            const code = request.params.arguments?.code;
            if (!code || typeof code !== "string") {
                return {
                    content: [{ type: "text", text: "Missing required parameter: `code`." }],
                    isError: true,
                };
            }
            const migrationResult = migrateCode(code);
            const migrationReport = formatMigrationResult(migrationResult);
            return { content: withDisclaimer([{ type: "text", text: migrationReport }]) };
        }
        // ── debug_issue ──────────────────────────────────────────────────────────
        case "debug_issue": {
            let category = request.params.arguments?.category;
            const desc = request.params.arguments?.description;
            if (!category && desc) {
                category = autoDetectIssue(desc) ?? undefined;
            }
            if (!category) {
                return {
                    content: [{
                            type: "text",
                            text: `Please provide a \`category\` or \`description\`.\n\nAvailable categories: ${DEBUG_CATEGORIES.join(", ")}`,
                        }],
                    isError: true,
                };
            }
            const debugGuide = getDebugGuide(category);
            const prefix = desc && autoDetectIssue(desc) === category
                ? `> Auto-detected category: **${category}** from your description.\n\n`
                : "";
            return { content: withDisclaimer([{ type: "text", text: prefix + debugGuide }]) };
        }
        // ── generate_scene ───────────────────────────────────────────────────────
        case "generate_scene": {
            const sceneDesc = request.params.arguments?.description;
            if (!sceneDesc || typeof sceneDesc !== "string") {
                return {
                    content: [{ type: "text", text: "Missing required parameter: `description`." }],
                    isError: true,
                };
            }
            const sceneResult = generateScene(sceneDesc);
            const sceneReport = formatGeneratedScene(sceneResult);
            return { content: withDisclaimer([{ type: "text", text: sceneReport }]) };
        }
        default:
            return {
                content: [{ type: "text", text: `Unknown tool: ${request.params.name}` }],
                isError: true,
            };
    }
});
const transport = new StdioServerTransport();
await server.connect(transport);
