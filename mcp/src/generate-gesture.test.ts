import { describe, it, expect } from "vitest";
import { generateGestureCode, formatGestureCode, GESTURE_TYPES } from "./generate-gesture.js";

describe("generateGestureCode", () => {
  it("returns result for each gesture type on Android", () => {
    for (const type of GESTURE_TYPES) {
      const result = generateGestureCode(type, "android");
      expect(result).not.toBeNull();
      expect(result!.code).toBeTruthy();
      expect(result!.title).toBeTruthy();
    }
  });

  it("returns iOS code for tap-to-place-ar", () => {
    const result = generateGestureCode("tap-to-place-ar", "ios");
    expect(result).not.toBeNull();
    expect(result!.code).toContain("SwiftUI");
  });

  it("falls back to Android code when iOS not available", () => {
    const result = generateGestureCode("pinch-to-scale", "ios");
    expect(result).not.toBeNull();
    // Should return Android code as fallback
    expect(result!.code).toContain("@Composable");
  });

  it("returns null for unknown type", () => {
    const result = generateGestureCode("unknown" as any);
    expect(result).toBeNull();
  });

  it("editable-model contains isEditable", () => {
    const result = generateGestureCode("editable-model", "android");
    expect(result!.code).toContain("isEditable = true");
  });

  it("formatGestureCode produces markdown", () => {
    const result = generateGestureCode("tap-to-select", "android")!;
    const text = formatGestureCode(result, "android");
    expect(text).toContain("## Tap to Select");
    expect(text).toContain("```kotlin");
    expect(text).toContain("### Available Gesture Types");
  });
});
