# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-26 (final update, end of marathon)
**Branch:** `main`
**Releases:** v3.4.0 through v3.4.5 (GitHub), MCP v3.4.7 (npm + official registry), sceneview.js v1.1.0 (npm)
**Total commits on main:** ~1250+

---

### Everything delivered — 2026-03-26 session (FINAL)

#### Filament.js Web — PROVEN AND LIVE
- **SceneView Web Filament.js renders PBR models in the browser** — DamagedHelmet.glb loaded and rendered correctly
- `filament-pure-test.html` — minimal proof of concept, Filament.js WASM rendering confirmed working
- `sceneview-demo.html` — full demo with 4 model switching (DamagedHelmet, Lantern, Fox, Avocado)
- **sceneview.js v1.1.0 published on npm** — one-liner 3D for the web
- Double-init bug fixed, library production-ready

#### Procedural Geometry in MCP Artifacts
- Procedural geometry generation working in MCP artifact responses
- **676 tests** across all MCP servers

#### 9 MCP Servers — All v2.0.0
- SceneView MCP, Real Estate, E-commerce, Architecture, French Admin, Legal Docs — upgraded to v2.0.0
- **2 NEW MCPs**: Education MCP + Finance MCP
- Social Media MCP **RETIRED** (non-concurrence clause with employer)
- All servers anonymized under "SceneView Tools" / orgs

#### GitHub Orgs Reorganized
- **sceneview-tools** org created — commercial/tool repos
- **mcp-tools-lab** org created — MCP ecosystem repos
- Clean separation between open-source (sceneview) and commercial (sceneview-tools, mcp-tools-lab)

#### Open Source Contributions
- **PR #472 on Anthropic Claude Cookbooks** — SceneView MCP featured
- **2 PRs on awesome-mcp-servers** — SceneView MCPs listed

#### v3.4.5 Released
- New GitHub Release published

#### Revenue & Monetization
- **Polar.sh**: account approved, Stripe connected (ready for production)
- **GitHub Sponsors**: 5 tiers configured ($5/$15/$50/$99/$200)
- **Pro link on website** now points to Polar.sh

#### Profile & Credentials
- npm profile cleaned up
- All credentials saved/documented
- Social Media MCP retired for non-concurrence safety

#### Quality Automation
- Quality check scheduled every 3 hours (automated monitoring)

#### Communication
- Wave 1 communication drafts ready (LinkedIn, etc.)

---

### Everything delivered — 2026-03-25 marathon (100+ commits, 5 releases)

#### Releases & Publishing (5 GitHub releases + MCP)
- v3.4.0, v3.4.1, v3.4.2, v3.4.3, v3.4.4 released on GitHub
- MCP v3.4.7 published on npm (`@sceneview/mcp`) and official MCP registry (`io.github.sceneview/mcp`)
- `render_3d_preview` MCP tool added (code snippets + model URLs)
- MCP namespace fixed after org rename (lowercase `sceneview`)

#### MCP Servers Published (7+)
- `@sceneview/mcp` — core SceneView MCP (3D/AR code gen, iOS support, render_3d_preview)
- Real estate, E-commerce, Architecture, French admin, Social media, Legal docs MCP servers
- All servers have legal protection (TERMS, PRIVACY, disclaimers)

#### Chrome Extension, Prompt Store, AI Tools
- Chrome extension, prompt store, n8n templates, Telegram bot, AI invoice tool

#### Website (sceneview.github.io — static HTML, replaced Kobweb)
- Dark theme, model-viewer 3D hero, M3 Expressive design
- Playground page, 3D embed widget, smart links, SEO (meta tags, JSON-LD, sitemap)

#### Bug Fixes
- **AR crash #713 FIXED** — materials regenerated for Filament 1.70.0
- **MeshNode boundingBox #711 fixed**
- 15 audit issues, 6 Dependabot vulns, 28 stale refs cleaned

#### Legal, Branding, Revenue Setup
- All projects have LICENSE, TERMS, PRIVACY
- SVG logos, adaptive icons, favicon, social preview
- GitHub Sponsors active (Stripe verified), Polar.sh created, Open Collective $2.6k
- W-8BEN filed, strategy doc, MCP Pro scaffold, GPT Store prep

