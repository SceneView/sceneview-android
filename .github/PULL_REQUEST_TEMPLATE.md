## What
<!-- Short description of the change -->

## Why
<!-- The problem this fixes or the feature this adds -->

## How
<!-- Approach taken. Any non-obvious design decisions -->

## Checklist

- [ ] `/review` passed — no threading, Compose API, or style issues
- [ ] `/document` run — KDoc updated for changed public APIs, `llms.txt` updated if signatures changed
- [ ] `/test` run — new tests added for new behaviour
- [ ] **Android**: `./gradlew :sceneview:assembleDebug :arsceneview:assembleDebug` passes
- [ ] **iOS** (if applicable): `swift build` passes in `SceneViewSwift/`
- [ ] **KMP** (if applicable): `./gradlew :sceneview-core:allTests` passes
- [ ] Filament materials recompiled (if `.mat` files changed)
- [ ] Minimal diff — no unrelated reformatting

> **AI-assisted contributions welcome.**
> Run `/contribute` in Claude Code for a guided workflow that handles review, docs, and test generation automatically.
> MCP server: `npx -y @sceneview/mcp` — gives Claude full SceneView API context.
