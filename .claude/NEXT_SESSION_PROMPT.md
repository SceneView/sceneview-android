# Next Session — Vendor remaining 8 stub libraries into hub-gateway

## Context

Lis `.claude/handoff.md` et `.claude/NOTICE-2026-04-11-mcp-gateway-live.md`.

**Hub-mcp Gateway #2** est **LIVE** sur `hub-mcp.mcp-tools-lab.workers.dev` :
- 11 libs / 52 tools / 58 tests
- Stripe LIVE : 4 plans (Portfolio 29€, Team 79€ + yearly)
- Auth + rate-limit + quota + tier gating + usage logging + /checkout/success
- 3 libs REAL (architecture-mcp npm, automotive-3d monorepo, healthcare-3d monorepo)
- **8 libs STUBS** à vendorer

## Objectif

Remplacer les 8 stubs par des vrais handlers depuis les packages npm.
Chaque package npm exporte des **pure functions** dans `dist/tools/*.js` mais
n'exporte PAS `TOOL_DEFINITIONS` + `dispatchTool`. Il faut wrapper.

## Le problème rencontré (session précédente)

J'ai tenté de wrapper realestate, french-admin, ecommerce-3d, health-fitness.
**Vitest/Vite ne résout PAS les imports `from "realestate-mcp/dist/tools/index.js"`**
car les packages n'ont pas de `exports` field dans package.json qui expose
ces sub-paths. Wrangler's esbuild les résout (le deploy fonctionne) mais
les tests cassent.

### Solutions à explorer

1. **vitest.config.ts `resolve.alias`** : mapper `realestate-mcp/dist/tools/index.js`
   vers le chemin absolu dans node_modules
2. **Thin wrapper dans hub-gateway** qui re-exporte via un fichier intermédiaire
   que vitest peut résoudre
3. **Dynamic `import()` dans un try/catch** au lieu d'import statique — vitest
   peut mocker

### WIP stashé

```bash
cd ~/Projects/sceneview/.claude/worktrees/multi-gateway-sprint
git stash pop  # "WIP: 4 npm library wrappers..."
```

Ce stash contient :
- `src/libraries/npm-adapter.ts` — generic wrapper (KEEP)
- `src/libraries/realestate.ts` — wrapper avec @ts-expect-error imports
- `src/libraries/french-admin.ts` — idem
- `src/libraries/ecommerce-3d.ts` — wrapper avec noms de fonctions corrigés
- `src/libraries/health-fitness.ts` — wrapper sans les 2 live API tools
- `src/mcp/access.ts` — FREE_TOOLS mis à jour pour les noms réels
- `package.json` — 6 npm deps ajoutées (architecture-mcp déjà installé)

## Packages npm et leurs exports

| Package | Tools | Fonctions dans `dist/tools/` | Dep installée |
|---|---|---|---|
| realestate-mcp@2.1.5 | 8 (+ 1 non-exportée) | generateFloorPlan, createVirtualTour, propertyDescription, stagingSuggestions, neighborhoodAnalysis, renderPropertyPreview, estimatePrice, compareProperties | ✅ |
| french-admin-mcp@2.1.6 | 13 (+ 3 non-exportées) | simulerImpots, calculerChargesAe, genererFacture, declarationUrssaf, aideCaf, redigerCourrier, simulerRetraite, simulerChomage, calculerIndemnitesLicenciement, verifierDroitsFormation, simulerAidesLogement, calculerIndemnitesConges, verifierDroitsChomageDemission | ✅ |
| ecommerce-3d-mcp@2.0.5 | 11 | Exports from `dist/generators.js`: generateModelViewerEmbed, createARTryOnEmbed, generateConfigurator, generateOptimizationReport, generateTurntableEmbed, generateShopifySnippet, generateWooCommerceSnippet, generateSEO3DMetadata, generateProductPage, analyzeConversion, generateSizeGuide | ✅ |
| health-fitness-mcp@1.1.0 | 10 (+ 2 live API) | calculateBmi, calculateTdee, planMacros, generateWorkout, analyzeBodyComposition, calculateHydration, calculateHeartRateZones, calculateSleep, planStretching, estimateCaloriesBurned | ✅ |
| legal-docs-mcp@2.0.5 | ? | dist/tools.js (pas dist/tools/index.js) | ✅ |
| education-mcp@1.0.4 | ~6 | dist/tools/ (explainConcept, generateExercise, generateFlashcards, generateLessonPlan, generateQuiz, gradeRubric) | ✅ |
| finance-mcp | ❌ pas de main field npm | SKIP — garder stub | ❌ |
| social-media-mcp@2.0.0 | ❌ deprecated | SKIP — garder stub | ❌ |

## Pattern prouvé (architecture-mcp)

`src/libraries/architecture.ts` est le template de référence :
- Import des pure functions depuis `architecture-mcp/dist/tools/index.js`
- TOOL_DEFINITIONS[] avec JSON Schema hand-translated du Zod
- HANDLERS map tool_name → function
- dispatchTool() wrapper avec try/catch + disclaimer

## Règles

- **NE PAS toucher** `mcp/`, `mcp-gateway/`, Stripe secrets, price ids
- Les stubs `finance-mcp` et `social-media-mcp` RESTENT en stub
- Chaque library wrapper = 1 fichier ~100-200 lignes
- FREE_TOOLS whitelist: 1 free discovery tool par library réelle
- Tests: chaque wrapper doit au moins vérifier que dispatch d'un tool réel retourne du vrai content (pas "pilot stub")
- Deploy + push main après chaque batch

## Budget

~30-50k tokens. Faire les 6 wrappers (realestate, french-admin, ecommerce-3d, health-fitness, legal-docs, education), résoudre le vitest import issue, tests, deploy, push.
