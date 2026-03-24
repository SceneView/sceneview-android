---
title: SceneView — 3D & AR SDK for Android, iOS, macOS, visionOS
description: "The #1 open-source 3D & AR SDK. Jetpack Compose on Android, SwiftUI on iOS. Drop-in Scene{} and ARScene{} composables for model viewing, AR placement, physics, and immersive experiences."
---

<div class="sv-hero" markdown>

# SceneView

<p class="sv-tagline">3D and AR as declarative UI — Android, iOS, macOS, and visionOS. Build immersive experiences with the tools you already know.</p>

<div class="sv-stats" markdown>
<span class="sv-stat">Jetpack Compose</span>
<span class="sv-stat">SwiftUI</span>
<span class="sv-stat">Filament</span>
<span class="sv-stat">RealityKit</span>
<span class="sv-stat">ARCore / ARKit</span>
<span class="sv-stat">Kotlin Multiplatform</span>
</div>

[Get started](codelabs/codelab-3d-compose.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/SceneView/sceneview){ .md-button }

</div>

<div class="sv-logo-ticker" aria-label="Supported platforms and technologies">
<div class="sv-logo-ticker__track">
<div class="sv-logo-ticker__item"><img src="assets/images/logo-android.svg" alt="Android" loading="lazy"><span>Android</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-ios.svg" alt="iOS" loading="lazy"><span>iOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-macos.svg" alt="macOS" loading="lazy"><span>macOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-visionos.svg" alt="visionOS" loading="lazy"><span>visionOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-compose.svg" alt="Jetpack Compose" loading="lazy"><span>Compose</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-swiftui.svg" alt="SwiftUI" loading="lazy"><span>SwiftUI</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-filament.svg" alt="Filament" loading="lazy"><span>Filament</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-realitykit.svg" alt="RealityKit" loading="lazy"><span>RealityKit</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-arcore.svg" alt="ARCore" loading="lazy"><span>ARCore</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-arkit.svg" alt="ARKit" loading="lazy"><span>ARKit</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-kotlin.svg" alt="Kotlin" loading="lazy"><span>Kotlin</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-swift.svg" alt="Swift" loading="lazy"><span>Swift</span></div>
<!-- Duplicate set for seamless loop -->
<div class="sv-logo-ticker__item"><img src="assets/images/logo-android.svg" alt="Android" loading="lazy"><span>Android</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-ios.svg" alt="iOS" loading="lazy"><span>iOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-macos.svg" alt="macOS" loading="lazy"><span>macOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-visionos.svg" alt="visionOS" loading="lazy"><span>visionOS</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-compose.svg" alt="Jetpack Compose" loading="lazy"><span>Compose</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-swiftui.svg" alt="SwiftUI" loading="lazy"><span>SwiftUI</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-filament.svg" alt="Filament" loading="lazy"><span>Filament</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-realitykit.svg" alt="RealityKit" loading="lazy"><span>RealityKit</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-arcore.svg" alt="ARCore" loading="lazy"><span>ARCore</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-arkit.svg" alt="ARKit" loading="lazy"><span>ARKit</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-kotlin.svg" alt="Kotlin" loading="lazy"><span>Kotlin</span></div>
<div class="sv-logo-ticker__item"><img src="assets/images/logo-swift.svg" alt="Swift" loading="lazy"><span>Swift</span></div>
</div>
</div>

## Write 3D the same way you write UI

