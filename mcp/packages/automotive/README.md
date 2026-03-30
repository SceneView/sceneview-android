# automotive-3d-mcp

MCP server for Automotive 3D visualization with [SceneView](https://github.com/sceneview/sceneview).

Give Claude (or any MCP-compatible AI assistant) the ability to generate complete, compilable Kotlin code for automotive 3D applications on Android using Jetpack Compose and SceneView.

## Tools

| Tool | Description |
|---|---|
| `get_car_configurator` | 3D car configurator with color/material variants, camera presets (exterior, interior, detail), turntable, AR mode. 10 body styles. |
| `get_hud_overlay` | Heads-up display overlay via ViewNode — speedometer, navigation, alerts, fuel, temperature, gear indicator, turn signals, lane assist. 6 styles. |
| `get_dashboard_3d` | 3D instrument cluster with animated gauges — speedometer, tachometer, fuel, temperature, oil pressure, battery, boost, odometer. 6 themes. |
| `get_ar_showroom` | AR car placement in real space (driveway, parking lot, garage). Color swap, open doors, night lighting, measurements, photo capture. |
| `get_parts_catalog` | 3D parts explorer with exploded view, part selection, cross-section, search. 10 categories with realistic part data and pricing. |
| `list_car_models` | Database of 18+ free car 3D models — Khronos ToyCar, Sketchfab free cars, engines, interiors, wheels. With source URLs and licenses. |
| `validate_automotive_code` | Validates SceneView automotive code for threading, null-safety, API misuse, scale issues, format warnings. |

## Installation

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "automotive-3d": {
      "command": "npx",
      "args": ["-y", "automotive-3d-mcp"]
    }
  }
}
```

### From source

```bash
git clone https://github.com/sceneview/sceneview.git
cd sceneview/mcp/packages/automotive
npm install
npm run build
npm start
```

## Usage examples

Ask your AI assistant:

- "Build me a 3D car configurator for a sedan with color picker and turntable"
- "Create an AR showroom to place an SUV in my driveway with night lighting"
- "Generate a heads-up display with speedometer, navigation, and fuel gauge in sport style"
- "Build a 3D instrument cluster with speedometer and tachometer in electric theme"
- "Create an engine parts catalog with exploded view and part numbers"
- "List free 3D car models I can use for a configurator demo"
- "Validate my car viewer SceneView code for common mistakes"

Every tool returns complete, compilable Kotlin code using SceneView 3.6.0 with proper:
- Gradle dependencies
- SceneView composable setup (engine, modelLoader, collisionSystem)
- Null-safe model loading
- LightNode with named `apply` parameter (not trailing lambda)
- Material 3 UI controls
- AR mode with ARCore setup instructions

## Dependencies

- [SceneView](https://github.com/sceneview/sceneview) 3.6.0 — 3D/AR rendering
- [ARSceneView](https://github.com/sceneview/sceneview) 3.6.0 — AR features
- [MCP SDK](https://github.com/modelcontextprotocol/sdk) — Model Context Protocol

## License

Apache 2.0 — see [LICENSE](LICENSE).
