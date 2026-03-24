export const SAMPLES = {
    "model-viewer": {
        id: "model-viewer",
        title: "3D Model Viewer",
        description: "Full-screen 3D scene with a GLB model, HDR environment, orbit camera, and animation controls",
        tags: ["3d", "model", "environment", "camera", "animation"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create an Android Compose screen that loads a GLB model and displays it with HDR lighting, orbit camera, and animation playback. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr") ?: createEnvironment(environmentLoader)
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
}`,
    },
    "ar-model-viewer": {
        id: "ar-model-viewer",
        title: "AR Tap-to-Place Model Viewer",
        description: "AR scene with plane detection. Tap a surface to place a 3D model with pinch-to-scale and drag-to-rotate gestures.",
        tags: ["ar", "model", "anchor", "plane-detection", "placement", "gestures"],
        dependency: "io.github.sceneview:arsceneview:3.3.0",
        prompt: "Create an AR screen that detects surfaces and lets the user tap to place a GLB model. Support pinch-to-scale and drag-to-rotate. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
        code: `@Composable
fun ARModelViewerScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
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
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 0.5f,
                        isEditable = true
                    )
                }
            }
        }
    }
}`,
    },
    "ar-augmented-image": {
        id: "ar-augmented-image",
        title: "AR Augmented Image",
        description: "Detects reference images in the camera feed and overlays 3D models or video above them.",
        tags: ["ar", "model", "image-tracking"],
        dependency: "io.github.sceneview:arsceneview:3.3.0",
        prompt: "Create an AR screen that detects a printed reference image and places a 3D model above it. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
        code: `@Composable
fun AugmentedImageScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/rabbit.glb")
    var augmentedImages by remember { mutableStateOf<Map<String, AugmentedImage>>(emptyMap()) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        sessionConfiguration = { session, config ->
            config.addAugmentedImage(
                session, "rabbit",
                assets.open("augmentedimages/rabbit.jpg").use(BitmapFactory::decodeStream)
            )
        },
        onSessionUpdated = { _, frame ->
            frame.getUpdatedAugmentedImages().forEach { image ->
                augmentedImages = augmentedImages.toMutableMap().apply {
                    this[image.name] = image
                }
            }
        }
    ) {
        for ((_, image) in augmentedImages) {
            AugmentedImageNode(augmentedImage = image) {
                modelInstance?.let { instance ->
                    ModelNode(modelInstance = instance, scaleToUnits = 0.1f)
                }
            }
        }
    }
}`,
    },
    "ar-cloud-anchor": {
        id: "ar-cloud-anchor",
        title: "AR Cloud Anchor",
        description: "Host and resolve persistent cross-device anchors using ARCore Cloud Anchors.",
        tags: ["ar", "anchor", "cloud-anchor"],
        dependency: "io.github.sceneview:arsceneview:3.3.0",
        prompt: "Create an AR screen that can host a cloud anchor (saving its ID) and resolve it later on another device. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
        code: `@Composable
fun CloudAnchorScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var session by remember { mutableStateOf<Session?>(null) }
    var cloudAnchorNode by remember { mutableStateOf<CloudAnchorNode?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        sessionConfiguration = { _, config ->
            config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
        },
        onSessionCreated = { s -> session = s }
    ) {
        cloudAnchorNode?.let { node ->
            CloudAnchorNode(anchor = node.anchor, cloudAnchorId = node.cloudAnchorId)
        }
    }
    // Host: CloudAnchorNode(engine, anchor).host(session) { id, state -> ... }
    // Resolve: CloudAnchorNode.resolve(engine, session, cloudAnchorId) { state, node -> ... }
}`,
    },
    "ar-point-cloud": {
        id: "ar-point-cloud",
        title: "AR Point Cloud",
        description: "Visualizes ARCore feature points as 3D spheres with confidence-based filtering.",
        tags: ["ar", "point-cloud"],
        dependency: "io.github.sceneview:arsceneview:3.3.0",
        prompt: "Create an AR screen that visualizes ARCore feature points as small 3D spheres, filtered by confidence. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
        code: `@Composable
