/**
 * Landing + pricing + docs HTML routes for the hub gateway.
 *
 * Rendered as plain HTML strings — no JSX, no build step, easy to
 * audit. The design intentionally mirrors Gateway #1's landing for
 * brand consistency while making the Portfolio value prop explicit.
 *
 * Routes:
 *   GET /        → landing (product pitch + CTA to /pricing)
 *   GET /pricing → Portfolio Access 29 EUR/mo + Team 79 EUR/mo
 *   GET /docs    → quick-start JSON-RPC curl snippets
 *   GET /health  → JSON registry summary (exposed by index.ts)
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import { getRegistrySummary } from "../mcp/registry.js";

const BASE_CSS = `
<style>
  :root {
    color-scheme: light dark;
    --bg: #ffffff;
    --fg: #0a0e1a;
    --muted: #5b6472;
    --accent: #2b6cff;
    --border: #e4e7ee;
    --card: #f6f8fc;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #0a0e1a;
      --fg: #f2f4f8;
      --muted: #8a93a6;
      --accent: #5b8dff;
      --border: #1c2233;
      --card: #11172a;
    }
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, system-ui, sans-serif;
    background: var(--bg);
    color: var(--fg);
    line-height: 1.55;
  }
  main {
    max-width: 960px;
    margin: 0 auto;
    padding: 3rem 1.5rem 4rem;
  }
  nav {
    max-width: 960px;
    margin: 0 auto;
    padding: 1.25rem 1.5rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid var(--border);
  }
  nav a { color: var(--fg); text-decoration: none; margin-left: 1.25rem; }
  nav a:hover { color: var(--accent); }
  .brand { font-weight: 600; letter-spacing: -0.01em; }
  h1 { font-size: 2.4rem; letter-spacing: -0.02em; margin: 0 0 1rem; }
  h2 { font-size: 1.4rem; letter-spacing: -0.01em; margin: 2.5rem 0 1rem; }
  p.lead { font-size: 1.15rem; color: var(--muted); max-width: 640px; }
  .grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); margin-top: 1.5rem; }
  .card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 1.25rem 1.25rem 1.5rem;
  }
  .card h3 { margin: 0 0 0.35rem; font-size: 1rem; }
  .card p { margin: 0; color: var(--muted); font-size: 0.92rem; }
  .btn {
    display: inline-block;
    background: var(--accent);
    color: white;
    padding: 0.7rem 1.25rem;
    border-radius: 10px;
    text-decoration: none;
    font-weight: 600;
    margin-top: 1.5rem;
  }
  .btn.secondary { background: transparent; border: 1px solid var(--border); color: var(--fg); }
  code, pre { font-family: "SF Mono", "JetBrains Mono", ui-monospace, monospace; }
  pre {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 1rem 1.25rem;
    overflow-x: auto;
    font-size: 0.85rem;
  }
  .price-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 2rem 1.75rem;
  }
  .price-card.featured { border-color: var(--accent); box-shadow: 0 8px 40px -20px color-mix(in srgb, var(--accent) 45%, transparent); }
  .price { font-size: 2.4rem; font-weight: 700; letter-spacing: -0.02em; }
  .price small { font-size: 0.95rem; color: var(--muted); font-weight: 400; margin-left: 0.3rem; }
  ul.features { list-style: none; padding: 0; margin: 1rem 0 0; }
  ul.features li { padding: 0.35rem 0; color: var(--muted); }
  ul.features li::before { content: "✓ "; color: var(--accent); font-weight: 700; }
  .muted { color: var(--muted); }
</style>
`;

const LAYOUT = (title: string, body: string) => `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${title}</title>
    ${BASE_CSS}
  </head>
  <body>
    <nav>
      <a class="brand" href="/">hub-mcp</a>
      <div>
        <a href="/pricing">Pricing</a>
        <a href="/docs">Docs</a>
        <a href="/health">Status</a>
      </div>
    </nav>
    <main>${body}</main>
  </body>
</html>`;

/** All HTML routes mounted under `/` by `src/index.ts`. */
export function landingRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  // ── GET / — landing ──────────────────────────────────────────────────────
  app.get("/", (c) =>
    c.html(
      LAYOUT(
        "hub-mcp — one subscription for the whole MCP portfolio",
        `
        <h1>One API, 15+ verticals.</h1>
        <p class="lead">
          hub-mcp is a single hosted gateway for the non-SceneView MCP
          portfolio: architecture, real&nbsp;estate, french-admin,
          e-commerce, legal docs, finance, education, social media,
          health &amp; fitness, and the active 3D verticals. One API
          key. One subscription. Every tool.
        </p>
        <a class="btn" href="/pricing">See pricing</a>
        <a class="btn secondary" href="/docs">Quick start</a>

        <h2>What's in the hub</h2>
        <div class="grid">
          <div class="card"><h3>architecture-mcp</h3><p>Building typologies, floor plans, glTF/USDZ assets.</p></div>
          <div class="card"><h3>realestate-mcp</h3><p>Listing tools + 3D staging assets (pilot next).</p></div>
          <div class="card"><h3>french-admin-mcp</h3><p>Impôts, CAF, démarches administratives (pilot next).</p></div>
          <div class="card"><h3>ecommerce-3d-mcp</h3><p>Product configurators + virtual try-on (pilot next).</p></div>
          <div class="card"><h3>legal-docs-mcp</h3><p>Contract templates + clause library (pilot next).</p></div>
          <div class="card"><h3>finance-mcp</h3><p>Market data + portfolio tools (pilot next).</p></div>
          <div class="card"><h3>education-mcp</h3><p>Curriculum planning + lesson generation (pilot next).</p></div>
          <div class="card"><h3>social-media-mcp</h3><p>Content scheduling + analytics (pilot next).</p></div>
          <div class="card"><h3>health-fitness-mcp</h3><p>Workouts, nutrition, wearables (pilot next).</p></div>
        </div>
        <p class="muted" style="margin-top: 1.5rem;">
          Need SceneView 3D/AR tools? Those live on a separate gateway:
          <a href="https://sceneview-mcp.mcp-tools-lab.workers.dev">sceneview-mcp</a>.
          A single API key works on both.
        </p>
      `,
      ),
    ),
  );

  // ── GET /pricing — Portfolio Access + Team ───────────────────────────────
  app.get("/pricing", (c) =>
    c.html(
      LAYOUT(
        "hub-mcp pricing",
        `
        <h1>Portfolio pricing</h1>
        <p class="lead">
          One plan. Every MCP in the hub. Pay monthly, cancel anytime.
        </p>
        <div class="grid" style="margin-top: 2rem;">
          <div class="price-card">
            <h3>Free</h3>
            <div class="price">€0<small>/mo</small></div>
            <ul class="features">
              <li>100 tool calls / month</li>
              <li>All public tools across the hub</li>
              <li>Community support on GitHub Discussions</li>
            </ul>
            <a class="btn secondary" href="/docs">Get started</a>
          </div>

          <div class="price-card featured">
            <h3>Portfolio Access</h3>
            <div class="price">€29<small>/mo</small></div>
            <ul class="features">
              <li>20 000 tool calls / month</li>
              <li>Every MCP in the hub (Pro tools unlocked)</li>
              <li>Access to Gateway #1 (sceneview-mcp) included</li>
              <li>Email support</li>
            </ul>
            <a class="btn" href="/billing/checkout?plan=portfolio-monthly">Subscribe</a>
          </div>

          <div class="price-card">
            <h3>Team</h3>
            <div class="price">€79<small>/mo</small></div>
            <ul class="features">
              <li>100 000 tool calls / month</li>
              <li>5 seats included</li>
              <li>Priority support</li>
              <li>Early access to new vertical MCPs</li>
            </ul>
            <a class="btn secondary" href="/billing/checkout?plan=team-monthly">Subscribe</a>
          </div>
        </div>

        <h2>FAQ</h2>
        <p class="muted">
          <strong>Do I need a separate sub for sceneview-mcp?</strong><br />
          No — Portfolio Access (and Team) are honored on both gateways.
          Use the same API key against <code>hub-mcp.mcp-tools-lab.workers.dev/mcp</code>
          or <code>sceneview-mcp.mcp-tools-lab.workers.dev/mcp</code>.
        </p>
        <p class="muted">
          <strong>What happens if I exceed my quota?</strong><br />
          Tool calls beyond the monthly cap return a <code>429</code>
          (or JSON-RPC <code>-32002</code>) with an
          <code>X-RateLimit-Reset</code> header. Your sub renews on the
          first of the following month.
        </p>
      `,
      ),
    ),
  );

  // ── GET /docs — quick-start ───────────────────────────────────────────────
  app.get("/docs", (c) => {
    const summary = getRegistrySummary();
    return c.html(
      LAYOUT(
        "hub-mcp docs",
        `
        <h1>Quick start</h1>
        <p class="lead">
          hub-mcp speaks JSON-RPC 2.0 over a single <code>POST /mcp</code>
          endpoint. Your API key (once Stripe checkout is wired) goes in
          the <code>Authorization: Bearer ...</code> header. The
          Streamable HTTP profile is used — no SSE yet.
        </p>

        <h2>List available tools</h2>
        <pre><code>curl -X POST ${getBaseUrl(c.env)}/mcp \\
  -H "content-type: application/json" \\
  -H "authorization: Bearer YOUR_API_KEY" \\
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'</code></pre>

        <h2>Call a tool</h2>
        <pre><code>curl -X POST ${getBaseUrl(c.env)}/mcp \\
  -H "content-type: application/json" \\
  -H "authorization: Bearer YOUR_API_KEY" \\
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "architecture__list_building_types",
      "arguments": {}
    }
  }'</code></pre>

        <h2 id="claude-desktop">Claude Desktop</h2>
        <p class="muted">
          Claude Desktop only supports <strong>stdio</strong> MCP servers.
          A lite npm package (<code>hub-mcp</code>) is being prepared — once published,
          add this to your <code>claude_desktop_config.json</code>:
        </p>
        <pre><code>{
  "hub-mcp": {
    "command": "npx",
    "args": ["-y", "hub-mcp@latest"],
    "env": {
      "HUB_API_KEY": "YOUR_API_KEY"
    }
  }
}</code></pre>
        <p class="muted" style="margin-top:1rem;">
          <strong>Coming soon</strong> — the npm package is not published yet.
          In the meantime, use the curl/HTTP approach above with any
          MCP client that supports HTTP transport (Cursor, Zed, custom agents).
        </p>

        <h2>Current registry</h2>
        <p class="muted">
          The hub exposes <strong>${summary.totalTools} tools</strong>
          across <strong>${summary.libraries.length} libraries</strong>
          today. Every library is added in a follow-up session after
          the MVP pilot is validated — see the
          <a href="https://github.com/sceneview-tools/">sceneview-tools org</a>
          for progress.
        </p>
      `,
      ),
    );
  });

  return app;
}

function getBaseUrl(env: Env): string {
  return env.GATEWAY_BASE_URL ?? "https://hub-mcp.mcp-tools-lab.workers.dev";
}
