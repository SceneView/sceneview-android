/**
 * Tests for the high-level API key helpers (`src/auth/api-keys.ts`).
 * These exercise generation, hashing, CRUD and validation against a
 * fresh in-memory D1 mock.
 */

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  createApiKey,
  generateApiKey,
  hashApiKey,
  listApiKeys,
  newUserId,
  revokeApiKey,
  validateApiKey,
} from "../src/auth/api-keys.js";
import { insertUser, updateUserTier } from "../src/db/users.js";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";

let mock: MockD1;

beforeEach(async () => {
  mock = await createMockD1();
});
afterEach(() => {
  mock.close();
});

async function seedUser(id = "usr_test", email = "test@example.com") {
  return insertUser(mock.db, { id, email });
}

describe("generateApiKey", () => {
  it("returns a valid sv_live_ plaintext with a 14-char prefix and sha256 hash", async () => {
    const key = await generateApiKey();
    expect(key.plaintext).toMatch(/^sv_live_[A-Z2-7]{32}$/);
    expect(key.prefix).toHaveLength(14);
    expect(key.prefix.startsWith("sv_live_")).toBe(true);
    expect(key.hash).toHaveLength(64);
    expect(key.hash).toMatch(/^[0-9a-f]{64}$/);
  });

  it("produces different keys on every call (high entropy)", async () => {
    const a = await generateApiKey();
    const b = await generateApiKey();
    expect(a.plaintext).not.toBe(b.plaintext);
    expect(a.hash).not.toBe(b.hash);
  });
});

describe("hashApiKey", () => {
  it("is deterministic for the same plaintext", async () => {
    const plaintext = "sv_live_ABCDEFGHIJKLMNOPQRSTUVWX23456789";
    const h1 = await hashApiKey(plaintext);
    const h2 = await hashApiKey(plaintext);
    expect(h1).toBe(h2);
  });

  it("differs across plaintexts", async () => {
    const h1 = await hashApiKey("sv_live_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    const h2 = await hashApiKey("sv_live_BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
    expect(h1).not.toBe(h2);
  });
});

describe("createApiKey / listApiKeys", () => {
  it("creates a row and returns the plaintext once, then lists it without plaintext", async () => {
    await seedUser();
    const { row, plaintext } = await createApiKey(mock.db, "usr_test", "CI");
    expect(plaintext.startsWith("sv_live_")).toBe(true);
    expect(row.name).toBe("CI");
    expect(row.key_hash).not.toBe(plaintext);
    expect(row.key_prefix).toBe(plaintext.slice(0, 14));

    const list = await listApiKeys(mock.db, "usr_test");
    expect(list).toHaveLength(1);
    // The list never contains plaintext, only the hash + prefix.
    expect(list[0].key_hash).toBe(row.key_hash);
    expect((list[0] as unknown as { plaintext?: string }).plaintext).toBeUndefined();
  });

  it("defaults the name to 'Default' when empty or whitespace", async () => {
    await seedUser();
    const { row: r1 } = await createApiKey(mock.db, "usr_test", "   ");
    expect(r1.name).toBe("Default");
    const { row: r2 } = await createApiKey(mock.db, "usr_test", undefined);
    expect(r2.name).toBe("Default");
  });
});

describe("revokeApiKey", () => {
  it("returns true on first revoke and false on replay", async () => {
    await seedUser();
    const { row } = await createApiKey(mock.db, "usr_test", "to-revoke");
    const first = await revokeApiKey(mock.db, row.id, "usr_test");
    expect(first).toBe(true);
    const second = await revokeApiKey(mock.db, row.id, "usr_test");
    expect(second).toBe(false);
  });

  it("returns false when trying to revoke a key owned by a different user", async () => {
    await seedUser("usr_alice", "alice@example.com");
    await seedUser("usr_bob", "bob@example.com");
    const { row } = await createApiKey(mock.db, "usr_alice", "alice-key");
    const result = await revokeApiKey(mock.db, row.id, "usr_bob");
    expect(result).toBe(false);
  });
});

describe("validateApiKey", () => {
  it("returns user + tier for a valid plaintext", async () => {
    await seedUser();
    await updateUserTier(mock.db, "usr_test", "pro");
    const { plaintext } = await createApiKey(mock.db, "usr_test", undefined);
    const validated = await validateApiKey(mock.db, plaintext);
    expect(validated).not.toBeNull();
    expect(validated?.userId).toBe("usr_test");
    expect(validated?.tier).toBe("pro");
  });

  it("returns null for an unknown plaintext", async () => {
    await seedUser();
    const validated = await validateApiKey(
      mock.db,
      "sv_live_DOESNOTEXIST1234567890DOESNOTEXIST12",
    );
    expect(validated).toBeNull();
  });

  it("returns null for a revoked key", async () => {
    await seedUser();
    const { row, plaintext } = await createApiKey(
      mock.db,
      "usr_test",
      "revoked-test",
    );
    await revokeApiKey(mock.db, row.id, "usr_test");
    const validated = await validateApiKey(mock.db, plaintext);
    expect(validated).toBeNull();
  });

  it("returns null for a plaintext without the sv_live_ prefix", async () => {
    await seedUser();
    const validated = await validateApiKey(mock.db, "not_a_key");
    expect(validated).toBeNull();
  });
});

describe("newUserId", () => {
  it("produces unique lowercase usr_ ids", () => {
    const a = newUserId();
    const b = newUserId();
    expect(a).toMatch(/^usr_[a-z2-7]+$/);
    expect(a).not.toBe(b);
  });
});
