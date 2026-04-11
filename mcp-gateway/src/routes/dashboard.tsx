/** @jsxImportSource hono/jsx */

/**
 * Dashboard HTML routes: landing, pricing, docs, login (via auth),
 * `/dashboard`, `/billing`, and the HTMX fragment endpoints for API
 * keys (`POST /dashboard/keys`, `POST /dashboard/keys/:id/revoke`).
 *
 * These routes render Hono JSX server-side. HTMX is used only for the
 * two in-page swaps; every other navigation is a plain HTTP request
 * that renders a full document so the site works without JavaScript.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import {
  optionalSession,
  requireSession,
  type SessionVariables,
} from "../auth/session-middleware.js";
import { renderLanding } from "../dashboard/landing.js";
import { renderPricing } from "../dashboard/pricing.js";
import { renderDocs } from "../dashboard/docs.js";
import { renderDashboard } from "../dashboard/index.js";
import { renderBillingPage } from "../dashboard/billing-page.js";
import { KeyRow, NewKeyAlert } from "../dashboard/components/key-row.js";
import { renderToHtml } from "../dashboard/render.js";
import {
  createApiKey,
  listApiKeys,
  revokeApiKey,
} from "../auth/api-keys.js";
import { getApiKeyById } from "../db/api-keys.js";
import { listSubscriptionsByUser } from "../db/subscriptions.js";
import {
  countSuccessfulUsageInMonth,
  listDailyUsage,
  monthBucket,
} from "../db/usage.js";
import { getLimitsForTier } from "../rate-limit/limits.js";

/** Mounts the HTML-returning dashboard routes on a Hono router. */
export function dashboardRoutes(): Hono<{
  Bindings: Env;
  Variables: SessionVariables;
}> {
  const app = new Hono<{ Bindings: Env; Variables: SessionVariables }>();

  // ── Public pages ─────────────────────────────────────────────────────────

  app.get("/", optionalSession(), async (c) => {
    const html = await renderLanding(!!c.get("session"));
    return c.html(html);
  });

  app.get("/pricing", optionalSession(), async (c) => {
    const html = await renderPricing(!!c.get("session"));
    return c.html(html);
  });

  app.get("/docs", optionalSession(), async (c) => {
    const html = await renderDocs(!!c.get("session"));
    return c.html(html);
  });

  // ── Authenticated pages ──────────────────────────────────────────────────

  app.get("/dashboard", requireSession(), async (c) => {
    const session = c.get("session")!;
    const user = session.user;

    const [keys, usage] = await Promise.all([
      listApiKeys(c.env.DB, user.id),
      listDailyUsage(c.env.DB, user.id, 30),
    ]);

    // Monthly quota: sum "ok" calls across all user keys for the current month.
    const bucket = monthBucket();
    let used = 0;
    for (const key of keys) {
      used += await countSuccessfulUsageInMonth(c.env.DB, key.id, bucket);
    }
    const limit = getLimitsForTier(user.tier).monthly;

    const html = await renderDashboard({
      user,
      keys,
      usage,
      monthlyQuota: { used, limit },
    });
    return c.html(html);
  });

  app.get("/billing", requireSession(), async (c) => {
    const session = c.get("session")!;
    const subs = await listSubscriptionsByUser(c.env.DB, session.user.id);
    const active = subs.find((s) => s.status === "active" || s.status === "trialing");
    const html = await renderBillingPage({
      user: session.user,
      subscription: active ?? null,
    });
    return c.html(html);
  });

  // ── HTMX fragment endpoints ──────────────────────────────────────────────

  app.post("/dashboard/keys", requireSession(), async (c) => {
    const session = c.get("session")!;
    const form = await c.req.parseBody();
    const name = typeof form.name === "string" ? form.name.trim() : "";
    const created = await createApiKey(c.env.DB, session.user.id, name);
    const html = await renderToHtml(
      <NewKeyAlert
        plaintext={created.plaintext}
        name={created.row.name}
      />,
    );
    return c.html(html.replace(/^<!doctype html>/i, ""));
  });

  app.post(
    "/dashboard/keys/:id/revoke",
    requireSession(),
    async (c) => {
      const session = c.get("session")!;
      const keyId = c.req.param("id");
      await revokeApiKey(c.env.DB, keyId, session.user.id);
      const row = await getApiKeyById(c.env.DB, keyId);
      if (!row || row.user_id !== session.user.id) {
        return c.text("", 404);
      }
      const html = await renderToHtml(<KeyRow row={row} />);
      return c.html(html.replace(/^<!doctype html>/i, ""));
    },
  );

  return app;
}
