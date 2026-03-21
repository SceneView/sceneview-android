# Physics Demo

Tap-to-spawn bouncing balls with simple gravity and collision physics.

## What it demonstrates

- Spawning 3D objects at runtime in response to tap gestures
- Pure-Kotlin physics simulation (gravity, bounce, restitution) via `PhysicsNode`
- Managing a dynamic list of scene nodes with automatic cleanup (max 10 balls)
- Floor collision using a `CubeNode` slab aligned with the physics ground plane

## Key APIs

- `PhysicsNode` — attaches Euler-integration physics (gravity, restitution, floor bounce) to any node
- `SphereNode` — procedural sphere geometry
- `CubeNode` — procedural box geometry used as the floor
- `rememberOnGestureListener` — tap detection for spawning balls on `onSingleTapConfirmed`

## Run

```bash
./gradlew :samples:physics-demo:installDebug
```
