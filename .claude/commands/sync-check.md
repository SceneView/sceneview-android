# /sync-check — Verify SceneView repo synchronization

Run this **before every PR**, at the **end of every session**, and whenever the user asks "where are we" or "is everything ok".

**CRITICAL RULE**: Never tell the user "everything is synced" or "we're good" without running ALL checks below first. Local file versions mean NOTHING if the packages aren't published.

---

## 1. Published packages vs local (MOST CRITICAL)

Check what's actually published and available to developers RIGHT NOW:

```bash
# Maven Central — what devs actually get with implementation("io.github.sceneview:sceneview:X.Y.Z")
curl -s "https://search.maven.org/solrsearch/select?q=g:io.github.sceneview+AND+a:sceneview&rows=1&wt=json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['response']['docs'][0]['latestVersion'])"

curl -s "https://search.maven.org/solrsearch/select?q=g:io.github.sceneview+AND+a:arsceneview&rows=1&wt=json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['response']['docs'][0]['latestVersion'])"

# npm — what devs get with npx sceneview-mcp
curl -s "https://registry.npmjs.org/sceneview-mcp/latest" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version','NOT FOUND'))"

# GitHub tags — what SPM users get
git tag -l 'v*' | sort -V | tail -1
```

Compare each published version with the local VERSION_NAME. If they don't match:
- **ALERT**: "⚠️ Version X.Y.Z is NOT published. Developers cannot install it."
- Check if a release workflow ran: `gh run list --workflow=release.yml --limit 3`
- Check if it succeeded or failed

**This check is a hard blocker. Do NOT skip it.**

## 2. Local version alignment

Check that VERSION_NAME is identical across all modules:
- `gradle.properties` (root — source of truth)
- `sceneview/gradle.properties`
- `arsceneview/gradle.properties`
- `sceneview-core/gradle.properties`
- `mcp/package.json` (version field)

Then check that docs and references match:
- `llms.txt` (Android artifact versions near top of file)
- `docs/docs/index.md`
- `docs/docs/quickstart.md`
- `README.md`

Report any mismatches with: file, current value, expected value.

## 3. Release workflow health

```bash
gh run list --workflow=release.yml --limit 5
```

- Did the last release succeed?
- If it failed, what job failed? (Maven Central, npm, Dokka, GitHub Release)
- Is there a pending tag that wasn't released?

## 4. MCP dist freshness

Compare version strings between `mcp/src/index.ts` and `mcp/dist/index.js`.
If versions differ, run `cd mcp && npm run prepare` and report what changed.

## 5. llms.txt vs Swift source

For each Swift file in `SceneViewSwift/Sources/SceneViewSwift/`:
- Check that the node/struct is documented in the iOS section of `llms.txt`
- Verify the documented signature matches the actual public API

Report any undocumented nodes or signature mismatches.

## 6. llms.txt vs Kotlin source

Check if any new `@Composable` functions or public node classes were added since the last tag:
```bash
LAST_TAG=$(git tag -l 'v*' | sort -V | tail -1)
git diff $LAST_TAG..HEAD -- sceneview/src/ arsceneview/src/ | grep "^+.*fun \|^+.*@Composable\|^+.*class.*Node"
```
If new APIs exist, flag them as needing llms.txt documentation.

## 7. Docs site deployment

- Check if docs workflow succeeded: `gh run list --workflow=docs.yml --limit 3`
- Is the site accessible? Check https://sceneview.github.io/
- Do the versions on the live site match the current version?

## 8. CLAUDE.md freshness

- Is the "Active branch" correct? (compare with `git branch --show-current`)
- Is the "last updated" date today or recent?
- Does the "Current state" summary match reality?

## 9. Build artifacts check

- Are there any tracked build artifacts? `git ls-files -- '*.o' '*.pcm' '*.swiftmodule' '*.class' 'build.db'`
- Are `SceneViewSwift/.build/`, `docs/.cache/`, `docs/site/` in `.gitignore`?

## 10. Stale branches

```bash
git branch -a --no-merged main
```
Flag any branches older than 7 days that haven't been merged.

## 11. Summary

Print a table:

| Check | Status | Details |
|---|---|---|
| Maven Central published | OK/FAIL | published: X.Y.Z, local: X.Y.Z |
| npm published | OK/FAIL | published: X.Y.Z, local: X.Y.Z |
| SPM tag exists | OK/FAIL | latest tag: vX.Y.Z |
| Release workflow | OK/FAIL | last run: success/failure |
| Local versions aligned | OK/FAIL | ... |
| MCP dist fresh | OK/FAIL | ... |
| llms.txt iOS | OK/FAIL | ... |
| llms.txt Android | OK/FAIL | ... |
| Docs site deployed | OK/FAIL | ... |
| CLAUDE.md | OK/FAIL | ... |
| No build artifacts | OK/FAIL | ... |
| No stale branches | OK/FAIL | ... |

**If ANY check fails, do NOT tell the user "everything is good". List every failure clearly and propose fixes.**
