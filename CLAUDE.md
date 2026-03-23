# SceneView for Android — AI Assistant Guide

> Compose-native 3D and AR SDK for Android, built on Google Filament and ARCore.
> The official successor to Google Sceneform (deprecated 2021).

**Version:** 3.2.2
**Maven:** `io.github.sceneview:sceneview:3.2.2` / `io.github.sceneview:arsceneview:3.2.2`
**Min SDK:** 24 | **Target SDK:** 36 | **Java:** 17
**Kotlin:** 2.1.21 | **Compose UI:** 1.10.5 | **Filament:** 1.56.0 | **ARCore:** 1.53.0

## Repository Structure

```
sceneview/          Core 3D library — Scene, SceneScope, all node types
arsceneview/        AR layer — ARScene, ARSceneScope, ARCore integration
samples/            14 sample apps (model-viewer, ar-model-viewer, physics-demo, etc.)
  common/           Shared helpers across sample apps
mcp/                @sceneview/mcp — MCP server for AI assistant integration (TypeScript)
docs/               Documentation site source
buildSrc/           Gradle plugins (Filament material compiler)
.claude/            Claude Code config: mcp.json, slash commands
llms.txt            Complete machine-readable API reference
```

## Critical Rules — MUST FOLLOW

1. **All Filament JNI calls on the main thread.** Never call `modelLoader.createModel*` or
   `materialLoader.*` from a background coroutine. Use `rememberModelInstance` in composables
   or `modelLoader.loadModelInstanceAsync` for imperative code.

2. **`rememberModelInstance` returns null while loading.** ALWAYS null-check before passing
   to `ModelNode`. The composable recomposes when the instance is ready.

3. **`LightNode` `apply` is a named parameter, NOT a trailing lambda.**
   Correct: `LightNode(type = ..., apply = { intensity(100_000f) })`
   Wrong: `LightNode(type = ...) { intensity(100_000f) }`

4. **Never use deprecated Sceneform APIs.** Use `io.github.sceneview.*` imports, not
   `com.google.ar.sceneform.*`. SceneView 3.x replaces Sceneform entirely.

5. **Let Compose lifecycle handle cleanup.** Do not call `destroy()` manually on nodes.
   Composable nodes are disposed automatically when they leave composition.

6. **Use `Position`/`Rotation`/`Scale` types** from `io.github.sceneview.math`, not raw `Float3`.

7. **Declare nodes as composables inside `Scene { }` / `ARScene { }`.** Never build nodes
   imperatively outside the content block.

8. **AR manifest requirements.** AR apps need both `<uses-permission android:name="android.permission.CAMERA" />`
   and `<uses-feature android:name="android.hardware.camera.ar" />` plus the ARCore meta-data tag.

## Build & Test Commands

```bash
# Core library
./gradlew :sceneview:assembleDebug
./gradlew :sceneview:assembleRelease

# AR library
./gradlew :arsceneview:assembleDebug

# Specific sample
./gradlew :samples:model-viewer:assembleDebug
./gradlew :samples:ar-model-viewer:assembleDebug
./gradlew :samples:physics-demo:assembleDebug

# All samples
./gradlew assembleSamplesDebug

# Lint
./gradlew :sceneview:lint :arsceneview:lint

# MCP server tests
cd mcp && npm test
```

## Code Style

- Kotlin with Jetpack Compose conventions
- Composable functions: `PascalCase` (e.g., `ModelNode`, `ARScene`)
- State hoisting pattern for all public composables
- KDoc on all public APIs
- No trailing lambda for `apply` parameters (use named argument)
- `remember*` prefix for composable factory functions

## Architecture

- `Scene` composable creates Filament Engine, manages the render loop
- `SceneScope` provides DSL for declaring nodes as composables
- `SceneNodeManager` bridges Compose snapshot state ↔ Filament scene graph
- Nodes follow Compose lifecycle: created on composition, destroyed on disposal
- AR layer extends 3D: `ARScene` extends `Scene`, `ARSceneScope` extends `SceneScope`
- Rendering pipeline: Filament engine (single instance) → SurfaceView/TextureView

## Node Types (22+)

**3D:** ModelNode, LightNode, CameraNode, CubeNode, SphereNode, CylinderNode,
PlaneNode, MeshNode, ImageNode, VideoNode, ViewNode, PhysicsNode, DynamicSkyNode,
FogNode, ReflectionProbeNode, LineNode, PathNode, BillboardNode, TextNode

**AR:** AnchorNode, PoseNode, HitResultNode, AugmentedImageNode, AugmentedFaceNode,
CloudAnchorNode, TrackableNode, StreetscapeGeometryNode

## Common Tasks

- **Add a new node type:** Create composable in `sceneview/src/.../node/`
- **Add a new sample:** Create module in `samples/`, add to `settings.gradle`
- **Publish release:** Tag with semver → `release.yml` auto-publishes to Maven Central
- **Update Filament:** Change version in `gradle.properties`, regenerate materials
- **Update docs:** Edit `docs/docs/`, push to trigger `docs.yml` workflow

## MCP Server

- Located at `mcp/`
- TypeScript, Node.js
- Run: `npx @sceneview/mcp`
- Test: `cd mcp && npm test`
- Add tools in `mcp/src/index.ts`
- Published to npm as `@sceneview/mcp`

### Available MCP Tools
- `get_sample(scenario)` — compilable Kotlin sample
- `list_samples(tag?)` — all samples with descriptions
- `get_setup(type)` — Gradle + manifest setup
- `validate_code(code)` — check for common mistakes
- `get_migration_guide()` — v2.x → v3.0 migration
- `get_node_reference(nodeType)` — full API for any node
- `get_platform_roadmap()` — multi-platform plans
- `get_best_practices(category?)` — performance & architecture
- `get_ar_setup()` — detailed AR configuration

## Release Process

1. Update `VERSION_NAME` in `gradle.properties`
2. Update `CHANGELOG.md`
3. Commit: `git commit -m "release: vX.Y.Z"`
4. Tag: `git tag vX.Y.Z`
5. Push: `git push origin main --tags`
6. `release.yml` auto-publishes to Maven Central + creates GitHub Release
7. `build-apks.yml` generates sample APKs and attaches to release
8. `docs.yml` rebuilds and publishes documentation

## CI/CD Workflows

| Workflow | Trigger | Purpose |
|----------|---------|--------|
| ci.yml | PR, push to main | Build, lint, test |
| release.yml | Tag push | Publish to Maven Central |
| build-apks.yml | Release | Build sample APKs |
| docs.yml | Push to main | Deploy documentation |
| maintenance.yml | Daily cron | Health checks, dependency audits |
| stale.yml | Cron | Auto-close stale issues |

## Quick Reference

```kotlin
// Minimal 3D scene
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = rememberEngine(),
    modelLoader = rememberModelLoader(engine),
    cameraManipulator = rememberCameraManipulator()
) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f)
    }
}

// Minimal AR scene
ARScene(
    modifier = Modifier.fillMaxSize(),
    engine = rememberEngine(),
    modelLoader = rememberModelLoader(engine),
    planeRenderer = true
) {
    // Tap-to-place handled via onTouchEvent callback
}
```
