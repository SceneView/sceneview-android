import { describe, it, expect } from "vitest";
import {
  convertAndroidToIos,
  convertIosToAndroid,
  generateMultiplatformCode,
  formatConversionResult,
  formatMultiplatformResult,
} from "./convert-platform.js";

describe("convertAndroidToIos", () => {
  it("converts SceneView( to SceneView {", () => {
    const result = convertAndroidToIos('SceneView(engine = engine) { }');
    expect(result.code).toContain("SceneView");
    expect(result.sourceplatform).toBe("android");
    expect(result.targetPlatform).toBe("ios");
  });

  it("converts .glb to .usdz", () => {
    const result = convertAndroidToIos('"models/chair.glb"');
    expect(result.code).toContain(".usdz");
  });

  it("adds warnings about RealityKit differences", () => {
    const result = convertAndroidToIos("SceneView(engine = engine) { }");
    expect(result.warnings.length).toBeGreaterThan(0);
  });

  it("formatConversionResult produces markdown", () => {
    const result = convertAndroidToIos("SceneView(engine = engine) { }");
    const text = formatConversionResult(result);
    expect(text).toContain("## Code Converted to");
    expect(text).toContain("swift");
  });
});

describe("convertIosToAndroid", () => {
  it("converts SceneView { to SceneView(engine = engine) {", () => {
    const result = convertIosToAndroid("SceneView { root in }");
    expect(result.code).toContain("SceneView(engine = engine)");
    expect(result.sourceplatform).toBe("ios");
    expect(result.targetPlatform).toBe("android");
  });

  it("converts .usdz to .glb", () => {
    const result = convertIosToAndroid('"models/chair.usdz"');
    expect(result.code).toContain(".glb");
  });
});

describe("generateMultiplatformCode", () => {
  it("generates both Android and iOS code for 3D scene", () => {
    const result = generateMultiplatformCode("3D product showcase");
    expect(result.androidCode).toContain("@Composable");
    expect(result.iosCode).toContain("SwiftUI");
    expect(result.notes.length).toBeGreaterThan(0);
  });

  it("generates AR code when description mentions AR", () => {
    const result = generateMultiplatformCode("AR furniture viewer");
    expect(result.androidCode).toContain("ARSceneView");
    expect(result.iosCode).toContain("ARSceneView");
  });

  it("formatMultiplatformResult produces markdown with both platforms", () => {
    const result = generateMultiplatformCode("3D viewer");
    const text = formatMultiplatformResult(result);
    expect(text).toContain("### Android");
    expect(text).toContain("### iOS");
    expect(text).toContain("```kotlin");
    expect(text).toContain("```swift");
  });
});
