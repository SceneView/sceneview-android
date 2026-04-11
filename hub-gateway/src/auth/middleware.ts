/**
 * Hono middleware for the hub gateway — extracts an API key from the
 * incoming request, authenticates against the shared D1 database,
 * and attaches the result to the Hono context as `c.var.auth`.
 *
 * Ported from mcp-gateway/src/auth/middleware.ts with two changes:
 *   - Uses a distinct KV cache prefix (`hub-auth:`) so the two
 *     gateways don't clobber each other's cache entries.
 *   - Emits the hub JSON-RPC error shape on failure rather than
 *     importing Gateway #1's transport module (they live in
 *     different worker bundles).
 *
 * Key extraction order:
 *   1. `Authorization: Bearer sv_live_...` (preferred)
 *   2. `?key=sv_live_...` query parameter — fallback for MCP clients
 *      that do not forward custom headers on remote endpoints.
 *
 * Caching:
 *   - Hits on `hub-auth:{hash}` skip D1 entirely (5 min TTL).
 *   - Misses call validateApiKey against D1 and warm the cache.
 *   - Revocation is bounded by that TTL.
 */

import type { Context, MiddlewareHandler } from "hono";
import type { Env } from "../env.js";
import type { UserTier } from "../db/schema.js";
import { hashApiKey, validateApiKey, type ValidatedApiKey } from "./api-keys.js";
import { JSON_RPC_ERRORS } from "../mcp/transport.js";

/** Cache key namespace used for hub auth entries in KV. */
const HUB_AUTH_CACHE_PREFIX = "hub-auth:";

/** TTL applied to cached auth records in KV, in seconds (5 min). */
export const HUB_AUTH_CACHE_TTL_SECONDS = 300;

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
  /** SHA-256 hex of the plaintext — reused as rate-limit bucket key. */
  keyHash: string;
}

interface CachedAuthEntry {
  keyId: string;
  keyPrefix: string;
  userId: string;
  tier: UserTier;
}

/** Hono `Variables` fragment downstream routes can declare. */
export interface AuthVariables {
  auth: AuthenticatedRequest;
}

/** Factory — call once and mount on the route groups that require auth. */
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
      await writeCachedAuth(c.env.RL_KV, keyHash, validated);
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

/** Reads a Bearer token from the headers or the `key` query param. */
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
  return c.json(
    {
      jsonrpc: "2.0",
      id: null,
      error: {
        code: JSON_RPC_ERRORS.UNAUTHORIZED,
        message: "Unauthorized",
        data: { detail },
      },
    },
    401,
  );
}

/** Reads a cached auth entry from KV, or returns null on miss / parse error. */
async function readCachedAuth(
  kv: KVNamespace,
  hash: string,
): Promise<CachedAuthEntry | null> {
  try {
    const raw = await kv.get(HUB_AUTH_CACHE_PREFIX + hash);
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
  validated: ValidatedApiKey,
): Promise<void> {
  const entry: CachedAuthEntry = {
    keyId: validated.key.id,
    keyPrefix: validated.key.key_prefix,
    userId: validated.userId,
    tier: validated.tier,
  };
  await kv.put(HUB_AUTH_CACHE_PREFIX + hash, JSON.stringify(entry), {
    expirationTtl: HUB_AUTH_CACHE_TTL_SECONDS,
  });
}

/** Type helper for routes that want to read the auth variable strongly. */
export function getAuth(
  c: Context<{ Variables: AuthVariables }>,
): AuthenticatedRequest {
  const auth = c.get("auth");
  if (!auth) {
    throw new Error("authMiddleware() must be mounted before this handler");
  }
  return auth;
}

/** Exposed for tests that need to inspect or pre-seed the cache. */
export const __testing = {
  HUB_AUTH_CACHE_PREFIX,
  extractApiKey,
};
