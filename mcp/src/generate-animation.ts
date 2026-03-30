/**
 * generate-animation.ts
 *
 * Generates compilable code for animation playback, morph targets,
 * transitions, and spring physics in SceneView (Android + iOS).
 */

export type AnimationType =
  | "model-playback"
  | "morph-targets"
  | "spring-position"
  | "spring-scale"
  | "compose-rotation"
  | "compose-scale"
  | "smooth-follow"
  | "transition";

export interface AnimationCodeResult {
  code: string;
  platform: "android" | "ios";
  animationType: AnimationType;
  description: string;
  notes: string[];
}

const ANDROID_ANIMATIONS: Record<AnimationType, { code: string; description: string; notes: string[] }> = {
  "model-playback": {
    description: "Play embedded glTF skeletal animation with autoAnimate or manual control",
    code: `@Composable
fun AnimatedModelScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/character.glb")

    SceneView(
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
                // autoAnimate plays the first embedded animation in a loop
                autoAnimate = true
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
            }
        )
    }
}`,
    notes: [
      "The GLB file must contain embedded animations (skeletal or morph target).",
      "Use `autoAnimate = true` for automatic looping of the first animation.",
      "For manual control, use the Animator API in an `onFrame` callback.",
    ],
  },

  "morph-targets": {
    description: "Animate morph targets (blend shapes) on a glTF model",
    code: `@Composable
fun MorphTargetScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/face.glb")
    var smileWeight by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "morph")
    val animatedWeight by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smile"
    )

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(),
        onFrame = { _ ->
            modelInstance?.let { instance ->
                // Apply morph target weights
                // Index 0 is typically the first morph target defined in the GLB
                instance.animator.let { animator ->
                    if (animator.morphTargetCount > 0) {
                        animator.setMorphTargetWeight(0, animatedWeight)
                    }
                }
            }
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
            apply = { intensity(100_000f) }
        )
    }
}`,
    notes: [
      "The GLB model must contain morph targets (also called blend shapes).",
      "Morph target indices correspond to the order defined in the glTF file.",
      "Check `animator.morphTargetCount` before applying weights.",
      "Weight range is 0.0 to 1.0.",
    ],
  },

  "spring-position": {
    description: "Spring-based position animation using KMP core SpringAnimation",
    code: `@Composable
fun SpringPositionDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/cube.glb")

    var targetY by remember { mutableFloatStateOf(0f) }
    val springY = remember {
        SpringAnimation(
            spring = Spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            initialValue = 0f
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator(),
            onFrame = { _ ->
                springY.target = targetY
                springY.advance(0.016f)
            }
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.5f,
                    position = Position(0f, springY.value, 0f)
                )
            }

            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = { intensity(100_000f) }
            )
        }

        Button(
            onClick = { targetY = if (targetY < 1f) 2f else 0f },
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Text("Bounce")
        }
    }
}`,
    notes: [
      "SpringAnimation is from the `sceneview-core` KMP module.",
      "Spring presets: StiffnessHigh (snappy), StiffnessMedium (general), StiffnessLow (heavy bounce).",
      "DampingRatio: NoBouncy (1.0), LowBouncy (0.75), MediumBouncy (0.5), HighBouncy (0.2).",
    ],
  },

  "spring-scale": {
    description: "Spring-based scale animation for bouncy resize effects",
    code: `@Composable
fun SpringScaleDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/star.glb")

    var isExpanded by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 2.0f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator(),
            onTouchEvent = { event, _ ->
                if (event.action == MotionEvent.ACTION_UP) {
                    isExpanded = !isExpanded
                }
                true
            }
        ) {
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = scale,
                    centerOrigin = Position(0f, 0f, 0f)
                )
            }

            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = { intensity(100_000f) }
            )
        }

        Text(
            "Tap to \${if (isExpanded) "shrink" else "expand"}",
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            color = Color.White,
            fontSize = 18.sp
        )
    }
}`,
    notes: [
      "Uses Compose's built-in `animateFloatAsState` with spring spec.",
      "Tap anywhere on the scene to toggle between expanded and normal size.",
      "The spring animation gives a natural bouncy feel.",
    ],
  },

  "compose-rotation": {
    description: "Continuous rotation animation using Compose InfiniteTransition",
    code: `@Composable
fun RotatingModelScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/shoe.glb")

    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotY"
    )

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                rotation = Rotation(0f, rotationY, 0f),
                centerOrigin = Position(0f, 0f, 0f)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = {
                intensity(100_000f)
                castShadows(true)
            }
        )
    }
}`,
    notes: [
      "Uses Compose's InfiniteTransition for smooth, battery-efficient rotation.",
      "Rotation is in degrees around the Y axis (turntable style).",
      "Change `durationMillis` to control rotation speed.",
    ],
  },

  "compose-scale": {
    description: "Animate scale with Compose animateFloatAsState for toggle effects",
    code: `@Composable
fun ScaleToggleScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/trophy.glb")

    var showModel by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (showModel) 1.0f else 0.01f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "scale"
    )

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = scale,
                centerOrigin = Position(0f, 0f, 0f)
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = { intensity(100_000f) }
        )
    }
}`,
    notes: [
      "Animate scale to near-zero (0.01f) instead of removing the node for smooth disappear.",
      "Combine with `animateColorAsState` for material color transitions.",
    ],
  },

  "smooth-follow": {
    description: "Smooth interpolation for a node following a moving target",
    code: `@Composable
fun SmoothFollowDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, "models/drone.glb")

    val smoothPosition = remember { SmoothTransform(smoothTime = 0.3f) }
    var targetPosition by remember { mutableStateOf(Position(0f, 1f, 0f)) }
    var time by remember { mutableFloatStateOf(0f) }

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(),
        onFrame = { frameTimeNanos ->
            time += 0.016f
            // Move target in a circle
            targetPosition = Position(
                kotlin.math.sin(time) * 2f,
                1f + kotlin.math.sin(time * 2f) * 0.5f,
                kotlin.math.cos(time) * 2f
            )
            smoothPosition.target = targetPosition
            smoothPosition.advance(0.016f)
        }
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 0.3f,
                position = smoothPosition.current
            )
        }

        LightNode(
            engine = engine,
            type = LightManager.Type.DIRECTIONAL,
            apply = { intensity(100_000f) }
        )
    }
}`,
    notes: [
      "SmoothTransform is from the `sceneview-core` KMP module.",
      "Ideal for camera follow, target tracking, or any smooth interpolation.",
      "`smoothTime` controls how quickly the node catches up (lower = faster).",
    ],
  },

  "transition": {
    description: "Animated state transition between two model positions",
    code: `@Composable
fun TransitionDemo() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelA = rememberModelInstance(modelLoader, "models/chair.glb")
    val modelB = rememberModelInstance(modelLoader, "models/table.glb")

    var isSlotA by remember { mutableStateOf(true) }
    val positionX by animateFloatAsState(
        targetValue = if (isSlotA) -1.5f else 1.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "posX"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            engine = engine,
            modelLoader = modelLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            modelA?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    position = Position(positionX, 0f, 0f),
                    centerOrigin = Position(0f, 0f, 0f)
                )
            }

            modelB?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    position = Position(-positionX, 0f, 0f),
                    centerOrigin = Position(0f, 0f, 0f)
                )
            }

            LightNode(
                engine = engine,
                type = LightManager.Type.DIRECTIONAL,
                apply = { intensity(100_000f) }
            )
        }

        Button(
            onClick = { isSlotA = !isSlotA },
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Text("Swap Positions")
        }
    }
}`,
    notes: [
      "Uses Compose spring animation for smooth position transitions.",
      "Both models animate simultaneously in opposite directions.",
      "Adjust spring parameters for different feels.",
    ],
  },
};

