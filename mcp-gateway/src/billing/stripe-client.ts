/**
 * Minimal Stripe REST client.
 *
 * We deliberately do NOT depend on the `stripe` npm package — it's
 * 500+ KB when pulled into a Workers bundle, pulls in a bunch of
 * `process`-dependent globals, and we only need four endpoints:
 *
 *   POST /v1/checkout/sessions
 *   POST /v1/billing_portal/sessions
 *   GET  /v1/customers/{id}
 *   GET  /v1/subscriptions/{id}
 *
 * All four are trivial POST form-encoded / GET requests authenticated
 * with the secret key as HTTP Basic auth user (Stripe convention).
 *
 * Do not log the secret key or the response body to stderr in prod.
 */

/** Base URL — override in tests by monkey-patching `globalThis.fetch`. */
const STRIPE_API = "https://api.stripe.com";

/** Shape of a Stripe error response body. */
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

/** Makes a Stripe API call with Basic auth. */
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
    for (const [k, v] of Object.entries(params)) {
      form.set(k, String(v));
    }
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

/** Minimal Stripe Checkout Session shape we care about. */
export interface StripeCheckoutSession {
  id: string;
  url: string;
  customer?: string;
  client_reference_id?: string;
  subscription?: string;
}

/** Minimal Stripe Customer Portal Session shape. */
export interface StripePortalSession {
  id: string;
  url: string;
}

/** Minimal Stripe Subscription shape. */
export interface StripeSubscription {
  id: string;
  customer: string;
  status: string;
  cancel_at_period_end: boolean;
  current_period_end: number;
  items: {
    data: Array<{
      price: { id: string };
    }>;
  };
}

/** Minimal Stripe Customer shape. */
export interface StripeCustomer {
  id: string;
  email?: string;
}

/** Helper to create a Checkout Session. */
export function createCheckoutSession(
  secretKey: string,
  params: {
    priceId: string;
    customerEmail?: string;
    customerId?: string;
    clientReferenceId: string;
    successUrl: string;
    cancelUrl: string;
  },
): Promise<StripeCheckoutSession> {
  const form: Record<string, string> = {
    mode: "subscription",
    "line_items[0][price]": params.priceId,
    "line_items[0][quantity]": "1",
    success_url: params.successUrl,
    cancel_url: params.cancelUrl,
    client_reference_id: params.clientReferenceId,
    "metadata[sv_user_id]": params.clientReferenceId,
  };
  if (params.customerId) {
    form.customer = params.customerId;
  } else if (params.customerEmail) {
    form.customer_email = params.customerEmail;
  }
  return stripeRequest<StripeCheckoutSession>(
    secretKey,
    "POST",
    "/v1/checkout/sessions",
    form,
  );
}

/** Helper to create a Customer Portal Session. */
export function createPortalSession(
  secretKey: string,
  params: { customerId: string; returnUrl: string },
): Promise<StripePortalSession> {
  return stripeRequest<StripePortalSession>(
    secretKey,
    "POST",
    "/v1/billing_portal/sessions",
    {
      customer: params.customerId,
      return_url: params.returnUrl,
    },
  );
}

/** Retrieves a subscription by id. */
export function retrieveSubscription(
  secretKey: string,
  subscriptionId: string,
): Promise<StripeSubscription> {
  return stripeRequest<StripeSubscription>(
    secretKey,
    "GET",
    `/v1/subscriptions/${encodeURIComponent(subscriptionId)}`,
  );
}

/** Retrieves a customer by id. */
export function retrieveCustomer(
  secretKey: string,
  customerId: string,
): Promise<StripeCustomer> {
  return stripeRequest<StripeCustomer>(
    secretKey,
    "GET",
    `/v1/customers/${encodeURIComponent(customerId)}`,
  );
}
