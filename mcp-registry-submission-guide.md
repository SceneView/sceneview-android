# MCP Registry Submission Guide

Last updated: 2026-03-27

## MCPs to Submit

| # | Package | npm | Version | Category |
|---|---------|-----|---------|----------|
| 1 | sceneview-mcp | `sceneview-mcp` | 3.5.2 | Development / 3D & AR |
| 2 | french-admin-mcp | `french-admin-mcp` | 2.1.3 | Productivity / Administration |
| 3 | interior-design-3d-mcp | `interior-design-3d-mcp` | 1.0.0 | Design / 3D Visualization |
| 4 | architecture-mcp | `architecture-mcp` | 2.0.3 | Design / Architecture |
| 5 | gaming-3d-mcp | `gaming-3d-mcp` | 1.0.0 | Development / Gaming |
| 6 | ecommerce-3d-mcp | `ecommerce-3d-mcp` | 2.0.3 | Productivity / E-commerce |

---

## Registry 1: mcpservers.org

**URL:** https://mcpservers.org/submit
**Method:** Web form (free or $39 premium)
**Effort:** ~2 min per MCP

### Fields

| Field | Required | Notes |
|-------|----------|-------|
| Server Name | Yes | e.g. "SceneView MCP" |
| Short Description | Yes | One-liner |
| Link | Yes | GitHub repo or npm page |
| Category | Yes | Dropdown: Web Scraping, Communication, Productivity, Development, Database, Cloud Service, File System, Cloud Storage, Version Control, Other |
| Contact Email | Yes | Your email |

### Premium option ($39/server)
- Faster review
- Official badge
- Dofollow link

### Submissions to make

**1. sceneview-mcp**
- Name: `SceneView MCP`
- Description: `MCP server for 3D & AR SDK code generation — Android (Jetpack Compose, Filament) and iOS (SwiftUI, RealityKit). 22 tools for model loading, AR scenes, animations, and geometry.`
- Link: `https://github.com/sceneview/sceneview`
- Category: `Development`

**2. french-admin-mcp**
- Name: `French Admin MCP`
- Description: `MCP server for French administration — taxes, URSSAF, CAF, unemployment (ARE), severance, CPF training, invoices, and administrative letters. 2026 tax brackets.`
- Link: `https://www.npmjs.com/package/french-admin-mcp`
- Category: `Productivity`

**3. interior-design-3d-mcp**
- Name: `Interior Design 3D MCP`
- Description: `MCP server for interior design 3D visualization — room planning, AR furniture placement, material switching, lighting design, and room tours.`
- Link: `https://www.npmjs.com/package/interior-design-3d-mcp`
- Category: `Other`

**4. architecture-mcp**
- Name: `Architecture 3D MCP`
- Description: `MCP server for architects — 3D concepts, floor plans, material palettes, lighting analysis, cost estimates, and contractor specs.`
- Link: `https://www.npmjs.com/package/architecture-mcp`
- Category: `Other`

**5. gaming-3d-mcp**
- Name: `Gaming 3D MCP`
- Description: `MCP server for gaming 3D visualization — character viewers, level editors, physics games, particle effects, 3D inventories, and game-ready models.`
- Link: `https://www.npmjs.com/package/gaming-3d-mcp`
- Category: `Development`

**6. ecommerce-3d-mcp**
- Name: `E-commerce 3D MCP`
- Description: `MCP server for e-commerce 3D product visualization — model viewers, AR try-on, product configurators, Shopify/WooCommerce integration, conversion optimization.`
- Link: `https://www.npmjs.com/package/ecommerce-3d-mcp`
- Category: `Productivity`

### Action required
Go to https://mcpservers.org/submit and fill the form 6 times (once per MCP). Free submissions work fine. Approval comes via email.

---

## Registry 2: PulseMCP

**URL:** https://pulsemcp.com/submit
**Method:** Web form (URL only) + ingestion from Official MCP Registry
**Effort:** ~1 min per MCP

### Fields

| Field | Required | Notes |
|-------|----------|-------|
| Type | Yes | Toggle: "MCP Server" or "MCP Client" |
| URL | Yes | GitHub repo, subfolder, or npm page |

### Important note
PulseMCP also ingests from the **Official MCP Registry** (modelcontextprotocol/registry) daily. If you publish there first, PulseMCP picks it up automatically within a week. If it takes longer, email hello@pulsemcp.com.

### Submissions to make

For each MCP, go to https://pulsemcp.com/submit and:
1. Select **MCP Server**
2. Enter the URL (use npm package URL or GitHub repo)

