import { describe, it, expect } from "vitest";
import {
  validateArtifactInput,
  generateArtifact,
  formatArtifactResponse,
  type ArtifactInput,
} from "./artifact.js";

// ─── validateArtifactInput ───────────────────────────────────────────────────

describe("validateArtifactInput", () => {
  it("accepts valid model-viewer input", () => {
    expect(validateArtifactInput({ type: "model-viewer" })).toBeNull();
  });

  it("accepts valid model-viewer with model URL", () => {
    expect(
      validateArtifactInput({
        type: "model-viewer",
        modelUrl: "https://example.com/model.glb",
      })
    ).toBeNull();
  });

  it("rejects invalid type", () => {
    const err = validateArtifactInput({ type: "invalid" as any });
    expect(err).toContain("Invalid type");
  });

  it("rejects chart-3d without data", () => {
    const err = validateArtifactInput({ type: "chart-3d" });
    expect(err).toContain("non-empty `data` array");
  });

  it("rejects chart-3d with empty data array", () => {
    const err = validateArtifactInput({ type: "chart-3d", data: [] });
    expect(err).toContain("non-empty `data` array");
  });

  it("rejects chart-3d data with missing label", () => {
    const err = validateArtifactInput({
      type: "chart-3d",
      data: [{ label: 123 as any, value: 100 }],
    });
    expect(err).toContain("string `label`");
  });

  it("rejects chart-3d data with missing value", () => {
    const err = validateArtifactInput({
      type: "chart-3d",
      data: [{ label: "Q1", value: "100" as any }],
    });
    expect(err).toContain("numeric `value`");
  });

  it("accepts valid chart-3d data", () => {
    expect(
      validateArtifactInput({
        type: "chart-3d",
        data: [
          { label: "Q1", value: 100 },
          { label: "Q2", value: 200 },
        ],
      })
    ).toBeNull();
  });

  it("rejects non-HTTP model URL", () => {
    const err = validateArtifactInput({
      type: "model-viewer",
      modelUrl: "ftp://example.com/model.glb",
    });
    expect(err).toContain("HTTP(S)");
  });

  it("accepts scene type", () => {
    expect(validateArtifactInput({ type: "scene" })).toBeNull();
  });

  it("accepts product-360 type", () => {
    expect(validateArtifactInput({ type: "product-360" })).toBeNull();
  });
});

// ─── generateArtifact — model-viewer ─────────────────────────────────────────

describe("generateArtifact — model-viewer", () => {
  it("generates valid HTML with model-viewer tag", () => {
    const result = generateArtifact({ type: "model-viewer" });
    expect(result.type).toBe("model-viewer");
    expect(result.html).toContain("<!DOCTYPE html>");
    expect(result.html).toContain("<model-viewer");
    expect(result.html).toContain("camera-controls");
    expect(result.html).toContain("Powered by SceneView");
  });

  it("uses default model when none provided", () => {
    const result = generateArtifact({ type: "model-viewer" });
    expect(result.html).toContain("DamagedHelmet.glb");
  });

  it("uses custom model URL", () => {
    const result = generateArtifact({
      type: "model-viewer",
      modelUrl: "https://example.com/car.glb",
    });
    expect(result.html).toContain("https://example.com/car.glb");
  });

  it("uses custom title", () => {
    const result = generateArtifact({
      type: "model-viewer",
      title: "My Helmet",
    });
    expect(result.title).toBe("My Helmet");
    expect(result.html).toContain("My Helmet");
  });

  it("includes AR by default", () => {
    const result = generateArtifact({ type: "model-viewer" });
    expect(result.html).toContain("ar ");
    expect(result.html).toContain("ar-modes");
    expect(result.html).toContain("View in AR");
  });

  it("disables AR when option is false", () => {
    const result = generateArtifact({
      type: "model-viewer",
      options: { ar: false },
    });
    expect(result.html).not.toContain("ar-modes");
    expect(result.html).not.toContain("View in AR");
  });

  it("includes auto-rotate by default", () => {
    const result = generateArtifact({ type: "model-viewer" });
    expect(result.html).toContain("auto-rotate");
  });

  it("disables auto-rotate when option is false", () => {
    const result = generateArtifact({
      type: "model-viewer",
      options: { autoRotate: false },
    });
    expect(result.html).not.toContain("auto-rotate");
  });

  it("uses custom background color", () => {
    const result = generateArtifact({
      type: "model-viewer",
      options: { backgroundColor: "#ff0000" },
    });
    expect(result.html).toContain("#ff0000");
  });

  it("uses custom camera orbit", () => {
    const result = generateArtifact({
      type: "model-viewer",
      options: { cameraOrbit: "45deg 60deg 200%" },
    });
    expect(result.html).toContain("45deg 60deg 200%");
  });

  it("includes model-viewer CDN script", () => {
    const result = generateArtifact({ type: "model-viewer" });
    expect(result.html).toContain("ajax.googleapis.com/ajax/libs/model-viewer");
  });
});

