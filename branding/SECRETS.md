# SceneView Secrets Inventory

> All secrets are stored as GitHub Repository Secrets on SceneView/sceneview.
> This document is a READ-ONLY inventory — never put actual secret values here.

## Status: ✅ = Configured | ❌ = Missing

### Android (Play Store)
| Secret | GitHub Name | Status | Notes |
|---|---|---|---|
| Upload keystore (base64) | `UPLOAD_KEYSTORE_BASE64` | ✅ | Created 2026-03-23 |
| Keystore password | `UPLOAD_KEYSTORE_PASSWORD` | ✅ | Created 2026-03-23 |
| Key alias | `UPLOAD_KEY_ALIAS` | ✅ | Created 2026-03-23 |
| Key password | `UPLOAD_KEY_PASSWORD` | ✅ | Created 2026-03-23 |
| Play Store service account | `PLAY_STORE_SERVICE_ACCOUNT_JSON` | ✅ | Created 2026-03-21 |

### Maven Central (Android library publish)
| Secret | GitHub Name | Status | Notes |
|---|---|---|---|
| Maven Central username | `MAVEN_CENTRAL_USERNAME` | ✅ | Created 2026-03-20 |
| Maven Central password | `MAVEN_CENTRAL_PASSWORD` | ✅ | Created 2026-03-20 |
| Sonatype username | `SONATYPE_NEXUS_USERNAME` | ✅ | Created 2026-03-19 |
| Sonatype password | `SONATYPE_NEXUS_PASSWORD` | ✅ | Created 2026-03-19 |
| Sonatype staging profile | `SONATYPE_STAGING_PROFILE_ID` | ✅ | Created 2026-03-20 |
| GPG signing key | `SIGNING_KEY` | ✅ | Created 2026-03-20 |
| GPG key ID | `SIGNING_KEY_ID` | ✅ | Created 2026-03-20 |
| GPG key password | `SIGNING_KEY_PASSWORD` | ✅ | Created 2026-03-20 |

### npm (Web library publish)
| Secret | GitHub Name | Status | Notes |
|---|---|---|---|
| npm token | `NPM_TOKEN` | ✅ | Created 2026-03-17 |

### GitHub
| Secret | GitHub Name | Status | Notes |
|---|---|---|---|
| Personal access token | `PERSONAL_TOKEN` | ✅ | Created 2026-03-17 |

### iOS (App Store)
| Secret | GitHub Name | Status | Notes |
|---|---|---|---|
| Build certificate (base64) | `IOS_BUILD_CERTIFICATE_BASE64` | ✅ | Created 2026-03-25, Apple Distribution, expires 2027-03-25 |
| Certificate password | `IOS_BUILD_CERTIFICATE_PASSWORD` | ✅ | Created 2026-03-25 |
| Provisioning profile (base64) | `IOS_PROVISIONING_PROFILE_BASE64` | ✅ | Created 2026-03-25, App Store type, io.github.sceneview.demo |
| App Store Connect API key | `APP_STORE_CONNECT_API_KEY` | ✅ | Created 2026-03-25, .p8 key, Gestionnaire d'apps role |
| App Store Connect issuer ID | `APP_STORE_CONNECT_ISSUER_ID` | ✅ | Created 2026-03-25 |
| App Store Connect key ID | `APP_STORE_CONNECT_KEY_ID` | ✅ | Created 2026-03-25, key C77W6AGSZT |

### Apple Developer Account
- **Team ID:** 5G3DZ3TH45
- **Team name:** Thomas Gorisse
- **Type:** Personne physique
- **Renewal:** 24 mars 2027
- **Bundle ID:** io.github.sceneview.demo

## How to add iOS secrets

1. **Apple Developer Portal** (developer.apple.com):
   - Certificates → Create → "Apple Distribution" → download .p12
   - `base64 -i certificate.p12 | pbcopy` → paste as `IOS_BUILD_CERTIFICATE_BASE64`
   - Password used when exporting → `IOS_BUILD_CERTIFICATE_PASSWORD`

2. **Provisioning Profile**:
   - Profiles → Create → "App Store" → select app ID → download .mobileprovision
   - `base64 -i profile.mobileprovision | pbcopy` → paste as `IOS_PROVISIONING_PROFILE_BASE64`

3. **App Store Connect API**:
   - appstoreconnect.apple.com → Users and Access → Keys → Generate
   - Download the .p8 key file
   - Key contents → `APP_STORE_CONNECT_API_KEY`
   - Issuer ID from the page → `APP_STORE_CONNECT_ISSUER_ID`
   - Key ID from the page → `APP_STORE_CONNECT_KEY_ID`

## Backup strategy

All secrets are in GitHub Repository Secrets (encrypted at rest).
For disaster recovery, consider also storing in:
- 1Password / Bitwarden vault (recommended)
- Google Drive encrypted archive (secondary)
