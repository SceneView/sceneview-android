# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 30 mars 2026 (session 11)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 11)

### 1. Repository reorganization (342 files changed)
- MCPs satellites → `mcp/packages/` (automotive, gaming, healthcare, interior)
- MCP docs → `mcp/docs/` (registry-submission-guide, strategy-report)
- Community docs → `.github/` (CoC, Governance, Support, Security, Sponsors, Privacy)
- `scripts/` → `tools/`, `config/detekt/` → `buildSrc/config/detekt/`
- Removed tracked build artifacts (docs/site, kotlin-js-store, buildSrc/.gradle)
- Root items: 67 → 33

### 2. Version cleanup (18 files)
- Fixed all remaining 3.5.1 → 3.5.2 references across docs, samples, website, Flutter, iOS, MCP READMEs

### 3. DESIGN.md — Google Stitch format
- Created complete design system document in Stitch agent-friendly format
- Enhanced with M3 Expressive philosophy, Liquid Glass specs, spring motion, M3 shape scale

### 4. Website M3 Expressive + Liquid Glass redesign
- `styles.css` complete rewrite (497 insertions): design tokens, spring animations, liquid glass nav/cards, dark mode
- Verified via preview tools — light + dark mode working
- Deployed to sceneview.github.io

### 5. Google Stitch MCP configured
- `.mcp.json` added with stitch-mcp proxy server
- API key saved in `~/.zshrc` as `STITCH_API_KEY`
- API key backed up in `profile-private/preferences/api-keys.md`
- Config synced to `profile-private/sync/from-perso/`

## DECISIONS MADE
- Website uses M3 Expressive (structure) + Liquid Glass (floating surfaces) — correct for web
- Android demo should use Material 3 Expressive (Compose Material 3)
- iOS demo should use Apple Liquid Glass / HIG (SwiftUI native) — NOT Material Design
- Dark mode hero title: solid white text (gradient text invisible in dark mode)
- Google Stitch needs API key from https://stitch.withgoogle.com — cannot be automated

## CURRENT STATE
- **Active branch**: main
- **Latest release**: v3.5.2 (ALL PUBLISHED — Maven Central + npm + GitHub + Stores)
- **MCP servers**: sceneview-mcp 3.5.4 on npm (32 tools, 1204 tests), 9 MCPs total
- **sceneview-web**: v3.5.2 on npm
- **Website**: sceneview.github.io — M3 Expressive + Liquid Glass redesign deployed
- **Google Stitch**: MCP configured, API key set
- **GitHub orgs**: sceneview, sceneview-tools, mcp-tools-lab

## NEXT STEPS (priority order)

### BLOCKER — Google Stitch MCP must be loaded first
- **STITCH_API_KEY** is in `~/.zshrc` — user must restart Claude Code to load the MCP
- Once loaded, ALL visual work goes through Stitch

### Phase 1 — FULL REDESIGN VIA GOOGLE STITCH
Everything visual must be redesigned using Google Stitch as the design tool.
Stitch generates the design → Claude applies it in code. NO manual CSS/UI writing.

1. **Website** (sceneview.github.io) — Full redesign via Stitch
   - Give Stitch context: DESIGN.md, SDK for 3D/AR, developer audience
   - M3 Expressive + Liquid Glass mix for web
   - All pages: index.html, showcase.html, claude-3d.html, etc.
2. **Android demo app** — Theme via Stitch (M3 Expressive)
   - Color.kt, Theme.kt, Shape.kt, Type.kt
3. **iOS demo app** — Theme via Stitch (Liquid Glass / Apple HIG)
   - All SwiftUI views (19 views, no centralized theme currently)
4. **Docs MkDocs** — CSS via Stitch
5. **All other demos** — web-demo, tv-demo, etc.
6. **Store assets** — Screenshots with new design

### Phase 2 — Post-redesign
- v3.6.0 roadmap: API simplification
- sceneview.js enhancements (setQuality, setBloom, addLight)
- iOS: verify SceneViewSwift fixes compile in Xcode

## RULES REMINDER
- **STITCH MANDATORY** — ALL design/UI work goes through Google Stitch MCP. NEVER write CSS/theme by hand.
- ALWAYS save API keys/credentials in `profile-private/preferences/api-keys.md` + `~/.zshrc`
- ALWAYS push `profile-private` after saving sensitive data
- Material 3 Expressive = Android/Web, Liquid Glass = Apple platforms
