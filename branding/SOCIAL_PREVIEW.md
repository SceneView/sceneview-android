# GitHub Social Preview — SceneView

**Size**: 1280x640 PNG (2:1 ratio, required by GitHub)
**Location**: Repo Settings > General > Social preview

## Design Specification

### Layout

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  ┌──┐                                                          │
│  │  │   Background: linear-gradient(135deg, #0D2137, #132D4F)  │
│  └──┘                                                          │
│                                                                │
│         ┌─┐                                                    │
│         │ ╲   3D isometric cube (from logo.svg)                │
│         │  │   centered horizontally, upper-third vertically   │
│         └──┘   ~200x200px                                      │
│                                                                │
│            SceneView                                           │
│            Inter Bold, 64px, #FFFFFF                           │
│                                                                │
│            3D & AR for Compose — Android, iOS, Web             │
│            Inter Regular, 28px, #8AB4F8                        │
│                                                                │
│   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐            │
│   │ Android │ │  iOS    │ │  Web    │ │ visionOS│            │
│   └─────────┘ └─────────┘ └─────────┘ └─────────┘            │
│   Platform pills: 12px Inter Medium, #FFFFFF bg, 8px padding  │
│   Pill bg: rgba(255,255,255,0.1), border: rgba(255,255,255,0.2)│
│                                                                │
│                                     github.com/SceneView       │
│                                     Inter Regular, 16px, #5A8DBF│
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Colors

| Element | Color |
|---|---|
| Background gradient start | `#0D2137` |
| Background gradient end | `#132D4F` |
| Cube faces | Same as `logo-dark.svg` gradients |
| Title text | `#FFFFFF` |
| Tagline text | `#8AB4F8` |
| Platform pill background | `rgba(255,255,255,0.1)` |
| Platform pill border | `rgba(255,255,255,0.2)` |
| Platform pill text | `#FFFFFF` |
| URL text | `#5A8DBF` |

### Export instructions

Using Figma, Sketch, or ImageMagick:

```bash
# Quick generation with ImageMagick (if available)
# For best results, use Figma with the layout above

convert -size 1280x640 \
  gradient:'#0D2137-#132D4F' \
  -gravity center \
  -font Inter-Bold -pointsize 64 -fill white \
  -annotate +0-40 'SceneView' \
  -font Inter -pointsize 28 -fill '#8AB4F8' \
  -annotate +0+30 '3D & AR for Compose — Android, iOS, Web' \
  branding/social-preview.png
```

### Checklist

- [ ] Export at exactly 1280x640px
- [ ] Upload at: github.com/sceneview/sceneview > Settings > Social preview
- [ ] Verify preview on: https://www.opengraph.xyz/
- [ ] Looks good on Twitter/X card preview
- [ ] Looks good on Slack/Discord link preview
- [ ] Text is legible at thumbnail sizes (~600px wide)
