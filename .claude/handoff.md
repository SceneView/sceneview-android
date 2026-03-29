# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 29 mars 2026 (session 3)
**Branch:** main (tout pushé sur origin)

## CE QUI A ETE FAIT CETTE SESSION

### 1. Screenshots stores — 14 screenshots capturés
- **iPhone** (1284×2778): 3D viewer, Samples, About
- **iPad Pro** (2048×2732): 3D viewer, Samples, About
- **iPad Air** (1640×2360): 3D viewer, Samples, About
- **Pixel 9** (1280×2856): Toy Car Night, Chair Studio, Lamp Sunset, Samples, About
- Screenshots sauvés: `/tmp/store-screenshots/final/` (14 fichiers)

### 2. Play Store screenshots uploadés + review relancée
- 4 nouveaux screenshots (toy car, chair, lamp, samples) remplacent les anciens blancs
- Review relancée avec "Restart review" — en attente Google

### 3. App Store screenshots — bloqué
- Version iOS "En attente de vérification" → screenshots verrouillés
- À uploader après la review

### 4. Fix CI docs — Python 3.14 breaking change
- `python-version: '3.x'` résolvait vers Python 3.14.3 qui cassait pygments
- Fix: pin `python-version: '3.13'` dans `.github/workflows/docs.yml`
- ✅ CI docs vert

### 5. iOS ExploreTab amélioré
- Fond gradient sombre (au lieu du blanc RealityKit par défaut)
- Shapes 2x plus grandes pour meilleure visibilité
- Grid floor supprimé (causait des artefacts visuels)
- ✅ iOS CI vert, TestFlight build OK

## CI A VERIFIER AU DEMARRAGE

```bash
gh run list --branch main --limit 5
```

## CE QUI RESTE A FAIRE

### Priorité 1 — App Store Connect (action Thomas)
- Créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
- Une fois créée, relancer le TestFlight workflow → upload automatique

### Priorité 2 — App Store screenshots
- Uploader les 9 screenshots iOS (iPhone + iPad Pro + iPad Air) quand la review sera terminée
- Screenshots disponibles dans `/tmp/store-screenshots/final/`

### Priorité 3 — Play Store review
- Attendre que Google approuve la nouvelle review avec screenshots mis à jour

### Priorité 4 — iOS demo enrichissement
- Ajouter des HDR environments au bundle iOS (studio.hdr etc.)
- Ajouter des modèles USDZ pour le viewer 3D (comme Android a des GLB)
- L'ExploreTab pourrait avoir un model picker comme Android

### Priorité 5 — Améliorer iOS CI
- Le TestFlight tourne sur macos-14 (Xcode 16.x) car macos-15 ne pré-installe pas le iOS SDK
- Quand GitHub fixera les runners macos-15, re-switcher

## ACTIONS THOMAS
1. **App Store Connect** : créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Play Store** : checker si review Google passe

## REGLES
- Merge direct sur main
- Fast release
- Zero données personnelles dans le repo
- Ne modifier que les orgs SceneView
- Assets hébergés localement
- Opus pour les agents importants
- Zero perte de données
