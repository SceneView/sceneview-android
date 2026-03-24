# Article Distribution Checklist

Use this checklist every time a new article is published. Complete every step in order —
cross-posting and social amplification should happen within 48 hours of the primary
publication to maximise algorithmic reach.

---

## Pre-publication (do once per article)

- [ ] Final proofread — spell check, all code snippets compile and match current library version
- [ ] All Gradle dependencies use the latest released version (`3.3.0` as of this writing)
- [ ] At least one code snippet is copy-paste runnable (not pseudocode)
- [ ] Hero image created (1200 × 630 px, text overlay with article title)
- [ ] Canonical URL decided (Medium is primary; dev.to cross-post points canonical → Medium URL)

---

## Step 1 — Publish on Medium

1. Go to [medium.com/new-story](https://medium.com/new-story)
2. Paste article content (Markdown renders correctly if using the import option)
3. Set the hero image
4. Add tags (max 5):
   - PhysicsNode article: `Android`, `JetpackCompose`, `Kotlin`, `Physics`, `MobileDevelopment`
   - TextNode article: `Android`, `AR`, `JetpackCompose`, `Kotlin`, `AugmentedReality`
   - Model Viewer article: `Android`, `JetpackCompose`, `3D`, `Kotlin`, `Tutorial`
   - Introducing article: `Android`, `JetpackCompose`, `AR`, `3D`, `Kotlin`
5. Submit to the **SceneView** Medium publication (if created) or **ProAndroidDev**
6. Schedule publish time: **Tuesday or Wednesday 9:00 AM CET** (best engagement window)
7. Copy the final canonical URL (e.g. `https://medium.com/@yourhandle/article-slug-...`)

---

## Step 2 — Cross-post on dev.to

1. Go to [dev.to/new](https://dev.to/new)
2. Paste the full article content
3. In the front matter, set `canonical_url:` to the Medium URL from Step 1
   ```
   ---
   title: "PhysicsNode — Real-Time 3D Physics in 10 Lines of Compose"
   published: true
   tags: android, jetpackcompose, kotlin, physics
   canonical_url: https://medium.com/@yourhandle/...
   cover_image: https://...
   ---
   ```
4. Add cover image (same 1200 × 630 hero)
5. Publish immediately (dev.to's algorithm favours freshness, no scheduling needed)

> Note: The `canonical_url` tells Google that Medium is the authoritative source.
> dev.to gets its own SEO value from the tag/feed pages, not the article itself.

---

## Step 3 — Submit to AndroidWeekly

URL: **[androidweekly.net/submit](https://androidweekly.net/submit)**

Fill in:
- **Article URL**: the Medium URL
- **Category**: Libraries & Code (for SDK articles) or Tutorials (for codelabs)
- **Description**: 1–2 sentence summary (copy from meta description)
- **Your name / Twitter**: for attribution

Submit on **Sunday evening** — the newsletter is compiled on Mondays for Thursday publication.
Each submission can only be in one issue; submit the most impactful article each week, not all at once.

---

## Step 4 — Submit to Kotlin Weekly

URL: Submit a GitHub issue at **[github.com/mbiamont/kotlin-weekly](https://github.com/mbiamont/kotlin-weekly)** → New Issue → Article submission template.

Fields: Title, URL, description, author. Use the same copy as AndroidWeekly.

---

## Step 5 — Post on Reddit

### r/androiddev (300k members)

Post type: **Link post** (not text post — links get more clicks)

Title template:
```
SceneView 3.2 — [one-line description of what the article shows]
```

Examples:
- `SceneView 3.2 — Real-time 3D physics in Jetpack Compose with 10 lines of Kotlin`
- `SceneView 3.2 — Camera-facing AR text labels with TextNode (no boilerplate)`
- `Building a 3D model viewer in 50 lines of Jetpack Compose`

Rules:
- Do **not** post the same URL twice in 60 days
- Stick around for 30 minutes after posting to reply to comments
- If asked "why not use library X?" — have a prepared comparison answer ready
- Flair: `Library` or `Tutorial`

### r/Kotlin (80k members)

Same link, adjusted title to emphasise the Kotlin angle:
```
Pure-Kotlin 3D physics simulation (Euler integration, no JNI) — SceneView 3.2
```

### r/augmentedreality (for AR articles only)

```
ARCore + Jetpack Compose: add text labels to any AR anchor in ~10 lines [SceneView 3.2]
```

---

## Step 6 — Post on Twitter / X

Character limit: 280. Use one post per article; add a reply thread for code snippets if the topic warrants it.

### Template — PhysicsNode article
```
Real-time 3D physics in Jetpack Compose. Gravity, bounce, floor collision — zero external engine.

Scene {
  PhysicsNode(node = ball, restitution = 0.65f, radius = 0.15f)
}

That's the whole simulation.

👉 [article link]

#AndroidDev #JetpackCompose #Kotlin
```

### Template — TextNode & BillboardNode article
```
Floating AR text labels in Jetpack Compose — one composable, always faces the camera.

TextNode(
  text = "Waypoint 1",
  cameraPositionProvider = { cameraPos }
)

Works inside ARScene { } on real-world anchors.

👉 [article link]

#AndroidDev #AR #ARCore #JetpackCompose
```

### Template — Model Viewer article
```
Production-quality 3D model viewer for Android.
HDR lighting, orbit camera, model switcher.
50 lines of Jetpack Compose.

(Yes, that's the whole app.)

👉 [article link]

#AndroidDev #JetpackCompose #3D #Kotlin
```

### Template — Introducing SceneView
```
3D and AR in Jetpack Compose shouldn't require 300 lines of boilerplate.

We rewrote SceneView from scratch.

Scene {
  ModelNode(rememberModelInstance(loader, "models/car.glb"))
}

That's a 3D scene. Here's what 3.2 looks like:

👉 [article link]

#AndroidDev #JetpackCompose #AR #OpenSource
```

Timing: post Tuesday–Thursday, 10:00–12:00 CET or 18:00–20:00 CET.

---

## Step 7 — Post on LinkedIn

LinkedIn rewards longer posts (150–300 words) over pure link shares. Use the hook-body-CTA structure.

### Template — PhysicsNode article

```
I've been building 3D Android apps for years.
The hardest part was never the rendering — it was the boilerplate around it.

Today we're showing something different: real-time physics in 10 lines of Jetpack Compose.
No Bullet. No Box2D. No JNI. Pure Kotlin.

Scene {
    PhysicsNode(
        node = ball,
        restitution = 0.65f,
        floorY = 0f,
        radius = 0.15f
    )
}

That's gravity, floor collision, and sleep detection.

Here's how it works (and how to add it to any 3D node in your app):
→ [article link]

#Android #JetpackCompose #MobileDevelopment #Kotlin #OpenSource
```

### Template — TextNode & BillboardNode article

```
AR labels are one of the most requested features from SceneView developers.

"How do I show a floating label above a model?"
"How do I add waypoint markers to AR anchors?"

In SceneView 3.2, it's a single composable:

TextNode(
    text = "Waypoint",
    cameraPositionProvider = { cameraPos }
)

Camera-facing, always readable, state-driven. Works in pure 3D scenes and ARCore.

Full article (with AR anchor labels + custom bitmap billboards):
→ [article link]

#Android #AR #JetpackCompose #ARCore #OpenSource
```

### Template — Introducing SceneView

```
I've been building mobile AR for 5 years.
The biggest barrier was always the code: 300 lines of boilerplate before you see anything on screen.

SceneView 3.0 changed that.

3D scene:
Scene { ModelNode(rememberModelInstance(loader, "models/car.glb")) }

AR tap-to-place:
ARScene(planeRenderer = true) { ... }

That's it. No lifecycle. No SurfaceView. No manual cleanup.

Here's the full story of why we rewrote it for Jetpack Compose:
→ [article link]

#Android #AR #JetpackCompose #MobileDevelopment #OpenSource
```

Timing: Tuesday–Thursday, 08:00–10:00 CET (before the workday starts in Europe).

---

## Step 8 — GitHub README update

After every significant new feature article:

- [ ] Update the **Samples** table in the README with the new sample
- [ ] Update the **What's New** section with a one-line summary and article link
- [ ] Bump the version badge if a new release was published

---

## Step 9 — Optional — Hacker News (Show HN)

Only do this for major releases or particularly novel technical content (e.g., the physics article or the AI-first design story). "Show HN" posts require something genuinely new.

Title format:
```
Show HN: SceneView – 3D/AR as Jetpack Compose composables (open source, MIT)
```

Rules:
- Post on a **weekday between 08:00–11:00 EST** for maximum visibility
- The post body must add context beyond the title — explain what makes it interesting to the HN audience (technical architecture, open-source angle, AI-first design)
- Monitor comments for the first 2 hours and reply to all technical questions

---

## Recommended hashtags by platform

### Twitter / X
Primary: `#AndroidDev #JetpackCompose #Kotlin`
Secondary by article type:
- Physics: `#GameDev #MobileDev`
- AR: `#AR #ARCore #AugmentedReality`
- 3D: `#3D #OpenGL #Filament`
- Release: `#OpenSource #Android`

### LinkedIn
`#Android #JetpackCompose #MobileDevelopment #Kotlin #OpenSource`
AR articles add: `#AugmentedReality #AR`

### dev.to tags (max 4)
- Physics: `android, jetpackcompose, kotlin, physics`
- AR labels: `android, ar, jetpackcompose, kotlin`
- Model viewer: `android, jetpackcompose, tutorial, kotlin`
- Introducing: `android, jetpackcompose, ar, opensource`

### Reddit (flair, not hashtags)
r/androiddev: Library / Tutorial
r/Kotlin: Library / Project

---

## Post-publication tracking

Track these metrics 7 days after publication:

| Metric | Target | Where to check |
|---|---|---|
| Medium reads | > 500 | Medium Stats |
| dev.to reactions | > 50 | dev.to dashboard |
| Reddit upvotes | > 100 on r/androiddev | Reddit post |
| GitHub stars delta | +20 per article | GitHub Insights |
| AndroidWeekly inclusion | 1 per month | Newsletter archive |
| Twitter impressions | > 5 000 | Twitter Analytics |

---

## Article backlog status

| Article | Medium published | dev.to | AndroidWeekly | Reddit | Twitter | LinkedIn |
|---|---|---|---|---|---|---|
| Introducing SceneView 3.0 | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| Model Viewer 50 Lines | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| PhysicsNode | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| TextNode & BillboardNode | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
| AR tap-to-place codelab | [ ] | [ ] | [ ] | [ ] | [ ] | [ ] |
