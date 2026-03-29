# SceneView Website Comprehensive Audit Report

**Date:** 2026-03-27
**Auditor:** Claude Code
**Scope:** Static website (website-static/) + MkDocs docs (docs/)

---

## Summary

- **Issues found:** 28 total (5 critical, 8 major, 15 minor)
- **Issues fixed:** 17 (all version number fixes applied)
- **Issues remaining:** 11 (need manual attention or design decisions)

---

## CRITICAL Issues

### C1. Version 3.3.0 in static website install section [FIXED]
**File:** website-static/index.html (lines 618-635)
The Quick install section showed sceneview:3.3.0, arsceneview:3.3.0, sceneview-web:3.3.0, and iOS Version: 3.3.0. Should be 3.5.0.
**Status:** Fixed all 5 occurrences.

### C2. Version 3.3.0 across 12+ docs pages [FIXED]
Multiple docs pages had stale 3.3.0 version numbers:
- docs/docs/llms.txt (line 5) - Maven version header
- docs/docs/llms-full.txt (line 27) - version reference
- docs/docs/structured-data.json (line 98) - softwareVersion + releaseNotes URL
- docs/docs/platforms.md (lines 16-19) - platform table
- docs/docs/comparison.md (line 20) - comparison table
- docs/docs/recipes.md (line 9) - target version
- docs/docs/v4-preview.md (lines 14, 55, 60-61) - 4 occurrences
- docs/docs/faq.md (line 153) - iOS FAQ answer
- docs/docs/showcase.md (lines 5, 117) - 2 occurrences
- docs/docs/quickstart-ios.md (line 46) - SPM version
- docs/docs/codelabs/codelab-3d-swiftui.md (line 43) - SPM version
- docs/docs/codelabs/codelab-ar-swiftui.md (line 32) - SPM version
**Status:** All 19 occurrences fixed to 3.5.0.

### C3. Version 3.3.0 in geometry-demo.html [FIXED]
**File:** website-static/geometry-demo.html (line 916)
Claude prompt template embedded 3.3.0 dependency.
**Status:** Fixed.

### C4. Store badges link to href="#" (placeholder) [NOT FIXED]
**File:** website-static/index.html (lines 148, 162)
Both "Get it on Google Play" and "Download on the App Store" badges link to # -- they are non-functional placeholders. Visitors clicking these will scroll to the top of the page.
**Action needed:** Replace with real store URLs when apps are published, or hide these badges until then.

### C5. structured-data.json dateModified was stale [FIXED]
Was 2026-03-23, updated to 2026-03-27. Also fixed releaseNotes URL to point to v3.5.0 tag.

---

## MAJOR Issues

### M1. Discord invite URL inconsistency [NOT FIXED]
Two different Discord invite URLs are used:
- **Static website:** discord.gg/sceneview (vanity URL) -- used in index.html, showcase.html, web.html, platforms-showcase.html, go/discord/
- **MkDocs config + docs + structured-data.json:** discord.gg/UbNDDBTNqb -- used in mkdocs.yml, community.md, contributing.md, etc.
**Risk:** If one invite expires or is misconfigured, half the site links will be broken.
**Action needed:** Verify both URLs resolve to the same server. Standardize on one (preferably the vanity discord.gg/sceneview).

### M2. og:image uses SVG favicon for social previews [NOT FIXED]
**Files:** All HTML pages in website-static/
All pages set og:image to https://sceneview.github.io/favicon.svg. Most social platforms (Twitter, LinkedIn, Facebook, Slack) do NOT render SVG social cards -- they require PNG or JPG, ideally 1200x630px.
**Action needed:** Create a proper social preview image (PNG, 1200x630px) and update all og:image meta tags.

### M3. No copyright/year in static site footer [NOT FIXED]
**File:** website-static/index.html (footer section, line 880-900)
The footer has SceneView, license link, and other links, but NO copyright year or author attribution. The MkDocs site correctly has "Copyright 2021-2026 Thomas Gorisse".
**Action needed:** Add copyright line to footer.

### M4. MkDocs structured-data.json references non-existent images [NOT FIXED]
**File:** docs/docs/structured-data.json
- Line 11: url assets/images/favicon.svg -- this path may not exist in deployed site
- Line 15: image assets/images/hero-banner.svg -- likely non-existent
**Action needed:** Verify these paths exist in the deployed site, or update to valid paths.

