/**
 * education-mcp pilot library.
 *
 * Upstream: `education-mcp` (#9 DL/mo, ~566), owned by the
 * `mcp-tools-lab` GitHub org. Curriculum planning, lesson generation,
 * quiz building, vocabulary trainers.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "education__generate_lesson_plan",
    description:
      "Generate a structured lesson plan for a subject, grade level, and duration. Returns objectives, warm-up, activities, assessment, and homework.",
    inputSchema: {
      type: "object",
      properties: {
        subject: { type: "string" },
        gradeLevel: { type: "string", description: "K-2, 3-5, 6-8, 9-12, college." },
        topic: { type: "string" },
        durationMinutes: { type: "number" },
      },
      required: ["subject", "gradeLevel", "topic"],
      additionalProperties: false,
    },
  },
  {
    name: "education__build_quiz",
    description:
      "Build a quiz of N multiple-choice questions on a given topic with answer keys and difficulty estimates.",
    inputSchema: {
      type: "object",
      properties: {
        topic: { type: "string" },
        numQuestions: { type: "number" },
        difficulty: {
          type: "string",
          description: "easy, medium, hard, mixed.",
        },
      },
      required: ["topic", "numQuestions"],
      additionalProperties: false,
    },
  },
  {
    name: "education__curriculum_align",
    description:
      "Align a lesson plan against a named standard (Common Core, IB, Cambridge, French Socle Commun) and report covered / missing strands.",
    inputSchema: {
      type: "object",
      properties: {
        lessonPlanMarkdown: { type: "string" },
        standard: { type: "string" },
      },
      required: ["lessonPlanMarkdown", "standard"],
      additionalProperties: false,
    },
  },
];

export async function dispatchTool(
  toolName: string,
  _args: Record<string, unknown> | undefined,
  _ctx: DispatchContext = {},
): Promise<ToolResult> {
  return {
    content: [
      {
        type: "text",
        text:
          `education-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `See hub-gateway/src/libraries/education.ts for the wiring checklist.`,
      },
    ],
  };
}
