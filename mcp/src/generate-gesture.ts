/**
 * generate-gesture.ts
 *
 * Generates compilable gesture/interaction code for SceneView.
 */

export const GESTURE_TYPES = [
  "tap-to-select",
  "drag-to-rotate",
  "pinch-to-scale",
  "tap-to-place-ar",
  "editable-model",
  "multi-select",
  "surface-cursor",
  "custom-touch",
] as const;

export type GestureType = (typeof GESTURE_TYPES)[number];

interface GestureTemplate {
  title: string;
  description: string;
  android: string;
  ios?: string;
}

const GESTURE_TEMPLATES: Record<GestureType, GestureTemplate> = {
  "tap-to-select": {
    title: "Tap to Select Node",
    description: "Tap on a 3D model to select it and show a visual highlight.",
    android: `@Composable
fun TapToSelectScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var selectedNode by remember { mutableStateOf<String?>(null) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP) {
                selectedNode = hitResult?.node?.name
            }
            true
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }

    // Show selection UI
    selectedNode?.let { name ->
        Text(
            text = "Selected: $name",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}`,
    ios: `import SwiftUI
import SceneViewSwift

struct TapToSelectScene: View {
    @State private var selectedEntity: Entity?

    var body: some View {
        SceneView { root in
            // Content loaded in .task
        }
        .onTapGesture { location in
            // RealityKit handles entity selection via tap
        }
        .overlay(alignment: .bottom) {
            if let entity = selectedEntity {
                Text("Selected: \\(entity.name)")
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(8)
                    .padding()
            }
        }
    }
}`,
  },

  "drag-to-rotate": {
    title: "Drag to Rotate Model",
    description: "Drag on the model to rotate it around its Y axis with sensitivity control.",
    android: `@Composable
fun DragToRotateScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var rotationY by remember { mutableFloatStateOf(0f) }
    var lastX by remember { mutableFloatStateOf(0f) }
    val sensitivity = 0.5f

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onTouchEvent = { event, _ ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastX
                    rotationY += dx * sensitivity
                    lastX = event.x
                    true
                }
                else -> false
            }
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f),
                rotation = Rotation(y = rotationY)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
    ios: `import SwiftUI
import SceneViewSwift

struct DragToRotateScene: View {
    @State private var rotationY: Float = 0
    @State private var lastDragX: CGFloat = 0

    var body: some View {
        SceneView { root in
            // Load model in .task
        }
        .gesture(
            DragGesture()
                .onChanged { value in
                    let dx = Float(value.translation.width - lastDragX) * 0.5
                    rotationY += dx
                    lastDragX = value.translation.width
                }
                .onEnded { _ in
                    lastDragX = 0
                }
        )
    }
}`,
  },

  "pinch-to-scale": {
    title: "Pinch to Scale Model",
    description: "Two-finger pinch gesture to scale the model with min/max limits.",
    android: `@Composable
fun PinchToScaleScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    val minScale = 0.3f
    val maxScale = 3.0f

    Scene(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Detect pinch with two pointers
                    var prevSpan = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val p1 = event.changes[0].position
                            val p2 = event.changes[1].position
                            val span = (p1 - p2).getDistance()
                            if (prevSpan > 0f) {
                                val ratio = span / prevSpan
                                scaleFactor = (scaleFactor * ratio).coerceIn(minScale, maxScale)
                            }
                            prevSpan = span
                            event.changes.forEach { it.consume() }
                        } else {
                            break
                        }
                    }
                }
            },
        engine = engine,
        modelLoader = modelLoader
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = scaleFactor,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "tap-to-place-ar": {
    title: "AR Tap to Place",
    description: "Tap on a detected AR plane to place a 3D model at that position.",
    android: `@Composable
fun TapToPlaceARScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null) {
                // Detach old anchor to free ARCore resources
                anchor?.detach()
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
                        isEditable = true  // Enables pinch-to-scale + drag-to-rotate
                    )
                }
            }
        }
    }
}`,
    ios: `import SwiftUI
import SceneViewSwift
import RealityKit

struct TapToPlaceARScene: View {
    @State private var model: ModelEntity?

    var body: some View {
        ARSceneView(
            planeDetection: .horizontal,
            showCoachingOverlay: true,
            onTapOnPlane: { position, arView in
                guard let model else { return }
                let anchor = AnchorEntity(world: position)
                let clone = model.clone(recursive: true)
                clone.scale = .init(repeating: 0.5)
                anchor.addChild(clone)
                arView.scene.addAnchor(anchor)
            }
        )
        .edgesIgnoringSafeArea(.all)
        .task {
            do {
                model = try await ModelEntity.load(named: "models/object.usdz")
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`,
  },

  "editable-model": {
    title: "Editable Model (One-Line Gestures)",
    description: "Use isEditable = true to get pinch-to-scale, drag-to-rotate, and tap-to-select with zero extra code.",
    android: `@Composable
fun EditableModelScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0f, 0f, 0f),
                // This single flag enables:
                // - Pinch to scale
                // - Drag to rotate
                // - Tap to select
                isEditable = true
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "multi-select": {
    title: "Multi-Model Selection",
    description: "Tap to select/deselect multiple models with visual feedback.",
    android: `@Composable
fun MultiSelectScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    val selectedNodes = remember { mutableStateListOf<String>() }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP) {
                hitResult?.node?.name?.let { name ->
                    if (selectedNodes.contains(name)) {
                        selectedNodes.remove(name)
                    } else {
                        selectedNodes.add(name)
                    }
                }
            }
            true
        }
    ) {
        // Place multiple copies
        val positions = listOf(
            Position(-1.5f, 0f, 0f),
            Position(0f, 0f, 0f),
            Position(1.5f, 0f, 0f),
        )
        positions.forEachIndexed { index, pos ->
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = if ("model_$index" in selectedNodes) 1.2f else 1.0f,
                    centerOrigin = Position(0f, 0f, 0f),
                    position = pos
                )
            }
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                direction(0f, -1f, -0.5f)
            }
        )
    }
}`,
  },

  "surface-cursor": {
    title: "AR Surface Cursor (HitResultNode)",
    description: "Show a cursor that follows the surface detected by AR plane detection.",
    android: `@Composable
