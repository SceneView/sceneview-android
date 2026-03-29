# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 10)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION (session 10)

### 1. Comprehensive quality audit — 143 files, ~59K lines cleaned
- **Version alignment**: ALL references updated from 3.5.0→3.5.1 across 100+ files (core, docs, MCPs, Flutter, React Native, Swift, samples, satellite MCPs)
- **model-viewer→sceneview.js migration**: Migrated ALL remaining model-viewer/Three.js pages to sceneview.js (embed, preview, claude-3d, platforms-showcase)
- **Three.js removal**: Deleted Three.js (53K LOC) and model-viewer.min.js from website-static
- **Dead page cleanup**: Deleted demo-dashboard.html, demo-ar-staging.html, filament-chart.html

### 2. SceneViewSwift fixes
- **SceneSnapshot**: Fixed visionOS compilation (ARView unavailable — changed guard to `#if os(iOS)`)
- **VideoNode**: Fixed memory leak — NotificationCenter observer never removed, added VideoLoopObserver with deinit cleanup
- **CameraNode**: Removed unnecessary `#if !os(macOS)` guards (PerspectiveCameraComponent available macOS 15+)
- **GeometryNode**: Removed unreachable `#else` dead code branches

### 3. Demo page fixes (visual verification with preview tools)
- **sceneview-demo.html**: Rewritten from raw Filament API to SceneView.modelViewer() — was crashing with SwapChain error, now renders perfectly
- **sceneview-3d-chart.html**: Removed 6 non-existent API calls (setQuality/setBloom/setVignette/setOrbitSpeed/clearLights/animateCamera), replaced with direct camera positioning
- **sceneview-garden-demo.html**: Same — removed 6 non-existent API calls
- **sceneview-architecture-demo.html**: Same — removed 7 non-existent API calls
- **showcase.html**: Cleaned model-viewer CSS selectors
- **sceneview-demo.html**: Fixed broken model buttons (removed dead models, added working ones)

### 4. CI workflow hardening
- **maintenance.yml**: Fixed Filament version grep pattern, removed failing `gh label create`, added graceful fallback
- **docs.yml**: Fixed download-artifact@v4, added deploy retry for concurrent conflicts
- **ios.yml**: Changed runner to macos-14 for consistency
- **All 10 workflows verified green**

### 5. MCP fixes
- Removed `engine = engine` from LightNode calls (3 files)
- Fixed stale roadmap references (v3.4.0/v3.5.0 "upcoming" → v3.6.0/v4.0.0)
- Fixed CDN URLs (unpkg→jsdelivr)
- All 1204 tests pass

### 6. Satellite MCPs version alignment
- mcp-automotive, mcp-gaming, mcp-healthcare, mcp-interior: all 3.5.0→3.5.1

## DECISIONS MADE
- model-viewer and Three.js are GONE from the website — everything uses sceneview.js
- Non-existent sceneview.js methods (setQuality, setBloom, addLight, animateCamera, etc.) removed from demos — these were aspirational API that was never implemented
- sceneview-demo.html fully rewritten to use SceneView.modelViewer() instead of raw Filament API

## WHAT WAS DONE IN SESSION 9

### 1. Release workflow fully fixed
- Maven Central: Fixed Gradle configuration cache incompatibility
- MCP npm: Uses package.json version, skips if already published
- sceneview-web npm: Fixed Gradle task, build output path, npm package name
- Create GitHub Release: Only runs on tag push

### 2. Version 3.5.1 fully released
- Maven Central: sceneview + arsceneview 3.5.1 published
- npm: sceneview-mcp 3.5.4, sceneview-web 3.5.1 published
- GitHub Release: v3.5.1 created

## CURRENT STATE
- **Active branch**: main
- **Latest release**: v3.5.1 (GitHub Release + Maven Central + npm)
- **MCP servers**: sceneview-mcp 3.5.4 on npm (32 tools, 1204 tests)
- **sceneview-web**: v3.5.1 on npm
- **Website**: All pages verified visually — zero JS errors, zero model-viewer, zero Three.js
- **CI**: All 10 workflows green

## NEXT STEPS
- Visual polish: garden demo model appears small — consider adjusting camera distance
- Consider implementing setQuality/setBloom/addLight in sceneview.js for richer demos
- Deploy website to sceneview.github.io (separate repo)
- iOS demo: verify SceneViewSwift fixes compile in Xcode
- Android demo: verify Play Store build
