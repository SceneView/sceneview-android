/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import { Layout } from "./layout.js";
import { renderToHtml } from "./render.js";

/** `/docs` page with install instructions per MCP client. */
export const Docs: FC = () => (
  <Layout
    title="Docs"
    description="Install SceneView MCP in Claude Desktop, Cursor, Zed, or any MCP-capable agent. Hosted HTTP endpoint plus legacy stdio fallback."
    active="docs"
  >
    <h1>Docs</h1>
    <p>
      Point any MCP-capable agent at the hosted gateway. Free tools work
      without authentication; Pro tools require an API key you receive
      on the <a href="/pricing">pricing</a> checkout success page.
    </p>

    <h2>Quickstart</h2>
    <ol style="color:var(--sv-fg-muted);line-height:1.8;">
      <li>
        Subscribe to Pro or Team on the <a href="/pricing">pricing page</a>.
      </li>
      <li>
        Copy the <code>sv_live_</code> key from the success page (it is
        only shown once).
      </li>
      <li>
        Paste it into your <code>claude_desktop_config.json</code> (or
        Cursor / Zed equivalent) under the <code>sceneview</code> server.
      </li>
      <li>
        Restart your MCP client and prompt away — the gateway is live at
        <code>https://sceneview-mcp.workers.dev/mcp</code>.
      </li>
    </ol>

    <h2>Claude Desktop</h2>
    <p>
      Add a new server to your config file (macOS:{" "}
      <code>~/Library/Application Support/Claude/claude_desktop_config.json</code>).
    </p>
    <pre><code>{`{
  "mcpServers": {
    "sceneview": {
      "url": "https://sceneview-mcp.workers.dev/mcp",
      "headers": {
        "Authorization": "Bearer sv_live_YOUR_KEY_HERE"
      }
    }
  }
}`}</code></pre>

    <h2>Cursor</h2>
    <p>
      Open Cursor Settings, go to <em>MCP</em>, add a new HTTP server:
    </p>
    <pre><code>{`{
  "name": "sceneview",
  "url": "https://sceneview-mcp.workers.dev/mcp",
  "headers": {
    "Authorization": "Bearer sv_live_YOUR_KEY_HERE"
  }
}`}</code></pre>

    <h2>Zed</h2>
    <p>
      Add the server to <code>~/.config/zed/settings.json</code> under
      the <code>context_servers</code> section.
    </p>

    <h2>Raw curl</h2>
    <p>
      The endpoint speaks Streamable HTTP JSON-RPC:
    </p>
    <pre><code>{`curl -X POST https://sceneview-mcp.workers.dev/mcp \\
  -H "Authorization: Bearer sv_live_YOUR_KEY_HERE" \\
  -H "Content-Type: application/json" \\
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'`}</code></pre>

    <h2>Local stdio (free mode)</h2>
    <p>
      The legacy npm package still works if you want the free tools
      without any network round-trip:
    </p>
    <pre><code>{`npx sceneview-mcp`}</code></pre>
    <p>
      Set <code>SCENEVIEW_API_KEY</code> to unlock Pro tools via the
      hosted proxy.
    </p>

    <h2>Usage and rate limits</h2>
    <ul style="color:var(--sv-fg-muted);line-height:1.8;">
      <li>Free: 60 calls/hour, 1 000 calls/month, 1 API key</li>
      <li>Pro: 600 calls/hour, 10 000 calls/month, 3 API keys</li>
      <li>Team: 3 000 calls/hour, 50 000 calls/month, 10 API keys</li>
    </ul>
    <p>
      Every <code>/mcp</code> response carries standard{" "}
      <code>X-RateLimit-Limit</code>,{" "}
      <code>X-RateLimit-Remaining</code>, and{" "}
      <code>X-RateLimit-Reset</code> headers so clients can pre-empt
      throttling.
    </p>

    <h2>Errors</h2>
    <p>
      All errors are valid JSON-RPC 2.0 responses. Common codes:
    </p>
    <ul style="color:var(--sv-fg-muted);line-height:1.8;">
      <li><code>-32001</code> Unauthorized — bad or missing API key</li>
      <li><code>-32002</code> Access denied — Pro tool on the free tier</li>
      <li><code>-32029</code> Rate limited — hourly or monthly quota exceeded</li>
    </ul>

    <h2>Need help?</h2>
    <p>
      Open an issue on{" "}
      <a href="https://github.com/sceneview/sceneview/issues">
        github.com/sceneview/sceneview
      </a>{" "}
      or email <a href="mailto:hello@sceneview.dev">hello@sceneview.dev</a>.
    </p>
  </Layout>
);

/** Top-level renderer used by the route handler. */
export function renderDocs(): Promise<string> {
  return renderToHtml(<Docs />);
}
