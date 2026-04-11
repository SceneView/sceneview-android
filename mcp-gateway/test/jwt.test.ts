/**
 * Unit tests for `src/auth/jwt.ts`.
 */

import { describe, expect, it } from "vitest";
import { signJwt, verifyJwt } from "../src/auth/jwt.js";

describe("jwt", () => {
  const SECRET = "test-secret-32-chars-for-hs256-ok";

  it("signs and verifies a round-trip token", async () => {
    const token = await signJwt(SECRET, { sub: "usr_round_trip" });
    expect(token.split(".")).toHaveLength(3);
    const verified = await verifyJwt(SECRET, token);
    expect(verified).not.toBeNull();
    expect(verified!.claims.sub).toBe("usr_round_trip");
    expect(verified!.claims.exp).toBeGreaterThan(verified!.claims.iat);
  });

  it("produces deterministic signatures for identical inputs", async () => {
    const now = Math.floor(Date.now() / 1000);
    const a = await signJwt(SECRET, { sub: "u", nowSeconds: now });
    const b = await signJwt(SECRET, { sub: "u", nowSeconds: now });
    expect(a).toBe(b);
  });

  it("rejects tokens signed with a different secret", async () => {
    const token = await signJwt(SECRET, { sub: "u" });
    const verified = await verifyJwt("different-secret-value-1234567890", token);
    expect(verified).toBeNull();
  });

  it("rejects malformed tokens", async () => {
    const verified1 = await verifyJwt(SECRET, "not-a-jwt");
    const verified2 = await verifyJwt(SECRET, "a.b");
    const verified3 = await verifyJwt(SECRET, "a.b.c.d");
    expect(verified1).toBeNull();
    expect(verified2).toBeNull();
    expect(verified3).toBeNull();
  });

  it("rejects expired tokens", async () => {
    const past = Math.floor(Date.now() / 1000) - 100;
    const token = await signJwt(SECRET, {
      sub: "u",
      nowSeconds: past,
      ttlSeconds: 50,
    });
    const verified = await verifyJwt(SECRET, token);
    expect(verified).toBeNull();
  });

  it("rejects tokens whose payload has been tampered with", async () => {
    const token = await signJwt(SECRET, { sub: "alice" });
    const [header, , signature] = token.split(".");
    // Re-encode a payload with a different sub but keep the old signature.
    const badPayload = btoa(
      JSON.stringify({ sub: "bob", iat: 1, exp: 9999999999 }),
    )
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
    const tampered = `${header}.${badPayload}.${signature}`;
    const verified = await verifyJwt(SECRET, tampered);
    expect(verified).toBeNull();
  });

  it("rejects tokens whose signature has been swapped", async () => {
    const a = await signJwt(SECRET, { sub: "alice" });
    const b = await signJwt(SECRET, { sub: "bob" });
    const [aHeader, aPayload] = a.split(".");
    const [, , bSig] = b.split(".");
    const frankenstein = `${aHeader}.${aPayload}.${bSig}`;
    const verified = await verifyJwt(SECRET, frankenstein);
    expect(verified).toBeNull();
  });

  it("throws when signing with an empty secret", async () => {
    await expect(
      signJwt("", { sub: "u" }),
    ).rejects.toThrow(/empty secret/);
  });
});
