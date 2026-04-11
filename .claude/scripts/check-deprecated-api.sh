#!/usr/bin/env bash
#
# check-deprecated-api.sh — block commits that reintroduce deprecated
# SceneView API names (Scene{}, ARScene{}) outside the historical whitelist.
#
# The rename from Scene/ARScene to SceneView/ARSceneView landed in v3.6
# (commit e6a26a06) for cross-platform consistency with SceneViewSwift.
# The old names are kept as @Deprecated aliases but must NOT appear in
# new code, KDoc, docs, or MCP-generated templates, otherwise AI clients
# reading the codebase will generate code against the deprecated API.
#
# This check runs on staged files (before commit) and scans for the
# deprecated patterns. It excludes files that must legitimately reference
# the old names: migration guides, changelog, the deprecated alias
# definitions themselves, the Filament framework class, SwiftUI's
# unrelated `Scene` protocol, and the MCP validators/tests that intentionally
# pattern-match the old name as a detector.
#
# Usage:
#   bash .claude/scripts/check-deprecated-api.sh          # check staged files
#   bash .claude/scripts/check-deprecated-api.sh --all    # check entire tree
#
# Exit code: 0 if clean, 1 if violations found.

set -u

ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$ROOT" ]; then
  echo "Not inside a git repo." >&2
  exit 0
fi
cd "$ROOT"

# ─── Files that may legitimately mention Scene{}/ARScene{} ──────────────────
#
# These are either historical docs (migration guides, changelog, roadmap),
# the deprecated alias definitions themselves, KDoc that references the
# Filament framework `Scene` class (not the composable), SwiftUI's own
# `Scene` protocol, or validator files that pattern-match Scene{} as a
# detector.

