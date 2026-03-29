# SceneView React Native Demo

> **Status: Alpha**
>
> This demo demonstrates the React Native bridge to SceneView.
> 3D model loading works on both Android and iOS. AR scene is functional on Android.

## Architecture

```
React Native (JS/TS)
  +-- Native Component --> Android: SceneView (Filament)
  +-- Native Component --> iOS: SceneViewSwift (RealityKit)
```

## Assets

The demo ships with bundled 3D models and environment maps in `assets/`:

| File | Description |
|---|---|
| `models/robot.glb` | Animated robot (idle animation) |
| `models/damaged_helmet.glb` | Khronos PBR showcase model |
| `models/cyberpunk_car.glb` | Detailed car model |
| `models/fox.glb` | Animated fox (lightweight) |
| `models/animated_butterfly.glb` | Animated butterfly |
| `environments/studio_small.hdr` | Studio lighting environment |

### Android asset setup

For Android, assets must be accessible from the app's asset manager. Copy or symlink the
`assets/` directory contents into your Android project:

```bash
# From the react-native-demo directory:
cp -r assets/models android/app/src/main/assets/
cp -r assets/environments android/app/src/main/assets/
```

Or add an asset source directory in `android/app/build.gradle`:

```groovy
android {
    sourceSets {
        main {
            assets.srcDirs += ['../../assets']
        }
    }
}
```

### iOS asset setup

For iOS, add the assets to your Xcode project's bundle resources. In your Podfile or
Xcode project settings, ensure the `models/` and `environments/` directories are
included as resource bundles.

In `ios/<YourApp>.xcodeproj`, drag the `assets/models` and `assets/environments`
folders into the project navigator with "Create folder references" selected.

## Run

```bash
cd samples/react-native-demo
npm install
npx react-native run-android  # or run-ios
```

## Requirements

- Node.js 18+
- React Native 0.72+
- Android SDK 24+ (for Android)
- iOS 17+ (for iOS)
