# Line Path

3D line and path rendering with an animated sine wave.

## What it demonstrates

- Drawing individual line segments in 3D space with `LineNode`
- Rendering multi-point paths with `PathNode` (spiral helix)
- Animating path geometry by updating point positions each frame (sine wave with moving phase)
- Building an axis gizmo (X/Y/Z) from colored line segments

## Key APIs

- `LineNode` — single line segment between two 3D points with a material
- `PathNode` — polyline through a list of `Position` points, open or closed
- `materialLoader.createColorInstance` — solid-color PBR material from a Compose `Color`
- `rememberInfiniteTransition` — Compose animation driving the sine wave phase

## Run

```bash
./gradlew :samples:line-path:installDebug
```
