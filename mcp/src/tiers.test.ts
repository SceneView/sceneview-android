import { describe, it, expect } from "vitest";
import {
  isProTool,
  getToolTier,
  getProToolNames,
  getFreeToolNames,
  PRO_UPGRADE_MESSAGE,
  TOOL_TIERS,
} from "./tiers.js";

// ─── Free tools list (must match tiers.ts) ──────────────────────────────────

const EXPECTED_FREE_TOOLS = [
  "list_samples",
  "get_sample",
  "get_setup",
  "get_node_reference",
  "list_platforms",
  "validate_code",
  "get_troubleshooting",
  "debug_issue",
  "get_best_practices",
  "get_animation_guide",
  "get_gesture_guide",
  "get_performance_tips",
  "get_material_guide",
  "get_collision_guide",
  "get_platform_roadmap",
];

// ─── isProTool ──────────────────────────────────────────────────────────────

describe("isProTool", () => {
  it.each(EXPECTED_FREE_TOOLS)(
    "returns false for free tool: %s",
    (toolName) => {
      expect(isProTool(toolName)).toBe(false);
    },
  );

  it("returns true for pro tool: get_ios_setup", () => {
    expect(isProTool("get_ios_setup")).toBe(true);
  });

  it("returns true for pro tool: migrate_code", () => {
    expect(isProTool("migrate_code")).toBe(true);
  });

  it("returns true for pro tool: render_3d_preview", () => {
    expect(isProTool("render_3d_preview")).toBe(true);
  });

  it("returns true for pro tool: get_car_configurator (automotive)", () => {
    expect(isProTool("get_car_configurator")).toBe(true);
  });

  it("returns true for pro tool: get_physics_game (gaming)", () => {
    expect(isProTool("get_physics_game")).toBe(true);
  });

  it("returns true for pro tool: get_surgical_planning (healthcare)", () => {
    expect(isProTool("get_surgical_planning")).toBe(true);
  });

  it("returns true for pro tool: get_room_planner (interior)", () => {
    expect(isProTool("get_room_planner")).toBe(true);
  });
});

// ─── getToolTier ────────────────────────────────────────────────────────────

describe("getToolTier", () => {
  it("returns 'free' for a free tool", () => {
    expect(getToolTier("list_samples")).toBe("free");
  });

  it("returns 'pro' for a pro tool", () => {
    expect(getToolTier("get_ios_setup")).toBe("pro");
  });

  it("defaults to 'pro' for unknown tools", () => {
    expect(getToolTier("totally_unknown_tool")).toBe("pro");
  });

  it("defaults to 'pro' for empty string", () => {
    expect(getToolTier("")).toBe("pro");
  });
});

// ─── getProToolNames ────────────────────────────────────────────────────────

describe("getProToolNames", () => {
  it("returns only pro tools", () => {
    const proTools = getProToolNames();
    for (const name of proTools) {
      expect(getToolTier(name)).toBe("pro");
    }
  });

  it("includes known pro tools", () => {
    const proTools = getProToolNames();
    expect(proTools).toContain("get_ios_setup");
    expect(proTools).toContain("migrate_code");
    expect(proTools).toContain("get_car_configurator");
  });

  it("does not include any free tools", () => {
    const proTools = getProToolNames();
    for (const freeTool of EXPECTED_FREE_TOOLS) {
      expect(proTools).not.toContain(freeTool);
    }
  });

  it("returns a new array (not a reference to the internal list)", () => {
    const a = getProToolNames();
    const b = getProToolNames();
    expect(a).not.toBe(b);
    expect(a).toEqual(b);
  });
});

// ─── getFreeToolNames ───────────────────────────────────────────────────────

describe("getFreeToolNames", () => {
  it("returns only free tools", () => {
    const freeTools = getFreeToolNames();
    for (const name of freeTools) {
      expect(getToolTier(name)).toBe("free");
    }
  });

  it("includes known free tools", () => {
    const freeTools = getFreeToolNames();
    expect(freeTools).toContain("list_samples");
    expect(freeTools).toContain("validate_code");
    expect(freeTools).toContain("get_best_practices");
  });

  it("does not include any pro tools", () => {
    const freeTools = getFreeToolNames();
    const proTools = getProToolNames();
    for (const proTool of proTools) {
      expect(freeTools).not.toContain(proTool);
    }
  });

  it("returns a new array (not a reference to the internal list)", () => {
    const a = getFreeToolNames();
    const b = getFreeToolNames();
    expect(a).not.toBe(b);
    expect(a).toEqual(b);
  });
});

// ─── Free and pro lists don't overlap ───────────────────────────────────────

describe("tier list integrity", () => {
  it("free and pro tool lists have no overlap", () => {
    const freeSet = new Set(getFreeToolNames());
    const proTools = getProToolNames();
    const overlap = proTools.filter((name) => freeSet.has(name));
    expect(overlap).toEqual([]);
  });

  it("all tools in TOOL_TIERS are accounted for in free or pro", () => {
    const freeSet = new Set(getFreeToolNames());
    const proSet = new Set(getProToolNames());
    const allRegistered = Object.keys(TOOL_TIERS);

    for (const name of allRegistered) {
      const inFree = freeSet.has(name);
      const inPro = proSet.has(name);
      expect(inFree || inPro).toBe(true);
      // A tool must not appear in both lists
      expect(inFree && inPro).toBe(false);
    }
  });

  it("no tool appears in both free and pro lists simultaneously", () => {
    const freeTools = getFreeToolNames();
    const proTools = getProToolNames();
    const combined = [...freeTools, ...proTools];
    const unique = new Set(combined);
    expect(unique.size).toBe(combined.length);
  });
});

// ─── PRO_UPGRADE_MESSAGE ────────────────────────────────────────────────────

describe("PRO_UPGRADE_MESSAGE", () => {
  it("contains the upgrade URL", () => {
    expect(PRO_UPGRADE_MESSAGE).toContain("https://polar.sh/sceneview");
  });

  it("mentions Pro", () => {
    expect(PRO_UPGRADE_MESSAGE).toMatch(/pro/i);
  });

  it("mentions the SCENEVIEW_API_KEY env var", () => {
    expect(PRO_UPGRADE_MESSAGE).toContain("SCENEVIEW_API_KEY");
  });
});
