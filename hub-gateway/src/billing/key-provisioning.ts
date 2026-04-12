/**
 * API key provisioning for the hub gateway checkout flow.
 *
 * When Stripe sends `checkout.session.completed`, the webhook handler
 * calls `provisionApiKey(env, email, tier, sessionId)` which:
 *
 *   1. Upserts the user in D1 (existing → update tier, new → insert)
 *   2. Generates a fresh `sv_live_` API key (same format as Gateway #1)
 *   3. Stores the key hash in D1 `api_keys` table
 *   4. Stores the plaintext + metadata under `checkout_key:{session_id}`
 *      in KV with a 24h TTL so `/checkout/success` can display it once
 *
 * The plaintext NEVER appears in logs, never lands in D1, and can only
 * be retrieved once: the /checkout/success handler deletes the KV entry
 * on first read.
 *
 * Key format: `sv_live_` + 32 chars of base32 (160 bits of entropy),
 * matching Gateway #1 exactly so a single key works on both gateways.
 */

import type { Env } from "../env.js";
import type { UserTier } from "../db/schema.js";

/** KV key prefix for the single-use API key handoff. */
export const CHECKOUT_KEY_KV_PREFIX = "checkout_key:";

/** TTL for the checkout key in KV (24 hours). */
export const CHECKOUT_KEY_TTL_SECONDS = 86_400;

/** Shape of the KV value stored under `checkout_key:{session_id}`. */
export interface CheckoutKeyEntry {
  plaintext: string;
  prefix: string;
  tier: UserTier;
  email: string;
  createdAt: number;
}

/** RFC 4648 base32 alphabet (uppercase, no padding). */
const BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

function base32Encode(bytes: Uint8Array): string {
  let bits = 0;
  let value = 0;
  let out = "";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += BASE32[(value >> bits) & 31];
    }
  }
  if (bits > 0) out += BASE32[(value << (5 - bits)) & 31];
  return out;
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/** Generates a fresh `sv_live_` API key with 160 bits of entropy. */
async function generateApiKey(): Promise<{
  plaintext: string;
  prefix: string;
  hash: string;
}> {
  const bytes = new Uint8Array(20);
  crypto.getRandomValues(bytes);
  const body = base32Encode(bytes).slice(0, 32);
  const plaintext = `sv_live_${body}`;
  const prefix = plaintext.slice(0, 14);
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(plaintext),
  );
  const hash = toHex(new Uint8Array(digest));
  return { plaintext, prefix, hash };
}

/**
 * Provisions a user + API key for a successful checkout.
 *
 * Returns the plaintext API key (for logging confirmation only —
 * NEVER persist it). The plaintext is also stashed in KV for the
 * /checkout/success page.
 */
export async function provisionApiKey(
  env: Env,
  email: string,
  tier: UserTier,
  sessionId: string,
  stripeCustomerId?: string | null,
): Promise<{ plaintext: string; prefix: string } | null> {
  const now = Date.now();
  const emailLower = email.toLowerCase();

  try {
    // ── Upsert user ────────────────────────────────────────────────────
    const existing = await env.DB.prepare(
      "SELECT id, tier FROM users WHERE email = ?1 LIMIT 1",
    )
      .bind(emailLower)
      .first<{ id: string; tier: string }>();

    let userId: string;
    if (existing) {
      userId = existing.id;
      await env.DB.prepare(
        "UPDATE users SET tier = ?1, stripe_customer_id = COALESCE(?2, stripe_customer_id), updated_at = ?3 WHERE id = ?4",
      )
        .bind(tier, stripeCustomerId ?? null, now, userId)
        .run();
    } else {
      userId = `usr_hub_${crypto.randomUUID().replace(/-/g, "").slice(0, 12)}`;
      await env.DB.prepare(
        `INSERT INTO users (id, email, stripe_customer_id, tier, created_at, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)`,
      )
        .bind(userId, emailLower, stripeCustomerId ?? null, tier, now, now)
        .run();
    }

    // ── Generate API key ───────────────────────────────────────────────
    const key = await generateApiKey();
    const keyId = `key_hub_${crypto.randomUUID().replace(/-/g, "").slice(0, 12)}`;
    await env.DB.prepare(
      `INSERT INTO api_keys (id, user_id, name, key_hash, key_prefix, created_at)
       VALUES (?1, ?2, ?3, ?4, ?5, ?6)`,
    )
      .bind(keyId, userId, "Hub Checkout", key.hash, key.prefix, now)
      .run();

    // ── Stash plaintext in KV for /checkout/success ────────────────────
    const entry: CheckoutKeyEntry = {
      plaintext: key.plaintext,
      prefix: key.prefix,
      tier,
      email: emailLower,
      createdAt: now,
    };
    await env.RL_KV.put(
      `${CHECKOUT_KEY_KV_PREFIX}${sessionId}`,
      JSON.stringify(entry),
      { expirationTtl: CHECKOUT_KEY_TTL_SECONDS },
    );

    // ── Invalidate auth cache so new tier is picked up fast ────────────
    try {
      await env.RL_KV.delete(`hub-auth:${key.hash}`);
    } catch {
      // TTL is the backstop.
    }

    return { plaintext: key.plaintext, prefix: key.prefix };
  } catch (err) {
    // Never let a provisioning failure crash the webhook response.
    // Stripe will retry. Log for observability.
    console.error(
      `[hub-mcp] provisionApiKey failed for ${emailLower} (session ${sessionId}):`,
      err instanceof Error ? err.message : err,
    );
    return null;
  }
}
