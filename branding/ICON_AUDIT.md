# SceneView Icon & Branding Audit

Audited: 2026-03-26

## Brand Assets (Source of Truth)

| File | Description |
|---|---|
| `branding/logo.svg` | Light mode logo — 512x512, blue cube + viewport brackets |
| `branding/logo-dark.svg` | Dark mode logo — brighter blues for dark backgrounds |
| `website-static/favicon.svg` | Favicon — 32x32, compact isometric cube + brackets |

### Brand Colors
- Primary: `#1A73E8`
- Light: `#8AB4F8`
- Dark background: `#0D2137`
- Cube faces: `#1558B0` (left/shadow), `#4A90D9` (right/lit), `#8AB4F8` (top/light)

---

## Platform Status

### Android Demo (`samples/android-demo/`)
- **Status: COMPLETE**
- Adaptive icon foreground: `ic_launcher_foreground.xml` — blue cube with viewport brackets
- v26 background: `#0D2137` (dark navy)
- v33 background: `#1A73E8` (brand primary) + monochrome support
- Bitmap fallbacks: all densities (mdpi through xxxhdpi)
- Play Store icon: `ic_launcher-playstore.png` (512x512)

### Android TV Demo (`samples/android-tv-demo/`)
- **Status: COMPLETE** (fixed in this audit)
- Adaptive icon foreground: identical to android-demo
- v26 background: `#0D2137` (dark navy)
- v33 monochrome: added (was missing)
- Note: no bitmap fallback PNGs (OK for TV, adaptive icon covers modern devices)

### iOS Demo (`samples/ios-demo/`)
- **Status: NEEDS MANUAL WORK**
- `AppIcon.appiconset/Contents.json` exists but has NO image file
- AccentColor: set to `#1A73E8` (light) / `#8AB4F8` (dark) — fixed in this audit
- **ACTION REQUIRED**: Generate a 1024x1024 PNG from `branding/logo.svg` with `#0D2137` background and add it as `AppIcon.appiconset/AppIcon.png`, then update Contents.json to reference it

### Website (`website-static/`)
- **Status: COMPLETE**
- `favicon.svg`: blue cube with viewport brackets, brand colors
- Referenced in `index.html` as `<link rel="icon" type="image/svg+xml" href="/favicon.svg">`
- og:image points to favicon.svg — works but a 1200x630 PNG social preview would be better

### GitHub Social Preview
- **Status: TEMPLATE READY**
- `branding/social-preview.html` — 1280x640 template with dark mode logo
- **ACTION REQUIRED**: Screenshot this HTML at 1280x640, upload as repository social preview in GitHub Settings > General > Social preview

### npm Package (`mcp/`)
- **Status: NO ICON FIELD**
- npm doesn't have an icon field in package.json
- The npm registry page will show the GitHub social preview if set
- No action needed beyond setting the GitHub social preview

### Chrome Extension / sceneview-tools
- **Status: NOT IN REPO**
- No extension icons found in the repository
- If the Chrome extension is published separately, it needs 16/48/128px PNGs
- Generate from `branding/logo.svg` with `#0D2137` background

---

## Manual Actions Required

1. **iOS AppIcon**: Render `branding/logo.svg` at 1024x1024 with `#0D2137` background, save as PNG, add to `samples/ios-demo/SceneViewDemo/Assets.xcassets/AppIcon.appiconset/`
2. **GitHub Social Preview**: Open `branding/social-preview.html` in browser, screenshot at 1280x640, upload to GitHub repo settings
3. **Chrome Extension Icons** (if needed): Generate 16x16, 48x48, 128x128 PNGs from logo.svg
