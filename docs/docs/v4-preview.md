# SceneView 4.0 preview

A look at the next major release and why it matters.

!!! info "v3.2.0 is production-ready today"
    You don't need to wait for 4.0. Everything below adds capabilities on top — it doesn't
    replace anything. [Get started now](index.md#get-started).

---

## The journey

| Version | Theme |
|---|---|
| **2.x** | View-based Sceneform successor |
| **3.0** | Compose rewrite — "3D is just Compose UI" |
| **3.1** | `rememberModelInstance`, camera manipulator, gesture polish |
| **3.2** | Physics, dynamic sky, fog, reflections, lines, text, post-processing |
| **4.0** | Multi-scene, portals, XR, Kotlin Multiplatform |

---

## Multiple `Scene {}` on one screen

Today, you get one `Scene` per screen. In 4.0, multiple independent scenes share a single
Filament `Engine`, each with its own camera, environment, and node tree.

```kotlin
@Composable
fun DashboardScreen() {
    Column {
        // Product hero
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
            engine = engine,
            environment = darkEnvironment
        ) {
            SphereNode(radius = 0.5f, materialInstance = globeMaterial)
        }

        // Standard Compose content
        LazyColumn { /* cards, charts, text */ }
    }
}
```

Dashboards, e-commerce feeds, social timelines — 3D elements mixed freely with
`LazyColumn`, `Pager`, `BottomSheet`.

---

## `PortalNode` — a scene inside a scene

Render a secondary scene inside a 3D frame. A window into another world.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(modelInstance = room, scaleToUnits = 2.0f)

    // A portal on the wall
    PortalNode(
        position = Position(0f, 1.5f, -2f),
        size = Size(1.2f, 1.8f),
        scene = portalScene
    ) {
        ModelNode(modelInstance = fantasyLandscape, scaleToUnits = 5.0f)
        DynamicSkyNode(sunPosition = Position(0.2f, 0.8f, 0.3f))
        FogNode(density = 0.05f, color = Color(0.6f, 0.7f, 1.0f))
    }
}
```

**Use cases:** AR portals, product showcases with custom lighting, game level transitions,
real estate walkthroughs.

---

## SceneView-XR — Android XR & spatial computing

A new module for spatial computing headsets and passthrough AR.
Same composable API — now in spatial environments.

```kotlin
implementation("io.github.sceneview:sceneview-xr:4.0.0")
```

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

Your existing 3D/AR skills and code patterns transfer directly to spatial computing.

---

## Also in 4.0

- **Filament 2.x migration** — improved performance, better materials, reduced memory
- **Kotlin Multiplatform proof of concept** — iOS via Filament's Metal backend (experimental)
- **`ParticleNode`** — GPU particle system for fire, smoke, sparkles, confetti
- **`AnimationController`** — composable-level animation blending, cross-fading, and layering
- **`CollisionNode`** — declarative collision detection between scene nodes

---

## Who should care about 4.0

<div class="grid cards" markdown>

-   :material-shopping: **E-commerce teams**

    ---

    Multi-scene lets you embed 3D product viewers in `LazyColumn` feeds, `BottomSheet` configurators, and `Pager` carousels — all on one screen, all with independent cameras.

-   :material-office-building: **Real estate / architecture**

    ---

    `PortalNode` lets users peek through doors into furnished rooms, walk through 3D floor plans, and compare lighting conditions — all without loading separate screens.

-   :material-head-snowflake: **XR teams**

    ---

    `SceneView-XR` means the same code and patterns you build for phone AR transfer directly to Android XR headsets. No new framework to learn.

-   :material-cellphone-link: **Cross-platform teams**

    ---

    KMP proof of concept means you can start sharing scene definitions between Android and iOS. One Kotlin codebase, two platforms.

</div>

---

## Summary

| Limitation today | v4.0 solution |
|---|---|
| One Scene per screen | Multiple independent Scenes |
| Flat scene graph | `PortalNode` — scenes within scenes |
| Android only | KMP proof of concept (iOS) |
| Phone/tablet only | `SceneView-XR` for spatial computing |

[:octicons-arrow-right-24: Full roadmap on GitHub](https://github.com/SceneView/sceneview-android/blob/main/ROADMAP.md)
