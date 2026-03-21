export const SAMPLES = {
    "model-viewer": {
        id: "model-viewer",
        title: "3D Model Viewer",
        description: "Full-screen 3D scene with a GLB model, HDR environment, and orbit camera",
        tags: ["3d", "model", "environment", "camera"],
        dependency: "io.github.sceneview:sceneview:3.1.1",
        prompt: "Create an Android Compose screen called `ModelViewerScreen` that loads a GLB file from assets/models/my_model.glb and displays it in a full-screen 3D scene with an orbit camera (drag to rotate, pinch to zoom). Add an HDR environment from assets/environments/sky_2k.hdr for realistic lighting. Use SceneView `io.github.sceneview:sceneview:3.1.1`.",
        code: `@Composable
fun ModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")!!
        },
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f },
        cameraManipulator = rememberCameraManipulator()
    ) {
        rememberModelInstance(modelLoader, "models/my_model.glb")?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                autoAnimate = true,
                isEditable = true
            )
        }
    }
}`,
    },
    "geometry-scene": {
        id: "geometry-scene",
        title: "3D Geometry Scene",
        description: "Procedural 3D scene using primitive geometry nodes (cube, sphere, plane) — no GLB required",
        tags: ["3d", "geometry", "animation"],
        dependency: "io.github.sceneview:sceneview:3.1.1",
        prompt: "Create an Android Compose screen called `GeometrySceneScreen` that renders a full-screen 3D scene with a red rotating cube, a metallic blue sphere, and a green floor plane. No model files — use SceneView built-in geometry nodes. Orbit camera. Use SceneView `io.github.sceneview:sceneview:3.1.1`.",
        code: `@Composable
fun GeometrySceneScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing)),
        label = "angle"
    )

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        mainLightNode = rememberMainLightNode(engine) { intensity(80_000f) },
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Rotating red cube
        CubeNode(
            engine,
            size = Size(0.5f, 0.5f, 0.5f),
            materialInstance = materialLoader.createColorInstance(
                Color.Red, metallic = 0f, roughness = 0.5f
            ),
            position = Position(x = -0.6f),
            rotation = Rotation(y = angle)
        )
        // Metallic blue sphere
        SphereNode(
            engine,
            radius = 0.3f,
            materialInstance = materialLoader.createColorInstance(
                Color.Blue, metallic = 0.8f, roughness = 0.2f
            ),
            position = Position(x = 0.6f)
        )
        // Floor plane
        PlaneNode(
            engine,
            size = Size(2f, 0f, 2f),
            materialInstance = materialLoader.createColorInstance(
                Color(0xFF4CAF50), metallic = 0f, roughness = 0.9f
            ),
            position = Position(y = -0.35f)
        )
    }
}`,
    },
    "ar-tap-to-place": {
        id: "ar-tap-to-place",
        title: "AR Tap-to-Place",
        description: "AR scene where each tap places a GLB model on a detected surface. Placed models are pinch-to-scale and drag-to-rotate.",
        tags: ["ar", "model", "anchor", "plane-detection", "placement", "gestures"],
        dependency: "io.github.sceneview:arsceneview:3.1.1",
        prompt: "Create an Android Compose screen called `TapToPlaceScreen` that opens the camera in AR mode. Show a plane detection grid. When the user taps a detected surface, place a 3D GLB model from assets/models/chair.glb at that point. The user should be able to pinch-to-scale and drag-to-rotate after placing. Multiple taps = multiple objects. Use SceneView `io.github.sceneview:arsceneview:3.1.1`.",
        code: `@Composable
fun TapToPlaceScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/chair.glb")
    var placedAnchors by remember { mutableStateOf(listOf<Anchor>()) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null)
                placedAnchors = placedAnchors + hitResult.createAnchor()
            true
        }
    ) {
        placedAnchors.forEach { anchor ->
            AnchorNode(anchor = anchor) {
                ModelNode(
                    modelInstance = modelInstance ?: return@AnchorNode,
                    scaleToUnits = 0.5f,
                    isEditable = true
                )
            }
        }
    }
}`,
    },
    "ar-placement-cursor": {
        id: "ar-placement-cursor",
        title: "AR Placement Cursor",
        description: "AR scene with a reticle that follows the surface at screen center. Tap to confirm placement.",
        tags: ["ar", "model", "anchor", "plane-detection", "placement", "camera"],
        dependency: "io.github.sceneview:arsceneview:3.1.1",
        prompt: "Create an Android Compose AR screen called `ARCursorScreen`. Show a small reticle that snaps to the nearest detected surface at the center of the screen as the user moves the camera. When the user taps, place a GLB model from assets/models/object.glb at that position and hide the reticle. Use SceneView `io.github.sceneview:arsceneview:3.1.1`.",
        code: `@Composable
fun ARCursorScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    val view = LocalView.current

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { _, config ->
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null)
                anchor = hitResult.createAnchor()
            true
        }
    ) {
        if (anchor == null) {
            HitResultNode(xPx = view.width / 2f, yPx = view.height / 2f) {
                SphereNode(radius = 0.02f)
            }
        }
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                ModelNode(
                    modelInstance = modelInstance ?: return@AnchorNode,
                    scaleToUnits = 0.5f,
                    isEditable = true
                )
            }
        }
    }
}`,
    },
    "ar-augmented-image": {
        id: "ar-augmented-image",
        title: "AR Augmented Image",
        description: "Detects a reference image in the camera feed and overlays a 3D model above it.",
        tags: ["ar", "model", "anchor", "image-tracking"],
        dependency: "io.github.sceneview:arsceneview:3.1.1",
        prompt: "Create an Android Compose AR screen called `AugmentedImageScreen` that detects a printed reference image (from R.drawable.target_image, physical width 15 cm) and places a 3D GLB model from assets/models/overlay.glb above it, scaled to match the image width. The model should disappear when the image is lost. Use SceneView `io.github.sceneview:arsceneview:3.1.1`.",
        code: `@Composable
fun AugmentedImageScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val context = LocalContext.current
    var trackedImages by remember { mutableStateOf(listOf<AugmentedImage>()) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        sessionConfiguration = { session, config ->
            config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
                db.addImage(
                    "target",
                    BitmapFactory.decodeResource(context.resources, R.drawable.target_image),
                    0.15f
                )
            }
        },
        onSessionUpdated = { _, frame ->
            trackedImages = frame
                .getUpdatedTrackables(AugmentedImage::class.java)
                .filter { it.trackingState == TrackingState.TRACKING }
        }
    ) {
        trackedImages.forEach { image ->
            AugmentedImageNode(augmentedImage = image) {
                rememberModelInstance(modelLoader, "models/overlay.glb")?.let { instance ->
                    ModelNode(modelInstance = instance, scaleToUnits = image.extentX)
                }
            }
        }
    }
}`,
    },
    "ar-face-filter": {
        id: "ar-face-filter",
        title: "AR Face Filter",
        description: "Front-camera AR that detects faces and renders a 3D mesh material over them.",
        tags: ["ar", "face-tracking", "camera"],
        dependency: "io.github.sceneview:arsceneview:3.1.1",
        prompt: "Create an Android Compose AR screen called `FaceFilterScreen` using the front camera. Detect all visible faces and apply a custom material from assets/materials/face_mask.filamat to the face mesh. Use SceneView `io.github.sceneview:arsceneview:3.1.1` with `Session.Feature.FRONT_CAMERA` and `AugmentedFaceMode.MESH3D`.",
        code: `@Composable
fun FaceFilterScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    var trackedFaces by remember { mutableStateOf(listOf<AugmentedFace>()) }
    val faceMaterial = remember(materialLoader) {
        materialLoader.createInstance("materials/face_mask.filamat")
    }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
        sessionConfiguration = { _, config ->
            config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        },
        onSessionUpdated = { session, _ ->
            trackedFaces = session
                .getAllTrackables(AugmentedFace::class.java)
                .filter { it.trackingState == TrackingState.TRACKING }
        }
    ) {
        trackedFaces.forEach { face ->
            AugmentedFaceNode(augmentedFace = face, meshMaterialInstance = faceMaterial)
        }
    }
}`,
    },
};
export const SAMPLE_IDS = Object.keys(SAMPLES);
export function getSample(id) {
    return SAMPLES[id];
}
