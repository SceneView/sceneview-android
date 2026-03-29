#!/usr/bin/env bash
# release-checklist.sh — Pre-release validation
#
# Checks everything that must be true before tagging a release.
# Exit code 0 = ready to release, non-zero = blockers found.
#
# Usage:
#   ./release-checklist.sh [version]
#   Example: ./release-checklist.sh 3.6.0

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

TARGET_VERSION="${1:-$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)}"
BLOCKERS=0
WARNINGS=0

echo -e "${CYAN}=== SceneView Release Checklist ===${NC}"
echo -e "Target version: ${GREEN}$TARGET_VERSION${NC}"
echo ""

check() {
    local name="$1"
    local status="$2" # PASS, FAIL, WARN
    local detail="$3"

    case "$status" in
        PASS) printf "  ${GREEN}[PASS]${NC}  %-45s %s\n" "$name" "$detail" ;;
        FAIL) printf "  ${RED}[FAIL]${NC}  %-45s %s\n" "$name" "$detail"; BLOCKERS=$((BLOCKERS + 1)) ;;
        WARN) printf "  ${YELLOW}[WARN]${NC}  %-45s %s\n" "$name" "$detail"; WARNINGS=$((WARNINGS + 1)) ;;
    esac
}

# ─── 1. Version alignment ─────────────────────────────────────────────────
echo -e "${CYAN}--- Version Alignment ---${NC}"

ROOT_V=$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)
[ "$ROOT_V" = "$TARGET_VERSION" ] && check "gradle.properties" "PASS" "$ROOT_V" || check "gradle.properties" "FAIL" "Expected $TARGET_VERSION, got $ROOT_V"

for module in sceneview arsceneview sceneview-core; do
    PROPS="$module/gradle.properties"
    if [ -f "$PROPS" ]; then
        V=$(grep '^VERSION_NAME=' "$PROPS" | cut -d= -f2 || echo "MISSING")
        [ "$V" = "$TARGET_VERSION" ] && check "$PROPS" "PASS" "$V" || check "$PROPS" "FAIL" "Expected $TARGET_VERSION, got $V"
    fi
done

MCP_V=$(python3 -c "import json; print(json.load(open('mcp/package.json'))['version'])" 2>/dev/null || echo "MISSING")
[ "$MCP_V" = "$TARGET_VERSION" ] && check "mcp/package.json" "PASS" "$MCP_V" || check "mcp/package.json" "WARN" "Expected $TARGET_VERSION, got $MCP_V (MCP may have own version)"

