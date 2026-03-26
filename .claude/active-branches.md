# Active Branches — SceneView Android

> Last updated: 2026-03-23 10:30 UTC

## Branch Status

```
main ──────────────────────────────────────── ✅ CLEAN (29 merges pushed)
  │
  ├── 🔧 claude/deeplinks-environment-samples  [+2 commits]  2026-03-23
  │   └── Environment-aware deeplinks for sample quick-launch
  │       Status: READY TO REVIEW — small feature, clean
  │
  ├── 🔧 claude/fix-website-readme             [+1 commit]   2026-03-23
  │   └── Fix docs deployment + README/CLAUDE.md corrections
  │       Status: READY TO REVIEW — docs fix only
  │
  ├── 🧪 claude/sceneview-marketing-showcase   [+1 commit]   2026-03-23
  │   └── M3 website redesign with SVGs, showcase, try-demo
  │       Status: DO NOT MERGE — large experimental redesign, ideas extracted
  │
  ├── 🧪 claude/v4-roadmap-implementation      [+23 commits] 2026-03-23
  │   └── v4.1.0 experimental: RaycastNode, collisions, gestures, mesh API
  │       Status: DO NOT MERGE — ideas extracted to roadmap 4.1 memory
  │
  └── 🔄 qa/2026-03-20-rendering-check         [+5 commits]  2026-03-20
      └── CI AR emulator testing — motion injection for virtualscene
          Status: WORK IN PROGRESS — CI experiments
```

## Legend

- ✅ Clean / merged
- 🔧 Small branch, ready to review
- 🧪 Experimental — DO NOT MERGE (ideas captured in memory)
- 🔄 Work in progress

## Cleanup Summary (2026-03-23)

### Merged into main (11 branches)
- `feat/post-processing` — Bloom, DoF, SSAO, Fog sample
- `feat/physics-node` — PhysicsNode + physics-demo
- `feat/dynamic-sky-fog-node` — DynamicSkyNode + FogNode + sample
- `feat/reflection-probe-node` — ReflectionProbeNode
- `feat/line-path-node` — LineNode, PathNode, TextNode + samples
- `feat/ar-model-viewer-gestures` — Plane mesh + gesture docs
- `feat/model-viewer-model-picker` — Model picker
- `feat/model-viewer-animation` — Animation playback controls
- `feat/mcp-get-node-reference` — MCP tool
- `feat/marketing-seo-improvements` — FUNDING.yml, MCP README, roadmap docs
- `claude/check-progress-Digzb` — HDR safe fallback fix

### Deleted (16 branches)
- 5 merged branches: `release/3.1.2`, `deps/filament-1.70.0`, `deps/kotlin-2.3.20`, `feat/text-billboard-node`, `claude/identify-project-focus-FU1rl`
- 11 historical (2022-2024): `arsceneview_1_0_0`, `performance-configuration`, `restructure_geometries`, `rip_renderable`, `view-node-interaction`, `sameer-debug-screen-record`, `transform-component`, `dokka-aggregated-publishing`, `revert-kotlin-version-bump`, `update-misc-dependencies`, `feat/ar_view_node_sample`
- 1 obsolete: `fix/ci-gradle-compatibility`

### Ideas captured for roadmap 4.1
See memory: `project_roadmap_4_1.md`
- Camera modes (Fly, Follow, Cinematic)
- Collision & Picking (raycast, hit results)
- Additional geometry (Torus, Cone, Capsule)
- Instanced rendering, LOD, Bezier paths
- AR PerformanceProfile
- BillboardNode for Android (iOS parity)
