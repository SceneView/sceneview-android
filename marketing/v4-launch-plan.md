# SceneView 4.0 Launch Plan

Marketing plan for the v4.0 release — the biggest update since the Compose rewrite.

---

## Key messages

### Primary
> **"3D everywhere — on every platform."** SceneView 4.0 lets you put multiple 3D scenes anywhere in your Compose layout or SwiftUI view hierarchy. Same API, no limits, cross-platform.

### Secondary
> **"A window into another world."** PortalNode renders a scene inside a scene — AR portals, product showcases, game level transitions. Available on Android and iOS.

### Tertiary
> **"Your skills work in XR."** SceneView-XR brings the same composable API to Android XR spatial computing headsets — alongside visionOS support via RealityKit.

---

## Current state (pre-4.0)

v3.3.0 shipped the cross-platform foundation:
- **Android:** 26+ node types, Filament, ARCore — stable
- **iOS:** 16 node types, RealityKit, ARKit — alpha
- **macOS:** SceneViewSwift via SPM — alpha
- **visionOS:** SceneViewSwift via SPM — alpha
- **KMP core:** Math, collision, geometry, animation shared across platforms
- **MCP server:** AI-assisted development for both Android and iOS

v4.0 builds on this cross-platform foundation.

---

## Launch timeline

### T-4 weeks: Pre-announcement
- [ ] Teaser tweet: "Something big is coming to SceneView" + multi-scene screenshot (both platforms)
- [ ] Discord announcement: v4.0 beta branch available for testing
- [ ] Blog post: "The road to SceneView 4.0" — engineering deep-dive on multi-scene architecture
- [ ] iOS beta: update SceneViewSwift with v4.0 features

### T-2 weeks: Beta release
- [ ] Publish `4.0.0-beta01` to Maven Central
- [ ] Update SceneViewSwift SPM tag for iOS beta
- [ ] Update docs site with v4.0 API additions (keep v3.3.0 docs intact)
- [ ] Share beta with top contributors for feedback (Android and iOS)

### T-0: Launch day
- [ ] Publish `4.0.0` to Maven Central
- [ ] Tag SceneViewSwift `4.0.0` for SPM
- [ ] GitHub Release with full changelog (both platforms)
- [ ] X/Twitter thread (template below) — cross-platform angle
- [ ] LinkedIn post + video (template below) — show both platforms side by side
- [ ] Dev.to article (template below)
- [ ] Update docs site: home page, cheatsheet, architecture page (both platforms)
- [ ] Discord announcement
- [ ] Reddit: r/androiddev, r/iosprogramming, r/swift, r/augmentedreality, r/kotlin

### T+1 week: Amplification
- [ ] YouTube video: "SceneView 4.0 — Cross-Platform 3D Everywhere"
- [ ] Submit to Android Weekly newsletter
- [ ] Submit to iOS Dev Weekly newsletter
- [ ] Submit to Swift Weekly Brief
- [ ] Submit to Kotlin Weekly newsletter
- [ ] Reach out to Android GDEs and Apple Developer Advocates for coverage

---

## X/Twitter thread template

**Tweet 1 (hook):**
SceneView 4.0 is here.

Multiple 3D scenes on one screen.
Portals — scenes inside scenes.
Android XR + visionOS.
Cross-platform: Compose + SwiftUI.

Thread below.

**Tweet 2 (multi-scene):**
Put 3D in a LazyColumn. In a BottomSheet. In a ScrollView. In a List.

Each Scene has its own camera, lighting, and environment. They share resources. Zero overhead.

Works on Android (Compose) and iOS (SwiftUI).

**Tweet 3 (portals):**
PortalNode — render a scene inside a scene.

A door that opens to another world. A product configurator with custom lighting zones. A game level transition.

All declarative. Both platforms.

**Tweet 4 (XR):**
SceneView-XR — Android XR spatial computing.
SceneViewSwift — visionOS spatial computing.

Same concepts you know. Your 3D skills transfer to headsets on both ecosystems.

**Tweet 5 (CTA):**
Try it now:

Android:
implementation("io.github.sceneview:sceneview:4.0.0")

iOS:
.package(url: "https://github.com/SceneView/sceneview", from: "4.0.0")

Docs: sceneview.github.io
Star us: github.com/SceneView/sceneview

---

## LinkedIn post template

**Caption:**
We just shipped SceneView 4.0 — the biggest update to the #1 cross-platform 3D & AR SDK.

What's new:
-> Multiple 3D scenes on one screen (both platforms)
-> PortalNode — render a scene inside a scene
-> SceneView-XR for Android XR spatial computing
-> Deeper visionOS support via RealityKit
-> More iOS node parity — approaching feature-complete

This matters because 3D is no longer a special case on either platform. It's just another view in your layout.

Android: implementation("io.github.sceneview:sceneview:4.0.0")
iOS: .package(url: "...", from: "4.0.0")

#Android #iOS #JetpackCompose #SwiftUI #3D #AR #XR #visionOS #OpenSource #CrossPlatform

---

## Dev.to article outline

**Title:** SceneView 4.0: Cross-Platform 3D Everywhere — Multi-Scene, Portals, and Spatial Computing

**Sections:**
1. What is SceneView? (recap — now cross-platform)
2. What's new in 4.0
   - Multi-scene: 3D in LazyColumn/ScrollView (code for both platforms)
   - PortalNode: scenes inside scenes (code + use cases)
   - Android XR + visionOS: spatial computing on both ecosystems
3. Cross-platform status: Android (stable), iOS (beta in v4.0)
4. Migration from 3.3.0 (what changes, what stays the same — both platforms)
5. Benchmarks: multi-scene performance
6. What's next: Flutter/React Native bridges, ParticleNode
7. Getting started (both platforms)

**Tags:** android, ios, kotlin, swift, crossplatform

---

## Key metrics to track

| Metric | Source | Goal (launch week) |
|---|---|---|
| GitHub stars | GitHub API | +500 |
| Maven downloads | Maven Central | +2,000 |
| SPM installations | GitHub traffic | +500 |
| Discord members | Discord analytics | +200 |
| Dev.to views | Dev.to dashboard | 10,000 |
| X impressions | Twitter analytics | 50,000 |
| Docs page views | Google Analytics | 5,000 |

---

## Demo apps for launch content

### Multi-scene dashboard (both platforms)
A screen with multiple 3D elements:
- Product viewer (Scene #1) — 3D shoe with orbit camera
- Data globe (Scene #2) — rotating Earth with markers
- Side-by-side Android + iOS screenshots

### Portal walkthrough (Android)
A room with a door frame. Walk through the portal into a fantasy landscape.

### XR furniture configurator (Android XR + visionOS)
Place furniture in a room, customize materials via floating UI panels. Show the same experience on Android XR headset and Apple Vision Pro.
