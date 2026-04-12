# Privacy Policy — sceneview-mcp

**Last updated:** April 8, 2026

## Summary

**Free tier:** sceneview-mcp does not collect, store, or process any personal data.

**Pro tier:** Billing data is processed by Stripe and Polar.sh. The MCP server itself only validates your API key — no personal data is stored locally.

## Details

### Data Collection — Free Tier

- **No personal data collected** — We do not collect names, email addresses, IP addresses, or any other personally identifiable information.
- **No cookies** — The Service does not use cookies of any kind.
- **No behavioral tracking** — No fingerprinting or cross-session tracking.
- **No request logging** — Tool arguments, results, and prompt content are never logged or transmitted.
- **Anonymous telemetry** — The free tier sends an anonymous, opt-out telemetry ping on the MCP handshake and on each tool call. See the "Telemetry (Free Tier)" section below for what's collected and how to opt out.

### Telemetry (Free Tier)

To understand which MCP clients our users run on and which tools are actually useful, sceneview-mcp sends a minimal, anonymous telemetry event at two moments:

1. **On initialization** — once per MCP client handshake.
2. **On tool call** — once per invocation of any tool.

**What's collected (exhaustive list):**

| Field | Example | Why |
|---|---|---|
| `timestamp` | `2026-04-11T09:23:15.421Z` | Time-bucketing for weekly rollups |
| `event` | `"init"` or `"tool"` | Distinguish handshake from tool calls |
| `client` | `"claude-desktop"` | Which MCP client is being used |
| `clientVersion` | `"0.11.0"` | Client version as reported during the MCP handshake |
| `mcpVersion` | `"4.0.0-rc.1"` | Version of this MCP server |
| `tier` | `"free"` or `"pro"` | Tier the tool resolved to (not the user's subscription) |
| `tool` | `"get_node_reference"` | Tool name — only for `event: "tool"` |

**What's NEVER collected:**

- IP address, hostname, OS user, machine identifier
- Prompt text, tool arguments, tool results
- API keys, billing information, email addresses
- Files read or generated, project paths
- Any other personally identifiable information

The telemetry endpoint is a Cloudflare Worker that deliberately does not log client IP addresses or forward them to any downstream store. Events are aggregated into anonymous weekly counters only.

**How to opt out:**

Set `SCENEVIEW_TELEMETRY=0` in the environment where the MCP server runs. For Claude Desktop, add it to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "sceneview-mcp"],
      "env": { "SCENEVIEW_TELEMETRY": "0" }
    }
  }
}
```

Telemetry is also automatically skipped when `CI=true` is set (continuous-integration environments).

**Why it's opt-out, not opt-in:** opt-in telemetry systematically under-represents the exact users we need to learn from, which makes product decisions worse for everyone. The payload is minimal enough and the opt-out mechanism simple enough that we believe this is the right trade-off. If you disagree, the one-line opt-out above is a first-class citizen.

### Data Collection — Pro Tier

When you subscribe to SceneView MCP Pro:
- **Billing data** (email, payment method) is collected and processed by **Stripe** and **Polar.sh** under their respective privacy policies. We do not store this data ourselves.
- **API key validation** — Your API key (a Stripe subscription ID) is sent to the Stripe API to verify your subscription status. The result is cached in-memory for 5 minutes and then discarded. No validation results are persisted to disk.
- **Usage logging** — Tool invocations are logged to stderr for debugging only. These logs are ephemeral (process-lifetime) and contain only a truncated key prefix and the tool name — no personal data.

### Data Processing

- All MCP tool calls are processed in-memory and discarded immediately after the response is returned.
- No data is persisted to disk, database, or cloud storage by the MCP server itself.

### Third-Party Services

| Service | Purpose | Data Sent |
|---|---|---|
| **GitHub public API** | Fetch open issues for `sceneview://known-issues` | None (public API, no auth) |
| **Stripe API** | Validate Pro subscription status | API key (subscription ID) only |

- GitHub API results are cached in-memory for 10 minutes
- Stripe validation results are cached in-memory for 5 minutes
- Both caches are discarded when the process ends

### GDPR Compliance

**Free tier:** Compliant by design — no personal data is collected or processed.

**Pro tier:** Billing data is processed by Stripe (data processor) and Polar.sh (payment platform). Thomas Gorisse acts as data controller for the billing relationship. Your rights under GDPR:
- **Right to access**: Contact us for details on your billing data held by Stripe/Polar.
- **Right to deletion**: Cancel your subscription and contact us to request data erasure.
- **Right to portability**: Available through Stripe/Polar dashboards.
- **Data Protection Officer**: Not required under current processing scale.

### CCPA Compliance

This Service is compliant with the California Consumer Privacy Act (CCPA) by design: no personal information is collected, sold, or shared.

### Children's Privacy

This Service does not knowingly collect any data from anyone, including children under 13.

### Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be reflected in the "Last updated" date above.

### Contact

Thomas Gorisse — [https://github.com/sceneview/sceneview](https://github.com/sceneview/sceneview)
