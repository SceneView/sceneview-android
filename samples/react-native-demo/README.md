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
