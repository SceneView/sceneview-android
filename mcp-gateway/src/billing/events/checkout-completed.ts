/**
 * Handler for the `checkout.session.completed` Stripe webhook event.
 *
 * Flow (MVP — no dashboard auth):
 *   1. Read the session `id`, `customer`, `customer_details.email`,
 *      `customer_email`, and `subscription` from the event payload.
 *   2. Resolve the buyer email — required to identify the user. Falls
 *      back to re-fetching the Checkout Session from Stripe so the
 *      handler keeps working even when the webhook payload is sparse.
 *   3. Upsert a `users` row by email (self-serve signup).
 *   4. Persist the Stripe customer id on the user row.
 *   5. Retrieve the subscription from Stripe, resolve its tier from the
 *      price id, upsert a `subscriptions` row, and bump `users.tier`.
 *   6. Generate a fresh API key (`sv_live_...`), persist only the hash
 *      in D1, and store the plaintext under `checkout_key:{session_id}`
 *      in KV for 24h so the `/checkout/success` page can display it
 *      exactly once to the buyer.
 *   7. Invalidate the KV auth cache for every key owned by the user so
 *      the new tier is picked up within the 300 s TTL.
 *
 * The plaintext API key NEVER appears in logs, never lands in D1, and
 * can only be retrieved once: the `/checkout/success` handler deletes
 * the KV entry on first read.
 */

import type { Env } from "../../env.js";
import type { StripeEvent } from "../webhook.js";
import {
  getUserByEmail,
  getUserById,
  insertUser,
  updateUserStripeCustomer,
  updateUserTier,
} from "../../db/users.js";
import {
  getSubscriptionByStripeId,
  insertSubscription,
  updateSubscriptionStatus,
} from "../../db/subscriptions.js";
import { listApiKeysByUser } from "../../db/api-keys.js";
import {
  retrieveCheckoutSession,
  retrieveSubscription,
} from "../stripe-client.js";
import { getTierForPriceId } from "../tiers.js";
import { createApiKey, newUserId } from "../../auth/api-keys.js";

/** Shape of the `data.object` for a `checkout.session.completed` event. */
interface CheckoutSessionObject {
  id: string;
  customer?: string | null;
  customer_email?: string | null;
  customer_details?: {
    email?: string | null;
    name?: string | null;
  } | null;
  client_reference_id?: string | null;
  subscription?: string | null;
  mode?: string;
  metadata?: Record<string, string>;
}

/** KV key prefix used to hand off the plaintext API key to the success page. */
export const CHECKOUT_KEY_KV_PREFIX = "checkout_key:";

