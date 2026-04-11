/**
 * D1 layer tests.
 *
 * These exercise the `src/db/*` repository functions against an
 * in-memory SQLite database seeded with the real migrations under
 * `mcp-gateway/migrations/`. See `test/helpers/mock-d1.ts` for
 * why we avoid Miniflare + vitest-pool-workers.
 */

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { createMockD1, type MockD1 } from "./helpers/mock-d1.js";

import {
  getUserByEmail,
  getUserById,
  getUserByStripeCustomerId,
  insertUser,
  updateUserStripeCustomer,
  updateUserTier,
} from "../src/db/users.js";
import {
  getSubscriptionByStripeId,
  insertSubscription,
  listSubscriptionsByUser,
  updateSubscriptionStatus,
} from "../src/db/subscriptions.js";
import {
  getApiKeyByHash,
  getApiKeyById,
  insertApiKey,
  listApiKeysByUser,
  revokeApiKeyRow,
  touchApiKey,
} from "../src/db/api-keys.js";
import {
  countSuccessfulUsageInMonth,
  countUsageInMonth,
  insertUsageRecord,
  monthBucket,
} from "../src/db/usage.js";
import {
  consumeMagicLink,
  deleteExpiredMagicLinks,
  getMagicLink,
  insertMagicLink,
} from "../src/db/magic-links.js";

let mock: MockD1;

beforeEach(async () => {
  mock = await createMockD1();
});
afterEach(() => {
  mock.close();
});

describe("db/users", () => {
  it("insert + getById + getByEmail", async () => {
    await insertUser(mock.db, {
      id: "usr_test1",
      email: "alice@example.com",
    });
    const byId = await getUserById(mock.db, "usr_test1");
    expect(byId?.email).toBe("alice@example.com");
    expect(byId?.tier).toBe("free");
    const byEmail = await getUserByEmail(mock.db, "alice@example.com");
    expect(byEmail?.id).toBe("usr_test1");
  });

  it("updateUserTier bumps tier", async () => {
    await insertUser(mock.db, { id: "usr_t2", email: "bob@example.com" });
    await updateUserTier(mock.db, "usr_t2", "pro");
    const u = await getUserById(mock.db, "usr_t2");
    expect(u?.tier).toBe("pro");
  });

  it("updateUserStripeCustomer + getUserByStripeCustomerId", async () => {
    await insertUser(mock.db, { id: "usr_t3", email: "carol@example.com" });
    await updateUserStripeCustomer(mock.db, "usr_t3", "cus_XYZ");
    const u = await getUserByStripeCustomerId(mock.db, "cus_XYZ");
    expect(u?.id).toBe("usr_t3");
  });
});

describe("db/subscriptions", () => {
  it("insert + getByStripeId + listByUser", async () => {
    await insertUser(mock.db, { id: "usr_s1", email: "s1@example.com" });
    await insertSubscription(mock.db, {
      id: "sub_s1",
      userId: "usr_s1",
      stripeSubscriptionId: "stripe_sub_1",
      stripePriceId: "price_pro_monthly",
      tier: "pro",
      status: "active",
      currentPeriodEnd: Date.now() + 86_400_000,
    });
    const byStripe = await getSubscriptionByStripeId(mock.db, "stripe_sub_1");
    expect(byStripe?.id).toBe("sub_s1");
    const byUser = await listSubscriptionsByUser(mock.db, "usr_s1");
    expect(byUser).toHaveLength(1);
  });

  it("updateSubscriptionStatus flips to canceled", async () => {
    await insertUser(mock.db, { id: "usr_s2", email: "s2@example.com" });
    await insertSubscription(mock.db, {
      id: "sub_s2",
      userId: "usr_s2",
      stripeSubscriptionId: "stripe_sub_2",
      stripePriceId: "price_pro_monthly",
      tier: "pro",
      status: "active",
      currentPeriodEnd: 0,
    });
    await updateSubscriptionStatus(mock.db, "sub_s2", {
      status: "canceled",
      currentPeriodEnd: 0,
      cancelAtPeriodEnd: true,
    });
    const row = await getSubscriptionByStripeId(mock.db, "stripe_sub_2");
    expect(row?.status).toBe("canceled");
    expect(row?.cancel_at_period_end).toBe(1);
  });
});

