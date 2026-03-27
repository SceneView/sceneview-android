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
    "ar-face-mesh": {
        id: "ar-face-mesh",
        title: "AR Face Mesh",
        description: "AR face tracking with AugmentedFaceNode — applies a textured mesh overlay to detected faces using the front camera.",
        tags: ["ar", "face-tracking", "model"],
        dependency: "io.github.sceneview:arsceneview:3.3.0",
        prompt: "Create an AR screen that uses the front camera to detect faces and overlay a 3D mesh on them. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
        code: `@Composable
fun ARFaceMeshScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    var trackedFaces by remember { mutableStateOf(listOf<AugmentedFace>()) }

    val faceMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = Color(0.8f, 0.6f, 0.4f, 0.5f),
            metallic = 0f,
            roughness = 0.9f
        )
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
            trackedFaces = session.getAllTrackables(AugmentedFace::class.java)
                .filter { it.trackingState == TrackingState.TRACKING }
        }
    ) {
        trackedFaces.forEach { face ->
            AugmentedFaceNode(
                augmentedFace = face,
                meshMaterialInstance = faceMaterial
            )
        }
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
    "camera-animation": {
        id: "camera-animation",
        title: "Camera Animation",
        description: "Animated camera flythrough around a 3D model — smooth orbit using LaunchedEffect and trigonometric interpolation.",
        tags: ["3d", "camera", "animation", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a camera that automatically orbits around a model in a smooth circle. Include a play/pause button. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun CameraAnimationScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    var isOrbiting by remember { mutableStateOf(true) }
    var angle by remember { mutableFloatStateOf(0f) }

    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 1.5f, z = 4f)
        lookAt(Position(0f, 0f, 0f))
    }

    // Animate camera orbit
    LaunchedEffect(isOrbiting) {
        while (isOrbiting) {
            withFrameNanos { _ ->
                angle += 0.5f
                val radians = Math.toRadians(angle.toDouble())
                cameraNode.position = Position(
                    x = (4f * sin(radians)).toFloat(),
                    y = 1.5f,
                    z = (4f * cos(radians)).toFloat()
                )
                cameraNode.lookAt(Position(0f, 0f, 0f))
            }
        }
    }

    Column {
        Scene(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            environment = rememberEnvironment(environmentLoader) {
                environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                    ?: createEnvironment(environmentLoader)
            },
            mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f }
        ) {
            rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
                ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
            }
        }
        Button(
            onClick = { isOrbiting = !isOrbiting },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
        ) {
            Text(if (isOrbiting) "Stop Orbit" else "Start Orbit")
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
    "video-texture": {
        id: "video-texture",
        title: "Video Texture",
        description: "Video playback on a 3D plane using VideoNode with MediaPlayer — supports looping, chroma-key, and auto-sizing.",
        tags: ["3d", "video", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a video playing on a floating 3D plane. Include play/pause controls and chroma-key support. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun VideoTextureScreen() {
    val context = LocalContext.current
    val engine = rememberEngine()
    var isPlaying by remember { mutableStateOf(true) }

    val player = remember {
        MediaPlayer().apply {
            setDataSource(context, Uri.parse("android.resource://\${context.packageName}/raw/video"))
            isLooping = true
            prepare()
            start()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Column {
        Scene(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine
        ) {
            VideoNode(
                player = player,
                // size = null auto-sizes from video aspect ratio (longer edge = 1 unit)
                position = Position(z = -2f),
                chromaKeyColor = null // set to android.graphics.Color.GREEN for green-screen
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                if (isPlaying) player.pause() else player.start()
                isPlaying = !isPlaying
            }) {
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}`,
    },
    "multi-model-scene": {
        id: "multi-model-scene",
        title: "Multi-Model Scene",
        description: "Scene with multiple 3D models loaded independently, positioned and scaled to create a complete environment.",
        tags: ["3d", "model", "multi-model", "environment"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene that loads multiple GLB models (a car, a building, and trees) and positions them to form a street scene. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun MultiModelScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 3f, z = 8f),
            targetPosition = Position(0f, 0f, 0f)
        ),
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f }
    ) {
        // Ground plane
        val groundMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.DarkGray, roughness = 0.9f)
        }
        PlaneNode(size = Size(20f, 20f), materialInstance = groundMat)

        // Car in the center
        rememberModelInstance(modelLoader, "models/car.glb")?.let { car ->
            ModelNode(
                modelInstance = car,
                scaleToUnits = 2.0f,
                position = Position(x = 0f, y = 0f, z = 0f),
                autoAnimate = true
            )
        }

        // Building on the left
        rememberModelInstance(modelLoader, "models/building.glb")?.let { building ->
            ModelNode(
                modelInstance = building,
                scaleToUnits = 5.0f,
                position = Position(x = -6f, y = 0f, z = -3f)
            )
        }

        // Trees along the right side
        for (i in 0..2) {
            rememberModelInstance(modelLoader, "models/tree.glb")?.let { tree ->
                ModelNode(
                    modelInstance = tree,
                    scaleToUnits = 3.0f,
                    position = Position(x = 5f, y = 0f, z = i * -3f)
                )
            }
        }
    }
}`,
    },
    "gesture-interaction": {
        id: "gesture-interaction",
        title: "Gesture Interaction",
        description: "Full gesture handling — tap to select, double-tap to scale, long-press for info, pinch-to-scale, drag-to-move on editable nodes.",
        tags: ["3d", "gestures", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with a model that responds to tap (select), double-tap (scale up), long-press (show info), and supports pinch-to-scale and drag-to-move. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun GestureInteractionScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    var selectedNode by remember { mutableStateOf<String?>(null) }
    var infoText by remember { mutableStateOf("Tap a model to select it") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator(),
            environment = rememberEnvironment(environmentLoader) {
                environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                    ?: createEnvironment(environmentLoader)
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { event, node ->
                    selectedNode = node?.name
                    infoText = if (node != null) "Selected: \${node.name}" else "Tap a model to select it"
                },
                onDoubleTap = { event, node ->
                    node?.let {
                        it.scale = if (it.scale.x > 1.5f) Scale(1f) else Scale(2f)
                        infoText = "Double-tap: toggled scale"
                    }
                },
                onLongPress = { event, node ->
                    node?.let {
                        infoText = "Position: \${it.worldPosition}, Scale: \${it.scale}"
                    }
                }
            )
        ) {
            rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    isEditable = true, // enables pinch-to-scale and drag-to-move
                    autoAnimate = true
                )
            }
        }

        // Info overlay
        Text(
            text = infoText,
            modifier = Modifier.align(Alignment.TopCenter).padding(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}`,
    },
    "environment-lighting": {
        id: "environment-lighting",
        title: "Environment & Lighting",
        description: "Complete lighting setup — HDR environment (IBL + skybox), main directional light, point light, and spot light with LightNode.",
        tags: ["3d", "environment", "lighting", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with full HDR environment lighting (IBL + skybox), a directional sun light, a red point light, and a blue spot light. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun EnvironmentLightingScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 2f, z = 5f),
            targetPosition = Position(0f, 0f, 0f)
        ),
        // HDR environment provides both IBL (indirect lighting) and skybox (background)
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        // Main directional light (sun)
        mainLightNode = rememberMainLightNode(engine) {
            intensity = 100_000f
            // castShadows is true by default for the main light
        }
    ) {
        // Floor to receive shadows
        val floorMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.LightGray, roughness = 0.8f)
        }
        PlaneNode(size = Size(10f, 10f), materialInstance = floorMat)

        // Model
        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, position = Position(y = 0.5f))
        }

        // Red point light on the left
        LightNode(
            type = LightManager.Type.POINT,
            apply = {
                color(1.0f, 0.2f, 0.2f)
                intensity(200_000f)
                falloff(5.0f)
            },
            position = Position(x = -2f, y = 2f, z = 1f)
        )

        // Blue spot light on the right
        LightNode(
            type = LightManager.Type.SPOT,
            apply = {
                color(0.2f, 0.4f, 1.0f)
                intensity(300_000f)
                falloff(8.0f)
                castShadows(true)
            },
            position = Position(x = 2f, y = 3f, z = 1f)
        )
    }
}`,
    },
    "procedural-geometry": {
        id: "procedural-geometry",
        title: "Procedural Geometry",
        description: "Procedural shapes — CubeNode, SphereNode, CylinderNode, PlaneNode — with PBR materials (metallic, roughness, color).",
        tags: ["3d", "geometry", "model"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene showing procedural geometry shapes (cube, sphere, cylinder, plane) with different PBR materials. No model files needed. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun ProceduralGeometryScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 2f, z = 6f),
            targetPosition = Position(0f, 0.5f, 0f)
        ),
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        mainLightNode = rememberMainLightNode(engine) { intensity = 100_000f }
    ) {
        // Floor
        val floorMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.DarkGray, roughness = 0.9f)
        }
        PlaneNode(size = Size(8f, 8f), materialInstance = floorMat)

        // Red matte cube
        val redMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Red, metallic = 0f, roughness = 0.6f)
        }
        CubeNode(
            size = Size(0.6f),
            center = Position(0f, 0.3f, 0f),
            materialInstance = redMat,
            position = Position(x = -2f)
        )

        // Chrome sphere
        val chromeMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Gray, metallic = 1f, roughness = 0.05f, reflectance = 0.9f)
        }
        SphereNode(
            radius = 0.4f,
            materialInstance = chromeMat,
            position = Position(x = -0.7f, y = 0.4f)
        )

        // Green cylinder
        val greenMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Green, metallic = 0.2f, roughness = 0.4f)
        }
        CylinderNode(
            radius = 0.25f,
            height = 0.8f,
            materialInstance = greenMat,
            position = Position(x = 0.7f, y = 0.4f)
        )

        // Gold sphere
        val goldMat = remember(materialLoader) {
            materialLoader.createColorInstance(
                Color(1f, 0.84f, 0f),
                metallic = 1f,
                roughness = 0.3f
            )
        }
        SphereNode(
            radius = 0.35f,
            materialInstance = goldMat,
            position = Position(x = 2f, y = 0.35f)
        )
    }
}`,
    },
    "compose-ui-3d": {
        id: "compose-ui-3d",
        title: "Compose UI in 3D",
        description: "Embed interactive Jetpack Compose UI (Cards, Buttons, Text) inside 3D space using ViewNode.",
        tags: ["3d", "compose-ui", "text"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D scene with interactive Compose UI elements (Card with text and a button) floating in 3D space using ViewNode. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun ComposeUI3DScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val windowManager = rememberViewNodeManager()
    var clickCount by remember { mutableIntStateOf(0) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(),
        viewNodeWindowManager = windowManager
    ) {
        // 3D model behind the UI
        rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let { instance ->
            ModelNode(modelInstance = instance, scaleToUnits = 1.0f, position = Position(z = -1f))
        }

        // Floating Compose Card in 3D space
        ViewNode(
            windowManager = windowManager,
            position = Position(x = 0f, y = 1.2f, z = 0.5f)
        ) {
            Card(
                modifier = Modifier.width(200.dp).padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hello 3D World!", style = MaterialTheme.typography.titleMedium)
                    Text("Clicks: \$clickCount", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { clickCount++ }) {
                        Text("Click Me")
                    }
                }
            }
        }
    }
}`,
    },
    "node-hierarchy": {
        id: "node-hierarchy",
        title: "Node Hierarchy",
        description: "Parent-child node relationships — a spinning solar system with planet groups orbiting a central sun.",
        tags: ["3d", "hierarchy", "geometry", "animation"],
        dependency: "io.github.sceneview:sceneview:3.3.0",
        prompt: "Create a 3D solar system where planets orbit a sun using parent-child node hierarchies. Each planet group rotates independently. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
        code: `@Composable
