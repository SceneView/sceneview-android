# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (marathon session — FINAL)
**Branch:** `main`
**Releases:** v3.4.0 + v3.4.1 (GitHub), MCP v3.4.6 (npm + official registry)
**Total commits:** 50+

### Everything delivered this marathon session

**Releases & Publishing:**
- v3.4.0 + v3.4.1 released on GitHub
- MCP published on official registry: `io.github.sceneview/mcp` v3.4.6
- sceneview-mcp on npm (with render_3d_preview tool)

**GitHub Org:**
- Renamed SceneView -> sceneview (lowercase)

**Website (static HTML, replaces Kobweb):**
- sceneview.github.io — dark theme, model-viewer 3D, M3 Expressive
- /go — universal smart links (platform detection -> store redirect, 8 redirectors)
- /preview — interactive 3D preview (model URL -> AR + share)
- /preview/embed — embeddable iframe viewer

**Infrastructure & Secrets:**
- 21+ GitHub Secrets configured (Android + iOS + Maven + npm + GPG)
- Apple Developer: cert + provisioning + API key (team 5G3DZ3TH45)
- Google Play: W-8BEN tax form completed (France, 0% withholding)
- GitHub Sponsors: active, Stripe verified
- Polar.sh: SceneView org created + MCP Pro product ($9.99/mo, test mode)

**Bug Fixes:**
- AR crash #713 FIXED — materials regenerated for Filament 1.70
- MeshNode boundingBox #711 fixed
- 15 audit issues fixed (stale files, versions, references)
- 6 Dependabot vulnerabilities resolved

**Code & Architecture:**
- WebXR AR/VR (sceneview-web)
- Desktop software 3D renderer
- Android demo Material 3 (4 tabs, 14 demos)
- WASM target prep in sceneview-core
- Samples renamed to {platform}-demo convention (7 platforms)
- README: 622 -> 200 lines
- CHANGELOG, ROADMAP rewritten
- GitHub description + 20 topics

**Branding & Assets:**
- SVG logos, adaptive icons, favicon
- Social preview image uploaded to GitHub repo
- Store checklist prepared
- LinkedIn posts drafted (3 options in English — DO NOT POST yet)

**Docs & Community:**
- Platforms page (all supported platforms)
- Android XR documentation
- visionOS spatial features documentation
- Filament Desktop documentation
- Community page
- SPONSORS.md, CONTRIBUTING.md
- GitHub issue/PR templates + README badges

**DevOps & Workflows:**
- Discord notification workflow
- MCP Pro API scaffold (Cloudflare Workers + Stripe)
- OpenAI GPT Store preparation

**Strategy & Monetization:**
- pro/STRATEGY.md — full plan (deeplinks, monetization, plugins, 3D AI)
- pro/LINKEDIN_DRAFT.md — 3 post options ready
- Revenue: Open Collective ($2.6k balance) + GitHub Sponsors + Polar.sh (future)

### CRITICAL RULES
- **NEVER touch Octopus Community** — employer (CDI)
- **NEVER enter sensitive financial data**

### Pending (Thomas — manual actions required)
- [ ] Polar.sh Go Live (switch from test mode to production)
- [ ] GitHub Sponsors tier update (see branding/SPONSOR_TIERS.md)
- [ ] Play Store deploy (~27 March, key reset done)
- [ ] App Store first build (signing issues being resolved)
- [ ] LinkedIn posts approval then publish (3 drafts ready, DO NOT POST)
- [ ] Maven Central v3.4.0 publish
- [ ] Re-enable Mac sleep

### What's Next (prioritized)
1. **Polar.sh Go Live** — switch from test mode to production
2. **GitHub Sponsors tier update** — see branding/SPONSOR_TIERS.md
3. **Play Store deploy** — key reset should be done ~27 March
4. **App Store first build** — Xcode signing issues being resolved
5. **LinkedIn posts** — approve and publish (3 drafts ready)
6. **Maven Central v3.4.0 publish** — release to Maven Central
7. **MCP Pro implementation** — connect Stripe + Redis via Cloudflare Workers
8. Filament JNI Desktop (build from source)
9. Android XR module
10. AI 3D app prototype
11. Merge sceneview+arsceneview (v3.5.0)
12. Unify naming: SceneView {} everywhere

## Previous Sessions (archived)

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

**2026-03-25 night — Autonomous session:**
- WASM target, WebXR AR/VR, CI fix, Desktop renderer
- Android demo M3, website Kobweb, SceneView Pro structure
- Platform roadmap updated

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync |
| `/release` | Release workflow |
