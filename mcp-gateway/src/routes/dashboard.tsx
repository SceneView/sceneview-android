/** @jsxImportSource hono/jsx */

/**
 * Public HTML routes: landing, pricing, docs.
 *
 * The MVP gateway has no dashboard login: the previous `/dashboard`,
 * `/billing`, `/dashboard/keys` and `/dashboard/keys/:id/revoke`
 * routes required a session and are no longer mounted. Provisioning
 * now happens entirely through Stripe Checkout, and buyers see their
 * API key once on `/checkout/success`.
 *
 * These routes render Hono JSX server-side. HTMX is loaded from the
 * layout but is not currently used on any of these pages.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { renderLanding } from "../dashboard/landing.js";
import { renderPricing } from "../dashboard/pricing.js";
import { renderDocs } from "../dashboard/docs.js";

/** Mounts the public HTML dashboard routes on a Hono router. */
export function dashboardRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.get("/", async (c) => {
    const html = await renderLanding();
    return c.html(html);
  });

  app.get("/pricing", async (c) => {
    const html = await renderPricing();
    return c.html(html);
  });

  app.get("/docs", async (c) => {
    const html = await renderDocs();
    return c.html(html);
  });

  return app;
}
