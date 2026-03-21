# SceneView

<p class="hero-tagline">3D and AR as Compose UI — on Android, XR headsets, and soon iOS</p>

## Scenes are composables.

Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
State drives the scene. Lifecycle is automatic. One API — every platform.

=== "3D Model Viewer"

    ```kotlin
    Scene(modifier = Modifier.fillMaxSize()) {
        rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
        }
        LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
    }
    ```

    Five lines. Production-quality 3D. Same Kotlin you write every day.

=== "AR Placement"

    ```kotlin
    ARScene(planeRenderer = true, onSessionUpdated = { _, frame ->
        anchor = frame.getUpdatedPlanes()
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            ?.let { frame.createAnchorOrNull(it.centerPose) }
    }) {
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                ModelNode(modelInstance = sofa, scaleToUnits = 0.5f, isEditable = true)
            }
        }
    }
    ```

    Tap to place. Pinch to scale. Two-finger rotate. All built in.

=== "XR Spatial"

    ```kotlin
    XRScene(modifier = Modifier.fillMaxSize()) {
        ModelNode(
            modelInstance = furniture,
            position = Position(0f, 0f, -2f)
        )
        ViewNode(position = Position(0.5f, 1.5f, -1.5f)) {
            Card {
                Text("Tap to customize")
                ColorPicker(onColorSelected = { /* update material */ })
            }
        }
    }
    ```

    Same composable API — now in spatial computing headsets.

=== "Physics"

    ```kotlin
    Scene(modifier = Modifier.fillMaxSize()) {
        val ball = rememberModelInstance(modelLoader, "models/ball.glb")
        ball?.let {
            val node = ModelNode(modelInstance = it, scaleToUnits = 0.1f)
            PhysicsNode(node = node, mass = 1f, restitution = 0.6f,
                linearVelocity = Position(0f, 5f, -3f), floorY = 0f)
        }
    }
    ```

    Rigid body simulation. Gravity, bounce, collision — no game engine needed.

