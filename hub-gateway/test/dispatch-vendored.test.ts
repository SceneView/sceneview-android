/**
 * Deep dispatch tests for each of the 8 vendored libraries.
 *
 * Verifies that:
 *   1. isError is falsy (real handler ran without throwing)
 *   2. The response text does NOT contain "pilot stub"
 *   3. The response text contains the library's disclaimer footer
 *
 * Each library gets 2-3 tools exercised with real arguments that the
 * upstream functions accept.
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

function assertVendored(
  body: ToolCallBody,
  tool: string,
  disclaimer: string,
): void {
  expect(body.result.isError, `${tool}: isError`).toBeFalsy();
  const text = body.result.content[0].text;
  expect(text, `${tool}: must not be stub`).not.toContain("pilot stub");
  expect(text, `${tool}: disclaimer`).toContain(disclaimer);
}

// ---------------------------------------------------------------------------
// 1. education
// ---------------------------------------------------------------------------

describe("education library dispatch", () => {
  it("education__generate_quiz with topic + numQuestions", async () => {
    const { status, body } = await call(100, "education__generate_quiz", {
      topic: "Math",
      numQuestions: 5,
    });
    expect(status).toBe(200);
    assertVendored(body, "education__generate_quiz", "Not a substitute for professional instruction");
  });

  it("education__explain_concept with concept arg", async () => {
    const { status, body } = await call(101, "education__explain_concept", {
      concept: "photosynthesis",
    });
    expect(status).toBe(200);
    assertVendored(body, "education__explain_concept", "Not a substitute for professional instruction");
  });

  it("education__generate_flashcards with topic + count", async () => {
    const { status, body } = await call(102, "education__generate_flashcards", {
      topic: "World War II",
      count: 8,
    });
    expect(status).toBe(200);
    assertVendored(body, "education__generate_flashcards", "Not a substitute for professional instruction");
  });
});

// ---------------------------------------------------------------------------
// 2. finance
// ---------------------------------------------------------------------------

describe("finance library dispatch", () => {
  it("finance__simulate_loan with all required args", async () => {
    const { status, body } = await call(200, "finance__simulate_loan", {
      type: "fixed",
      principal: 200000,
      annualRate: 4.0,
      termYears: 25,
    });
    expect(status).toBe(200);
    assertVendored(body, "finance__simulate_loan", "Not financial or investment advice");
  });

  it("finance__inflation_calculator with amount + years", async () => {
    const { status, body } = await call(201, "finance__inflation_calculator", {
      amount: 1000,
      years: 10,
    });
    expect(status).toBe(200);
    assertVendored(body, "finance__inflation_calculator", "Not financial or investment advice");
  });

  it("finance__calculate_budget with income + expenses", async () => {
    const { status, body } = await call(202, "finance__calculate_budget", {
      monthlyIncome: 3500,
      expenses: [
        { category: "rent", amount: 900 },
        { category: "food", amount: 400 },
      ],
    });
    expect(status).toBe(200);
    assertVendored(body, "finance__calculate_budget", "Not financial or investment advice");
  });
});

// ---------------------------------------------------------------------------
// 3. realestate
// ---------------------------------------------------------------------------

describe("realestate library dispatch", () => {
  it("realestate__estimate_price with full address", async () => {
    const { status, body } = await call(300, "realestate__estimate_price", {
      address: "10 rue Victor Hugo",
      city: "Lyon",
      propertyType: "apartment",
      areaSqm: 65,
    });
    expect(status).toBe(200);
    assertVendored(body, "realestate__estimate_price", "Not a substitute for a licensed real estate appraisal");
  });

  it("realestate__property_description with propertyType + area", async () => {
    const { status, body } = await call(301, "realestate__property_description", {
      propertyType: "house",
      areaSqm: 120,
    });
    expect(status).toBe(200);
    assertVendored(body, "realestate__property_description", "Not a substitute for a licensed real estate appraisal");
  });

  it("realestate__neighborhood_analysis with address + city", async () => {
    const { status, body } = await call(302, "realestate__neighborhood_analysis", {
      address: "5 avenue des Champs",
      city: "Paris",
    });
    expect(status).toBe(200);
    assertVendored(body, "realestate__neighborhood_analysis", "Not a substitute for a licensed real estate appraisal");
  });
});

// ---------------------------------------------------------------------------
// 4. french-admin
// ---------------------------------------------------------------------------

describe("french-admin library dispatch", () => {
  it("french_admin__simuler_impots with revenu + parts", async () => {
    const { status, body } = await call(400, "french_admin__simuler_impots", {
      revenuNet: 40000,
      parts: 2,
    });
    expect(status).toBe(200);
    assertVendored(body, "french_admin__simuler_impots", "Ne constitue pas un conseil juridique ou fiscal");
  });

  it("french_admin__calculer_charges_ae with CA + activite", async () => {
    const { status, body } = await call(401, "french_admin__calculer_charges_ae", {
      chiffreAffaires: 50000,
      activite: "prestation_service_bnc",
    });
    expect(status).toBe(200);
    assertVendored(body, "french_admin__calculer_charges_ae", "Ne constitue pas un conseil juridique ou fiscal");
  });

  it("french_admin__simuler_chomage with salaire + duree", async () => {
    const { status, body } = await call(402, "french_admin__simuler_chomage", {
      salaireBrutMensuel: 2800,
      dureeTravailMois: 24,
    });
    expect(status).toBe(200);
    assertVendored(body, "french_admin__simuler_chomage", "Ne constitue pas un conseil juridique ou fiscal");
  });
});

// ---------------------------------------------------------------------------
// 5. ecommerce-3d
// ---------------------------------------------------------------------------

describe("ecommerce-3d library dispatch", () => {
  it("ecommerce3d__generate_product_3d with name", async () => {
    const { status, body } = await call(500, "ecommerce3d__generate_product_3d", {
      name: "Modern Sofa",
    });
    expect(status).toBe(200);
    assertVendored(body, "ecommerce3d__generate_product_3d", "Review all generated code before deploying to production");
  });

  it("ecommerce3d__generate_turntable with name", async () => {
    const { status, body } = await call(501, "ecommerce3d__generate_turntable", {
      name: "Watch",
    });
    expect(status).toBe(200);
    assertVendored(body, "ecommerce3d__generate_turntable", "Review all generated code before deploying to production");
  });

  it("ecommerce3d__shopify_snippet with name", async () => {
    const { status, body } = await call(502, "ecommerce3d__shopify_snippet", {
      name: "Leather Bag",
    });
    expect(status).toBe(200);
    assertVendored(body, "ecommerce3d__shopify_snippet", "Review all generated code before deploying to production");
  });
});

// ---------------------------------------------------------------------------
// 6. legal-docs
// ---------------------------------------------------------------------------

describe("legal-docs library dispatch", () => {
  it("legal_docs__generate_cgv with company object", async () => {
    const { status, body } = await call(600, "legal_docs__generate_cgv", {
      company: { name: "ACME SAS", address: "1 rue de la Paix, Paris", email: "contact@acme.fr" },
    });
    expect(status).toBe(200);
    assertVendored(body, "legal_docs__generate_cgv", "Not legal advice");
  });

  it("legal_docs__generate_nda with two parties", async () => {
    const { status, body } = await call(601, "legal_docs__generate_nda", {
      disclosingParty: { name: "Alice Corp", address: "Paris" },
      receivingParty: { name: "Bob LLC", address: "London" },
    });
    expect(status).toBe(200);
    assertVendored(body, "legal_docs__generate_nda", "Not legal advice");
  });

  it("legal_docs__generate_privacy_policy with company", async () => {
    const { status, body } = await call(602, "legal_docs__generate_privacy_policy", {
      company: { name: "Tech SARL", address: "42 rue du Code, Paris", email: "privacy@tech.fr" },
    });
    expect(status).toBe(200);
    assertVendored(body, "legal_docs__generate_privacy_policy", "Not legal advice");
  });
});

// ---------------------------------------------------------------------------
// 7. social-media
// ---------------------------------------------------------------------------

describe("social-media library dispatch", () => {
  it("social_media__hashtag_research with topic", async () => {
    const { status, body } = await call(700, "social_media__hashtag_research", {
      topic: "machine learning",
    });
    expect(status).toBe(200);
    assertVendored(body, "social_media__hashtag_research", "Never publishes on your behalf");
  });

  it("social_media__analyze_post with content + platform", async () => {
    const { status, body } = await call(701, "social_media__analyze_post", {
      content: "Just shipped v4!",
      platform: "twitter",
    });
    expect(status).toBe(200);
    assertVendored(body, "social_media__analyze_post", "Never publishes on your behalf");
  });

  it("social_media__generate_twitter_thread with topic", async () => {
    const { status, body } = await call(702, "social_media__generate_twitter_thread", {
      topic: "AI in healthcare",
      tweetCount: 5,
    });
    expect(status).toBe(200);
    assertVendored(body, "social_media__generate_twitter_thread", "Never publishes on your behalf");
  });
});

// ---------------------------------------------------------------------------
// 8. health-fitness
// ---------------------------------------------------------------------------

describe("health-fitness library dispatch", () => {
  it("health_fitness__calculate_bmi with weight + height", async () => {
    const { status, body } = await call(800, "health_fitness__calculate_bmi", {
      weightKg: 75,
      heightCm: 180,
    });
    expect(status).toBe(200);
    assertVendored(body, "health_fitness__calculate_bmi", "Not medical advice");
  });

  it("health_fitness__calculate_tdee with full args", async () => {
    const { status, body } = await call(801, "health_fitness__calculate_tdee", {
      weightKg: 75,
      heightCm: 180,
      age: 30,
      biologicalSex: "male",
      activityLevel: "moderate",
    });
    expect(status).toBe(200);
    assertVendored(body, "health_fitness__calculate_tdee", "Not medical advice");
  });

  it("health_fitness__calculate_heart_rate_zones with age", async () => {
    const { status, body } = await call(802, "health_fitness__calculate_heart_rate_zones", {
      age: 30,
      restingHeartRate: 65,
    });
    expect(status).toBe(200);
    assertVendored(body, "health_fitness__calculate_heart_rate_zones", "Not medical advice");
  });
});
