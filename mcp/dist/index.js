#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListResourcesRequestSchema, ListToolsRequestSchema, ReadResourceRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { getSample, SAMPLE_IDS, SAMPLES } from "./samples.js";
const __dirname = dirname(fileURLToPath(import.meta.url));
let API_DOCS;
try {
    API_DOCS = readFileSync(resolve(__dirname, "../llms.txt"), "utf-8");
}
catch {
    API_DOCS = "SceneView API docs not found. Run `npm run prepare` to bundle llms.txt.";
}
const server = new Server({ name: "@sceneview/mcp", version: "3.0.1" }, { capabilities: { resources: {}, tools: {} } });
server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: [
        {
            uri: "sceneview://api",
            name: "SceneView API Reference",
            description: "Complete SceneView 3.0.0 API — Scene, ARScene, SceneScope DSL, ARSceneScope DSL, node types, resource loading, camera, gestures, math types, threading rules, and common patterns. Read this before writing any SceneView code.",
            mimeType: "text/markdown",
        },
    ],
}));
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    if (request.params.uri === "sceneview://api") {
        return {
            contents: [{ uri: "sceneview://api", mimeType: "text/markdown", text: API_DOCS }],
        };
    }
    throw new Error(`Unknown resource: ${request.params.uri}`);
});
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: "get_sample",
            description: "Returns a complete, compilable Kotlin sample for a given SceneView scenario. Use this to get a working starting point before customising.",
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
    ],
}));
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    switch (request.params.name) {
        case "get_sample": {
            const scenario = request.params.arguments?.scenario;
            const sample = getSample(scenario);
            if (!sample) {
                return {
                    content: [{ type: "text", text: `Unknown scenario "${scenario}". Available: ${SAMPLE_IDS.join(", ")}` }],
                    isError: true,
                };
            }
            return {
                content: [
                    {
                        type: "text",
                        text: [
                            `## ${sample.title}`,
                            ``,
                            `**Gradle dependency:**`,
                            `\`\`\`kotlin`,
                            `implementation("${sample.dependency}")`,
                            `\`\`\``,
                            ``,
                            `**Kotlin (Jetpack Compose):**`,
                            `\`\`\`kotlin`,
                            sample.code,
                            `\`\`\``,
                            ``,
                            `**Prompt that generates this:**`,
                            `> ${sample.prompt}`,
                        ].join("\n"),
                    },
                ],
            };
        }
        case "get_setup": {
            const type = request.params.arguments?.type;
            if (type === "3d") {
                return {
                    content: [
                        {
                            type: "text",
                            text: [
                                `## SceneView — 3D setup`,
                                ``,
                                `### build.gradle.kts`,
                                `\`\`\`kotlin`,
                                `dependencies {`,
                                `    implementation("io.github.sceneview:sceneview:3.0.0")`,
                                `}`,
                                `\`\`\``,
                                ``,
                                `No manifest changes required for 3D-only scenes.`,
                            ].join("\n"),
                        },
                    ],
                };
            }
            if (type === "ar") {
                return {
                    content: [
                        {
                            type: "text",
                            text: [
                                `## SceneView — AR setup`,
                                ``,
                                `### build.gradle.kts`,
                                `\`\`\`kotlin`,
                                `dependencies {`,
                                `    implementation("io.github.sceneview:arsceneview:3.0.0")`,
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
                    ],
                };
            }
            return {
                content: [{ type: "text", text: `Unknown type "${type}". Use "3d" or "ar".` }],
                isError: true,
            };
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
