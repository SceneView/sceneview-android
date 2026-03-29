# /publish-check — Verify all published artifacts are up to date

Check every platform where SceneView is published and compare with local version.

---

## Published Artifact Registry

| Artifact | Platform | Install command | Check URL |
|---|---|---|---|
| sceneview | Maven Central | `implementation("io.github.sceneview:sceneview:X.Y.Z")` | Maven search API |
| arsceneview | Maven Central | `implementation("io.github.sceneview:arsceneview:X.Y.Z")` | Maven search API |
| sceneview-core | Maven Central | KMP shared module | Maven search API |
| sceneview-mcp | npm | `npx sceneview-mcp` | npm registry API |
| SceneViewSwift | SPM (GitHub tags) | `.package(url: "...", from: "X.Y.Z")` | git tags |
| sceneview.js | npm | `npm i sceneview` | npm registry API |
| GitHub Release | GitHub | Download page | gh CLI |
| Website | GitHub Pages | sceneview.github.io | curl |

## Checks to run

### 1. Maven Central
```bash
echo "=== Maven Central ==="
for artifact in sceneview arsceneview sceneview-core; do
  V=$(curl -s "https://search.maven.org/solrsearch/select?q=g:io.github.sceneview+AND+a:$artifact&rows=1&wt=json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['response']['docs'][0]['latestVersion'] if d['response']['docs'] else 'NOT FOUND')" 2>/dev/null)
  echo "  $artifact: $V"
done
```

### 2. npm
```bash
echo "=== npm ==="
for pkg in sceneview-mcp sceneview; do
  V=$(curl -s "https://registry.npmjs.org/$pkg/latest" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','NOT FOUND'))" 2>/dev/null)
  echo "  $pkg: $V"
done
```

### 3. GitHub Release & Tags
```bash
echo "=== GitHub ==="
LATEST_TAG=$(git tag -l 'v*' | sort -V | tail -1)
echo "  Latest tag: $LATEST_TAG"
gh release view "$LATEST_TAG" --json tagName,publishedAt 2>/dev/null || echo "  No GitHub release for $LATEST_TAG"
```

### 4. Local version
```bash
echo "=== Local ==="
LOCAL_V=$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)
echo "  gradle.properties: $LOCAL_V"
```

### 5. Compare and report

Print a summary table:

| Artifact | Published | Local | Status |
|---|---|---|---|
| sceneview (Maven) | X.Y.Z | X.Y.Z | OK / BEHIND / AHEAD |
| arsceneview (Maven) | X.Y.Z | X.Y.Z | OK / BEHIND / AHEAD |
| sceneview-mcp (npm) | X.Y.Z | X.Y.Z | OK / BEHIND / AHEAD |
| SceneViewSwift (tag) | vX.Y.Z | X.Y.Z | OK / BEHIND / AHEAD |
| GitHub Release | vX.Y.Z | X.Y.Z | OK / BEHIND / AHEAD |

### 6. Actionable recommendations

For each artifact that is BEHIND:
- "Run `/release` to publish version X.Y.Z"
- "Run `cd mcp && npm publish` to push MCP to npm"
- "Create a GitHub Release for tag vX.Y.Z"
- "Push a new git tag for SPM users"

**NEVER say "everything is published" without actually checking the APIs above.**