is_whitelisted() {
  case "$1" in
    # Historical migration / changelog docs
    MIGRATION.md | CHANGELOG.md | ROADMAP.md) return 0 ;;
    docs/docs/migration.md | docs/docs/migration-v4.md) return 0 ;;
    docs/docs/changelog.md | docs/docs/comparison.md) return 0 ;;
    docs/v3.6.0-roadmap.md | docs/ios-swift-package-design.md) return 0 ;;
    # nodes.md line 149 legitimately mentions the Filament class Scene;
    # we allow the whole file since the composable refs were already fixed
    docs/docs/nodes.md) return 0 ;;

    # The deprecated alias definitions themselves
    sceneview/src/main/java/io/github/sceneview/Scene.kt) return 0 ;;
    arsceneview/src/main/java/io/github/sceneview/ar/ARScene.kt) return 0 ;;

    # Filament framework class bindings
    sceneview-web/src/jsMain/kotlin/io/github/sceneview/web/bindings/Filament.kt) return 0 ;;

    # SwiftUI.Scene is a protocol unrelated to SceneView
    samples/ios-demo/SceneViewDemo/SceneViewDemoApp.swift) return 0 ;;
    SceneViewSwift/Examples/SceneViewDemo/SceneViewDemo/SceneViewDemoApp.swift) return 0 ;;

    # Session notes, historical reports — not user-facing
    .claude/branding.md | .claude/handoff.md | .claude/website-review-report.md) return 0 ;;
    .claude/active-branches.md) return 0 ;;
    .claude/reports/*.md) return 0 ;;
    .claude/plans/*.md) return 0 ;;

    # The detector script documents the patterns it matches; the
    # quality-gate workflow file has an explanatory header comment
    # that necessarily contains "Scene{}/ARScene{}" as prose.
    .claude/scripts/check-deprecated-api.sh) return 0 ;;
    .github/workflows/quality-gate.yml) return 0 ;;

    # Demo recordings — historical marketing artifacts
    mcp/demo/sceneview-3.0-linkedin-video.html) return 0 ;;
    mcp/demo/sceneview-mcp-linkedin-video.html) return 0 ;;
    mcp/demo/sceneview-mcp-linkedin-video-capture.html) return 0 ;;
    mcp/demo/record-terminal.sh) return 0 ;;
  esac

  # MCP validators intentionally match the deprecated pattern to flag it
  case "$1" in
    mcp/src/validator.ts | mcp/src/validator.test.ts) return 0 ;;
    mcp/packages/interior/src/validator.ts | mcp/packages/interior/src/validator.test.ts) return 0 ;;
    mcp/packages/gaming/src/validator.ts | mcp/packages/gaming/src/validator.test.ts) return 0 ;;
    mcp/packages/healthcare/src/validator.ts | mcp/packages/healthcare/src/validator.test.ts) return 0 ;;
    mcp/packages/automotive/src/validator.ts | mcp/packages/automotive/src/validator.test.ts) return 0 ;;
    mcp-interior/src/validator.ts | mcp-interior/src/validator.test.ts) return 0 ;;
    mcp-gaming/src/validator.ts | mcp-gaming/src/validator.test.ts) return 0 ;;
  esac

  # Generated / vendored
  case "$1" in
    */node_modules/*) return 0 ;;
    */build/*) return 0 ;;
    */dist/*) return 0 ;;
    */generated/*) return 0 ;;
  esac

  return 1
}

# ─── File extensions we scan ────────────────────────────────────────────────

is_scannable() {
  case "$1" in
    *.kt | *.java | *.swift | *.dart) return 0 ;;
    *.ts | *.tsx | *.js | *.jsx) return 0 ;;
    *.md | *.txt | *.html | *.yml | *.yaml | *.json) return 0 ;;
    *.svg) return 0 ;;
  esac
  return 1
}

# ─── Select files to scan ───────────────────────────────────────────────────

if [ "${1:-}" = "--all" ]; then
  FILES=$(git ls-files)
  MODE="all tracked files"
else
  FILES=$(git diff --cached --name-only --diff-filter=ACMR)
  MODE="staged files"
fi

if [ -z "$FILES" ]; then
  # Nothing staged — OK
  exit 0
fi

# ─── Scan ───────────────────────────────────────────────────────────────────
#
# Pattern: a word boundary followed by "Scene" or "ARScene", optional whitespace,
# then { or (. The negative lookbehind (?<![\w.]) is not portable across greps,
# so we use a pre-filter in awk instead.
#
# We need to skip:
# - SceneView, ARSceneView, SceneScope, ARSceneScope, SceneNodeManager, SceneRenderer,
#   SceneEnvironment, SceneEntities (substrings that start with "Scene")
# - XRScene, FilamentScene (substrings that end with "Scene")
# - SwiftUI.Scene (followed by a dot, not a brace)
# - Filament.Scene, rememberScene (same)

VIOLATIONS=0
VIOLATION_LINES=""

for file in $FILES; do
  [ -f "$file" ] || continue
  is_scannable "$file" || continue
  is_whitelisted "$file" && continue

  # Pipe through awk to get line numbers for Scene{/Scene(/ARScene{/ARScene(
  # with word-boundary + not-preceded-by-word-char semantics.
  matches=$(awk '
    {
      line = $0
      # Remove string contents that contain legitimate escaped Scene refs?
      # For now, match anything. False positives are rare — most legitimate
      # refs are in the whitelist above.
      if (match(line, /(^|[^a-zA-Z0-9_.])Scene[[:space:]]*[{(]/)) {
        print FILENAME ":" NR ":" line
      } else if (match(line, /(^|[^a-zA-Z0-9_.])ARScene[[:space:]]*[{(]/)) {
        print FILENAME ":" NR ":" line
      }
    }
  ' FILENAME="$file" "$file" 2>/dev/null)

  if [ -n "$matches" ]; then
    VIOLATIONS=$((VIOLATIONS + 1))
    VIOLATION_LINES="${VIOLATION_LINES}${matches}
"
  fi
done

if [ "$VIOLATIONS" -gt 0 ]; then
  echo ""
  echo "  ╭──────────────────────────────────────────────────────────────────╮"
  echo "  │  DEPRECATED API DETECTED                                         │"
  echo "  ├──────────────────────────────────────────────────────────────────┤"
  echo "  │  \`Scene { }\` and \`ARScene { }\` were renamed to \`SceneView { }\`   │"
  echo "  │  and \`ARSceneView { }\` in v3.6 (commit e6a26a06).                │"
  echo "  │                                                                  │"
  echo "  │  The old names are kept as @Deprecated aliases but must NOT     │"
  echo "  │  appear in new code, KDoc, or MCP-generated templates.          │"
  echo "  │                                                                  │"
  echo "  │  Fix:                                                            │"
  echo "  │    \`Scene(\`    ->  \`SceneView(\`                                  │"
  echo "  │    \`Scene {\`    ->  \`SceneView {\`                                │"
  echo "  │    \`ARScene(\`  ->  \`ARSceneView(\`                                │"
  echo "  │    \`ARScene {\`  ->  \`ARSceneView {\`                              │"
  echo "  │                                                                  │"
  echo "  │  If the reference is historical (migration doc, changelog,      │"
  echo "  │  deprecated alias body, Filament framework class, SwiftUI's     │"
  echo "  │  own Scene protocol, or a validator pattern), add the file to   │"
  echo "  │  the whitelist in .claude/scripts/check-deprecated-api.sh.       │"
  echo "  ╰──────────────────────────────────────────────────────────────────╯"
  echo ""
  echo "  $MODE with deprecated refs ($VIOLATIONS file$([ "$VIOLATIONS" -gt 1 ] && echo 's')):"
  echo ""
  printf "%s" "$VIOLATION_LINES" | while IFS= read -r line; do
    [ -z "$line" ] && continue
    echo "    $line"
  done
  echo ""
  exit 1
fi

exit 0
