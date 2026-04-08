# SceneView Complete Sample Index

## Summary

SceneView ships with 39 samples across 3 platforms: Android (Kotlin/Compose), iOS (Swift/SwiftUI), and Web (Kotlin/JS). Each sample demonstrates a specific capability and can be used as a starting point for your own project.

---

## All Samples

| # | ID | Platform | Category | Description | Key Tags |
|---|---|---|---|---|---|
| 1 | model-viewer | Android | 3D | Full-screen 3D scene with GLB model, HDR environment, orbit camera, animation | 3d, model, environment, camera, animation |
| 2 | ar-model-viewer | Android | AR | Tap-to-place model with plane detection, pinch-to-scale, drag-to-rotate | ar, model, anchor, plane-detection, gestures |
| 3 | ar-augmented-image | Android | AR | Detect reference images in camera feed, overlay 3D models | ar, model, image-tracking |
| 4 | ar-cloud-anchor | Android | AR | Host and resolve persistent cross-device cloud anchors | ar, anchor, cloud-anchor |
| 5 | ar-point-cloud | Android | AR | Visualize ARCore feature points as 3D spheres | ar, point-cloud |
| 6 | ar-face-mesh | Android | AR | Front camera face tracking with mesh overlay | ar, face-tracking, model |
| 7 | gltf-camera | Android | 3D | Use camera definitions embedded in glTF files | 3d, model, camera |
| 8 | camera-manipulator | Android | 3D | Orbit, pan, zoom with configurable sensitivity and bounds | 3d, camera, gestures |
| 9 | camera-animation | Android | 3D | Animated camera flythrough with trigonometric orbit | 3d, camera, animation, model |
| 10 | autopilot-demo | Android | 3D | Autonomous driving HUD with car, road geometry, telemetry overlay | 3d, model, animation, geometry |
| 11 | physics-demo | Android | 3D | Gravity and bounce simulation with spheres and floor | 3d, physics, geometry, animation |
| 12 | dynamic-sky | Android | 3D | Time-of-day sun cycle with animated light color and intensity | 3d, sky, environment, animation, lighting |
| 13 | line-path | Android | 3D | Animated sine waves and Lissajous curves with PathNode/LineNode | 3d, lines, geometry, animation |
| 14 | text-labels | Android | 3D | Camera-facing 3D text labels above geometry (planets) | 3d, text, geometry |
| 15 | reflection-probe | Android | 3D | Zone-based IBL overrides with material picker | 3d, reflection, environment, model |
| 16 | post-processing | Android | 3D | Bloom, vignette, tone mapping, FXAA, SSAO controls | 3d, post-processing, environment |
| 17 | video-texture | Android | 3D | Video playback on a 3D plane with chroma-key support | 3d, video, model |
| 18 | multi-model-scene | Android | 3D | Multiple models loaded and positioned in a complete environment | 3d, model, multi-model, environment |
| 19 | gesture-interaction | Android | 3D | Tap, double-tap, long-press, pinch-to-scale, drag-to-move | 3d, gestures, model |
| 20 | environment-lighting | Android | 3D | HDR IBL + skybox, directional sun, point light, spot light | 3d, environment, lighting, model |
| 21 | procedural-geometry | Android | 3D | Cube, sphere, cylinder, plane with PBR materials (no model files) | 3d, geometry, model |
| 22 | compose-ui-3d | Android | 3D | Embed Compose UI (Cards, Buttons, Text) in 3D space via ViewNode | 3d, compose-ui, text |
| 23 | node-hierarchy | Android | 3D | Parent-child hierarchies -- solar system with orbiting planets | 3d, hierarchy, geometry, animation |
| 24 | image-node | Android | 3D | Display images on 3D planes from assets, resources, or Bitmap | 3d, image, geometry |
| 25 | billboard-sprite | Android | 3D | Always-facing-camera sprites for markers and info overlays | 3d, billboard, image |
| 26 | animation-state | Android | 3D | Reactive animation state machine -- Idle, Walk, Run via Compose state | 3d, model, animation |
| 27 | spring-animation | Android | 3D | Spring physics animations via Compose animateFloatAsState | 3d, animation, spring, geometry |
| 28 | ar-surface-cursor | Android | AR | Center-screen reticle following detected surface via HitResultNode | ar, cursor, plane-detection, placement |
| 29 | ios-model-viewer | iOS | 3D | SwiftUI 3D scene with USDZ model, IBL, orbit camera, animation | 3d, model, environment, camera, ios, swift |
| 30 | ios-ar-model-viewer | iOS | AR | Tap-to-place on detected surfaces using ARKit + RealityKit | ar, model, anchor, plane-detection, ios, swift |
| 31 | ios-ar-augmented-image | iOS | AR | Detect reference images and overlay 3D content via ARKit | ar, model, image-tracking, ios, swift |
| 32 | ios-geometry-shapes | iOS | 3D | Procedural shapes (cube, sphere, cylinder, cone, plane) with PBR | 3d, geometry, ios, swift |
| 33 | ios-lighting | iOS | 3D | Directional, point, and spot lights with shadows | 3d, lighting, environment, ios, swift |
| 34 | ios-physics | iOS | 3D | Interactive physics with bouncing spheres and gravity | 3d, physics, geometry, ios, swift |
| 35 | ios-text-labels | iOS | 3D | Billboard text labels that always face the camera | 3d, text, geometry, ios, swift |
| 36 | ios-video-player | iOS | 3D | Video playback on 3D plane with play/pause/stop controls | 3d, video, ios, swift |
| 37 | web-model-viewer | Web | 3D | Browser-based 3D viewer with Filament.js (WebGL2/WASM) | 3d, model, web, filament-js |
| 38 | web-environment | Web | 3D | Browser 3D scene with IBL environment lighting from KTX | 3d, environment, web, filament-js, lighting |

