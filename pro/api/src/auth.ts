/**
 * API key validation against Stripe.
 *
 * Keys are expected in the `Authorization: Bearer sk_sv_...` header.
 * Each key maps to a Stripe customer with a subscription that determines
 * the tier (free / pro).
 */

export interface ApiKeyInfo {
  customerId: string;
  tier: "free" | "pro";
  valid: boolean;
}

/**
 * Validate an API key and return the associated customer info.
 *
 * @param apiKey - The bearer token from the Authorization header.
 * @param stripeSecretKey - Stripe secret key from env.
 * @returns Customer info or null if the key is invalid.
 */
export async function validateApiKey(
  apiKey: string,
  stripeSecretKey: string
): Promise<ApiKeyInfo | null> {
  if (!apiKey || !apiKey.startsWith("sk_sv_")) {
    return null;
  }

  // TODO: Look up the API key in Stripe.
  // Implementation will:
  // 1. Search Stripe customers by metadata.api_key === apiKey
  // 2. Check the customer's active subscription for the tier
  // 3. Return { customerId, tier, valid }
  //
  // For now, return a stub for scaffold testing:
  // const stripe = new Stripe(stripeSecretKey);
  // const customers = await stripe.customers.search({
  //   query: `metadata["api_key"]:"${apiKey}"`,
  // });

  console.log("TODO: Stripe API key lookup not yet implemented");

  return null;
}

/**
 * Extract the bearer token from an Authorization header value.
 */
export function extractBearerToken(
  authHeader: string | null
): string | null {
  if (!authHeader) return null;
  const match = authHeader.match(/^Bearer\s+(.+)$/i);
  return match ? match[1] : null;
}
