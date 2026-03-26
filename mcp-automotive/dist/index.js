#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import { generateCarConfigurator, CAR_BODY_STYLES, CAMERA_PRESETS, } from "./car-configurator.js";
import { generateHudOverlay, HUD_ELEMENTS, HUD_STYLES, } from "./hud-overlay.js";
import { generateDashboard3d, GAUGE_TYPES, DASHBOARD_THEMES, } from "./dashboard-3d.js";
import { generateArShowroom, SHOWROOM_LOCATIONS, SHOWROOM_FEATURES, } from "./ar-showroom.js";
import { generatePartsCatalog, PART_CATEGORIES, CATALOG_FEATURES, } from "./parts-catalog.js";
import { listCarModels, formatCarModelList, CAR_MODEL_CATEGORIES, } from "./car-models.js";
import { validateAutomotiveCode, formatValidationReport, } from "./validator.js";
// ─── Legal disclaimer ─────────────────────────────────────────────────────────
const DISCLAIMER = "\n\n---\n*Generated code suggestion for automotive 3D visualization. Review before production use. See [LICENSE](https://github.com/sceneview/sceneview/blob/main/mcp-automotive/LICENSE).*";
function withDisclaimer(content) {
    if (content.length === 0)
        return content;
    const last = content[content.length - 1];
    return [
        ...content.slice(0, -1),
        { ...last, text: last.text + DISCLAIMER },
    ];
}
// ─── Server ───────────────────────────────────────────────────────────────────
const server = new Server({ name: "automotive-3d-mcp", version: "1.0.0" }, { capabilities: { tools: {} } });
// ─── Tools ────────────────────────────────────────────────────────────────────
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "get_car_configurator",
            description: "Returns a complete, compilable Kotlin composable for a 3D car configurator using SceneView. Supports 10 body styles (sedan, SUV, coupe, sports, electric, etc.). Features include color picker with 8 paint options, material variants (metallic, matte, pearlescent, gloss), camera presets (exterior, interior, detail views), auto-rotation turntable, and AR mode for placing the car in your driveway. Uses KHR_materials_variants for paint switching and three-point studio lighting.",
            inputSchema: {
                type: "object",
                properties: {
                    bodyStyle: {
                        type: "string",
                        enum: CAR_BODY_STYLES,
                        description: `Car body style:\n${CAR_BODY_STYLES.map((s) => `- "${s}"`).join("\n")}`,
                    },
                    colorPicker: {
                        type: "boolean",
                        description: "Include color picker with 8 paint options (default: true).",
                    },
                    materialVariants: {
                        type: "boolean",
                        description: "Include material variant selector — metallic, matte, pearlescent, gloss (default: true).",
                    },
                    cameraPresets: {
                        type: "array",
                        items: {
                            type: "string",
                            enum: CAMERA_PRESETS,
                        },
                        description: `Camera presets to include (default: exterior-front, exterior-three-quarter, interior-driver):\n${CAMERA_PRESETS.map((p) => `- "${p}"`).join("\n")}`,
                    },
                    turntable: {
                        type: "boolean",
                        description: "Enable auto-rotation turntable (default: true).",
                    },
                    ar: {
                        type: "boolean",
                        description: "Generate AR version using ARScene — place car in real world (default: false). Requires arsceneview dependency.",
                    },
                },
                required: ["bodyStyle"],
            },
        },
        {
            name: "get_hud_overlay",
            description: "Returns a complete, compilable Kotlin composable for a heads-up display overlay using SceneView ViewNode. HUD elements include speedometer, navigation arrows, alerts, fuel gauge, temperature, gear indicator, turn signals, and lane assist. Choose from 6 styles (minimal, sport, luxury, combat, eco, retro). Supports night mode (green-on-black), metric/imperial units, and AR mode for projecting HUD elements in real space.",
            inputSchema: {
                type: "object",
                properties: {
                    elements: {
                        type: "array",
                        items: {
                            type: "string",
                            enum: HUD_ELEMENTS,
                        },
                        description: `HUD elements to display:\n${HUD_ELEMENTS.map((e) => `- "${e}"`).join("\n")}`,
                    },
                    style: {
                        type: "string",
                        enum: HUD_STYLES,
                        description: `HUD visual style (default: "minimal"):\n${HUD_STYLES.map((s) => `- "${s}"`).join("\n")}`,
                    },
                    nightMode: {
                        type: "boolean",
                        description: "Enable night mode — green-on-black color scheme (default: false).",
                    },
                    units: {
                        type: "string",
                        enum: ["metric", "imperial"],
                        description: 'Unit system — "metric" (km/h, °C) or "imperial" (mph, °F). Default: "metric".',
                    },
                    ar: {
                        type: "boolean",
                        description: "Generate AR version with HUD projected in real space (default: false).",
                    },
                },
                required: ["elements"],
            },
        },
        {
            name: "get_dashboard_3d",
            description: "Returns a complete, compilable Kotlin composable for a 3D instrument cluster using SceneView. Gauge types: speedometer, tachometer, fuel, temperature, oil-pressure, battery, boost, odometer. Themes: classic, digital, sport, luxury, electric, retro. The dashboard housing is a 3D model while gauge faces use ViewNode for crisp animated rendering. Includes spring-damped needle animations, red zone indicators, and interactive slider controls.",
            inputSchema: {
                type: "object",
                properties: {
                    gauges: {
                        type: "array",
                        items: {
                            type: "string",
                            enum: GAUGE_TYPES,
                        },
                        description: `Gauges to include:\n${GAUGE_TYPES.map((g) => `- "${g}"`).join("\n")}`,
                    },
                    theme: {
                        type: "string",
                        enum: DASHBOARD_THEMES,
                        description: `Dashboard theme (default: "classic"):\n${DASHBOARD_THEMES.map((t) => `- "${t}"`).join("\n")}`,
                    },
                    animated: {
                        type: "boolean",
                        description: "Enable spring-damped gauge animations (default: true).",
                    },
                    interactive: {
                        type: "boolean",
                        description: "Include interactive slider controls for demo input (default: true).",
                    },
                    ar: {
                        type: "boolean",
                        description: "Generate AR version (default: false).",
                    },
                },
                required: ["gauges"],
            },
        },
        {
            name: "get_ar_showroom",
            description: "Returns a complete, compilable Kotlin composable for an AR car showroom using SceneView ARScene. Place a full-size car in your driveway, parking lot, garage, showroom floor, or street. Features include walk-around, open doors animation, color swap, real-world measurements, side-by-side comparison, photo capture, and night lighting effects. Real 1:1 scale placement with ground-plane shadows.",
            inputSchema: {
                type: "object",
                properties: {
                    location: {
                        type: "string",
                        enum: SHOWROOM_LOCATIONS,
                        description: `Placement context:\n${SHOWROOM_LOCATIONS.map((l) => `- "${l}"`).join("\n")}`,
                    },
                    features: {
                        type: "array",
                        items: {
                            type: "string",
                            enum: SHOWROOM_FEATURES,
                        },
                        description: `Showroom features (default: walk-around, color-swap):\n${SHOWROOM_FEATURES.map((f) => `- "${f}"`).join("\n")}`,
                    },
                    realScale: {
                        type: "boolean",
                        description: "Place car at real 1:1 scale (~4.5m) or table-top (~2m). Default: true (real scale).",
                    },
                    shadows: {
                        type: "boolean",
                        description: "Enable ground-plane shadow lighting (default: true).",
                    },
                },
                required: ["location"],
            },
        },
        {
            name: "get_parts_catalog",
            description: "Returns a complete, compilable Kotlin composable for a 3D parts catalog explorer using SceneView. Categories: engine, transmission, suspension, brakes, exhaust, interior, body, electrical, wheels, cooling. Features include exploded-view slider, part selection highlighting, detail zoom, cross-section plane, assembly animation, part info cards, and search. Each category comes with realistic part data (names, part numbers, pricing). AR mode for hands-on part examination.",
            inputSchema: {
                type: "object",
                properties: {
                    category: {
                        type: "string",
                        enum: PART_CATEGORIES,
                        description: `Part category:\n${PART_CATEGORIES.map((c) => `- "${c}"`).join("\n")}`,
                    },
                    features: {
                        type: "array",
                        items: {
                            type: "string",
                            enum: CATALOG_FEATURES,
                        },
                        description: `Catalog features (default: exploded-view, part-selection, detail-zoom):\n${CATALOG_FEATURES.map((f) => `- "${f}"`).join("\n")}`,
                    },
                    partNumbers: {
                        type: "boolean",
                        description: "Show manufacturer part numbers (default: true).",
                    },
                    pricing: {
                        type: "boolean",
                        description: "Show part pricing (default: false).",
                    },
                    ar: {
                        type: "boolean",
                        description: "Generate AR version for hands-on examination (default: false).",
                    },
                },
                required: ["category"],
            },
        },
        {
            name: "list_car_models",
            description: "Lists free, openly-licensed 3D car models suitable for SceneView apps. Includes the Khronos ToyCar (CC0, GLB ready), Sketchfab free cars (sedans, SUVs, sports, classics, EVs), engine assemblies, brake calipers, suspension kits, interior cockpits, steering wheels, and dashboard panels. Categories: complete-car, concept, classic, parts, interior, wheels, engine, test-model. Each entry includes source URL, format, license, and SceneView compatibility notes.",
            inputSchema: {
                type: "object",
                properties: {
                    category: {
                        type: "string",
                        enum: CAR_MODEL_CATEGORIES,
                        description: `Filter by category:\n${CAR_MODEL_CATEGORIES.map((c) => `- "${c}"`).join("\n")}`,
                    },
                    tag: {
                        type: "string",
                        description: 'Filter by tag (e.g., "pbr", "engine", "low-poly", "sedan"). Omit to list all.',
                    },
                },
                required: [],
            },
        },
        {
            name: "validate_automotive_code",
            description: "Validates a Kotlin SceneView snippet for common automotive-app mistakes. Checks threading violations (Filament JNI on background thread), null-safety for model loading, LightNode trailing-lambda bug, deprecated 2.x APIs, automotive-specific issues (unrealistic scale, unsupported formats like FBX/.blend, turntable performance), and missing imports. Always call this before presenting generated automotive SceneView code.",
            inputSchema: {
                type: "object",
                properties: {
                    code: {
                        type: "string",
                        description: "The Kotlin source code to validate.",
                    },
                },
                required: ["code"],
            },
        },
    ],
}));
// ─── Tool handlers ────────────────────────────────────────────────────────────
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    switch (request.params.name) {
        // ── get_car_configurator ──────────────────────────────────────────────
        case "get_car_configurator": {
            const args = request.params.arguments ?? {};
            const bodyStyle = args.bodyStyle;
            if (!bodyStyle || !CAR_BODY_STYLES.includes(bodyStyle)) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `Invalid bodyStyle "${bodyStyle}". Valid: ${CAR_BODY_STYLES.join(", ")}`,
                        },
                    ],
                    isError: true,
                };
            }
            const code = generateCarConfigurator({
                bodyStyle,
                colorPicker: args.colorPicker ?? true,
                materialVariants: args.materialVariants ?? true,
                cameraPresets: args.cameraPresets ?? ["exterior-front", "exterior-three-quarter", "interior-driver"],
                turntable: args.turntable ?? true,
                ar: args.ar ?? false,
            });
            const mode = args.ar ? "AR" : "3D";
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## ${mode} Car Configurator — ${bodyStyle}`,
                            ``,
                            `**Gradle dependency:**`,
                            "```kotlin",
                            args.ar
                                ? `implementation("io.github.sceneview:arsceneview:3.3.0")`
                                : `implementation("io.github.sceneview:sceneview:3.3.0")`,
                            "```",
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            "```kotlin",
                            code,
                            "```",
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── get_hud_overlay ───────────────────────────────────────────────────
        case "get_hud_overlay": {
            const args = request.params.arguments ?? {};
            const elements = args.elements;
            if (!elements || !Array.isArray(elements) || elements.length === 0) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `At least one HUD element is required. Valid: ${HUD_ELEMENTS.join(", ")}`,
                        },
                    ],
                    isError: true,
                };
            }
            const code = generateHudOverlay({
                elements,
                style: args.style ?? "minimal",
                nightMode: args.nightMode ?? false,
                units: args.units ?? "metric",
                ar: args.ar ?? false,
            });
            const mode = args.ar ? "AR" : "3D";
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## ${mode} HUD Overlay — ${elements.join(", ")}`,
                            ``,
                            `**Gradle dependency:**`,
                            "```kotlin",
                            args.ar
                                ? `implementation("io.github.sceneview:arsceneview:3.3.0")`
                                : `implementation("io.github.sceneview:sceneview:3.3.0")`,
                            "```",
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            "```kotlin",
                            code,
                            "```",
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── get_dashboard_3d ──────────────────────────────────────────────────
        case "get_dashboard_3d": {
            const args = request.params.arguments ?? {};
            const gauges = args.gauges;
            if (!gauges || !Array.isArray(gauges) || gauges.length === 0) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `At least one gauge type is required. Valid: ${GAUGE_TYPES.join(", ")}`,
                        },
                    ],
                    isError: true,
                };
            }
            const code = generateDashboard3d({
                gauges,
                theme: args.theme ?? "classic",
                animated: args.animated ?? true,
                interactive: args.interactive ?? true,
                ar: args.ar ?? false,
            });
            const mode = args.ar ? "AR" : "3D";
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## ${mode} Dashboard — ${gauges.join(", ")}`,
                            ``,
                            `**Gradle dependency:**`,
                            "```kotlin",
                            args.ar
                                ? `implementation("io.github.sceneview:arsceneview:3.3.0")`
                                : `implementation("io.github.sceneview:sceneview:3.3.0")`,
                            "```",
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            "```kotlin",
                            code,
                            "```",
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── get_ar_showroom ───────────────────────────────────────────────────
        case "get_ar_showroom": {
            const args = request.params.arguments ?? {};
            const location = args.location;
            if (!location || !SHOWROOM_LOCATIONS.includes(location)) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `Invalid location "${location}". Valid: ${SHOWROOM_LOCATIONS.join(", ")}`,
                        },
                    ],
                    isError: true,
                };
            }
            const code = generateArShowroom({
                location,
                features: args.features ?? ["walk-around", "color-swap"],
                realScale: args.realScale ?? true,
                shadows: args.shadows ?? true,
            });
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## AR Showroom — ${location}`,
                            ``,
                            `**Gradle dependency:**`,
                            "```kotlin",
                            `implementation("io.github.sceneview:arsceneview:3.3.0")`,
                            "```",
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            "```kotlin",
                            code,
                            "```",
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── get_parts_catalog ─────────────────────────────────────────────────
        case "get_parts_catalog": {
            const args = request.params.arguments ?? {};
            const category = args.category;
            if (!category || !PART_CATEGORIES.includes(category)) {
                return {
                    content: [
                        {
                            type: "text",
                            text: `Invalid category "${category}". Valid: ${PART_CATEGORIES.join(", ")}`,
                        },
                    ],
                    isError: true,
                };
            }
            const code = generatePartsCatalog({
                category,
                features: args.features ?? ["exploded-view", "part-selection", "detail-zoom"],
                partNumbers: args.partNumbers ?? true,
                pricing: args.pricing ?? false,
                ar: args.ar ?? false,
            });
            const mode = args.ar ? "AR" : "3D";
            return {
                content: withDisclaimer([
                    {
                        type: "text",
                        text: [
                            `## ${mode} Parts Catalog — ${category}`,
                            ``,
                            `**Gradle dependency:**`,
                            "```kotlin",
                            args.ar
                                ? `implementation("io.github.sceneview:arsceneview:3.3.0")`
                                : `implementation("io.github.sceneview:sceneview:3.3.0")`,
                            "```",
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            "```kotlin",
                            code,
                            "```",
                        ].join("\n"),
                    },
                ]),
            };
        }
        // ── list_car_models ───────────────────────────────────────────────────
        case "list_car_models": {
            const args = request.params.arguments ?? {};
            const category = args.category;
            const tag = args.tag;
            const models = listCarModels(category, tag);
            const text = formatCarModelList(models);
            return {
                content: withDisclaimer([{ type: "text", text }]),
            };
        }
        // ── validate_automotive_code ──────────────────────────────────────────
        case "validate_automotive_code": {
            const code = request.params.arguments?.code;
            if (!code) {
                return {
                    content: [{ type: "text", text: "No code provided for validation." }],
                    isError: true,
                };
            }
            const result = validateAutomotiveCode(code);
            const report = formatValidationReport(result);
            return {
                content: [{ type: "text", text: report }],
            };
        }
        default:
            return {
                content: [
                    {
                        type: "text",
                        text: `Unknown tool: "${request.params.name}". Available: get_car_configurator, get_hud_overlay, get_dashboard_3d, get_ar_showroom, get_parts_catalog, list_car_models, validate_automotive_code`,
                    },
                ],
                isError: true,
            };
    }
});
// ─── Start ────────────────────────────────────────────────────────────────────
async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
}
main().catch((err) => {
    console.error("Fatal:", err);
    process.exit(1);
});