describe("db/api-keys", () => {
  it("insert + getByHash + getById + listByUser", async () => {
    await insertUser(mock.db, { id: "usr_k1", email: "k1@example.com" });
    await insertApiKey(mock.db, {
      id: "key_k1",
      userId: "usr_k1",
      name: "Default",
      keyHash: "hash_k1",
      keyPrefix: "sv_live_abcdef",
    });
    const byHash = await getApiKeyByHash(mock.db, "hash_k1");
    expect(byHash?.name).toBe("Default");
    const byId = await getApiKeyById(mock.db, "key_k1");
    expect(byId?.user_id).toBe("usr_k1");
    const list = await listApiKeysByUser(mock.db, "usr_k1");
    expect(list).toHaveLength(1);
  });

  it("revokeApiKeyRow flips revoked_at and ignores replay", async () => {
    await insertUser(mock.db, { id: "usr_k2", email: "k2@example.com" });
    await insertApiKey(mock.db, {
      id: "key_k2",
      userId: "usr_k2",
      name: "CI",
      keyHash: "hash_k2",
      keyPrefix: "sv_live_ghijkl",
    });
    const firstChanges = await revokeApiKeyRow(mock.db, "key_k2", "usr_k2");
    expect(firstChanges).toBe(1);
    const row = await getApiKeyById(mock.db, "key_k2");
    expect(row?.revoked_at).not.toBeNull();
    // Replay must not update again.
    const secondChanges = await revokeApiKeyRow(mock.db, "key_k2", "usr_k2");
    expect(secondChanges).toBe(0);
  });

  it("touchApiKey bumps last_used_at", async () => {
    await insertUser(mock.db, { id: "usr_k3", email: "k3@example.com" });
    await insertApiKey(mock.db, {
      id: "key_k3",
      userId: "usr_k3",
      name: "prod",
      keyHash: "hash_k3",
      keyPrefix: "sv_live_mnopqr",
    });
    await touchApiKey(mock.db, "key_k3");
    const row = await getApiKeyById(mock.db, "key_k3");
    expect(row?.last_used_at).not.toBeNull();
  });
});

describe("db/usage", () => {
  it("insert + count by month + count successful by month", async () => {
    await insertUser(mock.db, { id: "usr_u1", email: "u1@example.com" });
    await insertApiKey(mock.db, {
      id: "key_u1",
      userId: "usr_u1",
      name: "Default",
      keyHash: "hash_u1",
      keyPrefix: "sv_live_uuuuuu",
    });
    const bucket = monthBucket();
    await insertUsageRecord(mock.db, {
      apiKeyId: "key_u1",
      userId: "usr_u1",
      toolName: "list_samples",
      tierRequired: "free",
      status: "ok",
    });
    await insertUsageRecord(mock.db, {
      apiKeyId: "key_u1",
      userId: "usr_u1",
      toolName: "generate_scene",
      tierRequired: "pro",
      status: "denied",
    });
    const total = await countUsageInMonth(mock.db, "key_u1", bucket);
    expect(total).toBe(2);
    const ok = await countSuccessfulUsageInMonth(mock.db, "key_u1", bucket);
    expect(ok).toBe(1);
  });

  it("monthBucket formats as YYYY-MM", () => {
    const bucket = monthBucket(Date.UTC(2026, 3, 11)); // April 2026
    expect(bucket).toBe("2026-04");
  });
});

describe("db/magic-links", () => {
  it("insert + get + consume (replay-safe)", async () => {
    const expiresAt = Date.now() + 15 * 60 * 1000;
    await insertMagicLink(mock.db, {
      tokenHash: "magic_hash_1",
      email: "user@example.com",
      expiresAt,
    });
    const row = await getMagicLink(mock.db, "magic_hash_1");
    expect(row?.email).toBe("user@example.com");
    expect(row?.consumed_at).toBeNull();

    const first = await consumeMagicLink(mock.db, "magic_hash_1");
    expect(first).toBe(1);
    const second = await consumeMagicLink(mock.db, "magic_hash_1");
    expect(second).toBe(0);
  });

  it("deleteExpiredMagicLinks removes expired + consumed rows", async () => {
    await insertMagicLink(mock.db, {
      tokenHash: "expired",
      email: "e@example.com",
      expiresAt: 0,
    });
    await insertMagicLink(mock.db, {
      tokenHash: "fresh",
      email: "f@example.com",
      expiresAt: Date.now() + 900_000,
    });
    const deleted = await deleteExpiredMagicLinks(mock.db, Date.now());
    expect(deleted).toBeGreaterThanOrEqual(1);
    const fresh = await getMagicLink(mock.db, "fresh");
    expect(fresh).not.toBeNull();
  });
});
