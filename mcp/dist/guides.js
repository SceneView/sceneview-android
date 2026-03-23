/**
 * guides.ts
 *
 * Static content for the platform roadmap, best practices, and AR setup tools.
 */
// ─── Platform Roadmap ─────────────────────────────────────────────────────────
export const PLATFORM_ROADMAP = `# SceneView Multi-Platform Roadmap

## Current Status (v3.2.0)

| Platform | Status | Artifact |
|----------|--------|----------|
| **Android (Compose)** | Stable | \`io.github.sceneview:sceneview:3.2.0\` |
| **Android (AR)** | Stable | \`io.github.sceneview:arsceneview:3.2.0\` |
| **iOS** | Planned | — |
| **Kotlin Multiplatform** | Planned | — |
| **Web (Compose HTML)** | Exploratory | — |

## Android — What Ships Today

- **3D rendering** via Google Filament: PBR materials, HDR environments, glTF/GLB models, post-processing (bloom, SSAO, FXAA, tone mapping, vignette).
- **AR** via ARCore: plane detection, hit testing, anchors, cloud anchors, augmented images, depth, instant placement, light estimation, face tracking, point cloud.
- **Compose-native DSL**: all nodes are \`@Composable\` functions inside \`Scene { }\` or \`ARScene { }\`.
- **22+ node types**: ModelNode, LightNode, AnchorNode, CameraNode, TextNode, PathNode, ViewNode, PlaneNode, SphereNode, CylinderNode, CubeNode, DynamicSkyNode, FogNode, ReflectionProbeNode, PhysicsNode, HitResultNode, AugmentedImageNode, CloudAnchorNode, and more.
- **15 sample apps** covering model viewing, AR placement, augmented images, cloud anchors, physics, dynamic sky, text labels, line rendering, reflection probes, and post-processing.

## iOS — Planned

SceneView for iOS will target **Swift** and **SwiftUI** with a similar declarative API. The rendering backend will be **Metal** (via Filament's Metal backend). AR will use **ARKit**.

Key design goals:
- API parity with Android where possible (same node types, same composable patterns adapted to SwiftUI)
- Shared glTF/GLB asset pipeline
- Kotlin Multiplatform for shared business logic (optional)

## Kotlin Multiplatform — Planned

A shared \`sceneview-common\` module will provide:
- Scene graph data structures
- Math types (Position, Rotation, Scale, Quaternion)
- Model loading abstractions
- Animation state machines

Platform-specific modules will handle rendering (Filament on Android, Metal on iOS).

## Web — Exploratory

A Compose HTML / Kotlin/JS target is being explored for lightweight 3D viewers using WebGL/WebGPU. This is not on a near-term roadmap.

## How to Stay Updated

- **GitHub:** https://github.com/SceneView/sceneview
- **Releases:** https://github.com/SceneView/sceneview/releases
- **Website:** https://sceneview.github.io
`;
// ─── Best Practices ───────────────────────────────────────────────────────────
const PERFORMANCE_PRACTICES = `## Performance Best Practices

### Model Optimization
- **Use GLB over glTF** for production — GLB is a single binary file, faster to load than multi-file glTF.
- **Compress with Draco or Meshopt** — reduces model size by 70-90%. Filament supports both.
- **Limit polygon count** — aim for <100K triangles per model on mobile. Use LODs for complex scenes.
- **Texture size** — keep textures at 1024x1024 or smaller. Use KTX2 with Basis Universal compression.
- **Reduce draw calls** — merge meshes in your 3D editor. Each material = 1 draw call minimum.

### Runtime Performance
- **Reuse engines and loaders** — always use \`rememberEngine()\`, \`rememberModelLoader(engine)\`, etc. Creating multiple engines wastes GPU memory.
- **Limit simultaneous model loads** — \`rememberModelInstance\` is async, but loading 10+ models simultaneously can spike memory.
- **Use \`scaleToUnits\`** — avoids runtime bounding-box computation when you know the desired size.
- **Avoid per-frame allocations** — in \`onFrame\` callbacks, reuse objects instead of creating new Position/Rotation instances.
- **Profile with Android GPU Inspector** — Filament renders via OpenGL ES / Vulkan; AGI shows exactly where GPU time goes.

### Environment & Lighting
- **Use small HDR environments** — \`sky_2k.hdr\` (2K) is usually sufficient. 4K+ HDRs waste GPU memory on mobile.
- **Prefer \`ENVIRONMENTAL_HDR\`** for AR — gives the most realistic lighting but costs more. Use \`DISABLED\` if not needed.
- **Limit shadow-casting lights** — each shadow-casting light adds a depth render pass.
`;
const ARCHITECTURE_PRACTICES = `## Architecture Best Practices

### Compose Integration
- **Treat Scene/ARScene like any Compose layout** — it participates in the Compose lifecycle. Don't fight it with imperative code.
- **State hoisting** — hoist anchor state, model selection, and UI state to the parent composable. The Scene should be a pure renderer.
- **ViewModel for business logic** — keep AR session state (anchors, detected images) in a ViewModel. Pass it down to the Scene composable.
- **Side effects** — use \`LaunchedEffect\` and \`DisposableEffect\` for async operations, not raw coroutines in composables.

### Project Structure
\`\`\`
app/
  src/main/
    assets/
      models/         # GLB/glTF files
      environments/   # HDR environment maps
      materials/      # Custom .filamat materials
    kotlin/
      ui/
        scene/
          SceneScreen.kt       # Compose screen with Scene { }
          SceneViewModel.kt    # State management
        ar/
          ARScreen.kt          # Compose screen with ARScene { }
          ARViewModel.kt       # Anchor and session state
\`\`\`

### Error Handling
- **Always null-check \`rememberModelInstance\`** — it returns \`null\` while loading and if the asset fails to load.
- **Show loading indicators** — wrap the Scene with a loading overlay keyed on the model instance being null.
- **Handle AR session failures** — ARCore may not be installed or the device may not support AR. Check \`ArCoreApk.getInstance().checkAvailability(context)\`.
`;
const MEMORY_PRACTICES = `## Memory Management Best Practices

### Lifecycle Rules
- **Never call \`engine.destroy()\` manually** when using \`rememberEngine()\` — it handles cleanup on composition disposal.
- **Never call \`destroy()\` on nodes** created as composables — the composition lifecycle manages them.
- **Destroy order matters** for imperative code: MaterialInstance first, then Texture, then Engine. Reversing this causes SIGABRT.

### Reducing Memory Usage
- **Release unused model instances** — if you swap models, the old instance is released when recomposition removes its \`ModelNode\`.
- **Limit concurrent scenes** — each \`Scene\` composable creates its own Filament View and Renderer. Avoid multiple visible scenes.
- **Watch for Bitmap leaks** — if loading textures from Bitmaps, ensure they are recycled after Filament consumes them.
- **Use \`rememberEnvironment\`** — it caches the HDR environment and releases it on disposal. Don't load environments in \`LaunchedEffect\`.
`;
const THREADING_PRACTICES = `## Threading Best Practices

### The Golden Rule
**All Filament JNI calls must run on the main thread.** This includes:
- \`modelLoader.createModelInstance()\`
- \`materialLoader.createMaterial()\`
- \`engine.createTexture()\`
- \`Texture.Builder().build(engine)\`
- Any \`engine.*\` call

### Safe Patterns
- **In composables:** Use \`rememberModelInstance(modelLoader, path)\` — it handles threading internally.
- **In ViewModels:** Use \`modelLoader.loadModelInstanceAsync(path)\` — it posts the JNI call to the main thread.
- **Manual threading:** Wrap in \`withContext(Dispatchers.Main) { }\`.

### Anti-Patterns
\`\`\`kotlin
// WRONG — Filament JNI call on IO thread → crash
viewModelScope.launch(Dispatchers.IO) {
    val model = modelLoader.createModelInstance(buffer) // SIGABRT!
}

// CORRECT — load data on IO, create model on Main
viewModelScope.launch(Dispatchers.IO) {
    val buffer = loadFromNetwork(url)
    withContext(Dispatchers.Main) {
        val model = modelLoader.createModelInstance(buffer) // Safe
    }
}
\`\`\`

### ARCore Threading
- \`Session.update()\` must run on the main thread (SceneView handles this via \`onSessionUpdated\`).
- \`hitResult.createAnchor()\` is safe on the main thread (called from \`onTouchEvent\` which runs on main).
- Cloud anchor hosting/resolving is async — SceneView's \`CloudAnchorNode\` handles the callbacks correctly.
`;
export const BEST_PRACTICES = {
    all: [
        "# SceneView Best Practices\n",
        PERFORMANCE_PRACTICES,
        ARCHITECTURE_PRACTICES,
        MEMORY_PRACTICES,
        THREADING_PRACTICES,
    ].join("\n---\n\n"),
    performance: `# SceneView Best Practices\n\n${PERFORMANCE_PRACTICES}`,
    architecture: `# SceneView Best Practices\n\n${ARCHITECTURE_PRACTICES}`,
    memory: `# SceneView Best Practices\n\n${MEMORY_PRACTICES}`,
    threading: `# SceneView Best Practices\n\n${THREADING_PRACTICES}`,
};
// ─── AR Setup Guide ───────────────────────────────────────────────────────────
export const AR_SETUP_GUIDE = `# SceneView AR — Complete Setup Guide

## 1. Gradle Dependencies

\`\`\`kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("io.github.sceneview:arsceneview:3.2.0")
    // arsceneview includes sceneview transitively — no need to add both
}
\`\`\`

## 2. AndroidManifest.xml

\`\`\`xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required: camera access for AR -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- Required: declares AR hardware dependency -->
    <uses-feature android:name="android.hardware.camera.ar" android:required="true" />

    <!-- Optional: internet for cloud anchors -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!-- Required: tells Play Store this app needs ARCore -->
        <!-- Use "required" to block installs on non-AR devices -->
        <!-- Use "optional" to allow install but degrade gracefully -->
        <meta-data android:name="com.google.ar.core" android:value="required" />
    </application>
</manifest>
\`\`\`

### AR Required vs Optional

| Value | Behavior |
|-------|----------|
| \`"required"\` | App only visible in Play Store on ARCore-supported devices. ARCore auto-installed. |
| \`"optional"\` | App visible to all devices. You must check ARCore availability at runtime. |

For optional AR, check availability before showing AR features:
\`\`\`kotlin
val availability = ArCoreApk.getInstance().checkAvailability(context)
if (availability.isSupported) { /* show AR */ }
\`\`\`

## 3. Session Configuration Options

\`\`\`kotlin
ARScene(
    engine = engine,
    modelLoader = modelLoader,
    sessionConfiguration = { session, config ->

        // ── Depth ─────────────────────────────────────────────
        // Enables occlusion (virtual objects hidden behind real ones)
        config.depthMode =
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                Config.DepthMode.AUTOMATIC
            else Config.DepthMode.DISABLED

        // ── Light Estimation ──────────────────────────────────
        // ENVIRONMENTAL_HDR: most realistic (reflects real lighting on models)
        // AMBIENT_INTENSITY: lighter weight, just brightness + color temp
        // DISABLED: no lighting adjustment
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

        // ── Plane Detection ───────────────────────────────────
        // HORIZONTAL: floors, tables
        // VERTICAL: walls
        // HORIZONTAL_AND_VERTICAL: both
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

        // ── Instant Placement ─────────────────────────────────
        // Allows placing objects before planes are fully detected
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP

        // ── Cloud Anchors ─────────────────────────────────────
        // Required for cross-device persistent anchors
        config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED

        // ── Augmented Images ──────────────────────────────────
        // Register reference images for detection
        val imageDb = AugmentedImageDatabase(session)
        imageDb.addImage("my-image", bitmap, 0.15f) // 15cm physical width
        config.augmentedImageDatabase = imageDb

        // ── Focus Mode ────────────────────────────────────────
        config.focusMode = Config.FocusMode.AUTO
    }
) {
    // AR content goes here
}
\`\`\`

## 4. Common AR Patterns

### Tap-to-Place with Anchor
\`\`\`kotlin
var anchor by remember { mutableStateOf<Anchor?>(null) }

ARScene(
    engine = engine,
    modelLoader = modelLoader,
    planeRenderer = true,
    onTouchEvent = { event, hitResult ->
        if (event.action == MotionEvent.ACTION_UP && hitResult != null) {
            anchor = hitResult.createAnchor()
        }
        true
    }
) {
    anchor?.let { a ->
        AnchorNode(anchor = a) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.5f,
                    isEditable = true  // pinch-to-scale + drag-to-rotate
                )
            }
        }
    }
}
\`\`\`

### Surface Cursor (HitResultNode)
\`\`\`kotlin
ARScene(
    engine = engine,
    modelLoader = modelLoader,
    planeRenderer = true
) {
    HitResultNode(engine = engine) {
        modelInstance?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 0.3f)
        }
    }
}
\`\`\`

### Augmented Image Detection
\`\`\`kotlin
var images by remember { mutableStateOf<Map<String, AugmentedImage>>(emptyMap()) }

ARScene(
    engine = engine,
    sessionConfiguration = { session, config ->
        config.addAugmentedImage(session, "poster", posterBitmap)
    },
    onSessionUpdated = { _, frame ->
        frame.getUpdatedAugmentedImages().forEach { img ->
            images = images + (img.name to img)
        }
    }
) {
    for ((_, image) in images) {
        AugmentedImageNode(augmentedImage = image) {
            ModelNode(modelInstance = instance, scaleToUnits = 0.1f)
        }
    }
}
\`\`\`

## 5. Permissions

Camera permission must be requested at runtime (Android 6.0+). SceneView does **not** handle this — you must request it before showing ARScene:

\`\`\`kotlin
val cameraPermission = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted -> if (granted) showAR = true }

LaunchedEffect(Unit) {
    cameraPermission.launch(Manifest.permission.CAMERA)
}

if (showAR) {
    ARScene(engine = engine, modelLoader = modelLoader) { /* ... */ }
}
\`\`\`
`;
