/**
 * `/checkout/success` route — displayed when Stripe redirects the
 * buyer back after a successful Checkout Session.
 *
 * The webhook handler has stashed the newly generated plaintext API
 * key under `checkout_key:{session_id}` in KV with a 24h TTL. This
 * route reads that entry ONCE, deletes it immediately so the key can
 * never be retrieved again, and renders the success page with the
 * plaintext baked into the HTML.
 *
 * If the KV entry is missing (expired, already consumed, or the
 * webhook raced us), we render a friendly "already consumed" page
 * that directs the buyer to support.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { CHECKOUT_KEY_KV_PREFIX } from "../billing/events/checkout-completed.js";
import {
  renderCheckoutSuccess,
  renderCheckoutSuccessConsumed,
} from "../dashboard/checkout-success.js";
import type { UserTier } from "../db/schema.js";

/** Shape of the JSON value stored under `checkout_key:{session_id}`. */
export interface CheckoutKeyEntry {
  plaintext: string;
  prefix: string;
  name: string;
  tier: UserTier;
  email: string;
  createdAt: number;
}

/** Type guard for {@link CheckoutKeyEntry}. */
function isCheckoutKeyEntry(value: unknown): value is CheckoutKeyEntry {
  if (typeof value !== "object" || value === null) return false;
  const v = value as Record<string, unknown>;
  return (
    typeof v.plaintext === "string" &&
    typeof v.email === "string" &&
    (v.tier === "free" || v.tier === "pro" || v.tier === "team")
  );
}

/** Mounts the `/checkout/success` GET route. */
export function checkoutSuccessRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.get("/checkout/success", async (c) => {
    const sessionId = c.req.query("session_id") ?? null;
    if (!sessionId || !sessionId.startsWith("cs_")) {
      const html = await renderCheckoutSuccessConsumed(sessionId);
      return c.html(html, 400);
    }

    const kvKey = `${CHECKOUT_KEY_KV_PREFIX}${sessionId}`;
    const raw = await c.env.RL_KV.get(kvKey);
    if (!raw) {
      const html = await renderCheckoutSuccessConsumed(sessionId);
      return c.html(html, 410);
    }

    // Parse before we delete — if the JSON is corrupt we still want to
    // wipe the entry so a follow-up read hits the "consumed" branch.
    let entry: CheckoutKeyEntry | null = null;
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (isCheckoutKeyEntry(parsed)) {
        entry = parsed;
      }
    } catch {
      entry = null;
    }

    // Single-use: delete the KV entry immediately after reading it.
    try {
      await c.env.RL_KV.delete(kvKey);
    } catch {
      // Intentionally ignored — KV TTL is the backstop.
    }

    if (!entry) {
      const html = await renderCheckoutSuccessConsumed(sessionId);
      return c.html(html, 410);
    }

    const html = await renderCheckoutSuccess({
      apiKey: entry.plaintext,
      email: entry.email,
      tier: entry.tier,
    });
    return c.html(html);
  });

  return app;
}
