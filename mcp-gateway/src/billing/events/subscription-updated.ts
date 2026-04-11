/**
 * Handler for `customer.subscription.updated` and `customer.subscription.created`.
 *
 * Keeps the `subscriptions` row in sync with Stripe and updates the
 * owning user's tier on plan changes. Unknown customer ids are ignored
 * silently: Stripe may fire this before the Checkout completion event
 * for first-time signups, in which case `checkout-completed` will
 * backfill the row.
 */

import type { Env } from "../../env.js";
import type { StripeEvent } from "../webhook.js";
import {
  getSubscriptionByStripeId,
  insertSubscription,
  updateSubscriptionStatus,
} from "../../db/subscriptions.js";
import {
  getUserByStripeCustomerId,
  updateUserTier,
} from "../../db/users.js";
import { listApiKeysByUser } from "../../db/api-keys.js";
import { getTierForPriceId } from "../tiers.js";

/** Shape of the `data.object` for a subscription event. */
interface SubscriptionObject {
  id: string;
  customer: string;
  status: string;
  cancel_at_period_end: boolean;
  current_period_end: number;
  items?: {
    data?: Array<{ price?: { id?: string } }>;
  };
}

function newSubscriptionId(): string {
  const bytes = new Uint8Array(8);
  crypto.getRandomValues(bytes);
  let value = 0;
  let bits = 0;
  let out = "";
  const ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += ALPHABET[(value >> bits) & 31];
    }
  }
  return `sub_${out.slice(0, 12)}`;
}

export async function handleSubscriptionUpdated(
  env: Env,
  event: StripeEvent,
): Promise<void> {
  const sub = event.data.object as SubscriptionObject;
  if (!sub?.id) return;
  const user = await getUserByStripeCustomerId(env.DB, sub.customer);
  if (!user) return;

  const priceId = sub.items?.data?.[0]?.price?.id ?? "";
  const tier = getTierForPriceId(env, priceId);
  if (!tier) return;

  const existing = await getSubscriptionByStripeId(env.DB, sub.id);
  if (existing) {
    await updateSubscriptionStatus(env.DB, existing.id, {
      status: sub.status,
      currentPeriodEnd: sub.current_period_end * 1000,
      cancelAtPeriodEnd: !!sub.cancel_at_period_end,
    });
  } else {
    await insertSubscription(env.DB, {
      id: newSubscriptionId(),
      userId: user.id,
      stripeSubscriptionId: sub.id,
      stripePriceId: priceId,
      tier,
      status: sub.status,
      currentPeriodEnd: sub.current_period_end * 1000,
      cancelAtPeriodEnd: !!sub.cancel_at_period_end,
    });
  }

  if (user.tier !== tier) {
    await updateUserTier(env.DB, user.id, tier);
    // Invalidate KV cache so the tier change propagates quickly.
    const keys = await listApiKeysByUser(env.DB, user.id);
    for (const key of keys) {
      try {
        await env.RL_KV.delete(`auth:${key.key_hash}`);
      } catch {
        // Ignore — the TTL is the safety net.
      }
    }
  }
}
