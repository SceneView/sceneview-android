# Physics Demo

Tap anywhere to throw balls that fall under gravity and bounce off the floor — a pure-Kotlin physics simulation.

## What it demonstrates
- `PhysicsNode` — Euler-integration physics driving node position each frame (gravity, velocity, restitution)
- `SphereNode` and `CubeNode` — built-in primitive geometry (no model files needed)
- Tap gesture spawning via `onGestureListener` with `onSingleTapConfirmed`
- `SnapshotStateList` to trigger recomposition when balls are added/removed
- Performance management by capping the maximum number of simultaneous objects

## Key code

```kotlin
Scene(
    modifier = Modifier.fillMaxSize(),
    onGestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { event, _ ->
            val ball = SphereNode(engine = engine, radius = 0.15f)
            balls.add(ball)
            if (balls.size > MAX_BALLS) balls.removeAt(0)
            true
        }
    )
) {
    // Floor
    CubeNode(size = Size(6f, 0.1f, 6f), position = Position(y = -0.05f))

    // Physics-driven balls
    for (ball in balls) {
        Node(apply = { addChildNode(ball) })
        PhysicsNode(
            node = ball,
            mass = 1f,
            restitution = 0.65f,
            linearVelocity = Position(x = lateralSpeed, y = 0f, z = 0f),
            floorY = 0f,
            radius = 0.15f
        )
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:physics-demo` configuration.
