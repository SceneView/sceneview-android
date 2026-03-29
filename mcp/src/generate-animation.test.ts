import { describe, it, expect } from "vitest";
import { generateAnimationCode, formatAnimationCode, ANIMATION_TYPES } from "./generate-animation.js";

describe("generateAnimationCode", () => {
  it("returns result for each animation type on Android", () => {
    for (const type of ANIMATION_TYPES) {
      const result = generateAnimationCode(type, "android");
      expect(result).not.toBeNull();
      expect(result!.code).toBeTruthy();
      expect(result!.platform).toBe("android");
      expect(result!.animationType).toBe(type);
    }
  });

  it("returns result for iOS platform", () => {
    const result = generateAnimationCode("model-playback", "ios");
    expect(result).not.toBeNull();
    expect(result!.platform).toBe("ios");
  });

  it("returns null for unknown type", () => {
    const result = generateAnimationCode("unknown" as any, "android");
    expect(result).toBeNull();
  });

  it("generated Android code contains composable patterns", () => {
    const result = generateAnimationCode("model-playback", "android");
    expect(result!.code).toContain("@Composable");
    expect(result!.code).toContain("rememberEngine");
  });

  it("formatAnimationCode produces markdown", () => {
    const result = generateAnimationCode("spring-position", "android")!;
    const text = formatAnimationCode(result);
    expect(text).toContain("## Animation:");
    expect(text).toContain("```kotlin");
  });
});
