/**
 * Unit tests for `src/auth/magic-link.ts` primitives.
 *
 * The /login and /auth/verify routes are disabled in the MVP (they
 * return 503 — see `test/dashboard.test.ts`). The helpers tested here
 * are kept on disk so a future dashboard sprint can re-enable them
 * without reimplementing the D1 schema and the hash-compare logic.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  consumeMagicLinkToken,
  createMagicLink,
  isValidEmail,
  sendMagicLinkEmail,
} from "../src/auth/magic-link.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";
import { MockKv } from "./helpers/mock-kv.js";
import { insertMagicLink, getMagicLink } from "../src/db/magic-links.js";
import { hashApiKey } from "../src/auth/api-keys.js";

let mock: MockD1;
let kv: MockKv;

beforeEach(async () => {
  mock = await createMockD1();
  kv = new MockKv();
});
afterEach(() => {
  mock.close();
  void kv;
});

describe("isValidEmail", () => {
  it("accepts reasonable addresses", () => {
    expect(isValidEmail("alice@example.com")).toBe(true);
    expect(isValidEmail("ALICE@example.com")).toBe(true);
    expect(isValidEmail("a.b+tag@sub.example.co")).toBe(true);
  });

  it("rejects junk", () => {
    expect(isValidEmail("")).toBe(false);
    expect(isValidEmail("alice")).toBe(false);
    expect(isValidEmail("alice@")).toBe(false);
    expect(isValidEmail("@example.com")).toBe(false);
    expect(isValidEmail("alice@@example.com")).toBe(false);
    expect(isValidEmail("alice@example")).toBe(false);
  });
});

describe("createMagicLink + consumeMagicLinkToken", () => {
  it("generates a token, stores its hash, and consumes it exactly once", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "consume@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    expect(link.url).toContain("/auth/verify?token=");
    expect(link.token).toMatch(/^[A-Z2-7]{40}$/);
    expect(link.expiresAt).toBeGreaterThan(Date.now());

    const first = await consumeMagicLinkToken(mock.db, link.token);
    expect(first).toBe("consume@example.com");

    const replay = await consumeMagicLinkToken(mock.db, link.token);
    expect(replay).toBeNull();
  });

  it("normalizes the email to lowercase and trims whitespace", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "  Upper@Example.com  ",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const email = await consumeMagicLinkToken(mock.db, link.token);
    expect(email).toBe("upper@example.com");
  });

  it("rejects expired tokens", async () => {
    const token = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    const tokenHash = await hashApiKey(token);
    await insertMagicLink(mock.db, {
      tokenHash,
      email: "expired@example.com",
      expiresAt: Date.now() - 1000,
    });
    const email = await consumeMagicLinkToken(mock.db, token);
    expect(email).toBeNull();
  });

  it("rejects unknown tokens without leaking", async () => {
    const email = await consumeMagicLinkToken(
      mock.db,
      "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
    );
    expect(email).toBeNull();
  });

  it("throws on invalid email", async () => {
    await expect(
      createMagicLink({
        db: mock.db,
        email: "not-an-email",
        baseUrl: "https://sceneview-mcp.workers.dev",
      }),
    ).rejects.toThrow();
  });

  it("strips trailing slashes from baseUrl", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "slash@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev///",
    });
    expect(link.url).toMatch(
      /^https:\/\/sceneview-mcp\.workers\.dev\/auth\/verify\?/,
    );
  });

  it("stores the magic_link row hashed — a DB leak cannot reveal the token", async () => {
    const link = await createMagicLink({
      db: mock.db,
      email: "leak@example.com",
      baseUrl: "https://sceneview-mcp.workers.dev",
    });
    const hash = await hashApiKey(link.token);
    const row = await getMagicLink(mock.db, hash);
    expect(row).not.toBeNull();
    expect(row!.email).toBe("leak@example.com");
    // The raw token must NOT appear anywhere in the row.
    const serialized = JSON.stringify(row);
    expect(serialized).not.toContain(link.token);
  });
});

describe("sendMagicLinkEmail (MVP no-op)", () => {
  it("never touches the network — the Resend integration is disabled in the MVP", async () => {
    const spy = vi.spyOn(globalThis, "fetch");
    await sendMagicLinkEmail({
      apiKey: undefined,
      from: "test@example.com",
      to: "user@example.com",
      url: "https://x/y",
    });
    await sendMagicLinkEmail({
      apiKey: "re_test_key",
      from: "f@example.com",
      to: "t@example.com",
      url: "https://x/y",
    });
    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
  });
});
