# react-native-sceneview

React Native bindings for [SceneView](https://github.com/sceneview/sceneview) -- 3D and AR scenes powered by Filament (Android) and RealityKit (iOS).

> **Status:** Alpha -- 3D model loading works on both platforms. AR scene is functional on Android.

## Installation

```sh
npm install react-native-sceneview
```

### iOS

```sh
cd ios && pod install
```

Requires iOS 17+ and Xcode 15+. The host app must also add `SceneViewSwift` via Swift Package Manager.

### Android

Add the SceneView Maven repository to your project-level `build.gradle`:

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

Requires `minSdk 24`.

## Usage

### 3D Scene

```tsx
import { SceneView } from 'react-native-sceneview';

<SceneView
  style={{ flex: 1 }}
  environment="environments/studio.hdr"
  modelNodes={[{ src: 'models/robot.glb' }]}
  cameraOrbit
/>;
```

### AR Scene

```tsx
import { ARSceneView } from 'react-native-sceneview';

<ARSceneView
  style={{ flex: 1 }}
  planeDetection
  modelNodes={[{ src: 'models/chair.glb', position: [0, 0, -1] }]}
/>;
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `style` | `ViewStyle` | -- | Standard React Native style |
| `environment` | `string` | -- | HDR environment asset path |
| `modelNodes` | `ModelNode[]` | `[]` | Models to render |
| `cameraOrbit` | `boolean` | `true` | Enable orbit camera controls |
| `onTap` | `(event) => void` | -- | Tap callback (event pending) |
| `planeDetection` | `boolean` | `true` | AR: enable plane detection |
| `depthOcclusion` | `boolean` | `false` | AR: enable depth occlusion |
| `instantPlacement` | `boolean` | `false` | AR: enable instant placement |

## Architecture

```
React Native JS
    |
    v
requireNativeComponent('RNSceneView' / 'RNARSceneView')
    |
    +---> Android: ViewManager -> ComposeView -> Scene { } / ARScene { }
    |                              (Filament)
    |
    +---> iOS: RCTViewManager -> UIHostingController -> SceneView / ARSceneView
                                  (RealityKit)
```

- **Android** bridges to the Jetpack Compose `Scene`/`ARScene` composables via `ComposeView`
- **iOS** bridges to SceneViewSwift's SwiftUI views via `UIHostingController`
- Props are mapped from React Native's bridge types to native view parameters

## Limitations

- Geometry and light node props are defined in types but not yet rendered natively
- `onTap` and `onPlaneDetected` event callbacks are declared but events are not yet dispatched
- Scale prop on `ModelNode` is parsed as a uniform float (per-axis array not yet supported)
- Unsupported platforms (web, etc.) show a fallback message

## License

Apache-2.0
