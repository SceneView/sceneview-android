# Session resume prompt — paste this into a new Claude Code session

Copy everything below the `---` line and paste it as your first message in a fresh Claude Code session opened at `~/Projects/sceneview/`.

---

## Context: session `crazy-lichterman` just completed — here's what shipped

Read these files first (they're already on main, just `git pull`):

```
.claude/handoff.md                                    # top = my session summary at line 37
.claude/NOTICE-2026-04-11-mcp-gateway-live.md         # Gateway #1 LIVE — 5 non-negotiables
.claude/plans/fuzzy-prancing-turing.md                 # Rerun integration architecture plan
CHANGELOG.md                                           # v4.0.0-rc.1 entry at top
```

### What shipped (session crazy-lichterman, 2026-04-11/12)

**SceneView ↔ Rerun.io integration — 5 phases, all merged on main:**

1. `rerun-3d-mcp@1.0.0` on npm (`npx rerun-3d-mcp`) — 5 MCP tools, 73 tests
2. Playground "AR Debug (Rerun)" example — iframe embed of Rerun Web Viewer
3. Android `arsceneview.ar.rerun.RerunBridge` + `rememberRerunBridge` composable — 16 JVM tests
4. Android demo "AR Debug (Rerun)" tile + Python sidecar `samples/android-demo/tools/rerun-bridge.py`
5. iOS `SceneViewSwift.RerunBridge` + new `ARSceneView.onFrame` hook — 12 Swift tests + xcodebuild OK

**Also shipped:**
- Version bump 3.6.2 → 4.0.0-rc.1 across 28 files
- Git tag `v4.0.0-rc.1` + GitHub pre-release
- `sceneview-mcp@4.0.0-rc.1` on npm `@next` tag (NOT `@latest`)
- `publishConfig: { tag: "next" }` safeguard in `mcp/package.json`
- llms.txt Rerun section (~150 lines) + migration guide 3.6.x → 4.0.0
- CHANGELOG v4.0.0-rc.1 entry
- 7 announcement drafts in `~/Projects/profile-private/announcements/` (LinkedIn × 2, Reddit × 4, HN × 1) — none published yet

### Current npm state

```
sceneview-mcp:  { latest: '3.6.5', beta: '4.0.0-beta.1', next: '4.0.0-rc.1' }
rerun-3d-mcp:  { latest: '1.0.0' }
```

**CRITICAL:** `@latest` is 3.6.5 (NOT 4.x). Protected by `publishConfig.tag=next`. Do NOT promote to 4.x until at least one real Stripe checkout completes end-to-end.

### Gateway state

MCP Gateway #1 is LIVE with real Stripe (4 `price_1TL6...` products, webhook `we_1TL7Hf...`). Hub-gateway #2 also scaffolded by Session B. All 4 checkout plans return `cs_live_*`. See NOTICE file for the 5 non-negotiables.

### Worktree cleanup needed

```bash
git branch -d claude/crazy-lichterman
git push origin --delete claude/crazy-lichterman
git worktree remove .claude/worktrees/crazy-lichterman
```

### What's NOT done yet — prioritized next steps

1. **First real paying customer** — blocker for promoting `@latest` to 4.x. Needs a human with a real card on https://sceneview-mcp.mcp-tools-lab.workers.dev/pricing
2. **Announce rerun-3d-mcp + v4.0.0-rc.1** — drafts ready in `~/Projects/profile-private/announcements/`. Recommended order: r/androiddev first (Tuesday-Thursday 9am PT), then r/iOSProgramming + LinkedIn, then HN. One per day max.
3. **Promote to v4.0.0 stable** — after (1) is done: bump gradle.properties → 4.0.0, sync-versions, strict-semver tag `v4.0.0` (triggers Maven Central + SPM), `npm dist-tag add sceneview-mcp@4.0.0 latest`
4. **Filament 1.71.0 bump** — parked in #800, needs .filamat recompile (dedicated session)
5. **Render tests re-enable** — 4 test classes `@Ignore`'d (#803), root cause = SwiftShader JNI crashes

### 101 new tests added this session

- 16 JVM: `arsceneview` RerunWireFormatTest (12) + RerunBridgeTest (4)
- 12 Swift: `SceneViewSwiftTests` RerunWireFormatTests
- 73 vitest: `mcp/packages/rerun` (6 test suites)

Cross-platform wire format parity: 24 golden tests (12 Kotlin + 12 Swift) with character-identical expected JSON strings.
