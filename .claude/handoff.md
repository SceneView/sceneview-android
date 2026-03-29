# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 5)
**Branch:** main (all pushed to origin)

## WHAT WAS DONE THIS SESSION

### 1. Massive hype brand 3D asset expansion
Downloaded and integrated **12 new models** from Sketchfab:

**Hype brands (9 models):**
- Lamborghini Countach LPI 800-4 (12MB, Lexyc16, CC-BY-NC, 2005 likes)
- Nike Air Jordan (30MB, Ar41k, CC-BY, 723 likes)
- Ferrari F40 (7.7MB, Black Snow, CC-BY, 721 likes)
- Porsche 911 (930) Turbo (21MB, Lionsharp Studios, CC-BY, 6892 likes)
- PS5 DualSense (5.6MB, AHarmlessPotato, CC-BY, 537 likes)
- Tesla Cybertruck (1.8MB, hashikemu, CC-BY, 377 likes)
- Mercedes A45 AMG (15MB, Lexyc16, CC-BY-NC, 750 likes)
- Nintendo Switch Diorama (0.7MB, Mikkel Garde Blaase, CC-BY-NC, 829 likes)
- BMW M3 E30 (9.8MB, Lexyc16, CC-BY-NC, 1051 likes)

**From user-provided links (3 models):**
- Shelby Cobra 427 (15MB, vecarz, CC-BY-NC-SA)
- Audi TT Coupe (4.4MB, Ddiaz Design, CC-BY-NC-SA)
- Earthquake California M5.2 (39MB, Kyle, CC-BY)

**3 models from user links NOT downloadable** (Standard license):
- News Broadcast Studio (McWinterL)
- Chocolate Panettone (Anderson Rohr)
- Indoor Plants Pack 53 (AllQuad)

### 2. All platforms synced
Every model distributed to ALL platforms with carousel/picker updates:
- Android ExploreScreen: 41 models in carousel
- iOS ExploreTab: 28 USDZ (all in Xcode project.pbxproj)
- Website web demo: 28 buttons
- Web demo Kotlin/JS: 24 models
- Flutter: 15 models with picker
- React Native: 16 models with picker

### 3. Financial infrastructure research
Deep research on funding options:

**Key findings:**
- Sketchfab Pro ($15/mo) = NOT worth it (doesn't unlock more downloads or Standard models)
- Fab.com = NOT useful (no API, EULA incompatible with open-source repos)
- Open Collective virtual cards = DEAD (OSC and OCE both discontinued)
- Open Source Collective (OSC) = still active (unlike OCF which dissolved end 2024)

**Current balance:** $2,338 USD on Open Collective (OSC fiscal host)

**Recommended setup:**
- GitHub Sponsors (0% fees) = primary donation channel
- Open Collective = corporate sponsors + expense transparency
- Polar.sh = issue funding + tiers
- Auto-entrepreneur = for personal revenue when amounts increase
- NO association loi 1901 needed at current scale

### 4. Sponsoring visibility improvements
- **SPONSORS.md**: Complete rewrite with 3 funding platforms, tiers, where-money-goes section
- **README.md**: Added Open Collective badge, expanded support section with all 3 platforms
- **Website index.html**: Enhanced sponsor card with OC + Polar links, added OC to footer
- **FUNDING.yml**: Already had all 3 platforms configured

## CI TO CHECK AT START

```bash
gh run list --branch main --limit 5
```

## WHAT REMAINS TO DO

### Priority 1 — Thomas actions (accounts)
- **Activate Polar.sh** — go to polar.sh, connect SceneView GitHub org, set up tiers
- **Enrich GitHub Sponsors tiers** — add $50 and $100 tiers with better perks
- **App Store Connect** — create app "SceneView Demo" bundle ID `io.github.sceneview.demo`

### Priority 2 — URL-based model loading
- Add `rememberModelInstance(modelLoader, url: String)` overload using `loadModelInstanceAsync`
- Add download-to-temp wrapper in SceneViewSwift for remote USDZ
- Host largest models on GitHub Releases (`assets-v1` tag) for URL loading demo

### Priority 3 — Play Store review
- Wait for Google to approve with updated screenshots

### Priority 4 — iOS HDR environments
- Add HDR environment files to iOS bundle for environment switching

### Priority 5 — Progressive loading
- Enable Filament async loading (commented out in ModelLoader.kt)

## ASSET CATALOG STATUS
- **34 models** in catalog from 3 sources
- **6 HDR environments** from Poly Haven
- **61 GLB** in Android, **28 USDZ** in iOS
- **Sources**: Sketchfab (28), KhronosGroup (5), Fab.com (1)
- **Licenses**: CC-BY-4.0 (20+), CC-BY-NC-4.0 (8), CC-BY-NC-SA-4.0 (2), CC0-1.0 (1)
- **Scheduled**: Discovery runs Mon+Thu 10 AM

## FINANCIAL STATUS
- **Open Collective**: $2,338 USD (OSC fiscal host, 10% fee)
- **GitHub Sponsors**: configured for org `sceneview`, no active sponsors
- **Polar.sh**: in FUNDING.yml but page 404 — Thomas needs to activate
- **Monthly expenses**: Claude Max ~$168/mo (reimbursed via OC expense)
- **Process**: Pay with personal card → submit expense on OC → get reimbursed

## ACTIONS THOMAS
1. **Polar.sh**: activate account at polar.sh/sceneview
2. **GitHub Sponsors**: add $50 and $100 tiers
3. **App Store Connect**: create app "SceneView Demo" (`io.github.sceneview.demo`)
4. **Play Store**: check if Google review passes

## RULES
- Merge direct sur main
- Fast release
- Zero personal data in repo
- Only modify SceneView orgs
- Assets hosted locally
- Opus for important agents
- Zero data loss
