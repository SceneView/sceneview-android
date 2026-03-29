# SceneView Design System

> Agent-friendly design system (Google Stitch DESIGN.md format).
> Source of truth for all UI across website, apps, docs, and store assets.

---

## Brand Identity

- **Name:** SceneView
- **Tagline:** 3D and AR for every platform
- **Logo:** Isometric cube, blue gradient
- **Voice:** Technical but approachable. Developer-first, AI-optimized.

---

## Colors

### Primary

| Token | Light | Dark | Usage |
|---|---|---|---|
| `primary` | #1a73e8 | #8ab4f8 | Links, buttons, accents |
| `primary-hover` | #1557b0 | #aecbfa | Interactive hover states |
| `primary-light` | rgba(26,115,232,0.08) | rgba(138,180,248,0.10) | Subtle backgrounds |
| `primary-subtle` | rgba(26,115,232,0.12) | rgba(138,180,248,0.15) | Borders, overlays |

### Brand Gradient

| Token | Value | Usage |
|---|---|---|
| `gradient-hero` | `linear-gradient(135deg, #1565c0 0%, #5b3cc4 100%)` | Hero headings (light) |
| `gradient-hero-dark` | `linear-gradient(135deg, #8ab4f8 0%, #bb86fc 100%)` | Hero headings (dark) |
| `gradient-hero-alt` | `linear-gradient(135deg, #1a73e8 0%, #6c4fe8 100%)` | Secondary hero treatment |

### Surfaces

| Token | Light | Dark | Usage |
|---|---|---|---|
| `surface` | #ffffff | #0D1117 | Page background |
| `surface-dim` | #f1f3f5 | #161B22 | Secondary background, cards |
| `surface-container` | #ffffff | #161c2c | Elevated surfaces |

### Text

| Token | Light | Dark | Usage |
|---|---|---|---|
| `on-surface` | #1a1a2e | #f3f4f6 | Primary text |
| `on-surface-dim` | #3d4654 | #9ca3af | Secondary text |
| `on-surface-faint` | #5c6370 | #6b7280 | Tertiary text, captions |

### Borders

| Token | Light | Dark | Usage |
|---|---|---|---|
| `outline` | #d6dae0 | #2a3346 | Default borders |
| `outline-subtle` | #ebedf0 | #1f2937 | Light dividers |

### Status

| Token | Value | Usage |
|---|---|---|
| `success` | #16a34a | Positive states, checkmarks |
| `warning` | #f59e0b | Caution states |
| `danger` | #ea4335 | Error states, destructive |
| `info` | #ea580c | Informational highlights |

### Partner Colors

| Token | Value | Usage |
|---|---|---|
| `claude-orange` | #d97757 | Claude/Anthropic branded elements |
| `claude-gradient` | `linear-gradient(135deg, #d97757 0%, #c4622e 100%)` | Claude CTA buttons |
| `discord-purple` | #5865f2 | Discord community links |

### Code Syntax

| Token | Value | Element |
|---|---|---|
| `syntax-keyword` | #cba6f7 | Keywords (val, fun, import) |
| `syntax-function` | #89b4fa | Function names, methods |
| `syntax-string` | #a6e3a1 | String literals |
| `syntax-number` | #fab387 | Numeric values |
| `syntax-comment` | #6c7086 | Comments |
| `code-bg` | #1e1e2e | Code block background (light) |
| `code-bg-dark` | #0d1117 | Code block background (dark) |
| `code-text` | #cdd6f4 | Code text (light) |
| `code-text-dark` | #c9d1d9 | Code text (dark) |

---

## Typography

### Font Families

| Token | Value | Usage |
|---|---|---|
| `font-body` | 'Inter', system-ui, -apple-system, sans-serif | All body text |
| `font-mono` | 'JetBrains Mono', ui-monospace, 'Cascadia Code', 'Fira Code', monospace | Code, terminal |

### Font Scale (responsive)

| Token | Size | Usage |
|---|---|---|
| `text-hero` | clamp(2.5rem, 6vw, 4rem) | Hero title |
| `text-section` | clamp(1.75rem, 4vw, 2.5rem) | Section headings |
| `text-subtitle` | clamp(1rem, 2vw, 1.125rem) | Section subtitles |
| `text-card-title` | 1.125rem | Card titles |
| `text-body` | 1rem (16px) | Body text |
| `text-small` | 0.9rem | Secondary text, labels |
| `text-xs` | 0.85rem | Captions, badges |

### Font Weights

| Token | Value | Usage |
|---|---|---|
| `weight-regular` | 400 | Body text |
| `weight-medium` | 500 | Emphasized body |
| `weight-semibold` | 600 | Subheadings, buttons |
| `weight-bold` | 700 | Section headings |
| `weight-extrabold` | 800 | Hero title |

### Letter Spacing

| Token | Value | Usage |
|---|---|---|
| `tracking-tight` | -0.03em | Headlines |
| `tracking-normal` | 0 | Body text |
| `tracking-wide` | 0.05em | Uppercase labels |

---

## Spacing

Base unit: **8px**

| Token | Value | Usage |
|---|---|---|
| `space-xs` | 4px | Tight gaps, icon padding |
| `space-sm` | 8px | Compact gaps |
| `space-md` | 16px | Default gaps, card padding |
| `space-lg` | 24px | Section gaps, card padding (mobile) |
| `space-xl` | 32px | Card padding (desktop) |
| `space-2xl` | 48px | Large section gaps |
| `space-3xl` | 64px | Section separators |
| `space-4xl` | 96px | Section top/bottom padding |

