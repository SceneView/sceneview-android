/**
 * Magic-link authentication for the dashboard.
 *
 * Flow:
 *   1. User submits their email on `/login`.
 *   2. Gateway generates a random token, stores its SHA-256 hash in the
 *      `magic_links` D1 table with a short expiry (15 min), and sends
 *      an email with a link of the form `/auth/verify?token=<raw>`.
 *   3. User clicks the link. `consumeMagicLinkToken` hashes the raw
 *      token, looks up the row, marks it consumed atomically, and
 *      returns the owning email.
 *   4. The /auth/verify handler upserts a `users` row, signs a session
 *      JWT, and sets it as an httpOnly cookie.
 *
 * The raw token is never stored — only the hash. The email message
 * never contains sensitive data beyond the link itself.
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
 * Sends a magic-link email via the Resend API.
 *
 * Returns normally on success. If the API responds with a non-2xx status
 * the function throws with the body as detail so the caller can surface
 * a generic failure on the /login page.
 *
 * If `apiKey` is empty (local dev, no Resend configured), the function
 * resolves without making a network call so developers can read the
 * link directly from the response of the /login POST in tests.
 */
export async function sendMagicLinkEmail(args: {
  apiKey: string | undefined;
  from: string;
  to: string;
  url: string;
}): Promise<void> {
  if (!args.apiKey) return;
  const body = {
    from: args.from,
    to: [args.to],
    subject: "Your SceneView MCP sign-in link",
    html: renderMagicLinkHtml(args.url),
    text: renderMagicLinkText(args.url),
  };
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      authorization: `Bearer ${args.apiKey}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const detail = await response.text().catch(() => "");
    throw new Error(`resend: HTTP ${response.status} ${detail}`);
  }
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

/** Inline HTML used in the magic-link email body. */
function renderMagicLinkHtml(url: string): string {
  return (
    `<!doctype html><html><body style="font-family:system-ui,sans-serif;` +
    `max-width:560px;margin:2rem auto;color:#0f172a;">` +
    `<h2 style="color:#1e40af;">Sign in to SceneView MCP</h2>` +
    `<p>Click the button below to sign in. The link is valid for ` +
    `${MAGIC_LINK_TTL_MINUTES} minutes and can only be used once.</p>` +
    `<p><a href="${escapeHtml(url)}" style="display:inline-block;` +
    `padding:12px 24px;background:#1e40af;color:#fff;` +
    `text-decoration:none;border-radius:8px;font-weight:600;">` +
    `Sign in to your dashboard</a></p>` +
    `<p style="color:#64748b;font-size:0.875rem;">If the button does ` +
    `not work, copy and paste this URL:<br>` +
    `<a href="${escapeHtml(url)}">${escapeHtml(url)}</a></p>` +
    `<p style="color:#64748b;font-size:0.75rem;margin-top:2rem;">` +
    `If you did not request this email, you can safely ignore it.</p>` +
    `</body></html>`
  );
}

/** Plain-text fallback of the magic-link email body. */
function renderMagicLinkText(url: string): string {
  return (
    `Sign in to SceneView MCP\n\n` +
    `Click the link below to sign in. The link is valid for ` +
    `${MAGIC_LINK_TTL_MINUTES} minutes and can only be used once.\n\n` +
    `${url}\n\n` +
    `If you did not request this email, you can safely ignore it.`
  );
}

/** Minimal HTML escape for the URL embedded in the email body. */
function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