| MCP | URL to submit |
|-----|---------------|
| sceneview-mcp | `https://github.com/sceneview/sceneview/tree/main/mcp` |
| french-admin-mcp | `https://www.npmjs.com/package/french-admin-mcp` |
| interior-design-3d-mcp | `https://www.npmjs.com/package/interior-design-3d-mcp` |
| architecture-mcp | `https://www.npmjs.com/package/architecture-mcp` |
| gaming-3d-mcp | `https://www.npmjs.com/package/gaming-3d-mcp` |
| ecommerce-3d-mcp | `https://www.npmjs.com/package/ecommerce-3d-mcp` |

### Action required
Go to https://pulsemcp.com/submit and submit each URL. Quick process — just URL + type toggle.

---

## Registry 3: Glama.ai

**URL:** https://glama.ai/mcp/servers
**Method:** "Add Server" button on the servers page
**Effort:** ~1 min per MCP

### Process
1. Go to https://glama.ai/mcp/servers
2. Click **"Add Server"** button
3. Fill in the form (likely asks for GitHub/npm URL)
4. Submit

Glama indexes, scans, and ranks servers based on security, compatibility, and ease of use. They have 20,000+ servers indexed.

### URLs to submit
Same as PulseMCP table above. Submit each npm package URL or GitHub link.

### Action required
Go to https://glama.ai/mcp/servers, click "Add Server" for each MCP, and fill in the details.

---

## Registry 4: mcp.so

**URL:** https://mcp.so/submit
**Method:** Web form OR GitHub issue on chatmcp/mcpso
**Effort:** ~3 min per MCP

### Option A: Web form at mcp.so/submit

| Field | Required | Notes |
|-------|----------|-------|
| Type | Yes* | "MCP Server" or "MCP Client" |
| Name | Yes* | Server name |
| URL | Yes* | GitHub repo or npm URL |
| Server Config | No | JSON config block |
| Is Innovation | No | Checkbox |
| Is DXT | No | Desktop Extensions checkbox |

### Option B: GitHub issue (recommended — more detail, better listing)

Create issues at: https://github.com/chatmcp/mcpso/issues/new

Use this format per issue:

```markdown
# [Server Name] MCP Server

**Name:** [name]
**npm:** `[package-name]`
**URL:** [GitHub or npm URL]
**Website:** [website URL if any]

**Description:** [what it does]

**Category:** [category]
**Tags:** [comma-separated tags]

**Install:**

```json
{
  "mcpServers": {
    "[server-id]": {
      "command": "npx",
      "args": ["[package-name]"]
    }
  }
}
```
```

### Issues to create

**Issue 1: sceneview-mcp**
- Title: `Add SceneView MCP — 3D & AR SDK code generation for Android and iOS`
- Body:
```
# SceneView MCP Server

**Name:** SceneView MCP
**npm:** `sceneview-mcp`
**URL:** https://github.com/sceneview/sceneview
**Website:** https://sceneview.github.io

**Description:** MCP server for SceneView — cross-platform 3D & AR SDK. 22 tools for generating correct Jetpack Compose 3D scenes, AR experiences, model loading, animations, geometry, and iOS SwiftUI/RealityKit code.

**Category:** Development
**Tags:** 3d, ar, android, ios, jetpack-compose, swiftui, filament, realitykit, arcore, arkit, sdk

**Install:**
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["sceneview-mcp"]
    }
  }
}
```

**Issue 2: french-admin-mcp**
- Title: `Add French Admin MCP — French administration tools (taxes, URSSAF, CAF, ARE)`
- Body:
```
# French Admin MCP Server

**Name:** French Admin MCP
**npm:** `french-admin-mcp`
**URL:** https://www.npmjs.com/package/french-admin-mcp

**Description:** MCP server for French administration — income tax simulation, URSSAF contributions, CAF benefits, unemployment (ARE) calculation, severance pay, CPF training credits, invoicing, and administrative letter generation. Updated with 2026 tax brackets.

**Category:** Productivity
**Tags:** france, administration, taxes, urssaf, caf, impots, french, government

**Install:**
{
  "mcpServers": {
    "french-admin": {
      "command": "npx",
      "args": ["french-admin-mcp"]
    }
  }
}
```

**Issue 3: interior-design-3d-mcp**
- Title: `Add Interior Design 3D MCP — room planning and AR furniture placement`
- Body:
```
# Interior Design 3D MCP Server

**Name:** Interior Design 3D MCP
**npm:** `interior-design-3d-mcp`
**URL:** https://www.npmjs.com/package/interior-design-3d-mcp

**Description:** MCP server for interior design 3D visualization — room planning, AR furniture placement, material switching, lighting design, and virtual room tours.

**Category:** Design
**Tags:** interior-design, 3d, ar, furniture, room-planning, visualization, home

**Install:**
{
  "mcpServers": {
    "interior-design-3d": {
      "command": "npx",
      "args": ["interior-design-3d-mcp"]
    }
  }
}
```