#### iOS, Android, Docs, Code
- iOS: Xcode project, signing, App Store workflow (needs real cert)
- Android demo: Play Store ready, 4-tab M3, 14 demos
- WASM target, WebXR, Desktop renderer, CI fix
- README rewritten, CHANGELOG, ROADMAP, community docs
- GitHub org renamed to lowercase `sceneview`

---

### CRITICAL RULES (always apply)
- **NEVER touch Octopus Community** — Thomas's employer (CDI)
- **NEVER enter sensitive financial data**
- **NEVER post on LinkedIn** without Thomas's explicit approval
- **Website branding**: blue color scheme + dark mode M3 Expressive — validated, do not change
- **Maximum autonomy**: fast releasing, don't ask at every step

---

### Pending (Thomas — manual actions required)
- [ ] Login to Apple Developer to create real iOS distribution certificate
- [ ] Publish LinkedIn post (drafts ready, DO NOT POST without approval)
- [ ] Polar.sh Go Live (switch from test mode to production)
- [ ] Delete old Play Store apps (AR Wall Paint, AR for TikTok, Info Trafic Nantes)
- [ ] Re-enable Mac sleep (Battery settings)
- [ ] Play Store key reset completion (~27 March)
- [ ] Configure GitHub Sponsors tiers on github.com/sponsors/sceneview

---

### Version & Package Status

| Package | Latest | Published Where | Notes |
|---|---|---|---|
| sceneview (Android) | 3.3.0 | Maven Central | v3.4.x NOT yet on Maven Central |
| arsceneview (Android) | 3.3.0 | Maven Central | v3.4.x NOT yet on Maven Central |
| SceneViewSwift (iOS) | 3.3.0 | Swift Package (GitHub) | SPM direct from repo |
| @sceneview/mcp | 3.4.7 | npm + MCP Registry | Live and current |
| sceneview.js | 1.1.0 | npm | One-liner 3D for the web |
| GitHub Releases | v3.4.5 | GitHub | Latest tag |
| Education MCP | 2.0.0 | npm | NEW |
| Finance MCP | 2.0.0 | npm | NEW |
| All other MCPs | 2.0.0 | npm | 9 total (Social Media retired) |

---

### What's Next (prioritized, for future sessions)
1. **Maven Central v3.4.0 publish** — gradle.properties still says 3.3.0, needs version bump + Sonatype publish
2. **App Store first TestFlight build** — needs real Apple cert (Thomas action first)
3. **Play Store deploy** — key reset should be done by ~27 March
4. **MCP Pro backend** — connect Stripe + Redis via Cloudflare Workers
5. **LinkedIn posts** — approve and publish (drafts ready, DO NOT POST)
6. Filament JNI Desktop (18-29 day effort, high complexity)
7. Android XR module
8. KMP core XCFramework: build and integrate into SceneViewSwift
9. visionOS spatial features (immersive spaces, hand tracking)
10. Publish Flutter plugin to pub.dev
11. Publish React Native module to npm
12. AI 3D app prototype
13. Merge sceneview+arsceneview (v3.5.0)
14. Unify naming: SceneView {} everywhere

---

## Previous Sessions (archived)

**2026-03-26 marathon — Filament.js web, sceneview.js npm, 9 MCPs v2.0.0, orgs, PRs:**
- See "Everything delivered — 2026-03-26 session" section above

**2026-03-25 marathon — Full-day (100+ commits, 5 releases):**
- See "Everything delivered — 2026-03-25 marathon" section above

**2026-03-25 night — Autonomous session:**
- WASM target, WebXR AR/VR, CI fix, Desktop renderer, Android demo M3, website Kobweb, SceneView Pro

**2026-03-24 evening — Multi-platform expansion:**
- sceneview-web, Android TV, iOS demo, Flutter + React Native bridges

**2026-03-24 — SceneViewSwift + stabilization:**
- Phases 1-6, 4 math bugs fixed, AR bug fix, 1081 lines of tests

---

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync |
| `/release` | Release workflow |
