import { describe, it, expect } from "vitest";
import { generateEnvironmentCode, formatEnvironmentCode, ENVIRONMENT_TYPES } from "./generate-environment.js";

describe("generateEnvironmentCode", () => {
  it("returns result for each environment type on Android", () => {
    for (const type of ENVIRONMENT_TYPES) {
      const result = generateEnvironmentCode(type, "android");
      expect(result).not.toBeNull();
      expect(result!.code).toBeTruthy();
      expect(result!.platform).toBe("android");
    }
  });

  it("returns result for iOS platform", () => {
    const result = generateEnvironmentCode("hdr-environment", "ios");
    expect(result).not.toBeNull();
    expect(result!.platform).toBe("ios");
  });

  it("returns null for unknown type", () => {
    const result = generateEnvironmentCode("unknown" as any, "android");
    expect(result).toBeNull();
  });

  it("generated code contains Scene composable for Android", () => {
    const result = generateEnvironmentCode("studio-lighting", "android");
    expect(result!.code).toContain("Scene(");
  });

  it("formatEnvironmentCode produces markdown", () => {
    const result = generateEnvironmentCode("dynamic-sky", "android")!;
    const text = formatEnvironmentCode(result);
    expect(text).toContain("## Environment:");
    expect(text).toContain("```kotlin");
  });
});
