import { describe, it, expect } from "vitest";
import { generatePhysicsCode, formatPhysicsCode, PHYSICS_TYPES } from "./generate-physics.js";

describe("generatePhysicsCode", () => {
  it("returns result for each physics type on Android", () => {
    for (const type of PHYSICS_TYPES) {
      const result = generatePhysicsCode(type, "android");
      expect(result).not.toBeNull();
      expect(result!.code).toBeTruthy();
      expect(result!.title).toBeTruthy();
    }
  });

  it("gravity-drop has iOS code", () => {
    const result = generatePhysicsCode("gravity-drop", "ios");
    expect(result).not.toBeNull();
    expect(result!.code).toContain("SwiftUI");
  });

  it("returns null for unknown type", () => {
    const result = generatePhysicsCode("unknown" as any);
    expect(result).toBeNull();
  });

  it("rigid-body code contains collision response", () => {
    const result = generatePhysicsCode("rigid-body", "android");
    expect(result!.code).toContain("collision");
  });

  it("spring-physics code contains spring force", () => {
    const result = generatePhysicsCode("spring-physics", "android");
    expect(result!.code).toContain("springForce");
  });

  it("formatPhysicsCode produces markdown", () => {
    const result = generatePhysicsCode("projectile", "android")!;
    const text = formatPhysicsCode(result, "android");
    expect(text).toContain("## Projectile Motion");
    expect(text).toContain("```kotlin");
    expect(text).toContain("### Available Physics Types");
  });
});
