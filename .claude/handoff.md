# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (marathon session continued)
**Branch:** `main`
**Releases:** v3.4.0 + v3.4.1
**Total commits:** 50+

### Everything delivered

**Releases & Publishing:**
- v3.4.0 + v3.4.1 on GitHub
- MCP published on official registry: `io.github.sceneview/mcp` v3.4.6
- sceneview-mcp on npm (with render_3d_preview tool)

**GitHub Org:**
- Renamed SceneView -> sceneview (lowercase)

**Website (static HTML, replaces Kobweb):**
- sceneview.github.io — dark theme, model-viewer 3D, M3 Expressive
- /go — universal smart link (platform detection -> store redirect)
- /preview — interactive 3D preview (model URL -> AR + share)
- /preview/embed — embeddable iframe viewer

**Infrastructure:**
- 21 GitHub Secrets configured (Android + iOS + Maven + npm + GPG)
- Apple Developer: cert + provisioning + API key (team 5G3DZ3TH45)
- Google Play: W-8BEN tax form completed (France, 0% withholding)
- GitHub Sponsors: active, Stripe verified
- Polar.sh: SceneView org created + MCP Pro product ($9.99/mo, test mode)

**Code:**
- WebXR AR/VR (sceneview-web)
- Desktop software 3D renderer
- Android demo Material 3 (4 tabs, 14 demos)
- WASM target prep in sceneview-core
- Samples renamed to {platform}-demo convention (7 platforms)
- 15 audit issues fixed (stale files, versions, references)
- README: 622 -> 200 lines
- CHANGELOG, ROADMAP rewritten
- GitHub description + 20 topics

**Branding:**
- SVG logos, adaptive icons, favicon
- Store checklist prepared
- LinkedIn posts drafted (3 options in English — DO NOT POST yet)

**Strategy:**
- pro/STRATEGY.md — full plan (deeplinks, monetization, plugins, 3D AI)
- pro/LINKEDIN_DRAFT.md — 3 post options ready
- Revenue: Open Collective ($2.6k balance) + GitHub Sponsors + Polar.sh (future)

### CRITICAL RULES
- **NEVER touch Octopus Community** — employer (CDI)
- **NEVER enter sensitive financial data**

### Pending (Thomas)
- [ ] Polar.sh Go Live (switch from test mode to production)
- [ ] Play Store deploy retry (~27 March, key reset done)
- [ ] App Store first build via Xcode
- [ ] LinkedIn post (when site + stores verified)
- [ ] GitHub Sponsors tier update (see branding/SPONSOR_TIERS.md)

### What's Next
1. **Polar.sh Go Live** — switch from test mode to production
2. **Play Store deploy** — key reset should be done ~27 March
3. **App Store first build** — Xcode project for ios-demo
4. **LinkedIn post** — when stable (3 drafts ready, DO NOT POST)
5. **GitHub Sponsors tier update** — see branding/SPONSOR_TIERS.md
6. **MCP Pro implementation** — Cloudflare Workers + Stripe via Polar.sh
7. Filament JNI Desktop (build from source)
8. Android XR module
9. AI 3D app prototype
10. Merge sceneview+arsceneview (v3.5.0)
11. Unify naming: SceneView {} everywhere

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync |
| `/release` | Release workflow |
