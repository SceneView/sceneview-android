/**
 * `/checkout/success` — displayed when Stripe redirects the buyer back
 * after a successful payment.
 *
 * The webhook has stashed the plaintext API key under
 * `checkout_key:{session_id}` in KV with a 24h TTL. This route reads
 * that entry ONCE, deletes it immediately (single-use), and renders
 * an HTML page showing:
 *
 *   - The API key in a copyable code block
 *   - Claude Desktop JSON config snippet
 *   - The user's tier (Portfolio Access or Team)
 *   - A reminder that the key is shown once and won't appear again
 *
 * If the KV entry is missing (expired, already consumed, or the
 * webhook raced us), we render a "consumed" page that says to check
 * email or contact support.
 */

import { Hono } from "hono";
import type { Env } from "../env.js";
import {
  CHECKOUT_KEY_KV_PREFIX,
  type CheckoutKeyEntry,
} from "../billing/key-provisioning.js";

function isCheckoutKeyEntry(v: unknown): v is CheckoutKeyEntry {
  if (typeof v !== "object" || v === null) return false;
  const o = v as Record<string, unknown>;
  return (
    typeof o.plaintext === "string" &&
    typeof o.email === "string" &&
    (o.tier === "pro" || o.tier === "team")
  );
}

export function checkoutSuccessRoutes(): Hono<{ Bindings: Env }> {
  const app = new Hono<{ Bindings: Env }>();

  app.get("/checkout/success", async (c) => {
    const sessionId = c.req.query("session_id") ?? null;
    if (!sessionId || !sessionId.startsWith("cs_")) {
      return c.html(renderConsumed(), 400);
    }

    const kvKey = `${CHECKOUT_KEY_KV_PREFIX}${sessionId}`;
    const raw = await c.env.RL_KV.get(kvKey);
    if (!raw) {
      return c.html(renderConsumed(), 410);
    }

    let entry: CheckoutKeyEntry | null = null;
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (isCheckoutKeyEntry(parsed)) entry = parsed;
    } catch {
      entry = null;
    }

    // Single-use: delete immediately after reading.
    try {
      await c.env.RL_KV.delete(kvKey);
    } catch {
      // KV TTL is the backstop.
    }

    if (!entry) {
      return c.html(renderConsumed(), 410);
    }

    return c.html(renderSuccess(entry));
  });

  return app;
}

// ── HTML renderers ────────────────────────────────────────────────────────

const TIER_LABEL: Record<string, string> = {
  pro: "Portfolio Access",
  team: "Team",
};

function renderSuccess(entry: CheckoutKeyEntry): string {
  const tierName = TIER_LABEL[entry.tier] ?? entry.tier;
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>hub-mcp — Welcome!</title>
  <style>
    :root { color-scheme: light dark; --bg: #0a0e1a; --fg: #f2f4f8; --muted: #8a93a6; --accent: #5b8dff; --card: #11172a; --border: #1c2233; --green: #34d399; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, system-ui, sans-serif; background: var(--bg); color: var(--fg); line-height: 1.55; }
    main { max-width: 720px; margin: 0 auto; padding: 3rem 1.5rem 4rem; }
    h1 { font-size: 2rem; letter-spacing: -0.02em; margin: 0 0 0.5rem; }
    .badge { display: inline-block; background: var(--green); color: #000; font-size: 0.85rem; font-weight: 600; padding: 0.25rem 0.75rem; border-radius: 6px; }
    p { color: var(--muted); }
    .key-box { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 1.25rem; margin: 1.5rem 0; position: relative; }
    .key-box code { font-family: "SF Mono", "JetBrains Mono", ui-monospace, monospace; font-size: 0.95rem; word-break: break-all; color: var(--green); }
    .key-box .copy-btn { position: absolute; top: 0.75rem; right: 0.75rem; background: var(--accent); color: white; border: none; border-radius: 8px; padding: 0.4rem 0.8rem; font-size: 0.8rem; cursor: pointer; }
    .warning { background: #2d1f00; border: 1px solid #5c3d00; border-radius: 10px; padding: 1rem 1.25rem; margin: 1.5rem 0; color: #fbbf24; font-size: 0.9rem; }
    pre { background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 1rem 1.25rem; overflow-x: auto; font-size: 0.85rem; color: var(--fg); }
    a { color: var(--accent); }
  </style>
</head>
<body>
  <main>
    <h1>Welcome to hub-mcp!</h1>
    <span class="badge">${tierName}</span>
    <p>Your subscription is active and your API key is ready. <strong>This key is shown once</strong> — copy it now and store it somewhere safe.</p>

    <div class="key-box">
      <code id="api-key">${entry.plaintext}</code>
      <button class="copy-btn" onclick="navigator.clipboard.writeText(document.getElementById('api-key').textContent);this.textContent='Copied!'">Copy</button>
    </div>

    <div class="warning">
      This API key will NOT be shown again. If you lose it, you'll need to contact support to get a new one.
    </div>

    <h2>Claude Desktop config</h2>
    <p>Add this to your <code>claude_desktop_config.json</code>:</p>
    <pre><code>{
  "mcpServers": {
    "hub-mcp": {
      "command": "npx",
      "args": ["-y", "architecture-mcp@beta"],
      "env": {
        "HUB_MCP_API_KEY": "${entry.plaintext}"
      }
    }
  }
}</code></pre>

    <h2>Direct API usage</h2>
    <pre><code>curl -X POST https://hub-mcp.mcp-tools-lab.workers.dev/mcp \\
  -H "content-type: application/json" \\
  -H "authorization: Bearer ${entry.plaintext}" \\
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'</code></pre>

    <p>Your key works on <strong>both</strong> gateways:</p>
    <ul>
      <li><code>hub-mcp.mcp-tools-lab.workers.dev/mcp</code> — this hub (45 tools)</li>
      <li><code>sceneview-mcp.mcp-tools-lab.workers.dev/mcp</code> — SceneView 3D/AR</li>
    </ul>

    <p><a href="/pricing">View your plan</a> &middot; <a href="/docs">Quick start</a></p>
  </main>
</body>
</html>`;
}

function renderConsumed(): string {
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>hub-mcp — Key already consumed</title>
  <style>
    :root { color-scheme: light dark; --bg: #0a0e1a; --fg: #f2f4f8; --muted: #8a93a6; --accent: #5b8dff; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, system-ui, sans-serif; background: var(--bg); color: var(--fg); line-height: 1.55; }
    main { max-width: 600px; margin: 0 auto; padding: 3rem 1.5rem 4rem; }
    h1 { font-size: 1.8rem; margin: 0 0 1rem; }
    p { color: var(--muted); }
    a { color: var(--accent); }
  </style>
</head>
<body>
  <main>
    <h1>API key already consumed</h1>
    <p>This checkout success link has already been used, or it has expired (24h max). Your API key was shown once on the first visit.</p>
    <p>If you didn't copy your key, you can:</p>
    <ul>
      <li>Check your email — the key prefix was logged at checkout time</li>
      <li>Open a GitHub issue at <a href="https://github.com/sceneview/sceneview/issues">sceneview/sceneview</a> and we'll generate a replacement</li>
    </ul>
    <p><a href="/pricing">Back to pricing</a></p>
  </main>
</body>
</html>`;
}
