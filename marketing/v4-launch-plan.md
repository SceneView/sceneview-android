# SceneView 4.0 Launch Plan

Marketing plan for the v4.0 release — the biggest update since the Compose rewrite.

---

## Key messages

### Primary
> **"3D everywhere in your app."** SceneView 4.0 lets you put multiple 3D scenes anywhere in your Compose layout — LazyColumn, Pager, BottomSheet. Same API, no limits.

### Secondary
> **"A window into another world."** PortalNode renders a scene inside a scene — AR portals, product showcases, game level transitions.

### Tertiary
> **"Your Compose skills work in XR."** SceneView-XR brings the same composable API to Android XR spatial computing headsets.

---

## Launch timeline

### T-4 weeks: Pre-announcement
- [ ] Teaser tweet: "Something big is coming to SceneView" + multi-scene screenshot
- [ ] Discord announcement: v4.0 beta branch available for testing
- [ ] Blog post: "The road to SceneView 4.0" — engineering deep-dive on multi-scene architecture

### T-2 weeks: Beta release
- [ ] Publish `4.0.0-beta01` to Maven Central
- [ ] Update docs site with v4.0 API additions (keep v3.2 docs intact)
- [ ] Share beta with top contributors for feedback

### T-0: Launch day
- [ ] Publish `4.0.0` to Maven Central
- [ ] GitHub Release with full changelog
- [ ] X/Twitter thread (template below)
- [ ] LinkedIn post + video (template below)
- [ ] Dev.to article (template below)
- [ ] Update docs site: home page, cheatsheet, architecture page
- [ ] Discord announcement
- [ ] Reddit: r/androiddev, r/augmentedreality, r/kotlin

### T+1 week: Amplification
- [ ] YouTube video: "SceneView 4.0 — 3D everywhere in your Compose app"
- [ ] Submit to Android Weekly newsletter
- [ ] Submit to Kotlin Weekly newsletter
- [ ] Reach out to Android GDEs for coverage

---

## X/Twitter thread template

**Tweet 1 (hook):**
SceneView 4.0 is here.

Multiple 3D scenes on one screen.
Portals — scenes inside scenes.
Android XR support.
Same Compose API.

Thread below. 🧵

**Tweet 2 (multi-scene):**
Put 3D in a LazyColumn. In a BottomSheet. In a Pager.

Each Scene has its own camera, lighting, and environment. They share one Engine. Zero overhead.

```kotlin
Column {
    Scene(...) { ModelNode(...) }  // product hero
    Scene(...) { SphereNode(...) } // data globe
    LazyColumn { /* regular content */ }
}
```

**Tweet 3 (portals):**
PortalNode — render a scene inside a scene.

A door that opens to another world. A product configurator with custom lighting zones. A game level transition.

All declarative. All Compose.

**Tweet 4 (XR):**
SceneView-XR — the same API you know, now in spatial computing.

XRScene { } works exactly like Scene { }. Your 3D skills and code patterns transfer directly to Android XR headsets.

**Tweet 5 (CTA):**
Try it now:
```
implementation("io.github.sceneview:sceneview:4.0.0")
```

Docs: sceneview.github.io
Discord: discord.gg/UbNDDBTNqb
Star us: github.com/SceneView/sceneview

---

## LinkedIn post template

**Caption:**
We just shipped SceneView 4.0 — the biggest update to Android's #1 3D & AR library since the Compose rewrite.

What's new:
→ Multiple 3D scenes on one screen (LazyColumn, BottomSheet, Pager)
→ PortalNode — render a scene inside a scene
→ SceneView-XR for Android XR spatial computing
→ Kotlin Multiplatform proof of concept

This matters because 3D is no longer a special case. It's just another Compose composable. Put it anywhere. Mix it with anything.

Same API. Same Kotlin. Now limitless.

Docs → sceneview.github.io
Source → github.com/SceneView/sceneview

#Android #JetpackCompose #Kotlin #3D #AR #XR #OpenSource

---

## Dev.to article outline

**Title:** SceneView 4.0: Multiple 3D Scenes, Portals, and Android XR — All in Jetpack Compose

**Sections:**
1. What is SceneView? (recap for new readers)
2. What's new in 4.0
   - Multi-scene: 3D in LazyColumn (code example + diagram)
   - PortalNode: scenes inside scenes (code example + use cases)
   - SceneView-XR: same API in spatial computing (code example)
3. Migration from 3.2.0 (what changes, what stays the same)
4. Benchmarks: multi-scene performance, shared Engine efficiency
5. What's next: ParticleNode, AnimationController, KMP iOS
6. Getting started

**Tags:** android, kotlin, compose, 3d, augmentedreality

---

## Key metrics to track

| Metric | Source | Goal (launch week) |
|---|---|---|
| GitHub stars | GitHub API | +500 |
| Maven downloads | Maven Central | +2,000 |
| Discord members | Discord analytics | +200 |
| Dev.to views | Dev.to dashboard | 10,000 |
| X impressions | Twitter analytics | 50,000 |
| Docs page views | Google Analytics | 5,000 |

---

## Demo apps for launch content

### Multi-scene dashboard
A Compose screen with:
- Product viewer (Scene #1) — 3D shoe with orbit camera
- Data globe (Scene #2) — rotating Earth with markers
- AR preview (Scene #3) — small AR window

### Portal walkthrough
A room with a door frame. Walk through the portal into a fantasy landscape with different sky and fog.

### XR furniture configurator
Place furniture in a room, customize materials via floating Compose UI panels.
