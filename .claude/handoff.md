# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 28 mars 2026 (nuit)
**Branch:** main (tout mergé, 0 PR ouverte, 0 branche stale)
**30+ PRs mergées cette session**

## DEMANDES NON RÉALISÉES — PRIORITÉ ABSOLUE PROCHAINE SESSION

Thomas a demandé ces changements PLUSIEURS FOIS et ils ne sont PAS correctement en ligne :

### 1. Platforms Showcase (platforms-showcase.html)
- Les wireframes/cards ne sont pas clairs — on ne comprend pas à quoi ils correspondent
- Ajouter Claude Desktop/Mac comme plateforme avec un mockup réaliste
- Retirer les fausses cards (Watch, Car)
- Le thème dark s'applique bizarrement

### 2. Boutons branded "Open in Claude/Play/Apple"
- Couleurs/icônes officielles de chaque plateforme partout sur le site
- Claude : couleur orange/ambre + logo Claude
- Google Play / App Store : badges officiels

### 3. Playground (playground.html) — refonte complète
- Le playground actuel ne fonctionne pas bien
- Besoin d'une review et fix complet

### 4. Section "Sample apps" sur la landing — pas impactant visuellement

### 5. Vérifier que les 6 onglets langages sont LIVE sur la landing (Kotlin, Swift, JS, Dart, TS, Claude)

## CE QUI EST FAIT

### Stores
- **Play Store** : AAB uploadé, en review, auto-deploy configuré
- **iOS** : compile + archive OK, **BLOQUÉ** : créer l'app sur App Store Connect (bundle ID: io.github.sceneview.demo)
- **Auto-versioning** : versionCode = github.run_number, auto-deploy sur push to main

### Site web (sceneview.github.io)
- Hero fade-in smooth, Install 7 onglets après hero, thème suit le système
- AI section avec mockup Claude Code Mac, pro cards cliquables, logo nettoyé
- Store badges → "Coming soon" + "Notify me"

### CI/CD
- Workflows nettoyés, paths-ignore, JS tests non-bloquants
- Toutes branches supprimées (0 stale)

### Code
- Android demo 14/14 démos LIVE
- iOS demo réécrit (4 tabs, 5 démos)
- README refait (4 langages, Claude comme plateforme)

## ACTIONS THOMAS
1. **App Store Connect** : créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Play Store** : checker si review Google est passée

## RÈGLES
- Merge direct sur main après review auto
- Fast release : auto-deploy stores sur push to main
- Alerter sur consommation tokens
- Certificats sauvegardés sur Drive (Projects/Secrets/ios-distribution-2027/)
