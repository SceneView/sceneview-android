# SceneView GPT — OpenAI GPT Store

## Overview

Custom GPT for the OpenAI GPT Store that helps developers build 3D and AR apps using the SceneView SDK.

## GPT Store Configuration

### Basic Info
- **Name:** SceneView 3D & AR Assistant
- **Description:** Build 3D and AR apps for Android, iOS, Web, and more. Expert guidance for SceneView SDK — code generation, validation, and interactive 3D previews.
- **Category:** Programming

### Conversation Starters
1. "Build me an AR furniture placement app for Android"
2. "Create a 3D product viewer for my e-commerce site"
3. "How do I set up SceneView on iOS with SwiftUI?"
4. "Show me a physics simulation with collisions"

### Capabilities
- [x] Code Interpreter
- [x] Web Browsing
- [ ] DALL-E (not needed)

### Knowledge Files
Upload these files from this directory:
1. `knowledge-overview.md` — Platform overview, setup, architecture
2. `knowledge-api.md` — API reference, composables, node types
3. `knowledge-practices.md` — Best practices, patterns, troubleshooting
4. `knowledge-samples.md` — All 39 code sample descriptions and tags

### Actions (Optional)
Import `openapi-schema.json` to enable live code validation and 3D preview generation via the SceneView API.

## How to Publish

1. Go to https://chatgpt.com/gpts/editor
2. Create new GPT
3. Paste the contents of `system-prompt.md` as the Instructions
4. Upload all 4 knowledge files
5. Optionally import `openapi-schema.json` as an Action
6. Set conversation starters from above
7. Upload logo from `../branding/exports/`
8. Publish to GPT Store

## Links
- SDK: https://github.com/sceneview/sceneview
- Website: https://sceneview.github.io
- MCP: https://www.npmjs.com/package/sceneview-mcp
- Docs: https://sceneview.github.io/docs
