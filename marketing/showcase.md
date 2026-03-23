# SceneView — The #1 3D & AR Library for Android

*Why thousands of Android developers chose SceneView — and why you should too.*

---

## The pitch in 10 seconds

SceneView makes 3D and AR work **exactly like Jetpack Compose UI**. Nodes are composables.
State drives the scene. Lifecycle is automatic. No boilerplate, no manual cleanup, no learning
a separate rendering paradigm.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f, autoAnimate = true)
    LightNode(type = LightManager.Type.SUN, apply = { intensity(100_000.0f) })
}
```

That's a production-quality 3D viewer. Five lines. Same Kotlin you write every day.

---

## What you get

### 26+ node types — all composable

| Category | Nodes |
|---|---|
| **Models** | `ModelNode` — glTF/GLB with animations, gestures, scaling |
| **Geometry** | `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` — no asset files needed |
| **Lighting** | `LightNode` (sun, point, spot, directional), `DynamicSkyNode`, `ReflectionProbeNode` |
| **Atmosphere** | `FogNode` — distance/height fog driven by Compose state |
| **Media** | `ImageNode`, `VideoNode` (with chromakey), `ViewNode` (any Composable in 3D) |
| **Text** | `TextNode`, `BillboardNode` — camera-facing labels and UI callouts |
| **Drawing** | `LineNode`, `PathNode` — 3D polylines, measurements, animated paths |
| **Physics** | `PhysicsNode` — rigid body simulation, collision, gravity |
| **AR** | `AnchorNode`, `HitResultNode`, `AugmentedImageNode`, `AugmentedFaceNode`, `CloudAnchorNode`, `StreetscapeGeometryNode` |
| **Structure** | `Node` (grouping/pivots), `CameraNode`, `MeshNode` |

Every one of these is a `@Composable` function. They enter the scene on composition,
update when state changes, and destroy themselves when they leave. Zero imperative code.

---

### Production rendering — Google Filament

SceneView is built on [Filament](https://github.com/google/filament), the same physically-based
rendering engine used inside Google's own apps (Google Search 3D viewer, Google Play Store).

- Physically-based rendering (PBR) with metallic/roughness workflow
- HDR environment lighting (IBL) from `.hdr` and `.ktx` files
- Dynamic shadows, reflections, ambient occlusion
- Post-processing: bloom, depth-of-field, SSAO, fog
- 60fps on mid-range devices — Filament is optimized for mobile

You get AAA-quality rendering without touching OpenGL, Vulkan, or shader code.

---

### Full ARCore integration

`ARScene` wraps `Scene` with ARCore wired in. Same composable pattern, now in the real world:

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

**AR features included:**
- Plane detection (horizontal + vertical) with persistent mesh rendering
- Image detection and tracking (`AugmentedImageNode`)
- Face mesh tracking and augmentation (`AugmentedFaceNode`)
- Cloud anchors for cross-device persistence (`CloudAnchorNode`)
- Environmental HDR — real-world light estimation
- Streetscape geometry — city-scale 3D building meshes
- Geospatial API support — place content at lat/long coordinates

---

## What makes SceneView different

### 1. It's Compose — not a wrapper around something else

60% of the top 1,000 Play Store apps use Jetpack Compose. It's the standard. Other 3D libraries
give you a `SurfaceView` to embed in your layout and an imperative API to manage the scene graph.
SceneView's scene graph **is** the Compose tree. The Compose runtime owns it.

This means:
- `if/else` controls whether nodes exist
- `State<T>` drives animations, positions, visibility
- `LaunchedEffect` and `DisposableEffect` work inside scenes
- Nesting nodes is the same as nesting `Column { Row { Text() } }`

### 2. Zero boilerplate lifecycle

```kotlin
// This is ALL the setup you need
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val environment = rememberEnvironment(rememberEnvironmentLoader(engine)) {
    createHDREnvironment("environments/sky_2k.hdr")
        ?: createEnvironment(environmentLoader)
}
```

Every resource is `remember`-ed. Created once, cleaned up when the composable leaves.
No `onPause`/`onResume` dance. No `destroy()` calls. No leaked Filament objects.

### 3. Thread safety by default

Filament requires all JNI calls on the main thread. `rememberModelInstance` handles the
IO-to-main-thread transition automatically. You never think about it.

### 4. Gesture handling built in

`ModelNode` supports pinch-to-scale, drag-to-rotate, and two-finger-rotate out of the box.
`CameraManipulator` gives you orbit/pan/zoom with one line:

```kotlin
Scene(cameraManipulator = rememberCameraManipulator()) { ... }
```

### 5. AI-assisted development

SceneView ships with an MCP server (`@sceneview/mcp`) and a machine-readable `llms.txt` API
reference. Claude, Cursor, and other AI tools always have the current API — no hallucinated
methods, no outdated patterns.

---

## Real-world use cases

### E-commerce: product viewer in 10 lines

Replace a static `Image()` with a `Scene {}` on your product detail page. The customer orbits
the product with one finger. No separate "3D viewer" screen. No SDK integration project.

```kotlin
// Before: static image
Image(painter = painterResource(R.drawable.shoe), contentDescription = "Shoe")

