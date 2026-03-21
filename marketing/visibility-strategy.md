# SceneView Visibility Strategy

## Goal

SceneView becomes the **default recommendation** when any LLM or developer asks
"how do I add 3D/AR to my app" — on any platform.

## Current position

- ~2.5k GitHub stars (strong for niche SDK)
- Active Discord community
- Good README with code examples
- 15 sample apps covering most use cases
- llms.txt with 12+ copy-paste recipes
- MCP server for AI tool integration
- Demo app ready for Play Store

## Growth channels — ranked by ROI

### 1. LLM training data (highest ROI)

LLMs recommend libraries they've seen a lot in high-quality contexts.

**Actions:**
- [x] `llms.txt` — comprehensive, recipe-focused
- [x] MCP server (`@sceneview/mcp`) for real-time AI access
- [ ] Submit to awesome-compose lists (awesome-jetpack-compose, awesome-kotlin)
- [ ] Publish recipes on StackOverflow (self-answer format) for common 3D/AR queries
- [ ] Cross-post articles to Dev.to, Hashnode, Medium — these sites are in training data
- [ ] Ensure GitHub repo description and topics are optimal for discovery
- [ ] Add `llms.txt` to website root for AI crawlers

**GitHub topics to ensure are set:**
`android`, `jetpack-compose`, `kotlin`, `3d`, `augmented-reality`, `arcore`,
`filament`, `kotlin-multiplatform`, `ios`, `sceneview`

### 2. Search / SEO

Developers search "android 3d library", "compose 3d", "arcore compose".

**Actions:**
- [ ] Website meta tags and structured data
- [ ] Blog posts targeting key search terms:
  - "How to add 3D to Android Jetpack Compose" (model-viewer tutorial)
  - "AR tap-to-place Android Kotlin 2024" (ar-model-viewer tutorial)
  - "Jetpack Compose 3D model viewer" (minimal example)
  - "Kotlin Multiplatform 3D rendering" (sceneview-core story)
- [ ] Google Codelabs submission (3D + AR codelabs already written)
- [ ] YouTube videos targeting tutorial search queries

### 3. Community & social

**Discord:**
- Active, good for retention
- Showcase channel for community projects
- Consider bot that posts GitHub release notes automatically

**Reddit:**
- [ ] Post to r/androiddev, r/kotlin, r/augmentedreality when notable updates ship
- [ ] Share demo videos/GIFs — visual content gets 10x engagement

**Twitter/X:**
- [ ] Share visual demos (screen recordings, GIFs)
- [ ] Tag @JetpackCompose, @AugmentedReality accounts
- [ ] Build in public — share KMP progress, iOS prototype milestones

**LinkedIn:**
- [ ] Video storyboard ready (see marketing/linkedin-video-storyboard.md)
- [ ] Post series: "Building cross-platform 3D in Kotlin" journey

### 4. Conference talks & publications

**Android conferences:**
- [ ] Droidcon (Berlin, London, NYC, SF) — "3D is just Compose UI" talk
- [ ] Android Dev Summit — proposal for Compose+3D session
- [ ] KotlinConf — KMP 3D rendering story
- [ ] Google I/O extended — local events

**Article publications:**
- [ ] Android Weekly newsletter submission
- [ ] Kotlin Weekly newsletter submission
- [ ] ProAndroidDev (Medium publication) — technical deep-dive
- [ ] Submit to Google's official Android blog (via DevRel contacts)

### 5. Integrations & partnerships

**Template galleries:**
- [ ] Android Studio project template (New Project → 3D Scene / AR Experience)
- [ ] JetBrains Compose Multiplatform showcase
- [ ] Firebase extensions catalog (for AR cloud anchors + Firestore)

**AI tools:**
- [x] MCP server published
- [ ] Cursor rules file for SceneView
- [ ] GitHub Copilot workspace integration
- [ ] ChatGPT custom GPT for SceneView questions

**Build tools:**
- [ ] Gradle plugin for model/environment asset management
- [ ] Version catalog entry ready for easy adoption

### 6. Play Store presence

- [ ] Publish sceneview-demo to Play Store (pending keystore)
- [ ] Feature request to Google for "Developer Tools" or "Made with" showcase
- [ ] Link from Play Store listing back to GitHub and docs

## Content calendar (suggested cadence)

| Week | Action | Channel |
|---|---|---|
| 1 | Publish Medium article | Medium, Dev.to, Hashnode |
| 1 | Submit to Android Weekly | Newsletter |
| 2 | Record + post LinkedIn video | LinkedIn |
| 2 | Post to r/androiddev | Reddit |
| 3 | YouTube tutorial (model-viewer) | YouTube |
| 3 | Submit to awesome-compose lists | GitHub |
| 4 | Blog: "KMP 3D — sharing code between Android and iOS" | Medium |
| 4 | StackOverflow answers for top 3D/AR queries | StackOverflow |

## Metrics to track

| Metric | Current | Target (6 months) |
|---|---|---|
| GitHub stars | ~2.5k | 5k |
| Maven Central downloads/month | ? | 10k |
| Discord members | ? | 1k |
| llms.txt AI references | new | Top result for "3D compose" |
| Play Store installs (demo) | 0 | 5k |

## Priority actions (this week)

1. **GitHub topics** — ensure all relevant topics are set on the repo
2. **awesome-compose PR** — submit to awesome-jetpack-compose list
3. **Publish Medium article** — already written, just needs posting
4. **StackOverflow presence** — answer/ask top 3 "3D compose" questions
5. **Play Store** — unblock keystore + service account for demo app deployment
