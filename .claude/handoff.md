# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session nuit — massive evolution wave)
**Branch:** main (tout mergé, 0 PR ouverte)
**Repo website:** sceneview.github.io (séparé, pushé directement)

## CE QUI A ÉTÉ FAIT CETTE SESSION

### Release v3.5.0 — Maven Central PUBLIÉ
- **Maven Central v3.5.0 PUBLIÉ avec succès** — la priorité #1 est enfin résolue
- Fix du Gradle task : `publishAndReleaseToMavenCentral` (était incorrect avant)
- GitHub Release v3.5.0 créée
- sceneview-mcp 3.5.0 déjà sur npm (de la run précédente)
- gradle.properties VERSION_NAME=3.5.0

### 10 agents d'évolution lancés et mergés
Tous en worktree isolé, tous pushés sur origin, tous mergés sur main sans conflit :

1. **evolve-build** — Version catalog, BOM, Gradle 8.13, detekt config
2. **evolve-kmp-core** — +4333 lignes : Capsule collision, Octree, CSG, 8 nouvelles géométries (Torus, Icosphere, Lathe, Heightmap, RoundedCube, Extrude, Capsule), Noise, Splines, Color utils
3. **evolve-llms-txt** — Réécriture complète llms.txt, source-verified API reference
4. **evolve-android-lib** — AnimationState, ShadowOptions, EnvironmentPresets, DebugOverlay, SafeFilament, SceneInspector, TextNode amélioré, 6 test suites
5. **evolve-android-demo** — 5 nouveaux demos, string resources, accessibilité
6. **evolve-sceneview-swift** — AnimationController, GestureSystem, CustomMaterial, NodeModifiers, NodeBuilder, ViewNode, SceneReconstruction, SceneSerialization, SceneSnapshot, SceneObservation + 8 test suites
7. **evolve-ios-demo** — Architecture multi-fichiers, 14 démos (AllShapes, AutoRotate, Billboard, CustomMesh, DynamicSky, Fog, ImagePlane, LightTypes, LinesPaths, Materials, OrbitCamera, Physics, SceneGallery, Text), 4 onglets (Explore, AR, Samples, About)
8. **evolve-website** — Design premium, docs.html, SceneView.js intégré, responsive
9. **evolve-mcp** — 10 nouveaux tools (animation, gesture, physics, environment, multiplatform, convert, explain, optimize, debug), v3.6.0, 1204 tests / 42 fichiers
10. **evolve-kmp-core** — Collision (Capsule, Octree, MeshCollider, CollisionResponse), Geometry (8 types), Math (Noise, Splines, Color)

### sceneview.js v2.0.0 (mergé session précédente)
- 3459 lignes — full scene graph, animation, geometry, collision, materials, media nodes
- Feature parity avec native platforms

### CI Status
- Website deploy : succès
- iOS CI : succès (TestFlight failure — besoin du cert Apple)
- Android CI : en cours
- Play Store deploy : en cours

## CE QUI RESTE À FAIRE

### Prochaine session
1. **Vérifier CI finales** — Android build + Play Store deploy
2. **Fix sceneview-web npm publish** — task Gradle `jsBrowserProductionLibraryDistribution` n'existe pas
3. **MCP v3.6.0 publish** — npm publish (10 nouveaux tools)
4. **Dependabot vulns** — 5 alertes GitHub (2 high, 3 moderate)
5. **App Store** — Thomas doit créer l'app sur App Store Connect + cert
6. **Play Store** — vérifier si review Google passée

## ACTIONS THOMAS
1. **App Store Connect** : créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Apple Developer** : créer certificat de distribution iOS
3. **Play Store** : checker si review Google est passée

## RÈGLES
- Merge direct sur main après review auto
- Fast release : auto-deploy stores sur push to main
- Alerter sur consommation tokens
- JAMAIS toucher [REDACTED]
- Assets toujours hébergés localement
- TOUJOURS opus pour les agents importants
- ZÉRO perte de données — chaque agent sur sa branche, pushée
