# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-26 (continuation of 03-25 marathon)
**Branch:** `main`
**Releases:** v3.4.0 through v3.4.5 (GitHub), MCP v3.4.7 (npm + official registry)
**Total commits on main:** ~1250+

---

### Everything delivered — 2026-03-26 session

#### Filament.js Web — PROVEN
- **SceneView Web Filament.js renders PBR models in the browser** — DamagedHelmet.glb loaded and rendered correctly
- `filament-pure-test.html` — minimal proof of concept, Filament.js WASM rendering confirmed working
- `sceneview-demo.html` — full demo with 4 model switching (DamagedHelmet, Lantern, Fox, Avocado)
- `sceneview.js` — 15KB wrapper library created (simplified Filament.js API for SceneView)
- Known issue: `sceneview.js` has a double-init bug being investigated/fixed

#### v3.4.5 Released
- New GitHub Release published

#### MCP & Branding
- 8 MCP servers anonymized under "SceneView Tools" branding
- All hooks removed from settings

#### Revenue & Monetization
- **Polar.sh**: account approved, Stripe connected (ready for production)
- **GitHub Sponsors**: 5 tiers configured ($5/$15/$50/$99/$200)

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
- [ ] Delete old Play Store apps (AR Wall Paint, AR for TikTok, Info Trafic Nantes)
- [ ] Re-enable Mac sleep (Battery settings)
- [ ] Play Store key reset completion (~27 March)

---

### Version & Package Status

| Package | Latest | Published Where | Notes |
|---|---|---|---|
| sceneview (Android) | 3.3.0 | Maven Central | v3.4.x NOT yet on Maven Central |
| arsceneview (Android) | 3.3.0 | Maven Central | v3.4.x NOT yet on Maven Central |
| SceneViewSwift (iOS) | 3.3.0 | Swift Package (GitHub) | SPM direct from repo |
| @sceneview/mcp | 3.4.7 | npm + MCP Registry | Live and current |
| GitHub Releases | v3.4.5 | GitHub | Latest tag |
| sceneview.js | 15KB | Local (not published) | Filament.js wrapper library |

---

### What's Next (prioritized, for future sessions)
1. **Fix sceneview.js double-init bug** — wrapper library has initialization race condition
2. **Maven Central v3.4.0 publish** — gradle.properties still says 3.3.0, needs version bump + Sonatype publish
3. **Publish sceneview.js to npm** — `@sceneview/sceneview-web` or `@sceneview/sceneview.js`
4. **App Store first TestFlight build** — needs real Apple cert (Thomas action first)
5. **Play Store deploy** — key reset should be done by ~27 March
6. **MCP Pro backend** — connect Stripe + Redis via Cloudflare Workers
7. **LinkedIn posts** — approve and publish (drafts ready, DO NOT POST)
8. Filament JNI Desktop (18-29 day effort, high complexity)
9. Android XR module
10. KMP core XCFramework: build and integrate into SceneViewSwift
11. visionOS spatial features (immersive spaces, hand tracking)
12. AI 3D app prototype
13. Merge sceneview+arsceneview (v3.5.0)
14. Unify naming: SceneView {} everywhere
15. Publish Flutter plugin to pub.dev
16. Publish React Native module to npm

---

## Previous Sessions (archived)

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
