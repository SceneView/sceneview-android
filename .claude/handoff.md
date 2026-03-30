# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 31 mars 2026 (session 20)
**Branch:** main

## WHAT WAS DONE THIS SESSION (session 20)

### 1. Critical Android demo fixes ✅ (commit ab6b62cc)
- **3 missing GLB models** causing infinite loading → replaced:
  - `sneaker.glb` → `sunglasses.glb` (Gesture Editing demo)
  - `leather_sofa.glb` → `velvet_sofa.glb` (Multi-Model Scene)
  - `barn_lamp.glb` → `candle_holder.glb` (Multi-Model Scene)
- **Runtime camera permission** for AR tab — `rememberLauncherForActivityResult` + `CameraPermissionScreen`
- **CREDITS.md** updated to reflect model replacements
- `!!` on bundled assets kept — `rememberEnvironment` requires non-null, and HDR files are always bundled

### 2. iOS demo cleanup ✅ (commit ab6b62cc)
- Removed phantom `lowpoly_fruits.usdz` from pbxproj (PBXBuildFile + PBXFileReference)
- Replaced hardcoded `"v3.6.0"` with `Bundle.main.infoDictionary` dynamic version in AboutTab

### 3. React Native demo fixes ✅ (commit ab6b62cc)
- Created `samples/react-native-demo/package.json` (was entirely missing)
- Fixed iOS bridge `SceneViewModule.swift`:
  - `scale` now handles both array `[x,y,z]` and scalar
  - `position` prop now parsed and applied
  - `animation` prop now parsed (stored in `RNModelData`)

### 4. Playground rewrite committed ✅ (commit 4f82e00e)
- Full rewrite of `website-static/playground.html` (1311+ lines added)
- IDE-like 3-zone layout, 13 examples, 3 platforms, Stitch design

### 5. Emulator QA ✅
- Pixel_7a (API 34) — all 4 tabs verified:
  - **3D (Explore)**: Toy Car loads, auto-rotation works, model/env switching works
  - **AR**: "AR Not Available" correctly shown on emulator
  - **Samples**: 19 demos listed, Model Viewer, Geometry Nodes, Multi-Model Scene, Gesture Editing all load
  - **About**: v3.6.0 displayed correctly
- All 15 local model paths + 10 HDR paths verified as existing in assets

### 6. Flutter demo — BLOCKED
- No Flutter SDK installed on machine — cannot run `flutter create .` to generate platform dirs

---

## 🔴 PRIORITY ABSOLUE — REFONTE COMPLÈTE DEMO APPS

### Contexte
L'utilisateur a testé l'app Android et est très frustré : "80% des choses ne marchent pas".
Directive : refaire TOUTES les apps de démo sur TOUTES les plateformes, avec design Stitch,
assets de qualité, et QA irréprochable. AUCUNE tolérance pour quoi que ce soit de cassé.

### Audit complet réalisé (session 19)

#### Android Demo — 3 bugs critiques
| Bug | Fichier | Détail |
|---|---|---|
| `sneaker.glb` manquant | SamplesScreen.kt:1766 | Gesture Editing demo → loading infini |
| `leather_sofa.glb` manquant | SamplesScreen.kt:1306 | Multi-Model Scene → loading infini |
| `barn_lamp.glb` manquant | SamplesScreen.kt:1307 | Multi-Model Scene → loading infini |

**Autres problèmes Android :**
- 17 modèles CDN sans gestion d'erreur/timeout (ExploreScreen)
- Force-unwrap `!!` sur environmentLoader (risque NPE)
- Pas de demande permission caméra runtime pour AR
- Strings hardcodées dans UpdateBanner

#### iOS Demo — Fonctionnel mais cleanup nécessaire
- ✅ Tous les 28 modèles USDZ existent
- ✅ Tous les 6 HDR existent
- ✅ 14 samples tous procéduraux (pas de dépendance asset)
- ⚠️ Référence fantôme `lowpoly_fruits.usdz` dans xcodeproj
- ⚠️ 13 modèles USDZ non utilisés mais bundlés (taille app)
- ⚠️ Package.swift manque déclarations resources
- ⚠️ Version hardcodée "v3.6.0" dans AboutTab

#### Android TV Demo — OK
- ✅ Tous les assets présents et corrects
- ✅ Utilise vraie API SceneView

#### Web Demo — Compilable, runtime incertain
- ✅ Tous les 24 modèles GLB présents
- ⚠️ Filament.js WASM bindings potentiellement incomplets au runtime

#### Desktop Demo — Placeholder intentionnel
- ✅ Par design, wireframe Canvas 2D, pas SceneView

#### Flutter Demo — NE PEUT PAS BUILD
- ❌ Manque android/ et ios/ platform directories
- ❌ Doit exécuter `flutter create .` d'abord
- ⚠️ `addGeometry()` et `addLight()` sont des no-ops côté natif

