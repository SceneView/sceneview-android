# /sync-check — Verify SceneView repo synchronization

Run this before every PR or at the end of a session to catch drift between the different areas of the repo.

---

## 1. Version alignment

Check that VERSION_NAME is identical across all modules:
- `gradle.properties` (root)
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

## 2. MCP dist freshness

Compare timestamps and version strings between `mcp/src/index.ts` and `mcp/dist/index.js`.
If the source is newer or versions differ, run `cd mcp && npm run prepare` and report what changed.

## 3. llms.txt vs Swift source

For each Swift file in `SceneViewSwift/Sources/SceneViewSwift/`:
- Check that the node/struct is documented in the iOS section of `llms.txt`
- Verify the documented signature matches the actual public API

Report any undocumented nodes or signature mismatches.

## 4. llms.txt vs Kotlin source

Check if any new `@Composable` functions or public node classes were added since the last tag:
```bash
git diff v3.2.0..HEAD -- sceneview/src/ arsceneview/src/ | grep "^+.*fun \|^+.*@Composable\|^+.*class.*Node"
```
If new APIs exist, flag them as needing llms.txt documentation.

## 5. CLAUDE.md freshness

- Is the "Active branch" correct? (compare with `git branch --show-current`)
- Is the "last updated" date today or recent?
- Does the "Current state" summary match reality?

## 6. Build artifacts check

- Are there any tracked build artifacts? `git ls-files -- '*.o' '*.pcm' '*.swiftmodule' '*.class' 'build.db'`
- Are `SceneViewSwift/.build/`, `docs/.cache/`, `docs/site/` in `.gitignore`?

## 7. Summary

Print a table:

| Check | Status | Details |
|---|---|---|
| Versions aligned | OK/FAIL | ... |
| MCP dist fresh | OK/FAIL | ... |
| llms.txt iOS | OK/FAIL | ... |
| llms.txt Android | OK/FAIL | ... |
| CLAUDE.md | OK/FAIL | ... |
| No build artifacts | OK/FAIL | ... |

If all checks pass, print: "All sync checks passed."
If any fail, list the fixes needed.
