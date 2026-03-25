# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (night session, autonomous)
**Branch:** `main` (direct pushes)
**Commits:** ~15 commits pushed this session

### What Was Done

**Samples reorganization (big cleanup):**
- Deleted 15 obsolete samples (all merged into android-demo)
- Renamed all samples to `{platform}-demo` convention
- Moved desktop-demo into samples/
- Copied flutter-demo and react-native-demo into samples/
- Final structure: 7 platform demos + common + recipes

**New features:**
- sceneview-core WASM target (`wasmJs()`, 14 tests)
- WebXR AR/VR in sceneview-web (6 declaration files, Filament integration)
- Desktop software 3D renderer (Compose Canvas, wireframe shapes)
- Android demo app Material 3 (4 tabs, 14 demos, blue branding)

**CI fixes:**
- material-icons-extended pinned to 1.7.8 (1.10.5 doesn't exist)
- material-icons-core pinned to 1.7.8
- Missing icons dependency in gltf-camera and reflection-probe (before deletion)
- CI workflows updated for new sample names
- APK builds passing green ✅

**Website:**
- sceneview.github.io deployed (Kobweb, GitHub Actions workflow mode)

**Docs:**
- README: 9-platform table added
- llms.txt: synced across root, docs/, mcp/
- CLAUDE.md: updated with night session, Android XR roadmap

**Business:**
- SceneView Pro structure (3 passive revenue layers)
- Branding guide with store asset checklist
- Email to employer for AE authorization sent

### Current Sample Structure

```
samples/
├── android-demo/         ← Android (Play Store) — 4 tabs, 14 demos
├── android-tv-demo/      ← Android TV — D-pad, auto-rotation
├── web-demo/             ← Web — Filament.js, WebXR AR/VR
├── ios-demo/             ← iOS (App Store) — SwiftUI 3 tabs
├── desktop-demo/         ← Desktop — software 3D, Compose Desktop
├── flutter-demo/         ← Flutter — PlatformView bridge
├── react-native-demo/    ← React Native — Fabric bridge
├── common/               ← Shared Android helpers
└── recipes/              ← Code recipes (markdown)
```

### Known Issues

- Dependabot: 6 vulnerabilities (1 high, 3 moderate, 2 low) — check
- git gc warning: too many unreachable loose objects (`git prune` needed)
- Flutter/RN demos are copies — originals still in flutter/ and react-native/
- Desktop: software renderer only, Filament JNI needs building from source

### Decisions Made

- **Sample naming:** `{platform}-demo` everywhere
- **All samples in samples/:** even non-Gradle ones (Flutter, RN, iOS)
- **Android XR:** added to roadmap, uses Jetpack XR SceneCore
- **Wear OS / Android Auto:** excluded (no 3D rendering support)
- **Brand colors:** Blue gradient — #1565C0 (dark), #2196F3 (primary), #64B5F6 (light)
- **Revenue model:** Freemium + credits, 3 passive layers, auto-entrepreneur → SASU

### What's Next (Priority Order)

1. **Check employer response** — Hélène's authorization email
2. **Fix Dependabot vulnerabilities** — 6 security issues
3. **Run git prune** — clean up loose objects
4. **Verify all CI green** — latest commits building correctly
5. **Store assets** — create feature graphics, screenshots for Play/App Store
6. **Filament JNI from source** — real 3D on Desktop
7. **Android XR module** — Jetpack XR SceneCore integration
8. **GitHub Sponsors tiers** — set up sponsor badges
9. **MCP registry submission** — submit to Anthropic
10. **LinkedIn announcement** — ONLY when everything is stable

### Design Rules (Carry Forward)

- **Site is the mirror:** every new sample/platform = update site
- **Unified samples:** `{platform}-demo`, same features, same theme
- **AI-first:** every API change → "can an AI generate correct code for this?"
- **Blue brand:** isometric cube logo, blue gradient
- **Autonomy first:** revenue model must be self-service, zero management

## Agent Roles

| Command | Role |
|---|---|
| `/review` | Code review checklist |
| `/evaluate` | Independent quality scoring |
| `/test` | Test coverage audit |
| `/sync-check` | Repo sync verification |
| `/contribute` | Full contribution workflow |
| `/release` | Guided release workflow |