---

## Grouped by Platform

### Android 3D Samples (19)

model-viewer, gltf-camera, camera-manipulator, camera-animation, autopilot-demo, physics-demo, dynamic-sky, line-path, text-labels, reflection-probe, post-processing, video-texture, multi-model-scene, gesture-interaction, environment-lighting, procedural-geometry, compose-ui-3d, node-hierarchy, image-node, billboard-sprite, animation-state, spring-animation

### Android AR Samples (7)

ar-model-viewer, ar-augmented-image, ar-cloud-anchor, ar-point-cloud, ar-face-mesh, ar-surface-cursor

### iOS 3D Samples (5)

ios-model-viewer, ios-geometry-shapes, ios-lighting, ios-text-labels, ios-video-player

### iOS AR Samples (3)

ios-ar-model-viewer, ios-ar-augmented-image, ios-physics

### Web Samples (2)

web-model-viewer, web-environment

---

## Top 5 Most-Used Samples with Code

### 1. model-viewer -- 3D Model Viewer (Android)

The foundational sample. Load a GLB model with HDR environment, orbit camera, and animation.

```kotlin
@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f },
        cameraManipulator = rememberCameraManipulator()
    ) {
        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                autoAnimate = true,
                isEditable = true
            )
        }
    }
}
```

**Key pattern:** `rememberModelInstance` + null check + `ModelNode` with `scaleToUnits`.

### 2. ar-model-viewer -- AR Tap-to-Place (Android)

Tap a detected surface to place a 3D model with gesture support.

```kotlin
@Composable
fun ARModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null)
                anchor = hitResult.createAnchor()
            true
        }
    ) {
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                modelInstance?.let { instance ->
                    ModelNode(modelInstance = instance, scaleToUnits = 0.5f, isEditable = true)
                }
            }
        }
    }
}
```

**Key pattern:** `onTouchEvent` creates anchor + `AnchorNode` wraps `ModelNode`.

