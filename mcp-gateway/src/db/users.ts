/**
 * Typed queries against the `users` table.
 *
 * All identifiers follow the `usr_xxx` convention generated upstream
 * (step 7's `auth/api-keys.ts` or the dashboard magic-link flow). We do
 * not generate ids here to keep this module dependency-free.
 */

import { allRows, execute, firstRow } from "./client.js";
import type { UserRow, UserTier } from "./schema.js";

/** Returns a user by primary key, or null. */
export function getUserById(
  db: D1Database,
  id: string,
): Promise<UserRow | null> {
  return firstRow<UserRow>(db, "SELECT * FROM users WHERE id = ?1", id);
}

/** Returns a user by unique email, or null. */
export function getUserByEmail(
  db: D1Database,
  email: string,
): Promise<UserRow | null> {
  return firstRow<UserRow>(
    db,
    "SELECT * FROM users WHERE email = ?1",
    email,
  );
}

/** Returns a user by Stripe customer id, or null. */
export function getUserByStripeCustomerId(
  db: D1Database,
  stripeCustomerId: string,
): Promise<UserRow | null> {
  return firstRow<UserRow>(
    db,
    "SELECT * FROM users WHERE stripe_customer_id = ?1",
    stripeCustomerId,
  );
}

/** Inserts a new user row. Fails if the id or email is already taken. */
export async function insertUser(
  db: D1Database,
  row: {
    id: string;
    email: string;
    tier?: UserTier;
    stripeCustomerId?: string | null;
  },
): Promise<UserRow> {
  const now = Date.now();
  const tier: UserTier = row.tier ?? "free";
  await execute(
    db,
    `INSERT INTO users (id, email, stripe_customer_id, tier, created_at, updated_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?5)`,
    row.id,
    row.email,
    row.stripeCustomerId ?? null,
    tier,
    now,
  );
  return {
    id: row.id,
    email: row.email,
    stripe_customer_id: row.stripeCustomerId ?? null,
    tier,
    created_at: now,
    updated_at: now,
  };
}

/** Updates the tier of a user and bumps `updated_at`. */
export async function updateUserTier(
  db: D1Database,
  id: string,
  tier: UserTier,
): Promise<void> {
  await execute(
    db,
    "UPDATE users SET tier = ?1, updated_at = ?2 WHERE id = ?3",
    tier,
    Date.now(),
    id,
  );
}

/** Updates the Stripe customer id of a user (set after first checkout). */
export async function updateUserStripeCustomer(
  db: D1Database,
  id: string,
  stripeCustomerId: string,
): Promise<void> {
  await execute(
    db,
    "UPDATE users SET stripe_customer_id = ?1, updated_at = ?2 WHERE id = ?3",
    stripeCustomerId,
    Date.now(),
    id,
  );
}

/** Returns every user (admin / tests / backfill). */
export function listAllUsers(db: D1Database): Promise<UserRow[]> {
  return allRows<UserRow>(db, "SELECT * FROM users ORDER BY created_at DESC");
}