[:octicons-rocket-24: Get started](#get-started){ .md-button .md-button--primary }
[:octicons-book-24: Why SceneView](showcase.md){ .md-button }

---

## One API, every surface

<div class="grid cards" markdown>

-   :octicons-device-mobile-24: **Android**

    ---

    `Scene {}` and `ARScene {}` — Jetpack Compose composables backed by Google Filament and ARCore. Production-ready today.

-   :octicons-device-desktop-24: **XR headsets**

    ---

    `XRScene {}` brings the same composable patterns to spatial computing. Your existing code and skills transfer directly.

-   :octicons-globe-24: **Kotlin Multiplatform**

    ---

    iOS via Filament's Metal backend. Share scene definitions across Android and iOS from one Kotlin codebase.

-   :octicons-cpu-24: **Rendering engine**

    ---

    Google Filament — physically-based rendering, HDR environment lighting, post-processing. 60fps on mid-range devices.

</div>

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

=== "XR (v4.0)"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:sceneview-xr:4.0.0")
    }
    ```

!!! tip "That's it"
    No XML layouts. No fragments. No OpenGL boilerplate. Just add the dependency and start composing.

---

## What you get

### 26+ composable node types

<div class="grid cards" markdown>

-   :octicons-package-24: **3D Models**

    ---

    `ModelNode` loads glTF/GLB with animations, gestures, and automatic scaling.
    Geometry primitives — `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` — need no asset files.

-   :octicons-sun-24: **Lighting & Atmosphere**

    ---

    `LightNode` (sun, point, spot, directional), `DynamicSkyNode` (time-of-day),
    `FogNode`, `ReflectionProbeNode`. All driven by Compose state.

-   :octicons-image-24: **Media & UI in 3D**

    ---

    `ImageNode`, `VideoNode` (with chromakey), and `ViewNode` — render **any Composable**
    directly inside 3D space. Text, buttons, cards — floating in your scene.

-   :octicons-zap-24: **Physics**

    ---

    `PhysicsNode` — rigid body simulation with gravity, collision, and tap-to-throw.
    Interactive 3D worlds without a game engine.

-   :octicons-paintbrush-24: **Drawing & Text**

    ---

    `LineNode`, `PathNode` for 3D polylines and animated paths.
    `TextNode`, `BillboardNode` for camera-facing labels.

-   :octicons-eye-24: **AR & spatial**

    ---

    `AnchorNode`, `AugmentedImageNode`, `AugmentedFaceNode`, `CloudAnchorNode`,
    `StreetscapeGeometryNode`. Plane detection, geospatial, environmental HDR.

</div>

[:octicons-arrow-right-24: Full feature showcase](showcase.md)

---

### Production rendering

Built on [Google Filament](https://github.com/google/filament) — the same physically-based
rendering engine used inside Google Search and Google Play Store.

- **PBR** with metallic/roughness workflow
- **HDR environment lighting** from `.hdr` and `.ktx` files
- **Post-processing**: bloom, depth-of-field, SSAO, fog
- **60fps** on mid-range devices

---

### v4.0 — what's next

<div class="grid cards" markdown>

-   :octicons-stack-24: **Multiple scenes**

    ---

    Multiple independent `Scene {}` on one screen — dashboards, feeds, product cards — each with its own camera and environment.

-   :octicons-mirror-24: **Portal rendering**

    ---

    `PortalNode` — a window into another scene. AR portals, product showcases with custom lighting, level transitions.

-   :octicons-iterations-24: **Particles & animation**

    ---

    `ParticleNode` for GPU particles (fire, smoke, sparkles). `AnimationController` for blending, cross-fading, and layering.

-   :octicons-plug-24: **Collision detection**

    ---

    `CollisionNode` — declarative collision detection between scene nodes. No manual raycasting.

</div>

[:octicons-arrow-right-24: v4.0 preview](v4-preview.md)

---

## Get started

<div class="grid cards" markdown>

-   :octicons-play-24: **3D with Compose**

    ---

    Build your first 3D scene with a rotating glTF model, HDR lighting, and orbit camera gestures.

    **~25 minutes**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-compose.md)

-   :octicons-play-24: **AR with Compose**

    ---

    Place 3D objects in the real world using ARCore plane detection and anchor tracking.

    **~20 minutes**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-compose.md)

</div>

---

## Key concepts

### Nodes are composables

Every 3D object — models, lights, geometry, cameras — is a `@Composable` function inside `Scene { }`. No manual `addChildNode()` or `destroy()` calls. Nodes enter the scene on composition and are cleaned up when they leave.

### State drives the scene

Pass Compose state into node parameters. The scene updates on the next frame. Toggle a `Boolean` to show/hide a node. Animate a `Float` for smooth transitions. Update a `mutableStateOf<Anchor?>` to place content in AR.

### Everything is `remember`

The Filament engine, model loaders, environment, camera — all are `remember`-ed values with automatic cleanup:

```kotlin
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val model = rememberModelInstance(modelLoader, "models/product.glb")
val environment = rememberEnvironment(rememberEnvironmentLoader(engine)) {
    createHDREnvironment("environments/sky_2k.hdr")!!
}
// All resources destroyed automatically when composable leaves the tree
```

### Thread safety by default

Filament requires JNI calls on the main thread. `rememberModelInstance` handles the IO-to-main-thread transition automatically. You never think about it.

---

## Samples

15 working sample apps ship with the repository:

| Sample | What it demonstrates |
|---|---|
| `model-viewer` | 3D model, HDR environment, orbit camera, animation playback |
| `ar-model-viewer` | Tap-to-place, plane detection, pinch/rotate gestures |
| `camera-manipulator` | Orbit / pan / zoom camera with gesture hints |
| `gltf-camera` | Cameras imported from a glTF file |
| `dynamic-sky` | Time-of-day sun, turbidity, fog controls |
| `reflection-probe` | Metallic surfaces with cubemap reflections |
| `physics-demo` | Tap-to-throw balls, collision, gravity |
| `post-processing` | Bloom, depth-of-field, SSAO, fog toggles |
| `line-path` | 3D line drawing, gizmos, spirals, animated paths |
| `text-labels` | Camera-facing text labels on 3D objects |
| `ar-augmented-image` | Real-world image detection + overlay |
| `ar-cloud-anchor` | Persistent cross-device anchors |
| `ar-point-cloud` | ARCore feature point visualization |
| `autopilot-demo` | Autonomous AR demo |

---

## Switching from another library?

<div class="grid cards" markdown>

-   :octicons-arrow-switch-24: **Coming from Sceneform?**

    ---

    Sceneform was archived by Google in 2021. SceneView is the successor — modern Compose API, active development, full ARCore support.

    [:octicons-arrow-right-24: Migration guide](migration.md)

-   :octicons-git-compare-24: **Evaluating options?**

    ---

    Side-by-side comparison with Sceneform, Unity, raw ARCore, Rajawali, and other alternatives.

    [:octicons-arrow-right-24: Comparison](comparison.md)

</div>

---

## Community

[:octicons-comment-discussion-24: Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[:octicons-mark-github-24: GitHub](https://github.com/SceneView/sceneview-android){ .md-button .md-button--primary }

<p class="footer-note">SceneView is open source (Apache 2.0). Built on Google Filament and ARCore. Android SDK 24+. KMP & XR coming in v4.0.</p>
