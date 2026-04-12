#!/usr/bin/env bash
# dev-setup.sh — Bootstrap local development for sceneview-telemetry
#
# Usage: bash scripts/dev-setup.sh
#
# Applies D1 migrations against local SQLite, sends a test event, and
# prints the stats URL. Run once before `wrangler dev`.

set -euo pipefail

BASE_URL="http://localhost:8787"

echo "==> Applying D1 migrations (local SQLite)..."
npx wrangler d1 migrations apply sceneview-telemetry --local

echo ""
echo "==> Starting wrangler dev in the background..."
npx wrangler dev --port 8787 &
WRANGLER_PID=$!
echo "    wrangler dev PID: $WRANGLER_PID"

# Give the worker a moment to start
sleep 3

echo ""
echo "==> Sending test event..."
curl -s -X POST "$BASE_URL/v1/events" \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "'"$(date -u +%Y-%m-%dT%H:%M:%SZ)"'",
    "event": "init",
    "client": "test",
    "clientVersion": "1.0",
    "mcpVersion": "4.0.0-rc.1",
    "tier": "free"
  }' | (command -v jq &>/dev/null && jq . || cat)

echo ""
echo "==> Stats endpoint:"
echo "    $BASE_URL/v1/stats"
curl -s "$BASE_URL/v1/stats" | (command -v jq &>/dev/null && jq . || cat)

echo ""
echo "==> Worker running at $BASE_URL (PID $WRANGLER_PID)"
echo "    Press Ctrl+C to stop, or run: kill $WRANGLER_PID"
