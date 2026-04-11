# React Native Demo — Setup Status

> **Status: scaffolded.** JS, Android, and iOS native projects are in
> place. The app is reviewable as a contribution but still requires a
> one-time `npm install` + native build to run on a device. See
> *"What's still needed to run"* below.

## What is here

| File / dir | Purpose |
|---|---|
| `package.json` | JS dependencies, version (kept in sync with `gradle.properties`) |
| `index.js` | Root entry that registers `App` with `AppRegistry` |
| `app.json` | RN app name (`SceneViewRNDemo`) and display name |
| `babel.config.js` | Babel preset (`@react-native/babel-preset`) |
| `metro.config.js` | Metro bundler config — watches the linked bridge module and blocklists its duplicate `react*` copies |
| `tsconfig.json` | TypeScript config extending `@react-native/typescript-config`, with a path mapping to the local bridge source |
| `.watchmanconfig` | Empty watchman config (required by RN) |
| `Gemfile` | CocoaPods Ruby deps for `ios/` |
| `src/App.tsx` | Full feature showcase using `@sceneview-sdk/react-native` |
| `assets/environments/studio_small.hdr` | HDR environment for the demo scene |
| `android/` | Android Gradle project. Namespace is `io.github.sceneview.demo.rn`. The app `sourceSets` pulls `../assets` into `assets/environments/` so SceneView can load HDR files by relative path. MainActivity/MainApplication live under `android/app/src/main/java/io/github/sceneview/demo/rn/`. |
| `ios/` | Xcode workspace + `Podfile`. Display name still reads *SceneView RN Demo*. |
| `README.md` | High-level description of the demo |

## What's still needed to run

The scaffold was generated with
`npx @react-native-community/cli init SceneViewRNDemo --version 0.73.0`,
copied into this directory, and rewritten with the SceneView namespace.
Before a first `run-android` / `run-ios`, these steps are required:

1. **Install and build the bridge module first.** The linked
   `react-native/react-native-sceneview` module's `prepare` script runs
   `bob build`, which requires its own install:
   ```bash
   cd ../../react-native/react-native-sceneview
   npm install
   npm run build
   cd -
   ```
2. **Install the demo's JS deps:**
   ```bash
   npm install
   ```
3. **iOS only — install pods:**
   ```bash
   cd ios && bundle install && bundle exec pod install && cd -
   ```
4. **Run:**
   ```bash
   npm run android   # requires an emulator or connected device
   npm run ios       # requires an iOS simulator or connected device
   ```

## Known DX blocker on `npm install`

Step 1 above is the mitigation for the `bob build` failure described in
previous sessions. Longer term the fix is to publish
`@sceneview-sdk/react-native` to npm so the `file:` dependency can be
replaced with a registry version.

## Note on package name

The bridge module's `package.json` was renamed to
`@sceneview-sdk/react-native`, but its `podspec`, `tsconfig.json` path
mapping, `README.md`, `package-lock.json`, and `example/` still
reference the old name `react-native-sceneview`. The demo here imports
from the new scoped name (`@sceneview-sdk/react-native`) to match
`package.json`. The remaining stale references in the bridge module
should be reconciled in a follow-up that touches the bridge module
itself.