Nodes are composables. Lifecycle is automatic. State drives everything.
No boilerplate — just `Scene { }` like you'd write `Column { }`.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    // Load a glTF model — returns null while loading, handles lifecycle
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
    // Add lighting
    LightNode(apply = { type(LightManager.Type.SUN) })
}
```

---

## Features

<div class="sv-features" markdown>

<div class="sv-feature-card" markdown>
<img src="screenshots/model-viewer.png" alt="3D Model Viewer">
<div class="sv-card-body" markdown>

### Model Viewer

Load and display glTF/GLB models with PBR materials, HDR environment lighting, and automatic animations. Orbit camera with gesture controls built-in.

</div>
</div>

<div class="sv-feature-card" markdown>
<img src="screenshots/ar-model-viewer.png" alt="AR Model Viewer">
<div class="sv-card-body" markdown>

### Augmented Reality

Tap-to-place 3D objects on real-world surfaces. Full ARCore integration with plane detection, image tracking, and anchor persistence.

</div>
</div>

<div class="sv-feature-card" markdown>
<img src="screenshots/camera-manipulator.png" alt="Camera Controls">
<div class="sv-card-body" markdown>

### Camera Controls

Built-in orbit, pan, and zoom camera with smooth damping. Import camera animations from glTF files or control programmatically.

</div>
</div>

<div class="sv-feature-card" markdown>
<img src="screenshots/ar-augmented-image.png" alt="Image Tracking">
<div class="sv-card-body" markdown>

### Image Tracking

Detect real-world images and overlay 3D content. Track multiple images simultaneously with ARCore's augmented image database.

</div>
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
        .package(url: "https://github.com/SceneView/SceneViewSwift.git", from: "3.3.0")
    ]
    ```

---

## AR is just as easy

=== "Android (Compose)"

    ```kotlin
    ARScene(
        modifier = Modifier.fillMaxSize(),
        onSessionUpdated = { session, frame ->
            // Access ARCore frame data
        }
    ) {
        // Place a model when the user taps a detected plane
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

=== "iOS (SwiftUI)"

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

-   **3D with Compose**

    ---

    Build your first 3D scene with a rotating glTF model, HDR lighting, and orbit camera gestures.

    ~25 minutes

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-compose.md)

-   **AR with Compose**

    ---

    Place 3D objects in the real world using ARCore plane detection and anchor tracking.

    ~20 minutes

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-compose.md)

-   **3D with SwiftUI**

    ---

    Build a 3D model viewer on iOS/macOS using SceneViewSwift, RealityKit, and orbit camera.

    ~15 minutes

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-swiftui.md)

-   **AR with SwiftUI**

    ---

    Detect planes with ARKit and tap to place 3D objects in the real world on iOS.

    ~15 minutes

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-swiftui.md)

</div>

---

## Samples

<div class="sv-gallery" markdown>

![3D Model Viewer](screenshots/model-viewer.png)
![AR Model Viewer](screenshots/ar-model-viewer.png)
![Camera Manipulator](screenshots/camera-manipulator.png)
![glTF Camera](screenshots/gltf-camera.png)
![AR Point Cloud](screenshots/ar-point-cloud.png)
![Autopilot Demo](screenshots/autopilot-demo.png)

</div>

[:octicons-arrow-right-24: All Android samples](samples.md) | [:octicons-arrow-right-24: Apple samples](samples-ios.md)

---

## Key concepts

### Nodes are composables

Every 3D object — models, lights, geometry, cameras — is a `@Composable` function inside `Scene { }`. No manual `addChildNode()` or `destroy()` calls.

### State drives the scene

Pass Compose state into node parameters. The scene updates on the next frame. Toggle a `Boolean` to show/hide a node. Update a `mutableStateOf<Anchor?>` to place content in AR.

### Everything is `remember`

The Filament engine, model loaders, environment, camera — all are `remember`-ed values with automatic cleanup. Create them, use them, forget about them.

### Multi-platform, native renderers

Android uses Filament. Apple platforms (iOS, macOS, visionOS) use RealityKit. Shared
logic (math, collision, geometry, animations) lives in `sceneview-core` via Kotlin Multiplatform.
Each platform gets native performance and native tooling — no compromises.

---

## Upgrading from v2.x?

See the [Migration guide](migration.md) for a step-by-step walkthrough of every breaking change.

---

## Community

[Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[GitHub](https://github.com/SceneView/sceneview){ .md-button .md-button--primary }