const IOS_ANIMATIONS: Record<string, { code: string; description: string; notes: string[] }> = {
  "model-playback": {
    description: "Play embedded USDZ animation in SwiftUI",
    code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct AnimatedModelView: View {
    @State private var model: ModelNode?

    var body: some View {
        SceneView { root in
            if let model {
                root.addChild(model.entity)
                // RealityKit plays animations automatically if present
                model.entity.availableAnimations.forEach { animation in
                    model.entity.playAnimation(animation.repeat())
                }
            }
        }
        .cameraControls(.orbit)
        .task {
            do {
                model = try await ModelNode.load("models/character.usdz")
                model?.scaleToUnits(1.0)
            } catch {
                print("Failed to load model: \\(error)")
            }
        }
    }
}`,
    notes: [
      "USDZ files can embed animations. RealityKit plays them via `playAnimation()`.",
      "Use `.repeat()` for looping, `.repeat(count:)` for finite repeats.",
      "Check `entity.availableAnimations` to list all embedded animations.",
    ],
  },

  "spring-position": {
    description: "Spring-based position animation in SwiftUI with withAnimation",
    code: `import SwiftUI
import SceneViewSwift
import RealityKit

struct SpringPositionView: View {
    @State private var model: ModelNode?
    @State private var isUp = false

    var body: some View {
        VStack {
            SceneView { root in
                if let model {
                    root.addChild(model.entity)
                    let y: Float = isUp ? 1.5 : 0.0
                    model.entity.position = .init(x: 0, y: y, z: 0)
                }
            }
            .cameraControls(.orbit)

            Button("Bounce") {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.4, blendDuration: 0)) {
                    isUp.toggle()
                }
            }
            .padding()
        }
        .task {
            do {
                model = try await ModelNode.load("models/cube.usdz")
                model?.scaleToUnits(0.5)
            } catch {
                print("Failed to load: \\(error)")
            }
        }
    }
}`,
    notes: [
      "SwiftUI's `withAnimation(.spring(...))` animates state changes.",
      "For RealityKit entity transforms, use `move(to:relativeTo:duration:)` for smooth transitions.",
      "Adjust `response` (speed) and `dampingFraction` (bounciness) for different spring feels.",
    ],
  },
};

export function generateAnimationCode(
  animationType: AnimationType,
  platform: "android" | "ios" = "android"
): AnimationCodeResult | null {
  if (platform === "ios") {
    const iosAnim = IOS_ANIMATIONS[animationType];
    if (iosAnim) {
      return {
        code: iosAnim.code,
        platform: "ios",
        animationType,
        description: iosAnim.description,
        notes: iosAnim.notes,
      };
    }
    // Fallback: not all types have iOS equivalents
    return {
      code: IOS_ANIMATIONS["model-playback"].code,
      platform: "ios",
      animationType: "model-playback",
      description: `iOS equivalent for '${animationType}' not available. Showing model-playback instead.`,
      notes: [`The '${animationType}' animation type is Android-specific. Use RealityKit's native animation APIs for iOS.`],
    };
  }

  const anim = ANDROID_ANIMATIONS[animationType];
  if (!anim) return null;
  return {
    code: anim.code,
    platform: "android",
    animationType,
    description: anim.description,
    notes: anim.notes,
  };
}

export const ANIMATION_TYPES: AnimationType[] = [
  "model-playback",
  "morph-targets",
  "spring-position",
  "spring-scale",
  "compose-rotation",
  "compose-scale",
  "smooth-follow",
  "transition",
];

export function formatAnimationCode(result: AnimationCodeResult): string {
  const lang = result.platform === "ios" ? "swift" : "kotlin";
  const parts: string[] = [
    `## Animation: ${result.animationType}`,
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
