# SceneView Android Demo

Play Store-ready showcase app demonstrating SceneView's full feature set.

## Features

- **4-tab Material 3 Expressive UI** (Explore, Showcase, Gallery, QA)
- **14 interactive demos** covering all node types
- 3D model viewer with orbit camera and HDR environments
- AR tap-to-place with plane detection
- Geometry nodes, animations, physics, dynamic sky
- Dark mode support

## Run

```bash
./gradlew :samples:android-demo:assembleDebug
```

Install the APK on a connected device:

```bash
adb install -r samples/android-demo/build/outputs/apk/debug/android-demo-debug.apk
```

## Requirements

- Android device or emulator (API 24+)
- For AR features: ARCore-compatible device
