export type SampleId =
  | "model-viewer"
  | "ar-model-viewer"
  | "ar-augmented-image"
  | "ar-cloud-anchor"
  | "ar-point-cloud"
  | "gltf-camera"
  | "camera-manipulator"
  | "autopilot-demo"
  | "physics-demo"
  | "dynamic-sky"
  | "line-path"
  | "text-labels"
  | "reflection-probe"
  | "post-processing"
  | "ios-model-viewer"
  | "ios-ar-model-viewer"
  | "ios-ar-augmented-image"
  | "ios-geometry-shapes"
  | "ios-lighting"
  | "ios-physics"
  | "ios-text-labels"
  | "ios-video-player";

export type SampleTag =
  | "3d"
  | "ar"
  | "model"
  | "geometry"
  | "animation"
  | "camera"
  | "environment"
  | "anchor"
  | "plane-detection"
  | "image-tracking"
  | "cloud-anchor"
  | "point-cloud"
  | "placement"
  | "gestures"
  | "physics"
  | "sky"
  | "fog"
  | "lines"
  | "text"
  | "reflection"
  | "post-processing"
  | "ios"
  | "swift"
  | "video"
  | "lighting";

export interface Sample {
  id: SampleId;
  title: string;
  description: string;
  tags: SampleTag[];
  dependency: string;
  /** Optional SPM dependency URL for iOS samples */
  spmDependency?: string;
  prompt: string;
  code: string;
  /** Programming language of the sample code */
  language?: "kotlin" | "swift";
}