fun PointCloudScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var pointCount by remember { mutableIntStateOf(0) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = false,
        sessionConfiguration = { _, config ->
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        },
        onSessionUpdated = { _, frame ->
            frame.acquirePointCloud()?.use { cloud ->
                pointCount = cloud.ids?.limit() ?: 0
                // Process points: cloud.points buffer has [x, y, z, confidence] per point
            }
        }
    ) {
        // Render point cloud model instances at detected positions
    }
}`,
    },
    "gltf-camera": {
        id: "gltf-camera",
        title: "glTF Camera",
        description: "Extracts and uses camera definitions embedded in a glTF file for cinematic viewpoints.",
        tags: ["3d", "model", "camera"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene that loads a GLB file containing embedded camera definitions, then uses those cameras for cinematic viewpoints. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun GltfCameraScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/scene_with_cameras.glb")
    val cameraNode = rememberCameraNode(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr") ?: createEnvironment(environmentLoader)
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
        }
    }
}`,
    },
    "camera-manipulator": {
        id: "camera-manipulator",
        title: "Camera Manipulator",
        description: "Orbit, pan, and zoom camera with customizable sensitivity and bounds.",
        tags: ["3d", "camera", "gestures"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a fully configurable orbit camera — drag to rotate, two-finger pan, pinch to zoom. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun CameraManipulatorScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(z = 4f)
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = cameraNode.worldPosition,
            targetPosition = Position(0f)
        )
    ) {
        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
        }
    }
}`,
    },
    "autopilot-demo": {
        id: "autopilot-demo",
        title: "Autopilot Demo",
        description: "Full autonomous driving HUD with animated car, traffic lights, road, and real-time telemetry overlay.",
        tags: ["3d", "model", "animation", "geometry"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a Tesla FSD-style autopilot visualization with a 3D car on a road, traffic lights, and a HUD overlay showing speed, distance, and status. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun AutopilotScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader
    ) {
        // Road surface
        PlaneNode(engine, size = Size(6f, 0f, 50f),
            position = Position(y = -0.01f))

        // Ego car
        rememberModelInstance(modelLoader, "models/car.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 2f)
        }

        // Traffic light with state machine
        // See samples/autopilot-demo for the full implementation
    }
}`,
    },
    "physics-demo": {
        id: "physics-demo",
        title: "Physics Demo",
        description: "Interactive physics simulation with bouncing spheres, gravity, configurable restitution, and colour selection.",
        tags: ["3d", "physics", "geometry", "animation"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene where tapping spawns coloured spheres that fall under gravity and bounce off a floor. Add a bounciness slider. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun PhysicsDemoScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    var restitution by remember { mutableFloatStateOf(0.7f) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader
    ) {
        // Floor
        PlaneNode(engine, size = Size(4f, 0f, 4f),
            materialInstance = materialLoader.createColorInstance(Color.DarkGray))

        // Spawn spheres and attach PhysicsNode
        val sphere = remember(engine) {
            SphereNode(engine, radius = 0.15f).apply {
                position = Position(y = 3f)
            }
        }
        PhysicsNode(
            node = sphere,
            restitution = restitution,
            radius = 0.15f
        )
    }
}`,
    },
    "dynamic-sky": {
        id: "dynamic-sky",
        title: "Dynamic Sky",
        description: "Time-of-day sun cycle with DynamicSkyNode and atmospheric fog via FogNode.",
        tags: ["3d", "sky", "fog", "environment"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a time-of-day sun that moves from sunrise through noon to sunset, with atmospheric fog. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun DynamicSkyScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var timeOfDay by remember { mutableFloatStateOf(12f) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader
    ) {
        DynamicSkyNode(
            timeOfDay = timeOfDay,
            turbidity = 2f,
            sunIntensity = 110_000f
        )

        rememberModelInstance(modelLoader, "models/scene.glb")?.let { instance ->
            ModelNode(modelInstance = instance)
        }
    }
    // Add a Slider to control timeOfDay from 0 to 24
}`,
    },
    "line-path": {
        id: "line-path",
        title: "Line & Path",
        description: "Animated 3D line art with sine waves, Lissajous curves, and parameter sliders.",
        tags: ["3d", "lines", "geometry", "animation"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene that draws animated parametric curves (sine wave, Lissajous) using PathNode with amplitude and frequency sliders. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun LinePathScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    var amplitude by remember { mutableFloatStateOf(1f) }
    var frequency by remember { mutableFloatStateOf(2f) }

    val points = remember(amplitude, frequency) {
        (0..200).map { i ->
            val t = i / 200f * Math.PI.toFloat() * 4
            Position(x = t * 0.5f - 3f, y = sin(t * frequency) * amplitude, z = 0f)
        }
    }

    Scene(modifier = Modifier.fillMaxSize(), engine = engine, materialLoader = materialLoader) {
        val path = remember(engine, points) {
            PathNode(engine = engine, points = points)
        }
        Node(node = path)
    }
    // Add Sliders for amplitude and frequency
}`,
    },
    "text-labels": {
        id: "text-labels",
        title: "Text Labels",
        description: "Camera-facing 3D text labels (TextNode + BillboardNode) with interactive label cycling.",
        tags: ["3d", "text", "geometry"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with floating text labels that always face the camera. Labels show planet names and can be tapped to cycle display modes. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun TextLabelsScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    var cameraPos by remember { mutableStateOf(Position()) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        onFrame = { cameraPos = cameraNode.worldPosition }
    ) {
        TextNode(
            materialLoader = materialLoader,
            text = "Earth",
            fontSize = 48f,
            textColor = android.graphics.Color.WHITE,
            backgroundColor = 0xCC000000.toInt(),
            widthMeters = 0.6f,
            heightMeters = 0.2f,
            cameraPositionProvider = { cameraPos }
        )
    }
}`,
    },
    "reflection-probe": {
        id: "reflection-probe",
        title: "Reflection Probe",
        description: "Zone-based IBL overrides with material picker (Chrome, Gold, Copper, Rough) and probe toggle.",
        tags: ["3d", "reflection", "environment", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a metallic sphere and a ReflectionProbeNode that overrides the IBL. Add a material picker to switch between Chrome, Gold, Copper, and Rough. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun ReflectionProbeScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    var cameraPosition by remember { mutableStateOf(Position()) }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/studio.hdr") ?: createEnvironment(environmentLoader)
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = environment,
        onFrame = { cameraPosition = cameraNode.worldPosition }
    ) {
        ReflectionProbeNode(
            filamentScene = scene,
            environment = environment,
            cameraPosition = cameraPosition
        )

        rememberModelInstance(modelLoader, "models/sphere.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
        }
    }
}`,
    },
    "post-processing": {
        id: "post-processing",
        title: "Post-Processing",
        description: "Real-time post-processing effects: bloom, vignette, tone mapping, FXAA, and SSAO controls.",
        tags: ["3d", "post-processing", "environment"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with interactive post-processing controls for bloom, vignette, tone mapping, FXAA, and SSAO. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun PostProcessingScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val view = rememberView(engine)
    var bloomStrength by remember { mutableFloatStateOf(0.1f) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        view = view
    ) {
        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
        }
    }
    // Configure view.bloomOptions, view.vignetteOptions, etc.
    // See samples/post-processing for full interactive controls
}`,
    },
};
export const SAMPLE_IDS = Object.keys(SAMPLES);
export function getSample(id) {
    return SAMPLES[id];
}
