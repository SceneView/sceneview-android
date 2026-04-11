/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import { Layout } from "./layout.js";
import { renderToHtml } from "./render.js";

/** Single tier card shown in the pricing grid. */
interface TierCardProps {
  name: string;
  price: string;
  period: string;
  description: string;
  features: readonly string[];
  cta: { href: string; label: string };
  featured?: boolean;
}

const TierCard: FC<TierCardProps> = (props) => (
  <div
    class={`pricing-card${props.featured ? " pricing-card--featured" : ""}`}
  >
    <h3>{props.name}</h3>
    <div class="price">
      {props.price}
      <small> / {props.period}</small>
    </div>
    <p>{props.description}</p>
    <ul>
      {props.features.map((f) => (
        <li>{f}</li>
      ))}
    </ul>
    <a
      href={props.cta.href}
      class={`btn ${props.featured ? "btn-primary" : "btn-secondary"}`}
    >
      {props.cta.label}
    </a>
  </div>
);

/** `/pricing` page. */
export const Pricing: FC<{ signedIn?: boolean }> = ({ signedIn }) => (
  <Layout
    title="Pricing"
    description="Simple, transparent pricing for SceneView MCP. Free to start. Upgrade to Pro at 19 EUR/month or Team at 49 EUR/month when you need specialized packages and higher quotas."
    active="pricing"
    signedIn={signedIn}
  >
    <section style="text-align:center;">
      <h1>Pricing</h1>
      <p class="lead" style="max-width:640px;margin:1rem auto 0;">
        Start free. Upgrade when you need specialized packages, higher
        throughput, or organisation billing.
      </p>
    </section>

    <section class="pricing-grid">
      <TierCard
        name="Free"
        price="0 EUR"
        period="forever"
        description="Everything you need to learn SceneView."
        features={[
          "15 free tools",
          "SceneView API reference",
          "Known issues resource",
          "Community support",
          "Self-hosted stdio npm package",
        ]}
        cta={{ href: "/docs", label: "Read the docs" }}
      />
      <TierCard
        name="Pro"
        price="19 EUR"
        period="month"
        description="For developers shipping real 3D and AR apps."
        features={[
          "Everything in Free",
          "36+ Pro tools",
          "Scene generation and 3D artifacts",
          "AR and multi-platform guides",
          "Hosted HTTP endpoint, 50k calls / month",
          "Email support",
        ]}
        cta={{ href: "/dashboard", label: "Upgrade" }}
        featured
      />
      <TierCard
        name="Team"
        price="49 EUR"
        period="month"
        description="For organisations with multiple developers."
        features={[
          "Everything in Pro",
          "Automotive, Gaming, Healthcare, Interior packages",
          "250k calls / month",
          "Per-seat API keys",
          "Priority support",
          "Custom invoicing",
        ]}
        cta={{ href: "/dashboard", label: "Upgrade" }}
      />
    </section>

    <section style="margin-top:3rem;">
      <h2>Annual plans</h2>
      <p>
        Save roughly two months on every tier by switching to annual
        billing from the Stripe customer portal: Pro at 190 EUR / year
        and Team at 490 EUR / year.
      </p>
    </section>

    <section style="margin-top:2.5rem;">
      <h2>Frequently asked</h2>
      <h3>What happens when I hit my monthly quota?</h3>
      <p>
        Calls beyond the quota return a JSON-RPC rate_limited error
        without breaking the connection. Upgrade in the dashboard or
        wait until the next billing cycle; no auto-overage charges.
      </p>
      <h3>Can I self-host?</h3>
      <p>
        Yes. The npm package keeps working as a stdio server. Set
        <code>SCENEVIEW_API_KEY</code> to unlock Pro tools via the
        hosted proxy, or run fully local on the Free tier.
      </p>
      <h3>Do you store my prompts?</h3>
      <p>
        No. The gateway logs per-call metadata (tool name, status,
        timestamp) for billing and rate limiting. Request bodies and
        responses are never persisted.
      </p>
      <h3>What about taxes?</h3>
      <p>
        EU VAT is handled automatically by Stripe Tax based on your
        billing address. Invoices are available in the customer portal.
      </p>
    </section>
  </Layout>
);

/** Top-level renderer used by the route handler. */
export function renderPricing(signedIn: boolean): Promise<string> {
  return renderToHtml(<Pricing signedIn={signedIn} />);
}
