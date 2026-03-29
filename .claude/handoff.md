# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 2)
**Branch:** main (tout pushé sur origin)

## CE QUI A ETE FAIT CETTE SESSION

### 1. CI — Tout vert
- Android CI: ✅
- iOS CI: ✅
- TestFlight: ✅ archive build OK, upload bloqué par App Store Connect (app pas encore créée)

### 2. Fix TestFlight pipeline (5 itérations)
- Asset catalog: `ASSETCATALOG_COMPILER_GENERATE_ASSET_SYMBOLS=NO`
- iOS SDK: switch `macos-15` → `macos-14` (iOS SDK pré-installé)
- Duplicate structs: nettoyé `SceneViewDemoApp.swift` (557 lignes supprimées — monolithe réduit à @main + ContentView)
- **Résultat final**: build archive passe, export échoue car "Error Downloading App Information" = app pas créée sur App Store Connect

### 3. Dependabot — 3 alertes résolues (0 restantes)
- `path-to-regexp` 0.1.12 → 8.4.0 (ReDoS)
- `brace-expansion` 1.1.12 → 5.0.5 (memory exhaustion)
- Résolutions yarn dans `build.gradle`, lock file régénéré

### 4. Publications npm
- `sceneview-web@3.5.0` publié sur npm (34 kB)
- `sceneview-mcp` synced à 3.5.2 (match npm)

### 5. Dependency bumps
- TypeScript 5.8 → 6.0.2 (breaking change tsconfig fixé, 1204 tests OK)
- Compose 1.7.3 → 1.10.3 (Dependabot PR mergée)

### 6. Fixes code
- KMP core iOS: `CLOCK_MONOTONIC` UInt type mismatch dans TimeSource.kt
- iOS demo: duplicate struct definitions supprimées

### 7. Santé repo
- 0 alertes Dependabot
- 0 PR ouvertes
- Versions sync: 32 checks, 0 erreurs (1 faux positif migration.md, MCP intentionnellement 3.5.2)
- MCP: 1204 tests passent
- KMP core: JS tests passent, iOS compile

## CI A VERIFIER AU DEMARRAGE

```bash
gh run list --branch main --limit 5
```

## CE QUI RESTE A FAIRE

### Priorité 1 — App Store Connect (action Thomas)
- Créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
- Une fois créée, relancer le TestFlight workflow → upload automatique

### Priorité 2 — Play Store
- Vérifier si review Google est passée

### Priorité 3 — Améliorer iOS CI
- Le TestFlight tourne sur macos-14 (Xcode 16.x) car macos-15 ne pré-installe pas le iOS SDK
- Quand GitHub fixera les runners macos-15, re-switcher

## ACTIONS THOMAS
1. **App Store Connect** : créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Play Store** : checker si review Google est passée

## REGLES
- Merge direct sur main
- Fast release
- Zero données personnelles dans le repo
- Ne modifier que les orgs SceneView
- Assets hébergés localement
- Opus pour les agents importants
- Zero perte de données
