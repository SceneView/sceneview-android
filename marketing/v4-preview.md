# SceneView 4.0 — What's coming

*A look at the next major release and why it matters for Android 3D & AR.*

---

## The journey so far

| Version | Theme | Key moment |
|---|---|---|
| **2.x** | View-based Sceneform successor | First Filament + ARCore integration |
| **3.0** | Compose rewrite | Nodes become composables — "3D is just Compose UI" |
| **3.1** | Stability + DX | `rememberModelInstance`, camera manipulator, gesture polish |
| **3.2** | Physics & Spatial UI | `PhysicsNode`, `DynamicSkyNode`, `FogNode`, `LineNode`, `TextNode`, `ReflectionProbeNode`, post-processing |
| **4.0** | Multi-scene, portals, XR | The release that makes SceneView a platform |

Each release has expanded what's possible without changing the core principle:
**nodes are composables, state drives the scene, lifecycle is automatic.**

v4.0 continues that trajectory — but the scope jumps significantly.

---

## 4.0 feature preview

### Multiple `Scene {}` composables on one screen

Today, you get one `Scene` per screen. In 4.0, multiple independent scenes share a single
Filament `Engine`, each with its own camera, environment, and node tree.

```kotlin
@Composable
fun DashboardScreen() {
    Column {
        // Product hero — rotating model with HDR environment
        Scene(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            engine = engine,
            environment = studioEnvironment
        ) {
            ModelNode(modelInstance = product, scaleToUnits = 1.0f)
        }

        // Inline data globe — different camera, different lighting
        Scene(
            modifier = Modifier.size(200.dp),
            engine = engine,  // same engine, shared GPU resources
            environment = darkEnvironment
        ) {
            SphereNode(radius = 0.5f, materialInstance = globeMaterial)
            dataPoints.forEach { point ->
                CubeNode(
                    position = point.position,
                    size = Size(0.02f),
                    materialInstance = point.material
                )
            }
        }

        // Rest of the dashboard — standard Compose
        LazyColumn { /* cards, charts, text */ }
    }
}
```

**Why it matters:** Dashboards, e-commerce feeds, social timelines — anywhere you want
multiple 3D elements on the same screen without a single `Scene` owning the full viewport.
Each scene is just another composable in your layout. Mix freely with `LazyColumn`, `Pager`,
`BottomSheet` — whatever your app needs.

---

### `PortalNode` — a scene inside a scene