/** 24 hours — gives the user plenty of time to refresh the success page. */
export const CHECKOUT_KEY_TTL_SECONDS = 86_400;

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
  if (!session || !session.id) return;

  // ── 1. Resolve the buyer email ───────────────────────────────────────────
  const email = await resolveBuyerEmail(env, session);
  if (!email) {
    console.warn(
      `[checkout-completed] session ${session.id} has no email — skipping`,
    );
    return;
  }

  // ── 2. Upsert the user (self-serve signup via Stripe) ───────────────────
  let user = await resolveUser(env, email, session.client_reference_id);
  if (!user) {
    user = await insertUser(env.DB, {
      id: newUserId(),
      email,
    });
  }

  // ── 3. Persist the Stripe customer id ──────────────────────────────────
  if (session.customer && user.stripe_customer_id !== session.customer) {
    await updateUserStripeCustomer(env.DB, user.id, session.customer);
    user = { ...user, stripe_customer_id: session.customer };
  }

  // ── 4. Resolve + upsert the subscription ───────────────────────────────
  //
  // The webhook payload's `subscription` field is sometimes null on the
  // first `checkout.session.completed` delivery, even for mode=subscription
  // checkouts — Stripe's own docs note that the subscription is expanded
  // asynchronously. When that happens we re-fetch the session from the
  // REST API (same fallback we use for `email`) so we don't lose the
  // mapping and leave the user stuck on the free tier.
  if (!env.STRIPE_SECRET_KEY) {
    console.warn(
      `[checkout-completed] session ${session.id}: STRIPE_SECRET_KEY not set, skipping`,
    );
    return;
  }
  let subscriptionId: string | null = session.subscription ?? null;
  if (!subscriptionId) {
    try {
      const full = await retrieveCheckoutSession(env.STRIPE_SECRET_KEY, session.id);
      subscriptionId = full.subscription ?? null;
    } catch (err) {
      console.error(
        `[checkout-completed] failed to re-fetch session ${session.id} for subscription`,
        err,
      );
    }
  }
  if (!subscriptionId) {
    console.warn(
      `[checkout-completed] session ${session.id} has no subscription id (even after re-fetch)`,
    );
    return;
  }
  const sub = await retrieveSubscription(
    env.STRIPE_SECRET_KEY,
    subscriptionId,
  );
  const priceId = sub.items?.data?.[0]?.price?.id ?? "";
  const tier = getTierForPriceId(env, priceId);
  if (!tier) {
    console.warn(
      `[checkout-completed] unknown price id ${priceId} — skipping`,
    );
    return;
  }

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

  // ── 5. Bump the user tier ──────────────────────────────────────────────
  if (user.tier !== tier) {
    await updateUserTier(env.DB, user.id, tier);
  }

  // ── 6. Generate a brand new API key and stash the plaintext in KV ──────
  const created = await createApiKey(env.DB, user.id, "Checkout");
  try {
    await env.RL_KV.put(
      `${CHECKOUT_KEY_KV_PREFIX}${session.id}`,
      JSON.stringify({
        plaintext: created.plaintext,
        prefix: created.row.key_prefix,
        name: created.row.name,
        tier,
        email,
        createdAt: Date.now(),
      }),
      { expirationTtl: CHECKOUT_KEY_TTL_SECONDS },
    );
    console.info(
      `[checkout-completed] generated API key ${created.row.key_prefix} for ${email} (session ${session.id})`,
    );
  } catch (err) {
    // If the KV put fails we still want the user provisioned in D1.
    // The user can contact support and we can regenerate a key for them.
    console.error(
      `[checkout-completed] failed to stash key in KV for session ${session.id}`,
      err,
    );
  }

  // ── 7. Invalidate the KV auth cache so the new tier is picked up fast ──
  await invalidateAuthCacheForUser(env, user.id);
}

/**
 * Best-effort email resolution for a Checkout Session.
 *
 * Checks the webhook payload first (the fast path) and falls back to
 * re-fetching the Checkout Session from Stripe when the payload is too
 * sparse. Returns `null` only when Stripe itself has no email for the
 * session — in practice this never happens for a completed session
 * because Stripe always collects an email during checkout.
 */
async function resolveBuyerEmail(
  env: Env,
  session: CheckoutSessionObject,
): Promise<string | null> {
  const fromPayload =
    session.customer_details?.email?.trim() ||
    session.customer_email?.trim() ||
    null;
  if (fromPayload) return fromPayload.toLowerCase();

  if (!env.STRIPE_SECRET_KEY) return null;
  try {
    const full = await retrieveCheckoutSession(env.STRIPE_SECRET_KEY, session.id);
    const email =
      full.customer_details?.email?.trim() ||
      full.customer_email?.trim() ||
      null;
    return email ? email.toLowerCase() : null;
  } catch (err) {
    console.error(
      `[checkout-completed] failed to retrieve session ${session.id}`,
      err,
    );
    return null;
  }
}

/**
 * Resolves the buyer's user row. Prefers `client_reference_id` for
 * backward compatibility (legacy magic-link flow) then falls back to
 * looking up by email.
 */
async function resolveUser(
  env: Env,
  email: string,
  clientReferenceId: string | null | undefined,
) {
  if (clientReferenceId) {
    const byId = await getUserById(env.DB, clientReferenceId);
    if (byId) return byId;
  }
  const byEmail = await getUserByEmail(env.DB, email);
  return byEmail;
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
