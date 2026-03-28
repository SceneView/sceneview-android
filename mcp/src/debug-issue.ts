/**
 * debug-issue.ts
 *
 * Targeted debugging guide for common SceneView issues.
 * Given a symptom, returns a focused diagnostic checklist.
 */

export type DebugCategory =
  | "model-not-showing"
  | "ar-not-working"
  | "crash"
  | "performance"
  | "build-error"
  | "black-screen"
  | "lighting"
  | "gestures"
  | "ios";

export const DEBUG_CATEGORIES: DebugCategory[] = [
  "model-not-showing",
  "ar-not-working",
  "crash",
  "performance",
  "build-error",
  "black-screen",
  "lighting",
  "gestures",
  "ios",
];

const DEBUG_GUIDES: Record<DebugCategory, { title: string; guide: string }> = {
  "model-not-showing": {
    title: "Model Not Showing / Invisible",
    guide: `## Debugging: Model Not Showing

### Quick Diagnostic Checklist

1. **Is \`rememberModelInstance\` returning null?**
   - It returns \`null\` while loading AND if the file fails to load.
   - Add a log: \`Log.d("SV", "model: \$modelInstance")\`
   - Show a loading indicator while null.

2. **Is the asset path correct?**
   - Assets must be in \`src/main/assets/\` (not \`res/\`)
   - Path is relative to assets root: \`"models/chair.glb"\` (no leading slash)
   - Check file extension: \`.glb\` or \`.gltf\` (case-sensitive on Android)

3. **Is there a light in the scene?**
   - Without light, PBR models appear black (not invisible — but very dark).
   - Add a directional light:
   \`\`\`kotlin
   LightNode(
       engine = engine,
       type = LightManager.Type.DIRECTIONAL,
       apply = {
           intensity(100_000f)
           castShadows(true)
       }
   )
   \`\`\`
   - Or load an HDR environment for IBL.

4. **Is the model too small or too large?**
   - Default scale = 1.0 in model units. Some models are in millimeters (1000x too small) or centimeters (10x).
   - Try \`scaleToUnits = 1.0f\` to normalize.
   - Check in a 3D editor (Blender) what units the model uses.

5. **Is the camera pointing at the model?**
   - Default camera is at origin looking -Z.
   - Model at (0, 0, 0) may be inside or behind the camera.
   - Try \`centerOrigin = Position(0f, 0f, 0f)\` on ModelNode.
   - Or move camera: \`cameraNode = rememberCameraNode(engine) { lookAt(Position(0f, 1f, 3f), Position(0f, 0f, 0f)) }\`

6. **Is the Scene composable actually visible?**
   - Check Modifier: \`Modifier.fillMaxSize()\` — not \`Modifier.size(0.dp)\` or hidden behind another composable.

7. **Is the GLB file valid?**
   - Test in https://gltf-viewer.donmccurdy.com/
   - Corrupt or incompatible GLB files silently fail to load.

### Common Fixes

\`\`\`kotlin
// Minimal working example — if this shows the model, the issue is in your code
@Composable
fun DebugModelViewer() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    val modelInstance = rememberModelInstance(modelLoader, "models/YOUR_FILE.glb")
    Log.d("SceneView", "Model loaded: \${modelInstance != null}")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader
    ) {
        // Light is essential!
        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = { intensity(100_000f) }
        )

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }
    }
}
\`\`\``,
  },

  "ar-not-working": {
    title: "AR Not Working",
    guide: `## Debugging: AR Not Working

### Quick Diagnostic Checklist

1. **Camera permission granted?**
   - AR requires \`CAMERA\` permission at runtime (not just manifest).
   - Request it BEFORE showing ARScene:
   \`\`\`kotlin
   val launcher = rememberLauncherForActivityResult(
       ActivityResultContracts.RequestPermission()
   ) { granted -> showAR = granted }
   LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
   \`\`\`

2. **ARCore installed?**
   - Check: \`ArCoreApk.getInstance().checkAvailability(context)\`
   - Emulators need Google Play Services for AR manually installed.
   - Real devices auto-install if manifest has \`com.google.ar.core\` meta-data.

3. **Manifest correct?**
   \`\`\`xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-feature android:name="android.hardware.camera.ar" android:required="true" />
   <application>
       <meta-data android:name="com.google.ar.core" android:value="required" />
   </application>
   \`\`\`

4. **Planes not detecting?**
   - Point at a **textured surface** (not plain white walls).
   - Wait 2-3 seconds for ARCore to initialize.
   - Check \`planeFindingMode\` is not \`DISABLED\`.
   - Ensure good lighting — ARCore struggles in dim conditions.

5. **Objects not appearing on tap?**
   - Is \`onTouchEvent\` wired up?
   - Is \`hitResult\` null? Log it: \`Log.d("AR", "hitResult: \$hitResult")\`
   - Are you creating an anchor? \`hitResult.createAnchor()\` must be called.
   - Is the model loaded? Check \`rememberModelInstance\` is not null.

6. **Anchor drift / objects moving?**
   - ALWAYS use \`AnchorNode(anchor = ...)\` — never set \`worldPosition\` manually.
   - ARCore's coordinate system shifts during tracking; anchors compensate.

7. **Testing on emulator?**
   - ARCore emulator support is limited. Test on a real device.
   - If using emulator: Extended Controls > Virtual Sensors > enable AR.

### Common AR Crash Causes

| Symptom | Cause | Fix |
|---------|-------|-----|
| Crash on ARScene open | No camera permission | Request at runtime |
| "ARCore not installed" | Missing Play Services | Add manifest meta-data |
| Session create fails | Device not supported | Check \`checkAvailability()\` |
| Black camera feed | Permission denied | Check runtime permission result |`,
  },

  crash: {
    title: "Crash / SIGABRT / Native Error",
    guide: `## Debugging: Crashes

### SIGABRT / Native Crash

**Most common cause:** Filament resource lifecycle violation.

#### 1. Wrong thread
- ALL Filament JNI calls must be on the **main thread**.
- Check for \`Dispatchers.IO\` or \`Dispatchers.Default\` near Filament calls.
- Fix: wrap in \`withContext(Dispatchers.Main) { ... }\`

#### 2. Wrong destroy order
- Materials MUST be destroyed before Textures.
- Textures MUST be destroyed before Engine.
- \`rememberEngine()\` handles this automatically — avoid manual destroy.
- If imperative: \`materialLoader.destroyMaterialInstance(mi)\` then \`engine.safeDestroyTexture(tex)\`

#### 3. Double destroy
- Calling \`engine.destroy()\` alongside \`rememberEngine()\` → SIGABRT.
- \`rememberEngine()\` destroys on composition disposal — remove manual calls.
- Same for nodes: don't call \`node.destroy()\` on composable nodes.

#### 4. Accessing destroyed resources
- If a composable leaves the tree, its nodes are destroyed.
- Accessing a node/model after removal → native crash.
- Fix: null-check before access, use Compose state properly.

### NullPointerException

- \`rememberModelInstance\` returns null while loading — always null-check.
- \`hitResult\` can be null in \`onTouchEvent\` if no surface is hit.

### Out of Memory

- Multiple \`Engine\` instances waste GPU memory → use single \`rememberEngine()\`.
- Large models (>100K triangles) on old devices → reduce poly count.
- Multiple 4K HDR environments → use 2K or smaller.

### Diagnostic Steps

1. Enable Filament debug logging:
   \`\`\`kotlin
   // In Application.onCreate()
   System.setProperty("filament.backend.debug", "true")
   \`\`\`

2. Check logcat for Filament errors:
   \`\`\`
   adb logcat -s Filament
   \`\`\`

3. Run with Android Studio memory profiler to detect leaks.`,
  },

  performance: {
    title: "Performance Problems",
    guide: `## Debugging: Performance

### Measuring Performance

1. **Enable FPS overlay:**
   \`\`\`kotlin
   Scene(
       engine = engine,
       // Check frame time in onFrame callback
       onFrame = { frameTimeNanos ->
           val fps = 1_000_000_000.0 / frameTimeNanos
           Log.d("FPS", "%.1f".format(fps))
       }
   )
   \`\`\`

2. **Android GPU Inspector** — shows exactly where GPU time goes.
3. **Android Studio Profiler** — CPU and memory usage.

### Common Performance Issues

#### Low FPS (<30)

| Cause | Fix |
|-------|-----|
| High poly model (>100K tris) | Reduce in Blender, use Draco/Meshopt compression |
| Uncompressed textures | Use KTX2 with Basis Universal, max 1024x1024 |
| Too many draw calls | Merge meshes in 3D editor (1 material = 1 draw call) |
| Per-frame allocations | Reuse objects in \`onFrame\`, avoid creating Position/Rotation each frame |
| Multiple engines | Use single \`rememberEngine()\`, never create multiple |
| Post-processing enabled | Disable if not needed: \`Scene(postProcessing = false)\` |
| Shadow-casting lights | Each shadow light = extra depth pass. Limit to 1-2. |

#### High Memory (>500MB)

| Cause | Fix |
|-------|-----|
| 4K HDR environments | Use 2K (\`sky_2k.hdr\`) |
| Multiple scenes | Each \`Scene\` = separate Filament View + Renderer |
| Unreleased models | Let unused models leave composition (auto-cleanup) |
| Bitmap texture leaks | Recycle bitmaps after Filament consumes them |
| Concurrent model loads | Max 3-4 simultaneous \`rememberModelInstance\` calls |

#### Model Optimization Checklist

- [ ] GLB format (not glTF multi-file)
- [ ] <100K triangles per model
- [ ] Textures <=1024x1024
- [ ] KTX2 compressed textures (Basis Universal)
- [ ] Draco or Meshopt mesh compression
- [ ] Single material per mesh where possible
- [ ] Remove unused animations / morph targets`,
  },

  "build-error": {
    title: "Build / Gradle Errors",
    guide: `## Debugging: Build Errors

### "Cannot resolve io.github.sceneview:sceneview:3.4.7"

1. Check repositories in \`settings.gradle.kts\`:
   \`\`\`kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
       }
   }
   \`\`\`
2. Check internet connectivity and proxy settings.
3. Try: \`./gradlew --refresh-dependencies\`

### Java Version Mismatch

SceneView requires **Java 17**.
\`\`\`kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
\`\`\`

### AGP / Gradle Version Compatibility

| AGP | Gradle | Status |
|-----|--------|--------|
| 8.7+ | 8.11.1+ | Recommended |
| 8.4-8.6 | 8.6-8.10 | Works |
| <8.4 | <8.6 | May have issues |

### "Duplicate class" Errors

SceneView bundles Filament. If you also depend on Filament directly:
\`\`\`kotlin
// Remove direct Filament dependency — SceneView includes it
// implementation("com.google.android.filament:filament-android:1.x.x") // REMOVE
implementation("io.github.sceneview:sceneview:3.4.7") // This includes Filament
\`\`\`

### "Cannot find Filament material"

- Pre-compiled materials live in \`src/main/assets/materials/\`
- Don't delete them when cleaning
- If \`filamentPluginEnabled=true\` in gradle.properties, you need Filament desktop tools

### Gradle Clean

\`\`\`bash
./gradlew clean
rm -rf ~/.gradle/caches
rm -rf .gradle
./gradlew --refresh-dependencies
\`\`\``,
  },

  "black-screen": {
    title: "Black Screen / No Rendering",
    guide: `## Debugging: Black Screen

### AR Black Screen

1. **Camera permission not granted** — most common cause.
   - Check logcat for permission denial.
   - Request permission before showing ARScene.

2. **ARCore not initialized** — takes 1-2 seconds.
   - Show a loading overlay until first frame.

3. **Device camera in use** — another app or the system camera is using it.
   - Close other camera apps.

### 3D Black Screen

1. **No light source** — PBR models need light to be visible.
   - Add \`LightNode\` or HDR environment.

2. **Camera inside model** — default camera at origin, model also at origin.
   - Set camera position: \`cameraNode = rememberCameraNode(engine) { lookAt(...) }\`
   - Or set \`centerOrigin\` on ModelNode.

3. **Environment not loaded** — HDR file path wrong or file missing.
   - Check logcat for "Environment not found" messages.
   - Test without environment first.

4. **Scene composable has zero size** — \`Modifier.fillMaxSize()\` missing.

5. **OpenGL ES version** — Filament requires OpenGL ES 3.0+.
   - Check: \`GLES30.glGetString(GLES30.GL_VERSION)\`
   - Very old devices (<2015) may not support it.

### Diagnostic Code

\`\`\`kotlin
Scene(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Red), // Should see red if Scene has zero size
    engine = engine,
    onFrame = { Log.d("SV", "Frame rendered") } // If not logged, Scene isn't rendering
) {
    // Minimal visible content
    CubeNode(engine = engine, size = 1.0f)
    LightNode(engine = engine, type = LightManager.Type.DIRECTIONAL, apply = { intensity(100_000f) })
}
\`\`\``,
  },

  lighting: {
    title: "Lighting Issues",
    guide: `## Debugging: Lighting Issues

### Model Too Dark / Black

- **No light source** — add a directional light or HDR environment.
- **Intensity too low** — directional lights typically need 100,000+ lux.
- **Wrong light type** — \`POINT\` lights need to be near the model; \`DIRECTIONAL\` lights work everywhere.

### Model Too Bright / Overexposed in AR

- **Tone mapping** — AR scenes use the camera feed; default tone mapping enhances contrast.
- Fix: set linear tone mapping on the AR view.
- **Double lighting** — if you add lights AND use \`ENVIRONMENTAL_HDR\`, the model gets double-lit.
- Fix: use one lighting method, not both.

### Flat / No Shadows

- Shadows disabled by default in \`Scene\` (enabled in \`ARScene\`).
- Enable: \`Scene(view = rememberView(engine).also { it.setShadowingEnabled(true) })\`
- Light must have \`castShadows(true)\` in its \`apply\` block.

### Environment / IBL Not Working

\`\`\`kotlin
val environmentLoader = rememberEnvironmentLoader(engine)
Scene(
    environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
            ?: createEnvironment(environmentLoader)
    }
) { ... }
\`\`\`
- Check HDR file exists in \`src/main/assets/environments/\`.
- Use 2K HDR files (not 4K — wastes memory on mobile).`,
  },

  gestures: {
    title: "Gesture / Interaction Issues",
    guide: `## Debugging: Gesture Issues

### Model Not Responding to Touch

1. **\`isEditable\` not set:**
   \`\`\`kotlin
   ModelNode(
       modelInstance = instance,
       isEditable = true  // Enables pinch-to-scale + drag-to-rotate
   )
   \`\`\`

2. **\`onTouchEvent\` consuming events:**
   - If \`onTouchEvent\` returns \`true\`, it consumes the event before nodes see it.
   - Return \`false\` for events you don't handle.

3. **Node has no collision shape:**
   - By default, ModelNode uses its bounding box for hit testing.
   - Geometry nodes (CubeNode, SphereNode) have built-in collision.

### Tap-to-Place Not Working in AR

1. Check \`hitResult\` is not null — log it.
2. Ensure plane detection is enabled: \`planeFindingMode = HORIZONTAL_AND_VERTICAL\`.
3. Plane renderer helps user see where planes are: \`planeRenderer = true\`.
4. Create anchor from hit: \`hitResult.createAnchor()\`.

### Camera Orbit Not Working

- Default Scene has orbit camera. If overridden:
  \`\`\`kotlin
  Scene(
      cameraManipulator = rememberCameraManipulator()  // Enables orbit
  )
  \`\`\`
- In AR, camera is controlled by ARCore — orbit is disabled.`,
  },

  ios: {
    title: "iOS / SceneViewSwift Issues",
    guide: `## Debugging: iOS Issues

### Model Not Loading

1. **Wrong format** — RealityKit uses USDZ or .reality, NOT GLB/glTF.
2. **Missing \`try await\`** — \`ModelNode.load()\` is \`async throws\`:
   \`\`\`swift
   .task {
       do {
           model = try await ModelNode.load("models/car.usdz")
       } catch {
           print("Load failed: \\(error)")
       }
   }
   \`\`\`
3. **File not in bundle** — check Xcode: target > Build Phases > Copy Bundle Resources.
4. **Using \`addChild(model)\` instead of \`addChild(model.entity)\`** — node wrappers aren't Entity subclasses.

### AR Camera Black Screen (iOS)

- **Missing Info.plist entry:**
  \`\`\`xml
  <key>NSCameraUsageDescription</key>
  <string>Camera needed for AR</string>
  \`\`\`
- **Simulator** — ARKit doesn't work on simulators. Use a real device.
- **Device unsupported** — check \`ARWorldTrackingConfiguration.isSupported\`.

### ARSceneView Crash on macOS / visionOS

- \`ARSceneView\` uses \`ARView\` which is iOS-only.
- macOS: use \`SceneView\` (3D only).
- visionOS: use \`RealityView\` with \`ARKitSession\` directly.

### SPM Package Resolution Fails

- Require Xcode 15.0+ (iOS 17 / visionOS targets).
- Clean: Xcode > Product > Clean Build Folder.
- Reset packages: File > Packages > Reset Package Caches.
- URL must be exactly: \`https://github.com/sceneview/sceneview\`

### Swift Concurrency Warnings

- RealityKit entities are main-actor-bound.
- Always load models in \`.task { }\` (inherits @MainActor) or annotate functions with \`@MainActor\`.
- SceneViewSwift nodes are \`@unchecked Sendable\` — the warning is expected.`,
  },
};

