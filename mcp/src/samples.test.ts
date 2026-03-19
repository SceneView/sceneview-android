import { describe, it, expect } from "vitest";
import { SAMPLES, SAMPLE_IDS, getSample } from "./samples.js";

describe("SAMPLE_IDS", () => {
  it("contains all 6 expected scenarios", () => {
    expect(SAMPLE_IDS).toContain("model-viewer");
    expect(SAMPLE_IDS).toContain("geometry-scene");
    expect(SAMPLE_IDS).toContain("ar-tap-to-place");
    expect(SAMPLE_IDS).toContain("ar-placement-cursor");
    expect(SAMPLE_IDS).toContain("ar-augmented-image");
    expect(SAMPLE_IDS).toContain("ar-face-filter");
  });

  it("SAMPLE_IDS matches keys of SAMPLES", () => {
    expect(SAMPLE_IDS.sort()).toEqual(Object.keys(SAMPLES).sort());
  });
});

describe("every sample", () => {
  for (const id of SAMPLE_IDS) {
    const sample = SAMPLES[id];

    it(`${id}: has all required fields`, () => {
      expect(sample.id).toBe(id);
      expect(sample.title).toBeTruthy();
      expect(sample.description).toBeTruthy();
      expect(sample.tags.length).toBeGreaterThan(0);
      expect(sample.dependency).toMatch(/^io\.github\.sceneview:/);
      expect(sample.prompt).toBeTruthy();
      expect(sample.code).toBeTruthy();
    });

    it(`${id}: code is non-empty Kotlin`, () => {
      expect(sample.code).toContain("@Composable");
      expect(sample.code).toContain("fun ");
    });

    it(`${id}: dependency is a valid sceneview artifact`, () => {
      expect(["io.github.sceneview:sceneview:3.1.1", "io.github.sceneview:arsceneview:3.1.1"]).toContain(
        sample.dependency
      );
    });
  }
});

describe("AR samples", () => {
  const arIds = SAMPLE_IDS.filter((id) => SAMPLES[id].tags.includes("ar"));

  it("all AR samples use arsceneview dependency", () => {
    for (const id of arIds) {
      expect(SAMPLES[id].dependency).toBe("io.github.sceneview:arsceneview:3.1.1");
    }
  });

  it("all AR samples contain ARScene in code", () => {
    for (const id of arIds) {
      expect(SAMPLES[id].code).toContain("ARScene");
    }
  });

  it("all AR samples have the 'ar' tag", () => {
    for (const id of arIds) {
      expect(SAMPLES[id].tags).toContain("ar");
    }
  });
});

describe("3D samples", () => {
  const d3Ids = SAMPLE_IDS.filter((id) => SAMPLES[id].tags.includes("3d"));

  it("all 3D samples use sceneview dependency", () => {
    for (const id of d3Ids) {
      expect(SAMPLES[id].dependency).toBe("io.github.sceneview:sceneview:3.1.1");
    }
  });

  it("all 3D samples contain Scene in code", () => {
    for (const id of d3Ids) {
      expect(SAMPLES[id].code).toContain("Scene(");
    }
  });
});

describe("getSample", () => {
  it("returns the correct sample by ID", () => {
    const s = getSample("model-viewer");
    expect(s).toBeDefined();
    expect(s!.id).toBe("model-viewer");
    expect(s!.title).toBe("3D Model Viewer");
  });

  it("returns undefined for an unknown ID", () => {
    expect(getSample("nonexistent-scenario")).toBeUndefined();
  });

  it("returns all samples without undefined", () => {
    for (const id of SAMPLE_IDS) {
      expect(getSample(id)).toBeDefined();
    }
  });
});

describe("tag filtering (simulating list_samples tool)", () => {
  const filterByTag = (tag: string) =>
    Object.values(SAMPLES).filter((s) => s.tags.includes(tag as any));

  it("tag 'ar' returns only AR samples", () => {
    const results = filterByTag("ar");
    expect(results.length).toBeGreaterThan(0);
    results.forEach((s) => expect(s.tags).toContain("ar"));
  });

  it("tag '3d' returns only 3D samples", () => {
    const results = filterByTag("3d");
    expect(results.length).toBeGreaterThan(0);
    results.forEach((s) => expect(s.tags).toContain("3d"));
  });

  it("tag 'face-tracking' returns only the face filter sample", () => {
    const results = filterByTag("face-tracking");
    expect(results).toHaveLength(1);
    expect(results[0].id).toBe("ar-face-filter");
  });

  it("tag 'image-tracking' returns only augmented image sample", () => {
    const results = filterByTag("image-tracking");
    expect(results).toHaveLength(1);
    expect(results[0].id).toBe("ar-augmented-image");
  });

  it("tag 'geometry' returns only geometry scene", () => {
    const results = filterByTag("geometry");
    expect(results).toHaveLength(1);
    expect(results[0].id).toBe("geometry-scene");
  });

  it("tag 'anchor' returns AR samples that use anchors", () => {
    const results = filterByTag("anchor");
    expect(results.length).toBeGreaterThan(0);
    results.forEach((s) => expect(s.tags).toContain("anchor"));
  });

  it("unknown tag returns empty array", () => {
    expect(filterByTag("nonexistent-tag")).toHaveLength(0);
  });
});
