#!/usr/bin/env bash
#
# bootstrap-d1.sh
#
# One-shot provisioning script for the SceneView MCP gateway on
# Cloudflare Workers. You run this ONCE per environment (production,
# staging, preview) to create the D1 database, the KV namespace, and
# set every secret listed in wrangler.toml.
#
# The script does not execute anything by default: it prints the
# commands you need to run so you can review them before sending any
# request to Cloudflare or Stripe. Set EXECUTE=1 in the environment to
# actually run them.
#
# Requires:
#   - wrangler >= 4 (npm i -g wrangler@latest)
#   - You must be logged in: `wrangler login`
#   - Tools: `uuidgen` (macOS/Linux default), `openssl` (for secrets)
#
# Usage:
#   bash scripts/bootstrap-d1.sh
#   EXECUTE=1 bash scripts/bootstrap-d1.sh

set -euo pipefail

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
echo "==> 4. Set secrets (paste each value when prompted)"
echo
echo "Generate a strong JWT secret locally first:"
echo "  openssl rand -hex 32"
echo
run "wrangler secret put JWT_SECRET"
run "wrangler secret put RESEND_API_KEY"
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
echo "  URL:          https://sceneview-mcp.workers.dev/stripe/webhook"
echo "  Events:       checkout.session.completed"
echo "                customer.subscription.created"
echo "                customer.subscription.updated"
echo "                customer.subscription.deleted"
echo "                invoice.payment_failed"
echo "  Signing secret: re-run 'wrangler secret put STRIPE_WEBHOOK_SECRET'"
echo "                  with the value Stripe gives you."

echo
echo "Done. Smoke test with:"
echo "  curl https://sceneview-mcp.workers.dev/health"
