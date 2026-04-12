# Deploying sceneview-telemetry

Cloudflare Worker that ingests anonymous telemetry events from `sceneview-mcp`.

## Prerequisites

- Cloudflare account (same as mcp-gateway)
- `wrangler` CLI authenticated (`wrangler login`)

## Step 1 — Create D1 database

```bash
wrangler d1 create sceneview-telemetry
```

Copy the returned `database_id` into `wrangler.toml` (replace `TODO_REPLACE_AFTER_D1_CREATE`).

## Step 2 — Create KV namespace

```bash
wrangler kv namespace create RL_KV
```

Copy the returned `id` into `wrangler.toml` (replace `TODO_REPLACE_AFTER_KV_CREATE`).

## Step 3 — Run D1 migration

```bash
wrangler d1 migrations apply sceneview-telemetry
```

This creates the `events` table + indexes.

## Step 4 — Deploy

```bash
wrangler deploy
```

The worker will be live at `sceneview-telemetry.<account>.workers.dev`.

## Step 5 — DNS (custom domain)

In the Cloudflare DNS dashboard for `sceneview.io`:

1. Add a CNAME record: `telemetry` -> `sceneview-telemetry.<account>.workers.dev`
2. Uncomment the `[routes]` section in `wrangler.toml`
3. Redeploy: `wrangler deploy`

## Step 6 — Verify

```bash
# Health check
curl https://telemetry.sceneview.io/health

# Send a test event
curl -X POST https://telemetry.sceneview.io/v1/events \
  -H "Content-Type: application/json" \
  -d '{"timestamp":"2026-04-12T10:00:00Z","event":"init","client":"test","clientVersion":"1.0","mcpVersion":"4.0.0-rc.1","tier":"free"}'

# Check stats
curl https://telemetry.sceneview.io/v1/stats
```

## No secrets required

This worker has no secrets — no API keys, no Stripe, no JWT. Everything is anonymous.
The only bindings are D1 (storage) and KV (rate limiting).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/v1/events` | Single event ingestion |
| POST | `/v1/batch` | Batch ingestion (up to 50 events) |
| GET | `/v1/stats` | Last 24h aggregated stats |

## Rate limits

30 requests/minute per IP (hashed, never stored raw). KV-backed sliding window.

## After deployment

Update `TELEMETRY_ENDPOINT` in `mcp/src/telemetry.ts` if the URL differs from `https://telemetry.sceneview.io/v1/events`.
