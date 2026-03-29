# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 4, continued)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION

### 1. Shared assets infrastructure
- Created `assets/` directory as single source of truth for all 3D resources
- `assets/catalog.json` — 22 models, 6 environments, 14 pending review items
- `.claude/scripts/sync-assets.sh` — check/fix/discover modes
- Sources: Sketchfab (API), Fab.com (browser), Poly Haven (API), KhronosGroup (GitHub)

### 2. Massive 3D asset expansion
Downloaded and integrated **16 new models** from 3 sources:

**From Sketchfab (CC-BY-4.0):**
- Red Car / Mitsubishi Evo (342KB) — from Fab.com
- Phoenix Bird (1.6MB, animated) by NORBERTO-3D
- Cyberpunk Car (2.4MB) by 4d_Bob
- Medieval Fantasy Book (3.7MB, animated) by Pixel
- Animated Butterfly (3.9MB, animated) by LasquetiSpice
- Retro Piano (4MB, CC-BY-NC) by DailyArt
- Mosquito in Amber (5.2MB) by Loïc Norgeot
- Animated Dragon (8MB, animated) by LasquetiSpice
- Cyberpunk Hovercar (7.2MB, animated) by Karol Miklas
- Cyberpunk Character (7.3MB) by Esk
- Ship in Clouds (7.6MB) by Bastien Genbrugge
- Fiat Punto GT (12MB) by Karol Miklas
- Black Dragon (13MB, animated) by Arturs J
- Porsche 911 Turbo (21MB) by Karol Miklas

**From KhronosGroup glTF-Sample-Assets (CC-BY/CC0):**
- Damaged Helmet (3.6MB) — iconic PBR reference
- Water Bottle (8.6MB) — transmission showcase
- Antique Camera (17MB) — detailed PBR
- Corset (13MB) — detailed PBR
- Dragon Attenuation (6.1MB) — crystal/transmission
- + Iridescent Dish, Sheen Chair, Toy Car (in assets, not all in carousel)

### 3. Demo apps updated
- **Android ExploreScreen**: 28 models in carousel (was 9)
- **iOS ExploreTab**: 16 real USDZ models (was 3 procedural shapes + 2 USDZ)
- All Xcode project.pbxproj refs added for 16 USDZ files

### 4. Fab.com complete catalog
Browsed all 11 Fab.com listings via Chrome:
- 1 downloaded (Red Car — small + free + GLB/USDZ)
- 3 free but UE-only format (no GLB/USDZ)
- 2 free but too large (52-234MB GLB)
- 5 paid

### 5. Scheduled asset discovery (2x/week)
- Task `discover-3d-assets` runs Monday + Thursday at 10 AM
- Searches: Sketchfab, Poly Haven, KhronosGroup, Smithsonian, Fab.com
- Quality-first: no longer skipping large models, size up to 50MB OK
- Auto-downloads, catalogs, syncs to all platforms, commits

### 6. Research: URL-based loading & asset hosting
- **Android**: `ModelLoader.loadModelInstanceAsync(url)` works for http/https via Fuel HTTP client
- **iOS**: Must download to temp file first, then `ModelNode.load(contentsOf: fileURL)`
- **Web**: URL is the native/only loading method
- **GitHub Releases**: 2GB/asset limit, CORS-enabled, viable as CDN for large models
- **Progressive loading**: Filament async API exists but is commented out in SceneView
- **Poly Haven**: 427 CC0 models but glTF-only (needs conversion, no GLB)
- **Smithsonian 3D**: API seems down/changed, needs browser investigation

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — URL-based model loading
- Add `rememberModelInstance(modelLoader, url: String)` overload that uses `loadModelInstanceAsync`
- Add download-to-temp wrapper in SceneViewSwift for remote USDZ
- Host largest models on GitHub Releases (`assets-v1` tag) for URL loading demo

### Priority 2 — App Store Connect (Thomas's action)
- Create app "SceneView Demo" bundle ID `io.github.sceneview.demo`

### Priority 3 — Play Store review
- Wait for Google to approve with updated screenshots

### Priority 4 — iOS HDR environments
- Add HDR environment files to iOS bundle for environment switching

### Priority 5 — Web demo asset sync
- Sync GLB models to `samples/web-demo/public/models/`

### Priority 6 — OpenCollective subscriptions
- Sketchfab Pro ($15/mo) for more downloads
- Consider for funding via OpenCollective

## ASSET CATALOG STATUS
- **22 models** in catalog from 3 sources
- **6 HDR environments** from Poly Haven
- **49 GLB** in Android, **16 USDZ** in iOS
- **Sources**: Sketchfab (16), KhronosGroup (5), Fab.com (1)
- **Licenses**: CC-BY-4.0 (19), CC0-1.0 (1), CC-BY-NC-4.0 (1)
- **Scheduled**: Discovery runs Mon+Thu 10 AM

## ACTIONS THOMAS
1. **App Store Connect**: create app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Play Store**: check if Google review passes

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
