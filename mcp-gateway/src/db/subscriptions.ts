/**
 * Typed queries against the `subscriptions` table.
 *
 * Subscription rows are a direct projection of Stripe subscription
 * objects and are written exclusively by the Stripe webhook handler
 * (sprint 2 / step 13).
 */

import { allRows, execute, firstRow } from "./client.js";
import type { SubscriptionRow, UserTier } from "./schema.js";

/** Returns the subscription with the given primary key, or null. */
export function getSubscriptionById(
  db: D1Database,
  id: string,
): Promise<SubscriptionRow | null> {
  return firstRow<SubscriptionRow>(
    db,
    "SELECT * FROM subscriptions WHERE id = ?1",
    id,
  );
}

/** Returns the subscription with the given Stripe subscription id, or null. */
export function getSubscriptionByStripeId(
  db: D1Database,
  stripeSubscriptionId: string,
): Promise<SubscriptionRow | null> {
  return firstRow<SubscriptionRow>(
    db,
    "SELECT * FROM subscriptions WHERE stripe_subscription_id = ?1",
    stripeSubscriptionId,
  );
}

/** Lists every subscription row belonging to a user. */
export function listSubscriptionsByUser(
  db: D1Database,
  userId: string,
): Promise<SubscriptionRow[]> {
  return allRows<SubscriptionRow>(
    db,
    "SELECT * FROM subscriptions WHERE user_id = ?1 ORDER BY created_at DESC",
    userId,
  );
}

/** Inserts a subscription row. */
export async function insertSubscription(
  db: D1Database,
  row: {
    id: string;
    userId: string;
    stripeSubscriptionId: string;
    stripePriceId: string;
    tier: UserTier;
    status: string;
    currentPeriodEnd: number;
    cancelAtPeriodEnd?: boolean;
  },
): Promise<SubscriptionRow> {
  const now = Date.now();
  const cancel = row.cancelAtPeriodEnd ? 1 : 0;
  await execute(
    db,
    `INSERT INTO subscriptions
       (id, user_id, stripe_subscription_id, stripe_price_id, tier, status,
        current_period_end, cancel_at_period_end, created_at, updated_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?9)`,
    row.id,
    row.userId,
    row.stripeSubscriptionId,
    row.stripePriceId,
    row.tier,
    row.status,
    row.currentPeriodEnd,
    cancel,
    now,
  );
  return {
    id: row.id,
    user_id: row.userId,
    stripe_subscription_id: row.stripeSubscriptionId,
    stripe_price_id: row.stripePriceId,
    tier: row.tier,
    status: row.status,
    current_period_end: row.currentPeriodEnd,
    cancel_at_period_end: cancel,
    created_at: now,
    updated_at: now,
  };
}

/** Updates status + period end for a subscription. */
export async function updateSubscriptionStatus(
  db: D1Database,
  id: string,
  patch: {
    status: string;
    currentPeriodEnd: number;
    cancelAtPeriodEnd: boolean;
  },
): Promise<void> {
  await execute(
    db,
    `UPDATE subscriptions
        SET status = ?1,
            current_period_end = ?2,
            cancel_at_period_end = ?3,
            updated_at = ?4
      WHERE id = ?5`,
    patch.status,
    patch.currentPeriodEnd,
    patch.cancelAtPeriodEnd ? 1 : 0,
    Date.now(),
    id,
  );
}