// ─── generateArtifact — chart-3d ─────────────────────────────────────────────

describe("generateArtifact — chart-3d", () => {
  const chartInput: ArtifactInput = {
    type: "chart-3d",
    data: [
      { label: "Q1", value: 50000 },
      { label: "Q2", value: 75000 },
      { label: "Q3", value: 120000 },
    ],
  };

  it("generates HTML with all bar labels", () => {
    const result = generateArtifact(chartInput);
    expect(result.type).toBe("chart-3d");
    expect(result.html).toContain("Q1");
    expect(result.html).toContain("Q2");
    expect(result.html).toContain("Q3");
  });

  it("formats large numbers with K/M suffix", () => {
    const result = generateArtifact(chartInput);
    expect(result.html).toContain("50K");
    expect(result.html).toContain("75K");
    expect(result.html).toContain("120K");
  });

  it("shows total", () => {
    const result = generateArtifact(chartInput);
    expect(result.html).toContain("245K");
  });

  it("uses custom colors", () => {
    const result = generateArtifact({
      type: "chart-3d",
      data: [{ label: "Revenue", value: 100, color: "#ff6600" }],
    });
    expect(result.html).toContain("#ff6600");
  });

  it("uses default colors when none specified", () => {
    const result = generateArtifact({
      type: "chart-3d",
      data: [{ label: "Revenue", value: 100 }],
    });
    expect(result.html).toContain("#4285F4"); // first default color
  });

  it("includes chart title", () => {
    const result = generateArtifact({
      type: "chart-3d",
      title: "Revenue Dashboard",
      data: [{ label: "Q1", value: 100 }],
    });
    expect(result.html).toContain("Revenue Dashboard");
    expect(result.title).toBe("Revenue Dashboard");
  });

  it("has SceneView branding", () => {
    const result = generateArtifact(chartInput);
    expect(result.html).toContain("Powered by SceneView");
  });

  it("handles millions formatting", () => {
    const result = generateArtifact({
      type: "chart-3d",
      data: [{ label: "ARR", value: 2500000 }],
    });
    expect(result.html).toContain("2.5M");
  });

  it("renders valid HTML document", () => {
    const result = generateArtifact(chartInput);
    expect(result.html).toContain("<!DOCTYPE html>");
    expect(result.html).toContain("</html>");
  });
});

// ─── generateArtifact — scene ────────────────────────────────────────────────

describe("generateArtifact — scene", () => {
  it("generates scene with model-viewer", () => {
    const result = generateArtifact({ type: "scene" });
    expect(result.type).toBe("scene");
    expect(result.html).toContain("<model-viewer");
    expect(result.html).toContain("Interactive 3D Scene");
  });

  it("uses custom title", () => {
    const result = generateArtifact({ type: "scene", title: "My World" });
    expect(result.html).toContain("My World");
  });

  it("includes control hints", () => {
    const result = generateArtifact({ type: "scene" });
    expect(result.html).toContain("Drag to orbit");
    expect(result.html).toContain("Scroll to zoom");
  });

  it("includes shadow and lighting", () => {
    const result = generateArtifact({ type: "scene" });
    expect(result.html).toContain("shadow-intensity");
    expect(result.html).toContain("environment-image");
  });
});

// ─── generateArtifact — product-360 ──────────────────────────────────────────

