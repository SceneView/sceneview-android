# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 8)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 8)

### 1. CI Fix — Swift 6 strict concurrency
- Added `@MainActor` to `HapticManager` — UIKit haptic generators are MainActor-isolated
- Fixed TestFlight deploy failure (exit code 65)
- All 4 CI workflows now green

### 2. macOS App Store submission — SUCCESS
- 21 files committed for macOS support (previous session, pushed this session)
- Created 3 synthetic macOS screenshots (2880x1800) with PIL
- Submitted build 357 — **En attente de vérification**

### 3. iOS demo improvements
- **Environment picker** — 6 HDR presets (Studio, Outdoor, Sunset, Night, Warm, Autumn) in ExploreTab
- **macOS app icons** — Generated proper sizes (16-1024px) from 1024x1024 source
- **Version fix** — AboutTab updated from v3.4.7 to v3.5.0

### 4. Version alignment — complete sweep
- MCP source: 18 files updated 3.4.7 → 3.5.0
- MCP bumped to 3.5.3 and **published to npm**
- README CDN: 1.2.0 → 3.5.0 (3 occurrences)
- website-static: 1.4.0 → 3.5.0 (index.html + web.html)
- CLAUDE.md: MCP version updated to 3.5.3
- All 1204 MCP tests pass

### 5. Documentation
- Rebuilt mkdocs site (166 files) — zero 3.4.7 references
- Site deployed via GitHub Actions
- Added ViewNode, SceneSnapshot, allPresets to llms.txt

### 6. CI/CD improvements
- Extended app-store.yml: iOS + macOS deploy jobs (parallel)
- macOS job ready — just needs `MACOS_PROVISIONING_PROFILE_BASE64` secret
- Renamed workflow: "Deploy Demo to App Store"

### 7. Cleanup
- Added `samples/ios-demo/.build/` to .gitignore

## Previous sessions (7 and earlier)

- iOS Distribution certificate created + provisioning profile
- iOS v1.0 build 355 submitted for review
- Xcode project manual signing configured
- App Store Connect API key created (8P64Z7HCSN)
- URL-based model loading (Android + iOS)
- iOS HDR environments (6 files)
- Progressive texture loading (Filament async)
- GitHub Releases CDN (25 models, `assets-v1` tag)
- Play Store APK 421→109 MB, deployed successfully
- GitHub Sponsors 4 tiers ($5/$25/$50/$100)
- Polar.sh active (3 products)

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — App Store reviews in progress
- **iOS**: v1.0 build 355 — **En attente de vérification**
- **macOS**: v1.0 build 357 — **En attente de vérification**

### Priority 2 — CI secrets for macOS auto-deploy
- Create macOS provisioning profile on Apple Developer Portal
- base64 encode it and set `MACOS_PROVISIONING_PROFILE_BASE64` GitHub secret
- Then macOS builds will auto-deploy alongside iOS

### Priority 3 — Future platform builds
- **tvOS**: Needs new Xcode target + build (RealityKit not available — would need SceneKit)
- **visionOS**: Needs new Xcode target + build (RealityKit available, feasible)

### Priority 4 — Monitoring
- Check iOS review result
- Check macOS review result
- Check Play Store status (deployed)

### Priority 5 — Polish
- Loading indicator for URL-based models in demo
- More demos in iOS app

### Priority 6 — Signing maintenance
- Backup `.secrets/` to Google Drive when sync resumes
- Update GitHub Actions secrets with new API key if CI needs it
- P12 password for `.secrets/ios_distribution_2027.p12` is `sceneview`

## SIGNING REFERENCE

| Item | Location | Notes |
|---|---|---|
| iOS Distribution cert | Login keychain | `iPhone Distribution: Thomas Gorisse (5G3DZ3TH45)` — expires 2027/03/29 |
| Apple Distribution cert | Login keychain | `Apple Distribution: Thomas Gorisse (5G3DZ3TH45)` — NO private key (old) |
| Provisioning profile | `~/Library/MobileDevice/Provisioning Profiles/3e147129-...` | "SceneView Demo App Store" |
| API key (upload) | `~/.private_keys/AuthKey_8P64Z7HCSN.p8` | Issuer: `551bbb3e-a7f4-4e2e-9486-bf487256fd0f` |
| API key (CI/CD) | GitHub Actions secrets | Key ID: `C77W6AGSZT` (p8 not available locally) |
| P12 backup | `.secrets/ios_distribution_2027.p12` | Password: `sceneview` |
| Private key backup | `.secrets/ios_distribution_2027.key` | PEM format |

### How to archive and upload (for future sessions)

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer

# Archive iOS
cd samples/ios-demo
xcodebuild -scheme SceneViewDemo -destination "generic/platform=iOS" archive \
  -archivePath /tmp/SceneViewDemo-iOS.xcarchive \
  CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM=5G3DZ3TH45 \
  "CODE_SIGN_IDENTITY=iPhone Distribution: Thomas Gorisse (5G3DZ3TH45)" \
  PROVISIONING_PROFILE_SPECIFIER="SceneView Demo App Store"

# Archive macOS
xcodebuild -scheme SceneViewDemo -destination "generic/platform=macOS" archive \
  -archivePath /tmp/SceneViewDemo-macOS.xcarchive \
  CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM=5G3DZ3TH45 \
  "CODE_SIGN_IDENTITY=Apple Distribution: Thomas Gorisse (5G3DZ3TH45)"

# Upload
xcrun altool --upload-app --type ios \
  --file /tmp/SceneViewDemo-export/SceneViewDemo.ipa \
  --apiKey 8P64Z7HCSN \
  --apiIssuer 551bbb3e-a7f4-4e2e-9486-bf487256fd0f
```

## ASSET CATALOG STATUS
- **34 models** in catalog from 3 sources
- **25 models on GitHub Releases CDN** (assets-v1 tag)
- **6 HDR environments** on both Android AND iOS
- Android demo: 26 local models + 24 CDN models = 50 total in carousel
- iOS demo: 28 USDZ models (local) + environment picker (6 HDR presets)
- **Sources**: Sketchfab (28), KhronosGroup (5), Fab.com (1)

## FINANCIAL STATUS
- **Open Collective**: $2,338 USD (OSC fiscal host, 10% fee)
- **GitHub Sponsors**: 4 tiers ($5/$25/$50/$100), no active sponsors yet
- **Polar.sh**: active, 3 products, checkout links working
- **Monthly expenses**: Claude Max ~$168/mo (reimbursed via OC expense)

## STORE STATUS
- **Play Store**: ✅ Deployed successfully (109 MB AAB)
- **App Store iOS**: 🟡 v1.0 (build 355) — En attente de vérification
- **App Store macOS**: 🟡 v1.0 (build 357) — En attente de vérification
- **App Store tvOS**: ⏳ Needs new Xcode target + build
- **App Store visionOS**: ⏳ Needs new Xcode target + build

## PUBLISHED ARTIFACTS
- **sceneview-mcp**: 3.5.3 on npm (1204 tests)
- **sceneview-web**: 3.5.0 on npm
- **sceneview**: 3.5.0 on Maven Central
- **arsceneview**: 3.5.0 on Maven Central
- **Website**: sceneview.github.io (deployed, all versions 3.5.0)

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
