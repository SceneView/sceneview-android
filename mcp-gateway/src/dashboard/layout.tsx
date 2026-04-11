/** @jsxImportSource hono/jsx */

/**
 * Shell HTML used by every server-rendered dashboard page.
 *
 * Design system (aligned with the repo's DESIGN.md): Material 3
 * Expressive, SceneView blue primary, CSS custom properties so both
 * light and dark themes swap via `prefers-color-scheme`. Every
 * value that might feel "branded" is a variable and not a hex literal.
 *
 * We intentionally inline the CSS in a `<style>` block instead of a
 * stylesheet asset: the whole site is served by a single Worker and
 * eliminating the round-trip for a CSS file lets the landing page
 * arrive in one request. Total page weight is tiny (< 8 KB gzipped).
 *
 * HTMX is loaded from unpkg with Subresource Integrity so future
 * releases cannot inject scripts. Inline scripts are minimised.
 */

import type { FC, PropsWithChildren } from "hono/jsx";

/** Props for {@link Layout}. */
export interface LayoutProps {
  /** Page title — prepended to the site name. */
  title: string;
  /** Meta description used for SEO and OG tags. */
  description?: string;
  /** Route the user is currently on — highlights the nav link. */
  active?: "home" | "pricing" | "docs";
}

export const Layout: FC<PropsWithChildren<LayoutProps>> = (props) => {
  const title = `${props.title} — SceneView MCP`;
  const description =
    props.description ??
    "SceneView MCP — expert 3D and AR knowledge for Claude, Cursor, and every AI coding agent.";
  return (
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>{title}</title>
        <meta name="description" content={description} />
        <meta property="og:title" content={title} />
        <meta property="og:description" content={description} />
        <meta property="og:type" content="website" />
        <meta
          property="og:image"
          content="https://sceneview.github.io/assets/og-image.png"
        />
        <meta
          property="og:site_name"
          content="SceneView MCP"
        />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={title} />
        <meta name="twitter:description" content={description} />
        <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'%3E%3Crect width='64' height='64' rx='12' fill='%231e40af'/%3E%3Ctext x='50%25' y='52%25' text-anchor='middle' fill='white' font-family='system-ui' font-size='28' font-weight='700'%3ESV%3C/text%3E%3C/svg%3E" />
        <script
          src="https://unpkg.com/htmx.org@1.9.12/dist/htmx.min.js"
          integrity="sha384-ujb1lZYygJmzgSwoxRggbCHcjc0rB2XoQrxeTUQyRjrOnlCoYta87iKBWq3EsdM2"
          crossorigin="anonymous"
        ></script>
        <style
          dangerouslySetInnerHTML={{
            __html: BASE_CSS,
          }}
        />
      </head>
      <body>
        <header class="site-header">
          <a href="/" class="brand" aria-label="SceneView MCP home">
            <span class="brand-mark">SV</span>
            <span class="brand-text">
              <strong>SceneView</strong>
              <span class="brand-suffix">MCP</span>
            </span>
          </a>
          <nav class="site-nav" aria-label="Primary">
            <NavLink href="/" active={props.active === "home"}>
              Home
            </NavLink>
            <NavLink href="/pricing" active={props.active === "pricing"}>
              Pricing
            </NavLink>
            <NavLink href="/docs" active={props.active === "docs"}>
              Docs
            </NavLink>
          </nav>
        </header>
        <main class="site-main">{props.children}</main>
        <footer class="site-footer">
          <div class="footer-inner">
            <p>SceneView MCP &middot; Expert 3D and AR knowledge for AI.</p>
            <p class="footer-links">
              <a href="/pricing">Pricing</a>
              <a href="/docs">Docs</a>
              <a href="https://github.com/sceneview/sceneview">GitHub</a>
            </p>
          </div>
        </footer>
      </body>
    </html>
  );
};

/** Navigation link with the active state baked in. */
const NavLink: FC<PropsWithChildren<{ href: string; active?: boolean }>> = (
  props,
) => (
  <a
    href={props.href}
    class={`nav-link${props.active ? " nav-link--active" : ""}`}
    aria-current={props.active ? "page" : undefined}
  >
    {props.children}
  </a>
);

/**
 * All CSS in one string so the JSX layer stays dependency free.
 *
 * Keep comments inline — this is the source of truth for the design
 * system used by every dashboard page.
 */
