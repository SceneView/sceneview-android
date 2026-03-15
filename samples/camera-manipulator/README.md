# Camera Manipulator

Demonstrates the built-in orbit/pan/zoom camera controller with a loaded glTF model.

## What it shows

- `rememberCameraManipulator` — one-line setup of Sketchfab-style orbit/pan/zoom
- Camera rig: a pivot `rememberNode` at the scene origin with the camera as a child
- `rememberCollisionSystem` — hit testing wired to gesture events
- `rememberOnGestureListener` — tap to select a node, double-tap to reset zoom
- Overlaying standard Compose UI on the 3D surface

## Gestures

| Gesture | Action |
|---|---|
| One-finger drag | Orbit around the model |
| Two-finger drag | Pan |
| Pinch | Zoom in/out |
| Double-tap | Reset camera to home position |

## Key APIs used

```kotlin
val view = rememberView(engine)
val collisionSystem = rememberCollisionSystem(view)

Scene(
    view = view,
    collisionSystem = collisionSystem,
    cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = cameraNode.worldPosition,
        targetPosition = centerNode.worldPosition
    )
) {
    modelInstance?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
}
```