export function getDebugGuide(category: DebugCategory): string {
  const entry = DEBUG_GUIDES[category];
  if (!entry) {
    return `Unknown debug category "${category}". Available: ${DEBUG_CATEGORIES.join(", ")}`;
  }
  return `# ${entry.title}\n\n${entry.guide}`;
}

export function autoDetectIssue(description: string): DebugCategory | null {
  const lower = description.toLowerCase();

  if (lower.includes("not showing") || lower.includes("invisible") || lower.includes("can't see") || lower.includes("model doesn't appear")) {
    return "model-not-showing";
  }
  if (lower.includes("ar not") || lower.includes("ar doesn't") || lower.includes("arcore") || lower.includes("plane") || lower.includes("anchor")) {
    return "ar-not-working";
  }
  if (lower.includes("crash") || lower.includes("sigabrt") || lower.includes("native") || lower.includes("fatal") || lower.includes("exception")) {
    return "crash";
  }
  if (lower.includes("slow") || lower.includes("fps") || lower.includes("lag") || lower.includes("jank") || lower.includes("performance") || lower.includes("memory")) {
    return "performance";
  }
  if (lower.includes("build") || lower.includes("gradle") || lower.includes("compile") || lower.includes("dependency") || lower.includes("cannot resolve")) {
    return "build-error";
  }
  if (lower.includes("black screen") || lower.includes("blank") || lower.includes("nothing renders")) {
    return "black-screen";
  }
  if (lower.includes("dark") || lower.includes("bright") || lower.includes("light") || lower.includes("shadow") || lower.includes("overexposed")) {
    return "lighting";
  }
  if (lower.includes("touch") || lower.includes("gesture") || lower.includes("tap") || lower.includes("drag") || lower.includes("rotate") || lower.includes("interact")) {
    return "gestures";
  }
  if (lower.includes("ios") || lower.includes("swift") || lower.includes("xcode") || lower.includes("spm") || lower.includes("realitykit") || lower.includes("usdz")) {
    return "ios";
  }

  return null;
}
