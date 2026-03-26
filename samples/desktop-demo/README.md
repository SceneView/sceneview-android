# SceneView Desktop Demo

> **Status: Software Renderer Placeholder**
>
> This demo does **NOT** use SceneView or Filament. It is a standalone Compose Desktop
> app that draws wireframe 3D shapes using `Canvas` (Compose 2D drawing) with manual
> perspective projection. It exists as a UI/UX placeholder for a future Filament JNI
> desktop integration.

## What it actually does

- Draws rotating wireframe shapes (cube, octahedron, diamond) using Compose `Canvas`
- Uses basic trigonometry for 3D rotation and perspective projection
- Depends on `sceneview-core` (KMP math module) but does not use it for rendering
- No textures, no PBR materials, no glTF loading, no shadows, no scene graph

## What it does NOT do

- Does **not** use Filament (no GPU-accelerated rendering)
- Does **not** use the SceneView `Scene { }` composable
- Does **not** load 3D models (`.glb`, `.gltf`)
- Does **not** share any rendering code with the Android or Web SceneView modules

## Why it exists

It demonstrates that Compose Desktop works as a UI framework for a future 3D viewer.
The LWJGL dependencies are declared in `build.gradle.kts` but unused -- they are
scaffolding for a future Filament JNI integration.

See [`docs/docs/desktop-filament.md`](../../docs/docs/desktop-filament.md) for the
research document on what a real Filament Desktop integration would require (estimated
18-29 days of work).

## Run

```bash
./gradlew :samples:desktop-demo:run
```

## Future: Filament JNI Desktop

When Filament JNI desktop binaries become available (must be built from source -- not
published to Maven Central), this demo would be replaced with a real `Scene { }` composable
using the same API as Android. See the research doc for details.
