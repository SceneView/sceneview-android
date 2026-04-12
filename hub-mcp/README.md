# hub-mcp

MCP Tools Hub -- 52 AI tools across 11 libraries, accessible via a single stdio server.

## Libraries included

| Library | Tools | Free tier |
|---|---|---|
| architecture-mcp | 10 | 2 |
| automotive-3d-mcp | 9 | 2 |
| healthcare-3d-mcp | 7 | 2 |
| realestate-mcp | 4 | 1 |
| french-admin-mcp | 4 | 1 |
| ecommerce-3d-mcp | 3 | 1 |
| legal-docs-mcp | 3 | 1 |
| finance-mcp | 3 | 1 |
| education-mcp | 3 | 1 |
| social-media-mcp | 3 | 1 |
| health-fitness-mcp | 3 | 1 |
| **Total** | **52** | **14** |

## Quick start

```bash
npx hub-mcp
```

### Claude Desktop

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "hub-mcp": {
      "command": "npx",
      "args": ["-y", "hub-mcp"]
    }
  }
}
```

## Free vs Pro

In **lite mode** (default), 14 discovery tools run locally. Pro tools return an upgrade prompt.

To unlock all 52 tools, set your API key:

```bash
HUB_MCP_API_KEY=sk-... npx hub-mcp
```

Or in Claude Desktop config:

```json
{
  "mcpServers": {
    "hub-mcp": {
      "command": "npx",
      "args": ["-y", "hub-mcp"],
      "env": {
        "HUB_MCP_API_KEY": "sk-..."
      }
    }
  }
}
```

Get an API key at https://hub-mcp.mcp-tools-lab.workers.dev/pricing

## Environment variables

| Variable | Description |
|---|---|
| `HUB_MCP_API_KEY` | API key for Pro tool access |
| `HUB_MCP_GATEWAY_URL` | Override gateway URL (default: `https://hub-mcp.mcp-tools-lab.workers.dev/mcp`) |
| `HUB_MCP_QUIET` | Set to `1` to suppress the startup banner |

## License

Apache-2.0