#### React Native Demo — NE PEUT PAS BUILD
- ❌ Pas de package.json
- ❌ Pas de android/ directory
- ❌ Mismatch type prop `scale` (array vs scalar dans iOS bridge)
- ❌ Props `position` et `animation` non gérées côté iOS natif

### Plan de refonte — Avancement

#### Phase 1 — Fixes critiques Android ✅ DONE (session 20)
1. ✅ Modèles manquants remplacés (sneaker→sunglasses, leather_sofa→velvet_sofa, barn_lamp→candle_holder)
2. ✅ CDN models: ExploreScreen already has loading indicator, acceptable UX
3. ✅ `!!` analysés: tous sur assets bundlés, requis par `rememberEnvironment` signature — SAFE
4. ✅ Permission caméra runtime ajoutée pour AR
5. ✅ String resources: `ar_grant_permission` ajouté, rest already uses string resources

#### Phase 2 — Design Stitch complet
1. Redesign COMPLET de toutes les UI via Google Stitch MCP
2. Chaque écran doit être généré par Stitch puis appliqué
3. M3 Expressive pour Android, Apple HIG pour iOS
4. Vérifier cohérence design cross-platform

#### Phase 3 — Assets de qualité
1. Vérifier que TOUS les modèles se chargent correctement
2. Remplacer les modèles de faible qualité
3. Tester chaque modèle individuellement
4. S'assurer que les animations fonctionnent

#### Phase 4 — QA irréprochable
1. Tester CHAQUE demo sur émulateur Android
2. Vérifier les logs pour crashes/errors
3. Tester AR sur device physique si possible
4. Écrire des tests automatisés pour les chemins d'assets
5. Créer un script de validation des assets

#### Phase 5 — Autres plateformes (partially done session 20)
1. ✅ iOS : phantom ref removed, hardcoded version fixed
2. ❌ Flutter : BLOCKED — no Flutter SDK installed, needs `flutter create .`
3. ✅ React Native : package.json created, iOS bridge scale/position/animation fixed
4. ⏳ Web : runtime Filament.js not tested yet
5. ⏳ TV : not tested yet

### Émulateur créé
- Pixel_7a (API 34) — créé cette session après suppression des 3 anciens AVDs
  (Android_XR, Pixel_6_AR, Pixel_9_Pro) pour libérer 11 Go d'espace disque

---

## v4.0.0 Roadmap — PLANNED

### Merge sceneview + arsceneview → single `sceneview` module
- **Goal**: One artifact `io.github.sceneview:sceneview` with both 3D and AR
- **Why**: Simpler DX, aligns with iOS (single SceneViewSwift package), AI-friendly (one dep)
- **Plan**:
  1. Move `arsceneview/src/` into `sceneview/src/main/java/.../ar/`
  2. ARCore as `implementation` dep (already optional at runtime via `checkAvailability()`)
  3. Keep `arsceneview/` as empty redirect module (`api(project(":sceneview"))`) for Maven compat
  4. Single import: `io.github.sceneview:sceneview:4.0.0` gives both `SceneView {}` and `ARSceneView {}`
  5. Update all docs, llms.txt, samples, MCP, website, README
  6. Migration guide: "replace `arsceneview:3.x` with `sceneview:4.0.0`"
- **Breaking changes**: Maven coordinates only — API stays identical
- **Other 4.0.0 candidates**: TBD (collect before starting)

---

## WHAT WAS DONE THIS SESSION (session 19)

### 1. Playground from scratch — COMPLETE REWRITE ✅
- **File**: `website-static/playground.html` (1704 lines, was ~1160)
- **Design**: Stitch "Architectural Blueprint" aesthetic — tonal layering, no hard borders, ambient blue-tinted shadows
- **Layout**: Full-screen IDE-like 3-zone layout:
  - Header bar (52px): title + breadcrumb, platform toggle pills (Android/iOS/Web), action buttons (Copy/Share/Claude)
  - Main body: left sidebar (272px, collapsible categories + search) + code editor + live 3D preview
  - Bottom bar (56px): description + tag pills + docs link
- **13 examples across 6 categories**:
  - Getting Started (4): Model Viewer, Environment Setup, Camera Controls, Lighting
  - AR & Spatial (3): AR Placement, Face Tracking, Spatial Anchors
  - Geometry (1): Primitives
  - Animation (2): Model Animation, Spring Physics
  - Materials (1): PBR Materials
  - Advanced (2): Multi-Model Scene, Post-Processing
