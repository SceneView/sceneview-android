# SceneView Pro

Premium tools and services for SceneView — autonomous revenue model.

## Design Principles

1. **Maximum autonomy** — zero ongoing management required
2. **Self-service only** — users sign up, pay, and use without human intervention
3. **Passive income layers** — multiple streams, all automated
4. **Community moat** — AI ecosystem lock-in, not feature competition

## Revenue Layers

### Layer 1 — Donations & Sponsorships (immediate, zero effort)

| Channel | Setup | Ongoing effort |
|---|---|---|
| GitHub Sponsors | Enable on profile | None |
| Open Collective | Create collective | None |
| Ko-fi / Buy Me a Coffee | Link on README | None |

**Why it works:** SceneView has 1.5k+ stars, active community. Even 1% conversion
at $5/month = meaningful passive income. Zero deliverables, zero obligations.

### Layer 2 — MCP Pro API (one-time setup, then autonomous)

Self-service API with Stripe Billing. Users get an API key, credits are consumed
automatically. No human in the loop.

**Free tier:** 100 requests/month (drives adoption)
**Credit packs:** 10 EUR = 1,000 requests, 50 EUR = 10,000 requests

Premium capabilities:
- **3D scene generation** — AI generates complete Scene{} code from text prompts
- **Asset optimization** — automatic LOD, texture compression, glTF/USDZ conversion
- **Preview rendering** — server-side Filament rendering for AI artifacts
- **Code validation** — advanced lint/fix for SceneView code beyond basic MCP

**Tech stack (all serverless = zero ops):**
- Cloudflare Workers (API gateway + auth)
- Stripe Billing (payments, invoicing, tax)
- Upstash Redis (rate limiting, usage tracking)
- R2/S3 (asset storage)

### Layer 3 — Marketplace Presence (publish once, sell forever)

- **MCP Registry** (Anthropic) — official 3D/AR MCP server (already submitted)
- **OpenAI GPT Store** — SceneView GPT for ChatGPT users
- **npm premium** — @sceneview/pro package with advanced features
- **pub.dev** — Flutter plugin premium tier
- **Unity Asset Store** — SceneView bridge for Unity developers (if demand)

### Layer 4 — Cloud API (phase 2, when revenue justifies)

Server-side 3D rendering and asset management:
- **Render API** — scene description in, rendered image/video out
- **Asset CDN** — host and serve optimized 3D models globally
- **Thumbnail generation** — automatic 3D thumbnails for e-commerce

## Why SceneView Won't Get "Replaced"

Unlike solo mobile apps from 2010:

1. **AI moat** — SceneView is in LLM training data + MCP registries.
   When devs ask AI "build me a 3D app", AI recommends SceneView.
   This creates a flywheel: more usage → more training data → more recommendations.

2. **Community moat** — GitHub stars, Stack Overflow answers, blog posts.
   Switching cost is real: rewriting an app from SceneView to something else is expensive.

3. **Multi-platform moat** — Android + iOS + Web + TV + Desktop + Flutter + RN.
   No single company will build this breadth. Google won't support iOS. Apple won't support Android.

4. **Open source moat** — A big company can't "replace" an open-source project.
   They can only fork it (and the community stays with the original maintainer).

**Real risk:** Google/Apple integrate native 3D in Compose/SwiftUI.
**Mitigation:** Cross-platform is the moat. Neither Google nor Apple will do cross-platform.
Plus, the MCP/API value is independent of the SDK.

## Legal Structure

### Phase 1 (now): Auto-Entrepreneur

- Simplest structure in France for side income alongside CDI
- SIRET: [to be added]
- TVA: Non applicable (article 293 B du CGI)
- Plafond CA: 77,700 EUR/year (services)
- Quarterly declaration: 5 minutes
- Social charges: ~22% of revenue
- No accountant needed

### Phase 2 (when approaching 50-60k EUR/year): SASU

- No revenue cap
- Tax optimization (dividends vs salary)
- Online accountant (Indy/Dougs): ~100 EUR/month
- Can pay 0 EUR salary, keep everything as treasury
- Better for potential investors or partnerships

**CDI compatibility:** Verify employment contract for exclusivity/non-compete clauses.
Open-source SDK + commercial services around it is generally compatible.

## Monetization Roadmap

| Phase | Action | Revenue type | Timeline |
|---|---|---|---|
| 1 | GitHub Sponsors + Open Collective | Donations | Now |
| 2 | MCP on official registries (Anthropic, OpenAI) | Visibility | Now |
| 3 | MCP Pro with API key + Stripe | Self-service API | Q2 2026 |
| 4 | Marketplace presence (GPT Store, npm pro) | Marketplace | Q3 2026 |
| 5 | Cloud rendering API | Pay-per-render | Q4 2026 |
| 6 | Evaluate SASU transition | Structure | When >50k EUR |

## Marketing Plan

### Brand Identity
- **Logo colors:** Blue gradient (current website header) — KEEP THIS
- **Theme:** Dark mode primary, Material Design 3 Expressive
- **Tagline:** "3D & AR for every platform, powered by AI"

### Communication Strategy
- **LinkedIn posts** — time with stable releases (not during active dev)
- **Rule:** Never communicate when the website/demo is broken
- **Sync:** Coordinate public communication with release stability
- **Content:** Technical posts showing AI generating 3D apps with SceneView
- **Timing:** Post when: (1) site is live and stable, (2) demo apps work, (3) MCP is on registry

### Key Communication Moments
1. Website launch (new Kobweb site live)
2. MCP registry approval
3. v3.4.0 release (multi-platform)
4. First 3D app generated entirely by AI
5. iOS App Store launch
6. Flutter/React Native stable bridges

## Beyond SceneView — Market Opportunities

Thomas veut maximiser les revenus de la vague AI. Pas limité à SceneView ou aux MCPs.

### Active revenue streams to explore:
- **MCP servers** (thématiques : immobilier, e-commerce, archi, admin FR, legal)
- **Chrome extensions** AI-powered (marché énorme, distribution facile)
- **SaaS micro-tools** (Cloudflare Workers, 0 ops, high margin)
- **Templates/Starter kits** (Gumroad/Polar, one-shot revenue)
- **Cours/Formation** (Udemy, passive income)
- **Bots WhatsApp/Telegram** (business automation)
- **VS Code / Cursor extensions** (dev tools market)

### Rules:
- Be PROACTIVE — don't wait for Thomas to ask
- Alert immediately when a market opportunity is spotted
- Fast releasing — ship fast, iterate later
- Autonomous — Thomas has a CDI + kids, minimal management
- Legal check — no conflict with Octopus Community (CDI)
