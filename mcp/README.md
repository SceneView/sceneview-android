# sceneview-mcp

[![npm](https://img.shields.io/npm/v/sceneview-mcp?color=cb3837&label=npm)](https://www.npmjs.com/package/sceneview-mcp)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

MCP server for [SceneView](https://github.com/SceneView/sceneview) — 3D and AR as Jetpack Compose composables for Android.

Add this to Claude and it **always knows how to use SceneView**. No copy-pasting docs. No hallucinated APIs. Correct, compilable Kotlin — first try.

---

## Quick start

Add to your Claude config and you're done:

```json
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "sceneview-mcp"]
    }
  }
}
```

<details>
<summary>Where does this go?</summary>

| Client | Config file |
|---|---|
| **Claude Code** (project) | `.claude/mcp.json` at project root |
| **Claude Code** (global) | `~/.claude/mcp.json` |
| **Claude Desktop** | `claude_desktop_config.json` |

After saving, run `/mcp` in Claude Code or restart Claude Desktop to pick it up.
</details>

---

## What Claude gets

### Resource — `sceneview://api`

The complete SceneView 3.0.0 API reference (`llms.txt`): composable signatures, node types, AR scope, resource loading, threading rules, and common patterns.

### Tool — `get_sample(scenario)`

Returns a complete, compilable Kotlin sample.

| Scenario | What you get |
|---|---|
| `model-viewer` | Full-screen 3D scene, HDR environment, orbit camera |
| `ar-tap-to-place` | AR tap-to-place with pinch-to-scale and drag-to-rotate |
| `ar-placement-cursor` | AR reticle that snaps to surfaces, tap to confirm |
| `ar-augmented-image` | Detect a reference image, overlay a 3D model |
| `ar-face-filter` | Front-camera face mesh with a custom material |

### Tool — `get_setup(type)`

Returns Gradle dependencies + AndroidManifest for `"3d"` or `"ar"` projects.

---

## How it works

```
You:    "Add AR placement to my app"
         │
Claude:  reads sceneview://api  ←  full API ref, always current
         │
Claude:  calls get_sample("ar-tap-to-place")  ←  working Kotlin
         │
Result:  Correct, compilable SceneView 3.0.0 code
```

---

## Try it — sample prompts

**3D model viewer**
> Create a Compose screen that loads `models/helmet.glb` in a full-screen 3D scene with orbit camera and HDR environment. Use SceneView 3.0.0.

**AR tap-to-place**
> Create an AR Compose screen. Tapping a detected surface places `models/chair.glb` with pinch-to-scale and drag-to-rotate. Multiple taps = multiple objects.

**AR placement cursor**
> Create an AR screen with a reticle that snaps to surfaces at screen center. Tap to place `models/object.glb` and hide the reticle.

**AR augmented image**
> Create an AR screen that detects `R.drawable.target_image` (15 cm) and places `models/overlay.glb` above it, scaled to match.

**AR face filter**
> Create an AR screen using the front camera that detects faces and applies `materials/face_mask.filamat` to the mesh.

**Product configurator**
> Create a 3D product configurator with Red/Blue/Green buttons. Apply the color as a material on `models/product.glb`. Add orbit camera and pinch-to-zoom.

**AR multi-object scene**
> Create an AR screen where a bottom sheet lets users pick between chair, table, and lamp GLBs. Tap to place. Each object gets pinch-to-scale and drag-to-rotate.

---

## Development

```bash
cd mcp
npm install
npm run prepare   # copies llms.txt + compiles TypeScript
npm start         # run over stdio
npx @modelcontextprotocol/inspector node dist/index.js   # test in MCP inspector
```

## Publishing

```bash
cd mcp
npm run prepare
npm publish --access public
```

---

## Links

- **SDK**: [github.com/SceneView/sceneview](https://github.com/SceneView/sceneview)
- **npm**: [npmjs.com/package/sceneview-mcp](https://www.npmjs.com/package/sceneview-mcp)
- **MCP spec**: [modelcontextprotocol.io](https://modelcontextprotocol.io)
