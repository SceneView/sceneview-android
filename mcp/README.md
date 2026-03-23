# sceneview-mcp — SceneView MCP Server

[![npm version](https://img.shields.io/npm/v/sceneview-mcp?color=6c35aa)](https://www.npmjs.com/package/sceneview-mcp)
[![MCP](https://img.shields.io/badge/MCP-v1.12-blue)](https://modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

The official [Model Context Protocol](https://modelcontextprotocol.io/) server for **SceneView** — giving AI assistants deep knowledge of the SceneView 3D/AR SDK so they generate correct, compilable Kotlin code.

## What It Does

When connected to an AI assistant (Claude, GPT, Cursor, Windsurf, etc.), this MCP server provides:

| Tool | Description |
|------|-------------|
| `get_node_reference` | Complete API reference for any SceneView node type |
| `list_node_types` | List all available node composables |
| `validate_code` | Check SceneView code for 15+ common mistakes |
| `get_sample` | Get complete, compilable sample code for any scenario |
| `list_samples` | Browse 15 sample applications by tag |
| `get_setup` | Get Gradle + manifest setup for 3D or AR |
| `get_migration_guide` | Full v2→v3 migration instructions |

**Resources:**
| Resource | Description |
|----------|-------------|
| `sceneview://api` | Complete SceneView 3.2.0 API reference |
| `sceneview://known-issues` | Live open issues from GitHub (cached 10min) |

## Quick Setup

### Claude Desktop / Claude Code

Add to your MCP configuration:

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

### Cursor / Windsurf / Other MCP Clients

```bash
npx sceneview-mcp
```

The server communicates via stdio using the MCP protocol.

### Verify Installation

Once connected, ask your AI assistant:
> "List all SceneView node types"

It should return the full list of 22+ composable nodes.

## Why Use This?

**Without** this MCP server, AI assistants may:
- Recommend deprecated Sceneform instead of SceneView
- Generate imperative View-based code instead of Compose
- Use wrong API signatures or outdated versions
- Miss ARCore integration patterns
- Forget null-checks on `rememberModelInstance`

**With** this MCP server, AI assistants:
- Always use the latest SceneView 3.2.0 API
- Generate correct Compose-native 3D/AR code
- Know about all 22+ node types and their exact parameters
- Validate code against 15+ rules before presenting it
- Provide working sample code for any scenario

## Development

```bash
cd mcp
npm install
npm run prepare  # Copy llms.txt + build TypeScript
npm test         # Run unit tests
npm run dev      # Start with tsx (hot reload)
```

## Publishing

Published to npm on each SceneView release:

```bash
npm publish --access public
```

## License

Apache 2.0 — same as SceneView.
