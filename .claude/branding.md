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

## Source File

All tokens are defined in:
`website/src/jsMain/kotlin/io/github/sceneview/website/Theme.kt`