---

## Border Radius

| Token | Value | Usage |
|---|---|---|
| `radius-xs` | 8px | Small elements, icon containers |
| `radius-sm` | 12px | Code blocks, inputs, badges |
| `radius-md` | 16px | Buttons, medium cards |
| `radius-lg` | 28px | Prominent cards, showcase items |
| `radius-full` | 9999px | Pills, avatars, fully rounded |

---

## Shadows

### Light Mode

| Token | Value | Usage |
|---|---|---|
| `shadow-sm` | 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06) | Subtle lift |
| `shadow-md` | 0 4px 12px rgba(0,0,0,0.1), 0 2px 4px rgba(0,0,0,0.06) | Cards, dropdowns |
| `shadow-lg` | 0 12px 40px rgba(0,0,0,0.12), 0 4px 12px rgba(0,0,0,0.06) | Modals, prominent |
| `shadow-primary` | 0 2px 8px rgba(26,115,232,0.3) | Primary button |
| `shadow-primary-hover` | 0 4px 16px rgba(26,115,232,0.4) | Primary button hover |

### Dark Mode

| Token | Value |
|---|---|
| `shadow-sm` | 0 1px 3px rgba(0,0,0,0.3) |
| `shadow-md` | 0 4px 12px rgba(0,0,0,0.4) |
| `shadow-lg` | 0 12px 40px rgba(0,0,0,0.5) |

---

## Motion

### Easing

| Token | Value | Usage |
|---|---|---|
| `ease-expressive` | cubic-bezier(0.2, 0, 0, 1) | All transitions |

### Duration

| Token | Value | Usage |
|---|---|---|
| `duration-short` | 200ms | Hover, focus, micro-interactions |
| `duration-medium` | 350ms | Card reveals, tab switches |
| `duration-long` | 700ms | Scroll reveal, page transitions |

### Patterns

- **Hover lift:** `translateY(-2px)` with shadow increase
- **Scroll reveal:** `translateY(24px) opacity(0)` to `translateY(0) opacity(1)` over `duration-long`
- **Reduced motion:** Respect `prefers-reduced-motion: reduce`

---

## Breakpoints

| Token | Value | Description |
|---|---|---|
| `bp-desktop` | > 1024px | Full layout, 3-column grids |
| `bp-tablet` | <= 1024px | 2-column grids |
| `bp-mobile` | <= 768px | Hamburger nav, single column |
| `bp-small` | <= 600px | Full-width buttons, stacked |
| `bp-xs` | <= 480px | Reduced padding, compact text |

---

## Layout

| Token | Value | Usage |
|---|---|---|
| `container-max` | 1200px | Content max width |
| `container-padding` | 24px (desktop), 16px (mobile) | Horizontal page padding |
| `nav-height` | 64px | Fixed navigation height |

---

## Components

### Navigation
- Height: `nav-height` (64px)
- Background: `surface` at 88% opacity + `backdrop-filter: blur(16px)`
- Border bottom: 1px solid `outline`
- Position: fixed, z-index: 100

### Buttons
- **Primary:** bg `primary`, text white, radius `radius-md`, shadow `shadow-primary`
- **Outline:** border 1.5px `outline`, transparent bg
- **Ghost:** no border, transparent bg
- **Padding:** 12px 24px (default), 14px 28px (large)
- **Font:** `weight-semibold`, `text-small`
- **Hover:** lift -1px, shadow increase, bg darken

### Cards
- Background: `surface-container`
- Border: 1px solid `outline`
- Radius: `radius-lg` (28px)
- Padding: `space-xl` (32px), `space-lg` on mobile
- Hover: lift -3px, shadow `shadow-lg`, border `primary-subtle`

### Code Blocks
- Background: `code-bg`
- Text: `code-text`, font `font-mono`
- Radius: `radius-sm`
- Padding: `space-md`
- Border: 1px solid rgba(255,255,255,0.05)
- Font size: `text-xs` (0.85rem)

### Tabs
- Padding: 10px 20px
- Background: `surface-dim`
- Radius: `radius-sm`
- Active: bg `surface-container`, text `primary`, `weight-semibold`

---

## Platform Mapping

| Platform | Framework | How to apply |
|---|---|---|
| **Website** | HTML/CSS | CSS custom properties from this file |
| **Android Demo** | Jetpack Compose | Material 3 theme with these tokens |
| **iOS Demo** | SwiftUI | Asset catalog + Color extensions |
| **Docs** | MkDocs Material | CSS overrides in stylesheets/ |
| **Play Store** | Store listing | Screenshots using these colors/typo |
| **App Store** | Store listing | Screenshots using these colors/typo |

---

## Usage with AI Agents

This file is optimized for consumption by AI coding agents (Claude Code, Cursor, Gemini CLI).

**To generate UI matching SceneView's design:**
1. Read this `DESIGN.md` for tokens and patterns
2. Use CSS custom properties (never hardcode values)
3. Support both light and dark modes
4. Follow the component patterns above
5. Use responsive typography with `clamp()`

**With Google Stitch MCP:**
Import this file into a Stitch project to enforce consistent branding across all generated screens.
