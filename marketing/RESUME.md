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

## What's DONE (committed & pushed)

### Batch 1 — Marketing content (14 files)
- Social: x-thread.md, linkedin-post.md, linkedin-video-storyboard.md
- Long-form: devto-article.md, medium-article.md, youtube-script.md
- Positioning: showcase.md, comparison.md, v4-preview.md
- Assets: assets-catalog.md (24 Khronos models, direct HDR URLs)
- GitHub: github-profile.md
- Codelabs: 2 tutorials
- README.md master index

### Batch 2 — Docs site + CI
- Full MkDocs site in `docs/` with 13 pages:
  - Home, Quickstart, FAQ, API Cheatsheet, Troubleshooting
  - Why SceneView, vs. Alternatives, v4.0 Preview
  - Samples gallery (15 samples)
  - 2 codelabs (3D + AR)
  - Contributing, Changelog, Migration guide
- `mkdocs.yml` with Material theme
- Custom CSS with mobile responsiveness
- 7 sample screenshots + 12 site screenshots
- GitHub Actions: docs.yml, docs-on-release.yml

### Batch 3 — Community health
- CODE_OF_CONDUCT.md (Contributor Covenant v2.1)
- SECURITY.md (vulnerability reporting, 90-day disclosure)
- .editorconfig (Kotlin 4-space, UTF-8)
- 3 GitHub Discussion templates (Q&A, Show & Tell, Ideas)

### Batch 4 — Core files updated
- llms.txt bumped to v3.2.0 with 8 new node types
- README.md updated with v3.2 node table and 6 new samples
- Changelog updated with v3.2.0 section
- 6 sample READMEs for new samples

## What could still be done (nice-to-have)

1. **Google Analytics property** — Replace G-XXXXXXXXXX in mkdocs.yml with real property ID
2. **Verify settings.gradle** — Ensure all 15 sample modules are included
3. **Dark mode screenshots** — wkhtmltoimage can't trigger JS palette; needs real Chromium
4. **API docs deployment** — Set up Dokka/Javadoc for sceneview.github.io/api/
5. **Social media posting** — Use the content in marketing/ to post
6. **Video production** — Use youtube-script.md and linkedin-video-storyboard.md to produce videos

## File counts
- 14 marketing content files
- 13 docs pages
- 6 sample READMEs
- 3 community health files
- 3 discussion templates
- 2 CI/CD workflows
- 12 site screenshots
- 7 sample screenshots
- ~4,500 lines of new content
