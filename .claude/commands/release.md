# /release — SceneView release workflow

Guided workflow to bump version, update all references, and prepare a release.

Ask the user: "What version are we releasing? (current: check root gradle.properties)"

---

## Step 1: Bump version everywhere

Update VERSION_NAME in all these files to the new version:
1. `gradle.properties` (root — source of truth)
2. `sceneview/gradle.properties`
3. `arsceneview/gradle.properties`
4. `sceneview-core/gradle.properties`
5. `mcp/package.json` (version field)

## Step 2: Update documentation versions

Replace the old version string with the new one in:
1. `llms.txt` (Android artifact versions near top)
2. `docs/docs/index.md` (install snippets, badge)
3. `docs/docs/quickstart.md` (dependency snippet)
4. `README.md` (install snippets)

## Step 3: Update CHANGELOG.md

Add a new section at the top of CHANGELOG.md with:
- Version number and short title
- Categorized changes: New, Improved, Fixed, Breaking (if any)
- Pull from recent git log: `git log <last-tag>..HEAD --oneline`

## Step 4: Rebuild MCP

```bash
cd mcp && npm run prepare && npm test
```

Verify dist/ files are updated and tests pass.

## Step 5: Update CLAUDE.md

Update the "Current state" section:
- Date to today
- Active branch
- Summary of what changed

## Step 6: Run /sync-check

Run the sync-check command to verify everything is aligned.

## Step 7: Commit and tag

```bash
git add -A
git commit -m "chore: release <version>"
git tag v<version>
```

## Step 8: Create PR or push

Ask the user: "Push to main and create a GitHub release, or create a PR first?"

If PR: `gh pr create --title "chore: release <version>"`
If direct: `git push origin main --tags && gh release create v<version> --generate-notes`

## Step 9: Publish MCP to npm (optional)

Ask the user: "Publish MCP server to npm?"
If yes: `cd mcp && npm publish`

---

**Important:** Never skip the /sync-check step. Version drift is the #1 source of bugs in this repo.
