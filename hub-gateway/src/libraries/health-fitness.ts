/**
 * health-fitness-mcp pilot library.
 *
 * Upstream: `health-fitness-mcp` (#11 DL/mo, ~335), owned by the
 * `thomasgorisse` GitHub org. Upstream repo is currently a 404 —
 * package.json on npm points at a non-existent GitHub URL. Repo
 * creation is tracked in .claude/SESSION_PROMPT.md.
 *
 * IMPORTANT: health-fitness-mcp is INFORMATIONAL ONLY and NEVER
 * provides medical advice or prescriptions. The dispatcher must
 * enforce this once real handlers ship.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "health_fitness__workout_plan",
    description:
      "Generate a weekly workout plan for a goal (strength, hypertrophy, endurance, fat_loss) given available equipment and experience level. Informational, not medical advice.",
    inputSchema: {
      type: "object",
      properties: {
        goal: { type: "string" },
        experience: { type: "string", description: "beginner, intermediate, advanced." },
        daysPerWeek: { type: "number" },
        equipment: {
          type: "array",
          description: "bodyweight, dumbbells, barbell, machines, kettlebells, bands.",
        },
      },
      required: ["goal", "experience"],
      additionalProperties: false,
    },
  },
  {
    name: "health_fitness__macro_calculator",
    description:
      "Estimate daily macros (protein, carbs, fat) from body stats, activity level, and a goal. Uses the Mifflin–St Jeor formula. Informational, not a nutrition prescription.",
    inputSchema: {
      type: "object",
      properties: {
        heightCm: { type: "number" },
        weightKg: { type: "number" },
        age: { type: "number" },
        biologicalSex: { type: "string", description: "male, female." },
        activityLevel: {
          type: "string",
          description: "sedentary, light, moderate, active, very_active.",
        },
        goal: { type: "string", description: "cut, maintain, bulk." },
      },
      required: ["heightCm", "weightKg", "age", "biologicalSex", "activityLevel", "goal"],
      additionalProperties: false,
    },
  },
  {
    name: "health_fitness__exercise_form_cues",
    description:
      "Return form cues and common mistakes for an exercise by name (squat, deadlift, bench_press, pull_up, running).",
    inputSchema: {
      type: "object",
      properties: {
        exercise: { type: "string" },
      },
      required: ["exercise"],
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
          `health-fitness-mcp pilot stub: ${toolName} is registered on the ` +
          `hub gateway but the upstream repository is currently missing ` +
          `(404). Informational only — never medical advice. See ` +
          `hub-gateway/src/libraries/health-fitness.ts for the wiring checklist.`,
      },
    ],
  };
}
