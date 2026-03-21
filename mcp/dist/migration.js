export const MIGRATION_GUIDE = `# SceneView 2.x → 3.0 Migration Guide

SceneView 3.0 is a full rewrite from Android Views to **Jetpack Compose**. Nearly every public API changed. This guide covers every breaking change and how to fix it.

---

## 1. Gradle dependency

| 2.x | 3.0 |
|-----|-----|
| \`io.github.sceneview:sceneview:2.x.x\` | \`io.github.sceneview:sceneview:3.2.0\` |
| \`io.github.sceneview:arsceneview:2.x.x\` | \`io.github.sceneview:arsceneview:3.2.0\` |

---

## 2. Root composable names

| 2.x | 3.0 |
|-----|-----|
| \`SceneView(…)\` | \`Scene(…)\` |
| \`ArSceneView(…)\` | \`ARScene(…)\` |

**Before:**
\`\`\`kotlin
SceneView(modifier = Modifier.fillMaxSize())
\`\`\`

**After:**
\`\`\`kotlin
val engine = rememberEngine()
Scene(modifier = Modifier.fillMaxSize(), engine = engine)
\`\`\`

---

## 3. Engine lifecycle

In 2.x the engine was managed internally. In 3.0 you own it — use \`rememberEngine()\` which ties it to the composition lifecycle.

| 2.x | 3.0 |
|-----|-----|
| Engine implicit | \`val engine = rememberEngine()\` |
| Never destroy manually | Never call \`engine.destroy()\` — \`rememberEngine\` does it |

---

## 4. Model loading

| 2.x | 3.0 |
|-----|-----|
| \`modelLoader.loadModelAsync(path) { … }\` | \`rememberModelInstance(modelLoader, path)\` (returns \`null\` while loading) |
| \`modelLoader.loadModel(path)\` | \`modelLoader.loadModelInstanceAsync(path)\` (imperative) |
| \`ModelRenderable.builder()\` | Removed — use GLB/glTF assets |

**Before:**
\`\`\`kotlin
var modelInstance by remember { mutableStateOf<ModelInstance?>(null) }
LaunchedEffect(Unit) {
    modelInstance = modelLoader.loadModelAsync("models/chair.glb")
}
\`\`\`

**After:**
\`\`\`kotlin
Scene(engine = engine, modelLoader = modelLoader) {
    rememberModelInstance(modelLoader, "models/chair.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
    }
}
\`\`\`

---

## 5. Node hierarchy — imperative → declarative DSL

In 2.x nodes were added imperatively (\`scene.addChild(node)\`). In 3.0 nodes are declared as composables inside \`Scene { }\` or \`ARScene { }\`.

**Before:**
\`\`\`kotlin
val modelNode = ModelNode().apply {
    loadModelGlbAsync(
        glbFileLocation = "models/chair.glb",
        scaleToUnits = 1f,
    )
}
sceneView.addChildNode(modelNode)
\`\`\`

**After:**
\`\`\`kotlin
Scene(engine = engine, modelLoader = modelLoader) {
    rememberModelInstance(modelLoader, "models/chair.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1f)
    }
}
\`\`\`

---

## 6. Removed nodes and replacements

| 2.x | 3.0 replacement |
|-----|-----------------|
| \`TransformableNode\` | Set \`isEditable = true\` on \`ModelNode\` |
| \`PlacementNode\` | \`AnchorNode(anchor = hitResult.createAnchor())\` + \`HitResultNode\` |
| \`ViewRenderable\` | \`ViewNode\` with a \`@Composable\` content lambda |
| \`AnchorNode()\` (no-arg) | \`AnchorNode(anchor = hitResult.createAnchor())\` |

**TransformableNode:**
\`\`\`kotlin
// Before
val node = TransformableNode(transformationSystem).apply {
    setParent(anchorNode)
}

// After
ModelNode(
    modelInstance = instance,
    scaleToUnits = 1f,
    isEditable = true  // enables pinch-to-scale + drag-to-rotate
)
\`\`\`

**ViewRenderable → ViewNode:**
\`\`\`kotlin
// Before
ViewRenderable.builder()
    .setView(context, R.layout.my_layout)
    .build()
    .thenAccept { renderable -> … }

// After
val windowManager = rememberViewNodeManager()
Scene(viewNodeWindowManager = windowManager) {
    ViewNode(windowManager = windowManager) {
        Card { Text("Hello 3D World!") }
    }
}
\`\`\`

---

## 7. Light configuration

LightNode's \`apply\` is a **named parameter** (not a trailing lambda). This is the most common silent breakage after migrating.

\`\`\`kotlin
// WRONG — trailing lambda is silently ignored
LightNode(engine = engine, type = LightManager.Type.DIRECTIONAL) {
    intensity(100_000f)
}

// CORRECT
LightNode(
    engine = engine,
    type = LightManager.Type.DIRECTIONAL,
    apply = {
        intensity(100_000f)
        castShadows(true)
    }
)
\`\`\`

---

## 8. Environment / IBL loading

| 2.x | 3.0 |
|-----|-----|
| \`environmentLoader.loadEnvironment(path)\` | \`environmentLoader.createHDREnvironment(path)\` |

\`\`\`kotlin
// 3.0
val environmentLoader = rememberEnvironmentLoader(engine)
Scene(
    environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
    }
) { … }
\`\`\`

---

## 9. AR session configuration

In 3.0 \`sessionConfiguration\` is a lambda parameter on \`ARScene\` (not a separate builder).

\`\`\`kotlin
ARScene(
    engine = engine,
    modelLoader = modelLoader,
    sessionConfiguration = { session, config ->
        config.depthMode =
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    }
) { … }
\`\`\`

---

## 10. AR anchors (no more worldPosition hacks)

| 2.x pattern | 3.0 |
|-------------|-----|
| \`node.worldPosition = hitResult.hitPose.position\` | \`AnchorNode(anchor = hitResult.createAnchor())\` |

Plain nodes whose \`worldPosition\` is set manually will drift when ARCore remaps its coordinate system. \`AnchorNode\` compensates automatically.

---

## 11. Shadows

In 3.0, \`ARScene\` has shadows enabled by default via \`createARView()\`. For \`Scene\` (3D only), shadows are disabled by default — enable with:

\`\`\`kotlin
Scene(
    view = rememberView(engine).also { it.setShadowingEnabled(true) },
    …
)
\`\`\`

---

## 12. Camera

| 2.x | 3.0 |
|-----|-----|
| \`CameraManipulator\` set on the View | \`cameraManipulator = rememberCameraManipulator()\` on \`Scene\` |
| Custom camera via \`setCameraNode\` | \`cameraNode = rememberCameraNode(engine) { … }\` on \`Scene\` |

---

## Checklist

- [ ] Replace \`SceneView(…)\` → \`Scene(engine = rememberEngine(), …)\`
- [ ] Replace \`ArSceneView(…)\` → \`ARScene(engine = rememberEngine(), …)\`
- [ ] Replace \`modelLoader.loadModelAsync\` → \`rememberModelInstance\`
- [ ] Add null-check on every \`rememberModelInstance\` result
- [ ] Replace \`TransformableNode\` → \`isEditable = true\`
- [ ] Replace \`PlacementNode\` → \`AnchorNode\` + \`HitResultNode\`
- [ ] Replace \`ViewRenderable\` → \`ViewNode\` with Compose lambda
- [ ] Fix \`LightNode { … }\` → \`LightNode(apply = { … })\`
- [ ] Remove manual \`engine.destroy()\` calls
- [ ] Replace manual \`worldPosition\` in AR → \`AnchorNode\`
`;
