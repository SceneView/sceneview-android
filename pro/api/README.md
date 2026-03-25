# SceneView MCP Pro API

Serverless API powering the SceneView Pro paid tier.

## Architecture

```
Client (API key) ──> Cloudflare Workers (gateway)
                         │
                         ├── Stripe (billing, usage-based credits)
                         ├── Upstash Redis (rate limiting, usage tracking)
                         └── MCP Server (proxied, premium features unlocked)
```

### Stack

| Component | Service | Purpose |
|---|---|---|
| API Gateway | Cloudflare Workers | Edge-deployed, low-latency routing |
| Billing | Stripe | Usage-based credits, API key management |
| Rate Limiting | Upstash Redis | Per-key rate limits, usage counters |
| Core Logic | SceneView MCP Server | Code generation, validation, optimization |

### Endpoints

| Method | Path | Tier | Description |
|---|---|---|---|
| POST | `/api/v1/generate` | Pro | Generate 3D scene code from natural language |
| POST | `/api/v1/validate` | Free (100/mo) / Pro (unlimited) | Validate SceneView code |
| POST | `/api/v1/optimize` | Pro | Optimize 3D assets (texture compression, LOD) |
| GET | `/api/v1/usage` | All | Current usage stats for the API key |

### Tiers

| Feature | Free | Pro |
|---|---|---|
| Code validation | 100 requests/month | Unlimited |
| Code generation | -- | Unlimited |
| Asset optimization | -- | Unlimited |
| Rate limit | 10 req/min | 60 req/min |

### Environment Variables

| Variable | Description |
|---|---|
| `STRIPE_SECRET_KEY` | Stripe API secret key |
| `UPSTASH_REDIS_URL` | Upstash Redis REST URL |
| `UPSTASH_REDIS_TOKEN` | Upstash Redis REST token |

## Development

```bash
cd pro/api
npm install
npm run dev      # local dev server (wrangler)
npm run deploy   # deploy to Cloudflare Workers
```

## Project Structure

```
pro/api/
├── src/
│   ├── index.ts        # Worker entry point, routing, middleware
│   ├── auth.ts         # API key validation via Stripe
│   └── rate-limit.ts   # Upstash Redis rate limiting
├── wrangler.toml       # Cloudflare Workers config
├── package.json
└── tsconfig.json
```
