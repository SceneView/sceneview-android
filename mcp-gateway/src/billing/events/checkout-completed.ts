/**
 * Handler for the `checkout.session.completed` Stripe webhook event.
 *
 * Steps:
 *   1. Look up the user by `client_reference_id` (our user row id).
 *   2. Persist the Stripe customer id on the user row if needed.
 *   3. Retrieve the subscription from Stripe to read its price id and
 *      status (the event payload only contains a subscription id).
 *   4. Upsert a `subscriptions` row with the resolved tier.
 *   5. Update `users.tier`.
 *   6. Invalidate the KV auth cache for every key owned by the user so
 *      the next `/mcp` call sees the new tier within the 300 s TTL.
 */

import type { Env } from "../../env.js";
import type { StripeEvent } from "../webhook.js";
import {
  getUserById,
  updateUserStripeCustomer,
  updateUserTier,
} from "../../db/users.js";
import {
  getSubscriptionByStripeId,
  insertSubscription,
  updateSubscriptionStatus,
} from "../../db/subscriptions.js";
import { listApiKeysByUser } from "../../db/api-keys.js";
import { retrieveSubscription } from "../stripe-client.js";
import { getTierForPriceId } from "../tiers.js";

/** Shape of the `data.object` for a `checkout.session.completed` event. */
interface CheckoutSessionObject {
  id: string;
  customer?: string | null;
  client_reference_id?: string | null;
  subscription?: string | null;
  mode?: string;
  metadata?: Record<string, string>;
}

/** New subscription-id allocator — `sub_` + 12 base32 chars. */
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

export async function handleCheckoutCompleted(
  env: Env,
  event: StripeEvent,
): Promise<void> {
  const session = event.data.object as CheckoutSessionObject;
  if (!session) return;

  const userId =
    session.client_reference_id ??
    session.metadata?.sv_user_id ??
    null;
  if (!userId) return;

  const user = await getUserById(env.DB, userId);
  if (!user) return;

  // Persist Stripe customer id on the user row.
  if (session.customer && user.stripe_customer_id !== session.customer) {
    await updateUserStripeCustomer(env.DB, user.id, session.customer);
  }

  // Retrieve the subscription so we can map the tier and status.
  if (!session.subscription || !env.STRIPE_SECRET_KEY) return;
  const sub = await retrieveSubscription(
    env.STRIPE_SECRET_KEY,
    session.subscription,
  );
  const priceId = sub.items?.data?.[0]?.price?.id ?? "";
  const tier = getTierForPriceId(env, priceId);
  if (!tier) return;

  // Upsert the subscription row.
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

  // Update user tier if it changed.
  if (user.tier !== tier) {
    await updateUserTier(env.DB, user.id, tier);
  }

  // Invalidate the KV auth cache so the new tier is picked up quickly.
  await invalidateAuthCacheForUser(env, user.id);
}

/**
 * Best-effort: walks every api_keys row owned by the user and deletes
 * their `auth:{hash}` entry in KV. A failure to delete is not fatal —
 * the 300 s TTL on every entry acts as a safety net.
 */
async function invalidateAuthCacheForUser(
  env: Env,
  userId: string,
): Promise<void> {
  const keys = await listApiKeysByUser(env.DB, userId);
  for (const key of keys) {
    try {
      await env.RL_KV.delete(`auth:${key.key_hash}`);
    } catch {
      // Ignore — the TTL is the real upper bound on propagation.
    }
  }
}