fun SurfaceCursorARScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cursorModel = rememberModelInstance(modelLoader, "models/cursor.glb")
    val objectModel = rememberModelInstance(modelLoader, "models/object.glb")
    var anchor by remember { mutableStateOf<Anchor?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        },
        onTouchEvent = { event, hitResult ->
            if (event.action == MotionEvent.ACTION_UP && hitResult != null) {
                anchor = hitResult.createAnchor()
            }
            true
        }
    ) {
        // Surface cursor follows the center of the screen
        HitResultNode(engine = engine) {
            cursorModel?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.1f
                )
            }
        }

        // Placed object
        anchor?.let { a ->
            AnchorNode(anchor = a) {
                objectModel?.let { instance ->
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

  "custom-touch": {
    title: "Custom Touch Handler",
    description: "Full control over touch events with hit testing and custom behavior.",
    android: `@Composable
fun CustomTouchScene() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")
    var touchInfo by remember { mutableStateOf("Touch a model") }
    var modelScale by remember { mutableFloatStateOf(1.0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = touchInfo,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Scene(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            onTouchEvent = { event, hitResult ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (hitResult?.node != null) {
                            touchInfo = "Touching: \${hitResult.node?.name} at (\${hitResult.distance}m)"
                            modelScale = 1.1f  // Slight grow on press
                        } else {
                            touchInfo = "Touched empty space at (\${event.x}, \${event.y})"
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        modelScale = 1.0f  // Reset on release
                        true
                    }
                    else -> false
                }
            }
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = modelScale,
                    centerOrigin = Position(0f, 0f, 0f)
                )
            }

            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = {
                    intensity(100_000f)
                    direction(0f, -1f, -0.5f)
                }
            )
        }
    }
}`,
  },
};

export function generateGestureCode(
  gestureType: GestureType,
  platform: "android" | "ios" = "android"
): { code: string; title: string; description: string } | null {
  const template = GESTURE_TEMPLATES[gestureType];
  if (!template) return null;

  const code = platform === "ios" && template.ios ? template.ios : template.android;
  return { code, title: template.title, description: template.description };
}

export function formatGestureCode(result: {
  code: string;
  title: string;
  description: string;
}, platform: "android" | "ios"): string {
  const lang = platform === "ios" ? "swift" : "kotlin";
  const platLabel = platform === "ios" ? "iOS (SwiftUI + RealityKit)" : "Android (Jetpack Compose)";
  return [
    `## ${result.title}`,
    ``,
    `**Platform:** ${platLabel}`,
    ``,
    result.description,
    ``,
    `\`\`\`${lang}`,
    result.code,
    `\`\`\``,
    ``,
    `### Available Gesture Types`,
    ``,
    ...GESTURE_TYPES.map((t) => `- \`${t}\``),
  ].join("\n");
}
