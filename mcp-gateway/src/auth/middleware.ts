/**
 * Hono middleware — extracts an API key from the incoming request and
 * populates `c.var.auth` with the authenticated user + tier, or short
 * circuits with a JSON-RPC-shaped 401 response.
 *
 * Key extraction:
 *   1. `Authorization: Bearer sv_live_...` (preferred)
 *   2. `?key=sv_live_...` query parameter. This fallback exists
 *      because some versions of Claude Desktop do not forward request
 *      headers on MCP remote endpoints, which is a known risk called
 *      out in the monetisation plan.
 *
 * Caching:
 *   - On a cache hit in KV under `auth:{hash}`, we skip D1 entirely.
 *   - On a miss, we call `validateApiKey` against D1 and populate the
 *     cache with a 300 s TTL.
 *   - Revocation propagation is bounded by that TTL, which is the
 *     trade-off documented in section 3.6 of the plan.
 *
 * This module intentionally has no Hono `import type` coupling beyond
 * what is already pulled in by the gateway — Hono's `MiddlewareHandler`
 * stays the only dependency on the framework.
 */

import type { Context, MiddlewareHandler, Next } from "hono";
import type { Env } from "../env.js";
import type { ApiKeyRow, UserTier } from "../db/schema.js";
import { hashApiKey, validateApiKey } from "./api-keys.js";
import { JSON_RPC_ERRORS, type JsonRpcResponse } from "../mcp/transport.js";

/** Cache key namespace used for auth entries in KV. */
const AUTH_CACHE_PREFIX = "auth:";

/** TTL applied to cached auth records in KV, in seconds (5 min). */
export const AUTH_CACHE_TTL_SECONDS = 300;

/** Shape of an authenticated request attached to the Hono context. */
export interface AuthenticatedRequest {
  /** Primary key id (api_keys.id) of the key that made the request. */
  keyId: string;
  /** First 14 chars of the plaintext for display / logging. */
  keyPrefix: string;
  /** User that owns the key. */
  userId: string;
  /** Tier granted to the user at lookup time. */
  tier: UserTier;
  /** SHA-256 hash of the plaintext — used as cache key for rate limits. */
  keyHash: string;
}

/** Cached auth entry persisted in KV. */
interface CachedAuthEntry {
  keyId: string;
  keyPrefix: string;
  userId: string;
  tier: UserTier;
}

/** Hono `Variables` fragment that downstream routes can declare. */
export interface AuthVariables {
  auth: AuthenticatedRequest;
}

/**
 * Hono middleware factory.
 *
 * Call `authMiddleware()` once and mount the returned handler on the
 * route groups that require authentication.
 */
export function authMiddleware(): MiddlewareHandler<{
  Bindings: Env;
  Variables: AuthVariables;
}> {
  return async (c, next) => {
    const plaintext = extractApiKey(c);
    if (!plaintext) {
      return jsonRpcUnauthorized(c, "Missing API key");
    }

    const keyHash = await hashApiKey(plaintext);
    const cached = await readCachedAuth(c.env.RL_KV, keyHash);

    let auth: AuthenticatedRequest;
    if (cached) {
      auth = {
        keyId: cached.keyId,
        keyPrefix: cached.keyPrefix,
        userId: cached.userId,
        tier: cached.tier,
        keyHash,
      };
    } else {
      const validated = await validateApiKey(c.env.DB, plaintext);
      if (!validated) {
        return jsonRpcUnauthorized(c, "Invalid or revoked API key");
      }
      await writeCachedAuth(c.env.RL_KV, keyHash, validated.key, validated.tier);
      auth = {
        keyId: validated.key.id,
        keyPrefix: validated.key.key_prefix,
        userId: validated.userId,
        tier: validated.tier,
        keyHash,
      };
    }

    c.set("auth", auth);
    await next();
    return;
  };
}

// ── Helpers ────────────────────────────────────────────────────────────────

/** Reads a Bearer token from the request headers or the `key` query param. */
function extractApiKey(c: Context): string | null {
  const authz = c.req.header("authorization") || c.req.header("Authorization");
  if (authz) {
    const m = /^Bearer\s+(\S+)/i.exec(authz);
    if (m) return m[1];
  }
  const qp = c.req.query("key");
  if (qp && typeof qp === "string") return qp;
  return null;
}

/** Builds a JSON-RPC 2.0 `-32001 Unauthorized` response. */
function jsonRpcUnauthorized(c: Context, detail: string): Response {
  const body: JsonRpcResponse = {
    jsonrpc: "2.0",
    id: null,
    error: {
      code: JSON_RPC_ERRORS.UNAUTHORIZED,
      message: "Unauthorized",
      data: { detail },
    },
  };
  return c.json(body, 401);
}

/** Reads a cached auth entry from KV, or returns null on miss/parse error. */
async function readCachedAuth(
  kv: KVNamespace,
  hash: string,
): Promise<CachedAuthEntry | null> {
  try {
    const raw = await kv.get(AUTH_CACHE_PREFIX + hash);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CachedAuthEntry;
    if (
      typeof parsed?.userId === "string" &&
      typeof parsed?.tier === "string"
    ) {
      return parsed;
    }
    return null;
  } catch {
    return null;
  }
}

/** Writes a cached auth entry to KV with the module TTL. */
async function writeCachedAuth(
  kv: KVNamespace,
  hash: string,
  key: ApiKeyRow,
  tier: UserTier,
): Promise<void> {
  const entry: CachedAuthEntry = {
    keyId: key.id,
    keyPrefix: key.key_prefix,
    userId: key.user_id,
    tier,
  };
  await kv.put(AUTH_CACHE_PREFIX + hash, JSON.stringify(entry), {
    expirationTtl: AUTH_CACHE_TTL_SECONDS,
  });
}

/** Type helper for routes that want to read the auth variable strongly. */
export function getAuth(c: Context<{ Variables: AuthVariables }>): AuthenticatedRequest {
  const auth = c.get("auth");
  if (!auth) {
    throw new Error(
      "authMiddleware() must be mounted before this handler",
    );
  }
  return auth;
}

/** Exposed so tests can avoid importing AUTH_CACHE_PREFIX indirectly. */
export const __testing = {
  AUTH_CACHE_PREFIX,
  extractApiKey,
  _typeNext: undefined as unknown as Next, // keeps tsc happy on unused imports
};
