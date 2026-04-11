/**
 * Dashboard auth routes — DISABLED in the MVP.
 *
 * The SceneView MCP gateway does not ship a dashboard login in the MVP:
 * API keys are handed out directly on the `/checkout/success` page
 * after a successful Stripe checkout. The magic-link code is still on
 * disk in `auth/magic-link.ts` and `db/magic-links.ts` for a future
 * re-introduction of a self-serve dashboard, but none of it is wired
 * into the public routes below.
 *
 * Historical routes:
 *   - `GET  /login`         — used to render an email form
 *   - `POST /login`         — used to send a magic-link email
 *   - `GET  /auth/verify`   — used to consume a magic-link token
 *   - `POST /auth/logout`   — used to clear the session cookie
 *
 * They now return HTTP 503 (Service Unavailable) with a plain-text
 * body that points the caller at `/pricing` so old bookmarks and
 * links resolve gracefully.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";

/** Body returned by every disabled auth route. */
const DISABLED_MESSAGE =
  "Dashboard sign-in is disabled in the MVP. Subscribe on /pricing " +
  "to receive your API key directly on the checkout success page.";

/**
 * Returns a Hono router mounted at `/`.
 *
 * The previous implementation accepted a `renderLoginPage` callback to
 * let the JSX dashboard replace the inline fallback; that injection
 * point is no longer used but is preserved in the signature (ignored)
 * so callers compile without changes.
 */
export function authRoutes(_options?: {
  renderLoginPage?: unknown;
}): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.get("/login", (c) => c.text(DISABLED_MESSAGE, 503));
  app.post("/login", (c) => c.text(DISABLED_MESSAGE, 503));
  app.get("/auth/verify", (c) => c.text(DISABLED_MESSAGE, 503));
  app.post("/auth/logout", (c) => c.text(DISABLED_MESSAGE, 503));

  return app;
}
