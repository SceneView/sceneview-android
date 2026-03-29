# SceneView Branding Assets

> **Design system source of truth:** [`DESIGN.md`](../DESIGN.md) (Google Stitch format)
> All colors, typography, spacing, and component patterns are defined there.

## Logo
The SceneView logo is a **3D isometric cube** in blue gradient.
- Top face: White (#FFFFFF)
- Right face: Light blue (#64B5F6)
- Left face: Dark blue (#1565C0)
- Background: Blue (#1565C0 or #2196F3)

The vector source is in `samples/android-demo/src/main/res/drawable/ic_launcher_foreground.xml`.

## Brand Colors

See `DESIGN.md` for the full palette (light + dark mode). Key values:

| Name | Hex | Usage |
|---|---|---|
| Primary | #1a73e8 | Website primary, links, accents |
| Primary (dark) | #8ab4f8 | Dark mode primary |
| Brand Dark Blue | #1565C0 | Logo, gradients |
| Brand Light Blue | #64B5F6 | Logo, highlights |
| Surface Dark | #0D1117 | Dark mode background |
| Surface Variant | #161B22 | Cards, elevated surfaces |

## Typography
- **Primary:** Inter (web), system default (mobile)
- **Code:** JetBrains Mono

See `DESIGN.md` for full font scale, weights, and letter spacing.

## Store Assets Needed

### Google Play Store
- [ ] Feature graphic (1024x500) — blue gradient + 3D cube + "SceneView" text
- [ ] Icon (512x512) — adaptive icon with blue cube
- [ ] Screenshots (phone, tablet, TV) — demo app running
- [ ] Short description: "3D & AR SDK for Android — Jetpack Compose, Filament, ARCore"
- [ ] Full description with features list

### Apple App Store
- [ ] App icon (1024x1024) — blue cube on blue gradient
- [ ] Screenshots (iPhone, iPad) — iOS demo app running
- [ ] App preview video (optional)
- [ ] Description matching Play Store

### npm
- [ ] Package icon for sceneview-mcp and sceneview.js

### Website
- [ ] Favicon (blue cube) — 32x32, 16x16
- [ ] OG image (1200x630) — blue gradient + cube + tagline
- [ ] Apple touch icon (180x180)
