/**
 * SceneView MCP Pro API — Cloudflare Workers entry point.
 *
 * Endpoints:
 *   POST /api/v1/generate  — Generate 3D scene code (pro only)
 *   POST /api/v1/validate  — Validate SceneView code (free: 100/mo, pro: unlimited)
 *   POST /api/v1/optimize  — Optimize 3D assets (pro only)
 *   GET  /api/v1/usage     — Current usage stats
 */

import { validateApiKey, extractBearerToken, type ApiKeyInfo } from "./auth";
import {
  checkRateLimit,
  trackUsage,
  getUsageStats,
  type RateLimitResult,
} from "./rate-limit";

interface Env {
  STRIPE_SECRET_KEY: string;
  UPSTASH_REDIS_URL: string;
  UPSTASH_REDIS_TOKEN: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}

function errorResponse(message: string, status: number): Response {
  return jsonResponse({ error: message }, status);
}

// ---------------------------------------------------------------------------
// Middleware: authenticate + rate-limit
// ---------------------------------------------------------------------------

interface AuthenticatedRequest {
  apiKeyInfo: ApiKeyInfo;
  rateLimit: RateLimitResult;
}

async function authenticate(
  request: Request,
  env: Env
): Promise<AuthenticatedRequest | Response> {
  const token = extractBearerToken(request.headers.get("Authorization"));
  if (!token) {
    return errorResponse("Missing Authorization header", 401);
  }

  const apiKeyInfo = await validateApiKey(token, env.STRIPE_SECRET_KEY);
  if (!apiKeyInfo || !apiKeyInfo.valid) {
    return errorResponse("Invalid API key", 401);
  }

  const rateLimit = await checkRateLimit(
    apiKeyInfo.customerId,
    apiKeyInfo.tier,
    env.UPSTASH_REDIS_URL,
    env.UPSTASH_REDIS_TOKEN
  );
  if (!rateLimit.allowed) {
    return errorResponse("Rate limit exceeded", 429);
  }

  return { apiKeyInfo, rateLimit };
}

// ---------------------------------------------------------------------------
// Route handlers
// ---------------------------------------------------------------------------

async function handleGenerate(
  request: Request,
  env: Env,
  auth: AuthenticatedRequest
): Promise<Response> {
  if (auth.apiKeyInfo.tier !== "pro") {
    return errorResponse("Code generation requires a Pro subscription", 403);
  }

  const body = (await request.json()) as { prompt?: string; platform?: string };
  if (!body.prompt) {
    return errorResponse("Missing 'prompt' field", 400);
  }

  await trackUsage(
    auth.apiKeyInfo.customerId,
    "generate",
    env.UPSTASH_REDIS_URL,
    env.UPSTASH_REDIS_TOKEN
  );

  // TODO: Proxy to the MCP server with premium context.
  // This will call the local MCP tools (get_scene_code, etc.) with
  // expanded model context, multi-file output, and platform targeting.

  return jsonResponse({
    code: "// TODO: Generated code will appear here",
    platform: body.platform || "android",
    prompt: body.prompt,
  });
}

async function handleValidate(
  request: Request,
  env: Env,
  auth: AuthenticatedRequest
): Promise<Response> {
  // Free tier: 100 validations/month
  if (auth.apiKeyInfo.tier === "free") {
    const stats = await getUsageStats(
      auth.apiKeyInfo.customerId,
      env.UPSTASH_REDIS_URL,
      env.UPSTASH_REDIS_TOKEN
    );
    if (stats.validationsThisMonth >= 100) {
      return errorResponse(
        "Free tier limit reached (100 validations/month). Upgrade to Pro for unlimited.",
        403
      );
    }
  }

  const body = (await request.json()) as { code?: string; platform?: string };
  if (!body.code) {
    return errorResponse("Missing 'code' field", 400);
  }

  await trackUsage(
    auth.apiKeyInfo.customerId,
    "validate",
    env.UPSTASH_REDIS_URL,
    env.UPSTASH_REDIS_TOKEN
  );

  // TODO: Proxy to the MCP server's validate_code tool.
  // Will check Compose structure, threading rules, API usage, etc.

  return jsonResponse({
    valid: true,
    issues: [],
    platform: body.platform || "android",
  });
}

async function handleOptimize(
  request: Request,
  env: Env,
  auth: AuthenticatedRequest
): Promise<Response> {
  if (auth.apiKeyInfo.tier !== "pro") {
    return errorResponse("Asset optimization requires a Pro subscription", 403);
  }

  const body = (await request.json()) as { assetUrl?: string; options?: Record<string, unknown> };
  if (!body.assetUrl) {
    return errorResponse("Missing 'assetUrl' field", 400);
  }

  await trackUsage(
    auth.apiKeyInfo.customerId,
    "optimize",
    env.UPSTASH_REDIS_URL,
    env.UPSTASH_REDIS_TOKEN
  );

  // TODO: Implement asset optimization pipeline.
  // This will handle texture compression (KTX2/Basis), mesh simplification,
  // LOD generation, and Draco compression for glTF/GLB files.

  return jsonResponse({
    originalUrl: body.assetUrl,
    optimizedUrl: null, // TODO: Return optimized asset URL
    savings: null,
  });
}

async function handleUsage(
  env: Env,
  auth: AuthenticatedRequest
): Promise<Response> {
  const stats = await getUsageStats(
    auth.apiKeyInfo.customerId,
    env.UPSTASH_REDIS_URL,
    env.UPSTASH_REDIS_TOKEN
  );

  return jsonResponse({
    tier: auth.apiKeyInfo.tier,
    customerId: auth.apiKeyInfo.customerId,
    usage: stats,
    limits: {
      validationsPerMonth: auth.apiKeyInfo.tier === "free" ? 100 : "unlimited",
      requestsPerMinute: auth.apiKeyInfo.tier === "free" ? 10 : 60,
    },
  });
}

// ---------------------------------------------------------------------------
// Worker fetch handler
// ---------------------------------------------------------------------------

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
          "Access-Control-Allow-Headers": "Authorization, Content-Type",
        },
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // Health check (no auth required)
    if (path === "/health") {
      return jsonResponse({ status: "ok", service: "sceneview-pro-api" });
    }

    // All /api/v1/* routes require authentication
    if (!path.startsWith("/api/v1/")) {
      return errorResponse("Not found", 404);
    }

    const authResult = await authenticate(request, env);
    if (authResult instanceof Response) {
      return authResult;
    }

    // Route dispatch
    switch (path) {
      case "/api/v1/generate":
        if (request.method !== "POST") return errorResponse("Method not allowed", 405);
        return handleGenerate(request, env, authResult);

      case "/api/v1/validate":
        if (request.method !== "POST") return errorResponse("Method not allowed", 405);
        return handleValidate(request, env, authResult);

      case "/api/v1/optimize":
        if (request.method !== "POST") return errorResponse("Method not allowed", 405);
        return handleOptimize(request, env, authResult);

      case "/api/v1/usage":
        if (request.method !== "GET") return errorResponse("Method not allowed", 405);
        return handleUsage(env, authResult);

      default:
        return errorResponse("Not found", 404);
    }
  },
};
