# SceneView Branding — Design System Reference

> Snapshot taken 2026-03-24. This file preserves the approved brand identity
> decisions. Do NOT modify the values below without explicit owner approval.

## Logo

- **Logo text**: "SceneView" in Inter 700 weight
- **Logo color (dark mode)**: `#8ab4f8` (PRIMARY_DARK — soft blue)
- **Logo color (light mode)**: `#1a73e8` (PRIMARY — Google Blue)
- **Approved direction**: The blue wordmark on dark navbar is the target identity.
  Next step: design an icon/symbol (3D cube, scene graph, or abstract shape)
  that pairs with the wordmark.

## Color Palette (M3 Expressive)

### Primary
| Token | Light | Dark |
|---|---|---|
| Primary | `#1a73e8` | `#8ab4f8` |
| On Primary | `#ffffff` | `#ffffff` |
| Primary Container | `#d3e3fd` | `#004a77` |

### Secondary
| Token | Light | Dark |
|---|---|---|
| Secondary | `#5f6368` | `#9aa0a6` |

### Tertiary (accent)
| Token | Light | Dark |
|---|---|---|
| Tertiary | `#8b5cf6` | `#a78bfa` |

### Surfaces
| Token | Light | Dark |
|---|---|---|
| Surface | `#fefefe` | `#111827` |
| Surface Container | `#f1f3f4` | `#1f2937` |
| Surface Container Low | `#f8f9fa` | `#171f2e` |

### Text
| Token | Light | Dark |
|---|---|---|
| On Surface | `#1f2937` | `#f9fafb` |
| On Surface Variant | `#5f6368` | `#9ca3af` |

### Code
| Token | Light | Dark |
|---|---|---|
| Code Background | `#f1f3f4` | `#1f2937` |

### Borders
| Token | Light | Dark |
|---|---|---|
| Border | `#dadce0` | `#374151` |
| Outline Variant | `#e8eaed` | `#2d3748` |

## Typography

| Role | Font | Weight | Size |
|---|---|---|---|
| Display Large (hero) | Inter | 700 | 57px |
| Headline Large (sections) | Inter | 600 | 32px |
| Title Large | Inter | 500 | 22px |
| Body Large | Inter | 400 | 16px |
| Label Large (chips) | Inter | 500 | 14px |
| Code | JetBrains Mono | 400-600 | — |

## Shape (M3 Expressive)

| Token | Value | Usage |
|---|---|---|
| Extra Large | 28px | Cards, dialogs |
| Large | 16px | Buttons, containers |
| Medium | 12px | Chips, small cards |
| Small | 8px | Code blocks |
| Full | 9999px | FABs, pills |

## Motion

| Token | Value |
|---|---|
| Emphasized | `cubic-bezier(0.2, 0, 0, 1)` |
| Duration Short | 200ms |
| Duration Medium | 300ms |
| Duration Long | 500ms |

## Platform Badges

| Platform | Color Style |
|---|---|
| Android | Blue (`rgba(26,115,232,0.1)` bg, `PRIMARY` text) |
| Android TV | Same as Android |
| iOS / Apple | Green (`rgba(52,199,89,0.1)` bg, `rgb(36,138,61)` text) |
| Web | Blue (same as Android) |
| Cross-Platform | Green (same as iOS) |

## Marketing Plan

### Launch Communication

**Milestone triggers** — communicate when:
1. **PR #709 merged** → "SceneView now runs on 9 platforms" (LinkedIn post)
2. **Website live on GitHub Pages** → share the URL, demo the hero 3D viewer
3. **Play Store demo published** → "Try SceneView on Android" with QR code
4. **App Store demo published** → "Try SceneView on iOS" — cross-platform story
5. **npm `@sceneview/sceneview-web` published** → "3D in the browser with Filament.js"
6. **v3.4.0 release** → full announcement with all platforms

### Channels
| Channel | Audience | Format |
|---|---|---|
| **LinkedIn** (Thomas Gorisse) | Developers, CTOs, recruiters | Long post + screenshot/video |
| **Twitter/X** | Android/iOS dev community | Short thread + GIF |
| **Reddit** (r/androiddev, r/iOSProgramming, r/webdev) | Platform-specific devs | Show & tell post |
| **Hacker News** | Technical audience | "Show HN: SceneView — 3D as Compose UI, now on 9 platforms" |
| **Dev.to / Medium** | Tutorial seekers | "Build a 3D viewer in 10 lines of Compose" article |
| **Discord** (SceneView server) | Existing community | Changelog + migration guide |
| **Product Hunt** | Early adopters, startups | Launch with demo video |

### Content Ideas
- **Before/after** : 100 lines of OpenGL boilerplate → 10 lines of SceneView Compose
- **Cross-platform demo video** : same 3D scene on Android, iOS, Web, TV, Desktop
- **"AI built this"** : how Claude Code contributed to the SDK (meta story)
- **Architecture diagram** : the native-renderer-per-platform strategy explained

### Timing
- Don't announce until the website is live and the demo apps are on the stores
- Best days for LinkedIn: Tuesday-Thursday morning
- Best for HN: weekday mornings US time
- Coordinate all channels for max impact (same week)

### Logo Next Steps
- Current: blue wordmark "SceneView" in Inter 700
- Needed: icon/symbol to pair with the wordmark
- Direction: 3D cube, scene graph node, or abstract geometric shape
- Keep the blue palette (#1a73e8 / #8ab4f8)

## Source File

All tokens are defined in:
`website/src/jsMain/kotlin/io/github/sceneview/website/Theme.kt`
