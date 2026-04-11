# React Native Demo — Setup Status

> **Status: JS-level scaffold complete.** The root entry, Metro/Babel config,
> TypeScript config and app.json are all in place. `android/` and `ios/`
> native projects still need to be generated — see below.

## What is here

| File | Purpose |
|---|---|
| `package.json` | JS dependencies, version (in sync with `gradle.properties`) |
| `index.js` | Root entry that registers `App` with `AppRegistry` |
| `app.json` | RN app name (`SceneViewRNDemo`) and display name |
| `babel.config.js` | Babel preset (`@react-native/babel-preset`) |
| `metro.config.js` | Metro bundler config — watches the linked bridge module and blocklists its duplicate `react*` copies |
| `tsconfig.json` | TypeScript config extending `@react-native/typescript-config`, with a path mapping to the local bridge source |
| `.watchmanconfig` | Empty watchman config (required by RN) |
| `src/App.tsx` | Full feature showcase using `@sceneview-sdk/react-native` |
| `assets/environments/studio_small.hdr` | HDR environment for the demo scene |
| `README.md` | High-level description of the demo |

## What is missing to build

`android/` and `ios/` native projects still need to be generated. The easiest
path is to scaffold a throwaway app with the community CLI and copy those two
directories back:

```bash
cd /tmp
npx @react-native-community/cli init SceneViewRNDemo \
  --version 0.73.0 \
  --skip-install
cp -R SceneViewRNDemo/android SceneViewRNDemo/ios \
  <REPO>/samples/react-native-demo/
```

Then wire the bridge module via the existing dependency:

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