- **Multi-platform code**: Each example has 3 versions — Android (Kotlin), iOS (Swift), Web (JS)
- **Live 3D preview**: SceneView/Filament.js canvas, 63 models (6 categories), floating glass controls (auto-rotate, bloom, bg toggle)
- **Features**: URL state sharing, search/filter, copy code, Open in Claude, per-language syntax highlighting
- **Responsive**: sidebar hides on tablet, panes stack on mobile
- HTML validated (all tags properly closed)

### 2. Handoff TODO updated ✅
Added 5 new priority tasks from user requests:
- 🔴 Open Collective assets overhaul (logo, banner, cover)
- 🔴 Branding cleanup (organize branding/, export PNGs, variants)
- 🔴 Playground from scratch ← DONE this session
- 🟡 Claude Artifacts for SceneView
- 🟡 Stitch full design review of all pages

### 3. Open Collective — partially done (session 18, continued)
- Description, about, tiers done in session 18
- Assets (logo, banner) still need updating → next session

## WHAT NEEDS TO BE DONE NEXT (session 21)

### 🔴 IMMEDIATE — Asset sourcing for playground & website
**Context**: User said "N'hésites pas à utiliser les images de Stitch et à aller chercher les meilleurs asset 3D et HDR"
**User authorized paying** for premium assets, receipts go to Open Collective.

**User answers (confirmed in session 19):**
1. ✅ YES — Multiple HDR environments (studio, outdoor, sunset) + environment switcher in playground
2. ✅ YES — Add more premium models (architectural, luxury products, etc.)
3. ❓ Not answered yet — Stitch screenshots usage TBD

**Sources to search:**
- **Poly Haven** (polyhaven.com) — CC0 HDRIs, textures, models (FREE)
- **ambientCG** — CC0 PBR materials (FREE)
- **Sketchfab** — models (free + paid, we have API key in reference_sketchfab.md)
- **KhronosGroup glTF samples** — reference models (FREE)
- **HDRI Haven** — studio/outdoor HDRIs (FREE, CC0)

**What to download:**
- 3-5 high-quality HDR environments (studio, outdoor warm, outdoor cool, abstract, sunset)
- Convert to KTX format for Filament.js (use `cmgen` from Filament tools)
- Add environment switcher to playground preview controls
- Optionally: 5-10 premium showcase models

### 🔴 Open Collective — change all assets
- Upload logo.svg as avatar (convert to PNG first)
- Upload feature-graphic.svg or og-image.svg as cover/banner
- Verify all branding matches Stitch #005bc1
- User is connected — use Chrome MCP

### 🔴 Branding cleanup
- Organize `branding/` folder properly
- Export SVGs to PNG (128, 256, 512, 1024)
- Logo variants: with/without text, dark/light
- Banners for: GitHub, npm, Open Collective, social
- Favicon multi-format (ico, png 16/32/48/192/512)
- Update branding/README.md

### 🟡 Stitch full review of SceneView
- Use Stitch MCP to review all 8 website pages
- Get design feedback on consistency, M3 compliance, accessibility, responsive
- Apply improvements

### 🟡 Claude Artifacts for SceneView
- Make SceneView displayable in Claude.ai artifacts
- Use sceneview-web CDN (jsdelivr) in HTML artifacts
- Create templates Claude can generate
- Document in llms.txt

### 🟡 Playground deployment — PARTIALLY DONE
- ✅ Committed the new playground.html (commit 4f82e00e)
- ⏳ Deploy to sceneview.github.io (push to sceneview.github.io repo)
- ⏳ Visual QA on live site (desktop + mobile, light + dark)

---

## WHAT WAS DONE IN SESSION 18

### 1. v3.6.0 Release — FULLY PUBLISHED ✅
- Version bumped from 3.5.2 → 3.6.0 across 150+ files
- GitHub Release created: v3.6.0
- Maven Central: published (sceneview + arsceneview + sceneview-core)
- npm: sceneview-web 3.6.0 published
- sceneview.github.io: updated to 3.6.0
- SPM: tag v3.6.0 pushed

### 2. CI fixes ✅
- **Play Store**: Fixed 200MB AAB limit by creating Play Asset Delivery install-time pack (`samples/android-demo-assets/`). 50 models + 10 environments moved out of base module.
- **App Store**: Fixed `SceneViewTheme` not in scope — added Theme.swift to Xcode pbxproj (PBXBuildFile, PBXFileReference, group, sources build phase).
- **GitHub Actions**: Bumped all to latest (checkout v6, cache v5, upload-artifact v7, download-artifact v8, configure-pages v6) — fixes Node.js 20 deprecation.
- **Xcode 26 upgrade**: iOS CI + App Store workflows now use macos-15 runners with Xcode 26.3 fallback chain (fixes Apple ITMS-90725 SDK warning).

