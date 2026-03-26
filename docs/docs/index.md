---
title: SceneView — 3D & AR SDK for Android, iOS, Web, Desktop, and more
description: "The #1 open-source 3D & AR SDK. 9 platforms: Android, iOS, Web, Desktop, TV, Flutter, React Native. Jetpack Compose, SwiftUI, Filament.js."
---

<div class="sv-hero" markdown>

# SceneView

<p class="sv-tagline">3D and AR as declarative UI. Build immersive experiences with the tools you already know.</p>

<p class="sv-platforms">Android · iOS · Web · Desktop · TV · Flutter · React Native</p>

[Get Started](quickstart.md){ .md-button .md-button--primary }
[GitHub](https://github.com/sceneview/sceneview){ .md-button }

</div>

## Write 3D the same way you write UI

Nodes are composables. Lifecycle is automatic. State drives everything.

=== "Kotlin (Android)"

    ```kotlin
    Scene(modifier = Modifier.fillMaxSize()) {
        rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
        }
        LightNode(apply = { type(LightManager.Type.SUN) })
    }
    ```

=== "Swift (iOS)"

    ```swift
    SceneView {
        ModelNode("helmet.usdz", scaleToUnits: 1.0)
        LightNode(.directional, color: .white, intensity: 1000)
    }
    ```

---

## Features

<div class="sv-features" markdown>

<div class="sv-feature-card" markdown>

<span class="sv-card-icon">:material-cube-outline:</span>

### Model Viewer

Load glTF/GLB and USDZ models with PBR materials, HDR environment lighting, and automatic animations. Orbit camera with gesture controls built in.

</div>

<div class="sv-feature-card" markdown>

<span class="sv-card-icon">:material-cellphone-arrow-down:</span>

### Augmented Reality

Tap-to-place 3D objects on real-world surfaces. ARCore on Android, ARKit on iOS. Plane detection, image tracking, and anchor persistence.

</div>

<div class="sv-feature-card" markdown>

<span class="sv-card-icon">:material-code-tags:</span>

### Declarative API

Nodes are composables inside `Scene { }`. State drives the scene. No boilerplate, no manual lifecycle management.

</div>

<div class="sv-feature-card" markdown>

<span class="sv-card-icon">:material-cellphone-link:</span>

### Cross-Platform

Android uses Filament. Apple platforms use RealityKit. Shared logic via Kotlin Multiplatform. Native performance on every platform.

</div>

</div>

---

## Install

=== "Android — 3D only"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:sceneview:3.3.0")
    }
    ```

=== "Android — 3D + AR"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:arsceneview:3.3.0")
    }
    ```

=== "iOS / macOS / visionOS"

    ```swift
    // Package.swift
    dependencies: [
        .package(url: "https://github.com/sceneview/sceneview-swift.git", from: "3.3.0")
    ]
    ```

---

## AR is just as easy

=== "Kotlin (Android)"

    ```kotlin
    ARScene(
        modifier = Modifier.fillMaxSize(),
        onSessionUpdated = { session, frame -> }
    ) {
        val anchor = rememberAnchor()
        anchor?.let {
            AnchorNode(anchor = it) {
                rememberModelInstance(modelLoader, "models/chair.glb")?.let { instance ->
                    ModelNode(modelInstance = instance, scaleToUnits = 0.5f)
                }
            }
        }
    }
    ```

=== "Swift (iOS)"

    ```swift
    ARSceneView(
        planeDetection: .horizontal,
        onTapOnPlane: { position, arView in
            let model = try? await ModelNode.load("chair.usdz")
            model?.scaleToUnits(0.5)
            let anchor = AnchorNode.world(position: position)
            anchor.add(model!.entity)
            arView.scene.addAnchor(anchor.entity)
        }
    )
    .ignoresSafeArea()
    ```

---

## Codelabs

<div class="grid cards" markdown>

-   :material-cube-outline:{ .lg .middle } **3D with Compose**

    ---

    Build your first 3D scene with a rotating glTF model, HDR lighting, and orbit camera gestures.

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-compose.md)

-   :material-cellphone-arrow-down:{ .lg .middle } **AR with Compose**

    ---

    Place 3D objects in the real world using ARCore plane detection and anchor tracking.

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-compose.md)

-   :material-apple:{ .lg .middle } **3D with SwiftUI**

    ---

    Build a 3D model viewer on iOS/macOS using SceneViewSwift, RealityKit, and orbit camera.

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-swiftui.md)

-   :material-cellphone-wireless:{ .lg .middle } **AR with SwiftUI**

    ---

    Detect planes with ARKit and tap to place 3D objects in the real world on iOS.

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-swiftui.md)

</div>

---

## Key Concepts

### Nodes are composables

Every 3D object -- models, lights, geometry, cameras -- is a `@Composable` function inside `Scene { }`. No manual `addChildNode()` or `destroy()` calls.

### State drives the scene

Pass Compose state into node parameters. The scene updates on the next frame. Toggle a `Boolean` to show/hide a node. Update a `mutableStateOf<Anchor?>` to place content in AR.

### Everything is `remember`

The Filament engine, model loaders, environment, camera -- all are `remember`-ed values with automatic cleanup. Create them, use them, forget about them.

### Multi-platform, native renderers

Android uses Filament. Apple platforms (iOS, macOS, visionOS) use RealityKit. Shared logic (math, collision, geometry, animations) lives in `sceneview-core` via Kotlin Multiplatform. Each platform gets native performance and native tooling.

---

## Samples

[:octicons-arrow-right-24: Android samples](samples.md) | [:octicons-arrow-right-24: Apple samples](samples-ios.md)

---

## Upgrading from v2.x?

See the [Migration guide](migration.md) for a step-by-step walkthrough of every breaking change.

---

<div class="sv-community" markdown>

## Community

[Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[GitHub](https://github.com/sceneview/sceneview){ .md-button .md-button--primary }

</div>
