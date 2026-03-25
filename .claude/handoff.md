# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 2026-03-25 (full day session)
**Branch:** `main` (direct pushes)

### What Was Done

**Release & Publishing:**
- v3.4.0 GitHub Release published (23 commits from v3.3.0)
- sceneview-mcp@3.4.2 on npm (with mcpName for registry)
- **SceneView MCP on official MCP Registry** — io.github.ThomasGorisse/sceneview
- Play Store deploy in progress (internal track)
- W-8BEN tax form submitted (France, 0% withholding)

**Apple Developer (Thomas Gorisse - 5G3DZ3TH45):**
- Apple Distribution certificate created (expires 2027-03-25)
- App ID registered: io.github.sceneview.demo
- Provisioning Profile: SceneView Demo App Store
- App Store Connect API key: SceneView CI/CD (Gestionnaire d'apps)
- All 6 iOS secrets uploaded to GitHub (21 total secrets)

**Code (night session):**
- WebXR AR/VR in sceneview-web (6 declaration files)
- Desktop software 3D renderer (Compose Canvas)
- Android demo Material 3 (4 tabs, 14 demos)
- WASM target prepared (disabled — kotlin-math lacks wasmJs variant)
- CI fixes (material-icons 1.7.8, AR emulator, wasmJs)

**Samples reorganization:**
- 15 obsolete samples deleted
- Renamed to {platform}-demo: android-demo, android-tv-demo, web-demo, ios-demo, desktop-demo, flutter-demo, react-native-demo
- Package name: io.github.sceneview.demo (matches Play Console)

**Website:**
- Kobweb site live on sceneview.github.io
- Updated to v3.4.0 (badge, platforms, 6 install tabs, changelog)

**Infrastructure:**
- 21 GitHub Secrets (all green)
- Hook: auto-reminder when secrets change
- Branding guide + SECRETS.md inventory
- SceneView Pro revenue model (3 passive layers)

### CRITICAL RULES

- **NEVER touch Octopus Community** — Thomas's current employer (CDI)
- **NEVER enter sensitive financial data** (bank, tax numbers, passwords)
- Octopus appears in Apple/Google accounts — DO NOT interact with it
- Use profile-private repo for pro/personal separation

### Known Issues

- Play Store: "Upload key reset" requested on 23 Mar — may need attention
- Play Store: package must match io.github.sceneview.demo exactly
- Dependabot: 6 vulnerabilities on main branch
- kotlin-math: no wasmJs variant (WASM target disabled)

### Pending (Thomas must do)

1. **npm org @sceneview** — create at npmjs.com/org/create
2. **GitHub Sponsors** — activate at github.com/sponsors/ThomasGorisse/dashboard
3. **Check Hélène's response** — AE authorization email
4. **Detach from Apple teams** — Digital Mate, Universal Music (NOT Octopus)

### What's Next

1. Verify Play Store deploy succeeds
2. Trigger first App Store build (all secrets ready)
3. Improve website (dark mode, more content, visual polish)
4. LinkedIn announcement (when everything is stable)
5. Filament JNI Desktop (hardware 3D)
6. Android XR module (Jetpack XR SceneCore)
7. Unified sample visual testing across platforms

### Design Rules

- **Site = mirror:** new sample/platform → update site
- **{platform}-demo:** consistent naming everywhere
- **AI-first:** "can AI generate correct code for this?"
- **Blue brand:** #1565C0 dark, #2196F3 primary, #64B5F6 light
- **Autonomy first:** revenue must be self-service

## Agent Roles

| Command | Role |
|---|---|
| `/review` | Code review checklist |
| `/evaluate` | Independent quality scoring |
| `/test` | Test coverage audit |
| `/sync-check` | Repo sync verification |
| `/release` | Guided release workflow |
