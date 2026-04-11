# React Native Demo — Setup Status

> **Status: code-only stub.** This sample currently ships only the bridge usage
> example (`src/App.tsx`) and the JS package metadata. It is **not** a runnable
> React Native project on its own — the native scaffolding has not been
> generated yet.

## What is here

| File | Purpose |
|---|---|
| `package.json` | JS dependencies, version (in sync with `gradle.properties`) |
| `src/App.tsx` | Full feature showcase using `@sceneview-sdk/react-native` |
| `assets/environments/studio_small.hdr` | HDR environment for the demo scene |
| `README.md` | High-level description of the demo |

## What is missing to build

To turn this into a runnable app you need a standard React Native project
skeleton, generated for example with:

```bash
npx @react-native-community/cli init SceneViewReactNativeDemo \
  --version 0.73.0 \
  --skip-install
```

then merge the following pieces back into this directory:

- `index.js` (or `index.tsx`) — registers `App` with `AppRegistry`
- `app.json` — RN app name + display name
- `tsconfig.json` — TypeScript config (extends `@react-native/typescript-config`)
- `babel.config.js` — Metro/Babel preset (`@react-native/babel-preset`)
- `metro.config.js` — Metro bundler config
- `android/` — Gradle project (`build.gradle`, `settings.gradle`, `app/`,
  `gradle/wrapper/`)
- `ios/` — Xcode project (`Podfile`, `*.xcodeproj`, `*.xcworkspace`,
  `AppDelegate.{h,mm}`, `Info.plist`)

After scaffolding, wire the bridge module via the existing dependency:

```json
"@sceneview-sdk/react-native": "file:../../react-native/react-native-sceneview"
```

and apply the asset hooks documented in `README.md` (Android `sourceSets`,
iOS resource bundle).

## Known DX blocker on `npm install`

`npm install` currently fails because the linked
`react-native/react-native-sceneview` module declares a `prepare` script that
runs `bob build` (`react-native-builder-bob`), which is not installed in that
linked path. Two options to unblock once the scaffolding is in place:

1. **Build the lib first**, then install the demo:
   ```bash
   cd ../../react-native/react-native-sceneview
   npm install
   npm run build
   cd -
   npm install
   ```
2. **Use the published package** instead of `file:` once the module ships to
   npm under `@sceneview-sdk/react-native`.

## Note on package name

The bridge module's `package.json` was renamed to
`@sceneview-sdk/react-native`, but its `podspec`, `tsconfig.json` path
mapping, `README.md`, `package-lock.json`, and `example/` still reference the
old name `react-native-sceneview`. The demo here imports from the new
scoped name (`@sceneview-sdk/react-native`) to match `package.json`. The
remaining stale references in the bridge module should be reconciled in a
follow-up that touches the bridge module itself.
