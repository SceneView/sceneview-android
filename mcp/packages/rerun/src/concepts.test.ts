import { describe, it, expect } from "vitest";
import { explainConcept, RERUN_CONCEPTS } from "./concepts.js";

describe("RERUN_CONCEPTS", () => {
  it("exposes the five concepts", () => {
    expect(RERUN_CONCEPTS).toEqual([
      "rrd",
      "timelines",
      "entities",
      "archetypes",
      "transforms",
    ]);
  });
});

describe("explainConcept", () => {
  it("returns a non-empty markdown explanation for every concept", () => {
    for (const c of RERUN_CONCEPTS) {
      const text = explainConcept(c);
      expect(text.length).toBeGreaterThan(100);
      expect(text).toMatch(/^##/m);
    }
  });

  it("rrd explanation mentions the binary format and Rerun Web Viewer", () => {
    const text = explainConcept("rrd");
    expect(text).toContain(".rrd");
    expect(text.toLowerCase()).toContain("viewer");
  });

  it("timelines explanation names the SceneView device_clock timeline", () => {
    const text = explainConcept("timelines");
    expect(text).toContain("device_clock");
    expect(text).toContain("set_time_nanos");
  });

  it("entities explanation lists the default bridge entity paths", () => {
    const text = explainConcept("entities");
    expect(text).toContain("world/camera");
    expect(text).toContain("world/planes");
    expect(text).toContain("world/points");
  });

  it("archetypes explanation covers Transform3D and Points3D", () => {
    const text = explainConcept("archetypes");
    expect(text).toContain("Transform3D");
    expect(text).toContain("Points3D");
    expect(text).toContain("LineStrips3D");
  });

  it("transforms explanation calls out the quaternion order", () => {
    const text = explainConcept("transforms");
    expect(text).toContain("(x, y, z, w)");
    expect(text).toContain("right-handed");
  });

  it("throws on an unknown concept", () => {
    expect(() =>
      // @ts-expect-error — testing runtime validation
      explainConcept("bogus"),
    ).toThrow(/unknown concept/i);
  });
});
