/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import { Layout } from "./layout.js";
import { renderToHtml } from "./render.js";

/** Public landing page at `/`. */
export const Landing: FC<{ signedIn?: boolean }> = ({ signedIn }) => (
  <Layout
    title="Expert 3D and AR knowledge for every AI agent"
    description="SceneView MCP — expert 3D and AR knowledge for Claude, Cursor, and every AI coding agent. Hosted gateway with usage-based billing."
    active="home"
    signedIn={signedIn}
  >
    <section class="hero">
      <h1>Expert 3D and AR knowledge for AI agents</h1>
      <p class="lead">
        SceneView MCP ships the full SceneView 3D and AR SDK to Claude,
        Cursor, and every AI coding agent. Free to start, upgrade for
        specialized packages and hosted reliability.
      </p>
      <div class="hero-cta">
        <a href="/docs" class="btn btn-primary">
          Get started
        </a>
        <a href="/pricing" class="btn btn-secondary">
          View pricing
        </a>
      </div>
    </section>

    <section class="dash-grid">
      <div class="stat-card">
        <div class="label">Free tools</div>
        <div class="value">15</div>
        <p style="margin:.5rem 0 0;font-size:.875rem;">
          Samples, guides, validation — no key required.
        </p>
      </div>
      <div class="stat-card">
        <div class="label">Pro tools</div>
        <div class="value">36+</div>
        <p style="margin:.5rem 0 0;font-size:.875rem;">
          AR, multi-platform, scene generation, 3D artifacts.
        </p>
      </div>
      <div class="stat-card">
        <div class="label">Specialized packages</div>
        <div class="value">4</div>
        <p style="margin:.5rem 0 0;font-size:.875rem;">
          Automotive, Gaming, Healthcare, Interior.
        </p>
      </div>
    </section>

    <section>
      <h2>Works with every MCP-capable agent</h2>
      <p>
        Claude Desktop, Cursor, Zed, Continue, and any client that speaks
        the Streamable HTTP transport. Point them at the hosted endpoint
        and ship correct 3D code from the first prompt.
      </p>
      <pre><code>{`{
  "mcpServers": {
    "sceneview": {
      "url": "https://sceneview-mcp.workers.dev/mcp",
      "headers": { "Authorization": "Bearer sv_live_..." }
    }
  }
}`}</code></pre>
    </section>
  </Layout>
);

/** Top-level renderer used by the route handler. */
export function renderLanding(signedIn: boolean): Promise<string> {
  return renderToHtml(<Landing signedIn={signedIn} />);
}
