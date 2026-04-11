# NOTICE — 2026-04-11 21:51 — MCP Gateway is LIVE, accepting real payments

**From:** session in worktree `bold-rhodes` (guided walkthrough with the user, agent: Claude Opus 4.6 1M)
**Commits on `main`:** `73509a95` → `88aec77b` → `25ce60f9` → `6c938b3d`
**Severity:** 🔴 **Read this if your worktree touches `mcp-gateway/`, `mcp/` (the npm package), Stripe anything, or the `sceneview-mcp` product in any way.**

## What shipped

The full "first paying customer" pipeline went LIVE. The gateway is no longer in test mode — real cards can be charged starting now, starting with the first user who clicks Subscribe on [`https://sceneview-mcp.mcp-tools-lab.workers.dev/pricing`](https://sceneview-mcp.mcp-tools-lab.workers.dev/pricing).

### 1. Stripe account fully activated (LIVE mode)

- **KYC validated instantly** (entity shared with the existing Thomas Gorisse / GitHub Sponsors / Polar Stripe entity — no 24-48 h manual review).
- Legal structure: **auto-entrepreneur existant** (not SASU — fiscal decision made this session, do not revisit unless annual revenue approaches the 36 800 EUR franchise-en-base threshold for prestations de services).
- Stripe Tax **disabled** on purpose (no VAT collection obligation under franchise en base de TVA → adding Stripe Tax now would add 0.5% per transaction for zero compliance benefit; revisit if/when CA approaches thresholds).
- Stripe Climate **disabled** on purpose (reassess after first 50 paying customers).

### 2. 4 products created in the Stripe LIVE catalogue

| Plan | Product ID | Price ID (LIVE) | Amount |
|---|---|---|---|
| Pro Monthly | `prod_UJji2OHd9mMLkM` | `price_1TL6FLEr7tnnFQbdmgSwz5Ow` | 19 EUR / month |
| Pro Yearly | `prod_UJjoWo4eB4eNq9` | `price_1TL6KREr7tnnFQbdifEbYYcG` | 190 EUR / year |
| Team Monthly | `prod_UJjod2rU8MRwpz` | `price_1TL6L9Er7tnnFQbdC9CDxQNY` | 49 EUR / month |
| Team Yearly | `prod_UJjrjhJweUxoCf` | `price_1TL6NVEr7tnnFQbdVNLFF9lN` | 490 EUR / year |

These are the **only** plan→price mappings currently wired in `mcp-gateway/wrangler.toml` vars. Yearly prices are intentionally `monthly × 10` ("save 2 months yearly"), not `× 12`.

### 3. Webhook endpoint wired

- **ID:** `we_1TL7HfEr7tnnFQbdFDu7bmUr`
- **URL:** `https://sceneview-mcp.mcp-tools-lab.workers.dev/stripe/webhook`
- **Events (5, do not add more):** `checkout.session.completed`, `customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_failed`
- **Signing secret** (`whsec_...`): stored as Cloudflare Secret `STRIPE_WEBHOOK_SECRET`, never committed.

### 4. Cloudflare Worker deployed with live secrets

- **Version ID at go-live:** `5947f365-b55b-425c-ab28-f3392caba1c4`
- **Secrets rotated from test to live:** `STRIPE_SECRET_KEY` (`sk_live_...`), `STRIPE_WEBHOOK_SECRET` (`whsec_...`). Set via `wrangler secret put` interactively, never in env vars, never in shell history.
- **Compromised restricted key `rk_live_51TKzezEr7tnnFQbdyOTv...`** was rolled via the Stripe dashboard (expired) because it was briefly exposed in a terminal history during price-id retrieval. The live sk_live_ currently in use is a fresh standard secret key revealed after that rollback, never exposed in any chat.

### 5. Critical production bug fixed (commit `88aec77b`)

Before today, `POST /billing/checkout` was returning:

```
502  Stripe error: `customer_creation` can only be used in `payment` mode.
```

…every single time the call had no `customerId` and no `customerEmail` — which is the common case for the Stripe-first pivot (anonymous checkout). **No paying customer could have completed a checkout since the Stripe-first refactor (`673ddd88`) shipped.** This was a silent, latent bug that would have blocked revenue indefinitely.

Fix: drop the `form.customer_creation = "always"` branch from `src/billing/stripe-client.ts:createCheckoutSession()`. In `mode: "subscription"`, Stripe auto-creates the customer anyway; the flag only applies to `mode: "payment"` and returns a 400 when sent in subscription mode.

One test in `test/billing-routes.test.ts` was updated to assert `body.not.toContain("customer_creation")`. 168/168 gateway tests still pass.

### 6. `sceneview-mcp@4.0.0-beta.1` on npm (tag `beta`, not `latest`)

```
npm view sceneview-mcp dist-tags
{ latest: '3.6.4', beta: '4.0.0-beta.1' }
```

- `@latest = 3.6.4` is **unchanged**. The 3 450 DL/mo existing users keep running the local-only package, no breakage.
- `@beta = 4.0.0-beta.1` is the **lite proxy** package: free tools dispatch locally, Pro tools route to the gateway `/mcp` endpoint with the `SCENEVIEW_API_KEY` Bearer.
- Lite mode banner on stderr confirms whether `SCENEVIEW_API_KEY` is set:
  - unset → `[sceneview-mcp] v4.0.0-beta.1 — LITE (free tools only)` + upsell URL
  - set → `[sceneview-mcp] v4.0.0-beta.1 — HOSTED (Pro tools → gateway)`
- Source: `mcp/src/proxy.ts` (225 lines, 17 tests in `proxy.test.ts`).

### 7. Claude Desktop config wired in LITE mode

`~/Library/Application Support/Claude/claude_desktop_config.json` now contains:

```json
"sceneview": {
  "command": "npx",
  "args": ["-y", "sceneview-mcp@beta"],
  "env": {}
}
```

Adds the 15 free tools to Claude Desktop at next restart. Add `SCENEVIEW_API_KEY` to `env` to unlock Pro tools after completing a real checkout.

### 8. End-to-end smoke test results (2026-04-11 21:51)

```
/health                              200 ok
/pricing                              200
/mcp (no auth)                        401
/billing/checkout  plan=pro-monthly   303 → cs_live_a1LlcMRmSBw0DeQJlv
/billing/checkout  plan=pro-yearly    303 → cs_live_a1hsz6TXoXEZcqWUVx
/billing/checkout  plan=team-monthly  303 → cs_live_a1QUAFSJu7BXLNoQpA
/billing/checkout  plan=team-yearly   303 → cs_live_a1DrR2H2FupqAmJXVD
```

All four plans redirect to **real Stripe Checkout sessions in LIVE mode** (`cs_live_...`, not `cs_test_...`). Team Monthly first returned a 502 during Cloudflare PoP propagation — three retries one second apart all returned 303; transient, not reproducible.

End-to-end MCP dispatch was also validated earlier today against a synthetic API key injected directly into D1 (then cleaned up) — real stdio JSON-RPC round-trip through the lite package, through the Worker, through the tool registry, back to the client.

## ⚠️ Side effects and gotchas for other worktrees

### Do not revert any of these commits

```
6c938b3d  feat(mcp-gateway): wire live Stripe price ids — GO-LIVE
88aec77b  fix(mcp-gateway): drop customer_creation=always in subscription mode
25ce60f9  chore(mcp-gateway): add go-live script for Stripe TEST → LIVE cutover
73509a95  feat(mcp): v4.0.0-beta.1 lite package — Pro tools proxied to hosted gateway
```

Rebasing old branches (pre-`c5d95b2c`) onto current main will resurrect the TEST price ids in `mcp-gateway/wrangler.toml` if the old branch touched that file. **Re-resolve conflicts in favour of the new price_1TL6... ids**, never re-introduce price_1TL04h... / price_1TL08S... — those are stale test-mode ids that Stripe will reject.

### Do not re-publish `sceneview-mcp@latest` to 4.0.0 yet

The `@latest` tag stays on 3.6.4 until we have at least one real paying customer validating the full pipeline end-to-end (Stripe → webhook → D1 → KV handoff → /checkout/success → client enters SCENEVIEW_API_KEY). Bumping `@latest` to 4.x **now** would break the 3 450 DL/mo existing users by silently removing local Pro tool access.

### Do not modify `mcp-gateway/src/billing/stripe-client.ts` without reading `88aec77b` first

The `createCheckoutSession` function no longer sets `customer_creation` at all. This is **intentional**. If you are adding a new code path that calls it with `mode: "payment"` (one-off charge), you'll need to add `customer_creation` back *conditionally* guarded by `mode === "payment"`. If you are adding a new subscription code path, leave it alone — Stripe auto-creates the customer in subscription mode.

### Do not regenerate the Stripe API key or rotate secrets without coordination

`STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` are already set in Cloudflare Secrets. Running `wrangler secret put` again will prompt for a new value and overwrite them — if you don't have the right values at hand, you'll black-hole revenue. The sk_live_ is revealable again via dashboard.stripe.com/apikeys + 2FA. The whsec_ is revealable via dashboard.stripe.com/workbench/webhooks/we_1TL7HfEr7tnnFQbdFDu7bmUr + 👁 icon.

### Session B (`multi-gateway-sprint`) specifically — do not duplicate the customer_creation bug

When you scaffold the second gateway (for architecture, realestate, french-admin, finance, education, health-fitness, social-media, legal-docs, ecommerce-3d, automotive, healthcare, etc.), **copy `88aec77b`'s fixed version** of `src/billing/stripe-client.ts`, not any pre-refactor version you might find in old branches. The bug was latent for weeks in the Stripe-first refactor.

## What's NOT done (still manual / follow-up)

- **First real paying customer** — requires a human with a real credit card to click Subscribe and submit. Not a tech blocker; it's the natural consequence of "we just went live, nobody's bought yet".
- **Marketing / promotion** — the go-live was silent. No LinkedIn post, no Reddit announcement, no Hacker News, no email blast. Session D and future growth sessions own this.
- **`sceneview-mcp@latest` bump** — stays on 3.6.4 until at least one end-to-end real checkout succeeds.
- **Custom domain** `mcp.sceneview.dev` — still serving on `sceneview-mcp.mcp-tools-lab.workers.dev`. Nice-to-have, not blocking revenue.

## Where to find more

- `mcp-gateway/wrangler.toml` — live price ids + D1 + KV + DASHBOARD_BASE_URL wiring
- `.claude/scripts/mcp-gateway-golive.sh` — the (now obsolete) one-shot cutover script from commit `25ce60f9`; kept in repo as reference for any future TEST→LIVE flip
- `.claude/plans/mcp-gateway-golive.md` — the broader go-live plan (older, written before this session)
- `profile-private/preferences/api-keys.md` — Stripe account context (entity name, email, fiscal decision rationale — reference for future sessions touching Stripe)

---

**Bottom line for sister sessions:** SceneView MCP is now a revenue-generating product. Treat the pipeline as production-critical from this commit forward. If your change affects `mcp-gateway/` or `mcp/`, add a post-change smoke test against `/health` and `POST /billing/checkout plan=pro-monthly` expecting `303 → cs_live_...` before pushing.
