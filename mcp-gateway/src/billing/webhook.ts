/**
 * Stripe webhook signature verification + dispatcher.
 *
 * Stripe signs every webhook with an HMAC-SHA256 computed over the
 * concatenation `timestamp.payload`, where `timestamp` is the `t=`
 * value from the `Stripe-Signature` header. We verify manually instead
 * of depending on the `stripe` npm package (heavy, Node-specific).
 *
 * Header format:
 *   t=<unix_seconds>,v1=<hex_sha256>,v1=<alt_sha256>
 *
 * Verification succeeds if:
 *   - the header parses
 *   - the timestamp is within {@link SIGNATURE_TOLERANCE_SECONDS}
 *   - any `v1` signature matches `HMAC(secret, "${t}.${payload}")`
 */

import type { Env } from "../env.js";

/** Payload tolerance — Stripe recommends 5 min, we align with it. */
export const SIGNATURE_TOLERANCE_SECONDS = 300;

/** Shape of a Stripe event envelope we rely on. */
export interface StripeEvent<T = unknown> {
  id: string;
  type: string;
  data: { object: T };
  created: number;
  livemode?: boolean;
}

/**
 * Parses `Stripe-Signature` into its constituent parts.
 * Returns `null` on any malformation.
 */
export function parseStripeSignatureHeader(
  header: string,
): { t: number; v1: string[] } | null {
  if (!header) return null;
  const parts = header.split(",").map((p) => p.trim());
  let t: number | null = null;
  const v1: string[] = [];
  for (const part of parts) {
    const eq = part.indexOf("=");
    if (eq === -1) continue;
    const key = part.slice(0, eq);
    const value = part.slice(eq + 1);
    if (key === "t") {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) t = parsed;
    } else if (key === "v1") {
      v1.push(value);
    }
  }
  if (t === null || v1.length === 0) return null;
  return { t, v1 };
}

/**
 * Computes the expected signature string for a given payload.
 *
 * Separated from {@link verifyWebhookSignature} so tests can exercise
 * the maths without mocking `Date.now`.
 */
export async function computeStripeSignature(
  secret: string,
  timestamp: number,
  payload: string,
): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(`${timestamp}.${payload}`),
  );
  return bufferToHex(new Uint8Array(sig));
}

/**
 * Verifies an incoming Stripe webhook.
 *
 * Returns the parsed event envelope on success, or `null` for any
 * failure. Never throws — callers use the null return to emit a 400.
 */
export async function verifyWebhookSignature(args: {
  secret: string;
  header: string | null | undefined;
  rawBody: string;
  nowSeconds?: number;
}): Promise<StripeEvent | null> {
  if (!args.secret) return null;
  if (!args.header) return null;
  const parsed = parseStripeSignatureHeader(args.header);
  if (!parsed) return null;
  const now = args.nowSeconds ?? Math.floor(Date.now() / 1000);
  if (Math.abs(now - parsed.t) > SIGNATURE_TOLERANCE_SECONDS) return null;

  const expected = await computeStripeSignature(
    args.secret,
    parsed.t,
    args.rawBody,
  );
  const ok = parsed.v1.some((candidate) => constantTimeEquals(candidate, expected));
  if (!ok) return null;

  try {
    const event = JSON.parse(args.rawBody) as StripeEvent;
    if (typeof event?.type === "string" && event?.data?.object !== undefined) {
      return event;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Dispatches a verified event to the right handler. Handlers live in
 * `./events/*.ts` and are imported lazily to keep the webhook cold-start
 * cost low.
 */
export async function dispatchWebhookEvent(
  env: Env,
  event: StripeEvent,
): Promise<void> {
  switch (event.type) {
    case "checkout.session.completed": {
      const { handleCheckoutCompleted } = await import(
        "./events/checkout-completed.js"
      );
      await handleCheckoutCompleted(env, event);
      return;
    }
    case "customer.subscription.updated":
    case "customer.subscription.created": {
      const { handleSubscriptionUpdated } = await import(
        "./events/subscription-updated.js"
      );
      await handleSubscriptionUpdated(env, event);
      return;
    }
    case "customer.subscription.deleted": {
      const { handleSubscriptionDeleted } = await import(
        "./events/subscription-deleted.js"
      );
      await handleSubscriptionDeleted(env, event);
      return;
    }
    case "invoice.payment_failed": {
      const { handlePaymentFailed } = await import("./events/payment-failed.js");
      await handlePaymentFailed(env, event);
      return;
    }
    default:
      // Unknown type — ignore silently. Stripe sends events we never ask for.
      return;
  }
}

// ── Helpers ────────────────────────────────────────────────────────────────

function bufferToHex(bytes: Uint8Array): string {
  const hex: string[] = [];
  for (const b of bytes) hex.push(b.toString(16).padStart(2, "0"));
  return hex.join("");
}

function constantTimeEquals(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
