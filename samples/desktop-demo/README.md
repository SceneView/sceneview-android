# SceneView Desktop

3D rendering for Windows, macOS, and Linux using **Compose Desktop** and **Filament** (JNI).

## Status: Scaffold (Filament JNI integration planned)

## Architecture

```
Compose Desktop (UI)
  └── SwingPanel { FilamentPanel }  ← Filament's official Swing integration
        └── Filament Engine (C++ via JNI)
              ├── filament-java.jar      ← Java API (same classes as Android)
              ├── filament-jni.dylib/so/dll  ← Native library per platform
              └── sceneview-core (KMP)   ← Shared math, collision, geometry
```

**Key insight:** Filament has official JNI desktop bindings (`filament-java.jar` +
`FilamentCanvas`/`FilamentPanel`). The Java API is the SAME as Android —
`Engine`, `Scene`, `View`, `Renderer`, `Camera` are identical classes.
This means most of SceneView's Android Kotlin code could be reused on desktop.

## How It Will Work

1. **Filament init:** `Filament.init()` loads the native library
2. **AWT/Swing panel:** `FilamentPanel` creates an OpenGL surface for Filament
3. **Compose integration:** Compose Desktop's `SwingPanel` embeds the FilamentPanel
4. **Same API:** `Engine.create()`, `Scene`, `ModelNode` — same as Android

```kotlin
// Future API (when Filament JNI jars are available)
@Composable
fun DesktopScene(modifier: Modifier = Modifier) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val engine = Engine.create()
            val renderer = engine.createRenderer()
            FilamentPanel(engine, renderer)
        }
    )
}
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

1. **Build Filament from source** with JNI for desktop targets:
   ```bash
   git clone https://github.com/google/filament.git
   cd filament
   ./build.sh -j release  # builds filament-java.jar + native libs
   ```
2. **Publish JNI jars** to Maven Local or a custom repo
3. **Integrate via SwingPanel** — `FilamentPanel` in Compose Desktop
4. **Port SceneView composables** — the Android `Scene { }` API reuses Filament Java classes

## References

- [Filament BUILDING.md](https://github.com/google/filament/blob/main/BUILDING.md) — JNI build instructions
- [filament-java-example](https://github.com/simonepelosi/filament-java-example) — Desktop JVM example
- Filament Java classes: `FilamentCanvas` (AWT), `FilamentPanel` (Swing)
