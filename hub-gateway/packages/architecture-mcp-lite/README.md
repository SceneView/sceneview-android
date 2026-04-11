# architecture-mcp — lite proxy (2.0.0-beta.1)

A thin Node MCP server that forwards every tool call to the hosted
hub gateway at
[`hub-mcp.mcp-tools-lab.workers.dev`](https://hub-mcp.mcp-tools-lab.workers.dev/).

## Why this exists

Previously, `architecture-mcp` shipped the full tool implementation
inside the npm package. As of 2.x, the tool logic lives server-side
on the hub gateway, so:

- one **Portfolio Access** subscription (€29/mo) covers the entire
  non-SceneView MCP portfolio instead of a per-package paywall;
- tool updates ship instantly without a new npm release;
- free-tier users still get 100 tool calls/month with no API key;
- Pro tools unlock with a single `HUB_MCP_API_KEY`.

## Install

```bash
npm install -g architecture-mcp@beta
```

or run ephemerally via npx:

```bash
HUB_MCP_API_KEY=sk_... npx architecture-mcp@beta
```

## Configure

| Env var            | Default                                               | Purpose                        |
|--------------------|-------------------------------------------------------|--------------------------------|
| `HUB_MCP_API_KEY`  | *(unset)*                                             | Bearer token for Pro tools     |
| `HUB_MCP_URL`      | `https://hub-mcp.mcp-tools-lab.workers.dev/mcp`       | Override for local dev / tests |

Get a key by subscribing at
[`hub-mcp.mcp-tools-lab.workers.dev/pricing`](https://hub-mcp.mcp-tools-lab.workers.dev/pricing).

## Claude Desktop config

```json
{
  "mcpServers": {
    "architecture": {
      "command": "npx",
      "args": ["-y", "architecture-mcp@beta"],
      "env": { "HUB_MCP_API_KEY": "sk_..." }
    }
  }
}
```

## State

This is the **first beta** of the lite proxy mode. The hub gateway
still ships only stub tool handlers for the pilot — the real
architecture-mcp logic will land in a follow-up session.

Status: **do not publish to npm yet**. Wait for:

1. Hub gateway deployed on `workers.dev`.
2. Real architecture-mcp tool logic vendored into the gateway.
3. Stripe prices wired + checkout tested in TEST mode.
