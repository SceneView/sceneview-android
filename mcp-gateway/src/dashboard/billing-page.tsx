/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import type { SubscriptionRow, UserRow } from "../db/schema.js";
import { Layout } from "./layout.js";
import { TierBadge } from "./components/tier-badge.js";
import { renderToHtml } from "./render.js";

/** Props for {@link BillingPage}. */
export interface BillingPageProps {
  user: UserRow;
  subscription: SubscriptionRow | null;
}

/** `/billing` — subscription state and the Stripe portal button. */
export const BillingPage: FC<BillingPageProps> = ({ user, subscription }) => (
  <Layout title="Billing" active="billing" signedIn>
    <h1>Billing</h1>
    <p>
      Tier: <TierBadge tier={user.tier} />
    </p>
    {subscription ? (
      <section class="card">
        <h2 style="margin-top:0;">Current subscription</h2>
        <dl style="display:grid;grid-template-columns:max-content 1fr;gap:.5rem 1.5rem;margin:0;">
          <dt style="color:var(--sv-fg-muted);">Status</dt>
          <dd style="margin:0;">{subscription.status}</dd>
          <dt style="color:var(--sv-fg-muted);">Plan</dt>
          <dd style="margin:0;">{subscription.tier}</dd>
          <dt style="color:var(--sv-fg-muted);">Renews</dt>
          <dd style="margin:0;">
            {new Date(subscription.current_period_end).toISOString().slice(0, 10)}
          </dd>
          {subscription.cancel_at_period_end ? (
            <>
              <dt style="color:var(--sv-fg-muted);">Cancellation</dt>
              <dd style="margin:0;">Scheduled at period end</dd>
            </>
          ) : null}
        </dl>
        <form method="post" action="/billing/portal" style="margin-top:1.5rem;">
          <button type="submit" class="btn btn-primary">
            Open customer portal
          </button>
        </form>
      </section>
    ) : (
      <section class="card">
        <h2 style="margin-top:0;">Upgrade</h2>
        <p>
          You're on the Free tier. Upgrade to unlock Pro tools and the
          hosted HTTP endpoint with higher monthly quotas.
        </p>
        <div style="display:flex;gap:.75rem;flex-wrap:wrap;">
          <form method="post" action="/billing/checkout">
            <input type="hidden" name="plan" value="pro-monthly" />
            <button type="submit" class="btn btn-primary">
              Upgrade to Pro (19 EUR / month)
            </button>
          </form>
          <form method="post" action="/billing/checkout">
            <input type="hidden" name="plan" value="team-monthly" />
            <button type="submit" class="btn btn-secondary">
              Upgrade to Team (49 EUR / month)
            </button>
          </form>
        </div>
      </section>
    )}
  </Layout>
);

/** Top-level renderer used by the route handler. */
export function renderBillingPage(props: BillingPageProps): Promise<string> {
  return renderToHtml(<BillingPage {...props} />);
}
