# GitHub repository profile updates

Suggested updates for the SceneView GitHub repositories and organization.

---

## Repository description

**Short (visible in search results):**

> 3D and AR as Jetpack Compose composables. 26+ node types, physics, dynamic sky, fog — powered by Google Filament and ARCore.

**Topics/tags to add:**

`android`, `ios`, `kotlin`, `swift`, `jetpack-compose`, `swiftui`, `3d`, `ar`, `augmented-reality`, `arcore`, `arkit`, `filament`, `realitykit`, `sceneview`, `gltf`, `usdz`, `compose`, `physics`, `pbr-rendering`, `kotlin-multiplatform`

---

## Organization profile README (SceneView/.github)

If you have a `SceneView/.github` repo with a profile README, update it:

```markdown
# SceneView

The #1 3D & AR library for Android and iOS.

SceneView brings Google Filament and ARCore into Jetpack Compose on Android, and RealityKit and ARKit into SwiftUI on iOS.
Nodes are composables. State drives the scene. Lifecycle is automatic.

## Repositories

| Repository | What it is |
|---|---|
| [sceneview](https://github.com/SceneView/sceneview) | Core SDK — `Scene {}` and `ARScene {}` composables for Android |
| [sceneview.github.io](https://github.com/SceneView/sceneview.github.io) | Documentation website |

## Quick start

Add the dependency and start composing:

    implementation("io.github.sceneview:sceneview:3.3.0")

- [Documentation](https://sceneview.github.io)
- [Samples](https://github.com/SceneView/sceneview/tree/main/samples)
- [Discord](https://discord.gg/UbNDDBTNqb)
```

---

## Social preview image

Create a 1280x640 image for the GitHub social preview (Settings > Social preview):

**Layout:**
- Left half: SceneView logo + "3D & AR for Compose" tagline
- Right half: a screenshot of the model-viewer sample (helmet with HDR reflections)
- Bottom strip: `sceneview:3.3.0` · `26+ nodes` · `Filament` · `ARCore` · `Apache 2.0`
- Background: dark gradient (#1a1a2e to #16213e)
- Text: white, clean sans-serif

**Tools:** Figma, Canva, or any image editor. Export as PNG, 1280x640.

---

## GitHub Releases — template for future releases

When publishing a new release, use this format:

```markdown
## What's new

[1-3 sentence summary of the release theme]

### New nodes
- **`NodeName`** — one-line description

### Improvements
- Bullet points

### Dependencies
- Filament X.Y → **X.Z**
- Kotlin X.Y → **X.Z**

### Migration
[Link to migration section if breaking changes]

---

**Get started:**
```
implementation("io.github.sceneview:sceneview:X.Y.Z")
implementation("io.github.sceneview:arsceneview:X.Y.Z")
```

[Full changelog](https://github.com/SceneView/sceneview/blob/main/CHANGELOG.md) · [Documentation](https://sceneview.github.io)
```

---

## README.md badge row

Add these badges to the top of the main README:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview)](https://central.sonatype.com/artifact/io.github.sceneview/sceneview)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/DISCORD_ID?label=Discord&logo=discord)](https://discord.gg/UbNDDBTNqb)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat)
```
