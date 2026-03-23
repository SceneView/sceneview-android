# Release Template

## Pre-Release Checklist

- [ ] All CI checks passing on main
- [ ] CHANGELOG.md updated with release notes
- [ ] Version bumped in gradle.properties
- [ ] README.md version references updated
- [ ] llms.txt version references updated
- [ ] docs updated with new features
- [ ] Sample apps tested on physical device
- [ ] Migration notes added (if breaking changes)

## Release Process

1. Create release branch: `release/{version}`
2. Bump version in `gradle.properties`
3. Update all version references (README, llms.txt, docs)
4. Push tag: `git tag v{version} && git push origin v{version}`
5. GitHub Actions automatically:
   - Publishes to Maven Central
   - Generates Dokka API docs
   - Publishes MCP to npm
   - Creates GitHub Release
   - Builds sample APKs

## Post-Release Checklist

- [ ] Verify Maven Central artifacts are live
- [ ] Verify npm @sceneview/mcp updated
- [ ] Verify docs site updated
- [ ] Post release announcement (Discord, social media)
- [ ] Update comparison/benchmark pages
- [ ] Monitor for regression reports (48h)
- [ ] Close related GitHub issues

## Release Notes Template

```markdown
## {version} — {title}

### Highlights
- **Feature 1** — description
- **Feature 2** — description

### New APIs
- `NewNode` — description

### Fixes
- Fix description (#issue)

### Dependencies
- Dep X.Y.Z → A.B.C

### Migration
See [Migration Guide](MIGRATION.md) for upgrade steps.
```
