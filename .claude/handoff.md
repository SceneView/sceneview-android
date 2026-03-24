# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-24 / 2026-03-25 (evening + night session)
**Branch:** `feat/multi-platform-expansion`
**PR:** https://github.com/SceneView/sceneview/pull/709 (ready for review, ~38 commits)

### What Was Done

**Modules created:**
- `sceneview-web` — Kotlin/JS + Filament.js WASM (bindings, DSL, orbit camera, auto-resize, animation, WebXR AR, tests)
- `sceneview-desktop` — Compose Desktop + LWJGL scaffold (Filament JNI architecture documented)

**Samples:**
- `tv-model-viewer` — Android TV, D-pad controls
- `web-model-viewer` — Browser 3D viewer
- `ios-demo` — SwiftUI 3-tab App Store app

**Bridges completed:**
- Flutter Android (ComposeView + Scene) + iOS (UIHostingController + SceneViewSwift)
- React Native Android (ViewManager + Scene) + iOS (RCTViewManager + SceneViewSwift)
- Review fixes: RN per-instance state, Flutter AR node, web light implementation

**Website Kobweb:**
- 5 pages (Index, Quickstart, Samples, Playground, Changelog)
- M3 Expressive theme, responsive mobile (hamburger menu fixed)
- DamagedHelmet.glb model, model-viewer web component
- Copyright updated, styles.css + CSS injection for static export
- Build: `kobwebExport` + `injectResponsiveCss` — BUILD SUCCESSFUL

**CI/CD:**
- `docs.yml` — Kobweb (root) + MkDocs (/docs/) combined deploy
- `app-store.yml` — TestFlight pipeline
- `ci.yml` — web-desktop + website jobs added
- `release.yml` — npm sceneview-web publish added

**MCP:**
- 2 web samples, `get_web_setup` tool, web validator (4 rules, 384 tests)
- dist rebuilt

**Docs:**
- 4 quickstart guides (web, TV, Flutter, React Native)
- llms.txt with WebXR AR documentation, synced to docs/ and mcp/
- README with 9-platform table

**Infrastructure:**
- Harness design patterns (handoff.md, /evaluate, settings.json hooks)
- Branding reference (.claude/branding.md) — colors, typography, shapes
- Marketing plan — 6 triggers, 7 channels, release sync protocol
- WASM target prepared (actuals written, blocked by kotlin-math)

### Known Issues

- `material-icons-extended:1.10.5` not on Maven Central (pre-existing)
- Website mobile navbar: CSS injection needed for Kobweb static export
- Filament JNI desktop: requires building from source (no Maven artifact)
- kotlin-math: no wasmJs target yet

### Decisions Made

- **Web renderer:** Filament.js (same engine as Android)
- **WebXR:** production-ready, integrated with Filament.js
- **Desktop:** Filament JNI via SwingPanel (same Java API as Android)
- **WASM:** prepared but blocked by kotlin-math dependency
- **settings.gradle:** PREFER_PROJECT for Kotlin/JS repos
- **Android Auto/CarPlay:** excluded (no custom views)
- **Branding:** blue #1a73e8 / #8ab4f8, Inter font, M3 Expressive shapes

### What's Next (Priority Order)

1. **Merge PR #709** — review and merge into main
2. **Deploy website** — trigger docs.yml workflow_dispatch after merge
3. **Deploy stores** — Play Store (demo-v1.0.0 tag) + App Store (ios-demo-v1.0.0)
4. **Build Filament from source** — get filament-java.jar for desktop
5. **Publish npm** — `@sceneview/sceneview-web` + sceneview-web dist
6. **Marketing communication** — after all green on pre-launch checklist
7. **visionOS spatial features** — immersive spaces, hand tracking

## Agent Roles

| Command | Role |
|---|---|
| `/review` | Code review checklist |
| `/evaluate` | Independent quality scoring |
| `/test` | Test coverage audit |
| `/sync-check` | Repo sync verification |
| `/contribute` | Full contribution workflow |
| `/release` | Guided release workflow |