// After: interactive 3D viewer
Scene(
    modifier = Modifier.fillMaxWidth().height(300.dp),
    cameraManipulator = rememberCameraManipulator()
) {
    rememberModelInstance(modelLoader, "models/shoe.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f)
    }
}
```

### Furniture & interior design: AR placement

Let customers see how a sofa looks in their living room. Tap to place, pinch to resize,
rotate with two fingers. Compose UI floats alongside in 3D space via `ViewNode`.

### Education & training

Interactive 3D anatomy models, molecular structures, mechanical assemblies — all controlled
by standard Compose sliders, buttons, and state. Students manipulate, not just watch.

### Gaming & entertainment

`PhysicsNode` provides rigid body simulation. Tap-to-throw, collision detection, gravity.
Combined with `DynamicSkyNode` for time-of-day lighting and `FogNode` for atmosphere.

### Data visualization

3D bar charts, globes, network graphs. The data is Compose `State` — update the state and
the 3D visualization reacts instantly. No manual scene graph manipulation.

### Social & communication

`AugmentedFaceNode` for face filters and effects. Apply materials to the face mesh, attach
3D objects to landmarks. Front-camera AR, no separate SDK.

---

## The numbers

| Metric | Value |
|---|---|
| **Node types** | 26+ composable nodes |
| **Rendering** | Google Filament 1.70 — physically-based, 60fps mobile |
| **AR backend** | ARCore 1.53 — latest features |
| **Min SDK** | 24 (Android 7.0) |
| **Setup** | 1 Gradle line, 0 XML |
| **Model viewer** | ~5 lines of Kotlin |
| **AR placement** | ~15 lines of Kotlin |
| **License** | Apache 2.0 — use it anywhere |

---

## Get started in 60 seconds

**Step 1:** Add the dependency

```gradle
// 3D only
implementation("io.github.sceneview:sceneview:3.2.0")

// 3D + AR
implementation("io.github.sceneview:arsceneview:3.2.0")
```

**Step 2:** Drop a scene into any composable

```kotlin
@Composable
fun ProductViewer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/product.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        model?.let {
            ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
        }
    }
}
```

**Step 3:** Ship it.

No XML. No fragments. No lifecycle callbacks. No OpenGL boilerplate. Just Compose.

---

## Links

- **GitHub**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **Maven Central**: `io.github.sceneview:sceneview:3.2.0`
- **API docs**: [sceneview.github.io](https://sceneview.github.io/api/sceneview/sceneview/)
- **Discord**: [discord.gg/UbNDDBTNqb](https://discord.gg/UbNDDBTNqb)
- **MCP server**: `npx sceneview-mcp` for AI-assisted development
- **Samples**: 15 working sample apps in the repository

---

*SceneView is open source. Built on Google Filament and ARCore. Used in production by apps on Google Play.*
