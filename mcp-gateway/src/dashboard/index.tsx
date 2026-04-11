/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import type { ApiKeyRow, UserRow } from "../db/schema.js";
import { Layout } from "./layout.js";
import { TierBadge } from "./components/tier-badge.js";
import { KeyRow, NewKeyAlert } from "./components/key-row.js";
import { UsageGraph, type UsageBucket } from "./components/usage-graph.js";
import { renderToHtml } from "./render.js";

/** Props for the dashboard index page. */
export interface DashboardProps {
  user: UserRow;
  keys: ApiKeyRow[];
  usage: UsageBucket[];
  newKey?: { plaintext: string; name: string } | null;
  monthlyQuota: { used: number; limit: number };
}

/** `/dashboard` — the signed-in landing page. */
export const Dashboard: FC<DashboardProps> = (props) => (
  <Layout
    title="Dashboard"
    description="Your SceneView MCP usage and API keys."
    active="dashboard"
    signedIn
  >
    <div
      style="display:flex;justify-content:space-between;align-items:baseline;flex-wrap:wrap;gap:1rem;"
    >
      <div>
        <h1 style="margin-bottom:.25rem;">Dashboard</h1>
        <p style="margin:0;">
          {props.user.email} <TierBadge tier={props.user.tier} />
        </p>
      </div>
      <div>
        <a href="/billing" class="btn btn-secondary">
          Billing
        </a>
        <form
          method="post"
          action="/auth/logout"
          style="display:inline;margin-left:.5rem;"
        >
          <button type="submit" class="btn btn-secondary">
            Sign out
          </button>
        </form>
      </div>
    </div>

    <section class="dash-grid" style="margin-top:2rem;">
      <div class="stat-card">
        <div class="label">Monthly usage</div>
        <div class="value">
          {props.monthlyQuota.used}
          <span style="font-size:1rem;color:var(--sv-fg-muted);font-weight:500;">
            {" "}
            / {props.monthlyQuota.limit}
          </span>
        </div>
      </div>
      <div class="stat-card">
        <div class="label">API keys</div>
        <div class="value">{props.keys.length}</div>
      </div>
      <div class="stat-card">
        <div class="label">Tier</div>
        <div class="value" style="font-size:1.25rem;">
          <TierBadge tier={props.user.tier} />
        </div>
      </div>
    </section>

    <section>
      <UsageGraph buckets={props.usage} />
    </section>

    <section>
      <div
        style="display:flex;justify-content:space-between;align-items:center;"
      >
        <h2>API keys</h2>
        <form
          hx-post="/dashboard/keys"
          hx-target="#keys-created"
          hx-swap="innerHTML"
          style="display:inline-flex;gap:.5rem;"
        >
          <input type="text" name="name" placeholder="Key name" required />
          <button type="submit" class="btn btn-primary">
            New key
          </button>
        </form>
      </div>

      <div id="keys-created">
        {props.newKey ? (
          <NewKeyAlert
            plaintext={props.newKey.plaintext}
            name={props.newKey.name}
          />
        ) : null}
      </div>

      {props.keys.length === 0 ? (
        <p>No keys yet. Create one to start using the hosted endpoint.</p>
      ) : (
        <table class="keys-table">
          <thead>
            <tr>
              <th>Prefix</th>
              <th>Name</th>
              <th>Created</th>
              <th>Last used</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {props.keys.map((k) => (
              <KeyRow row={k} />
            ))}
          </tbody>
        </table>
      )}
    </section>
  </Layout>
);

/** Top-level renderer used by the route handler. */
export function renderDashboard(props: DashboardProps): Promise<string> {
  return renderToHtml(<Dashboard {...props} />);
}
