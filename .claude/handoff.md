# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (marathon session — night + day)
**Branch:** `main`
**Total commits:** ~40+ this session

### Major Achievements

1. **v3.4.0 released** — GitHub Release, 23 commits from v3.3.0
2. **MCP on official registry** — `io.github.ThomasGorisse/sceneview` live
3. **Website rebuilt** — Kobweb → static HTML/CSS (zero build, model-viewer 3D)
4. **21 GitHub Secrets** — Android + iOS + Maven + npm all configured
5. **Apple Developer** — cert, provisioning profile, API key created
6. **README rewritten** — 622→200 lines, multi-platform, clean
7. **Samples reorganized** — 15→7 demos, `{platform}-demo` naming
8. **GitHub repo polished** — description, topics, cleanup

### Website
- **Tech:** Static HTML/CSS/JS + model-viewer web component
- **Location:** `website-static/` (deployed to `sceneview.github.io`)
- **Deploy:** Push to `SceneView/sceneview.github.io` repo directly
- **Old Kobweb:** Still in `website/` — can be removed in future session

### Stores
- **Play Store:** Blocked by upload key reset (48-72h from Mar 23)
- **App Store:** All secrets ready, workflow needs Xcode project (ios-demo uses Package.swift, not .xcodeproj)
- **W-8BEN tax form:** Submitted (0% withholding, France treaty)

### CRITICAL RULES
- **NEVER touch Octopus Community** — Thomas's employer (CDI)
- **NEVER enter sensitive financial data**
- Use profile-private for pro/personal separation

### What's Next
1. **Play Store** — retry deploy (key reset should be done by Mar 26)
2. **App Store** — create Xcode project for ios-demo OR restructure workflow
3. **npm @sceneview org** — Thomas needs to create at npmjs.com
4. **GitHub Sponsors** — Thomas needs to activate
5. **Model-viewer fix** — verify 3D helmet renders in real browser
6. **Remove old Kobweb** — clean up `website/` directory
7. **Merge sceneview+arsceneview** — v3.5.0 breaking change
8. **Unify naming** — `SceneView {}` everywhere
9. **Filament JNI Desktop** — hardware 3D rendering
10. **Android XR** — Jetpack XR SceneCore module

### Design Rules
- **Site = mirror:** new feature → update site
- **`{platform}-demo`:** consistent naming
- **AI-first:** can AI generate correct code?
- **Blue brand:** #1565C0, #2196F3, #64B5F6
- **Fast releasing:** ship → tag → publish → communicate

## Agent Roles
| Command | Role |
|---|---|
| `/review` | Code review checklist |
| `/evaluate` | Quality scoring |
| `/sync-check` | Repo sync verification |
| `/release` | Release workflow |
