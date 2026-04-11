#!/usr/bin/env bash
#
# bootstrap-d1.sh
#
# One-shot provisioning script for the SceneView MCP gateway on
# Cloudflare Workers. You run this ONCE per environment (production,
# staging, preview) to create the D1 database, the KV namespace, and
# set the Stripe secrets listed in wrangler.toml.
#
# The script does not execute anything by default: it prints the
# commands you need to run so you can review them before sending any
# request to Cloudflare or Stripe. Set EXECUTE=1 in the environment to
# actually run them.
#
# Architecture note — Stripe-first, stateless gateway:
# The gateway used to require magic-link auth (Resend + JWT sessions)
# and a user-facing dashboard, but both were stripped in 673ddd88 when
# we pivoted to a stateless Stripe-first flow. API keys are now
# provisioned via the Stripe webhook + a single-use KV handoff on
# /checkout/success, so there is no JWT_SECRET or RESEND_API_KEY to
# set any more. Only the Stripe secrets are required.
#
# Requires:
#   - wrangler >= 4 (npm i -g wrangler@latest)
#   - You must be logged in: `wrangler login`
#   - Tools: `openssl` only if you also want to rotate ancillary secrets
#
# Usage:
#   bash scripts/bootstrap-d1.sh
#   EXECUTE=1 bash scripts/bootstrap-d1.sh

set -euo pipefail

# Canonical production URL. The Worker is deployed here — see wrangler.toml
# for the corresponding DASHBOARD_BASE_URL. Update this constant if you
# ever add a custom domain (e.g. mcp.sceneview.dev).
GATEWAY_URL="https://sceneview-mcp.mcp-tools-lab.workers.dev"

run() {
  echo "\$ $*"
  if [ "${EXECUTE:-0}" = "1" ]; then
    eval "$@"
  fi
}

echo "==> 1. Create the D1 database (copy the id into wrangler.toml)"
run "wrangler d1 create sceneview-mcp"

echo
echo "==> 2. Create the KV namespace for auth cache + rate limiting"
run "wrangler kv namespace create RL_KV"

echo
echo "==> 3. Apply the D1 migrations"
run "npm run db:migrate"

echo
echo "==> 4. Set Stripe secrets (paste each value when prompted)"
echo
run "wrangler secret put STRIPE_SECRET_KEY"
run "wrangler secret put STRIPE_WEBHOOK_SECRET"

echo
echo "==> 5. Set Stripe price ids (edit wrangler.toml or use put)"
echo "Alternative: 'wrangler deploy' will read the [vars] section."
echo "  STRIPE_PRICE_PRO_MONTHLY"
echo "  STRIPE_PRICE_PRO_YEARLY"
echo "  STRIPE_PRICE_TEAM_MONTHLY"
echo "  STRIPE_PRICE_TEAM_YEARLY"

echo
echo "==> 6. Deploy"
run "wrangler deploy"

echo
echo "==> 7. Configure the Stripe webhook endpoint in the Stripe dashboard:"
echo "  URL:          ${GATEWAY_URL}/stripe/webhook"
echo "  Events:       checkout.session.completed"
echo "                customer.subscription.created"
echo "                customer.subscription.updated"
echo "                customer.subscription.deleted"
echo "                invoice.payment_failed"
echo "  Signing secret: re-run 'wrangler secret put STRIPE_WEBHOOK_SECRET'"
echo "                  with the value Stripe gives you."

echo
echo "Done. Smoke test with:"
echo "  curl ${GATEWAY_URL}/health"
