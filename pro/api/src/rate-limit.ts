/**
 * Rate limiting and usage tracking via Upstash Redis.
 *
 * Free tier: 10 req/min, 100 validations/month
 * Pro tier:  60 req/min, unlimited
 */

export interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  resetAt: number; // Unix timestamp (seconds)
}

export interface UsageStats {
  requestsThisMinute: number;
  validationsThisMonth: number;
  generationsThisMonth: number;
  optimizationsThisMonth: number;
}

const LIMITS = {
  free: { requestsPerMinute: 10, validationsPerMonth: 100 },
  pro: { requestsPerMinute: 60, validationsPerMonth: Infinity },
} as const;

/**
 * Check whether a request is within the rate limit for the given customer.
 *
 * @param customerId - Stripe customer ID (used as the Redis key prefix).
 * @param tier - Customer tier.
 * @param redisUrl - Upstash Redis REST URL from env.
 * @param redisToken - Upstash Redis REST token from env.
 */
export async function checkRateLimit(
  customerId: string,
  tier: "free" | "pro",
  redisUrl: string,
  redisToken: string
): Promise<RateLimitResult> {
  const limit = LIMITS[tier].requestsPerMinute;

  // TODO: Implement Upstash Redis sliding-window rate limit.
  // Implementation will:
  // 1. INCR `ratelimit:{customerId}:{minuteBucket}`
  // 2. EXPIRE key after 60s
  // 3. Compare count against limit
  //
  // const redis = new Redis({ url: redisUrl, token: redisToken });
  // const key = `ratelimit:${customerId}:${Math.floor(Date.now() / 60000)}`;
  // const count = await redis.incr(key);
  // if (count === 1) await redis.expire(key, 60);

  console.log("TODO: Upstash rate limiting not yet implemented");

  return {
    allowed: true,
    remaining: limit,
    resetAt: Math.floor(Date.now() / 1000) + 60,
  };
}

/**
 * Increment a monthly usage counter for a specific endpoint.
 */
export async function trackUsage(
  customerId: string,
  endpoint: "validate" | "generate" | "optimize",
  redisUrl: string,
  redisToken: string
): Promise<void> {
  // TODO: Increment monthly usage counter in Redis.
  // const redis = new Redis({ url: redisUrl, token: redisToken });
  // const month = new Date().toISOString().slice(0, 7); // "2024-03"
  // await redis.incr(`usage:${customerId}:${endpoint}:${month}`);

  console.log(`TODO: Track usage for ${customerId} on ${endpoint}`);
}

/**
 * Get current usage stats for a customer.
 */
export async function getUsageStats(
  customerId: string,
  redisUrl: string,
  redisToken: string
): Promise<UsageStats> {
  // TODO: Fetch monthly counters from Redis.
  // const redis = new Redis({ url: redisUrl, token: redisToken });
  // const month = new Date().toISOString().slice(0, 7);
  // const [validations, generations, optimizations] = await Promise.all([
  //   redis.get(`usage:${customerId}:validate:${month}`),
  //   redis.get(`usage:${customerId}:generate:${month}`),
  //   redis.get(`usage:${customerId}:optimize:${month}`),
  // ]);

  console.log("TODO: Fetch usage stats from Redis");

  return {
    requestsThisMinute: 0,
    validationsThisMonth: 0,
    generationsThisMonth: 0,
    optimizationsThisMonth: 0,
  };
}
