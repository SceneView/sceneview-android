# Session 27 — Overnight Tasks Plan

> Execute ALL tasks below. Each task has acceptance criteria.
> Priority order: 1 (highest) to 5 (lowest).

## TASK 1: Rewrite Android Demo App (P1)

### Goal
Replace the current 40-model gallery with a **feature showcase** app that demonstrates ALL SDK capabilities with a Sketchfab search integration.

### Acceptance Criteria
- [ ] Remove hardcoded model list (40 GLB files, ~259MB assets)
- [ ] Add Sketchfab search: user types query → fetches results → downloads GLB on demand
- [ ] Tabs: **3D Viewer** | **AR** | **Features** | **About**
- [ ] Features tab showcases ALL node types with live demos:
  - CubeNode, SphereNode, CylinderNode, PlaneNode (geometry)
  - LightNode (directional, point, spot — with sliders)
  - DynamicSkyNode (time-of-day slider)
  - PhysicsNode (tap to drop objects)
  - ImageNode, TextNode, VideoNode
  - BillboardNode
  - ShapeNode (custom polygon)
  - Camera controls (orbit, first-person)
- [ ] Each feature demo has a screenshot test
- [ ] Material 3 Expressive design, dark/light mode
- [ ] Version badge shows 3.6.1
- [ ] Builds without errors: `./gradlew :samples:android-demo:assembleDebug`

### Key files
- `samples/android-demo/` — entire app
- `samples/android-demo/build.gradle` — remove asset pack dependency
- Use Sketchfab API: `https://api.sketchfab.com/v3/search?type=models&downloadable=true&q={query}`

---

## TASK 2: Rewrite iOS Demo App (P1)

### Goal
Match the Android demo — Sketchfab search, feature showcase, SwiftUI native.

### Acceptance Criteria
- [ ] Remove hardcoded model list
- [ ] Add Sketchfab search (URLSession + SwiftUI)
- [ ] Tabs: **3D** | **AR** | **Features**
- [ ] Features tab: all iOS node types (GeometryNode.cube/.sphere, LightNode, DynamicSkyNode, etc.)
- [ ] Each feature has a screenshot via XCTest
- [ ] SwiftUI native, iOS 17+
- [ ] Dark/light mode

### Key files
- `samples/ios-demo/`
- `SceneViewSwift/Sources/`

---

## TASK 3: Rewrite Web Demo (P2)

### Goal
Modern web demo with Sketchfab search and geometry showcase.

### Acceptance Criteria
- [ ] Replace hardcoded model list with Sketchfab search
- [ ] Add geometry showcase section (cube, sphere, cylinder, plane with color pickers)
- [ ] Add WebXR AR/VR buttons
- [ ] Responsive design
- [ ] Screenshot tests via Playwright or Puppeteer

### Key files
- `samples/web-demo/`
- `sceneview-web/`

---

## TASK 4: Rewrite Flutter Demo (P2)

### Goal
Feature showcase for Flutter bridge capabilities.

### Acceptance Criteria
- [ ] Sketchfab search for models
- [ ] Demo all Flutter bridge features: onTap, onPlaneDetected, rotation, AR mode
- [ ] Material 3 Flutter theme
- [ ] Screenshot tests via integration_test

### Key files
- `samples/flutter-demo/`
- `flutter/sceneview_flutter/`

---

## TASK 5: Rewrite React Native Demo (P2)

### Goal
Feature showcase for RN bridge capabilities.

### Acceptance Criteria
- [ ] Sketchfab search for models
- [ ] Demo: geometry nodes (cube, sphere, cylinder, plane), light nodes, AR mode
- [ ] Demo: onTap events
- [ ] Screenshot tests

### Key files
- `samples/react-native-demo/`
- `react-native/react-native-sceneview/`

---

## TASK 6: Visual Verification for ALL Platforms (P1)

### Goal
Every platform has automated screenshot capture for visual regression.

### Acceptance Criteria
- [ ] Android: VisualVerificationTest already exists — extend to cover ALL node types
- [ ] iOS: Add XCUITest with screenshot capture
- [ ] Web: Add Playwright/Puppeteer screenshot test
- [ ] Flutter: Add integration_test with screenshot
- [ ] React Native: Add Detox or Maestro screenshot test
- [ ] All screenshots uploaded as CI artifacts
- [ ] HTML visual report generated for each platform

### CI workflows needed
- `.github/workflows/render-tests.yml` — already exists for Android
- `.github/workflows/ios-render-tests.yml` — NEW
- `.github/workflows/web-render-tests.yml` — NEW

---

## TASK 7: Store Publication Verification (P3)

### Goal
Verify all apps are live on their stores and website.

### Acceptance Criteria
- [ ] Play Store: verify "3D & AR Explorer" is live, check version
- [ ] App Store: verify iOS app is live or in review
- [ ] npm: verify sceneview-mcp, sceneview-web, @sceneview-sdk/react-native at 3.6.1
- [ ] pub.dev: verify sceneview at 3.6.1
- [ ] Website: verify sceneview.github.io is up to date
- [ ] Maven Central: re-trigger release workflow for 3.6.1 (needs maintainer)

---

## TASK 8: Sketchfab API Integration Module (P2)

### Goal
Create a shared Sketchfab search module usable by all sample apps.

### Acceptance Criteria
- [ ] `samples/common/` — add SketchfabApi.kt (KMP-compatible)
- [ ] Search endpoint: `GET /v3/search?type=models&downloadable=true&q={query}`
- [ ] Download endpoint: `GET /v3/models/{uid}/download`
- [ ] Pagination support
- [ ] Thumbnail display
- [ ] Error handling (rate limits, network errors)
- [ ] No API key required for search (public API)

---

## Execution Order

```
Night 1: TASK 1 (Android demo) + TASK 6 (visual verification) + TASK 8 (Sketchfab module)
Night 2: TASK 2 (iOS demo) + TASK 3 (Web demo)
Night 3: TASK 4 (Flutter demo) + TASK 5 (RN demo) + TASK 7 (store verification)
```

## Quality Gates (run after EACH task)

1. Build passes: `./gradlew assembleDebug` (Android), `xcodebuild` (iOS), `npm run build` (Web)
2. All existing tests still pass
3. Screenshots captured and saved as artifacts
4. No new lint errors
5. Version 3.6.1 displayed in app
