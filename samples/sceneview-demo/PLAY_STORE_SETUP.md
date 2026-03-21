# Google Play Store Setup — SceneView Demo

Step-by-step guide to publish the SceneView Demo app on Google Play.

---

## 1. Prerequisites

- [ ] Google Play Developer account ($25 one-time fee) → [Register here](https://play.google.com/console/signup)
- [ ] Access to the SceneView GitHub repository with admin rights (for secrets)

---

## 2. Create the App on Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Click **"Create app"**
3. Fill in:
   - **App name:** `SceneView Demo`
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
4. Accept declarations and click **Create app**

---

## 3. Complete the Store Listing

The Play Store metadata is pre-configured in `play/listings/`:

```
play/
├── listings/
│   ├── en-US/
│   │   ├── title.txt
│   │   ├── short-description.txt
│   │   └── full-description.txt
│   └── fr-FR/
│       ├── title.txt
│       ├── short-description.txt
│       └── full-description.txt
└── release-notes/
    ├── en-US/
    │   └── default.txt
    └── fr-FR/
        └── default.txt
```

You still need to manually upload:
- **App icon:** 512×512 PNG
- **Feature graphic:** 1024×500 PNG
- **Screenshots:** At least 2 phone screenshots (16:9 or 9:16)

### Content Rating

1. Go to **Policy → App content → Content rating**
2. Start questionnaire → Category: **Utility / Productivity**
3. Answer all "No" (no violence, no user data collection, etc.)
4. Apply rating

### Privacy Policy

Since the app doesn't collect personal data, you can use a simple privacy policy hosted on GitHub Pages or link to the SceneView repository.

---

## 4. Generate a Signing Keystore

```bash
keytool -genkeypair \
  -alias sceneview-demo \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore sceneview-demo.keystore \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=SceneView, O=SceneView, L=Paris, C=FR"
```

**Important:** Keep this keystore file secure. You'll need it for every future release.

---

## 5. Configure Google Play App Signing

1. In Play Console → **Setup → App signing**
2. Choose **"Use Google-generated key"** (recommended)
3. Upload your keystore's **upload key certificate** when prompted

---

## 6. Set up a Service Account for CI/CD

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create or select a project linked to your Play Console
3. Navigate to **IAM & Admin → Service Accounts**
4. Click **Create Service Account**:
   - Name: `sceneview-play-publisher`
   - Role: none (we'll grant Play Console access instead)
5. Create a JSON key → Download it
6. In **Google Play Console → Setup → API access**:
   - Link the Google Cloud project
   - Grant the service account access with **"Release manager"** permission

---

## 7. Configure GitHub Secrets

Go to your GitHub repo → **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|--------|-------|
| `DEMO_KEYSTORE_BASE64` | Base64-encoded keystore: `base64 -i sceneview-demo.keystore` |
| `DEMO_KEYSTORE_PASSWORD` | Your keystore password |
| `DEMO_KEY_ALIAS` | `sceneview-demo` |
| `DEMO_KEY_PASSWORD` | Your key password |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Full content of the service account JSON key file |

---

## 8. First Release (Manual)

The first release **must** be uploaded manually:

1. Build the release AAB locally:
   ```bash
   export SCENEVIEW_DEMO_KEYSTORE_FILE=path/to/sceneview-demo.keystore
   export SCENEVIEW_DEMO_KEYSTORE_PASSWORD=YOUR_STORE_PASSWORD
   export SCENEVIEW_DEMO_KEY_ALIAS=sceneview-demo
   export SCENEVIEW_DEMO_KEY_PASSWORD=YOUR_KEY_PASSWORD
   ./gradlew :samples:sceneview-demo:bundleRelease
   ```
2. Find the AAB at `samples/sceneview-demo/build/outputs/bundle/release/`
3. In Play Console → **Production → Create new release**
4. Upload the AAB
5. Add release notes
6. **Review and roll out** → Start with internal testing first

---

## 9. Automated Releases (After First)

Once the first release is live, automated deployment works via GitHub Actions:

### Option A: Tag-based release
```bash
# Bump versionCode and versionName in build.gradle first
git tag demo-v1.1.0
git push origin demo-v1.1.0
```
This triggers the workflow and publishes to the **internal** track.

### Option B: Manual workflow dispatch
1. Go to **Actions → "Deploy Demo to Play Store"**
2. Click **Run workflow**
3. Select the track: `internal`, `alpha`, `beta`, or `production`

### Promotion Flow
Recommended promotion path:
```
internal → alpha → beta → production
```

Promote in Play Console: **Release → Testing → [track]** → Promote to next track.

---

## 10. Version Management

Before each release, update `samples/sceneview-demo/build.gradle`:

```groovy
defaultConfig {
    versionCode 2        // Increment for each release
    versionName "1.1.0"  // Semantic version
}
```

Update release notes in `play/release-notes/en-US/default.txt`.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Package name not found" | First release must be done manually |
| "APK/AAB not signed correctly" | Check keystore env vars and signing config |
| "Service account unauthorized" | Verify Play Console API access + permissions |
| "Version code already used" | Increment `versionCode` in build.gradle |
| Upload action fails with 403 | Service account needs "Release manager" role |