**Issue 4: architecture-mcp**
- Title: `Add Architecture 3D MCP — 3D concepts, floor plans, and contractor specs`
- Body:
```
# Architecture 3D MCP Server

**Name:** Architecture 3D MCP
**npm:** `architecture-mcp`
**URL:** https://www.npmjs.com/package/architecture-mcp

**Description:** MCP server for architects and interior designers — 3D building concepts, floor plans, material palettes, lighting analysis, cost estimates, and contractor specifications.

**Category:** Design
**Tags:** architecture, 3d, floor-plan, building, design, construction, visualization

**Install:**
{
  "mcpServers": {
    "architecture": {
      "command": "npx",
      "args": ["architecture-mcp"]
    }
  }
}
```

**Issue 5: gaming-3d-mcp**
- Title: `Add Gaming 3D MCP — character viewers, level editors, and game-ready models`
- Body:
```
# Gaming 3D MCP Server

**Name:** Gaming 3D MCP
**npm:** `gaming-3d-mcp`
**URL:** https://www.npmjs.com/package/gaming-3d-mcp

**Description:** MCP server for gaming 3D visualization — character viewers, level editors, physics games, particle effects, 3D inventories, and game-ready model generation.

**Category:** Development
**Tags:** gaming, 3d, game-dev, character, level-editor, physics, particles, visualization

**Install:**
{
  "mcpServers": {
    "gaming-3d": {
      "command": "npx",
      "args": ["gaming-3d-mcp"]
    }
  }
}
```

**Issue 6: ecommerce-3d-mcp**
- Title: `Add E-commerce 3D MCP — product visualization, AR try-on, and configurators`
- Body:
```
# E-commerce 3D MCP Server

**Name:** E-commerce 3D MCP
**npm:** `ecommerce-3d-mcp`
**URL:** https://www.npmjs.com/package/ecommerce-3d-mcp

**Description:** MCP server for e-commerce 3D product visualization — model viewers, AR try-on, product configurators, Shopify/WooCommerce integration, conversion optimization, and size guides.

**Category:** Productivity
**Tags:** ecommerce, 3d, ar, product-visualization, shopify, woocommerce, try-on, configurator

**Install:**
{
  "mcpServers": {
    "ecommerce-3d": {
      "command": "npx",
      "args": ["ecommerce-3d-mcp"]
    }
  }
}
```

### Action required
Either use the web form at https://mcp.so/submit (faster, less detail) or create 6 GitHub issues at https://github.com/chatmcp/mcpso/issues/new (better listing quality).

---

## Bonus: Official MCP Registry (modelcontextprotocol/registry)

**URL:** https://github.com/modelcontextprotocol/registry
**Method:** CLI tool (`mcp-publisher`)
**Effort:** ~15 min setup, then ~2 min per MCP

This is the official Anthropic-maintained registry. PulseMCP ingests from it automatically.

### Setup

```bash
# Clone the registry repo
git clone https://github.com/modelcontextprotocol/registry.git
cd registry

# Build the publisher CLI
make publisher

# Authenticate with GitHub
./bin/mcp-publisher login --github
```

### Namespace
Since the GitHub org is `sceneview`, the namespace would be `io.github.sceneview/[server-name]`.

### Publishing
```bash
./bin/mcp-publisher publish
```

The exact server.json format is in the registry docs. This requires more setup but gives the highest credibility and auto-syndication to PulseMCP.

### Action required
1. Clone the registry repo
2. Build the publisher tool
3. Authenticate with GitHub (as sceneview org)
4. Create server.json for each MCP
5. Publish each one

---

## Summary: Priority Action Plan

| Step | Registry | Method | Time | Impact |
|------|----------|--------|------|--------|
| 1 | **mcp.so** | GitHub issues (6 issues) | 15 min | 19,000+ servers indexed, high traffic |
| 2 | **mcpservers.org** | Web form (6 submissions) | 12 min | Clean directory, good SEO |
| 3 | **PulseMCP** | Web form (6 URLs) | 6 min | Curated, ingests from official registry |
| 4 | **Glama.ai** | Add Server button (6 times) | 6 min | 20,000+ servers, security-ranked |
| 5 | **Official Registry** | CLI publisher tool | 30 min | Highest credibility, auto-syndicates |

**Total estimated time: ~1 hour for all registries**

All submissions are manual (web forms or GitHub issues). No programmatic API is available for bulk submission.
