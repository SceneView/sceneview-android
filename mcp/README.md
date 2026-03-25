# sceneview-mcp — SceneView MCP Server

[![npm version](https://img.shields.io/npm/v/sceneview-mcp?color=6c35aa)](https://www.npmjs.com/package/sceneview-mcp)
[![npm downloads](https://img.shields.io/npm/dm/sceneview-mcp?color=blue)](https://www.npmjs.com/package/sceneview-mcp)
[![MCP](https://img.shields.io/badge/MCP-v1.12-blue)](https://modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/License-MIT-green)](./LICENSE)
[![Node](https://img.shields.io/badge/Node-%3E%3D18-brightgreen)](https://nodejs.org/)

> **Disclaimer:** This tool generates code suggestions for the SceneView SDK. Generated code is provided "as is" without warranty. Always review generated code before use in production. This is not a substitute for professional software engineering review. See [TERMS.md](./TERMS.md) and [PRIVACY.md](./PRIVACY.md).

The official [Model Context Protocol](https://modelcontextprotocol.io/) server for **SceneView** — giving AI assistants deep knowledge of the SceneView 3D/AR SDK so they generate correct, compilable Kotlin code.

---

## What It Does

When connected to an AI assistant (Claude, Cursor, Windsurf, etc.), this MCP server provides **10 tools** and **2 resources** that give the assistant expert-level knowledge of the SceneView SDK:

### Tools

| Tool | Description |
|------|-------------|
| `get_node_reference` | Complete API reference for any SceneView node type (26+ types) |
| `list_node_types` | List all available node composables |
| `validate_code` | Check SceneView code for 15+ common mistakes before presenting it |
| `get_sample` | Get complete, compilable sample code for any of 14 scenarios |
| `list_samples` | Browse all sample applications, optionally filtered by tag |
| `get_setup` | Gradle + manifest setup for 3D or AR projects |
| `get_migration_guide` | Full SceneView 2.x to 3.0 migration instructions |
| `get_platform_roadmap` | Multi-platform roadmap (Android, iOS, KMP, Web) |
| `get_best_practices` | Performance, architecture, memory, and threading best practices |
| `get_ar_setup` | Detailed AR setup: manifest, permissions, session config, patterns |

### Resources

| Resource | Description |
|----------|-------------|
| `sceneview://api` | Complete SceneView 3.3.0 API reference (llms.txt) |
| `sceneview://known-issues` | Live open issues from GitHub (cached 10 min) |

---

## Installation

### Claude Desktop

Add to your Claude Desktop configuration file:

- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

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

Restart Claude Desktop after saving the file.

### Claude Code

Run from your terminal:

```bash
claude mcp add sceneview -- npx -y sceneview-mcp
```

Or add to your `.claude/settings.json`:

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

### Cursor

Open **Settings > MCP** and add a new server:

**Name:** `sceneview`
**Type:** `command`
**Command:** `npx -y sceneview-mcp`

Or add to your `.cursor/mcp.json`:

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

### Windsurf

Open **Settings > MCP** and add:

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

### Other MCP Clients

The server communicates via **stdio** using the MCP protocol. Start it directly:

```bash
npx sceneview-mcp
```

---

## Verify Installation

Once connected, ask your AI assistant:

> "List all SceneView node types"

It should return the full list of 26+ composable nodes. If it does, the MCP server is working.

---

## Tool Examples

### Get a sample project

> "Show me an AR tap-to-place sample with SceneView"

The assistant will call `get_sample("ar-model-viewer")` and return a complete, compilable Kotlin composable with all imports and dependencies.

### Validate generated code

> "Create a 3D model viewer and validate the code"

The assistant will generate the code, then call `validate_code` to check it against 15+ rules (threading, null safety, API correctness, lifecycle) before presenting it.

### Look up a node's API

> "What parameters does LightNode accept?"

The assistant will call `get_node_reference("LightNode")` and return the exact function signature, parameter types, defaults, and a usage example.

### Get setup instructions

> "How do I set up ARCore with SceneView in my project?"

The assistant will call `get_ar_setup` and return the complete Gradle dependency, AndroidManifest.xml changes, session configuration options, and working AR starter templates.

### Get best practices

> "What are the performance best practices for SceneView?"

The assistant will call `get_best_practices("performance")` and return guidance on model optimization, runtime performance, environment/lighting setup, and common anti-patterns.

### Check the roadmap

> "Does SceneView support iOS?"

The assistant will call `get_platform_roadmap` and return the current multi-platform status and future plans.

### Migrate from v2 to v3

> "I'm upgrading from SceneView 2.x. What changed?"

The assistant will call `get_migration_guide` and return every breaking change with before/after code examples.

---

## Why Use This?

**Without** this MCP server, AI assistants may:
- Recommend deprecated Sceneform instead of SceneView
- Generate imperative View-based code instead of Compose
- Use wrong API signatures or outdated versions
- Miss ARCore integration patterns
- Forget null-checks on `rememberModelInstance`

**With** this MCP server, AI assistants:
- Always use the latest SceneView 3.3.0 API
- Generate correct Compose-native 3D/AR code
- Know about all 26+ node types and their exact parameters
- Validate code against 15+ rules before presenting it
- Provide working sample code for any scenario

---

## Troubleshooting

### "MCP server not found" or connection errors

1. Ensure Node.js 18+ is installed: `node --version`
2. Test the server manually: `npx sceneview-mcp` — it should start without errors and wait for input
3. Restart your AI client after changing the MCP configuration

### "npx command not found"

Install Node.js from [nodejs.org](https://nodejs.org/) (LTS recommended). npm and npx are included.

### Server starts but tools are not available

- In Claude Desktop, check the MCP icon in the input bar. It should show "sceneview" as connected.
- In Cursor, check **Settings > MCP** and verify the server shows a green status.
- Try restarting the MCP server by restarting your AI client.

### Stale data from `sceneview://known-issues`

GitHub issues are cached for 10 minutes. Wait for the cache to expire or restart the server.

### Validation false positives

The `validate_code` tool uses pattern matching and may flag valid code in some edge cases. If a validation warning seems incorrect, review the rule explanation in the output — it includes the rule ID and a detailed explanation.

### Firewall or proxy issues

The only network call is to the GitHub API (for `sceneview://known-issues`). All other tools work fully offline. If you are behind a corporate proxy, set the `HTTPS_PROXY` environment variable:

```json
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "sceneview-mcp"],
      "env": {
        "HTTPS_PROXY": "http://proxy.example.com:8080"
      }
    }
  }
}
```

---

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

## Legal

- [LICENSE](./LICENSE) — MIT License
- [TERMS.md](./TERMS.md) — Terms of Service
- [PRIVACY.md](./PRIVACY.md) — Privacy Policy (no data collected)

## License

MIT — see [LICENSE](./LICENSE).
