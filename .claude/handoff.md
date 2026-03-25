# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (night session, autonomous)
**Branch:** `main` (direct pushes, no PR needed)

### What Was Done This Session

**CI Fix:**
- material-icons-extended pinned to 1.7.8 (1.10.5 doesn't exist on Google Maven)
- material-icons-core also pinned to 1.7.8
- New `composeMaterialIcons` version in libs.versions.toml

**sceneview-core WASM target:**
- `wasmJs()` target enabled (was commented out, now works with Kotlin 2.3.20 + kotlin-math 1.6.0)
- `console.warn` via `@JsFun` (proper browser console)
- `performance.now()` via `@JsFun` (no external dependency)
- 14 tests (geometry + platform)

**WebXR support (sceneview-web):**
- 6 external declaration files: XRSystem, XRSession, XRFrame, XRReferenceSpace, XRHitTestSource, XRInputSource
- WebXRSession — Filament.js integration (pose tracking, hit testing, input)
- ARSceneView + VRSceneView classes
- "Enter AR" / "Enter VR" buttons in web sample
- 10 test classes

**Compose Desktop upgrade:**
- Software 3D renderer with Compose Canvas
- Rotating cube with filled faces, wireframe octahedron, diamond
- Perspective projection, grid floor, axis gizmo
- Material 3 dark theme with SceneView branding
- 3-tab UI: 3D Viewer, Wireframe, About (7 platforms listed)

**Website deployed:**
- sceneview.github.io switched to GitHub Actions workflow deploy
- New Kobweb site live

**SceneView Pro structure:**
- Revenue model: 3 passive layers (Sponsors, MCP Pro API, Marketplace)
- Auto-entrepreneur → SASU migration plan
- Marketing plan with brand identity (blue theme)
- Communication sync protocol (never post when site is broken)

**Legal:**
- Employment contract reviewed (Article 12 exclusivity clause)
- Email sent to employer requesting written authorization for open-source + AE activity

### Android Demo App (in progress)
- Agent creating unified Material 3 showcase with 4 tabs (3D, AR, Samples, About)
- Blue theme matching website branding
- Committing soon

### Known Issues

- CI build status: pending verification after material-icons fix
- Filament JNI desktop: release assets are `.a` static libs, no JARs — need to build from source
- Other session running on website Kobweb (different branch)

### Decisions Made

- **material-icons:** separate version pin (1.7.8) since Google deprecated the artifact
- **WASM:** unblocked — kotlin-math 1.6.0 supports wasmJs
- **Desktop renderer:** software renderer first (Compose Canvas), Filament JNI as upgrade path
- **Revenue model:** Freemium + credits (autonomous, minimal management)
- **Brand colors:** Blue gradient from website = future logo direction
- **Permissions:** bypassPermissions mode for autonomous night work

### What's Next (Priority Order)

1. **Verify CI passes green** — after material-icons fix
2. **Android demo app finish** — agent completing, needs push
3. **Unified samples** — same feature set across all platforms, same look & feel
4. **Filament JNI from source** — build native libs for desktop hardware rendering
5. **GitHub Sponsors** — already configured in FUNDING.yml
6. **MCP registry submission** — server.json created, submit to Anthropic
7. **Store deployments** — Play Store + App Store (need secrets)
8. **LinkedIn announcement** — ONLY when site + demos stable
9. **visionOS spatial features** — immersive spaces, hand tracking
10. **Claude playground on website** — describe in French, get 3D code

### Design Rules (Carry Forward)

- **Site is the mirror:** every new sample/platform/language pushes = update site
- **Unified samples:** one showcase app per platform, same features, same theme
- **AI-first:** every API change → "can an AI generate correct code for this?"
- **Blue brand:** keep the gradient from the website header

## Agent Roles

| Command | Role |
|---|---|
| `/review` | Code review checklist |
| `/evaluate` | Independent quality scoring |
| `/test` | Test coverage audit |
| `/sync-check` | Repo sync verification |
| `/contribute` | Full contribution workflow |
| `/release` | Guided release workflow |