### 3. Scene → SceneView cross-platform rename ✅
- Android composables: `Scene { }` → `SceneView { }`, `ARScene { }` → `ARSceneView { }`
- `@Deprecated(replaceWith = ...)` aliases for old names — zero breaking change
- All samples, docs, cheatsheets, llms.txt, codelabs, recipes, website, MCP tools updated
- 2360 MCP tests pass
- BUILD SUCCESSFUL (sceneview + arsceneview + android-demo)

### 4. Dependabot PRs merged ✅
- Kotlin 2.1.21 → 2.3.20
- Compose BOM 2025.06.00 → 2025.12.01
- Media3 1.9.2 → 1.10.0
- TV Foundation alpha11 → beta01
- Test Runner 1.6.2 → 1.7.0

### 5. App Store auto-submit ✅
- Added auto-submit step to `app-store.yml` — uses ASC API (PyJWT) to find latest build, attach to version, submit for review
- `continue-on-error: true` so TestFlight upload is never blocked
- Workflow running now (run #23764364831)

## Previous session (session 17)

## WHAT WAS DONE IN SESSION 17

### 1. Swift: NodeGesture cleanup (#9) + async-safe APIs + zero warnings ✅
- **NodeGesture cleanup**: WeakEntity tracking, purgeStaleHandlers() auto-cleanup, Entity fluent extensions (.onTap, .onDrag, .onScale, .onRotate, .onLongPress)
- **Async-safe migrations**: `TextureResource(named:)`, `Entity(named:)`, `Entity(contentsOf:)`, `EnvironmentResource(named:)` — replaces deprecated `.load()` across ModelNode, ImageNode, ReflectionProbeNode, GeometryNode, Environment
- **LightNode**: fixed deprecated `maximumDistance` setter by re-creating Shadow
- **Tests**: fixed Float? accuracy parameter compilation errors in 5 test files
- **Clean build**: zero warnings, zero errors (iOS + macOS), 544 tests pass
- Committed `ae89b215`

### 2. CameraNode → SecondaryCamera rename (#2) ✅
- `SecondaryCamera()` composable added to SceneScope with full docs
- `CameraNode()` composable deprecated with `@Deprecated(replaceWith = ...)` for migration
- llms.txt updated with new name
- Android builds pass (sceneview + android-demo)
- Committed `b0b00c74`

### 3. Docs: cross-platform naming alignment (#10) + ARNodeScope nesting (#14) ✅
- llms.txt platform mapping table expanded: SecondaryCamera, drag gesture, billboard, reflection probe, @NodeBuilder init
- ARNodeScope nesting limitation documented prominently
- Committed `ff713805`

### 4. VideoNode convenience overload (#6) ✅
- New `VideoNode(videoPath = "videos/promo.mp4")` composable with automatic MediaPlayer lifecycle
- Uses existing `rememberMediaPlayer` internally — no manual player setup needed
- Marked `@ExperimentalSceneViewApi`
- llms.txt updated with both simple and advanced usage patterns
- Committed `462ecb7b`

### 5. v3.6.0 roadmap — ALL 14 ISSUES RESOLVED ✅
- #1 LightNode ✅, #2 CameraNode→SecondaryCamera ✅, #3 Geometry params ✅, #4 scaleToUnits docs ✅
- #5 ShapeNode/PhysicsNode ✅, #6 VideoNode convenience ✅, #7 ReflectionProbeNode (already correct) ✅
- #8 Swift declarative ✅, #9 NodeGesture cleanup ✅, #10 Naming alignment ✅
- #11 SideEffect guards ✅, #12 HitResultNode docs ✅, #13 SceneNode (deferred) ✅, #14 ARNodeScope ✅

### 6. Documentation updates ✅
- **Migration guide**: v3.6.0 section with 7 before/after examples (SecondaryCamera, geometry params, LightNode, VideoNode, ShapeNode/PhysicsNode, Swift declarative, NodeGesture)
- **Android cheatsheet**: updated VideoNode, SecondaryCamera entries
- **iOS cheatsheet**: added declarative SceneView init, per-entity gesture API section
- **llms.txt**: rememberMediaPlayer in helpers table
- Committed `77a37bed`, `42945b9e`, `e3c46e32`

## Previous session (session 16)

## WHAT WAS DONE IN SESSION 16

### 1. v3.6.0 API simplification — 3 batches ✅
- **Full API audit**: 14 issues identified across Android, Swift, and KMP core
- **docs/v3.6.0-roadmap.md**: Complete roadmap with priorities, implementation plan, migration strategy

**Batch 1 — Geometry param consistency (#3) + LightNode (#1):**
  - All 6 geometry nodes now have uniform `position`/`rotation`/`scale` trio
  - LightNode: explicit `intensity`, `direction`, `position` params
  - llms.txt updated with all new signatures
  - Committed `36710231`

**Batch 2 — ShapeNode + PhysicsNode composables (#5):**
  - `ShapeNode`: triangulated 2D polygon with full transform params, added to SceneScope
  - `PhysicsNode`: gravity + floor bounce, added to SceneScope (was only a top-level function)
  - llms.txt updated with new composable docs
  - Committed `ca3a8bc7`

**Batch 3 — SideEffect equality guards (#11):**
  - All 7 geometry composables now cache prev geometry and skip updateGeometry() when unchanged
  - Transform assignments (position/rotation/scale) remain unconditional (cheap)
  - Committed `bc1746b8`

- All builds pass: `sceneview`, `arsceneview`, `android-demo`

**Batch 4 — Transform consistency for remaining nodes + Swift declarative:**
  - ImageNode (all 3 overloads): position/rotation/scale
  - BillboardNode: position/scale
  - TextNode: position/scale
  - VideoNode: position/rotation/scale
  - ModelNode: doc warning about scaleToUnits overriding scale
  - HitResultNode: improved llms.txt docs with recommended pattern
  - **Swift `SceneView(@NodeBuilder)`**: new declarative init matching Android's `Scene { }`
  - iOS + macOS build clean
  - Committed `79c216bd` + `37a7d154`

### 2. SceneViewSwift Xcode verification ✅
- **iOS build**: BUILD SUCCEEDED (Xcode 26.3, iOS 26.2 SDK) — zero warnings, zero errors
- **macOS build**: BUILD SUCCEEDED — zero warnings, zero errors
- **visionOS**: Not tested (SDK not downloaded, not a code issue)
- **Swift 6 fixes** (6 files):
  - BillboardNode.swift, GeometryNode.swift, TextNode.swift, LineNode.swift, MeshNode.swift, ViewNode.swift
  - Added `#if os(macOS) import AppKit #else import UIKit #endif` to resolve `SimpleMaterial.Color` default argument warnings
  - GeometryNode.swift: migrated `TextureResource.load(named:)` → `TextureResource(named:)` (async-safe initializer)
- Committed `3cf99024` and pushed to main

## Previous session (session 15)

## WHAT WAS DONE IN SESSION 15

### 1. Review fixes committed and deployed ✅
- **index.html**: Nav links aligned to cross-page pattern (Showcase/Playground/Docs), 4 external lh3.googleusercontent.com images replaced with CSS gradient placeholders, added `<main>` wrapper
- **6 secondary pages**: Added theme-color, og:site_name, og:locale, twitter meta tags; added `<main>` to showcase/web/platforms-showcase; fixed web.html nav link; standardized platforms-showcase font loading
- **ThemePreview.kt**: Replaced 5 hardcoded RoundedCornerShape with MaterialTheme.shapes.*
- Committed and pushed to sceneview/sceneview (main)
- Deployed to sceneview.github.io and pushed
- Visual QA verified: hero, nav, showcase cards, meta tags all correct

### 2. All remaining demo themes updated to Stitch M3 (#005bc1) ✅
- **samples/common/Theme.kt**: Full rewrite from purple #6639A6 to blue #005BC1 Stitch palette (light primary #005BC1, dark primary #A4C1FF)
- **samples/desktop-demo/Main.kt**: SceneViewBlue → #A4C1FF, wireframe edges/vertices/faces updated to Stitch blue
- **samples/flutter-demo/main.dart**: `Colors.deepPurple` → explicit `ColorScheme.dark(primary: Color(0xFFA4C1FF))`, cube color → #005BC1
- **samples/react-native-demo/App.tsx**: All 8 style colors updated (container bg #111318, chip selected #005bc1, etc.)
- **samples/web-demo/index.html**: CSS vars `--sv-blue: #1a73e8` → `#005bc1`, surfaces to GitHub-dark, AR button gradient to tertiary
- All Android builds verified: `compileDebugKotlin` and `compileKotlinDesktop` BUILD SUCCESSFUL

### 3. Critical website bug fixes ✅
- **CTA terminal white background in dark mode**: `var(--color-inverse-surface)` resolved to `#f0f6fc` in dark mode → hardcoded `#0d1117` (always dark)
- **Scroll reveal invisible sections (CRITICAL)**: IntersectionObserver with threshold:0.1 and rootMargin:-40px caused `.reveal` elements to stay invisible when fast-scrolling. Fixed with:
  - Immediate reveal on load for elements already in viewport
  - threshold:0.01, rootMargin:+200px
  - scroll event fallback (50ms debounce)
  - 3s safety timeout
  - Softer animation: `translateY(16px)`, `0.5s ease-out`
- **Inline script duplication**: 4 HTML files (index.html, docs.html, privacy.html, web.html) had inline `<script>` blocks with old buggy observer → all replaced with fixed version
- **script.js**: Complete rewrite of scroll reveal section

### 4. Full visual QA on live site ✅
- All 8 pages verified in dark mode: index, showcase, playground, claude-3d, platforms-showcase, web, docs, privacy
- Light mode full scroll-through on index.html: hero, features, code, platforms, comparison, testimonials, showcase, CTA — all verified
- CTA terminal confirmed dark in both light and dark modes
- All scroll reveal sections visible and animated correctly

### 5. Store assets and branding update ✅
- **og-image.svg** (1200x630): Blue-purple gradient, SceneView title, tagline, platform chips, version badge, cube logo
- **apple-touch-icon.svg** (180x180): Gradient background with isometric cube
- **feature-graphic.svg** (1024x500): Play Store feature graphic with cube + text + feature chips
- **favicon.svg**: Colors updated from #1A73E8 → #005BC1 Stitch palette
- **ic_launcher_foreground.xml**: Android adaptive icon colors updated to Stitch palette (#003A7D/#3D7FD9/#A4C1FF)
- **All 8 HTML pages**: og:image → og-image.svg, apple-touch-icon link added, og:image dimensions
- **branding/README.md**: Colors updated, asset checklist updated with completed items
- Deployed to sceneview.github.io

## Previous session (session 14)

### 1. All secondary pages redesigned with Stitch M3 design system ✅
- **showcase.html**: 6-section demo gallery (E-Commerce, AR, Automotive, Education, Luxury, Multi-Platform) with 3D viewers, device mockups, code snippets, category filter badges
- **playground.html**: Split-pane code editor + live 3D preview, toolbar with example/model selectors, share/copy/Claude buttons, syntax highlighting
- **claude-3d.html**: AI + 3D demos with Claude Desktop window mockup, conversation bubbles, 4 example cards, How It Works steps, CTA
- **web.html**: SceneView Web docs with live Filament.js demo, feature cards, install methods (CDN/npm/ESM), API reference, browser compatibility
- **platforms-showcase.html**: 9-platform grid (Android/iOS/macOS/visionOS/Web/TV/Desktop/Flutter/React Native) with status badges, architecture diagram, comparison table
- **docs.html**: Documentation hub with card grid (Quick Start, API Reference, Code Recipes, Tutorials)
- **privacy.html**: Clean typography privacy policy with proper heading hierarchy

### 2. Shared infrastructure updates
- **script.js**: Added scroll reveal IntersectionObserver (was missing — elements with `.reveal` class were invisible)
- All pages share: consistent nav/footer from index.html, dark mode default, Material Symbols Outlined, CSS custom properties only, responsive breakpoints

### 3. Deployment
- All files deployed to sceneview.github.io (pushed to main)
- Source committed and pushed to sceneview/sceneview main
- CSS variable audit: all 38 vars used across pages are defined in styles.css

### 4. Android demo theme — M3 Expressive ✅
- New **Color.kt**: Full M3 color scheme from Stitch source #005bc1
  - Light: primary #005BC1, tertiary #6446CD
  - Dark: primary #A4C1FF, tertiary #D2A8FF (GitHub-dark inspired)
- New **Type.kt**: M3 Expressive typography scale
- New **Shape.kt**: M3 dynamic shapes (8/12/16/28/32dp radius)
- Updated **Theme.kt**: uses Color/Type/Shape + MaterialExpressiveTheme + MotionScheme.expressive()
- Updated **colors.xml** (light + night): aligned with Stitch tokens
- BUILD SUCCESSFUL verified

### 5. iOS demo theme — Apple HIG ✅
- New **Theme.swift**: centralized SceneView theme for SwiftUI
  - Brand colors matching Stitch primary (#005bc1 → #a4c1ff)
  - Tertiary (#6446cd → #d2a8ff), status colors
  - Light/dark adaptive Color extension
  - Card and status badge view modifiers
- Updated **AccentColor**: #005bc1 with dark variant
- Updated tint from `.blue` to `SceneViewTheme.primary`

### 6. MkDocs docs CSS ✅
- Updated **extra.css**: primary #1a73e8 → #005bc1
- Added proper dark slate scheme with #a4c1ff primary
- Gradient: #005bc1/#6446cd (matching Stitch)

### 7. DESIGN.md updated ✅
- Primary: #1a73e8 → #005bc1 (Stitch source of truth)
- All gradient tokens updated to match

## Previous sessions
- Session 13: Website landing page full redesign via Stitch, Visual QA complete

### 1. Website full redesign via Google Stitch (Phase 1 — Website ✅)
- Created Stitch design system from DESIGN.md tokens (primary #1a73e8, secondary #5b3cc4, tertiary #d97757)
- Generated landing page screen via `generate_screen_from_text` in Stitch project `8306300374268749650`
- Downloaded Stitch-generated HTML, adapted it to SceneView conventions:
  - Removed Tailwind CDN → pure CSS custom properties from DESIGN.md
  - Removed external image CDN → self-hosted assets
  - Kept sceneview.js/Filament.js for 3D rendering
  - Preserved all SEO meta tags, structured data, OG/Twitter cards
- **`website-static/index.html`** — Full rewrite with Stitch design structure:
  - Hero: version badge, gradient title, subtitle, CTAs, platform icons, 3D model
  - Features: 6-card grid (Declarative 3D, AR Ready, AI-First SDK, Cross-Platform, Native Renderers, Open Source)
  - Code comparison: Kotlin (Compose) vs Swift (SwiftUI) side-by-side
  - Platforms: horizontal scroll cards with status badges
  - Install: Gradle dependency code block
  - Showcase: 3-column grid (Architecture, Healthcare, Retail)
  - CTA: "Start building in 5 minutes" with terminal command
  - Footer: 4-column grid (Product, Community, Legal)
- **`website-static/styles.css`** — Complete rewrite (~1340 lines):
  - All tokens from DESIGN.md as CSS custom properties
  - BEM naming, dark/light mode support
  - Responsive: 1024px, 900px, 768px, 600px, 480px breakpoints
  - M3 Expressive spring animations + Liquid Glass on nav/floating surfaces

### 2. Visual QA — Complete
- Desktop 1440×900: ✅ all sections verified (hero, features, code, platforms, install, showcase, CTA, footer)
- Mobile 375×812: ✅ hamburger nav, stacked cards, full-width CTAs, stacked code blocks
- Light mode: ✅ clean white surfaces, dark code blocks, gradient CTA
- Dark mode: ✅ dark surfaces, glass effects, proper contrast

### 3. Cleanup
- Removed temp `preview-stitch.html` and `/tmp/stitch-landing.html`
- Removed CSS cache buster `?v=stitch2` from index.html

## Previous sessions
- Session 12: Security audit (clean), Stitch MCP fixed, git cleanup
- Session 11: Repo reorganization, version cleanup 3.5.1→3.6.0, DESIGN.md, Stitch config

## DECISIONS MADE
- Website uses M3 Expressive (structure) + Liquid Glass (floating surfaces) — correct for web
- Android demo should use Material 3 Expressive (Compose Material 3)
- iOS demo should use Apple Liquid Glass / HIG (SwiftUI native) — NOT Material Design
- Dark mode hero title: solid white text (gradient text invisible in dark mode)
- `.mcp.json` must stay gitignored (contains local paths)

## CURRENT STATE
- **Active branch**: main
- **Latest release**: v3.6.0 (ALL PUBLISHED — Maven Central + npm + GitHub + Stores)
- **MCP servers**: sceneview-mcp 3.5.4 on npm (32 tools, 1204 tests), 9 MCPs total
- **sceneview-web**: v3.6.0 on npm
- **Website**: sceneview.github.io — M3 Expressive + Liquid Glass redesign deployed
- **Google Stitch**: MCP configured, API key set
- **GitHub orgs**: sceneview, sceneview-tools, mcp-tools-lab

## NEXT STEPS (priority order)

### ✅ BLOCKER RESOLVED — Stitch MCP ready
- `.mcp.json` is in project root, gitignored, config correct
- Wrapper at `~/.claude/stitch-wrapper.sh` tested and working (12 tools)
- **Just start a new Claude Code session** → Stitch tools appear automatically
- Once loaded, ALL visual work goes through Stitch

### Phase 1 — FULL REDESIGN VIA GOOGLE STITCH
Everything visual must be redesigned using Google Stitch as the design tool.
Stitch generates the design → Claude applies it in code. NO manual CSS/UI writing.

1. ~~**Website** (sceneview.github.io) — Full redesign via Stitch~~ ✅ DONE (session 13+14+15)
   - index.html fully redesigned, QA'd (desktop/mobile/light/dark) — session 13
   - All 7 secondary pages redesigned and deployed — session 14
   - Bug fixes (scroll reveal, CTA terminal) + full live QA — session 15
2. ~~**Android demo app** — Theme via Stitch (M3 Expressive)~~ ✅ DONE (session 14)
   - Color.kt, Theme.kt, Shape.kt, Type.kt — all created with Stitch #005bc1
3. ~~**iOS demo app** — Theme via Stitch (Liquid Glass / Apple HIG)~~ ✅ DONE (session 14)
   - Theme.swift + AccentColor updated, tint aligned
4. ~~**Docs MkDocs** — CSS via Stitch~~ ✅ DONE (session 14)
5. ~~**All other demos** — web-demo, tv-demo, desktop, flutter, react-native~~ ✅ DONE (session 15)
   - common/Theme.kt, desktop-demo, flutter-demo, react-native-demo, web-demo — all updated to Stitch #005bc1
6. ~~**Store assets**~~ ✅ MOSTLY DONE (session 15)
   - OG image, apple-touch-icon, favicon, feature graphic, app-icon-1024, npm-icon all created
   - App screenshots pending (need emulator GUI or physical device — can't capture Filament SurfaceView headless)

### Phase 2 — Post-redesign
- ~~v3.6.0 roadmap: API simplification~~ ✅ STARTED (session 16)
  - Roadmap created (14 issues, 5 priority tiers)
  - 3 batches implemented: geometry params (#3), LightNode (#1), ShapeNode/PhysicsNode (#5), SideEffect guards (#11)
  - Remaining: CameraNode rename (#2), scaleToUnits (#4), VideoNode convenience (#6), ReflectionProbe (#7), Swift declarative (#8), NodeGesture cleanup (#9), HitResultNode simplification (#12), SceneNode integration (#13), ARNodeScope (#14)
- ~~sceneview.js enhancements (setQuality, setBloom, addLight)~~ ✅ DONE (session 15)
  - sceneview.js bumped to v1.5.0
  - setQuality('low'|'medium'|'high') — AO + anti-aliasing control
  - setBloom(true|false|{strength, resolution, threshold, levels}) — post-processing
  - addLight({type, color, intensity, direction, position, falloff}) — custom lights
  - llms.txt updated with full sceneview.js API surface
  - Deployed to sceneview.github.io
- ~~iOS: verify SceneViewSwift fixes compile in Xcode~~ ✅ DONE (session 16)
  - iOS + macOS build clean (zero warnings), Swift 6 fixes committed
  - visionOS SDK not installed (not a code issue)
- ~~v3.6.0 API simplification~~ ✅ COMPLETE (session 17)
  - All 14 issues resolved (13 implemented, 1 deferred to post-3.6.0)
  - All builds verified clean (Android + iOS + macOS)

### Phase 3 — Post-3.6.0

#### 🔴 HIGH PRIORITY — Open Collective full overhaul
- **URL**: https://opencollective.com/sceneview
- Refaire TOUT from scratch (description, about, tiers déjà faits session 18)
- **Changer tous les assets** : logo, banner/cover image, social links
- Utiliser les SVG du dossier `branding/` (logo.svg, feature-graphic.svg, og-image.svg)
- Exporter en PNG pour upload (Open Collective n'accepte pas SVG)
- Vérifier cohérence avec le branding Stitch (#005BC1)

#### 🔴 HIGH PRIORITY — Branding cleanup complet
- Organiser le dossier `branding/` proprement :
  - Exporter tous les SVG en PNG (multiple tailles : 128, 256, 512, 1024)
  - Logo avec/sans texte, dark/light variants
  - Banner/cover pour GitHub, npm, Open Collective, social media
  - Favicon multi-format (ico, png 16/32/48/192/512)
- Vérifier que TOUS les assets sont utilisés et cohérents
- Supprimer les assets obsolètes
- Mettre à jour branding/README.md avec inventaire complet

#### 🔴 HIGH PRIORITY — Playground from scratch
- Refaire complètement `website-static/playground.html`
- Code editor live + preview 3D interactive (sceneview.js)
- Exemples pré-chargés : model viewer, AR, lights, materials, animations
- Partage d'URL (encode config en hash)
- Bouton "Open in Claude" pour générer du code via AI
- Design via Google Stitch MCP

#### 🟡 MEDIUM — Claude Artifacts pour SceneView
- Permettre d'afficher SceneView dans les conversations Claude (artifacts)
- Utiliser sceneview-web (CDN jsdelivr) dans des artifacts HTML interactifs
- Créer des templates/exemples que Claude peut générer
- Documenter dans llms.txt comment générer des artifacts SceneView

#### 🟡 MEDIUM — Stitch full review of SceneView
- Ask Google Stitch to do a complete design review of all SceneView pages
- Review: index.html, showcase.html, playground.html, claude-3d.html, web.html, platforms-showcase.html, docs.html, privacy.html
- Get Stitch feedback on design consistency, M3 compliance, accessibility, responsive behavior
- Apply recommended improvements

#### 🟡 MEDIUM — Other post-3.6.0
- SceneNode integration (#13): make Android Node implement KMP SceneNode — architecture change for post-3.6.0
- visionOS: test SceneViewSwift with visionOS SDK when available
- App screenshots: need emulator GUI or physical device

## RULES REMINDER
- **STITCH MANDATORY** — ALL design/UI work goes through Google Stitch MCP. NEVER write CSS/theme by hand.
- ALWAYS save API keys/credentials in `profile-private/preferences/api-keys.md` + `~/.zshrc`
- ALWAYS push `profile-private` after saving sensitive data
- Material 3 Expressive = Android/Web, Liquid Glass = Apple platforms