Render a secondary scene inside a 3D frame. Think of it as a window into another world,
placed inside your current scene.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    // The real world (or your main 3D scene)
    ModelNode(modelInstance = room, scaleToUnits = 2.0f)

    // A portal on the wall — look through it into a different scene
    PortalNode(
        position = Position(0f, 1.5f, -2f),
        size = Size(1.2f, 1.8f),
        scene = portalScene  // independent scene with its own environment
    ) {
        // Inside the portal: different lighting, different world
        ModelNode(modelInstance = fantasyLandscape, scaleToUnits = 5.0f)
        DynamicSkyNode(sunPosition = Position(0.2f, 0.8f, 0.3f))
        FogNode(density = 0.05f, color = Color(0.6f, 0.7f, 1.0f))
    }
}
```

**Use cases:**
- AR portals — look through a "window" in your room into a virtual space
- Product showcases — each product in its own lighting environment
- Games — level transitions, dimensional rifts
- Real estate — stand in one room, see another through the portal

---

### SceneView-XR — Android XR & spatial computing

A new module for Android XR (spatial computing headsets and passthrough AR).
Same composable API, now in spatial environments.

```kotlin
// New module
implementation("io.github.sceneview:sceneview-xr:4.0.0")
```

```kotlin
// Same familiar pattern — now in spatial computing
XRScene(modifier = Modifier.fillMaxSize()) {
    // Content placed in the user's physical space
    ModelNode(
        modelInstance = furniture,
        scaleToUnits = 1.0f,
        position = Position(0f, 0f, -2f)  // 2 meters in front
    )

    // Spatial UI — Compose panels floating in space
    ViewNode(position = Position(0.5f, 1.5f, -1.5f)) {
        Card {
            Text("Tap to customize")
            ColorPicker(onColorSelected = { color -> /* update material */ })
        }
    }
}
```

**Why it matters:** Android XR is Google's push into spatial computing. SceneView-XR means
your existing 3D/AR skills and code patterns transfer directly. No new paradigm to learn.
The same `ModelNode`, `LightNode`, `ViewNode` composables — just placed in spatial space.

---

### Filament 2.x migration

When Filament 2.x stabilizes, SceneView 4.0 will adopt it for:
- Improved rendering performance
- Better material system
- New shader capabilities
- Reduced memory footprint

This is transparent to SceneView users — the composable API stays the same.

---

### Kotlin Multiplatform proof of concept

An experimental KMP target using Filament's Metal backend for iOS.
Same `Scene {}` composable, rendering natively on both platforms.

This is a proof of concept in 4.0 — not production-ready — but it signals the direction:
**one 3D composable API, multiple platforms.**

---

## What v3.x already delivers (and v4.0 builds on)

For developers evaluating SceneView today, here's the current feature set you get
immediately with v3.2.0:

### Rendering
- Physically-based rendering via Filament 1.70
- HDR environment lighting (`.hdr`, `.ktx`)
- Dynamic shadows, ambient occlusion
- Post-processing: bloom, depth-of-field, SSAO, fog
- 60fps on mid-range devices

### 3D nodes (all composable)
- `ModelNode` — glTF/GLB with animations, gestures
- `CubeNode`, `SphereNode`, `CylinderNode`, `PlaneNode` — geometry primitives
- `LightNode` — sun, point, spot, directional
- `DynamicSkyNode` — time-of-day sun positioning
- `FogNode` — atmospheric fog
- `ReflectionProbeNode` — local cubemap reflections
- `ImageNode`, `VideoNode` (with chromakey)
- `ViewNode` — any Composable rendered in 3D space
- `TextNode`, `BillboardNode` — camera-facing text and labels
- `LineNode`, `PathNode` — 3D polylines and paths
- `PhysicsNode` — rigid body simulation
- `CameraNode`, `MeshNode`, `Node` (grouping)

### AR (ARScene composable)
- Plane detection with persistent mesh rendering
- Image detection and tracking
- Face mesh tracking and augmentation
- Cloud anchors (cross-device)
- Environmental HDR lighting
- Streetscape geometry (city-scale 3D)
- Geospatial API support

### Developer experience
- `remember*` for all resources — automatic lifecycle
- Thread-safe model loading
- Orbit/pan/zoom camera in one line
- Multi-touch gestures built into nodes
- MCP server for AI-assisted development
- 15 working sample apps

---

## The v4.0 vision

SceneView started as "make 3D easy on Android." v3.0 proved that 3D could work like Compose.
v3.2 added physics, atmosphere, and spatial UI.

v4.0 is about removing the last limitations:

| Limitation today | v4.0 solution |
|---|---|
| One Scene per screen | Multiple independent Scenes |
| Flat scene graph | `PortalNode` — scenes within scenes |
| Android only | KMP proof of concept (iOS) |
| Phone/tablet only | `SceneView-XR` for spatial computing |
| Filament 1.x | Filament 2.x (when stable) |

The goal: **SceneView becomes the standard way to do 3D on Android** — from a product
thumbnail to a spatial computing experience, all with the same composable API.

---

## Timeline

v4.0 is on the [roadmap](https://github.com/SceneView/sceneview-android/blob/main/ROADMAP.md)
as the next major release following the 3.x feature series. Follow the repo for updates.

**You don't need to wait for 4.0.** Everything in v3.2.0 is production-ready today.
v4.0 adds capabilities on top — it doesn't replace anything.

```gradle
// Start building today
implementation("io.github.sceneview:sceneview:3.2.0")
implementation("io.github.sceneview:arsceneview:3.2.0")
```

---

*[github.com/SceneView/sceneview-android](https://github.com/SceneView/sceneview-android) — Apache 2.0 — the #1 3D & AR library for Android*
