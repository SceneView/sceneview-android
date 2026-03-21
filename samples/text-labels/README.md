# Text Labels

Camera-facing text labels floating above interactive 3D objects.

## What it demonstrates

- Rendering billboard text in 3D space with `TextNode` that always faces the camera
- Tap interaction on 3D objects (`onSingleTapConfirmed`) to cycle label text
- Auto-orbiting camera using `Transition.animateRotation` with an infinite transition
- Combining `SphereNode` geometry with floating `TextNode` labels

## Key APIs

- `TextNode` — renders text as a camera-facing quad with configurable font size, colors, and dimensions
- `SphereNode` — procedural sphere with `isTouchable` for tap detection
- `Transition.animateRotation` — smooth infinite camera orbit around the scene
- `rememberOnGestureListener` — scene-level gesture handling
- `materialLoader.createColorInstance(color: Int)` — PBR material from packed ARGB color

## Run

```bash
./gradlew :samples:text-labels:installDebug
```
