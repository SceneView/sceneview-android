# GitHub repository profile updates

Suggested updates for the SceneView GitHub repositories and organization.

---

## Repository description

**Short (visible in search results):**

> Cross-platform 3D & AR SDK — Jetpack Compose (Android) + SwiftUI (iOS/macOS/visionOS). 26+ Android nodes, 16 iOS nodes. Filament + RealityKit. AI-first. Apache 2.0.

**Topics/tags to add:**

`android`, `ios`, `macos`, `visionos`, `kotlin`, `swift`, `jetpack-compose`, `swiftui`, `3d`, `ar`, `augmented-reality`, `arcore`, `arkit`, `filament`, `realitykit`, `sceneview`, `gltf`, `usdz`, `compose`, `physics`, `pbr-rendering`, `kotlin-multiplatform`, `cross-platform`

---

## Organization profile README (SceneView/.github)

If you have a `SceneView/.github` repo with a profile README, update it:

```markdown
# SceneView

The #1 cross-platform 3D & AR SDK for Android and Apple platforms.

SceneView brings Google Filament and ARCore into Jetpack Compose on Android, and RealityKit and ARKit into SwiftUI on iOS, macOS, and visionOS.
Nodes are declarative UI. State drives the scene. Lifecycle is automatic.

## Platforms

| Platform | UI Framework | Renderer | Status |
|---|---|---|---|
| Android | Jetpack Compose | Filament | Stable (v3.3.0) |
| iOS | SwiftUI | RealityKit | Alpha (v3.3.0) |
| macOS | SwiftUI | RealityKit | Alpha (v3.3.0) |
| visionOS | SwiftUI | RealityKit | Alpha (v3.3.0) |

## Quick start

**Android:**

    implementation("io.github.sceneview:sceneview:3.3.0")

**iOS / macOS / visionOS:**

    .package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")

- [Documentation](https://sceneview.github.io)
- [Samples](https://github.com/SceneView/sceneview/tree/main/samples)
- [Discord](https://discord.gg/UbNDDBTNqb)
```

---

## Social preview image

Create a 1280x640 image for the GitHub social preview (Settings > Social preview):

**Layout:**
- Left half: SceneView logo + "Cross-Platform 3D & AR" tagline
- Right half: side-by-side Android (Compose) + iOS (SwiftUI) code screenshots
- Bottom strip: `v3.3.0` · `Android + iOS + macOS + visionOS` · `Filament + RealityKit` · `Apache 2.0`
- Background: dark gradient (#1a1a2e to #16213e)
- Text: white, clean sans-serif

**Tools:** Figma, Canva, or any image editor. Export as PNG, 1280x640.

---

## GitHub Releases — template for future releases

When publishing a new release, use this format:

```markdown
## What's new

[1-3 sentence summary of the release theme]

### Platforms
- Android: [status]
- iOS/macOS/visionOS: [status]

### New nodes
- **`NodeName`** — one-line description (platforms: Android, iOS)

### Improvements
- Bullet points

### Dependencies
- Filament X.Y → **X.Z** (Android)
- RealityKit requirement: iOS X+ (Apple)
- Kotlin X.Y → **X.Z**

### Migration
[Link to migration section if breaking changes]

---

**Get started:**

Android:
```
implementation("io.github.sceneview:sceneview:X.Y.Z")
implementation("io.github.sceneview:arsceneview:X.Y.Z")
```

iOS / macOS / visionOS:
```
.package(url: "https://github.com/SceneView/sceneview", from: "X.Y.Z")
```

[Full changelog](https://github.com/SceneView/sceneview/blob/main/CHANGELOG.md) · [Documentation](https://sceneview.github.io)
```

---

## README.md badge row

Add these badges to the top of the main README:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview)](https://central.sonatype.com/artifact/io.github.sceneview/sceneview)
[![Swift Package Manager](https://img.shields.io/badge/SPM-compatible-brightgreen)](https://github.com/SceneView/sceneview)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/DISCORD_ID?label=Discord&logo=discord)](https://discord.gg/UbNDDBTNqb)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat)
[![iOS](https://img.shields.io/badge/iOS-17%2B-blue.svg)](https://developer.apple.com/ios/)
[![visionOS](https://img.shields.io/badge/visionOS-1%2B-purple.svg)](https://developer.apple.com/visionos/)
```
