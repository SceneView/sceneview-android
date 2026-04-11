# hub-mcp-gateway

Gateway #2 of the Cloudflare Workers MCP stack — consolidates the 15+
non-SceneView MCPs from the portfolio (architecture, real estate,
french-admin, e-commerce, legal docs, finance, education, social media,
health & fitness, plus the 3D verticals) behind a single hosted
endpoint with unified **Portfolio Access** billing.

Sibling of [`mcp-gateway/`](../mcp-gateway) (Gateway #1, which serves
`sceneview-mcp` at `sceneview-mcp.mcp-tools-lab.workers.dev`).

## State: MVP scaffold

- [x] Worker shell (Hono + TypeScript + wrangler)
- [x] `/health` JSON endpoint
- [x] `/`, `/pricing`, `/docs` HTML routes
- [x] `POST /mcp` JSON-RPC endpoint (`initialize`, `tools/list`, `tools/call`)
- [x] 11 pilot libraries covering the whole active non-SceneView
      portfolio (stub dispatchers): `architecture`, `realestate`,
      `french_admin`, `ecommerce3d`, `legal_docs`, `finance`,
      `education`, `social_media`, `health_fitness`, `automotive3d`,
      `healthcare3d`
- [x] Vitest smoke tests for every route and per-library dispatch
- [ ] Auth middleware (to port from `mcp-gateway/src/auth/middleware.ts`)
- [ ] Rate limit middleware
- [ ] Stripe checkout routes + webhook (`/billing/checkout`, `/stripe/webhook`)
- [ ] Usage logging into the shared D1 `usage_logs` table
- [ ] Vendor real upstream tool handlers (replaces the 11 stub dispatchers)
- [ ] Publish 11 lite npm packages (`*-mcp@2.0.0-beta.1` proxy mode)

## Shared infrastructure with Gateway #1

By design, the hub gateway reuses Gateway #1's bindings so one
subscription covers both:

- D1: `sceneview-mcp` database (`8aaddcda-e36e-4287-9222-1df924426c9f`)
- KV: `RL_KV` namespace (`9a40d334be6149f7a4ba18451a60245f`)

An API key minted on Gateway #1 will authenticate on the hub as soon
as the auth middleware is ported. The schema does not need changes —
the tool/tier mapping is enforced application-side.

## Pricing

| Plan              | Price       | Tool calls / mo | Seats |
|-------------------|-------------|-----------------|-------|
| Free              | €0          | 100             | 1     |
| Portfolio Access  | **€29/mo**  | 20 000          | 1     |
| Team              | **€79/mo**  | 100 000         | 5     |

Portfolio Access and Team also cover Gateway #1 (`sceneview-mcp`) at
no extra cost. Stripe price ids are placeholders in `wrangler.toml` —
wire them when the Stripe dashboard entries are created.

## Dev

```bash
npm install
npm run dev        # wrangler dev
npm test           # vitest
npm run typecheck  # tsc --noEmit
```

## Deploy (once secrets are wired)

```bash
wrangler login
wrangler secret put STRIPE_SECRET_KEY
wrangler secret put STRIPE_WEBHOOK_SECRET
wrangler deploy
```

Public URL: `https://hub-mcp.mcp-tools-lab.workers.dev`

## Wiring a new vertical

1. Drop a module in `src/libraries/<id>.ts` exporting
   `TOOL_DEFINITIONS` and `dispatchTool` (see
   `src/libraries/architecture.ts` for the template).
2. Add an entry to the `LIBRARIES` array in `src/mcp/registry.ts`.
3. Prefix every tool with `<id>__` to avoid collisions (enforced at
   import time — the Worker fails to start if two libs share a name).
4. Add coverage in `test/mcp.test.ts` and run `npm test`.
5. Publish a lite npm package (`<id>-mcp@<next>-beta.1`) that
   forwards tool calls over `fetch` to
   `https://hub-mcp.mcp-tools-lab.workers.dev/mcp`.

## Session context

Scaffolded 2026-04-11 in the `multi-gateway-sprint` worktree. The
pilot library is intentionally a stub so the MVP can ship a
deployable worker today while follow-up sessions iterate on the real
tool wiring, auth, and Stripe plumbing.
