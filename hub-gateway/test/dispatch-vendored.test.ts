/**
 * Dispatch tests for each of the 8 pilot-stub libraries.
 *
 * Verifies that:
 *   1. isError is falsy (tool is found in the registry and handled)
 *   2. The response text contains the expected "pilot stub" marker
 *      (confirms the correct library's dispatcher ran)
 *
 * These libraries are currently pilot stubs — the upstream
 * implementations are not yet vendored into the gateway. When a
 * library graduates to a real handler (like architecture and
 * automotive-3d already have), move it to its own test file and
 * verify the upstream output directly.
 */

import { describe, it, expect, beforeEach } from "vitest";
import app from "../src/index.js";
import { hashApiKey } from "../src/auth/api-keys.js";
import { makeEnv } from "./helpers/fake-bindings.js";

const VALID_KEY = "sv_live_DISPATCHTEST0000000000000000000";
const VALID_HEADER = `Bearer ${VALID_KEY}`;

let FAKE_ENV: ReturnType<typeof makeEnv>["env"];

beforeEach(async () => {
  const hash = await hashApiKey(VALID_KEY);
  const ctx = makeEnv({
    api_keys: [
      {
        id: "key_dispatch0001",
        user_id: "usr_dispatch0001",
        key_hash: hash,
        key_prefix: "sv_live_DISPAT",
        revoked_at: null,
      },
    ],
    users: [{ id: "usr_dispatch0001", email: "dispatch@hub.local", tier: "pro" }],
  });
  FAKE_ENV = ctx.env;
});

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function rpc(id: number, tool: string, args: Record<string, unknown>): Request {
  return new Request("https://hub-mcp.test/mcp", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: VALID_HEADER,
    },
    body: JSON.stringify({
      jsonrpc: "2.0",
      id,
      method: "tools/call",
      params: { name: tool, arguments: args },
    }),
  });
}

interface ToolCallBody {
  id: number;
  result: {
    content: Array<{ type: string; text: string }>;
    isError?: boolean;
  };
}

async function call(
  id: number,
  tool: string,
  args: Record<string, unknown>,
): Promise<{ status: number; body: ToolCallBody }> {
  const res = await app.request(rpc(id, tool, args), {}, FAKE_ENV);
  const body = (await res.json()) as ToolCallBody;
  return { status: res.status, body };
}

function assertStub(
  body: ToolCallBody,
  tool: string,
  stubMarker: string,
): void {
  expect(body.result.isError, `${tool}: isError`).toBeFalsy();
  const text = body.result.content[0].text;
  expect(text, `${tool}: stub marker`).toContain(stubMarker);
}

// ---------------------------------------------------------------------------
// 1. education
// ---------------------------------------------------------------------------

