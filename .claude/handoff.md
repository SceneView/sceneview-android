# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 4)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION

### 1. Shared assets infrastructure
- Created `assets/` directory as single source of truth for all 3D resources
- `assets/catalog.json` tracks models, environments, licenses, sources
- `.claude/scripts/sync-assets.sh` distributes assets to platforms (check/fix/discover modes)
- Sources: Sketchfab (API), Fab.com (browser), Poly Haven (API)

### 2. Fab.com asset exploration
- Browsed all 11 Fab.com listings via Chrome browser
- Cataloged every listing: name, price, formats, license
- Downloaded "A Red Car" (342KB GLB + 236KB USDZ) — the only small free asset with GLB+USDZ
- Others either too large (52-234MB), UE-only format, or paid
- All findings recorded in catalog.json pendingReview section

### 3. Sketchfab model downloads
- Downloaded 3 new models via Sketchfab API:
  - **Animated Dragon** (8MB GLB, CC-BY, by LasquetiSpice) — 3 motion loops
  - **Animated Butterfly** (3.9MB GLB, CC-BY, by LasquetiSpice) — fluttering loop
  - **Retro Piano** (4MB GLB, CC-BY-NC, by DailyArt)
- All synced to Android (GLB) and iOS (USDZ) demo apps

### 4. Demo apps updated
- **Android ExploreScreen**: Added Red Car, Dragon, Butterfly, Piano to model carousel (now 12 models)
- **iOS ExploreTab**: Replaced Cube/Sphere/Cylinder with Red Car, Dragon, Butterfly, Piano (now 6 real models)
- Updated Xcode project.pbxproj with all new USDZ resources

### 5. Scheduled asset discovery
- Created weekly cron task `discover-3d-assets` — runs Mondays at 10 AM
- Searches Sketchfab API and Poly Haven for new free models/environments
- Auto-evaluates size, license, quality, downloads best finds, syncs to platforms

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — App Store Connect (Thomas's action)
- Create app "SceneView Demo" bundle ID `io.github.sceneview.demo`
- Once created, relaunch TestFlight workflow → automatic upload

### Priority 2 — App Store screenshots
- Upload the 9 iOS screenshots when review unlocks
- Screenshots available in `/tmp/store-screenshots/final/`

### Priority 3 — Play Store review
- Wait for Google to approve with updated screenshots

### Priority 4 — iOS HDR environments
- Add HDR environment files to iOS bundle for environment switching
- The Android demo already has 6 environments — iOS should match

### Priority 5 — Web demo asset sync
- Sync GLB models to `samples/web-demo/public/models/`
- Add model picker to web demo

### Priority 6 — More asset curation
- The scheduled task will handle ongoing discovery
- Consider downloading more Sketchfab models from search results (Pine Forest, Sleeping Cat, etc.)
- Keep looking for small, high-quality, realistic models

## ASSET CATALOG STATUS
- **Models**: 7 in catalog (game_boy_classic, tree_scene, toy_car, red_car, animated_dragon, animated_butterfly, retro_piano)
- **Environments**: 6 HDR (rooftop_night, studio, studio_warm, sunset, outdoor_cloudy, autumn_field)
- **Sources**: Sketchfab (API), Fab.com (browser), Poly Haven (API)
- **Scheduled**: Weekly discovery every Monday 10 AM

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
