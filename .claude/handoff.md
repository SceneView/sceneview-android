# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 8)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 8)

### 1. macOS support — committed & pushed
- 21 files committed (`e74e408e`) — all macOS compilation support:
  - `SceneViewSwift/Sources/` — Added `os(macOS)` platform checks across 12 files
  - `samples/ios-demo/` — Multiplatform Xcode project, Package.swift with .macOS(.v15)
  - `SceneViewDemoApp.swift` — NSColor typealias, AR tab iOS-only guard
  - `.entitlements` — App Sandbox + network.client
  - `Info.plist` — LSApplicationCategoryType
  - `AppIcon.appiconset/Contents.json` — macOS icon sizes
  - `SamplesTab.swift` — iOS-only navigationBarTitleDisplayMode

### 2. macOS App Store submission — SUCCESS
- Built macOS app locally (debug, unsigned, 1200x800 window)
- Created 3 synthetic macOS screenshots (2880x1800) with PIL — macOS window chrome
- Uploaded screenshots to App Store Connect
- Selected build 357 (with RealityKit fix)
- Handled export compliance ("Aucun des algorithmes")
- Filled contact info, unchecked login requirement
- **Submitted for review — "En attente de vérification"**

### 3. tvOS / visionOS — assessed, deferred
- Both show "À finaliser avant soumission" on App Store Connect but have NO builds
- Would require new Xcode targets + platform-specific SDK work
- Not actionable this session — deferred to future work

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
- **macOS**: v1.0 build 357 — **En attente de vérification** (just submitted session 8)

### Priority 2 — Future platform builds
- **tvOS**: Needs new Xcode target + build + screenshots + metadata
- **visionOS**: Needs new Xcode target + build + screenshots + metadata

### Priority 3 — Monitoring
- Check iOS review result
- Check macOS review result
- Check Play Store status (deployed)

### Priority 4 — Polish
- Loading indicator for URL-based models in demo
- Environment picker UI in iOS demo

### Priority 5 — Signing maintenance
- Backup `.secrets/` to Google Drive when sync resumes (currently paused)
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

# Archive
cd samples/ios-demo
xcodebuild -scheme SceneViewDemo -destination "generic/platform=iOS" archive \
  -archivePath /tmp/SceneViewDemo-iOS.xcarchive \
  CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM=5G3DZ3TH45 \
  "CODE_SIGN_IDENTITY=iPhone Distribution: Thomas Gorisse (5G3DZ3TH45)" \
  PROVISIONING_PROFILE_SPECIFIER="SceneView Demo App Store"

# Export IPA
xcodebuild -exportArchive \
  -archivePath /tmp/SceneViewDemo-iOS.xcarchive \
  -exportOptionsPlist /tmp/ExportOptions-local.plist \
  -exportPath /tmp/SceneViewDemo-export

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
- iOS demo: 28 USDZ models (local)
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

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