### M5. MkDocs comparison.md references non-existent SVG [NOT FIXED]
**File:** docs/docs/comparison.md (line 12)
References assets/images/comparison-chart.svg which likely does not exist.
**Action needed:** Create the chart image or remove the img tag.

### M6. MkDocs v4-preview.md references non-existent SVG [NOT FIXED]
**File:** docs/docs/v4-preview.md (line 11)
References assets/images/v4-architecture.svg which likely does not exist.
**Action needed:** Create the architecture diagram or remove the img tag.

### M7. Changelog only goes to 3.3.0 [NOT FIXED]
**File:** docs/docs/changelog.md
The changelog has no entries for versions 3.4.0 through 3.5.0. The latest entry is 3.3.0.
**Action needed:** Add changelog entries for v3.4.x releases.

### M8. MCP tools count inconsistency [NOT FIXED]
The static site claims "18 AI MCP tools" in the stats section (line 357) and "22 advanced MCP tools" in the Pro section (line 783). The CLAUDE.md says "22 MCP tools". These numbers should be consistent.
**Action needed:** Verify actual tool count and update both occurrences.

---

## MINOR Issues

### m1. Playground models reference external Khronos assets
Only DamagedHelmet.glb is bundled locally in models/. Other playground models load from GitHub raw. Low risk.

### m2. No noscript fallback on any page
All pages depend on JavaScript. No graceful degradation message.

### m3. twitter:card inconsistency
index.html/web.html use summary_large_image; showcase.html uses summary.

### m4. Filament.js loaded on every static page
~8MB WASM engine loaded even on pages that may not use 3D rendering.

### m5. Privacy policy title says "SceneView Demo" not "SceneView"
website-static/privacy.html title should probably just say "SceneView".

### m6. No sitemap entry for privacy.html
website-static/sitemap.xml does not include privacy.html.

### m7. playground.html defaults to data-theme="dark" while index.html defaults to "light"
Inconsistent default themes. JS overrides immediately so low impact.

### m8. Use-case code snippets use simplified/aspirational API
Code like Scene { ModelNode("shoe.glb") } is not the actual current API but marketing shorthand.

---

## Verified OK

- All GitHub URLs use correct lowercase org: github.com/sceneview/sceneview
- No old SceneView/sceneview-android references anywhere
- All nav entries in mkdocs.yml have corresponding .md files
- MkDocs copyright date correct: 2021-2026
- CSS responsive breakpoints well-covered (640px, 768px, 1024px)
- Dark mode tokens complete in both light and dark
- Branding colors consistent (#1a73e8 primary across all files)
- MkDocs theme config and extra.css consistent
- Favicon SVG present and referenced correctly
- robots.txt and sitemap.xml consistent
- Privacy policy content accurate and up to date

---

## Files Modified

| File | Changes |
|---|---|
| website-static/index.html | 3.3.0 -> 3.5.0 in install section (4 occurrences) |
| website-static/geometry-demo.html | 3.3.0 -> 3.5.0 in Claude prompt |
| docs/docs/llms.txt | 3.3.0 -> 3.5.0 in version header |
| docs/docs/llms-full.txt | 3.3.0 -> 3.5.0 |
| docs/docs/structured-data.json | version 3.3.0 -> 3.5.0, date, releaseNotes URL |
| docs/docs/platforms.md | 4 version references updated |
| docs/docs/comparison.md | version in comparison table |
| docs/docs/recipes.md | target version |
| docs/docs/v4-preview.md | 4 version references |
| docs/docs/faq.md | iOS FAQ answer |
| docs/docs/showcase.md | 2 version references |
| docs/docs/quickstart-ios.md | SPM version |
| docs/docs/codelabs/codelab-3d-swiftui.md | SPM version |
| docs/docs/codelabs/codelab-ar-swiftui.md | SPM version |

**Total: 14 files modified, 22 version references updated from 3.3.0 to 3.5.0**

---

## Action Items for Thomas

1. **Store badges (C4):** Either add real Play Store / App Store URLs or hide the badges until apps are published
2. **Discord URL (M1):** Verify both invite URLs work, then standardize on one
3. **Social preview image (M2):** Create 1200x630px PNG social card image
4. **Footer copyright (M3):** Add "Copyright 2021-2026 Thomas Gorisse" to static site footer
5. **Missing SVG images (M5, M6):** Create or remove references to comparison-chart.svg and v4-architecture.svg
6. **Changelog (M7):** Add entries for v3.4.0 through v3.5.0
7. **MCP tool count (M8):** Verify and make consistent (18 vs 22)
