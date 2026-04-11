/**
 * Authentication routes for the dashboard:
 *   - `GET  /login`         — minimal email form (HTML)
 *   - `POST /login`         — accepts an email, sends a magic link
 *   - `GET  /auth/verify`   — consumes a magic-link token, sets cookie
 *   - `POST /auth/logout`   — clears the session cookie
 *
 * The HTML returned by the GET handlers here is intentionally bare;
 * step 12 replaces them with styled Hono JSX pages that inherit from
 * `dashboard/layout.tsx`. The route module exports the handler in a
 * way that lets step 12 re-wire them without touching the POST logic.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import {
  DEFAULT_SESSION_TTL_SECONDS,
  signJwt,
} from "../auth/jwt.js";
import {
  consumeMagicLinkToken,
  createMagicLink,
  isValidEmail,
  sendMagicLinkEmail,
} from "../auth/magic-link.js";
import {
  buildClearSessionCookie,
  buildSessionCookie,
  shouldUseSecureCookies,
} from "../auth/session-middleware.js";
import { getUserByEmail, insertUser } from "../db/users.js";
import { newUserId } from "../auth/api-keys.js";

/** Default "from" address used when `env.MAGIC_LINK_FROM_EMAIL` is unset. */
const DEFAULT_FROM = "SceneView MCP <no-reply@sceneview.dev>";

/**
 * Returns a Hono router mounted at `/`.
 *
 * The `renderLoginPage` callback is injected by `index.ts`: in the raw
 * `routes/auth.ts` tests we pass a stub that returns plain text; the
 * production wiring uses the JSX dashboard layout.
 */
export function authRoutes(options?: {
  renderLoginPage?: (
    message: string | null,
    next: string | null,
  ) => string;
}): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();
  const render = options?.renderLoginPage ?? defaultLoginPage;

  app.get("/login", (c) => {
    const url = new URL(c.req.url);
    const next = url.searchParams.get("next");
    const message = url.searchParams.get("message");
    return c.html(render(message, next));
  });

  app.post("/login", async (c) => {
    const form = await c.req.parseBody();
    const email =
      typeof form.email === "string" ? form.email.trim().toLowerCase() : "";
    if (!isValidEmail(email)) {
      return c.html(
        render("Please enter a valid email address.", null),
        400,
      );
    }
    const baseUrl =
      c.env.DASHBOARD_BASE_URL?.replace(/\/+$/, "") ??
      inferBaseUrlFromRequest(c.req.url);
    try {
      const link = await createMagicLink({
        db: c.env.DB,
        email,
        baseUrl,
      });
      await sendMagicLinkEmail({
        apiKey: c.env.RESEND_API_KEY,
        from: c.env.MAGIC_LINK_FROM_EMAIL ?? DEFAULT_FROM,
        to: email,
        url: link.url,
      });
    } catch {
      // Never leak internal errors — the login page intentionally
      // always reports the same success message so we do not disclose
      // which emails exist in the user table.
    }
    return c.html(
      render(
        "Check your inbox. We just sent a sign-in link valid for 15 minutes.",
        null,
      ),
    );
  });

  app.get("/auth/verify", async (c) => {
    const token = c.req.query("token");
    if (!token) {
      return c.redirect("/login?message=Missing%20token", 302);
    }
    const email = await consumeMagicLinkToken(c.env.DB, token);
    if (!email) {
      return c.redirect(
        "/login?message=This%20link%20is%20invalid%20or%20has%20expired.",
        302,
      );
    }
    // Upsert the user row — the login flow is also the sign-up flow.
    let user = await getUserByEmail(c.env.DB, email);
    if (!user) {
      user = await insertUser(c.env.DB, { id: newUserId(), email });
    }
    if (!c.env.JWT_SECRET) {
      return c.text("Server misconfigured: JWT_SECRET is not set", 500);
    }
    const token2 = await signJwt(c.env.JWT_SECRET, {
      sub: user.id,
      ttlSeconds: DEFAULT_SESSION_TTL_SECONDS,
    });
    const cookie = buildSessionCookie({
      value: token2,
      maxAgeSeconds: DEFAULT_SESSION_TTL_SECONDS,
      secure: shouldUseSecureCookies(c.env),
    });
    const next = c.req.query("next");
    const destination = isSafeRedirect(next) ? next : "/dashboard";
    return new Response(null, {
      status: 302,
      headers: {
        location: destination,
        "set-cookie": cookie,
      },
    });
  });

  app.post("/auth/logout", (c) => {
    const cookie = buildClearSessionCookie(shouldUseSecureCookies(c.env));
    return new Response(null, {
      status: 303,
      headers: { location: "/", "set-cookie": cookie },
    });
  });

  return app;
}

/** Utility for the verify handler: accept only local paths, never URLs. */
function isSafeRedirect(value: string | undefined): value is string {
  if (!value) return false;
  if (!value.startsWith("/")) return false;
  if (value.startsWith("//")) return false;
  if (value.includes("://")) return false;
  return true;
}

/** Derives a base url from the request when `DASHBOARD_BASE_URL` is unset. */
function inferBaseUrlFromRequest(reqUrl: string): string {
  const u = new URL(reqUrl);
  return `${u.protocol}//${u.host}`;
}

/** Minimal fallback login page — replaced by Hono JSX in step 12. */
function defaultLoginPage(
  message: string | null,
  next: string | null,
): string {
  const safeMessage = message ? escapeHtml(message) : "";
  const hidden = next
    ? `<input type="hidden" name="next" value="${escapeHtml(next)}">`
    : "";
  return (
    `<!doctype html><html><body>` +
    `<h1>Sign in</h1>` +
    (safeMessage ? `<p>${safeMessage}</p>` : "") +
    `<form method="post" action="/login">` +
    hidden +
    `<input type="email" name="email" required>` +
    `<button type="submit">Send magic link</button>` +
    `</form></body></html>`
  );
}

/** Minimal HTML escape for inline text. */
function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
