# Article Distribution Checklist

Use this checklist every time a new article is published. Complete every step in order —
cross-posting and social amplification should happen within 48 hours of the primary
publication to maximise algorithmic reach.

---

## Pre-publication (do once per article)

- [ ] Final proofread — spell check, all code snippets compile and match current library version
- [ ] All Gradle dependencies use the latest released version (`3.3.0` as of this writing)
- [ ] All SPM package references use `from: "3.3.0"`
- [ ] Include both Android and iOS code examples where applicable
- [ ] At least one code snippet is copy-paste runnable (not pseudocode)
- [ ] Hero image created (1200 x 630 px, text overlay with article title)
- [ ] Canonical URL decided (Medium is primary; dev.to cross-post points canonical to Medium URL)

---

## Step 1 — Publish on Medium

1. Go to [medium.com/new-story](https://medium.com/new-story)
2. Paste article content (Markdown renders correctly if using the import option)
3. Set the hero image
4. Add tags (max 5):
   - Cross-platform article: `Android`, `iOS`, `JetpackCompose`, `SwiftUI`, `3D`
   - Physics article: `Android`, `iOS`, `JetpackCompose`, `Physics`, `MobileDevelopment`
   - TextNode article: `Android`, `iOS`, `AR`, `JetpackCompose`, `AugmentedReality`
   - Model Viewer article: `Android`, `iOS`, `JetpackCompose`, `3D`, `Tutorial`
   - Introducing article: `Android`, `iOS`, `CrossPlatform`, `3D`, `MobileDevelopment`
5. Submit to the **SceneView** Medium publication (if created) or **ProAndroidDev**
6. Schedule publish time: **Tuesday or Wednesday 9:00 AM CET** (best engagement window)
7. Copy the final canonical URL

---

## Step 2 — Cross-post on dev.to

1. Go to [dev.to/new](https://dev.to/new)
2. Paste the full article content
3. In the front matter, set `canonical_url:` to the Medium URL from Step 1
   ```
   ---
   title: "SceneView 3.3.0 — Cross-Platform 3D & AR for Compose and SwiftUI"
   published: true
   tags: android, ios, kotlin, swift
   canonical_url: https://medium.com/@yourhandle/...
   cover_image: https://...
   ---
   ```
4. Add cover image (same 1200 x 630 hero)
5. Publish immediately (dev.to's algorithm favours freshness, no scheduling needed)

> Note: The `canonical_url` tells Google that Medium is the authoritative source.

---

## Step 3 — Submit to AndroidWeekly

URL: **[androidweekly.net/submit](https://androidweekly.net/submit)**

Fill in:
- **Article URL**: the Medium URL
- **Category**: Libraries & Code (for SDK articles) or Tutorials (for codelabs)
- **Description**: 1-2 sentence summary emphasising cross-platform angle
- **Your name / Twitter**: for attribution

Submit on **Sunday evening** — the newsletter is compiled on Mondays for Thursday publication.

---

## Step 4 — Submit to iOS Dev Weekly

URL: **[iosdevweekly.com/submit](https://iosdevweekly.com/submit)**

Fill in:
- **Article URL**: the Medium URL
- **Description**: Emphasise SwiftUI + RealityKit angle, 16 node types, SPM installation
- **Category**: Tools / Libraries

This is new for SceneView — leverage the iOS angle for a fresh audience.

---

## Step 5 — Submit to Kotlin Weekly

URL: Submit a GitHub issue at **[github.com/mbiamont/kotlin-weekly](https://github.com/mbiamont/kotlin-weekly)** — New Issue — Article submission template.

Fields: Title, URL, description, author. Mention KMP core and cross-platform architecture.

---

## Step 6 — Submit to Swift Weekly Brief

URL: **[swiftweeklybrief.com](https://swiftweeklybrief.com)**

Emphasise: SwiftUI-native, RealityKit-based, visionOS support, 16 node types.

---

## Step 7 — Post on Reddit

### r/androiddev (300k members)

Post type: **Link post**

Title template:
```
SceneView 3.3.0 — now cross-platform: Android (Compose) + iOS (SwiftUI) + macOS + visionOS
```

### r/iosprogramming (150k members) — NEW

Title:
```
SceneView for iOS — 16 SwiftUI node types for 3D & AR, built on RealityKit (open source, Apache 2.0)
```

### r/Kotlin (80k members)

Same link, KMP angle:
```
SceneView 3.3.0 — cross-platform 3D/AR SDK with KMP core (Android + Apple)
```

### r/swift (100k members) — NEW

Title:
```
SceneView — open-source 3D & AR for SwiftUI (RealityKit), now with 16 node types and visionOS support
```

### r/augmentedreality

```
Cross-platform AR: SceneView 3.3.0 supports ARCore (Android) + ARKit (iOS) with the same declarative API
```

Rules:
- Do **not** post the same URL twice in 60 days
- Stick around for 30 minutes after posting to reply to comments
- For iOS subreddits: lead with Swift/SwiftUI, mention Android as bonus

---

## Step 8 — Post on Twitter / X

Character limit: 280. Use the cross-platform comparison hook:

### Template — Cross-platform launch
```
SceneView 3.3.0 — now cross-platform.

Android (Compose):
Scene { ModelNode(helmet, scaleToUnits = 1.0f) }

iOS (SwiftUI):
SceneView { ModelNode(named: "helmet.usdz") }

Same concepts. Native renderers. 26+ Android nodes, 16 iOS nodes.

github.com/SceneView/sceneview

#AndroidDev #iOSDev #JetpackCompose #SwiftUI #3D #AR
```

Timing: post Tuesday-Thursday, 10:00-12:00 CET or 18:00-20:00 CET.

---

## Step 9 — Post on LinkedIn

LinkedIn rewards longer posts (150-300 words) over pure link shares. Use the cross-platform angle as the hook — it's a genuinely novel positioning in the 3D SDK space.

See `linkedin-post.md` for templates.

---

## Step 10 — GitHub README update

After every significant article:

- [ ] Update the **Samples** table in the README with any new samples
- [ ] Update the **What's New** section with cross-platform status
- [ ] Bump version badges if a new release was published
- [ ] Ensure iOS installation instructions are visible

---

## Step 11 — iOS-specific distribution (NEW)

- [ ] Post in Apple Developer Forums (AR/VR section)
- [ ] Submit to CocoaPods Trunk (if wrapper created)
- [ ] Tweet from iOS developer accounts / communities
- [ ] Post in relevant visionOS developer communities
- [ ] Submit to App Store Connect (when iOS sample app ready)

---

## Recommended hashtags by platform

### Twitter / X
Primary: `#AndroidDev #iOSDev #JetpackCompose #SwiftUI #CrossPlatform`
Secondary by article type:
- Physics: `#GameDev #MobileDev`
- AR: `#AR #ARCore #ARKit #AugmentedReality`
- 3D: `#3D #Filament #RealityKit`
- Release: `#OpenSource #Kotlin #Swift`

### LinkedIn
`#Android #iOS #JetpackCompose #SwiftUI #MobileDevelopment #CrossPlatform #OpenSource`
AR articles add: `#AugmentedReality #AR #ARKit #ARCore`

### dev.to tags (max 4)
- Cross-platform: `android, ios, kotlin, swift`
- Physics: `android, ios, jetpackcompose, physics`
- AR labels: `android, ios, ar, augmentedreality`
- Model viewer: `android, ios, tutorial, 3d`

### Reddit (flair, not hashtags)
r/androiddev: Library / Tutorial
r/iosprogramming: Library / Open Source
r/Kotlin: Library / Project
r/swift: Library / Open Source

---

## Post-publication tracking

Track these metrics 7 days after publication:

| Metric | Target | Where to check |
|---|---|---|
| Medium reads | > 500 | Medium Stats |
| dev.to reactions | > 50 | dev.to dashboard |
| Reddit upvotes (Android) | > 100 on r/androiddev | Reddit post |
| Reddit upvotes (iOS) | > 50 on r/iosprogramming | Reddit post |
| GitHub stars delta | +30 per article | GitHub Insights |
| AndroidWeekly inclusion | 1 per month | Newsletter archive |
| iOS Dev Weekly inclusion | 1 per quarter | Newsletter archive |
| Twitter impressions | > 5,000 | Twitter Analytics |
| SPM installations | Track via GitHub traffic | GitHub Insights |

---

## Article backlog status

| Article | Medium published | dev.to | AndroidWeekly | iOS Dev Weekly | Reddit (Android) | Reddit (iOS) | Twitter | LinkedIn |
|---|---|---|---|---|---|---|---|---|
| Introducing SceneView 3.3.0 (cross-platform) | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| Model Viewer 50 Lines (Android + iOS) | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| PhysicsNode (cross-platform) | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| TextNode & BillboardNode (cross-platform) | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| AR tap-to-place codelab (Android + iOS) | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
