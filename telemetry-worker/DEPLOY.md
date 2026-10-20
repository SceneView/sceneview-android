# Deploying sceneview-telemetry

Cloudflare Worker that ingests anonymous telemetry events from `sceneview-mcp`.

## Local development

No Cloudflare account or real IDs needed. `wrangler dev` automatically uses local
SQLite for D1 and an in-memory store for KV — the `TODO_REPLACE_*` placeholders in
`wrangler.toml` are ignored in local mode.

### Quick start

```bash
# One-shot: apply migrations + start worker + send test event
bash scripts/dev-setup.sh
```

### Manual steps (if you prefer)

```bash
# 1. Apply migrations to local SQLite
npx wrangler d1 migrations apply sceneview-telemetry --local

# 2. Start the dev server (port 8787)
npx wrangler dev

# 3. Send a test event
curl -X POST http://localhost:8787/v1/events \
  -H "Content-Type: application/json" \
  -d '{"timestamp":"2026-04-12T10:00:00Z","event":"init","client":"test","clientVersion":"1.0","mcpVersion":"4.0.0-rc.1","tier":"free"}'

# 4. Check stats (needs STATS_TOKEN in .dev.vars, see below)
curl -H "Authorization: Bearer dev-token" http://localhost:8787/v1/stats
```

The `[dev]` section in `wrangler.toml` pins the local server to port 8787 over HTTP.
The read routes fail closed, locally too: without `STATS_TOKEN` they answer `503`.
For local work, put a throwaway value in `.dev.vars` (gitignored), which `wrangler dev`
loads automatically:

```bash
echo 'STATS_TOKEN=dev-token' > .dev.vars
```

---

## Prerequisites (production)

- Cloudflare account
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

Set `STATS_TOKEN` first (see [Required secret — STATS_TOKEN](#required-secret--stats_token)):
without it the read routes answer `503`. Ingestion works either way.

```bash
wrangler secret put STATS_TOKEN
wrangler deploy
```

The worker will be live at `sceneview-telemetry.<account>.workers.dev`.

## Step 4b — Enable cron trigger

Edit `wrangler.toml` to uncomment the `[triggers]` section:

```toml
[triggers]
crons = ["0 2 * * *"]
```

Then redeploy:

```bash
wrangler deploy
```

This enables the daily rollup at 02:00 UTC that aggregates events into `daily_rollups` and purges raw events older than 90 days. Without this, raw events accumulate indefinitely.

## Step 5 — Verify

```bash
# Health check
curl https://sceneview-telemetry.mcp-tools-lab.workers.dev/health

# Send a test event
curl -X POST https://sceneview-telemetry.mcp-tools-lab.workers.dev/v1/events \
  -H "Content-Type: application/json" \
  -d '{"timestamp":"2026-04-12T10:00:00Z","event":"init","client":"test","clientVersion":"1.0","mcpVersion":"4.0.0-rc.1","tier":"free"}'

# Check stats
curl -H "Authorization: Bearer $STATS_TOKEN" \
  https://sceneview-telemetry.mcp-tools-lab.workers.dev/v1/stats
```

## Required secret — STATS_TOKEN

Every read route — `/v1/stats`, `/v1/timeseries`, `/v1/top-clients`, `/v1/funnel` and
`/v1/export` — requires a bearer token:

```bash
wrangler secret put STATS_TOKEN
```

Requests must include the header:

```
Authorization: Bearer <your-token>
```

- `STATS_TOKEN` not configured → `503 {"error":"stats_token_not_configured"}`. The read
  routes fail closed: a missing secret never means open access.
- Missing or wrong token → `401 {"error":"unauthorized"}` with `WWW-Authenticate: Bearer`.
  The token is compared in constant time.

The read routes send no CORS headers, so a web page on another origin cannot read them
from a visitor's browser. Only the ingestion routes (`/v1/events`, `/v1/batch`) and
`/health` answer with `Access-Control-Allow-Origin: *`. Ingestion never needs the token.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/v1/events` | Single event ingestion |
| POST | `/v1/batch` | Batch ingestion (up to 50 events) |
| GET | `/v1/stats` | Aggregated stats (bearer token) |
| GET | `/v1/timeseries` | Daily series (bearer token) |
| GET | `/v1/top-clients` | Most active clients (bearer token) |
| GET | `/v1/funnel` | Usage funnel (bearer token) |
| GET | `/v1/export` | Raw event CSV (bearer token) |

## Rate limits

30 requests/minute per IP (hashed, never stored raw). KV-backed sliding window.

## After deployment

The `TELEMETRY_ENDPOINT` in `mcp/src/telemetry.ts` is already set to `https://sceneview-telemetry.mcp-tools-lab.workers.dev/v1/events`.

## Dashboard

A standalone HTML dashboard (`dashboard.html`) provides a visual stats overview.

**To use:**
1. Open `dashboard.html` in your browser (file:// protocol works locally)
2. On first load, the dashboard prompts for:
   - **API base URL** (e.g., `https://sceneview-telemetry.mcp-tools-lab.workers.dev`)
   - **Bearer token** (your `STATS_TOKEN` value, if set)
3. Credentials are stored in the browser's localStorage and persisted between sessions

> The read routes no longer send CORS headers, so a dashboard opened from `file://` or
> another origin cannot call them directly from the browser any more. Fetch them
> server-side (or through a same-origin proxy) with the bearer token instead.
4. The dashboard auto-refreshes every 60 seconds and works offline once loaded

**Features:**
- 24-hour event totals (inits vs tool calls vs unique clients)
- Top 20 tools by call count
- MCP versions in use
- Light/dark mode toggle
- Local-first design (no analytics on the dashboard itself)
