# NOTICE — 2026-04-11 — 8 hero assets added

**From:** session 30 (worktree `cranky-burnell`, agent: Claude Opus 4.6 1M)
**Commits on `main`:** `f198fe45` (assets) + `460c8853` (handoff)
**Severity:** ⚠️ Read this if your worktree touches the catalog, playground, ExploreScreen, website models, or the assets-v1 CDN.

## What changed

8 new realistic CC-BY 4.0 hero models added (Sketchfab, optimized via gltf-transform):

| ID | Source author | Size | Tags |
|---|---|---|---|
| `rolex_watch` | sudo-self | 2.5 MB (meshopt) | luxury, accessory |
| `sneaker_vibe` | vmmaniac | 5.1 MB (meshopt) | fashion |
| `moto_helmet` | sayedgamal655 | 3.4 MB | sport, protection |
| `dji_mavic_3` | aurumjuda747 | 9.4 MB | tech, drone |
| `jbl_tour_one_m3` | cubemodelex | 5.3 MB | audio |
| `canon_eos_rp` | fahadratul | 11 MB | photography |
| `photorealistic_guitar` | Keandir | 808 KB | instrument |
| `school_backpack` | MadeByYeshe | 3.3 MB | outdoor |

## Files modified (rebase / pull main before continuing)

- `assets/catalog.json` — 77 → 85 models (8 new entries appended)
- `samples/android-demo/src/main/java/io/github/sceneview/demo/explore/ExploreScreen.kt` — 8 new `ExploreModel(...)` rows in the Objects category, all loaded via `$CDN`
- `website-static/playground.html` — new `<optgroup label="Hero (new)">` with 8 `<option>` entries; existing optgroups untouched
- `website-static/models/platforms/*.glb` — 8 new files committed (snake_case filenames, e.g. `moto_helmet.glb`)
- `.claude/handoff.md` — session 30 block prepended

## Files NOT modified (safe)

- `samples/ios-demo/**` — iOS demo doesn't load any GLB/USDZ; no USDZ generated for the 8 assets (deferred — no local converter)
- `samples/android-tv-demo/**` — gitignored assets dir, sync ran but nothing tracked
- `samples/web-demo/src/jsMain/resources/index.html` — model list NOT updated (separate concern, can add if needed)
- `samples/flutter-demo/**`, `samples/react-native-demo/**` — over-synced platform dirs were cleaned up before commit
- `website-static/index.html` — hero canvas still on `DamagedHelmet.glb` (intentionally untouched)
- `website-static/platforms-showcase.html` — 5 sv-viewer slots still on the original models (intentionally untouched)

## Externalized state

- **GitHub Release `assets-v1`** — 47 → **55 assets**. The 8 new GLBs are uploaded and live (HTTP 200 verified). URL pattern: `https://github.com/sceneview/sceneview/releases/download/assets-v1/<id>.glb`
- **`sceneview.github.io`** — auto-deployed via `.github/workflows/deploy-website.yml` on the asset commit. CDN cache may take a few minutes to propagate; raw repo on `main` already has the files.

## Action required for other worktrees

1. **Rebase your worktree onto `origin/main`** before committing (avoids conflict in `assets/catalog.json` and `playground.html` optgroup block).
2. **If your worktree edits the playground options**: the new `Hero (new)` optgroup is the FIRST optgroup in the select. Other optgroups follow unchanged.
3. **If your worktree edits `assets/catalog.json`**: 8 new entries are appended at the end of `models[]` (between `cozy_living_room` and the top-level `environments` key). `lastUpdated` is now `2026-04-11`.
4. **If your worktree depends on a CDN URL**: 8 new IDs are available — see table above.
5. **No breaking renames or deletions.** Existing model IDs are unchanged.

## Pivot notice

The original user request was for CGTrader assets. CGTrader's licensing model (mostly paid + restrictive EULA on free models) is incompatible with an open-source SDK redistributing assets. Pivoted to Sketchfab CC-BY 4.0 (which is the project's existing pattern — see `feedback_self_hosted_assets.md` and the existing 66 CC-BY entries in the catalog). If a future request requires CGTrader specifically, the task would need a per-model license review.
