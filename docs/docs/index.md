# SceneView for Android

<p class="hero-tagline">The #1 3D & AR library for Android — powered by Google Filament and ARCore</p>

## 3D is just Compose UI.

Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
State drives the scene. Lifecycle is automatic.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
    LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
}
```

Five lines. Production-quality 3D. Same Kotlin you write every day.

[:octicons-rocket-24: Get started](#get-started){ .md-button .md-button--primary }
[:octicons-book-24: Why SceneView](showcase.md){ .md-button }

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

!!! tip "That's it"
    No XML layouts. No fragments. No OpenGL boilerplate. Just add the dependency and start composing.

---

## What you get

### 26+ composable node types

<div class="grid cards" markdown>

-   :material-cube-outline: **3D Models**

    ---

    `ModelNode` loads glTF/GLB with animations, gestures, and automatic scaling.
    Geometry primitives — `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` — need no asset files.

-   :material-lightbulb-on: **Lighting & Atmosphere**

    ---

    `LightNode` (sun, point, spot, directional), `DynamicSkyNode` (time-of-day),
    `FogNode`, `ReflectionProbeNode`. All driven by Compose state.

-   :material-image: **Media & UI in 3D**

    ---

    `ImageNode`, `VideoNode` (with chromakey), and `ViewNode` — render **any Composable**
    directly inside 3D space. Text, buttons, cards — floating in your scene.

-   :material-atom: **Physics**

    ---

    `PhysicsNode` — rigid body simulation with gravity, collision, and tap-to-throw.
    Interactive 3D worlds without a game engine.

-   :material-draw: **Drawing & Text**

    ---

    `LineNode`, `PathNode` for 3D polylines and animated paths.
    `TextNode`, `BillboardNode` for camera-facing labels.

-   :material-augmented-reality: **Full ARCore**

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

### AR that's just Compose

`ARScene { }` wraps `Scene` with ARCore wired in. Same pattern — now in the real world.

```kotlin
ARScene(
    planeRenderer = true,
    onSessionUpdated = { _, frame ->
        anchor = frame.getUpdatedPlanes()
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            ?.let { frame.createAnchorOrNull(it.centerPose) }
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = sofa, scaleToUnits = 0.5f)
        }
    }
}
```

Plane detection, image tracking, face mesh, cloud anchors, geospatial API, streetscape geometry — all as composables.

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

-   :material-swap-horizontal: **Coming from Sceneform?**

    ---

    Sceneform was archived by Google in 2021. SceneView is the successor — modern Compose API, active development, full ARCore support.

    [:octicons-arrow-right-24: Migration guide](migration.md)

-   :material-scale-balance: **Evaluating options?**

    ---

    Side-by-side comparison with Sceneform, Unity, raw ARCore, Rajawali, and other alternatives.

    [:octicons-arrow-right-24: Comparison](comparison.md)

</div>

---

## What's next: v4.0

The next major release brings multi-scene support, portal rendering, and Android XR spatial computing.

[:octicons-arrow-right-24: v4.0 preview](v4-preview.md) · [:octicons-arrow-right-24: Full roadmap](https://github.com/SceneView/sceneview-android/blob/main/ROADMAP.md)

---

## Community

[:simple-discord: Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[:simple-github: GitHub](https://github.com/SceneView/sceneview-android){ .md-button .md-button--primary }

<p class="footer-note">SceneView is open source (Apache 2.0). Built on Google Filament 1.70 and ARCore 1.53. Android SDK 24+.</p>
