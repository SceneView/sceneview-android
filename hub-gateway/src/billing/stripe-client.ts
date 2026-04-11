/**
 * Minimal Stripe REST client for the hub gateway.
 *
 * Ported from mcp-gateway/src/billing/stripe-client.ts — same
 * fetch-based, SDK-less approach (Stripe SDK is 500+ KB and drags
 * process globals into the Worker bundle). Only the endpoints the
 * hub actually needs are wrapped:
 *
 *   POST /v1/checkout/sessions                — start checkout
 *   GET  /v1/checkout/sessions/{id}           — resolve success URL
 *
 * The webhook path uses a separate HMAC verification (see
 * verifyWebhookSignature) and does NOT hit the Stripe API at all.
 *
 * CRITICAL: `createCheckoutSession` below does NOT set
 * `customer_creation`. In subscription mode Stripe auto-creates the
 * Customer anyway, and passing `customer_creation: "always"` returns
 * "customer_creation can only be used in payment mode" and 502s the
 * checkout. This is the bug that blocked 100% of Gateway #1 checkouts
 * until commit 88aec77b — do NOT reintroduce the flag here unless
 * you're adding a `mode: "payment"` code path.
 */

const STRIPE_API = "https://api.stripe.com";

export interface StripeErrorBody {
  error?: {
    message?: string;
    type?: string;
    code?: string;
  };
}

/** Thrown when the Stripe API returns a non-2xx status. */
export class StripeError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly body: StripeErrorBody | null,
  ) {
    super(message);
    this.name = "StripeError";
  }
}

/** Form-encoded POST / GET with Stripe Basic auth. */
export async function stripeRequest<T>(
  secretKey: string,
  method: "GET" | "POST",
  path: string,
  params?: Record<string, string | number | boolean>,
): Promise<T> {
  if (!secretKey) throw new Error("stripeRequest: STRIPE_SECRET_KEY is not set");
  const url = `${STRIPE_API}${path}`;
  const headers: Record<string, string> = {
    authorization: `Basic ${btoa(`${secretKey}:`)}`,
  };

  let body: string | undefined;
  let finalUrl = url;
  if (params) {
    const form = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) form.set(k, String(v));
    if (method === "POST") {
      headers["content-type"] = "application/x-www-form-urlencoded";
      body = form.toString();
    } else {
      finalUrl = `${url}?${form.toString()}`;
    }
  }

  const response = await fetch(finalUrl, { method, headers, body });
  const text = await response.text();
  let parsed: unknown = null;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = null;
    }
  }
  if (!response.ok) {
    const errorBody = parsed as StripeErrorBody | null;
    const msg =
      errorBody?.error?.message ??
      `Stripe HTTP ${response.status} on ${method} ${path}`;
    throw new StripeError(response.status, msg, errorBody);
  }
  return parsed as T;
}

/** Minimal Stripe Checkout Session shape the hub cares about. */
export interface StripeCheckoutSession {
  id: string;
  url: string;
  customer?: string | null;
  customer_email?: string | null;
  customer_details?: { email?: string | null; name?: string | null } | null;
  subscription?: string | null;
  metadata?: Record<string, string> | null;
}

/** Create a subscription-mode Checkout Session. */
export function createCheckoutSession(
  secretKey: string,
  params: {
    priceId: string;
    customerEmail?: string;
    successUrl: string;
    cancelUrl: string;
    metadata?: Record<string, string>;
  },
): Promise<StripeCheckoutSession> {
  const form: Record<string, string> = {
    mode: "subscription",
    "line_items[0][price]": params.priceId,
    "line_items[0][quantity]": "1",
    success_url: params.successUrl,
    cancel_url: params.cancelUrl,
  };
  if (params.metadata) {
    for (const [k, v] of Object.entries(params.metadata)) {
      form[`metadata[${k}]`] = v;
    }
  }
  if (params.customerEmail) {
    form.customer_email = params.customerEmail;
  }
  // DO NOT set customer_creation in subscription mode — see header.
  return stripeRequest<StripeCheckoutSession>(
    secretKey,
    "POST",
    "/v1/checkout/sessions",
    form,
  );
}

/** Retrieve a Checkout Session by id (e.g. for /checkout/success). */
export function retrieveCheckoutSession(
  secretKey: string,
  sessionId: string,
): Promise<StripeCheckoutSession> {
  return stripeRequest<StripeCheckoutSession>(
    secretKey,
    "GET",
    `/v1/checkout/sessions/${encodeURIComponent(sessionId)}`,
  );
}

/**
 * Verify a Stripe webhook signature using the timing-safe HMAC-SHA256
 * scheme documented at https://stripe.com/docs/webhooks/signatures.
 *
 * Returns true iff ANY `v1=` signature in the `Stripe-Signature`
 * header matches `HMAC(secret, "{timestamp}.{rawBody}")` AND the
 * timestamp is within `toleranceSeconds` of `nowSec`.
 *
 * The hub uses its OWN `STRIPE_WEBHOOK_SECRET` (a distinct Stripe
 * webhook endpoint pointed at hub-mcp.mcp-tools-lab.workers.dev/
 * stripe/webhook) — NEVER reuse Gateway #1's whsec_.
 */
export async function verifyWebhookSignature(
  rawBody: string,
  signatureHeader: string | null,
  secret: string,
  nowSec: number = Math.floor(Date.now() / 1000),
  toleranceSeconds = 300,
): Promise<boolean> {
  if (!signatureHeader || !secret) return false;
  const parts = signatureHeader.split(",");
  let timestamp: number | null = null;
  const v1Sigs: string[] = [];
  for (const p of parts) {
    const [k, v] = p.split("=");
    if (k === "t") timestamp = Number(v);
    else if (k === "v1" && v) v1Sigs.push(v);
  }
  if (!timestamp || v1Sigs.length === 0) return false;
  if (Math.abs(nowSec - timestamp) > toleranceSeconds) return false;

  const payload = `${timestamp}.${rawBody}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const mac = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(payload),
  );
  const expectedHex = Array.from(new Uint8Array(mac))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
  for (const sig of v1Sigs) {
    if (timingSafeEqual(sig, expectedHex)) return true;
  }
  return false;
}

/** Constant-time string compare to avoid timing attacks on HMACs. */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let out = 0;
  for (let i = 0; i < a.length; i++) out |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return out === 0;
}