describe("generateArtifact — product-360", () => {
  it("generates product viewer", () => {
    const result = generateArtifact({ type: "product-360" });
    expect(result.type).toBe("product-360");
    expect(result.html).toContain("<model-viewer");
    expect(result.html).toContain("360");
  });

  it("includes AR button with product text", () => {
    const result = generateArtifact({ type: "product-360" });
    expect(result.html).toContain("View in Your Space");
  });

  it("renders hotspots when provided", () => {
    const result = generateArtifact({
      type: "product-360",
      hotspots: [
        {
          position: "0.5 1.2 0.3",
          normal: "0 1 0",
          label: "Camera Lens",
          description: "50MP main sensor",
        },
      ],
    });
    expect(result.html).toContain("Camera Lens");
    expect(result.html).toContain("50MP main sensor");
    expect(result.html).toContain("hotspot-0");
  });

  it("renders multiple hotspots", () => {
    const result = generateArtifact({
      type: "product-360",
      hotspots: [
        { position: "0 0 0", normal: "0 1 0", label: "Top" },
        { position: "1 0 0", normal: "0 1 0", label: "Side" },
      ],
    });
    expect(result.html).toContain("hotspot-0");
    expect(result.html).toContain("hotspot-1");
    expect(result.html).toContain("Top");
    expect(result.html).toContain("Side");
  });

  it("shows rotation hint when no hotspots", () => {
    const result = generateArtifact({ type: "product-360" });
    expect(result.html).toContain("Drag to rotate");
    expect(result.html).not.toContain("Tap hotspots");
  });

  it("shows hotspot hint when hotspots present", () => {
    const result = generateArtifact({
      type: "product-360",
      hotspots: [{ position: "0 0 0", normal: "0 1 0", label: "X" }],
    });
    expect(result.html).toContain("Tap hotspots for details");
  });

  it("uses tone-mapping commerce for product photography", () => {
    const result = generateArtifact({ type: "product-360" });
    expect(result.html).toContain('tone-mapping="commerce"');
  });
});

// ─── formatArtifactResponse ──────────────────────────────────────────────────

describe("formatArtifactResponse", () => {
  it("includes the HTML in a code block", () => {
    const result = generateArtifact({ type: "model-viewer" });
    const text = formatArtifactResponse(result);
    expect(text).toContain("```html");
    expect(text).toContain("<!DOCTYPE html>");
    expect(text).toContain("```");
  });

  it("includes usage instructions", () => {
    const result = generateArtifact({ type: "model-viewer" });
    const text = formatArtifactResponse(result);
    expect(text).toContain("View in AR");
    expect(text).toContain("Drag to orbit");
  });

  it("includes SceneView branding", () => {
    const result = generateArtifact({ type: "model-viewer" });
    const text = formatArtifactResponse(result);
    expect(text).toContain("Powered by SceneView");
  });

  it("shows chart label for chart-3d type", () => {
    const result = generateArtifact({
      type: "chart-3d",
      data: [{ label: "Q1", value: 100 }],
    });
    const text = formatArtifactResponse(result);
    expect(text).toContain("3D chart");
  });

  it("shows content label for non-chart types", () => {
    const result = generateArtifact({ type: "scene" });
    const text = formatArtifactResponse(result);
    expect(text).toContain("3D content");
  });
});

// ─── HTML safety ─────────────────────────────────────────────────────────────

describe("HTML escaping", () => {
  it("escapes HTML in title", () => {
    const result = generateArtifact({
      type: "model-viewer",
      title: '<script>alert("xss")</script>',
    });
    expect(result.html).not.toContain("<script>alert");
    expect(result.html).toContain("&lt;script&gt;");
  });

  it("escapes HTML in model URL", () => {
    const result = generateArtifact({
      type: "model-viewer",
      modelUrl: 'https://example.com/model.glb"><script>alert(1)</script>',
    });
    expect(result.html).not.toContain('"><script>');
  });

  it("escapes HTML in chart labels", () => {
    const result = generateArtifact({
      type: "chart-3d",
      data: [{ label: "<b>Q1</b>", value: 100 }],
    });
    expect(result.html).not.toContain("<b>Q1</b>");
    expect(result.html).toContain("&lt;b&gt;Q1&lt;/b&gt;");
  });

  it("escapes HTML in hotspot labels", () => {
    const result = generateArtifact({
      type: "product-360",
      hotspots: [
        { position: "0 0 0", normal: "0 1 0", label: '<img src=x onerror="alert(1)">' },
      ],
    });
    expect(result.html).not.toContain("<img");
    expect(result.html).toContain("&lt;img");
  });
});
