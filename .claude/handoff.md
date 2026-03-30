# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 30 mars 2026 (session 12)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 12)

### 1. Security audit — API key leak check
- Deep scanned entire git history for API keys (AIza*, AQ.*, sk-*, etc.)
- **CONFIRMED: zero API keys in repo history** — all clean
- Stitch API key only exists in `~/.claude/stitch-wrapper.sh` (local, not tracked)

### 2. Stitch MCP — Fixed and verified
- `.mcp.json` in project root with correct config (gitignored)
- Wrapper script `~/.claude/stitch-wrapper.sh` tested: **12 tools discovered, proxy running**
- `stitch-mcp` v0.5.1 installed globally via npm
- **Root cause of Stitch not loading**: MCP servers load at session start — `.mcp.json` was reconfigured mid-session
- **FIX**: Just start a new Claude Code session → Stitch loads automatically

### 3. Git cleanup
- Committed `.mcp.json` removal from git tracking + added to `.gitignore`

## Previous session (session 11)
- Repository reorganization (342 files, root: 67→33 items)
- Version cleanup 3.5.1→3.5.2 (18 files)
- DESIGN.md in Google Stitch format
- Website M3 Expressive + Liquid Glass redesign (deployed)
- Google Stitch MCP configured

## DECISIONS MADE
- Website uses M3 Expressive (structure) + Liquid Glass (floating surfaces) — correct for web
- Android demo should use Material 3 Expressive (Compose Material 3)
- iOS demo should use Apple Liquid Glass / HIG (SwiftUI native) — NOT Material Design
- Dark mode hero title: solid white text (gradient text invisible in dark mode)
- `.mcp.json` must stay gitignored (contains local paths)

## CURRENT STATE
- **Active branch**: main
- **Latest release**: v3.5.2 (ALL PUBLISHED — Maven Central + npm + GitHub + Stores)
- **MCP servers**: sceneview-mcp 3.5.4 on npm (32 tools, 1204 tests), 9 MCPs total
- **sceneview-web**: v3.5.2 on npm
- **Website**: sceneview.github.io — M3 Expressive + Liquid Glass redesign deployed
- **Google Stitch**: MCP configured, API key set
- **GitHub orgs**: sceneview, sceneview-tools, mcp-tools-lab

## NEXT STEPS (priority order)

### ✅ BLOCKER RESOLVED — Stitch MCP ready
- `.mcp.json` is in project root, gitignored, config correct
- Wrapper at `~/.claude/stitch-wrapper.sh` tested and working (12 tools)
- **Just start a new Claude Code session** → Stitch tools appear automatically
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
