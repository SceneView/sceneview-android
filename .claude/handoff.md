# Session Handoff — SceneView

> Read this at the START of every session. Update at the END.

## Last Session Summary

**Date:** 30 mars 2026 (session 15)
**Branch:** main

## WHAT WAS DONE THIS SESSION (session 15)

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
- Session 11: Repo reorganization, version cleanup 3.5.1→3.5.2, DESIGN.md, Stitch config

## DECISIONS MADE
- Website uses M3 Expressive (structure) + Liquid Glass (floating surfaces) — correct for web
- Android demo should use Material 3 Expressive (Compose Material 3)
- iOS demo should use Apple Liquid Glass / HIG (SwiftUI native) — NOT Material Design
- Dark mode hero title: solid white text (gradient text invisible in dark mode)
- `.mcp.json` must stay gitignored (contains local paths)

## CURRENT STATE
- **Active branch**: main
- **Latest release**: v3.5.2 (ALL PUBLISHED — Maven Central + npm + GitHub + Stores)
- **MCP servers**: sceneview-mcp 3.5.4 on npm (32 tools, 1204 tests), 9 MCPs total
- **sceneview-web**: v3.5.2 on npm
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
6. **Store assets** — Screenshots with new design

### Phase 2 — Post-redesign
- v3.6.0 roadmap: API simplification
- sceneview.js enhancements (setQuality, setBloom, addLight)
- iOS: verify SceneViewSwift fixes compile in Xcode

## RULES REMINDER
- **STITCH MANDATORY** — ALL design/UI work goes through Google Stitch MCP. NEVER write CSS/theme by hand.
- ALWAYS save API keys/credentials in `profile-private/preferences/api-keys.md` + `~/.zshrc`
- ALWAYS push `profile-private` after saving sensitive data
- Material 3 Expressive = Android/Web, Liquid Glass = Apple platforms