export const SAMPLES: Record<SampleId, Sample> = {
  "model-viewer": {
    id: "model-viewer",
    title: "3D Model Viewer",
    description:
      "Full-screen 3D scene with a GLB model, HDR environment, orbit camera, and animation controls",
    tags: ["3d", "model", "environment", "camera", "animation"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create an Android Compose screen that loads a GLB model and displays it with HDR lighting, orbit camera, and animation playback. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "AR scene with plane detection. Tap a surface to place a 3D model with pinch-to-scale and drag-to-rotate gestures.",
    tags: ["ar", "model", "anchor", "plane-detection", "placement", "gestures"],
    dependency: "io.github.sceneview:arsceneview:3.3.0",
    prompt:
      "Create an AR screen that detects surfaces and lets the user tap to place a GLB model. Support pinch-to-scale and drag-to-rotate. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
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
    description:
      "Detects reference images in the camera feed and overlays 3D models or video above them.",
    tags: ["ar", "model", "image-tracking"],
    dependency: "io.github.sceneview:arsceneview:3.3.0",
    prompt:
      "Create an AR screen that detects a printed reference image and places a 3D model above it. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
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
    description:
      "Host and resolve persistent cross-device anchors using ARCore Cloud Anchors.",
    tags: ["ar", "anchor", "cloud-anchor"],
    dependency: "io.github.sceneview:arsceneview:3.3.0",
    prompt:
      "Create an AR screen that can host a cloud anchor (saving its ID) and resolve it later on another device. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
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
    description:
      "Visualizes ARCore feature points as 3D spheres with confidence-based filtering.",
    tags: ["ar", "point-cloud"],
    dependency: "io.github.sceneview:arsceneview:3.3.0",
    prompt:
      "Create an AR screen that visualizes ARCore feature points as small 3D spheres, filtered by confidence. Use SceneView `io.github.sceneview:arsceneview:3.3.0`.",
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
    description:
      "Extracts and uses camera definitions embedded in a glTF file for cinematic viewpoints.",
    tags: ["3d", "model", "camera"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene that loads a GLB file containing embedded camera definitions, then uses those cameras for cinematic viewpoints. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Orbit, pan, and zoom camera with customizable sensitivity and bounds.",
    tags: ["3d", "camera", "gestures"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene with a fully configurable orbit camera — drag to rotate, two-finger pan, pinch to zoom. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Full autonomous driving HUD with animated car, traffic lights, road, and real-time telemetry overlay.",
    tags: ["3d", "model", "animation", "geometry"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a Tesla FSD-style autopilot visualization with a 3D car on a road, traffic lights, and a HUD overlay showing speed, distance, and status. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Interactive physics simulation with bouncing spheres, gravity, configurable restitution, and colour selection.",
    tags: ["3d", "physics", "geometry", "animation"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene where tapping spawns coloured spheres that fall under gravity and bounce off a floor. Add a bounciness slider. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Time-of-day sun cycle with DynamicSkyNode and atmospheric fog via FogNode.",
    tags: ["3d", "sky", "fog", "environment"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene with a time-of-day sun that moves from sunrise through noon to sunset, with atmospheric fog. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Animated 3D line art with sine waves, Lissajous curves, and parameter sliders.",
    tags: ["3d", "lines", "geometry", "animation"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene that draws animated parametric curves (sine wave, Lissajous) using PathNode with amplitude and frequency sliders. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Camera-facing 3D text labels (TextNode + BillboardNode) with interactive label cycling.",
    tags: ["3d", "text", "geometry"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene with floating text labels that always face the camera. Labels show planet names and can be tapped to cycle display modes. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Zone-based IBL overrides with material picker (Chrome, Gold, Copper, Rough) and probe toggle.",
    tags: ["3d", "reflection", "environment", "model"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene with a metallic sphere and a ReflectionProbeNode that overrides the IBL. Add a material picker to switch between Chrome, Gold, Copper, and Rough. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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
    description:
      "Real-time post-processing effects: bloom, vignette, tone mapping, FXAA, and SSAO controls.",
    tags: ["3d", "post-processing", "environment"],
    dependency: "io.github.sceneview:sceneview:3.3.0",
    prompt:
      "Create a 3D scene with interactive post-processing controls for bloom, vignette, tone mapping, FXAA, and SSAO. Use SceneView `io.github.sceneview:sceneview:3.3.0`.",
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

  // ─── iOS Samples ────────────────────────────────────────────────────────────

  "ios-model-viewer": {
    id: "ios-model-viewer",
    title: "iOS 3D Model Viewer",
    description:
      "SwiftUI 3D scene with a USDZ model, IBL environment, orbit camera, and animation playback.",
    tags: ["3d", "model", "environment", "camera", "animation", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI screen that loads a USDZ model and displays it with IBL lighting, orbit camera, and animation playback. Use SceneViewSwift.",
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
    description:
      "AR scene with plane detection. Tap a surface to place a 3D model using ARKit + RealityKit.",
    tags: ["ar", "model", "anchor", "plane-detection", "placement", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create an iOS AR screen that detects surfaces and lets the user tap to place a USDZ model. Use SceneViewSwift.",
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
    description:
      "Detects reference images in the camera feed and overlays 3D content above them using ARKit.",
    tags: ["ar", "model", "image-tracking", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create an iOS AR screen that detects a printed reference image and places a 3D model above it. Use SceneViewSwift.",
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
    description:
      "Procedural geometry shapes — cube, sphere, cylinder, cone, and plane — with PBR materials.",
    tags: ["3d", "geometry", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI scene showing procedural geometry shapes (cube, sphere, cylinder, cone, plane) with different materials. Use SceneViewSwift.",
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
    description:
      "Directional, point, and spot lights with configurable intensity, color, and shadows.",
    tags: ["3d", "lighting", "environment", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI 3D scene with directional, point, and spot lights illuminating geometry. Use SceneViewSwift.",
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
    description:
      "Interactive physics simulation with bouncing spheres, gravity, and configurable restitution.",
    tags: ["3d", "physics", "geometry", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI 3D scene where tapping spawns coloured spheres that fall under gravity and bounce off a floor. Use SceneViewSwift.",
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
    description:
      "Camera-facing 3D text labels using TextNode and BillboardNode for always-facing-camera behavior.",
    tags: ["3d", "text", "geometry", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI 3D scene with floating text labels that always face the camera, showing planet names. Use SceneViewSwift.",
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
    description:
      "Video playback on a 3D plane using VideoNode with play/pause controls.",
    tags: ["3d", "video", "ios", "swift"],
    dependency: "https://github.com/SceneView/sceneview — from: \"3.3.0\"",
    spmDependency: "https://github.com/SceneView/sceneview",
    prompt:
      "Create a SwiftUI 3D scene with a video playing on a floating 3D plane. Include play/pause controls. Use SceneViewSwift.",
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
};

export const SAMPLE_IDS = Object.keys(SAMPLES) as SampleId[];

export function getSample(id: string): Sample | undefined {
  return SAMPLES[id as SampleId];
}
