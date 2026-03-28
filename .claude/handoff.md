# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 28 mars 2026 (session 2 — après-midi)
**Branch:** main (tout mergé, 0 PR ouverte)
**Repo website:** sceneview.github.io (séparé, pushé directement)

## CE QUI A ÉTÉ FAIT CETTE SESSION

### Site web (sceneview.github.io) — 6 commits pushés

1. **Badge hero "Rendered by SceneView Web"** — fix visibilité light mode (noir en light, blanc en dark)
2. **Platforms Showcase refonte complète** :
   - Watch et Car Dashboard SUPPRIMÉS (fausses plateformes)
   - Claude Desktop AJOUTÉ avec mockup macOS (fenêtre ambre, code AR dans le viewport)
   - Support light/dark mode complet
   - Dynamic Island sur iPhone
   - Labels plus clairs avec icônes
3. **Boutons branded** :
   - "Build this with Claude" (ambre, gradient) dans le hero — lié au helmet avec prompt pré-rempli pour reproduire le DamagedHelmet
   - Badges store (Google Play vert, App Store bleu, Claude ambre, GitHub) dans section samples
4. **Comparatif tokens 3 colonnes** :
   - Raw Engine (~100k tokens) vs SceneView (~500 tokens) vs SceneView+MCP (~300 tokens, "RECOMMENDED")
   - Chiffres : lines of code, tokens to generate, AI iterations, total cost
   - `npx sceneview-mcp` dans la card MCP
5. **Showcase pill navbar** — fond bleu + bordure pour que "Showcase" soit impossible à rater
6. **Section Sample Apps** — featured Showcase card avec mini-cards (E-Commerce, AR, Automotive, etc.)
7. **Hint Pro subtil** — "This helmet is rendered live... Want more? Explore Pro tools →" sous le casque
8. **Subtitle code section** — "Few lines of code means few tokens for AI"

### Demandes en cours de review

- Thomas veut un comparatif plus orienté "avec/sans SceneView" et "avec/sans MCP" — fait avec les 3 colonnes
- Thomas veut que le Showcase soit plus mis en avant — fait avec pill + featured card
- Thomas veut que "Open in Claude" soit lié au helmet — fait, bouton renommé "Build this with Claude" avec prompt

## CE QUI RESTE À FAIRE

### Prochaine session
1. **Playground review** — vérifier que le playground fonctionne correctement (code + 3D preview)
2. **Vérifier 6 onglets langages** — Kotlin, Swift, JS, Dart, TS, Claude dans la landing
3. **Light mode review complète** — vérifier chaque section en light mode
4. **Maven Central v3.4.5 publish** — priorité code
5. **App Store** — Thomas doit créer l'app sur App Store Connect
6. **Play Store** — vérifier si review Google passée

## ACTIONS THOMAS
1. **App Store Connect** : créer app "SceneView Demo" bundle ID `io.github.sceneview.demo`
2. **Play Store** : checker si review Google est passée

## RÈGLES
- Merge direct sur main après review auto
- Fast release : auto-deploy stores sur push to main
- Alerter sur consommation tokens
- JAMAIS toucher Octopus Community
- Assets toujours hébergés localement
