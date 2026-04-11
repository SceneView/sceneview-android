/**
 * Magic-link authentication primitives.
 *
 * NOT USED IN THE MVP.
 *
 * The SceneView MCP gateway currently hands out API keys directly
 * from the Stripe checkout flow (see `billing/events/checkout-completed.ts`
 * and `routes/checkout-success.ts`) so there is no dashboard sign-in.
 * The `/login`, `/auth/verify`, and `/auth/logout` routes in
 * `routes/auth.ts` return HTTP 503 — the helpers below are kept on
 * disk so a future sprint can re-enable them without re-implementing
 * the D1 schema and the hash-compare flow.
 *
 * TODO: wire this back in when the dashboard returns. Until then the
 * `sendMagicLinkEmail` helper is a pure stub that never touches the
 * network and never imports Resend.
 */

import { hashApiKey } from "./api-keys.js";
import {
  consumeMagicLink,
  getMagicLink,
  insertMagicLink,
} from "../db/magic-links.js";

/** Default lifetime of a magic link before it expires, in minutes. */
export const MAGIC_LINK_TTL_MINUTES = 15;

/** Length of the random base32 body of the token, in characters. */
const TOKEN_BODY_LENGTH = 40;

/** RFC 4648 base32 alphabet (uppercase, no padding). */
const BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

/** Result of generating a magic link. */
export interface GeneratedMagicLink {
  /** Raw token value embedded in the email URL. Leaks on wire only once. */
  token: string;
  /** Absolute URL the user clicks. */
  url: string;
  /** Millisecond epoch when the token stops being valid. */
  expiresAt: number;
}

/** Arguments to {@link createMagicLink}. */
export interface CreateMagicLinkArgs {
  db: D1Database;
  email: string;
  baseUrl: string;
  /** Override for tests — defaults to `Date.now()`. */
  nowMs?: number;
  /** Override for tests — defaults to {@link MAGIC_LINK_TTL_MINUTES}. */
  ttlMinutes?: number;
}

/**
 * Generates, persists, and returns a fresh magic link for the given email.
 *
 * The D1 row holds the SHA-256 of the raw token, so a DB leak does not
 * grant account access.
 */
export async function createMagicLink(
  args: CreateMagicLinkArgs,
): Promise<GeneratedMagicLink> {
  const normalizedEmail = args.email.trim().toLowerCase();
  if (!isValidEmail(normalizedEmail)) {
    throw new Error("createMagicLink: invalid email");
  }
  const now = args.nowMs ?? Date.now();
  const ttlMinutes = args.ttlMinutes ?? MAGIC_LINK_TTL_MINUTES;
  const expiresAt = now + ttlMinutes * 60 * 1000;

  const token = generateToken();
  const tokenHash = await hashApiKey(token);

  await insertMagicLink(args.db, {
    tokenHash,
    email: normalizedEmail,
    expiresAt,
  });

  const cleanBase = args.baseUrl.replace(/\/+$/, "");
  const url = `${cleanBase}/auth/verify?token=${encodeURIComponent(token)}`;

  return { token, url, expiresAt };
}

/**
 * Validates a raw token and atomically marks the row consumed.
 *
 * Returns the email the link was issued to on success, or `null` for any
 * of: unknown token, already consumed, expired.
 */
export async function consumeMagicLinkToken(
  db: D1Database,
  rawToken: string,
  nowMs: number = Date.now(),
): Promise<string | null> {
  if (!rawToken) return null;
  const tokenHash = await hashApiKey(rawToken);
  const row = await getMagicLink(db, tokenHash);
  if (!row) return null;
  if (row.consumed_at !== null && row.consumed_at !== undefined) return null;
  if (row.expires_at <= nowMs) return null;
  const changes = await consumeMagicLink(db, tokenHash);
  if (changes < 1) return null;
  return row.email;
}

/**
 * Magic-link email sender — NO-OP IN THE MVP.
 *
 * The historical implementation hit the Resend API. The MVP provisions
 * API keys via Stripe Checkout instead, so this helper no longer makes
 * any network call. It is preserved with the original signature so the
 * unit tests that exercise the primitives still compile, and so a
 * future dashboard sprint can restore the real implementation without
 * a wider refactor.
 */
export async function sendMagicLinkEmail(_args: {
  apiKey: string | undefined;
  from: string;
  to: string;
  url: string;
}): Promise<void> {
  // Intentionally empty. See file-level TODO.
  return;
}

// ── Helpers ────────────────────────────────────────────────────────────────

/** Minimal email sanity check — we delegate real validation to Resend. */
export function isValidEmail(email: string): boolean {
  if (typeof email !== "string") return false;
  const trimmed = email.trim();
  if (trimmed.length < 3 || trimmed.length > 254) return false;
  const at = trimmed.indexOf("@");
  if (at < 1 || at === trimmed.length - 1) return false;
  if (trimmed.indexOf("@", at + 1) !== -1) return false;
  const domain = trimmed.slice(at + 1);
  if (!domain.includes(".")) return false;
  return true;
}

/** Generates a fresh token: {@link TOKEN_BODY_LENGTH} base32 chars. */
function generateToken(): string {
  // 25 random bytes → 40 base32 chars (200 bits of entropy).
  const bytes = new Uint8Array(25);
  crypto.getRandomValues(bytes);
  return base32Encode(bytes).slice(0, TOKEN_BODY_LENGTH);
}

/** Encodes a byte array into an uppercase base32 string (no padding). */
function base32Encode(bytes: Uint8Array): string {
  let bits = 0;
  let value = 0;
  let out = "";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += BASE32_ALPHABET[(value >> bits) & 31];
    }
  }
  if (bits > 0) out += BASE32_ALPHABET[(value << (5 - bits)) & 31];
  return out;
}

