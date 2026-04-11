#!/usr/bin/env bash
# mcp-gateway-golive.sh — flip the MCP gateway from Stripe TEST to LIVE.
#
# Prerequisites (MUST be done manually, outside this script):
#   1. Stripe KYC complete (France — auto-entrepreneur OR SASU decision).
#      See profile-private/preferences/api-keys.md "Stripe — SceneView MCP"
#      section for the fiscal decision context.
#   2. In Stripe Dashboard (LIVE mode), create 4 products:
#        - Pro Monthly   19 EUR/mo
#        - Pro Yearly   190 EUR/yr
#        - Team Monthly  49 EUR/mo
#        - Team Yearly  490 EUR/yr
#      Copy the 4 `price_live_...` ids.
#   3. In Stripe Dashboard, create a webhook endpoint:
#        URL:    https://sceneview-mcp.mcp-tools-lab.workers.dev/stripe/webhook
#        Events: checkout.session.completed
#                customer.subscription.created
#                customer.subscription.updated
#                customer.subscription.deleted
#                invoice.payment_failed
#      Copy the `whsec_...` signing secret.
#   4. Have the Stripe live secret key (`sk_live_...`) ready.
#
# Usage:
#   bash .claude/scripts/mcp-gateway-golive.sh \
#     --pro-monthly price_live_... \
#     --pro-yearly  price_live_... \
#     --team-monthly price_live_... \
#     --team-yearly price_live_...
#
# The script then prompts interactively for STRIPE_SECRET_KEY and
# STRIPE_WEBHOOK_SECRET (so they never end up in shell history or
# command-line args visible in `ps`).
#
# What it does:
#   - Updates mcp-gateway/wrangler.toml with the 4 live price ids.
#   - Runs `wrangler secret put STRIPE_SECRET_KEY`.
#   - Runs `wrangler secret put STRIPE_WEBHOOK_SECRET`.
#   - Runs `npm test` on the gateway (168 tests).
#   - Deploys the worker via `wrangler deploy`.
#   - Smoke-tests /health and /billing/checkout (redirect to Stripe).
#   - Prints the final URL for the user to share.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GATEWAY_DIR="$REPO_ROOT/mcp-gateway"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PRO_MONTHLY=""
PRO_YEARLY=""
TEAM_MONTHLY=""
TEAM_YEARLY=""

while [ $# -gt 0 ]; do
  case "$1" in
    --pro-monthly)  PRO_MONTHLY="$2";  shift 2 ;;
    --pro-yearly)   PRO_YEARLY="$2";   shift 2 ;;
    --team-monthly) TEAM_MONTHLY="$2"; shift 2 ;;
    --team-yearly)  TEAM_YEARLY="$2";  shift 2 ;;
    -h|--help)
      sed -n '2,40p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown flag: $1${NC}" >&2
      exit 2
      ;;
  esac
done

for var in PRO_MONTHLY PRO_YEARLY TEAM_MONTHLY TEAM_YEARLY; do
  if [ -z "${!var}" ]; then
    echo -e "${RED}Missing --${var,,//_/-}${NC}" >&2
    echo "Run with --help for usage." >&2
    exit 2
  fi
  if [[ "${!var}" != price_live_* ]]; then
    echo -e "${RED}$var must start with 'price_live_' (got: ${!var})${NC}" >&2
    echo -e "${RED}Refusing to flip to LIVE mode with a test price id.${NC}" >&2
    exit 2
  fi
done

echo -e "${YELLOW}── mcp-gateway go-live ──${NC}"
echo "  Pro Monthly  : $PRO_MONTHLY"
echo "  Pro Yearly   : $PRO_YEARLY"
echo "  Team Monthly : $TEAM_MONTHLY"
echo "  Team Yearly  : $TEAM_YEARLY"
echo

# 1. Patch wrangler.toml ────────────────────────────────────────────────────
WRANGLER_TOML="$GATEWAY_DIR/wrangler.toml"
if [ ! -f "$WRANGLER_TOML" ]; then
  echo -e "${RED}Not found: $WRANGLER_TOML${NC}" >&2
  exit 1
fi

echo -e "${YELLOW}[1/5] Patching wrangler.toml price ids...${NC}"
python3 - <<PY
import re, pathlib
p = pathlib.Path("$WRANGLER_TOML")
txt = p.read_text()
for key, val in [
    ("STRIPE_PRICE_PRO_MONTHLY",  "$PRO_MONTHLY"),
    ("STRIPE_PRICE_PRO_YEARLY",   "$PRO_YEARLY"),
    ("STRIPE_PRICE_TEAM_MONTHLY", "$TEAM_MONTHLY"),
    ("STRIPE_PRICE_TEAM_YEARLY",  "$TEAM_YEARLY"),
]:
    pattern = rf'^{key}\s*=\s*"[^"]*"'
    new = f'{key} = "{val}"'
    if not re.search(pattern, txt, flags=re.M):
        raise SystemExit(f"Could not find {key} in wrangler.toml")
    txt = re.sub(pattern, new, txt, count=1, flags=re.M)
p.write_text(txt)
print("  ✓ wrangler.toml updated")
PY

# 2. Secrets (interactive, via wrangler secret put) ────────────────────────
echo
echo -e "${YELLOW}[2/5] Setting STRIPE_SECRET_KEY (paste sk_live_... when prompted, then Ctrl-D)${NC}"
cd "$GATEWAY_DIR"
wrangler secret put STRIPE_SECRET_KEY

echo
echo -e "${YELLOW}[3/5] Setting STRIPE_WEBHOOK_SECRET (paste whsec_... when prompted, then Ctrl-D)${NC}"
wrangler secret put STRIPE_WEBHOOK_SECRET

# 3. Tests ─────────────────────────────────────────────────────────────────
echo
echo -e "${YELLOW}[4/5] Running mcp-gateway tests...${NC}"
if ! npm test --silent 2>&1 | tail -5; then
  echo -e "${RED}Tests failed — aborting before deploy.${NC}" >&2
  exit 1
fi

# 4. Deploy ────────────────────────────────────────────────────────────────
echo
echo -e "${YELLOW}[5/5] Deploying worker...${NC}"
wrangler deploy

# 5. Smoke tests ───────────────────────────────────────────────────────────
echo
echo -e "${YELLOW}── Smoke tests ──${NC}"
URL="https://sceneview-mcp.mcp-tools-lab.workers.dev"

HEALTH=$(curl -s "$URL/health")
if echo "$HEALTH" | grep -q '"ok":true'; then
  echo -e "${GREEN}  ✓ /health OK${NC}"
else
  echo -e "${RED}  ✗ /health FAILED: $HEALTH${NC}"
  exit 1
fi

CHECKOUT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL/billing/checkout" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "plan=pro-monthly")
if [ "$CHECKOUT_CODE" = "303" ]; then
  echo -e "${GREEN}  ✓ /billing/checkout returns 303 (Stripe redirect)${NC}"
else
  echo -e "${RED}  ✗ /billing/checkout returned $CHECKOUT_CODE (expected 303)${NC}"
  exit 1
fi

echo
echo -e "${GREEN}── LIVE ──${NC}"
echo "  Pricing:        $URL/pricing"
echo "  Gateway /mcp:   $URL/mcp (Bearer sv_live_...)"
echo "  Webhook:        $URL/stripe/webhook"
echo
echo "  Next: share $URL/pricing (or a CTA on the website) and wait"
echo "        for the first checkout.session.completed event."
