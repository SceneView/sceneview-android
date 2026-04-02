/**
 * generate-environment.ts
 *
 * Generates code for HDR environment setup, dynamic sky, and lighting configurations.
 */
export const ENVIRONMENT_TYPES = [
    "hdr-environment",
    "dynamic-sky",
    "studio-lighting",
    "outdoor-lighting",
    "night-scene",
    "ar-lighting",
];
const ANDROID_ENVIRONMENTS = {
    "hdr-environment": {
        description: "Load an HDR environment map for physically-based image-based lighting",
        code: `@Composable
fun HDREnvironmentScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb")

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        mainLightNode = rememberMainLightNode(engine) {
            intensity = 100_000f
        },
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }
    }
}`,
        notes: [
            "Place HDR files in `src/main/assets/environments/`.",
            "Use 2K HDR files for mobile (4K wastes GPU memory).",
            "HDR environment provides both skybox (background) and IBL (reflections).",
            "Without IBL, metallic surfaces appear black.",
        ],
    },
    "dynamic-sky": {
        description: "Time-of-day dynamic sky with sun simulation",
        code: `@Composable
fun DynamicSkyScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/building.glb")

    var sunAngle by remember { mutableFloatStateOf(45f) }

    Column(modifier = Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            // DynamicSkyNode simulates a procedural sky with sun position
            DynamicSkyNode(
                engine = engine,
                sunAngle = sunAngle,
                turbidity = 4.0f, // atmospheric haze (2=clear, 10=overcast)
                groundAlbedo = 0.3f // ground reflectance
            )

            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 2.0f,
                    centerOrigin = Position(0f, 0f, 0f)
                )
            }

            LightNode(
                engine = engine,
                type = LightManager.Type.SUN,
                apply = {
                    intensity(110_000f)
                    castShadows(true)
                    direction(
                        -kotlin.math.cos(Math.toRadians(sunAngle.toDouble())).toFloat(),
                        -kotlin.math.sin(Math.toRadians(sunAngle.toDouble())).toFloat(),
                        0f
                    )
                }
            )
        }

        Slider(
            value = sunAngle,
            onValueChange = { sunAngle = it },
            valueRange = 0f..180f,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            "Sun angle: \${sunAngle.toInt()}°",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.White
        )
    }
}`,
        notes: [
            "DynamicSkyNode is a SceneScope composable — must be inside Scene { }.",
            "sunAngle: 0° = sunrise, 90° = noon, 180° = sunset.",
            "turbidity: 2 = crystal clear, 4 = average, 10 = overcast/hazy.",
            "Sync the LightNode direction with the sun angle for consistent shadows.",
        ],
    },
    "studio-lighting": {
        description: "Three-point studio lighting setup for product visualization",
        code: `@Composable
fun StudioLightingScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/product.glb")

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/studio_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Key light — main illumination from upper-left
        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(120_000f)
                direction(-0.5f, -1f, -0.5f)
                castShadows(true)
                color(1f, 0.98f, 0.95f) // slightly warm
            }
        )

        // Fill light — softer from the right
        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(40_000f)
                direction(0.7f, -0.5f, -0.3f)
                castShadows(false)
                color(0.9f, 0.95f, 1f) // slightly cool
            }
        )

        // Rim/back light — highlights edges
        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(60_000f)
                direction(0f, -0.3f, 1f)
                castShadows(false)
            }
        )

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }
    }
}`,
        notes: [
            "Three-point lighting: key (main), fill (secondary), rim (edge highlight).",
            "Key light at ~120K lux, fill at ~40K, rim at ~60K for product photography look.",
            "Use a studio HDR environment for realistic reflections on metallic surfaces.",
            "Adjust light colors for warm/cool mood (warm key + cool fill is classic).",
        ],
    },
    "outdoor-lighting": {
        description: "Outdoor scene with sunlight, sky, and atmospheric effects",
        code: `@Composable
fun OutdoorLightingScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/car.glb")

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("environments/outdoor_2k.hdr")
                ?: createEnvironment(environmentLoader)
        },
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Sun light
        LightNode(
            engine = engine,
            type = LightManager.Type.SUN,
            apply = {
                intensity(110_000f)
                direction(0f, -1f, -0.5f) // high noon, slightly forward
                castShadows(true)
                color(1f, 0.96f, 0.9f) // warm sunlight
            }
        )

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 2.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }

        // Ground plane for shadow reception
        val materialLoader = rememberMaterialLoader(engine)
        val groundMat = remember(materialLoader) {
            materialLoader.createColorInstance(Color(0.3f, 0.35f, 0.25f), roughness = 0.9f)
        }
        PlaneNode(
            size = Size(20f, 20f),
            materialInstance = groundMat
        )
    }
}`,
        notes: [
            "Use LightManager.Type.SUN for outdoor scenes (physically-based sun model).",
            "Sun intensity of ~110,000 lux matches real-world daylight.",
            "Add a ground plane for shadow reception.",
            "Use an outdoor HDR environment for sky reflections.",
        ],
    },
    "night-scene": {
        description: "Night/mood scene with point lights and dim ambient",
        code: `@Composable
fun NightSceneScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/scene.glb")

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        // Warm point light (like a lamp/candle)
        LightNode(
            engine = engine,
            type = LightManager.Type.POINT,
            apply = {
                intensity(50_000f)
                falloff(8f)
                castShadows(true)
                color(1f, 0.8f, 0.5f) // warm orange
            },
            position = Position(1f, 2f, 0f)
        )

        // Cool accent light
        LightNode(
            engine = engine,
            type = LightManager.Type.SPOT,
            apply = {
                intensity(30_000f)
                falloff(10f)
                castShadows(false)
                color(0.5f, 0.7f, 1f) // cool blue
                innerConeAngle(0.2f)
                outerConeAngle(0.5f)
            },
            position = Position(-2f, 3f, 1f)
        )

        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 2.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }
    }
}`,
        notes: [
            "Night scenes use POINT and SPOT lights instead of directional/sun.",
            "Lower intensities (30K-50K lux) create mood lighting.",
            "Use warm colors (orange/yellow) for lamps, cool colors (blue) for moonlight.",
            "falloff controls how far the light reaches (in meters).",
            "Spot lights need innerConeAngle and outerConeAngle (in radians).",
        ],
    },
    "ar-lighting": {
        description: "AR scene with environmental HDR light estimation",
        code: `@Composable
fun ARLightingScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/furniture.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            // ENVIRONMENTAL_HDR is the most realistic lighting mode
            // It captures real-world lighting and applies it to virtual objects
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        },
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
                        isEditable = true
                    )
                }
            }
        }
    }
}`,
        notes: [
            "ENVIRONMENTAL_HDR captures the real environment and applies it as IBL to virtual objects.",
            "This is the most realistic AR lighting mode but costs more CPU/GPU.",
            "For simpler lighting, use AMBIENT_INTENSITY (just brightness + color temperature).",
            "ARCore automatically adjusts directional light to match real shadows.",
        ],
    },
};
const IOS_ENVIRONMENTS = {
    "hdr-environment": {
        description: "RealityKit scene with environment lighting in SwiftUI",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct EnvironmentLightingView: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { root in
            if let model {
                root.addChild(model.entity)
            }
            // RealityKit uses built-in IBL automatically
            // For custom environments, use .environment(.lighting) modifiers
        }
        .cameraControls(.orbit)
        .task {
            do {
                model = try await ModelNode.load("models/helmet.usdz")
                model?.scaleToUnits(1.0)
            } catch {
                print("Failed to load: \\(error)")
            }
        }
    }
}`,
        notes: [
            "RealityKit provides built-in IBL automatically.",
            "For custom environment maps, use ImageBasedLightComponent on the anchor entity.",
            "USDZ models carry their own material properties that work with RealityKit's PBR pipeline.",
        ],
    },
    "ar-lighting": {
        description: "iOS AR with automatic environment lighting from ARKit",
        code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct ARLightingView: View {
    @State private var model: ModelNode?

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            showCoachingOverlay: true,
            onTapOnPlane: { position, arView in
                guard let model else { return }
                let anchor = AnchorNode.world(position: position)
                let clone = model.entity.clone(recursive: true)
                clone.scale = .init(repeating: 0.5)
                anchor.add(clone)
                arView.scene.addAnchor(anchor.entity)
            }
        )
        .edgesIgnoringSafeArea(.all)
        .task {
            do {
                model = try await ModelNode.load("models/furniture.usdz")
            } catch {
                print("Failed to load: \\(error)")
            }
        }
    }
}`,
        notes: [
            "ARKit automatically provides environment lighting estimation.",
            "RealityKit applies real-world lighting to virtual objects by default.",
            "No manual light estimation configuration needed on iOS.",
        ],
    },
};
export function generateEnvironmentCode(environmentType, platform = "android") {
    if (platform === "ios") {
        const iosEnv = IOS_ENVIRONMENTS[environmentType];
        if (iosEnv) {
            return {
                code: iosEnv.code,
                platform: "ios",
                environmentType,
                description: iosEnv.description,
                notes: iosEnv.notes,
            };
        }
        return {
            code: IOS_ENVIRONMENTS["hdr-environment"].code,
            platform: "ios",
            environmentType: "hdr-environment",
            description: `iOS equivalent for '${environmentType}' not available. Showing HDR environment instead.`,
            notes: [`The '${environmentType}' environment type is Android-specific. RealityKit handles lighting differently.`],
        };
    }
    const env = ANDROID_ENVIRONMENTS[environmentType];
    if (!env)
        return null;
    return {
        code: env.code,
        platform: "android",
        environmentType,
        description: env.description,
        notes: env.notes,
    };
}
export function formatEnvironmentCode(result) {
    const lang = result.platform === "ios" ? "swift" : "kotlin";
    const parts = [
        `## Environment: ${result.environmentType}`,
        `**Platform:** ${result.platform === "ios" ? "iOS (SwiftUI + RealityKit)" : "Android (Jetpack Compose + Filament)"}`,
        `**Description:** ${result.description}`,
        ``,
        `### Code`,
        ``,
        "```" + lang,
        result.code,
        "```",
        ``,
    ];
    if (result.notes.length > 0) {
        parts.push(`### Notes`);
        result.notes.forEach((n, i) => parts.push(`${i + 1}. ${n}`));
    }
    return parts.join("\n");
}
