/**
 * HS256 JSON Web Token sign/verify implemented on top of Web Crypto.
 *
 * The gateway is deployed to Cloudflare Workers which do not ship any
 * JWT library in scope by default, so we keep this module dependency
 * free. It implements just enough of RFC 7519 / RFC 7515 to produce and
 * validate the short-lived dashboard session cookies.
 *
 * Only the HS256 algorithm is supported. Tokens carry three claims:
 *
 *   - `sub`  — user id (primary key of the `users` table)
 *   - `iat`  — issued-at, seconds since epoch
 *   - `exp`  — expiry, seconds since epoch
 *
 * The JWT uses base64url encoding throughout. We never depend on Node's
 * `Buffer` — the runtime is Workers, not Node — so base64url conversion
 * is done manually with `TextEncoder`/`atob`/`btoa`.
 */

/** Supported algorithm. The header is always `{"alg":"HS256","typ":"JWT"}`. */
const ALG = "HS256";

/** Default lifetime of a dashboard session token, in seconds (7 days). */
export const DEFAULT_SESSION_TTL_SECONDS = 60 * 60 * 24 * 7;

/** Static header (pre-encoded in base64url). */
const HEADER_B64URL = base64UrlEncode(
  new TextEncoder().encode(JSON.stringify({ alg: ALG, typ: "JWT" })),
);

/** Claims we put on our session tokens. */
export interface SessionClaims {
  sub: string;
  iat: number;
  exp: number;
}

/** Returned by {@link verifyJwt} on success. */
export interface VerifiedJwt {
  claims: SessionClaims;
}

/**
 * Signs a new JWT for the given user id.
 *
 * The returned string is of the form `header.payload.signature`, each
 * segment base64url-encoded without padding.
 */
export async function signJwt(
  secret: string,
  claims: {
    sub: string;
    nowSeconds?: number;
    ttlSeconds?: number;
  },
): Promise<string> {
  if (!secret) throw new Error("signJwt: empty secret");
  const now = claims.nowSeconds ?? Math.floor(Date.now() / 1000);
  const exp = now + (claims.ttlSeconds ?? DEFAULT_SESSION_TTL_SECONDS);
  const payload: SessionClaims = {
    sub: claims.sub,
    iat: now,
    exp,
  };
  const payloadB64 = base64UrlEncode(
    new TextEncoder().encode(JSON.stringify(payload)),
  );
  const signingInput = `${HEADER_B64URL}.${payloadB64}`;
  const signature = await hmacSign(secret, signingInput);
  return `${signingInput}.${signature}`;
}

/**
 * Verifies a JWT against the given secret and returns the decoded claims.
 * Returns `null` for any failure: malformed, bad signature, expired.
 */
export async function verifyJwt(
  secret: string,
  token: string,
  nowSeconds: number = Math.floor(Date.now() / 1000),
): Promise<VerifiedJwt | null> {
  if (!secret || typeof token !== "string") return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [headerB64, payloadB64, signatureB64] = parts;
  if (headerB64 !== HEADER_B64URL) return null;

  const expected = await hmacSign(secret, `${headerB64}.${payloadB64}`);
  if (!constantTimeEquals(expected, signatureB64)) return null;

  let claims: SessionClaims;
  try {
    const raw = new TextDecoder().decode(base64UrlDecode(payloadB64));
    const parsed = JSON.parse(raw) as unknown;
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      typeof (parsed as SessionClaims).sub === "string" &&
      typeof (parsed as SessionClaims).iat === "number" &&
      typeof (parsed as SessionClaims).exp === "number"
    ) {
      claims = parsed as SessionClaims;
    } else {
      return null;
    }
  } catch {
    return null;
  }

  if (claims.exp <= nowSeconds) return null;
  return { claims };
}

// ── Internal helpers ────────────────────────────────────────────────────────

/** Computes an HS256 signature for the given signing input. */
async function hmacSign(secret: string, data: string): Promise<string> {
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
    new TextEncoder().encode(data),
  );
  return base64UrlEncode(new Uint8Array(sig));
}

/** base64url encoder (no padding) for bytes. */
function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  const b64 = btoa(binary);
  return b64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** base64url decoder (no padding) returning raw bytes. */
function base64UrlDecode(input: string): Uint8Array {
  const padded = input
    .replace(/-/g, "+")
    .replace(/_/g, "/")
    .padEnd(Math.ceil(input.length / 4) * 4, "=");
  const binary = atob(padded);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

/** Constant-time string equality to defend against signature-timing leaks. */
function constantTimeEquals(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
