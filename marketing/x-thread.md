# Twitter/X Thread — SceneView: #1 3D & AR SDK

*Copy-paste each numbered block as a separate tweet. Thread format.*

---

## Thread

**1/12 — Hook**

3D on Android used to require 500+ lines of boilerplate, lifecycle management, and OpenGL knowledge.

SceneView reduced it to this:

```
Scene {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f)
}
```

A thread on why it's the #1 3D & AR library for Android and iOS:

---

**2/12 — The core idea**

The insight: 3D nodes should work like Compose UI.

- `ModelNode` = like `Image()` but 3D
- `LightNode` = lighting as a composable
- `if/else` = controls what's in the scene
- `State<T>` = drives animations

No new paradigm. Just Compose — with depth.

---

**3/12 — Before vs. After**

Before (Sceneform / raw ARCore):
- XML layout + Fragment
- `ModelRenderable.builder().build().thenAccept { ... }`
- `onResume`, `onPause`, `onDestroy`
- Manual `setParent()`, manual `destroy()`

After (SceneView):
- One `Scene { }` composable
- `rememberModelInstance()` → null while loading, auto-recompose when ready
- Lifecycle = automatic

---

**4/12 — 26+ node types**

All composable:

Models: `ModelNode`
Geometry: `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode`
Lighting: `LightNode`, `DynamicSkyNode`
Atmosphere: `FogNode`, `ReflectionProbeNode`
Media: `ImageNode`, `VideoNode`, `ViewNode`
Text: `TextNode`, `BillboardNode`
Drawing: `LineNode`, `PathNode`
Physics: `PhysicsNode`
AR: `AnchorNode`, `AugmentedImageNode`, `AugmentedFaceNode`, `CloudAnchorNode`

---

**5/12 — The rendering engine**

Built on Google Filament — the same PBR engine Google uses in Search and Play Store.

- Physically-based rendering
- HDR environment lighting
- Dynamic shadows
- Post-processing: bloom, DOF, SSAO
- 60fps on mid-range phones

Not a toy renderer. Production quality.

---

**6/12 — AR in the same pattern**

```
ARScene(planeRenderer = true) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            ModelNode(modelInstance = sofa)
        }
    }
}
```

Plane detection, image tracking, face mesh, cloud anchors, geospatial API — all as composables inside `ARScene { }`.

---

**7/12 — The killer feature nobody talks about**

`ViewNode` — render ANY Compose UI inside 3D space.

A `Card` with price and "Buy Now" button floating next to an AR-placed product. A tooltip hovering over a 3D model. A real `TextField` in a 3D scene.

No other Android 3D library does this.

---

**8/12 — Physics**

`PhysicsNode` — rigid body simulation in a composable.

Gravity, collision, tap-to-throw. Combined with `DynamicSkyNode` for time-of-day lighting and `FogNode` for atmosphere.

Interactive 3D worlds in Compose. Not a game engine — but enough for most apps.

---

**9/12 — vs. the alternatives**

| | SceneView | Sceneform | Unity | Raw ARCore |
|---|---|---|---|---|
| Compose | Native | No | No | No |
| APK size | ~5 MB | ~3 MB | 40-80 MB | ~1 MB |
| Setup | 1 line | Archived | Separate build | 500+ lines |
| Status | Active | Dead | Active | No UI layer |

---

**10/12 — The use case that matters most**

Most apps won't be "3D apps."

But replacing `Image()` with `Scene {}` on a product page? That's 10 extra lines for a noticeably better experience.

3D as a finishing touch — not a feature. That's the real opportunity.

---

**11/12 — Now cross-platform**

SceneView now supports:
- Android: Jetpack Compose + Filament + ARCore
- iOS: SwiftUI + RealityKit + ARKit
- Shared core: Kotlin Multiplatform (math, collision, animation, geometry)

One SDK, two platforms, native on both.

---

**12/12 — Get started**

```
implementation("io.github.sceneview:sceneview:3.2.0")
implementation("io.github.sceneview:arsceneview:3.2.0")
```

15 sample apps. Full API docs. MCP server for AI-assisted development.

Open source. Apache 2.0.

github.com/SceneView/sceneview

#AndroidDev #JetpackCompose #3D #AR #Kotlin #SceneView

---

## Posting tips

- **Best time:** Tuesday–Thursday, 9–11 AM EST (US dev audience) or 3–5 PM CET (EU)
- **Thread format:** Post tweet 1, then reply chain for 2–12
- **Engagement:** Quote-tweet #1 with the code screenshot from tweet 6 or 7 for visual appeal
- **Pin:** Pin tweet 1 to your profile for the week
- **Cross-post:** Copy to Bluesky and Mastodon (Android dev community is active there)
- **Follow-up:** Reply to your own thread 24h later with a link to the Medium article
