# Session Handoff — SceneView

> This file is the structured handoff artifact between Claude Code sessions.
> Read this file at the START of every session. Update it at the END.
> Pattern: Anthropic harness design — context resets with structured handoffs.

## Last Session Summary

**Date:** 2026-03-24 (evening/night session)
**Branch:** `feat/multi-platform-expansion`
**PR:** https://github.com/SceneView/sceneview/pull/709 (ready for review)
**Commits:** 28 total

### What Was Done

1. **sceneview-web** — Kotlin/JS + Filament.js WASM module
   - Bindings, DSL builder, orbit camera (mouse/touch/pinch), auto-resize
   - Animation time tracking with looping
   - Tests: 226 JS tests pass
2. **sceneview-desktop** — Compose Desktop + LWJGL scaffold
3. **Samples** — tv-model-viewer (Android TV), web-model-viewer, ios-demo
4. **Bridges** — Flutter + React Native (Android + iOS, native rendering)
5. **CI/CD** — web-desktop CI job, app-store.yml, release npm publish
6. **Website Kobweb** — 5 pages M3 integrated, DamagedHelmet.glb, build OK
7. **MCP** — 2 web samples, get_web_setup tool, web validator (384 tests)
8. **Docs** — 4 quickstart guides, llms.txt synced, README 9 platforms

### Known Issues

- **material-icons-extended 1.10.5** — not on Maven Central yet, blocks model-viewer and sceneview-demo APK builds (pre-existing, not from this PR)
- **Website mobile navbar** — hamburger menu hidden by Compose HTML inline styles overriding CSS media queries (architectural limitation of Kobweb)
- **Hero 3D viewer** — model-viewer web component needs CDN access, empty in local dev

### Decisions Made

- **Web renderer**: Filament.js (same engine as Android) over Three.js/Babylon.js
- **Desktop**: Compose Desktop + LWJGL scaffold (Filament JNI not yet available)
- **settings.gradle**: PREFER_PROJECT (needed for Kotlin/JS Node.js repos)
- **Android Auto/CarPlay**: excluded (no custom views, not suitable for 3D)

### What's Next (Priority Order)

1. Merge PR #709 after review
2. Fix material-icons-extended version (downgrade or wait for publish)
3. Website: fix mobile navbar (refactor to CSS classes instead of inline styles)
4. Complete Filament JNI for desktop (requires building Filament from source)
5. visionOS spatial features (immersive spaces, hand tracking)
6. WebXR exploration for web AR (experimental)

## Sprint Contract Template

When starting a new feature chunk, fill this in:

```
### Sprint: [feature name]
**Scope:** [what's included]
**Done when:**
- [ ] Compiles on all targets
- [ ] Tests pass (unit + integration)
- [ ] Review checklist passes (/review)
- [ ] CLAUDE.md updated
- [ ] Committed and pushed
**Not included:** [explicit exclusions]
```

## Agent Roles

| Role | Command | Purpose |
|---|---|---|
| Generator | (default) | Write code, create features |
| Evaluator | `/review` | Verify quality, find bugs (separate from generator) |
| Documenter | `/document` | Generate KDoc/docs for changed APIs |
| Tester | `/test` | Audit test coverage, write missing tests |
| Maintainer | `/maintain` | Daily maintenance sweep |
| Releaser | `/release` | Guided release workflow |
