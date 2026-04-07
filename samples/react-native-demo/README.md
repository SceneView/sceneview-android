# SceneView React Native Demo

> **Status: Alpha**
>
> Feature showcase app demonstrating the React Native bridge to SceneView.
> Demonstrates all bridge capabilities: geometry nodes, light nodes, AR mode, tap events, and Sketchfab search.

## Architecture

```
React Native (JS/TS)
  +-- Native Component --> Android: SceneView (Filament)
  +-- Native Component --> iOS: SceneViewSwift (RealityKit)
```

## Tabs

| Tab | Demonstrates |
|---|---|
| **Search** | Sketchfab API search (fetch), model viewing with `modelNodes`, `onTap` events |
| **Geometry** | `geometryNodes` (cube, sphere, cylinder, plane), color picker, add/remove shapes |
| **Lights** | `lightNodes` (directional, point, spot), preset lighting scenes, custom light creation |
| **AR** | `ARSceneView` with `planeDetection`, `depthOcclusion`, `instantPlacement`, `onPlaneDetected` |

## Bridge Features Demonstrated

- `SceneView` component with `environment`, `cameraOrbit`
- `ARSceneView` with plane detection, depth occlusion, instant placement
- `modelNodes` for GLB model loading
- `geometryNodes` for procedural geometry (cube, sphere, cylinder, plane) with color
- `lightNodes` for scene lighting (directional, point, spot) with intensity and color
- `onTap` events with world-space coordinates and node names
- `onPlaneDetected` events for AR plane tracking

## Assets

| File | Description |
|---|---|
| `environments/studio_small.hdr` | Studio lighting environment |

### Android asset setup

For Android, assets must be accessible from the app's asset manager. Add an asset source
directory in `android/app/build.gradle`:

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
Xcode project settings, ensure the `environments/` directory is included as a resource bundle.

## Run

```bash
cd samples/react-native-demo
npm install
npx react-native run-android  # or run-ios
```

## Requirements

- Node.js 18+
- React Native 0.73+
- Android SDK 24+ (for Android)
- iOS 17+ (for iOS)
