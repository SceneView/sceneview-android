# Advanced guide: Physics simulation

**Time:** ~15 minutes
**Level:** Intermediate
**What you'll build:** A scene with bouncing spheres affected by gravity

---

## Overview

SceneView 3.2.0 includes a lightweight, built-in physics simulation via `PhysicsNode`.
No external physics engine is required — gravity, floor collisions, and bounce are handled
automatically.

---

## Step 1 — Create a sphere and attach physics

```kotlin
@Composable
fun PhysicsScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/sky_2k.hdr")
            ?: createEnvironment(environmentLoader)
    }

    SceneView(
        engine = engine,
        modelLoader = modelLoader,
        environment = environment
    ) {
        // Create a sphere node
        val sphere = remember(engine) {
            SphereNode(engine, radius = 0.15f).apply {
                position = Position(y = 3f)
            }
        }

        // Attach physics — it will fall and bounce
        PhysicsNode(
            node = sphere,
            mass = 1f,
            restitution = 0.7f,          // 70% energy preserved per bounce
            linearVelocity = Position(),  // starts at rest, gravity pulls it down
            floorY = 0f,                  // bounce off Y = 0
            radius = 0.15f               // match the sphere radius
        )
    }
}
```

---

## Step 2 — Key parameters

| Parameter | Effect |
|---|---|
| `mass` | Object mass in kg (reserved for future force calculations) |
| `restitution` | Bounciness `[0, 1]`. `0` = no bounce, `1` = perfectly elastic |
| `linearVelocity` | Initial velocity in m/s — use `Position(x = 1f, y = 5f)` to launch at an angle |
| `floorY` | Y-coordinate of the ground plane |
| `radius` | Collision radius — offsets contact so the sphere surface touches the floor |

---

## Step 3 — Multiple objects with different properties

```kotlin
SceneView(engine = engine, modelLoader = modelLoader, environment = environment) {
    // Heavy bowling ball — low bounce
    val heavyBall = remember(engine) {
        SphereNode(engine, radius = 0.2f).apply {
            position = Position(x = -0.5f, y = 4f)
        }
    }
    PhysicsNode(node = heavyBall, mass = 5f, restitution = 0.2f, radius = 0.2f)

    // Rubber ball — high bounce
    val rubberBall = remember(engine) {
        SphereNode(engine, radius = 0.1f).apply {
            position = Position(x = 0.5f, y = 3f)
        }
    }
    PhysicsNode(node = rubberBall, mass = 0.5f, restitution = 0.9f, radius = 0.1f)
}
```

The physics system automatically:

- Applies gravity (9.8 m/s² downward)
- Detects floor collisions using the `floorY` + `radius` offset
- Puts objects to sleep once they stop moving

---

## What's next

- Combine with `ModelNode` to drop 3D models instead of primitive spheres
- Use `linearVelocity` to create projectile effects
- See the `physics-demo` sample for a complete interactive example with colour selection and a bounciness slider
