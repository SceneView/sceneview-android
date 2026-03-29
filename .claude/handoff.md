# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 6)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION

### 1. URL-based model loading (Android + iOS)
- **Android**: Added `rememberModelInstance(modelLoader, fileLocation)` overload in `Scene.kt` that auto-detects HTTP/HTTPS URLs and routes through ModelLoader's existing Fuel HTTP client
- **iOS**: Added `ModelNode.load(from: URL)` in `ModelNode.swift` with download-to-temp approach (RealityKit requires local file URLs with correct extension)
- Updated `llms.txt` with both new APIs and platform mapping table

### 2. iOS HDR environments
- Copied 6 HDR files from Android demo to `samples/ios-demo/SceneViewDemo/Environments/`
- Files: `studio.hdr`, `outdoor_cloudy.hdr`, `sunset.hdr`, `rooftop_night.hdr`, `studio_warm.hdr`, `autumn_field.hdr`
- Updated Xcode `project.pbxproj` with PBXBuildFile, PBXFileReference, PBXGroup, and PBXResourcesBuildPhase entries

### 3. Progressive texture loading
- Enabled Filament async resource loading in `ModelLoader.kt`:
  - `resourceLoader.asyncUpdateLoad()` in `updateLoad()` (called every frame)
  - `resourceLoader.asyncBeginLoad(model)` + `evictResourceData()` in `loadResources()`
  - `resourceLoader.asyncBeginLoad(model)` in `loadResourcesSuspended()`
- Models now appear immediately with textures streaming in progressively

### 4. GitHub Releases CDN
- Created `assets-v1` release on GitHub for hosting large models
- Uploaded: earthquake_california.glb (39MB), nike_air_jordan.glb (30MB), porsche_911_turbo.glb (21MB), earthquake_california.usdz (24MB)
- Base URL: `https://github.com/sceneview/sceneview/releases/download/assets-v1/`

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — Thomas actions (accounts)
- **Activate Polar.sh** — go to polar.sh, connect SceneView GitHub org, set up tiers
- **Enrich GitHub Sponsors tiers** — add $50 and $100 tiers with better perks
- **App Store Connect** — create app "SceneView Demo" bundle ID `io.github.sceneview.demo`

### Priority 2 — Demo apps URL loading integration
- Add URL loading demo/example in Android ExploreScreen (load from GitHub Releases CDN)
- Add URL loading demo in iOS ExploreTab
- Test progressive loading behavior with large models

### Priority 3 — Play Store review
- Wait for Google to approve with updated screenshots

### Priority 4 — iOS environment switching UI
- HDR files are now in the iOS bundle
- Add environment picker UI in iOS demo (similar to Android's environment selector)

### Priority 5 — Further CDN optimization
- Consider hosting more models on GitHub Releases to reduce APK/IPA size
- Add progress indicator for URL-based model loading

## ASSET CATALOG STATUS
- **34 models** in catalog from 3 sources
- **6 HDR environments** from Poly Haven (now on both Android AND iOS)
- **61 GLB** in Android, **28 USDZ** in iOS
- **4 models on GitHub Releases CDN** (assets-v1 tag)
- **Sources**: Sketchfab (28), KhronosGroup (5), Fab.com (1)
- **Licenses**: CC-BY-4.0 (20+), CC-BY-NC-4.0 (8), CC-BY-NC-SA-4.0 (2), CC0-1.0 (1)

## FINANCIAL STATUS
- **Open Collective**: $2,338 USD (OSC fiscal host, 10% fee)
- **GitHub Sponsors**: configured for org `sceneview`, no active sponsors
- **Polar.sh**: in FUNDING.yml but page 404 — Thomas needs to activate
- **Monthly expenses**: Claude Max ~$168/mo (reimbursed via OC expense)
- **Process**: Pay with personal card → submit expense on OC → get reimbursed

## ACTIONS THOMAS
1. **Polar.sh**: activate account at polar.sh/sceneview
2. **GitHub Sponsors**: add $50 and $100 tiers
3. **App Store Connect**: create app "SceneView Demo" (`io.github.sceneview.demo`)
4. **Play Store**: check if Google review passes

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
