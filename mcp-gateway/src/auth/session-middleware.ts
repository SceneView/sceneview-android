/**
 * Dashboard session middleware.
 *
 * Reads the `sv_session` cookie, verifies the embedded JWT against
 * `env.JWT_SECRET`, and if the signature is valid + the `sub` matches
 * a real user, injects the user into the Hono context under
 * `c.var.session`. This middleware is the inverse of
 * `auth/middleware.ts` — API keys protect `/mcp`, JWT cookies protect
 * `/dashboard` and `/billing` html routes.
 *
 * Middleware factories come in two flavours:
 *   - `optionalSession()` — never fails, exposes `session` if present
 *   - `requireSession()`  — returns a redirect to `/login` if absent
 */

import type { Context, MiddlewareHandler } from "hono";
import type { Env } from "../env.js";
import type { UserRow } from "../db/schema.js";
import { getUserById } from "../db/users.js";
import { verifyJwt } from "./jwt.js";

/** Name of the cookie holding the dashboard JWT. */
export const SESSION_COOKIE = "sv_session";

/** Hono `Variables` fragment exposed to downstream dashboard handlers. */
export interface SessionVariables {
  session?: { user: UserRow };
}

/** Factory: never fails, but populates `c.var.session` when a cookie exists. */
export function optionalSession(): MiddlewareHandler<{
  Bindings: Env;
  Variables: SessionVariables;
}> {
  return async (c, next) => {
    await populateSession(c);
    await next();
  };
}

/**
 * Factory: short-circuits to `/login?next=<original>` when no valid
 * session cookie is present.
 *
 * The redirect is a 302 on GET and a 303 on POST so browsers switch
 * back to GET when following it (important for HTMX form submissions).
 */
export function requireSession(): MiddlewareHandler<{
  Bindings: Env;
  Variables: SessionVariables;
}> {
  return async (c, next) => {
    await populateSession(c);
    if (!c.get("session")) {
      const url = new URL(c.req.url);
      const next = url.pathname + url.search;
      const login = `/login?next=${encodeURIComponent(next)}`;
      const status = c.req.method === "GET" ? 302 : 303;
      return c.redirect(login, status);
    }
    await next();
    return;
  };
}

/** Strongly typed accessor for routes. Throws if no session is in scope. */
export function getSession(
  c: Context<{ Variables: SessionVariables }>,
): { user: UserRow } {
  const s = c.get("session");
  if (!s) throw new Error("requireSession() must be mounted before this handler");
  return s;
}

// ── Internal helpers ────────────────────────────────────────────────────────

/** Populates `c.var.session` if and only if a valid cookie is present. */
async function populateSession(
  c: Context<{ Bindings: Env; Variables: SessionVariables }>,
): Promise<void> {
  const secret = c.env.JWT_SECRET;
  if (!secret) return;
  const token = readSessionCookie(c);
  if (!token) return;
  const verified = await verifyJwt(secret, token);
  if (!verified) return;
  const user = await getUserById(c.env.DB, verified.claims.sub);
  if (!user) return;
  c.set("session", { user });
}

/** Parses the cookie header and returns the `sv_session` value, if any. */
export function readSessionCookie(c: Context): string | null {
  const cookieHeader = c.req.header("cookie");
  if (!cookieHeader) return null;
  const parts = cookieHeader.split(";");
  for (const part of parts) {
    const [rawName, ...rest] = part.split("=");
    if (!rawName) continue;
    const name = rawName.trim();
    if (name === SESSION_COOKIE) {
      return rest.join("=").trim();
    }
  }
  return null;
}

/**
 * Builds a `Set-Cookie` header value for the dashboard session.
 *
 * Flags:
 *   - `HttpOnly` — inaccessible to JS
 *   - `Secure`   — only sent over HTTPS in prod; omitted in dev so
 *                  `wrangler dev` over http:// still sees the cookie
 *   - `SameSite=Lax` — allows top-level navigations (magic-link clicks)
 *     while blocking CSRF
 *   - `Path=/`
 *   - `Max-Age=<seconds>`
 */
export function buildSessionCookie(args: {
  value: string;
  maxAgeSeconds: number;
  secure?: boolean;
}): string {
  const attrs = [
    `${SESSION_COOKIE}=${args.value}`,
    "Path=/",
    "HttpOnly",
    "SameSite=Lax",
    `Max-Age=${args.maxAgeSeconds}`,
  ];
  if (args.secure !== false) attrs.push("Secure");
  return attrs.join("; ");
}

/** Builds the `Set-Cookie` header that clears the session cookie. */
export function buildClearSessionCookie(secure?: boolean): string {
  return buildSessionCookie({ value: "", maxAgeSeconds: 0, secure });
}

/**
 * Returns true when the current environment should emit `Secure` cookies.
 * Kept as a separate helper so tests can override if they need to.
 */
export function shouldUseSecureCookies(env: Env): boolean {
  return (env.ENVIRONMENT ?? "production") !== "development";
}