const BASE_CSS = `
:root {
  /* SceneView palette — primary blue matches the repo branding. */
  --sv-primary: #1e40af;
  --sv-primary-hover: #1e3a8a;
  --sv-primary-fg: #ffffff;
  --sv-accent: #38bdf8;

  /* Material 3 Expressive-inspired surface layering. */
  --sv-bg: #f8fafc;
  --sv-surface: #ffffff;
  --sv-surface-alt: #f1f5f9;
  --sv-border: #e2e8f0;
  --sv-fg: #0f172a;
  --sv-fg-muted: #475569;

  /* Radius / spacing / shadows match DESIGN.md. */
  --sv-radius-sm: 6px;
  --sv-radius: 12px;
  --sv-radius-lg: 20px;
  --sv-shadow-sm: 0 1px 2px rgba(15,23,42,.05);
  --sv-shadow: 0 6px 18px rgba(15,23,42,.08);
  --sv-shadow-lg: 0 20px 60px rgba(15,23,42,.14);

  --sv-font: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --sv-font-mono: ui-monospace, "JetBrains Mono", Menlo, monospace;
}

@media (prefers-color-scheme: dark) {
  :root {
    --sv-bg: #020617;
    --sv-surface: #0f172a;
    --sv-surface-alt: #1e293b;
    --sv-border: #334155;
    --sv-fg: #f8fafc;
    --sv-fg-muted: #94a3b8;
    --sv-primary: #3b82f6;
    --sv-primary-hover: #60a5fa;
  }
}

* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body {
  font-family: var(--sv-font);
  background: var(--sv-bg);
  color: var(--sv-fg);
  line-height: 1.55;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
a { color: var(--sv-primary); text-decoration: none; }
a:hover { text-decoration: underline; }

.site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 2rem;
  background: var(--sv-surface);
  border-bottom: 1px solid var(--sv-border);
  position: sticky;
  top: 0;
  z-index: 10;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: .75rem;
  color: var(--sv-fg);
  font-weight: 700;
}
.brand:hover { text-decoration: none; }
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--sv-radius-sm);
  background: var(--sv-primary);
  color: var(--sv-primary-fg);
  font-weight: 700;
  font-size: 0.875rem;
  letter-spacing: 0.5px;
}
.brand-text { display: inline-flex; flex-direction: column; line-height: 1.1; }
.brand-suffix { color: var(--sv-fg-muted); font-size: 0.75rem; font-weight: 500; text-transform: uppercase; letter-spacing: 0.08em; }

.site-nav {
  display: flex;
  gap: .25rem;
  align-items: center;
}
.nav-link {
  padding: .5rem .875rem;
  border-radius: var(--sv-radius-sm);
  color: var(--sv-fg-muted);
  font-weight: 500;
  font-size: 0.9375rem;
}
.nav-link:hover {
  background: var(--sv-surface-alt);
  color: var(--sv-fg);
  text-decoration: none;
}
.nav-link--active {
  background: var(--sv-surface-alt);
  color: var(--sv-fg);
}

.site-main {
  flex: 1 1 auto;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 2.5rem 2rem 4rem 2rem;
}

.site-footer {
  border-top: 1px solid var(--sv-border);
  background: var(--sv-surface);
  padding: 2rem;
}
.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  color: var(--sv-fg-muted);
  font-size: 0.875rem;
}
.footer-links a { margin-left: 1rem; color: var(--sv-fg-muted); }
.footer-links a:first-child { margin-left: 0; }

/* ── Typography ────────────────────────────────────────────────────────── */
h1, h2, h3 {
  color: var(--sv-fg);
  line-height: 1.2;
  margin: 0 0 .75rem 0;
}
h1 { font-size: clamp(2rem, 4vw, 3rem); letter-spacing: -0.02em; }
h2 { font-size: 1.75rem; margin-top: 2.5rem; }
h3 { font-size: 1.25rem; margin-top: 2rem; }
p { color: var(--sv-fg-muted); margin: 0 0 1rem 0; }

/* ── Buttons ───────────────────────────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: .5rem;
  padding: .75rem 1.25rem;
  border-radius: var(--sv-radius);
  border: 1px solid transparent;
  font-family: inherit;
  font-weight: 600;
  font-size: .9375rem;
  cursor: pointer;
  transition: transform .05s ease, background .15s ease;
  text-decoration: none;
}
.btn:hover { text-decoration: none; }
.btn:active { transform: scale(.98); }
.btn-primary {
  background: var(--sv-primary);
  color: var(--sv-primary-fg);
}
.btn-primary:hover { background: var(--sv-primary-hover); color: var(--sv-primary-fg); }
.btn-secondary {
  background: var(--sv-surface);
  color: var(--sv-fg);
  border-color: var(--sv-border);
}
.btn-secondary:hover { background: var(--sv-surface-alt); }

/* ── Cards ─────────────────────────────────────────────────────────────── */
.card {
  background: var(--sv-surface);
  border: 1px solid var(--sv-border);
  border-radius: var(--sv-radius-lg);
  padding: 2rem;
  box-shadow: var(--sv-shadow-sm);
}

/* ── Hero ──────────────────────────────────────────────────────────────── */
.hero {
  text-align: center;
  padding: 3rem 1rem 4rem;
}
.hero p.lead {
  font-size: 1.25rem;
  max-width: 680px;
  margin: 1.5rem auto 2.5rem;
}
.hero-cta { display: inline-flex; gap: .75rem; flex-wrap: wrap; justify-content: center; }

/* ── Pricing table ─────────────────────────────────────────────────────── */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}
.pricing-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: var(--sv-surface);
  border: 1px solid var(--sv-border);
  border-radius: var(--sv-radius-lg);
  padding: 2rem;
  box-shadow: var(--sv-shadow-sm);
}
.pricing-card--featured {
  border-color: var(--sv-primary);
  box-shadow: var(--sv-shadow);
}
.pricing-card h3 { margin-top: 0; }
.pricing-card .price {
  font-size: 2.25rem;
  font-weight: 700;
  color: var(--sv-fg);
}
.pricing-card .price small {
  font-size: 0.875rem;
  color: var(--sv-fg-muted);
  font-weight: 500;
}
.pricing-card ul { list-style: none; padding: 0; margin: 0; flex: 1 1 auto; }
.pricing-card li { padding: .4rem 0; color: var(--sv-fg-muted); }
.pricing-card li::before { content: "\\2713\\00a0"; color: var(--sv-primary); font-weight: 700; }

/* ── Code blocks ───────────────────────────────────────────────────────── */
pre, code {
  font-family: var(--sv-font-mono);
  font-size: .875rem;
}
pre {
  background: var(--sv-surface-alt);
  border: 1px solid var(--sv-border);
  border-radius: var(--sv-radius);
  padding: 1rem 1.25rem;
  overflow-x: auto;
}
code { background: var(--sv-surface-alt); padding: .125rem .375rem; border-radius: 4px; }
pre code { background: transparent; padding: 0; }

/* ── Forms ─────────────────────────────────────────────────────────────── */
form { display: flex; flex-direction: column; gap: .75rem; }
label { font-weight: 600; color: var(--sv-fg); font-size: .875rem; }
input[type="email"], input[type="text"] {
  padding: .75rem 1rem;
  border-radius: var(--sv-radius);
  border: 1px solid var(--sv-border);
  background: var(--sv-surface);
  color: var(--sv-fg);
  font-family: inherit;
  font-size: 1rem;
}
input[type="email"]:focus, input[type="text"]:focus {
  outline: 2px solid var(--sv-primary);
  outline-offset: 2px;
}

.form-card {
  max-width: 420px;
  margin: 3rem auto;
  background: var(--sv-surface);
  border: 1px solid var(--sv-border);
  border-radius: var(--sv-radius-lg);
  padding: 2.5rem;
  box-shadow: var(--sv-shadow);
}

/* ── Dashboard ─────────────────────────────────────────────────────────── */
.dash-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}
.stat-card {
  background: var(--sv-surface);
  border: 1px solid var(--sv-border);
  border-radius: var(--sv-radius);
  padding: 1.25rem 1.5rem;
}
.stat-card .label {
  color: var(--sv-fg-muted);
  font-size: .75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.stat-card .value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--sv-fg);
  margin-top: .25rem;
}
.tier-badge {
  display: inline-flex;
  align-items: center;
  padding: .125rem .625rem;
  border-radius: 999px;
  font-size: .75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.tier-badge--free { background: rgba(148,163,184,.15); color: var(--sv-fg-muted); }
.tier-badge--pro { background: rgba(30,64,175,.12); color: var(--sv-primary); }
.tier-badge--team { background: rgba(14,165,233,.14); color: var(--sv-accent); }

.keys-table {
  width: 100%;
  border-collapse: collapse;
}
.keys-table th, .keys-table td {
  text-align: left;
  padding: .75rem 1rem;
  border-bottom: 1px solid var(--sv-border);
  font-size: .875rem;
}
.keys-table th {
  color: var(--sv-fg-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-size: .6875rem;
}

.alert {
  padding: 1rem 1.25rem;
  border-radius: var(--sv-radius);
  background: rgba(30,64,175,.08);
  border: 1px solid rgba(30,64,175,.25);
  color: var(--sv-fg);
  margin-bottom: 1.5rem;
}
.alert--success {
  background: rgba(34,197,94,.1);
  border-color: rgba(34,197,94,.3);
}

.usage-graph {
  width: 100%;
  height: auto;
  display: block;
}

/* ── Responsive ────────────────────────────────────────────────────────── */
@media (max-width: 600px) {
  .site-header { padding: 1rem; }
  .site-main { padding: 1.5rem 1rem 3rem; }
  .site-nav { gap: .125rem; }
  .nav-link { padding: .5rem .625rem; font-size: .8125rem; }
}
`;
