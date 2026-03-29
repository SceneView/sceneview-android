# /version-bump — Coordinated version update across all platforms

Bump the SceneView version across ALL locations in a single, atomic operation.

**Usage:** `/version-bump 3.6.0` or just `/version-bump` (will ask for version)

---

## Version Location Map (ALL files that contain the version)

### Source of truth
- `gradle.properties` → `VERSION_NAME=X.Y.Z`

### Android modules (must match root)
- `sceneview/gradle.properties` → `VERSION_NAME=`
- `arsceneview/gradle.properties` → `VERSION_NAME=`
- `sceneview-core/gradle.properties` → `VERSION_NAME=`

### npm packages
- `mcp/package.json` → `"version": "X.Y.Z"`
- `sceneview-web/package.json` → `"version": "X.Y.Z"` (if exists)

### Swift Package (uses git tags, not file version)
- `SceneViewSwift/` — version is the git tag `vX.Y.Z`

### Documentation
- `llms.txt` — artifact version references (`io.github.sceneview:sceneview:X.Y.Z`)
- `README.md` — install snippets
- `docs/docs/index.md` — install snippets, badges
- `docs/docs/quickstart.md` — dependency snippets

### Website (sceneview.github.io — separate repo)
- `index.html` — version badge if present
- Meta tags, JSON-LD schema version

### CLAUDE.md
- "Latest release" line in session continuity section

---

## Steps

### 1. Read current version
```bash
grep '^VERSION_NAME=' gradle.properties | cut -d= -f2
```

### 2. Ask for new version (if not provided as argument)
"Current version is X.Y.Z. What version do you want to bump to?"

### 3. Update ALL locations
Update every file listed above. Use `sed` or Edit tool. Be exhaustive.

### 4. Rebuild MCP dist
```bash
cd mcp && npm run prepare
```

### 5. Run sync-versions.sh
```bash
.claude/scripts/sync-versions.sh
```
If any mismatch remains, fix it before proceeding.

### 6. Verify with grep
```bash
# Should find NO references to old version
grep -rn "OLD_VERSION" gradle.properties */gradle.properties mcp/package.json llms.txt README.md
# Should find new version in all expected places
grep -rn "NEW_VERSION" gradle.properties */gradle.properties mcp/package.json llms.txt README.md
```

### 7. Commit
```bash
git add -A
git commit -m "chore: bump version to X.Y.Z"
```

### 8. Remind about website
"Don't forget to update the website (sceneview.github.io) if there are version references there."

---

**NEVER bump version in only one file. The whole point of this command is atomic, everywhere-at-once updates.**