LLMS_V=$(grep -m1 'io\.github\.sceneview:sceneview:' llms.txt | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
[ "$LLMS_V" = "$TARGET_VERSION" ] && check "llms.txt" "PASS" "$LLMS_V" || check "llms.txt" "FAIL" "Expected $TARGET_VERSION, got $LLMS_V"

echo ""

# ─── 2. CHANGELOG ─────────────────────────────────────────────────────────
echo -e "${CYAN}--- CHANGELOG ---${NC}"

if [ -f "CHANGELOG.md" ]; then
    CL_V=$(grep -m1 '^## ' CHANGELOG.md | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    [ "$CL_V" = "$TARGET_VERSION" ] && check "CHANGELOG has $TARGET_VERSION entry" "PASS" "" || check "CHANGELOG has $TARGET_VERSION entry" "FAIL" "Latest entry is $CL_V"
else
    check "CHANGELOG.md exists" "FAIL" "File not found"
fi

echo ""

# ─── 3. Git state ─────────────────────────────────────────────────────────
echo -e "${CYAN}--- Git State ---${NC}"

DIRTY=$(git status --porcelain | grep -v '??' | wc -l | tr -d ' ')
[ "$DIRTY" -eq 0 ] && check "Working tree clean" "PASS" "" || check "Working tree clean" "FAIL" "$DIRTY uncommitted changes"

BRANCH=$(git branch --show-current)
check "Current branch" "PASS" "$BRANCH"

TAG_EXISTS=$(git tag -l "v$TARGET_VERSION" | wc -l | tr -d ' ')
[ "$TAG_EXISTS" -eq 0 ] && check "Tag v$TARGET_VERSION not yet created" "PASS" "" || check "Tag v$TARGET_VERSION already exists" "WARN" "Tag exists — re-tagging will require force"

echo ""

# ─── 4. Build check ───────────────────────────────────────────────────────
echo -e "${CYAN}--- Build Check ---${NC}"

if [ -f "gradlew" ]; then
    echo -e "  Running: ./gradlew assembleDebug (this may take a few minutes)..."
    if ./gradlew assembleDebug --quiet 2>/dev/null; then
        check "Android assembleDebug" "PASS" ""
    else
        check "Android assembleDebug" "FAIL" "Build failed"
    fi
else
    check "Gradle wrapper" "FAIL" "gradlew not found"
fi

echo ""

# ─── 5. Tests ──────────────────────────────────────────────────────────────
echo -e "${CYAN}--- Tests ---${NC}"

if [ -d "mcp" ] && [ -f "mcp/package.json" ]; then
    echo -e "  Running: MCP tests..."
    if (cd mcp && npm test --silent 2>/dev/null); then
        check "MCP tests" "PASS" ""
    else
        check "MCP tests" "FAIL" "Tests failed"
    fi
fi

echo ""

# ─── 6. Security ──────────────────────────────────────────────────────────
echo -e "${CYAN}--- Security ---${NC}"

SECRETS_FOUND=0
for pattern in ".env" "credentials.json" "keystore.jks" "google-services.json" "local.properties"; do
    TRACKED=$(git ls-files "$pattern" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$TRACKED" -gt 0 ]; then
        check "No tracked $pattern" "FAIL" "Found in git index!"
        SECRETS_FOUND=$((SECRETS_FOUND + 1))
    fi
done
[ "$SECRETS_FOUND" -eq 0 ] && check "No secrets in tracked files" "PASS" ""

# Check for API keys in source
API_KEY_HITS=$(grep -rn "AIza\|sk-\|AKIA\|ghp_\|npm_" --include="*.kt" --include="*.swift" --include="*.ts" --include="*.js" \
    sceneview/ arsceneview/ SceneViewSwift/ mcp/src/ 2>/dev/null | grep -v "node_modules" | wc -l | tr -d ' ')
[ "$API_KEY_HITS" -eq 0 ] && check "No hardcoded API keys in source" "PASS" "" || check "No hardcoded API keys in source" "FAIL" "$API_KEY_HITS potential key(s) found"

echo ""

# ─── 7. MCP dist freshness ────────────────────────────────────────────────
echo -e "${CYAN}--- MCP Dist Freshness ---${NC}"

if [ -f "mcp/src/index.ts" ] && [ -f "mcp/dist/index.js" ]; then
    SRC_V=$(grep -m1 'version:' mcp/src/index.ts | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    DIST_V=$(grep -m1 'version:' mcp/dist/index.js | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    [ "$SRC_V" = "$DIST_V" ] && check "MCP src/dist version match" "PASS" "$SRC_V" || check "MCP src/dist version match" "FAIL" "src=$SRC_V dist=$DIST_V — run 'npm run prepare'"
else
    check "MCP dist exists" "WARN" "src or dist not found"
fi

echo ""

# ─── 8. Documentation ─────────────────────────────────────────────────────
echo -e "${CYAN}--- Documentation ---${NC}"

[ -f "llms.txt" ] && check "llms.txt exists" "PASS" "" || check "llms.txt exists" "FAIL" ""
[ -f "README.md" ] && check "README.md exists" "PASS" "" || check "README.md exists" "FAIL" ""
[ -f "CLAUDE.md" ] && check "CLAUDE.md exists" "PASS" "" || check "CLAUDE.md exists" "FAIL" ""
[ -f "CHANGELOG.md" ] && check "CHANGELOG.md exists" "PASS" "" || check "CHANGELOG.md exists" "FAIL" ""

echo ""

# ─── Summary ───────────────────────────────────────────────────────────────
echo -e "${CYAN}=== Release Readiness Summary ===${NC}"
echo ""

if [ "$BLOCKERS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}READY TO RELEASE v$TARGET_VERSION${NC}"
    echo "  Next steps:"
    echo "    git tag v$TARGET_VERSION"
    echo "    git push origin main --tags"
    exit 0
elif [ "$BLOCKERS" -eq 0 ]; then
    echo -e "${YELLOW}RELEASE POSSIBLE with $WARNINGS warning(s)${NC}"
    echo "  Review warnings above before proceeding."
    exit 0
else
    echo -e "${RED}NOT READY — $BLOCKERS blocker(s), $WARNINGS warning(s)${NC}"
    echo "  Fix all FAIL items before releasing."
    exit 1
fi
