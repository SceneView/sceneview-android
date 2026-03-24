# SceneView Desktop

3D rendering for Windows, macOS, and Linux using **Compose Desktop** and **Filament** (via LWJGL/JNI).

## Status: Scaffold

The Compose Desktop UI framework and LWJGL OpenGL context are in place. Filament JNI desktop bindings are not yet available — this module serves as the architecture scaffold for future implementation.

## Architecture

```
Compose Desktop (UI) → LWJGL (OpenGL context) → Filament (C++ renderer via JNI)
                                                  ↑
                                          sceneview-core (KMP: math, collision, geometry)
```

## Run

```bash
./gradlew :sceneview-desktop:run
```

## Distribute

```bash
./gradlew :sceneview-desktop:packageDmg   # macOS
./gradlew :sceneview-desktop:packageMsi   # Windows
./gradlew :sceneview-desktop:packageDeb   # Linux
```

## What's Needed

1. **Filament JNI desktop bindings** — build Filament from source with JNI for desktop targets, or create a thin JNI layer loading the native `.so`/`.dylib`/`.dll`
2. **OpenGL/Vulkan surface sharing** — pipe LWJGL's OpenGL context to Filament's renderer
3. **Compose Canvas integration** — render Filament output into Compose's drawing layer

## Part of SceneView

SceneView is a declarative 3D/AR SDK for Android, iOS, macOS, visionOS, Web, and Desktop.

- [GitHub](https://github.com/SceneView/sceneview)
