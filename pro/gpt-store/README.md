# SceneView GPT — OpenAI GPT Store Preparation

## What the SceneView GPT Does

A custom GPT published to the OpenAI GPT Store that helps developers build 3D and AR
apps using SceneView. It can:

1. **Generate code** — Produce correct, compilable Kotlin (Jetpack Compose) or Swift
   (SwiftUI) code for 3D scenes and AR experiences on the first try.
2. **Explain the API** — Answer questions about SceneView composables, node types,
   resource loading, threading rules, and platform differences.
3. **Validate code** — Check user-provided SceneView snippets for common mistakes
   (threading violations, deprecated APIs, null-safety issues, LightNode trailing-lambda bug).
4. **Provide samples** — Return working starter templates for common scenarios
   (model viewer, AR placement, geometry, animation, image tracking, etc.).
5. **Troubleshoot** — Diagnose crashes, build failures, AR drift, and performance issues.

## How to Create It on the OpenAI Platform

### Step 1 — Open the GPT Builder

1. Go to <https://chat.openai.com/gpts/editor>
2. Click **"Create a GPT"**

### Step 2 — Configure

| Field | Value |
|---|---|
| **Name** | SceneView Assistant |
| **Description** | Expert AI assistant for building 3D and AR apps with SceneView — Android (Jetpack Compose, Filament, ARCore) and Apple (SwiftUI, RealityKit, ARKit). |
| **Instructions** | Paste the contents of [`gpt-instructions.md`](./gpt-instructions.md) |
| **Conversation starters** | See below |
| **Knowledge** | Upload `llms.txt` from the repo root |
| **Capabilities** | Enable "Code Interpreter" (for code validation) |
| **Actions** | Import [`openapi.yaml`](./openapi.yaml) for the plugin API |

### Step 3 — Conversation Starters

```
- "Build me a 3D model viewer in Jetpack Compose"
- "Set up an AR app that places objects on detected planes"
- "Show me how to add lighting and environment to a scene"
- "Help me migrate from SceneView 2.x to 3.0"
- "Create an iOS AR app with image tracking using SwiftUI"
```

### Step 4 — Knowledge Files

Upload the following files as GPT knowledge:

| File | Source | Purpose |
|---|---|---|
| `llms.txt` | Repo root | Complete API reference — composable signatures, node types, patterns |

The GPT instructions reference this file. The GPT will search it when answering
API questions, generating code, or validating snippets.

### Step 5 — Actions (Optional Plugin API)

If you have a hosted backend, import `openapi.yaml` to enable three action endpoints:

- `POST /generate` — Generate SceneView code from a natural language prompt
- `POST /validate` — Validate a SceneView code snippet
- `GET /samples` — List available code samples with optional tag filtering

These mirror the MCP server tools (`get_sample`, `validate_code`, `list_samples`)
and can be implemented by wrapping the same logic in a REST API.

### Step 6 — Publish

1. Click **"Save"** then **"Confirm"**
2. Set visibility to **"Everyone"** (public) or **"Anyone with the link"**
3. Submit to the GPT Store for review

## Revenue Opportunity

GPTs in the OpenAI GPT Store can generate revenue through the GPT Builder Revenue
Program. High-usage GPTs earn a share of ChatGPT Plus subscription revenue based on
user engagement. A specialized 3D/AR development assistant has strong niche appeal
among the growing community of spatial computing developers.

## Maintenance

When the SceneView API changes (new version, new nodes, new platforms):

1. Regenerate `llms.txt` with updated API reference
2. Re-upload it as GPT knowledge
3. Update `gpt-instructions.md` if new patterns or gotchas are introduced
4. Update `openapi.yaml` if new endpoints are added
