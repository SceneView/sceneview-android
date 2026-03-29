# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 6, continued)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION

### 1. URL-based model loading (Android + iOS)
- **Android**: `rememberModelInstance(modelLoader, fileLocation)` overload — auto-detects HTTP/HTTPS URLs
- **iOS**: `ModelNode.load(from: URL)` with download-to-temp for RealityKit

### 2. iOS HDR environments
- 6 HDR files in `samples/ios-demo/SceneViewDemo/Environments/`
- Xcode project.pbxproj fully configured

### 3. Progressive texture loading
- Enabled Filament async resource loading (`asyncBeginLoad`/`asyncUpdateLoad`)

### 4. GitHub Releases CDN
- `assets-v1` release with **25 models** hosted
- URL: `https://github.com/sceneview/sceneview/releases/download/assets-v1/<filename>`

### 5. Play Store fix — APK size reduction
- **Problem**: AAB exceeded 200 MB limit (421 MB of embedded models)
- **Fix**: Migrated 24 large models (>7MB) from local assets to CDN URL loading
- **Result**: APK reduced from 421 MB → 109 MB
- **Play Store deploy: SUCCESS** ✅

### 6. GitHub Sponsors tiers
- Added **$50/mo** tier (priority support, early access, medium logo)
- Added **$100/mo** tier (dedicated support, large logo, roadmap input)
- Now 4 tiers: $5, $25, $50, $100

### 7. Polar.sh
- Account active with dashboard working
- 3 products already configured (MCP Creator Kit €29, MCP Pro €9.99/mo, Starter Kit €49)
- No public profile page — Polar uses checkout links now (normal behavior)

### 8. App Store Connect
- iOS 1.0 **in Apple review** (submitted, waiting 24-48h)
- macOS/tvOS/visionOS: builds needed — Xcode installation in progress

### 9. Xcode installation
- Installing via `mas` (Mac App Store CLI) — ~30 GB download in progress
- Needed for macOS/tvOS/visionOS archives + App Store uploads

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — Xcode builds (when Xcode finishes installing)
- Archive and upload macOS build to App Store Connect
- Archive and upload tvOS build
- Archive and upload visionOS build
- Submit all 3 for review

### Priority 2 — Monitoring
- Check iOS review status (24-48h)
- Check Play Store review status (app is now deployed)

### Priority 3 — Polish
- Add loading indicator for URL-based models in demo (they take a moment to download)
- Add environment picker UI in iOS demo

## ASSET CATALOG STATUS
- **34 models** in catalog from 3 sources
- **25 models on GitHub Releases CDN** (assets-v1 tag)
- **6 HDR environments** on both Android AND iOS
- Android demo: 26 local models + 24 CDN models = 50 total in carousel
- iOS demo: 28 USDZ models (local)
- **Sources**: Sketchfab (28), KhronosGroup (5), Fab.com (1)

## FINANCIAL STATUS
- **Open Collective**: $2,338 USD (OSC fiscal host, 10% fee)
- **GitHub Sponsors**: 4 tiers ($5/$25/$50/$100), no active sponsors yet
- **Polar.sh**: active, 3 products, checkout links working
- **Monthly expenses**: Claude Max ~$168/mo (reimbursed via OC expense)

## STORE STATUS
- **Play Store**: ✅ Deployed successfully (109 MB AAB)
- **App Store iOS**: 🟡 In review (v1.0)
- **App Store macOS**: ⏳ Needs build upload (Xcode installing)
- **App Store tvOS**: ⏳ Needs build upload
- **App Store visionOS**: ⏳ Needs build upload

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
