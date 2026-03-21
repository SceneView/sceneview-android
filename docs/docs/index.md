# SceneView — 3D and AR with Compose

<div class="hero-section" markdown>

![Hero Banner](assets/images/hero-banner.svg)

</div>

<div class="hero-tagline" markdown>

## 3D is just Compose UI.

SceneView brings the full power of **Google Filament** and **ARCore** into **Jetpack Compose**.
Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
Lifecycle is automatic. State drives everything.

[:octicons-rocket-24: Get started](#install){ .md-button .md-button--primary }
[:octicons-eye-24: Showcase](showcase.md){ .md-button }

</div>

---

## Visual showcase

See what developers are building with SceneView — 3D scenes, AR experiences, and interactive demos, all in pure Compose.

<div class="showcase-grid" markdown>

<div class="showcase-card" markdown>
![3D Model Viewer](assets/images/showcase-3d-model-viewer.svg)

**3D Model Viewer**

Photorealistic glTF rendering with HDR environment, orbit camera, and pinch-to-zoom gestures.

[:octicons-code-24: Source](https://github.com/SceneView/sceneview-android/tree/main/samples/model-viewer){ .showcase-link }
</div>

<div class="showcase-card" markdown>
![AR Model Viewer](assets/images/showcase-ar-model-viewer.svg)

**AR Tap-to-Place**

Detect real surfaces, tap to place 3D models, pinch to scale, drag to rotate — all with ARCore plane detection.

[:octicons-code-24: Source](https://github.com/SceneView/sceneview-android/tree/main/samples/ar-model-viewer){ .showcase-link }
</div>

<div class="showcase-card" markdown>
![Augmented Image](assets/images/showcase-ar-augmented-image.svg)

**Augmented Image**

Point your camera at a real-world image and overlay interactive 3D content — product previews, AR catalogs, educational models.

[:octicons-code-24: Source](https://github.com/SceneView/sceneview-android/tree/main/samples/ar-augmented-image){ .showcase-link }
</div>

<div class="showcase-card" markdown>
![Autopilot Demo](assets/images/showcase-autopilot.svg)

**Autopilot HUD**

Full autonomous driving interface built entirely with SceneView geometry nodes and Compose UI — no model files needed.

[:octicons-code-24: Source](https://github.com/SceneView/sceneview-android/tree/main/samples/autopilot-demo){ .showcase-link }
</div>

</div>

[:octicons-arrow-right-24: See all samples in the Showcase](showcase.md){ .md-button }

---

## Install

=== "3D only"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:sceneview:3.2.0")
    }
    ```

=== "3D + AR"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:arsceneview:3.2.0")
    }
    ```

---

## Quick start

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    Scene(modifier = Modifier.fillMaxSize()) {
        rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
        }
        LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000f) })
    }
}
```

That's it. No engine lifecycle callbacks. No `addChildNode()` or `destroy()` calls. The Compose runtime handles all of it.

---

## Get started

<div class="grid cards" markdown>

-   :material-cube-outline: **3D with Compose**

    ---

    Build your first 3D scene with a rotating glTF model, HDR lighting, and orbit camera gestures.

    **~25 minutes · Beginner**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-compose.md)

-   :material-augmented-reality: **AR with Compose**

    ---

    Place 3D objects in the real world using ARCore plane detection and anchor tracking.

    **~20 minutes · Beginner**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-compose.md)

</div>

---

## Key concepts

### Nodes are composables

Every 3D object — models, lights, geometry, cameras — is a `@Composable` function inside `Scene { }`. No manual `addChildNode()` or `destroy()` calls.

### State drives the scene

Pass Compose state into node parameters. The scene updates on the next frame. Toggle a `Boolean` to show/hide a node. Update a `mutableStateOf<Anchor?>` to place content in AR.

### Everything is `remember`

The Filament engine, model loaders, environment, camera — all are `remember`-ed values with automatic cleanup. Create them, use them, forget about them.

---

## Coming next

<div class="grid cards" markdown>

-   :material-cellphone-link: **Kotlin Multiplatform (iOS)**

    ---

    SceneView is exploring **KMP support** via Filament's Metal backend. Same Compose DSL, running natively on iOS.

    **Roadmap: v4.0**

    [:octicons-arrow-right-24: See the full roadmap](https://github.com/SceneView/sceneview-android/blob/main/ROADMAP.md)

-   :material-virtual-reality: **Android XR**

    ---

    Spatial computing support with a dedicated **SceneView-XR** module for Android XR headsets and passthrough AR.

    **Roadmap: v4.0**

    [:octicons-arrow-right-24: See the full roadmap](https://github.com/SceneView/sceneview-android/blob/main/ROADMAP.md)

</div>

---

## Upgrading from v2.x?

See the [Migration guide](migration.md) for a step-by-step walkthrough of every breaking change.

---

## Community

[:simple-discord: Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[:simple-github: GitHub](https://github.com/SceneView/sceneview-android){ .md-button .md-button--primary }
