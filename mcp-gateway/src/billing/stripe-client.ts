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
  customer?: string | null;
  customer_email?: string | null;
  customer_details?: {
    email?: string | null;
    name?: string | null;
  } | null;
  client_reference_id?: string | null;
  subscription?: string | null;
  metadata?: Record<string, string> | null;
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
    /** Optional — when provided the form is prefilled with that email. */
    customerEmail?: string;
    /** Optional — reuse an existing Stripe customer. */
    customerId?: string;
    /** Optional — legacy magic-link flow sets a sv user id here. */
    clientReferenceId?: string;
    successUrl: string;
    cancelUrl: string;
    /** Arbitrary metadata persisted on the session (visible in webhook). */
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
  if (params.clientReferenceId) {
    form.client_reference_id = params.clientReferenceId;
    form["metadata[sv_user_id]"] = params.clientReferenceId;
  }
  if (params.metadata) {
    for (const [k, v] of Object.entries(params.metadata)) {
      form[`metadata[${k}]`] = v;
    }
  }
  if (params.customerId) {
    form.customer = params.customerId;
  } else if (params.customerEmail) {
    form.customer_email = params.customerEmail;
  }
  // In subscription mode Stripe always auto-creates a Customer for the
  // subscription, so no explicit customer_creation flag is needed here.
  // (Setting customer_creation = "always" here used to return
  // "customer_creation can only be used in payment mode" and 502 the
  // checkout route.)
  return stripeRequest<StripeCheckoutSession>(
    secretKey,
    "POST",
    "/v1/checkout/sessions",
    form,
  );
}

/** Retrieves a Checkout Session by id (expands customer info). */
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
