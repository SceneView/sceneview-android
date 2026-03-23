# API Cheatsheet

A quick reference for SceneView's most-used APIs. Print it, pin it, keep it next to your keyboard.

---

## Setup

```kotlin
// build.gradle
implementation("io.github.sceneview:sceneview:3.2.0")     // 3D
implementation("io.github.sceneview:arsceneview:3.2.0")    // AR + 3D
```

---

## Core Remember Hooks

```kotlin
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val materialLoader = rememberMaterialLoader(engine)
val environmentLoader = rememberEnvironmentLoader(engine)

val model = rememberModelInstance(modelLoader, "models/file.glb")  // null while loading
val env = rememberEnvironment(environmentLoader) {
    createHDREnvironment("environments/sky.hdr")
        ?: createEnvironment(environmentLoader)
}

val cameraManipulator = rememberCameraManipulator()
val mainLight = rememberMainLightNode(engine) { intensity = 100_000f }
val cameraNode = rememberCameraNode(engine) { position = Position(0f, 2f, 5f) }
val viewNodeManager = rememberViewNodeManager()
```

---

## Scene

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    cameraManipulator = cameraManipulator,    // orbit/pan/zoom
    cameraNode = cameraNode,                  // OR fixed camera
    environment = env,
    mainLightNode = mainLight,
    surfaceType = SurfaceType.Surface,        // or TextureSurface
    isOpaque = true,
    viewNodeWindowManager = viewNodeManager,  // for ViewNode
    onGestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { event, node -> },
        onDoubleTap = { event, node -> },
        onLongPress = { event, node -> }
    ),
    onTouchEvent = { event, hitResult -> false },
    onFrame = { frameTimeNanos -> }
) {
    // SceneScope — declare nodes here
}
```

---

## ARScene

```kotlin
ARScene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    planeRenderer = true,
    sessionConfiguration = { session, config ->
        config.depthMode = Config.DepthMode.AUTOMATIC
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    },
    sessionFeatures = setOf(),  // e.g., Session.Feature.FRONT_CAMERA
    onSessionUpdated = { session, frame -> },
    onTouchEvent = { event, hitResult -> true }
) {
    // ARSceneScope — declare AR nodes here
}
```

---

## Node Types — 3D

| Node | Key Parameters |
|---|---|
| `ModelNode` | `modelInstance`, `scaleToUnits`, `centerOrigin`, `position`, `rotation`, `isEditable`, `autoAnimate`, `animationName`, `animationLoop` |
| `CubeNode` | `size: Size`, `materialInstance` |
| `SphereNode` | `radius: Float`, `materialInstance` |
| `CylinderNode` | `radius`, `height`, `materialInstance` |
| `PlaneNode` | `size: Size`, `materialInstance` |
| `LightNode` | `type: LightManager.Type`, `apply = { intensity(); color(); castShadows() }` |
| `ImageNode` | `imageFileLocation` / `imageResId` / `bitmap`, `size` |
| `VideoNode` | `player: MediaPlayer`, `chromaKeyColor`, `size` |
| `ViewNode` | `windowManager`, content = `@Composable` |
| `TextNode` | `text`, `fontSize`, `textColor`, `backgroundColor`, `widthMeters` |
| `BillboardNode` | `bitmap`, `widthMeters`, `heightMeters` |
| `LineNode` | `start`, `end`, `materialInstance` |
| `PathNode` | `points: List<Position>`, `closed`, `materialInstance` |
| `DynamicSkyNode` | `timeOfDay` (0-24), `turbidity`, `sunIntensity` |
| `FogNode` | `view`, `density`, `height`, `color`, `enabled` |
| `ReflectionProbeNode` | `filamentScene`, `environment`, `position`, `radius`, `cameraPosition` |
| `PhysicsNode` | `node`, `mass`, `restitution`, `linearVelocity`, `floorY`, `radius` |
| `MeshNode` | `primitiveType`, `vertexBuffer`, `indexBuffer`, `materialInstance` |
| `Node` | `position`, `rotation`, `scale` + child content |
| `CameraNode` | (via `rememberCameraNode`) |

---

## Node Types — AR

| Node | Key Parameters |
|---|---|
| `AnchorNode` | `anchor: Anchor` + child content |
| `HitResultNode` | `xPx`, `yPx` + child content (reticle) |
| `AugmentedImageNode` | `augmentedImage` + child content |
| `AugmentedFaceNode` | `augmentedFace`, `meshMaterialInstance` |
| `CloudAnchorNode` | `anchor`, `cloudAnchorId`, `onHosted` + child content |

---

## Common Node Properties

```kotlin
node.position = Position(x, y, z)      // meters
node.rotation = Rotation(x, y, z)      // degrees
node.scale = Scale(x, y, z)            // multiplier
node.isVisible = true
node.isEditable = true                 // pinch-scale, drag-move, rotate
node.isTouchable = true
node.onSingleTapConfirmed = { event -> true }
node.onFrame = { frameTimeNanos -> }

// Smooth movement
node.transform(position = Position(2f, 0f, 0f), smooth = true, smoothSpeed = 5f)
node.lookAt(targetNode)

// Animation
node.animateRotations(Rotation(0f), Rotation(y = 360f)).also {
    it.duration = 2000
    it.repeatCount = ValueAnimator.INFINITE
}.start()
```

---

## Math Types

```kotlin
import io.github.sceneview.math.*

Position(x = 0f, y = 1f, z = -2f)     // Float3, meters
Rotation(x = 0f, y = 90f, z = 0f)     // Float3, degrees
Scale(1.5f)                             // uniform
Scale(x = 2f, y = 1f, z = 2f)         // non-uniform
Direction(x = 0f, y = 1f, z = 0f)     // unit vector
Size(width = 1f, height = 0.5f)       // Float2
```

---

## Resource Loading

```kotlin
// Composable (preferred)
val model = rememberModelInstance(modelLoader, "models/file.glb")

// Imperative
val model = modelLoader.loadModelInstance("models/file.glb")
modelLoader.loadModelInstanceAsync("models/file.glb") { instance -> }

// Environment
environmentLoader.createHDREnvironment("environments/sky.hdr")
environmentLoader.createKtxEnvironment("environments/studio.ktx")

// Material
materialLoader.createColorInstance(Color.Red)
```

---

## Threading Rules

| Safe | Unsafe |
|---|---|
| `rememberModelInstance(...)` | `modelLoader.createModelInstance(...)` on IO |
| `loadModelInstanceAsync(...)` | `materialLoader.createMaterial(...)` on IO |
| Any composable in `Scene { }` | Direct Filament API on background thread |

**Rule:** Filament JNI = main thread only. `remember*` hooks handle this for you.
