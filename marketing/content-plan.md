# SceneView — Marketing Content Plan

## Goal
Grow developer adoption of SceneView 3.x by publishing authoritative, high-quality
content across the channels developers discover new libraries: blog posts, codelabs,
GitHub, social, and the project website.

---

## Target Audiences

| Audience | Pain Point | Message |
|---|---|---|
| Android dev (Compose) | "3D/AR is too hard" | SceneView makes it as simple as `Text()` |
| AR developer | "ARCore boilerplate is painful" | One line to tap-to-place a model |
| AI-assisted dev | "AI generates wrong 3D code" | SceneView's `llms.txt` makes AI generate correct code first try |
| Student / hobbyist | "Where do I start?" | Codelab: first AR app in 30 minutes |

---

## Content Calendar

### Month 1 — Foundation

| Week | Channel | Title | Status |
|---|---|---|---|
| 1 | Medium / dev.to | **Introducing SceneView 3.0 — 3D & AR in Jetpack Compose** | Draft ready |
| 1 | GitHub README | Refresh with GIF demos and quick-start | TODO |
| 2 | Google Codelabs | **Your First AR App with SceneView** (tap-to-place) | Draft ready |
| 3 | Medium / dev.to | **Building a 3D Model Viewer in 50 Lines of Compose** | Draft ready |
| 4 | LinkedIn | SceneView 3.0 video post (existing video asset) | Assets exist |

### Month 2 — Deep Dives

| Week | Channel | Title | Status |
|---|---|---|---|
| 5 | Medium / dev.to | **TextNode & BillboardNode — Adding Labels to Your AR Scene** | TODO |
| 6 | Google Codelabs | **AR Cloud Anchors — Shared AR Experiences** | TODO |
| 7 | Medium / dev.to | **PhysicsNode — Real-Time 3D Physics in 10 Lines** | TODO |
| 8 | Website | SceneView.io landing page launch | Draft ready |

### Month 3 — Ecosystem

| Week | Channel | Title | Status |
|---|---|---|---|
| 9 | Medium / dev.to | **SceneView + Claude: AI-First 3D Development** | TODO |
| 10 | Google Codelabs | **3D Product Viewer with SceneView** (e-commerce use case) | TODO |
| 11 | YouTube | SceneView demo video (existing assets + new recording) | TODO |
| 12 | KotlinConf / DroidCon | Talk proposal: "Declarative 3D: Bringing Compose to the Third Dimension" | TODO |

---

## Articles (ready to publish)

- `article-introducing-sceneview.md` — Main launch article, Medium/dev.to
- `article-model-viewer-50-lines.md` — Quick-win tutorial, Medium/dev.to

## Codelabs (ready to publish)

- `codelab-ar-tap-to-place/` — Full Google Codelab format, step-by-step

## Website

- `website-landing-page.md` — Hero copy, features section, getting started

---

## Social Templates

### Twitter/X post template
```
🚀 SceneView 3.x — 3D & AR in Jetpack Compose

Scene {
  ModelNode(rememberModelInstance(loader, "models/car.glb"))
}

That's it. No boilerplate. No lifecycle. Just Compose.

➡ github.com/SceneView/sceneview-android
#AndroidDev #JetpackCompose #AR
```

### LinkedIn article hook
> I've been building mobile AR for 5 years. The biggest barrier was always the code: 300 lines of boilerplate before you see anything on screen. SceneView 3.0 changes that — here's how we built it.

---

## SEO Keywords

- "3D Android Jetpack Compose"
- "ARCore Jetpack Compose"
- "AR app Android tutorial"
- "SceneView Android"
- "Filament Android Kotlin"
- "3D model viewer Android"
- "augmented reality Android library"

---

## Distribution Channels

| Channel | Effort | Reach | Priority |
|---|---|---|---|
| Medium (sceneview publication) | Low | Dev community | High |
| dev.to (cross-post) | Low (copy-paste) | Dev community | High |
| Google Codelabs | High | Android developers | High |
| GitHub README / wiki | Low | Active searchers | High |
| LinkedIn (Thomas' network) | Low | CTO / tech leads | Medium |
| AndroidWeekly newsletter | Zero (submit link) | 30k subscribers | High |
| Twitter/X | Low | Dev community | Medium |
| Reddit r/androiddev | Low | Grassroots | Medium |
| DroidCon / KotlinConf talk | High | Conference attendees | Long-term |