fun NodeHierarchyScreen() {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    var earthAngle by remember { mutableFloatStateOf(0f) }
    var marsAngle by remember { mutableFloatStateOf(0f) }

    // Animate planet orbits
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                earthAngle += 0.3f
                marsAngle += 0.18f
            }
        }
    }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = Position(x = 0f, y = 4f, z = 8f),
            targetPosition = Position(0f, 0f, 0f)
        ),
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        }
    ) {
        // Sun (center)
        val sunMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color.Yellow, metallic = 0f, roughness = 1f)
        }
        SphereNode(radius = 0.5f, materialInstance = sunMat)

        // Earth orbit group — parent node rotates, child offset creates orbit
        Node(rotation = Rotation(y = earthAngle)) {
            // Earth sphere
            val earthMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color.Blue, metallic = 0f, roughness = 0.7f)
            }
            SphereNode(radius = 0.2f, materialInstance = earthMat, position = Position(x = 2.5f))

            // Moon orbits Earth (nested hierarchy)
            Node(position = Position(x = 2.5f), rotation = Rotation(y = earthAngle * 3f)) {
                val moonMat = remember(materialLoader) {
                    materialLoader.createColorInstance(Color.LightGray, metallic = 0f, roughness = 0.9f)
                }
                SphereNode(radius = 0.06f, materialInstance = moonMat, position = Position(x = 0.4f))
            }
        }

        // Mars orbit group
        Node(rotation = Rotation(y = marsAngle)) {
            val marsMat = remember(materialLoader) {
                materialLoader.createColorInstance(Color.Red, metallic = 0f, roughness = 0.8f)
            }
            SphereNode(radius = 0.15f, materialInstance = marsMat, position = Position(x = 4f))
        }

        // Sun light
        LightNode(
            type = LightManager.Type.POINT,
            apply = {
                color(1.0f, 0.95f, 0.8f)
                intensity(500_000f)
                falloff(15.0f)
            }
        )
    }
}`,
    },
    // ─── iOS Samples ────────────────────────────────────────────────────────────
    "ios-model-viewer": {
        id: "ios-model-viewer",
        title: "iOS 3D Model Viewer",
        description: "SwiftUI 3D scene with a USDZ model, IBL environment, orbit camera, and animation playback.",
        tags: ["3d", "model", "environment", "camera", "animation", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI screen that loads a USDZ model and displays it with IBL lighting, orbit camera, and animation playback. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
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
        .onEntityTapped { entity in
            print("Tapped: \\(entity)")
        }
        .task {
            do {
                model = try await ModelNode.load("models/car.usdz")
                model?.scaleToUnits(1.0)
                model?.playAllAnimations()
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`,
    },
    "ios-ar-model-viewer": {
        id: "ios-ar-model-viewer",
        title: "iOS AR Tap-to-Place Model Viewer",
        description: "AR scene with plane detection. Tap a surface to place a 3D model using ARKit + RealityKit.",
        tags: ["ar", "model", "anchor", "plane-detection", "placement", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create an iOS AR screen that detects surfaces and lets the user tap to place a USDZ model. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct ARModelViewerScreen: View {
    @State private var model: ModelNode?

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            showCoachingOverlay: true,
            onTapOnPlane: { position, arView in
                guard let model else { return }
                let anchor = AnchorNode.world(position: position)
                let clone = model.entity.clone(recursive: true)
                clone.scale = .init(repeating: 0.3)
                anchor.add(clone)
                arView.scene.addAnchor(anchor.entity)
            }
        )
        .edgesIgnoringSafeArea(.all)
        .task {
            do {
                model = try await ModelNode.load("models/robot.usdz")
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`,
    },
    "ios-ar-augmented-image": {
        id: "ios-ar-augmented-image",
        title: "iOS AR Augmented Image",
        description: "Detects reference images in the camera feed and overlays 3D content above them using ARKit.",
        tags: ["ar", "model", "image-tracking", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create an iOS AR screen that detects a printed reference image and places a 3D model above it. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit
import ARKit

struct AugmentedImageScreen: View {
    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            imageTrackingDatabase: AugmentedImageNode.createImageDatabase([
                AugmentedImageNode.ReferenceImage(
                    name: "poster",
                    image: UIImage(named: "poster_reference")!,
                    physicalWidth: 0.3  // 30 cm
                )
            ]),
            onImageDetected: { imageName, anchor, arView in
                // Place a spinning cube above the detected image
                let cube = GeometryNode.cube(size: 0.08, color: .systemBlue)
                    .position(.init(x: 0, y: 0.06, z: 0))
                anchor.add(cube.entity)
                arView.scene.addAnchor(anchor.entity)
                print("Detected image: \\(imageName)")
            }
        )
        .edgesIgnoringSafeArea(.all)
    }
}`,
    },
    "ios-geometry-shapes": {
        id: "ios-geometry-shapes",
        title: "iOS Procedural Geometry",
        description: "Procedural geometry shapes — cube, sphere, cylinder, cone, and plane — with PBR materials.",
        tags: ["3d", "geometry", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI scene showing procedural geometry shapes (cube, sphere, cylinder, cone, plane) with different materials. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct GeometryShapesScreen: View {
    var body: some View {
        SceneView { root in
            // Red cube
            let cube = GeometryNode.cube(size: 0.3, color: .red, cornerRadius: 0.02)
                .position(.init(x: -0.8, y: 0, z: 0))
            root.addChild(cube.entity)

            // Metallic sphere
            let sphere = GeometryNode.sphere(
                radius: 0.2,
                material: .pbr(color: .gray, metallic: 1.0, roughness: 0.2)
            )
            .position(.init(x: -0.3, y: 0, z: 0))
            root.addChild(sphere.entity)

            // Green cylinder
            let cylinder = GeometryNode.cylinder(
                radius: 0.15, height: 0.4, color: .green
            )
            .position(.init(x: 0.2, y: 0, z: 0))
            root.addChild(cylinder.entity)

            // Blue cone
            let cone = GeometryNode.cone(
                height: 0.4, radius: 0.2, color: .systemBlue
            )
            .position(.init(x: 0.7, y: 0, z: 0))
            root.addChild(cone.entity)

            // Floor plane
            let floor = GeometryNode.plane(
                width: 3.0, depth: 3.0, color: .darkGray
            )
            .position(.init(x: 0, y: -0.25, z: 0))
            root.addChild(floor.entity)
        }
        .cameraControls(.orbit)
    }
}`,
    },
    "ios-lighting": {
        id: "ios-lighting",
        title: "iOS Lighting",
        description: "Directional, point, and spot lights with configurable intensity, color, and shadows.",
        tags: ["3d", "lighting", "environment", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI 3D scene with directional, point, and spot lights illuminating geometry. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct LightingScreen: View {
    var body: some View {
        SceneView { root in
            // Ground plane
            let floor = GeometryNode.plane(
                width: 4.0, depth: 4.0, color: .lightGray
            )
            root.addChild(floor.entity)

            // Metallic sphere to show reflections
            let sphere = GeometryNode.sphere(
                radius: 0.3,
                material: .pbr(color: .white, metallic: 0.8, roughness: 0.3)
            )
            .position(.init(x: 0, y: 0.3, z: 0))
            root.addChild(sphere.entity)

            // Directional light (sun) with shadows
            let sun = LightNode.directional(
                color: .warm,
                intensity: 1500,
                castsShadow: true
            )
            sun.entity.look(at: .zero, from: [3, 5, 3], relativeTo: nil)
            root.addChild(sun.entity)

            // Point light (red)
            let pointLight = LightNode.point(
                color: .custom(r: 1.0, g: 0.2, b: 0.2),
                intensity: 5000,
                attenuationRadius: 5.0
            )
            .position(.init(x: -1.0, y: 1.0, z: 0.5))
            root.addChild(pointLight.entity)

            // Spot light (blue)
            let spotLight = LightNode.spot(
                color: .custom(r: 0.2, g: 0.4, b: 1.0),
                intensity: 8000,
                innerAngle: .pi / 8,
                outerAngle: .pi / 4,
                attenuationRadius: 8.0
            )
            .position(.init(x: 1.0, y: 2.0, z: 0.5))
            spotLight.entity.look(at: .zero, from: spotLight.entity.position, relativeTo: nil)
            root.addChild(spotLight.entity)
        }
        .cameraControls(.orbit)
    }
}`,
    },
    "ios-physics": {
        id: "ios-physics",
        title: "iOS Physics Demo",
        description: "Interactive physics simulation with bouncing spheres, gravity, and configurable restitution.",
        tags: ["3d", "physics", "geometry", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI 3D scene where tapping spawns coloured spheres that fall under gravity and bounce off a floor. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct PhysicsDemoScreen: View {
    @State private var sphereColors: [SimpleMaterial.Color] = [
        .red, .blue, .green, .orange, .purple, .yellow
    ]
    @State private var spawnCount = 0

    var body: some View {
        SceneView { root in
            // Static floor
            let floor = GeometryNode.plane(
                width: 4.0, depth: 4.0, color: .darkGray
            )
            PhysicsNode.static(floor.entity, restitution: 0.8)
            root.addChild(floor.entity)

            // Spawn initial spheres at different heights
            for i in 0..<6 {
                let color = sphereColors[i % sphereColors.count]
                let sphere = GeometryNode.sphere(radius: 0.15, color: color)
                    .position(.init(
                        x: Float(i % 3) * 0.5 - 0.5,
                        y: Float(i / 3) * 1.0 + 1.5,
                        z: 0
                    ))
                    .withGroundingShadow()
                PhysicsNode.dynamic(
                    sphere.entity,
                    mass: 1.0,
                    restitution: 0.7,
                    friction: 0.3
                )
                root.addChild(sphere.entity)
            }

            // Walls to keep spheres in bounds
            let wallLeft = GeometryNode.cube(size: 0.05, color: .clear)
                .position(.init(x: -2, y: 1, z: 0))
            wallLeft.entity.scale = .init(x: 0.05, y: 4, z: 4)
            PhysicsNode.static(wallLeft.entity)
            root.addChild(wallLeft.entity)

            let wallRight = GeometryNode.cube(size: 0.05, color: .clear)
                .position(.init(x: 2, y: 1, z: 0))
            wallRight.entity.scale = .init(x: 0.05, y: 4, z: 4)
            PhysicsNode.static(wallRight.entity)
            root.addChild(wallRight.entity)
        }
        .cameraControls(.orbit)
    }
}`,
    },
    "ios-text-labels": {
        id: "ios-text-labels",
        title: "iOS 3D Text Labels",
        description: "Camera-facing 3D text labels using TextNode and BillboardNode for always-facing-camera behavior.",
        tags: ["3d", "text", "geometry", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI 3D scene with floating text labels that always face the camera, showing planet names. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct TextLabelsScreen: View {
    let planets: [(name: String, color: SimpleMaterial.Color, position: SIMD3<Float>)] = [
        ("Earth", .systemBlue, .init(x: -1.0, y: 0, z: 0)),
        ("Mars", .systemRed, .init(x: 0, y: 0, z: 0)),
        ("Venus", .systemOrange, .init(x: 1.0, y: 0, z: 0)),
    ]

    var body: some View {
        SceneView { root in
            for planet in planets {
                // Planet sphere
                let sphere = GeometryNode.sphere(
                    radius: 0.2, color: planet.color
                )
                .position(planet.position)
                root.addChild(sphere.entity)

                // Billboard text label above the planet
                let label = BillboardNode.text(
                    planet.name,
                    fontSize: 0.04,
                    color: .white
                )
                .position(.init(
                    x: planet.position.x,
                    y: planet.position.y + 0.35,
                    z: planet.position.z
                ))
                root.addChild(label.entity)
            }
        }
        .cameraControls(.orbit)
    }
}`,
    },
    "ios-video-player": {
        id: "ios-video-player",
        title: "iOS Video on 3D Surface",
        description: "Video playback on a 3D plane using VideoNode with play/pause controls.",
        tags: ["3d", "video", "ios", "swift"],
        dependency: "https://github.com/sceneview/sceneview — from: \"3.3.0\"",
        spmDependency: "https://github.com/sceneview/sceneview",
        prompt: "Create a SwiftUI 3D scene with a video playing on a floating 3D plane. Include play/pause controls. Use SceneViewSwift.",
        language: "swift",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct VideoPlayerScreen: View {
    @State private var videoNode: VideoNode?
    @State private var isPlaying = false

    var body: some View {
        ZStack {
            SceneView { root in
                if let videoNode {
                    root.addChild(videoNode.entity)
                }
            }
            .cameraControls(.orbit)
            .onAppear {
                videoNode = VideoNode.load(
                    "videos/intro.mp4",
                    width: 1.6,
                    height: 0.9,
                    loop: true
                )
                .position(.init(x: 0, y: 0.5, z: -2))
            }

            // Play/Pause overlay
            VStack {
                Spacer()
                HStack(spacing: 30) {
                    Button(action: {
                        videoNode?.play()
                        isPlaying = true
                    }) {
                        Image(systemName: "play.fill")
                            .font(.title)
                    }
                    Button(action: {
                        videoNode?.pause()
                        isPlaying = false
                    }) {
                        Image(systemName: "pause.fill")
                            .font(.title)
                    }
                    Button(action: {
                        videoNode?.stop()
                        isPlaying = false
                    }) {
                        Image(systemName: "stop.fill")
                            .font(.title)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(16)
                .padding(.bottom, 40)
            }
        }
    }
}`,
    },
    // ── Web Samples (Kotlin/JS + Filament.js) ──────────────────────────────
    "web-model-viewer": {
        id: "web-model-viewer",
        title: "Web 3D Model Viewer",
        description: "Browser-based 3D model viewer using Filament.js (WebGL2/WASM) — same engine as SceneView Android",
        tags: ["3d", "model", "web", "filament-js"],
        dependency: "@sceneview/sceneview-web",
        language: "kotlin-js",
        prompt: "Create a browser-based 3D model viewer using SceneView Web (Kotlin/JS + Filament.js). Load a GLB model with camera and lighting.",
        code: `import io.github.sceneview.web.SceneView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

fun main() {
    val canvas = document.getElementById("scene-canvas") as HTMLCanvasElement
    canvas.width = canvas.clientWidth
    canvas.height = canvas.clientHeight

    SceneView.create(
        canvas = canvas,
        configure = {
            camera {
                eye(0.0, 1.5, 5.0)
                target(0.0, 0.0, 0.0)
                fov(45.0)
            }
            light {
                directional()
                intensity(100_000.0)
                direction(0.0f, -1.0f, -0.5f)
            }
            model("models/DamagedHelmet.glb")
        },
        onReady = { sceneView ->
            sceneView.startRendering()

            window.addEventListener("resize", {
                canvas.width = canvas.clientWidth
                canvas.height = canvas.clientHeight
            })
        }
    )
}`,
    },
    "web-environment": {
        id: "web-environment",
        title: "Web Environment Lighting",
        description: "Browser 3D scene with IBL environment lighting and skybox from KTX files",
        tags: ["3d", "environment", "web", "filament-js", "lighting"],
        dependency: "@sceneview/sceneview-web",
        language: "kotlin-js",
        prompt: "Create a browser 3D viewer with HDR environment lighting (IBL + skybox) using SceneView Web and Filament.js.",
        code: `import io.github.sceneview.web.SceneView
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

fun main() {
    val canvas = document.getElementById("scene-canvas") as HTMLCanvasElement
    canvas.width = canvas.clientWidth
    canvas.height = canvas.clientHeight

    SceneView.create(
        canvas = canvas,
        configure = {
            camera {
                eye(0.0, 1.0, 4.0)
                target(0.0, 0.0, 0.0)
                fov(50.0)
                exposure(16.0, 1.0 / 125.0, 100.0)
            }
            // Environment from KTX IBL + skybox files
            environment(
                iblUrl = "environments/pillars_2k_ibl.ktx",
                skyboxUrl = "environments/pillars_2k_skybox.ktx"
            )
            model("models/DamagedHelmet.glb")
        },
        onReady = { sceneView ->
            sceneView.startRendering()
        }
    )
}`,
    },
};
export const SAMPLE_IDS = Object.keys(SAMPLES);
export function getSample(id) {
    return SAMPLES[id];
}
