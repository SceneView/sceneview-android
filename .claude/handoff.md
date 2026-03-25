# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (marathon — 24h session)
**Branch:** `main`
**Releases:** v3.4.0 + v3.4.1
**Total commits:** 50+

### Everything delivered

**Releases & Publishing:**
- v3.4.0 + v3.4.1 on GitHub
- sceneview-mcp@3.4.3 on npm (with render_3d_preview tool)
- MCP on official registry (io.github.ThomasGorisse/sceneview)

**Website (static HTML, replaces Kobweb):**
- sceneview.github.io — dark theme, model-viewer 3D, M3 Expressive
- /go — universal smart link (platform detection → store redirect)
- /preview — interactive 3D preview (model URL → AR + share)
- /preview/embed — embeddable iframe viewer

**Infrastructure:**
- 21 GitHub Secrets (Android + iOS + Maven + npm)
- Apple Developer: cert + provisioning + API key (team 5G3DZ3TH45)
- GitHub Sponsors: active, Stripe configured
- W-8BEN tax form submitted (France, 0%)
- Polar.sh added to FUNDING.yml

**Code:**
- WebXR AR/VR (sceneview-web)
- Desktop software 3D renderer
- Android demo Material 3 (4 tabs, 14 demos)
- Samples: 15→7, {platform}-demo naming
- README: 622→200 lines
- CHANGELOG, ROADMAP rewritten
- GitHub description + 20 topics

**Strategy:**
- pro/STRATEGY.md — full plan (deeplinks, monetization, plugins, 3D AI)
- pro/LINKEDIN_DRAFT.md — 3 post options ready
- Revenue: Open Collective ($2.6k balance) + GitHub Sponsors + Polar.sh (future)

### CRITICAL RULES
- **NEVER touch Octopus Community** — employer (CDI)
- **NEVER enter sensitive financial data**

### Pending (Thomas)
- [ ] Supprimer 3 apps Play Store (AR Wall Paint, AR for TikTok, Info Trafic Nantes)
- [ ] Créer Polar.sh (sign in with GitHub)
- [ ] Checker réponse Hélène
- [ ] Play Store retry (~26 mars, key reset done)

### What's Next
1. Play Store deploy (key reset should be done)
2. App Store Xcode project for ios-demo
3. LinkedIn post (when site + stores verified)
4. Filament JNI Desktop (build from source)
5. Android XR module
6. MCP Pro with Polar.sh subscriptions
7. AI 3D app prototype
8. Merge sceneview+arsceneview (v3.5.0)
9. Unify naming: SceneView {} everywhere

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync |
| `/release` | Release workflow |
