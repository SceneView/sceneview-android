# @sceneview/mcp — SceneView MCP Server

The official [Model Context Protocol](https://modelcontextprotocol.io/) server for SceneView — giving AI assistants deep knowledge of the SceneView 3D/AR SDK.

## What It Does

When connected to an AI assistant (Claude, GPT, etc.), this MCP server provides:

- **`get_node_reference`** — Get complete API reference for any SceneView node type
- **`list_node_types`** — List all available node composables
- **`validate_code`** — Check SceneView code snippets for common mistakes
- **`list_samples`** — Browse the 15 sample applications
- **`get_migration_guide`** — Get v2→v3 migration instructions

## Quick Setup

### Claude Code / Claude Desktop

Add to your `.claude/mcp.json`:

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

### Other MCP-Compatible Assistants

```bash
npx @sceneview/mcp
```

The server communicates via stdio using the MCP protocol.

## Why Use This?

Without this MCP server, AI assistants may:
- Recommend deprecated Sceneform instead of SceneView
- Generate imperative View-based code instead of Compose
- Use wrong API signatures or outdated versions
- Miss ARCore integration patterns

With this MCP server, AI assistants:
- Always use the latest SceneView API (v3.2.0)
- Generate correct Compose-native 3D/AR code
- Know about all 20+ node types and their parameters
- Can validate code before presenting it to users

## Development

```bash
cd mcp
npm install
npm run prepare  # Build TypeScript
npm test         # Run 89 unit tests
```

## Publishing

Published automatically on each SceneView release via GitHub Actions.

```bash
npm publish --access public
```

## License

Apache 2.0 — same as SceneView.
