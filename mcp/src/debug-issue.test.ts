import { describe, it, expect } from "vitest";
import { getDebugGuide, autoDetectIssue, DEBUG_CATEGORIES, type DebugCategory } from "./debug-issue.js";

describe("DEBUG_CATEGORIES", () => {
  it("has 9 categories", () => {
    expect(DEBUG_CATEGORIES).toHaveLength(9);
  });

  it("includes all expected categories", () => {
    expect(DEBUG_CATEGORIES).toContain("model-not-showing");
    expect(DEBUG_CATEGORIES).toContain("ar-not-working");
    expect(DEBUG_CATEGORIES).toContain("crash");
    expect(DEBUG_CATEGORIES).toContain("performance");
    expect(DEBUG_CATEGORIES).toContain("build-error");
    expect(DEBUG_CATEGORIES).toContain("black-screen");
    expect(DEBUG_CATEGORIES).toContain("lighting");
    expect(DEBUG_CATEGORIES).toContain("gestures");
    expect(DEBUG_CATEGORIES).toContain("ios");
  });
});

describe("getDebugGuide", () => {
  it("returns guide for model-not-showing", () => {
    const result = getDebugGuide("model-not-showing");
    expect(result).toContain("rememberModelInstance");
    expect(result).toContain("scaleToUnits");
    expect(result).toContain("LightNode");
  });

  it("returns guide for ar-not-working", () => {
    const result = getDebugGuide("ar-not-working");
    expect(result).toContain("CAMERA");
    expect(result).toContain("ARCore");
    expect(result).toContain("planeFindingMode");
  });

  it("returns guide for crash", () => {
    const result = getDebugGuide("crash");
    expect(result).toContain("SIGABRT");
    expect(result).toContain("main thread");
    expect(result).toContain("destroy order");
  });

  it("returns guide for performance", () => {
    const result = getDebugGuide("performance");
    expect(result).toContain("FPS");
    expect(result).toContain("poly");
    expect(result).toContain("KTX2");
  });

  it("returns guide for build-error", () => {
    const result = getDebugGuide("build-error");
    expect(result).toContain("Java 17");
    expect(result).toContain("mavenCentral");
  });

  it("returns guide for black-screen", () => {
    const result = getDebugGuide("black-screen");
    expect(result).toContain("Camera permission");
    expect(result).toContain("No light source");
  });

  it("returns guide for lighting", () => {
    const result = getDebugGuide("lighting");
    expect(result).toContain("Intensity");
    expect(result).toContain("Shadows");
  });

  it("returns guide for gestures", () => {
    const result = getDebugGuide("gestures");
    expect(result).toContain("isEditable");
    expect(result).toContain("onTouchEvent");
  });

  it("returns guide for ios", () => {
    const result = getDebugGuide("ios");
    expect(result).toContain("USDZ");
    expect(result).toContain("try await");
    expect(result).toContain("SPM");
  });

  it("returns error for unknown category", () => {
    const result = getDebugGuide("unknown" as DebugCategory);
    expect(result).toContain("Unknown debug category");
  });

  for (const category of DEBUG_CATEGORIES) {
    it(`${category}: guide is non-empty and well-formed`, () => {
      const result = getDebugGuide(category);
      expect(result.length).toBeGreaterThan(100);
      expect(result).toContain("#");
    });
  }
});

describe("autoDetectIssue", () => {
  it("detects model-not-showing from description", () => {
    expect(autoDetectIssue("My model is not showing")).toBe("model-not-showing");
    expect(autoDetectIssue("The 3D model doesn't appear")).toBe("model-not-showing");
    expect(autoDetectIssue("I can't see the model")).toBe("model-not-showing");
  });

  it("detects ar-not-working from description", () => {
    expect(autoDetectIssue("AR not working on my device")).toBe("ar-not-working");
    expect(autoDetectIssue("Planes are not detected")).toBe("ar-not-working");
    expect(autoDetectIssue("ARCore error")).toBe("ar-not-working");
  });

  it("detects crash from description", () => {
    expect(autoDetectIssue("My app crashes when loading a model")).toBe("crash");
    expect(autoDetectIssue("SIGABRT on destroy")).toBe("crash");
    expect(autoDetectIssue("Fatal exception in Filament")).toBe("crash");
  });

  it("detects performance from description", () => {
    expect(autoDetectIssue("The scene is very slow")).toBe("performance");
    expect(autoDetectIssue("Low FPS when rendering")).toBe("performance");
    expect(autoDetectIssue("High memory usage")).toBe("performance");
  });

  it("detects build-error from description", () => {
    expect(autoDetectIssue("Gradle build fails")).toBe("build-error");
    expect(autoDetectIssue("Cannot resolve dependency")).toBe("build-error");
  });

  it("detects black-screen from description", () => {
    expect(autoDetectIssue("I see a black screen")).toBe("black-screen");
    expect(autoDetectIssue("Nothing renders on screen")).toBe("black-screen");
  });

  it("detects lighting from description", () => {
    expect(autoDetectIssue("Model is too dark")).toBe("lighting");
    expect(autoDetectIssue("No shadows visible")).toBe("lighting");
    expect(autoDetectIssue("Scene is overexposed")).toBe("lighting");
  });

  it("detects gestures from description", () => {
    expect(autoDetectIssue("Can't tap on the model")).toBe("gestures");
    expect(autoDetectIssue("Drag to rotate not working")).toBe("gestures");
  });

  it("detects ios from description", () => {
    expect(autoDetectIssue("iOS model not loading")).toBe("ios");
    expect(autoDetectIssue("SwiftUI SceneView issue")).toBe("ios");
    expect(autoDetectIssue("Xcode SPM error")).toBe("ios");
  });

  it("returns null for unrecognized description", () => {
    expect(autoDetectIssue("How do I use SceneView?")).toBeNull();
  });
});
