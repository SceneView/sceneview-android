# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (marathon session — 90+ commits)
**Branch:** `main`
**Releases:** v3.4.0, v3.4.1, v3.4.2, v3.4.3 (GitHub), MCP v3.4.7 (npm + official registry)
**Total commits:** 90+

### Everything delivered this marathon session

**Releases & Publishing:**
- v3.4.0, v3.4.1, v3.4.2, v3.4.3 released on GitHub
- MCP v3.4.7 published on npm (`@sceneview/mcp`)
- MCP published on official registry: `io.github.sceneview/mcp`
- `render_3d_preview` MCP tool added

**GitHub Org:**
- Renamed SceneView -> sceneview (lowercase)
- 28 stale references cleaned up across codebase

**Website (static HTML, replaces Kobweb):**
- sceneview.github.io — dark theme, model-viewer 3D, M3 Expressive
- Interactive playground page with code examples + 3D preview
- 3D embed widget (`/preview/embed`) — embeddable iframe viewer
- /go/ — universal smart links (platform detection -> store redirect, 8 redirectors)
- /preview — interactive 3D preview (model URL -> AR + share)

**Bug Fixes:**
- AR crash #713 FIXED — materials regenerated for Filament 1.70.0
- MeshNode boundingBox #711 fixed
- 15 audit issues fixed (stale files, versions, references)
- 6 Dependabot vulnerabilities resolved

**iOS:**
- Xcode project configured with signing
- App Store workflow created (macos-14 runner for iOS SDK)
- Still needs real Apple Developer certificate for distribution

**Code & Architecture:**
- WebXR AR/VR (sceneview-web)
- Desktop software 3D renderer (Compose Canvas)
- Android demo Material 3 (4 tabs, 14 demos)
- WASM target prep in sceneview-core (wasmJs(), 14 tests)
- Samples renamed to {platform}-demo convention (7 platforms)
- README: 622 -> 200 lines
- CHANGELOG, ROADMAP rewritten
- GitHub description + 20 topics

**Branding & Assets:**
- SVG logos, adaptive icons, favicon
- Social preview image uploaded to GitHub repo
- Store checklist prepared
- LinkedIn posts drafted (3 options in English — DO NOT POST without Thomas approval)

**Docs & Community:**
- Platforms page (all supported platforms)
- Android XR documentation
- visionOS spatial features documentation
- Filament Desktop documentation
- Community page
- SPONSORS.md, CONTRIBUTING.md
- GitHub issue/PR templates + README badges

**DevOps & Workflows:**
- Discord notification webhook
- MCP Pro API scaffold (Cloudflare Workers + Stripe)
- OpenAI GPT Store preparation

**Strategy & Monetization:**
- pro/STRATEGY.md — full plan (deeplinks, monetization, plugins, 3D AI)
- pro/LINKEDIN_DRAFT.md — 3 post options ready
- Revenue streams active:
  - Open Collective ($2.6k balance)
  - GitHub Sponsors: active, Stripe verified
  - Polar.sh: SceneView org created + MCP Pro product ($9.99/mo, test mode)
  - Google Play: W-8BEN tax form completed (France, 0% withholding)

**Infrastructure & Secrets:**
- 21+ GitHub Secrets configured (Android + iOS + Maven + npm + GPG)
- Apple Developer: cert + provisioning + API key (team 5G3DZ3TH45)

### CRITICAL RULES
- **NEVER touch Octopus Community** — employer (CDI)
- **NEVER enter sensitive financial data**

### Pending (Thomas — manual actions required)
- [ ] Login to Apple Developer to create real iOS distribution certificate
- [ ] Publish LinkedIn post (3 drafts ready, DO NOT POST without Thomas approval)
- [ ] Polar.sh Go Live (switch from test mode to production)
- [ ] Delete old Play Store apps (AR Wall Paint, AR for TikTok, Info Trafic Nantes)
- [ ] Re-enable Mac sleep (Battery settings)

### What's Next (prioritized, for future sessions)
1. **Maven Central v3.4.0 publish** — gradle.properties still says 3.3.0, needs version bump + Sonatype publish
2. **App Store first TestFlight build** — needs real Apple cert (Thomas action first)
3. **Play Store deploy** — key reset should be done by ~27 March
4. **GitHub Sponsors tier update** — configure tiers ($5/$15/$50/$200) and benefits
5. **MCP Pro backend implementation** — connect Stripe + Redis via Cloudflare Workers
6. **LinkedIn posts** — approve and publish (3 drafts ready, DO NOT POST)
7. Filament JNI Desktop (18-29 day effort, high complexity)
8. Android XR module
9. KMP core XCFramework: build and integrate into SceneViewSwift
10. visionOS spatial features (immersive spaces, hand tracking)
11. AI 3D app prototype
12. Merge sceneview+arsceneview (v3.5.0)
13. Unify naming: SceneView {} everywhere
14. Publish sceneview-web to npm: `@sceneview/sceneview-web`
15. Publish Flutter plugin to pub.dev
16. Publish React Native module to npm

## Previous Sessions (archived)

**2026-03-25 night — Autonomous session:**
- WASM target, WebXR AR/VR, CI fix, Desktop renderer
- Android demo M3, website Kobweb, SceneView Pro structure
- Platform roadmap updated

**2026-03-24 evening — Multi-platform expansion:**
- sceneview-web (Kotlin/JS + Filament.js WASM)
- Android TV sample, iOS demo app, App Store workflow
- Flutter + React Native bridges completed
- Release workflow updated for npm

**2026-03-24 — SceneViewSwift + stabilization:**
- Phases 1-6 complete (Swift nodes, tests, docs, MCP iOS, project stabilization, cross-framework scaffolds)
- 4 critical math bugs fixed in sceneview-core
- Critical AR bug fix (Frame.hitTest direction)
- 1081 lines of tests added across 15 files

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync |
| `/release` | Release workflow |
