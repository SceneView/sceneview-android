# SceneView React Native Demo

> **Status: Scaffold**
>
> This demo demonstrates the React Native Fabric bridge to SceneView.
> The directory structure and API surface are defined, but native rendering
> integration is not yet complete.

## Architecture

```
React Native (JS/TS)
  +-- Fabric Component --> Android: SceneView (Filament)
  +-- Fabric Component --> iOS: SceneViewSwift (RealityKit)
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