describe("education library dispatch", () => {
  it("education__generate_lesson_plan with required args", async () => {
    const { status, body } = await call(100, "education__generate_lesson_plan", {
      subject: "Math",
      gradeLevel: "6-8",
      topic: "Algebra",
    });
    expect(status).toBe(200);
    assertStub(body, "education__generate_lesson_plan", "education-mcp pilot stub");
  });

  it("education__build_quiz with topic + numQuestions", async () => {
    const { status, body } = await call(101, "education__build_quiz", {
      topic: "photosynthesis",
      numQuestions: 5,
    });
    expect(status).toBe(200);
    assertStub(body, "education__build_quiz", "education-mcp pilot stub");
  });

  it("education__curriculum_align with lessonPlan + standard", async () => {
    const { status, body } = await call(102, "education__curriculum_align", {
      lessonPlanMarkdown: "## Lesson\nObjectives...",
      standard: "Common Core",
    });
    expect(status).toBe(200);
    assertStub(body, "education__curriculum_align", "education-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 2. finance
// ---------------------------------------------------------------------------

describe("finance library dispatch", () => {
  it("finance__market_quote with symbol", async () => {
    const { status, body } = await call(200, "finance__market_quote", {
      symbol: "AAPL",
    });
    expect(status).toBe(200);
    assertStub(body, "finance__market_quote", "finance-mcp pilot stub");
  });

  it("finance__portfolio_summary with holdings", async () => {
    const { status, body } = await call(201, "finance__portfolio_summary", {
      holdings: [{ symbol: "AAPL", quantity: 10, costBasis: 150 }],
    });
    expect(status).toBe(200);
    assertStub(body, "finance__portfolio_summary", "finance-mcp pilot stub");
  });

  it("finance__compound_interest with principal + rate + years", async () => {
    const { status, body } = await call(202, "finance__compound_interest", {
      principal: 10000,
      annualRatePercent: 5,
      years: 10,
    });
    expect(status).toBe(200);
    assertStub(body, "finance__compound_interest", "finance-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 3. realestate
// ---------------------------------------------------------------------------

describe("realestate library dispatch", () => {
  it("realestate__search_listings with location", async () => {
    const { status, body } = await call(300, "realestate__search_listings", {
      location: "Lyon",
    });
    expect(status).toBe(200);
    assertStub(body, "realestate__search_listings", "realestate-mcp pilot stub");
  });

  it("realestate__estimate_value with address", async () => {
    const { status, body } = await call(301, "realestate__estimate_value", {
      address: "10 rue Victor Hugo, Lyon",
    });
    expect(status).toBe(200);
    assertStub(body, "realestate__estimate_value", "realestate-mcp pilot stub");
  });

  it("realestate__staging_assets with roomType", async () => {
    const { status, body } = await call(302, "realestate__staging_assets", {
      roomType: "living_room",
    });
    expect(status).toBe(200);
    assertStub(body, "realestate__staging_assets", "realestate-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 4. french-admin
// ---------------------------------------------------------------------------

describe("french-admin library dispatch", () => {
  it("french_admin__calculate_impots with revenu + parts", async () => {
    const { status, body } = await call(400, "french_admin__calculate_impots", {
      revenuNet: 40000,
      parts: 2,
    });
    expect(status).toBe(200);
    assertStub(body, "french_admin__calculate_impots", "french-admin-mcp pilot stub");
  });

  it("french_admin__caf_eligibility with situation + revenu", async () => {
    const { status, body } = await call(401, "french_admin__caf_eligibility", {
      situation: "salarie",
      revenuMensuel: 2800,
    });
    expect(status).toBe(200);
    assertStub(body, "french_admin__caf_eligibility", "french-admin-mcp pilot stub");
  });

  it("french_admin__search_form with query", async () => {
    const { status, body } = await call(402, "french_admin__search_form", {
      query: "cerfa declaration revenus",
    });
    expect(status).toBe(200);
    assertStub(body, "french_admin__search_form", "french-admin-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 5. ecommerce-3d
// ---------------------------------------------------------------------------

describe("ecommerce-3d library dispatch", () => {
  it("ecommerce3d__search_products with category", async () => {
    const { status, body } = await call(500, "ecommerce3d__search_products", {
      category: "furniture",
    });
    expect(status).toBe(200);
    assertStub(body, "ecommerce3d__search_products", "ecommerce-3d-mcp pilot stub");
  });

  it("ecommerce3d__list_categories with no args", async () => {
    const { status, body } = await call(501, "ecommerce3d__list_categories", {});
    expect(status).toBe(200);
    assertStub(body, "ecommerce3d__list_categories", "ecommerce-3d-mcp pilot stub");
  });

  it("ecommerce3d__configurator_options with sku", async () => {
    const { status, body } = await call(502, "ecommerce3d__configurator_options", {
      sku: "SOFA-001",
    });
    expect(status).toBe(200);
    assertStub(body, "ecommerce3d__configurator_options", "ecommerce-3d-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 6. legal-docs
// ---------------------------------------------------------------------------

describe("legal-docs library dispatch", () => {
  it("legal_docs__list_templates with jurisdiction", async () => {
    const { status, body } = await call(600, "legal_docs__list_templates", {
      jurisdiction: "FR",
    });
    expect(status).toBe(200);
    assertStub(body, "legal_docs__list_templates", "legal-docs-mcp pilot stub");
  });

  it("legal_docs__generate_clause with clauseType", async () => {
    const { status, body } = await call(601, "legal_docs__generate_clause", {
      clauseType: "confidentiality",
    });
    expect(status).toBe(200);
    assertStub(body, "legal_docs__generate_clause", "legal-docs-mcp pilot stub");
  });

  it("legal_docs__review_nda with draftMarkdown", async () => {
    const { status, body } = await call(602, "legal_docs__review_nda", {
      draftMarkdown: "## NDA\nThis agreement...",
    });
    expect(status).toBe(200);
    assertStub(body, "legal_docs__review_nda", "legal-docs-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 7. social-media
// ---------------------------------------------------------------------------

describe("social-media library dispatch", () => {
  it("social_media__suggest_hashtags with platform + topic", async () => {
    const { status, body } = await call(700, "social_media__suggest_hashtags", {
      platform: "instagram",
      topic: "machine learning",
    });
    expect(status).toBe(200);
    assertStub(body, "social_media__suggest_hashtags", "social-media-mcp pilot stub");
  });

  it("social_media__caption_variants with platform + brief", async () => {
    const { status, body } = await call(701, "social_media__caption_variants", {
      platform: "twitter",
      brief: "Just shipped v4!",
    });
    expect(status).toBe(200);
    assertStub(body, "social_media__caption_variants", "social-media-mcp pilot stub");
  });

  it("social_media__plan_content_calendar with niche + platforms", async () => {
    const { status, body } = await call(702, "social_media__plan_content_calendar", {
      niche: "AI in healthcare",
      platforms: ["instagram", "linkedin"],
    });
    expect(status).toBe(200);
    assertStub(body, "social_media__plan_content_calendar", "social-media-mcp pilot stub");
  });
});

// ---------------------------------------------------------------------------
// 8. health-fitness
// ---------------------------------------------------------------------------

describe("health-fitness library dispatch", () => {
  it("health_fitness__workout_plan with goal + experience", async () => {
    const { status, body } = await call(800, "health_fitness__workout_plan", {
      goal: "strength",
      experience: "intermediate",
    });
    expect(status).toBe(200);
    assertStub(body, "health_fitness__workout_plan", "health-fitness-mcp pilot stub");
  });

  it("health_fitness__macro_calculator with full args", async () => {
    const { status, body } = await call(801, "health_fitness__macro_calculator", {
      heightCm: 180,
      weightKg: 75,
      age: 30,
      biologicalSex: "male",
      activityLevel: "moderate",
      goal: "maintain",
    });
    expect(status).toBe(200);
    assertStub(body, "health_fitness__macro_calculator", "health-fitness-mcp pilot stub");
  });

  it("health_fitness__exercise_form_cues with exercise", async () => {
    const { status, body } = await call(802, "health_fitness__exercise_form_cues", {
      exercise: "squat",
    });
    expect(status).toBe(200);
    assertStub(body, "health_fitness__exercise_form_cues", "health-fitness-mcp pilot stub");
  });
});
