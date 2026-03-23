# SceneView for Android — Claude Code guide

This is the SceneView SDK repository. SceneView provides 3D and AR as Jetpack Compose
composables for Android, built on Google Filament and ARCore.

## MCP Server

This repository includes an official MCP (Model Context Protocol) server at `mcp/`.
When connected, it gives you access to the full SceneView API reference, code validation,
sample code, and migration guides — directly within the conversation.

To use it, add this to your MCP configuration:

```json
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "@sceneview/mcp"]
    }
  }
}
```

Available tools: `get_node_reference`, `get_sample`, `list_samples`, `get_setup`,
`validate_code`, `get_migration_guide`.

**Always call `validate_code` before presenting generated SceneView code to the user.**

## Full API reference

See [`llms.txt`](./llms.txt) at the repo root for the complete, machine-readable API reference:
composable signatures, node types, resource loading, threading rules, and common patterns.

## When writing any SceneView code

- Use `Scene { }` for 3D-only scenes (`io.github.sceneview:sceneview:3.2.0`)
- Use `ARScene { }` for augmented reality (`io.github.sceneview:arsceneview:3.2.0`)
- Declare nodes as composables inside the trailing content block — not imperatively
- Load models with `rememberModelInstance(modelLoader, "models/file.glb")` — returns `null`
  while loading, always handle the null case
- `LightNode`'s `apply` is a **named parameter** (`apply = { intensity(…) }`), not a trailing lambda
- Never use deprecated Sceneform APIs — SceneView 3.x replaces Sceneform entirely
- Always use Compose-based APIs (composable node functions), not imperative View-based code

## Critical threading rule

Filament JNI calls must run on the **main thread**. Never call `modelLoader.createModel*`
or `materialLoader.*` from a background coroutine directly.
`rememberModelInstance` handles this correctly — use it in composables.
For imperative code, use `modelLoader.loadModelInstanceAsync`.

## Common mistakes to avoid

1. **Trailing lambda on LightNode** — use `apply = { ... }` named parameter instead
2. **Forgetting null check on model loading** — `rememberModelInstance` returns nullable
3. **Calling Filament APIs off the main thread** — causes JNI crashes
4. **Using old Sceneform imports** — use `io.github.sceneview.*` not `com.google.ar.sceneform.*`
5. **Missing camera permission for AR** — add both `<uses-permission>` and `<uses-feature>` to manifest
6. **Wrong destroy order** — let composable lifecycle handle cleanup, do not call `destroy()` manually

## Samples

| Directory | Demonstrates |
|---|---|
| `samples/model-viewer` | 3D model, HDR environment, orbit camera, animation controls |
| `samples/ar-model-viewer` | Tap-to-place, plane detection, pinch/rotate |
| `samples/ar-augmented-image` | Real-world image detection + video overlay |
| `samples/ar-cloud-anchor` | Persistent cross-device cloud anchors |
| `samples/ar-point-cloud` | ARCore feature point visualisation |
| `samples/gltf-camera` | Cameras imported from a glTF file |
| `samples/camera-manipulator` | Orbit / pan / zoom camera |
| `samples/autopilot-demo` | Autonomous driving HUD with traffic lights |
| `samples/physics-demo` | Bouncing spheres with gravity and restitution |
| `samples/dynamic-sky` | Time-of-day sun cycle + atmospheric fog |
| `samples/line-path` | Animated sine/Lissajous curves with PathNode |
| `samples/text-labels` | Camera-facing 3D text labels (TextNode) |
| `samples/reflection-probe` | Zone-based IBL overrides + material picker |
| `samples/post-processing` | Bloom, vignette, tone mapping, FXAA, SSAO |

## Module structure

| Module | Purpose |
|---|---|
| `sceneview/` | Core 3D library — `Scene`, `SceneScope`, all node types |
| `arsceneview/` | AR layer — `ARScene`, `ARSceneScope`, ARCore integration |
| `samples/common/` | Shared helpers across sample apps |
| `mcp/` | `@sceneview/mcp` — MCP server for AI assistant integration |

## Build & test

```bash
# Android SDK
./gradlew build

# MCP server
cd mcp && npm install && npm run prepare && npm test
```
