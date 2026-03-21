# Resume Instructions

If the session crashes, paste this to Claude Code to resume:

---

## Prompt to resume

```
Continue the "make SceneView #1" weekend push on branch `claude/sceneview-marketing-showcase-CJCtV`.

Read `marketing/RESUME.md` for full context on what's done and what's left.
```

---

## Branch
`claude/sceneview-marketing-showcase-CJCtV`

## What's DONE (already committed & pushed)

1. **Marketing website** — Full MkDocs site in `docs/` with:
   - Home page (`docs/index.md`)
   - Showcase/features page (`docs/showcase.md`)
   - Comparison vs alternatives (`docs/comparison.md`)
   - v4.0 preview (`docs/v4-preview.md`)
   - Samples gallery (`docs/samples.md`)
   - Two codelabs (`docs/codelabs/`)
   - Contributing, changelog, migration pages
   - `mkdocs.yml` config with Material theme
   - Custom CSS (`docs/stylesheets/extra.css`)
   - 7 sample screenshots in `docs/screenshots/`

2. **CI/CD** — Two GitHub Actions workflows:
   - `docs.yml` — builds & deploys docs on push
   - `docs-on-release.yml` — rebuilds on release

3. **Marketing content** (14 files in `marketing/`):
   - Social: x-thread.md, linkedin-post.md, linkedin-video-storyboard.md
   - Long-form: devto-article.md, medium-article.md, youtube-script.md
   - Positioning: showcase.md, comparison.md, v4-preview.md
   - Assets: assets-catalog.md (expanded with 24 Khronos models, direct HDR URLs)
   - GitHub: github-profile.md
   - Codelabs: 2 tutorials
   - README.md master index

## What's DONE (staged but NOT yet committed)

4. **6 sample READMEs** — Written for all v3.2.0 samples:
   - `samples/dynamic-sky/README.md`
   - `samples/line-path/README.md`
   - `samples/physics-demo/README.md`
   - `samples/post-processing/README.md`
   - `samples/reflection-probe/README.md`
   - `samples/text-labels/README.md`

## What's LEFT TO DO (in priority order)

### High priority
5. **CODE_OF_CONDUCT.md** — Add Contributor Covenant v2.1 to repo root (GitHub community health score)
6. **SECURITY.md** — Add security policy to repo root (vulnerability reporting, supported versions)
7. **Update llms.txt** — Version shows 3.1.1, needs update to 3.2.0 with 8 new node types:
   - PhysicsNode, DynamicSkyNode, FogNode, ReflectionProbeNode
   - LineNode, PathNode, TextNode (BillboardNode)
8. **.editorconfig** — Add for consistent formatting (Kotlin style, 4-space indent, UTF-8)

### Medium priority
9. **Enhance docs samples page** — `docs/samples.md` needs all 15 samples with descriptions and feature tags (currently only has basic list)
10. **Quickstart guide** — `docs/quickstart.md` step-by-step from empty project to first 3D scene
11. **Troubleshooting guide** — `docs/troubleshooting.md` with common errors and fixes
12. **Polish docs landing page** — Better hero section, tabbed code examples for 3D vs AR

### Lower priority
13. **GitHub Discussions templates** — `.github/DISCUSSION_TEMPLATE/` with Q&A, Show & Tell, Ideas
14. **Update settings.gradle** — Verify all 15 sample modules are included (some new ones may be missing)
15. **Build and screenshot the docs site** — `pip install mkdocs-material && mkdocs build` then screenshot
16. **Google Analytics** — Configure property ID in mkdocs.yml

## Repo structure reference
- `sceneview/` — Core 3D library
- `arsceneview/` — AR layer
- `samples/` — 15 sample apps + common
- `docs/` — MkDocs website source
- `marketing/` — Marketing content & assets catalog
- `.github/` — Workflows, templates, funding
- `mcp/` — MCP server for AI integration
