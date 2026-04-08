// ─── Stripe Billing Validation for SceneView MCP Pro ─────────────────────────
//
// Validates API keys against Stripe subscriptions to determine tier access.
// Uses native fetch() — no external Stripe SDK dependency.
// MCP servers log to stderr (stdout is reserved for the JSON-RPC protocol).
const cache = new Map();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
function getCached(apiKey) {
    const entry = cache.get(apiKey);
    if (!entry)
        return undefined;
    if (Date.now() - entry.cachedAt > CACHE_TTL_MS) {
        cache.delete(apiKey);
        return undefined;
    }
    return entry.status;
}
function setCache(apiKey, status) {
    cache.set(apiKey, { status, cachedAt: Date.now() });
}
/** Clears the validation cache. Useful for testing. */
export function clearCache() {
    cache.clear();
}
// ─── Development Allowlist ───────────────────────────────────────────────────
const DEV_ALLOWLIST = new Set([
    "dev_test_key",
    "sceneview_dev",
]);
function checkDevAllowlist(apiKey) {
    if (DEV_ALLOWLIST.has(apiKey)) {
        return { valid: true, tier: "pro" };
    }
    return { valid: false, tier: "free", error: "Invalid API key (dev mode — no Stripe configured)" };
}
async function fetchStripeSubscription(subscriptionId, stripeSecretKey) {
    const url = `https://api.stripe.com/v1/subscriptions/${encodeURIComponent(subscriptionId)}`;
    let response;
    try {
        response = await fetch(url, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${stripeSecretKey}`,
                "Content-Type": "application/x-www-form-urlencoded",
            },
        });
    }
    catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        return { valid: false, tier: "free", error: `Stripe API request failed: ${message}` };
    }
    if (!response.ok) {
        const body = await response.text().catch(() => "unknown error");
        if (response.status === 404) {
            return { valid: false, tier: "free", error: "Subscription not found" };
        }
        return {
            valid: false,
            tier: "free",
            error: `Stripe API error (${response.status}): ${body}`,
        };
    }
    let subscription;
    try {
        subscription = (await response.json());
    }
    catch {
        return { valid: false, tier: "free", error: "Failed to parse Stripe response" };
    }
    const isActive = subscription.status === "active" || subscription.status === "trialing";
    const expiresAt = new Date(subscription.current_period_end * 1000).toISOString();
    if (isActive) {
        return {
            valid: true,
            tier: "pro",
            customerId: subscription.customer,
            expiresAt,
        };
    }
    return {
        valid: false,
        tier: "free",
        customerId: subscription.customer,
        expiresAt,
        error: `Subscription status is "${subscription.status}" (expected "active" or "trialing")`,
    };
}
// ─── Public API ──────────────────────────────────────────────────────────────
/**
 * Validates an API key by checking it against Stripe subscriptions.
 *
 * - Results are cached in memory for 5 minutes to avoid excessive Stripe calls.
 * - If `STRIPE_SECRET_KEY` is not set, falls back to a development allowlist.
 * - The API key is treated as a Stripe subscription ID for the lookup.
 */
export async function validateApiKey(apiKey) {
    // Check cache first
    const cached = getCached(apiKey);
    if (cached)
        return cached;
    let result;
    if (isDevMode()) {
        // No Stripe key configured — use allowlist for development/testing
        result = checkDevAllowlist(apiKey);
    }
    else {
        const stripeKey = process.env.STRIPE_SECRET_KEY;
        result = await fetchStripeSubscription(apiKey, stripeKey);
    }
    setCache(apiKey, result);
    return result;
}
/**
 * Returns the configured SceneView API key from the environment, or undefined
 * if the user is on the free tier (no key set).
 */
export function getConfiguredApiKey() {
    return process.env.SCENEVIEW_API_KEY;
}
/**
 * Returns true when `STRIPE_SECRET_KEY` is not set in the environment.
 * In dev mode, API key validation falls back to a simple allowlist instead
 * of calling the Stripe API.
 */
export function isDevMode() {
    return !process.env.STRIPE_SECRET_KEY;
}
/**
 * Records usage of an MCP tool for a given API key.
 *
 * Stub for future billing/metering integration. Currently logs to stderr
 * (MCP servers must not write to stdout — it carries the JSON-RPC protocol).
 */
export async function recordUsage(apiKey, toolName) {
    const timestamp = new Date().toISOString();
    process.stderr.write(`[billing] ${timestamp} usage: key=${apiKey.slice(0, 8)}… tool=${toolName}\n`);
}