### 3. procedural-geometry -- Shapes Without Model Files (Android)

Create 3D scenes with geometry nodes and PBR materials -- no model files needed.

```kotlin
@Composable
fun ProceduralGeometryScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        cameraManipulator = rememberCameraManipulator(),
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f }
    ) {
        val redMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Red, metallic = 0f, roughness = 0.6f)
        }
        CubeNode(size = Size(0.6f), materialInstance = redMat, position = Position(x = -1f))

        val chromeMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Gray, metallic = 1f, roughness = 0.05f)
        }
        SphereNode(radius = 0.4f, materialInstance = chromeMat, position = Position(x = 0f, y = 0.4f))

        val greenMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Green, metallic = 0.2f, roughness = 0.4f)
        }
        CylinderNode(radius = 0.25f, height = 0.8f, materialInstance = greenMat, position = Position(x = 1f))
    }
}
```

**Key pattern:** `materialLoader.createColorInstance` inside `remember` + geometry node composables.

### 4. ios-model-viewer -- iOS 3D Model Viewer (Swift)

Load a USDZ model in SwiftUI with orbit camera and animation.

```swift
import SwiftUI
import SceneViewSwift
import RealityKit

struct ModelViewerScreen: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { root in
            if let model {
                root.addChild(model.entity)
            }
        }
        .environment(.studio)
        .cameraControls(.orbit)
        .task {
            do {
                model = try await ModelNode.load("models/car.usdz")
                model?.scaleToUnits(1.0)
                model?.playAllAnimations()
            } catch {
                print("Failed to load model: \(error)")
            }
        }
    }
}
```

**Key pattern:** `ModelNode.load` async + `root.addChild` + `.cameraControls(.orbit)`.

### 5. animation-state -- Reactive Animation State Machine (Android)

Switch between named animations using Compose state.

```kotlin
@Composable
fun AnimationStateScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var currentAnim by remember { mutableStateOf("Idle") }

    Column {
        SceneView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            rememberModelInstance(modelLoader, "models/character.glb")?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.5f,
                    autoAnimate = false,
                    animationName = currentAnim,
                    animationLoop = true
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Idle", "Walk", "Run").forEach { anim ->
                Button(onClick = { currentAnim = anim }) { Text(anim) }
            }
        }
    }
}
```

**Key pattern:** `animationName` driven by Compose state -- automatic transition when value changes.

---

## Sample Tags Reference

| Tag | Description | Sample Count |
|---|---|---|
| 3d | 3D scene (non-AR) | 30 |
| ar | Augmented reality | 9 |
| model | Uses 3D model files (GLB/USDZ) | 19 |
| geometry | Procedural geometry nodes | 11 |
| animation | Animated content | 10 |
| camera | Camera manipulation or animation | 5 |
| environment | HDR/IBL environment lighting | 9 |
| lighting | Light nodes or configuration | 4 |
| gestures | Touch/gesture interaction | 4 |
| physics | Physics simulation | 3 |
| anchor | AR world anchors | 3 |
| plane-detection | AR surface detection | 4 |
| image-tracking | AR image recognition | 2 |
| text | 3D text labels | 3 |
| video | Video playback in 3D | 2 |
| ios / swift | iOS platform | 8 |
| web / filament-js | Web platform | 2 |
| compose-ui | Compose UI in 3D (ViewNode) | 1 |
| billboard | Camera-facing sprites | 1 |
| spring | Spring physics animations | 1 |
| cloud-anchor | Cross-device persistent anchors | 1 |
| face-tracking | Face mesh tracking | 1 |
| cursor | AR surface reticle | 1 |
| hierarchy | Parent-child node relationships | 1 |
| reflection | Reflection probes | 1 |
| post-processing | Bloom, SSAO, DoF, etc. | 1 |
| multi-model | Multiple models in one scene | 1 |
| image | Image display on 3D planes | 2 |
| sky | Dynamic sky / time-of-day | 1 |
