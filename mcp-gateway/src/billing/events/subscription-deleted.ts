/**
 * Handler for `customer.subscription.deleted`.
 *
 * Downgrades the user back to the free tier and marks the subscription
 * row as canceled. Called when a Stripe subscription is fully ended
 * (either by the user clicking "cancel immediately" in the portal or
 * after the grace period has elapsed).
 */

import type { Env } from "../../env.js";
import type { StripeEvent } from "../webhook.js";
import {
  getSubscriptionByStripeId,
  updateSubscriptionStatus,
} from "../../db/subscriptions.js";
import {
  getUserByStripeCustomerId,
  updateUserTier,
} from "../../db/users.js";
import { listApiKeysByUser } from "../../db/api-keys.js";

interface SubscriptionObject {
  id: string;
  customer: string;
  current_period_end?: number;
  cancel_at_period_end?: boolean;
  status?: string;
}

export async function handleSubscriptionDeleted(
  env: Env,
  event: StripeEvent,
): Promise<void> {
  const sub = event.data.object as SubscriptionObject;
  if (!sub?.id) return;

  const user = await getUserByStripeCustomerId(env.DB, sub.customer);
  if (!user) return;

  const existing = await getSubscriptionByStripeId(env.DB, sub.id);
  if (existing) {
    await updateSubscriptionStatus(env.DB, existing.id, {
      status: "canceled",
      currentPeriodEnd: (sub.current_period_end ?? 0) * 1000,
      cancelAtPeriodEnd: true,
    });
  }

  if (user.tier !== "free") {
    await updateUserTier(env.DB, user.id, "free");
    const keys = await listApiKeysByUser(env.DB, user.id);
    for (const key of keys) {
      try {
        await env.RL_KV.delete(`auth:${key.key_hash}`);
      } catch {
        // Ignore.
      }
    }
  }
}
