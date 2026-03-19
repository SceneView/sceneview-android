#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { getSample, SAMPLE_IDS, SAMPLES } from "./samples.js";
import { validateCode, formatValidationReport } from "./validator.js";
import { MIGRATION_GUIDE } from "./migration.js";
import { fetchKnownIssues } from "./issues.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

let API_DOCS: string;
try {
  API_DOCS = readFileSync(resolve(__dirname, "../llms.txt"), "utf-8");
} catch {
  API_DOCS = "SceneView API docs not found. Run `npm run prepare` to bundle llms.txt.";
}

const server = new Server(
  { name: "@sceneview/mcp", version: "3.1.1" },
  { capabilities: { resources: {}, tools: {} } }
);

// ─── Resources ───────────────────────────────────────────────────────────────

server.setRequestHandler(ListResourcesRequestSchema, async () => ({
  resources: [
    {
      uri: "sceneview://api",
      name: "SceneView API Reference",
      description:
        "Complete SceneView 3.1.1 API — Scene, ARScene, SceneScope DSL, ARSceneScope DSL, node types, resource loading, camera, gestures, math types, threading rules, and common patterns. Read this before writing any SceneView code.",
      mimeType: "text/markdown",
    },
    {
      uri: "sceneview://known-issues",
      name: "SceneView Open GitHub Issues",
      description:
        "Live list of open issues from the SceneView GitHub repository. Check this before reporting a bug or when something isn't working — there may already be a known workaround.",
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
      description:
        "Returns a complete, compilable Kotlin sample for a given SceneView scenario. Use this to get a working starting point before customising. Call `list_samples` first if you are unsure which scenario fits.",
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
      description:
        "Lists all available SceneView code samples with their IDs, descriptions, and tags. Use this to find the right sample before calling `get_sample`, or to show the user what SceneView can do.",
      inputSchema: {
        type: "object",
        properties: {
          tag: {
            type: "string",
            description:
              "Optional tag to filter by (e.g. \"ar\", \"3d\", \"anchor\", \"face-tracking\", \"geometry\", \"animation\"). Omit to list all samples.",
          },
        },
        required: [],
      },
    },
    {
      name: "get_setup",
      description:
        "Returns the Gradle dependency and AndroidManifest snippet required to use SceneView in an Android project.",
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
      description:
        "Checks a Kotlin SceneView snippet for common mistakes: threading violations, wrong destroy order, missing null-checks on rememberModelInstance, LightNode trailing-lambda bug, deprecated 2.x APIs, and more. Always call this before presenting generated SceneView code to the user.",
      inputSchema: {
        type: "object",
        properties: {
          code: {
            type: "string",
            description: "The Kotlin source code to validate (composable function, class, or file).",
          },
        },
        required: ["code"],
      },
    },
    {
      name: "get_migration_guide",
      description:
        "Returns the full SceneView 2.x → 3.0 migration guide. Use this when a user reports code that worked in 2.x but breaks in 3.0, or when helping someone upgrade.",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
  ],
}));

// ─── Tool handlers ────────────────────────────────────────────────────────────

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  switch (request.params.name) {
    // ── get_sample ────────────────────────────────────────────────────────────
    case "get_sample": {
      const scenario = request.params.arguments?.scenario as string;
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
      return {
        content: [
          {
            type: "text",
            text: [
              `## ${sample.title}`,
              ``,
              `**Tags:** ${sample.tags.join(", ")}`,
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

    // ── list_samples ──────────────────────────────────────────────────────────
    case "list_samples": {
      const filterTag = request.params.arguments?.tag as string | undefined;
      const entries = Object.values(SAMPLES).filter(
        (s) => !filterTag || s.tags.includes(filterTag as any)
      );

      if (entries.length === 0) {
        return {
          content: [
            {
              type: "text",
              text: `No samples found with tag "${filterTag}". Available tags: 3d, ar, model, geometry, animation, camera, environment, anchor, plane-detection, image-tracking, face-tracking, placement, gestures`,
            },
          ],
        };
      }

      const header = filterTag
        ? `## SceneView samples tagged \`${filterTag}\` (${entries.length})\n`
        : `## All SceneView samples (${entries.length})\n`;

      const rows = entries
        .map(
          (s) =>
            `### \`${s.id}\`\n**${s.title}**\n${s.description}\n*Tags:* ${s.tags.join(", ")}\n*Dependency:* \`${s.dependency}\`\n\nCall \`get_sample("${s.id}")\` for the full code.`
        )
        .join("\n\n---\n\n");

      return { content: [{ type: "text", text: header + rows }] };
    }

    // ── get_setup ─────────────────────────────────────────────────────────────
    case "get_setup": {
      const type = request.params.arguments?.type as "3d" | "ar";
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
                `    implementation("io.github.sceneview:sceneview:3.1.1")`,
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
                `    implementation("io.github.sceneview:arsceneview:3.1.1")`,
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

    // ── validate_code ─────────────────────────────────────────────────────────
    case "validate_code": {
      const code = request.params.arguments?.code as string;
      if (!code || typeof code !== "string") {
        return {
          content: [{ type: "text", text: "Missing required parameter: `code`" }],
          isError: true,
        };
      }
      const issues = validateCode(code);
      const report = formatValidationReport(issues);
      return { content: [{ type: "text", text: report }] };
    }

    // ── get_migration_guide ───────────────────────────────────────────────────
    case "get_migration_guide": {
      return { content: [{ type: "text", text: MIGRATION_GUIDE }] };
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
