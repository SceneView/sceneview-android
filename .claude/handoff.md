# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 7)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 7)

### 1. iOS Distribution certificate — created from scratch
- Generated new private key + CSR locally (`/tmp/ios_dist_new.key`)
- Created iOS Distribution certificate via Apple Developer Portal (Chrome)
- Imported cert + key as p12 into login keychain
- Installed Apple WWDR G3 + Root CA intermediate certificates for trust chain
- Result: 2 valid codesigning identities (`Apple Distribution` + `iPhone Distribution`)

### 2. Provisioning profile regenerated
- Edited "SceneView Demo App Store" profile on Apple Developer Portal
- Selected new iOS Distribution certificate (Mar 29, 2027 expiry)
- Downloaded and installed profile (UUID: `3e147129-47e6-4b9d-a02b-c12e0d3df184`)

### 3. Xcode project signing fixed
- Changed Release config: `CODE_SIGN_IDENTITY = "iPhone Distribution"`, `CODE_SIGN_STYLE = Manual`
- Set `PROVISIONING_PROFILE_SPECIFIER = "SceneView Demo App Store"`
- Updated version to 1.0 (build 355)

### 4. iOS archive + upload SUCCESS
- `xcodebuild archive` succeeded with manual signing
- Exported IPA (185 MB) with certificate hash `875C4D3762A8183AFA02C156003BB3BCB18BDBC7`
- Uploaded via `xcrun altool` with new API key — **UPLOAD SUCCEEDED**
- Build 355 v1.0 processed and ready on App Store Connect

### 5. App Store Connect API key
- Created new key "SceneView Upload" (ID: `8P64Z7HCSN`)
- Issuer ID: `551bbb3e-a7f4-4e2e-9486-bf487256fd0f`
- Saved to `~/.private_keys/AuthKey_8P64Z7HCSN.p8`
- Also saved to `.secrets/` (gitignored)

### 6. Signing files backup
- `.secrets/ios_distribution_2027.p12` — combined cert + key
- `.secrets/ios_distribution_2027.key` — private key
- `.secrets/AuthKey_8P64Z7HCSN.p8` — App Store Connect API key
- `.secrets/` added to `.gitignore`

## Previous sessions (6 and earlier)

- URL-based model loading (Android + iOS)
- iOS HDR environments (6 files)
- Progressive texture loading (Filament async)
- GitHub Releases CDN (25 models, `assets-v1` tag)
- Play Store APK 421→109 MB, deployed successfully
- GitHub Sponsors 4 tiers ($5/$25/$50/$100)
- Polar.sh active (3 products)
- Xcode 26.3 installed

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — App Store submissions
- **iOS**: v1.0 build 355 **en attente de vérification** (submitted, 24-48h review)
- **macOS**: Needs build. Options:
  - Enable "Designed for iPad" on Apple Silicon (in App Store Connect > Pricing & Availability)
  - OR create native macOS target in Xcode project
  - Also needs: screenshots (1280×800 or 2880×1800), description, keywords, support URL, copyright
- **tvOS**: Needs new Xcode target + build + screenshots + metadata
- **visionOS**: Needs new Xcode target + build + screenshots + metadata

### Priority 2 — Monitoring
- Check iOS review result
- Check Play Store status (deployed)

### Priority 3 — Polish
- Loading indicator for URL-based models in demo
- Environment picker UI in iOS demo

### Priority 4 — Signing maintenance
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
- **App Store macOS**: ⏳ Needs build + screenshots + metadata
- **App Store tvOS**: ⏳ Needs new Xcode target
- **App Store visionOS**: ⏳ Needs new Xcode target

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
