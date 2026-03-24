# PhysicsNode — Real-Time 3D Physics in 10 Lines of Compose

*Tags: Android, iOS, JetpackCompose, SwiftUI, Physics, Kotlin, Swift, 3D*

---

Here's a challenge: add real-time gravity, bouncing balls, and floor collision to a 3D scene — no third-party physics engine, no C++ code, no XML.

With SceneView 3.3.0, it's 10 lines of Compose on Android — and just as simple on iOS with SwiftUI and RealityKit.

## The result

By the end of this article, you'll have a scene where:

- Tapping the screen spawns a ball at height
- Each ball falls under gravity (9.8 m/s²)
- Balls bounce off a floor with configurable bounciness
- Old balls disappear automatically when the scene gets crowded
- Zero imperative lifecycle management

## Setup (30 seconds)

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:3.3.0")
```

No extra physics libraries. No JNI dependencies. On Android, physics is implemented in pure Kotlin — Euler integration, ~160 lines of code, zero overhead when a body is asleep. On iOS, `PhysicsNode` leverages RealityKit's built-in physics engine for native Metal-accelerated simulation.

## The complete scene

```kotlin
@Composable
fun PhysicsDemoScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader)

    // Fixed camera — slightly above and behind the scene
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 1.5f, z = 4f)
        lookAt(Position(0f, 0.5f, 0f))
    }

    // Physics state — each SphereNode is driven by a PhysicsBody
    val balls = remember { mutableStateListOf<SphereNode>() }
    var ballCount by remember { mutableStateOf(0) }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        environment = environment,
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, _ ->
                val index = ballCount++
                val sign = if (index % 2 == 0) 1f else -1f
                val ball = SphereNode(engine = engine, radius = 0.15f).apply {
                    position = Position(x = sign * 0.3f, y = 2.5f, z = 0f)
                }
                balls.add(ball)
                if (balls.size > 10) balls.removeAt(0)
                true
            }
        )
    ) {
        // Floor slab
        CubeNode(
            size = Size(6f, 0.1f, 6f),
            position = Position(y = -0.05f)
        )

        // One PhysicsNode per ball — that's the whole simulation
        for ((index, ball) in balls.withIndex()) {
            val sign = if (index % 2 == 0) 1f else -1f
            Node(apply = { addChildNode(ball) })
            PhysicsNode(
                node = ball,
                restitution = 0.65f,
                linearVelocity = Position(x = sign * 0.4f, y = 0f, z = 0f),
                floorY = 0f,
                radius = 0.15f
            )
        }
    }
}
```

That's it. Spawn balls, attach physics, watch them fall and bounce. The `PhysicsNode` composable handles everything: registering the `onFrame` callback, stepping the Euler integration, detecting floor contact, and cleaning up when the ball leaves the composition.

## How it works

### PhysicsNode is just a composable

`PhysicsNode` isn't a special engine object — it's a `@Composable` function that attaches a `PhysicsBody` to any existing node via `node.onFrame`:

```kotlin
@Composable
fun PhysicsNode(
    node: Node,
    mass: Float = 1f,
    restitution: Float = 0.6f,
    linearVelocity: Position = Position(0f, 0f, 0f),
    floorY: Float = 0f,
    radius: Float = 0f
)
```

Pass a `SphereNode`, `CubeNode`, or any `ModelNode` — the physics simulation drives its `.position` every frame. When the composable leaves the composition (ball removed from `balls`), `DisposableEffect` clears the `onFrame` hook automatically.

### Euler integration — no magic

Each frame, `PhysicsBody.step()` does three things:

1. Add `gravity × dt` to the Y velocity
2. Integrate the new position: `pos += velocity × dt`
3. If the sphere's bottom surface crosses `floorY`, clamp it and reflect the Y velocity multiplied by `restitution`

When the rebound speed drops below 0.05 m/s, the body goes to sleep and stops consuming CPU entirely.

### State-driven ball lifecycle

Balls live in a `mutableStateListOf<SphereNode>()`. Composable `for` loops over that list — adding a ball triggers recomposition, which adds a `Node` + `PhysicsNode` pair to the scene. Removing a ball from the list tears down both composables and their callbacks.

No manual cleanup. No memory leaks.

## Going further

### Vary the restitution for different materials

```kotlin
// Rubber ball
PhysicsNode(node = ball, restitution = 0.85f, radius = 0.15f)

// Lead shot
PhysicsNode(node = ball, restitution = 0.1f, radius = 0.08f)
```

### Throw a 3D model, not a sphere

```kotlin
val modelInstance = rememberModelInstance(modelLoader, "models/toy_ball.glb")
modelInstance?.let { instance ->
    val modelNode = remember(engine) {
        ModelNode(modelInstance = instance, scaleToUnits = 0.3f)
    }
    Node(apply = { addChildNode(modelNode) })
    PhysicsNode(node = modelNode, restitution = 0.7f, radius = 0.15f)
}
```

`PhysicsNode` works on any `Node` subclass — spheres, cubes, GLB models, even `TextNode`s.

## iOS: PhysicsNode with RealityKit

On iOS, `PhysicsNode` wraps RealityKit's native physics engine. The API follows the same pattern:

```swift
import SceneViewSwift

struct PhysicsDemoView: View {
    var body: some View {
        SceneView {
            // Floor
            GeometryNode(.box(width: 6, height: 0.1, depth: 6))
                .position(y: -0.05)

            // Bouncing ball with physics
            PhysicsNode(mass: 1.0, restitution: 0.65) {
                GeometryNode(.sphere(radius: 0.15))
                    .position(y: 2.5)
            }
        }
    }
}
```

RealityKit handles the physics simulation natively on Metal — collision detection, gravity, and bounce are all GPU-accelerated. Same concept as Android, native performance on Apple hardware.

### Elevated floor

The `floorY` parameter places the collision plane anywhere in world space:

```kotlin
PhysicsNode(node = ball, floorY = 1.0f, radius = 0.15f)
// Ball bounces on a surface 1 metre above the scene origin
```

## The full sample

The complete physics demo — with a Material 3 top bar, hint text, and per-ball coloured materials — is in the SceneView repository:

`samples/physics-demo/`

```
git clone https://github.com/SceneView/sceneview
./gradlew :samples:physics-demo:installDebug
```

---

*Next: [TextNode & BillboardNode — Adding Labels to Your AR Scene →](#)*

*SceneView is open-source (Apache 2.0). Now cross-platform: Android + iOS + macOS + visionOS. Star the repo if this was useful: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)*
