#!/usr/bin/env bash
# quality-gate.sh — Comprehensive pre-push quality gate
#
# Runs ALL checks before pushing to the repository.
# Blocks push if any critical check fails.
#
# Usage:
#   ./quality-gate.sh              # Full check
#   ./quality-gate.sh --quick      # Skip build/test (for fast checks only)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

QUICK_MODE="${1:-}"
BLOCKERS=0
WARNINGS=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}=== SceneView Quality Gate ===${NC}"
echo ""

check() {
    local name="$1"
    local status="$2"
    local detail="${3:-}"

    case "$status" in
        PASS) printf "  ${GREEN}[PASS]${NC}  %-50s %s\n" "$name" "$detail" ;;
        FAIL) printf "  ${RED}[FAIL]${NC}  %-50s %s\n" "$name" "$detail"; BLOCKERS=$((BLOCKERS + 1)) ;;
        WARN) printf "  ${YELLOW}[WARN]${NC}  %-50s %s\n" "$name" "$detail"; WARNINGS=$((WARNINGS + 1)) ;;
    esac
}

# ─── 1. Git State ──────────────────────────────────────────────────────
echo -e "${CYAN}--- Git State ---${NC}"

BRANCH=$(git branch --show-current)
check "Current branch" "PASS" "$BRANCH"

# Check for merge conflicts
CONFLICT_FILES=$(git diff --name-only --diff-filter=U 2>/dev/null | wc -l | tr -d ' ')
[ "$CONFLICT_FILES" -eq 0 ] && check "No merge conflicts" "PASS" "" || check "No merge conflicts" "FAIL" "$CONFLICT_FILES files"

# Check for large files being committed
LARGE_FILES=$(git diff --cached --name-only 2>/dev/null | while read f; do
    [ -f "$f" ] && SIZE=$(wc -c < "$f" 2>/dev/null | tr -d ' ') && [ "$SIZE" -gt 10485760 ] && echo "$f"
done | wc -l | tr -d ' ')
[ "$LARGE_FILES" -eq 0 ] && check "No large files (>10MB) staged" "PASS" "" || check "No large files staged" "WARN" "$LARGE_FILES files"

echo ""

# ─── 2. Version Sync ─────────────────────────────────────────────────
echo -e "${CYAN}--- Version Sync ---${NC}"

SOURCE_VERSION=$(grep '^VERSION_NAME=' gradle.properties | cut -d= -f2)
check "Source version" "PASS" "$SOURCE_VERSION"

for module in sceneview arsceneview sceneview-core; do
    PROPS="$module/gradle.properties"
    if [ -f "$PROPS" ]; then
        V=$(grep '^VERSION_NAME=' "$PROPS" | cut -d= -f2 || echo "MISSING")
        [ "$V" = "$SOURCE_VERSION" ] && check "$PROPS" "PASS" "$V" || check "$PROPS" "FAIL" "Expected $SOURCE_VERSION, got $V"
    fi
done

LLMS_V=$(grep -m1 'io\.github\.sceneview:sceneview:' llms.txt | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
[ "$LLMS_V" = "$SOURCE_VERSION" ] && check "llms.txt version" "PASS" "$LLMS_V" || check "llms.txt version" "FAIL" "Expected $SOURCE_VERSION, got $LLMS_V"

README_V=$(grep -m1 'io\.github\.sceneview:sceneview:' README.md | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
[ "$README_V" = "$SOURCE_VERSION" ] && check "README.md version" "PASS" "$README_V" || check "README.md version" "FAIL" "Expected $SOURCE_VERSION, got $README_V"

echo ""

# ─── 3. Security ──────────────────────────────────────────────────────
echo -e "${CYAN}--- Security ---${NC}"

# Check for secrets in tracked files
for pattern in ".env" "credentials.json" "keystore.jks" "google-services.json"; do
    TRACKED=$(git ls-files "$pattern" 2>/dev/null | wc -l | tr -d ' ')
    [ "$TRACKED" -gt 0 ] && check "No tracked $pattern" "FAIL" "SECURITY RISK" || true
done

# local.properties should not be tracked
LP_TRACKED=$(git ls-files "local.properties" 2>/dev/null | wc -l | tr -d ' ')
[ "$LP_TRACKED" -eq 0 ] && check "local.properties not tracked" "PASS" "" || check "local.properties tracked" "FAIL" "Contains local paths/secrets"

# Check for API keys in staged changes
STAGED_KEYS=$(git diff --cached 2>/dev/null | grep -c "AIza\|sk-\|AKIA\|ghp_\|npm_\|PRIVATE_KEY" 2>/dev/null || true)
STAGED_KEYS=$(echo "$STAGED_KEYS" | tr -d '[:space:]' | head -c 10)
[ -z "$STAGED_KEYS" ] && STAGED_KEYS=0
[ "$STAGED_KEYS" -eq 0 ] 2>/dev/null && check "No API keys in staged changes" "PASS" "" || check "API keys in staged changes" "FAIL" "$STAGED_KEYS matches"

echo ""

# ─── 4. Code Quality ────────────────────────────────────────────────
echo -e "${CYAN}--- Code Quality ---${NC}"

# Check for common issues in changed files
CHANGED_KT=$(git diff --name-only HEAD 2>/dev/null | grep '\.kt$' || true)
if [ -n "$CHANGED_KT" ]; then
    # Check for !! (force unwrap) in changed Kotlin files
    FORCE_UNWRAP=0
    while IFS= read -r f; do
        if [ -f "$f" ]; then
            COUNT=$(grep -c '!!' "$f" 2>/dev/null || echo "0")
            FORCE_UNWRAP=$((FORCE_UNWRAP + COUNT))
        fi
    done <<< "$CHANGED_KT"
    [ "$FORCE_UNWRAP" -eq 0 ] && check "No force unwrap (!!) in Kotlin" "PASS" "" || check "Force unwrap (!!) in Kotlin" "WARN" "$FORCE_UNWRAP occurrences"

    # Check for Dispatchers.IO with Filament calls
    BGTHREAD=$(git diff HEAD 2>/dev/null | grep "+.*Dispatchers\.IO" | grep -c "modelLoader\|materialLoader\|createModel\|createMaterial" || echo "0")
    [ "$BGTHREAD" -eq 0 ] && check "No Filament calls on background thread" "PASS" "" || check "Filament on background thread" "FAIL" "THREADING VIOLATION"
fi

# Check for TODO/FIXME in staged changes
TODOS=$(git diff --cached 2>/dev/null | grep "^+" | grep -c "TODO\|FIXME\|HACK\|XXX" 2>/dev/null || true)
TODOS=$(echo "$TODOS" | tr -d '[:space:]' | head -c 10)
[ -z "$TODOS" ] && TODOS=0
[ "$TODOS" -eq 0 ] 2>/dev/null && check "No new TODO/FIXME in staged changes" "PASS" "" || check "New TODO/FIXME" "WARN" "$TODOS occurrences"

echo ""

# ─── 5. Build & Test (skip in quick mode) ────────────────────────────
if [ "$QUICK_MODE" != "--quick" ]; then
    echo -e "${CYAN}--- Build ---${NC}"

    if [ -f "gradlew" ]; then
        echo -e "  Building Android debug..."
        if ./gradlew assembleDebug --quiet 2>/dev/null; then
            check "Android assembleDebug" "PASS" ""
        else
            check "Android assembleDebug" "FAIL" "Build failed"
        fi
    fi

    echo ""
    echo -e "${CYAN}--- Tests ---${NC}"

    if [ -f "gradlew" ]; then
        echo -e "  Running unit tests..."
        if ./gradlew :sceneview:testDebugUnitTest :arsceneview:testDebugUnitTest --quiet 2>/dev/null; then
            check "Android unit tests" "PASS" ""
        else
            check "Android unit tests" "FAIL" ""
        fi
    fi

    if [ -d "mcp" ] && [ -f "mcp/package.json" ] && [ -d "mcp/node_modules" ]; then
        echo -e "  Running MCP tests..."
        if (cd mcp && npm test --silent 2>/dev/null); then
            check "MCP tests" "PASS" ""
        else
            check "MCP tests" "FAIL" ""
        fi
    fi

    echo ""
else
    echo -e "${YELLOW}Skipping build & test (--quick mode)${NC}"
    echo ""
fi

# ─── 6. Cross-platform consistency ────────────────────────────────────
echo -e "${CYAN}--- Cross-platform Consistency ---${NC}"

# Check that changed Android APIs have corresponding iOS/llms.txt updates
CHANGED_ANDROID=$(git diff --name-only HEAD 2>/dev/null | grep "^sceneview/src/\|^arsceneview/src/" || true)
if [ -n "$CHANGED_ANDROID" ]; then
    CHANGED_LLMS=$(git diff --name-only HEAD 2>/dev/null | grep "^llms.txt$" || true)
    if [ -z "$CHANGED_LLMS" ]; then
        # Check if the Android changes include new public APIs
        NEW_PUBLIC=$(git diff HEAD -- sceneview/src/ arsceneview/src/ 2>/dev/null | grep "^+.*fun \|^+.*class.*Node\|^+.*@Composable" | wc -l | tr -d ' ')
        if [ "$NEW_PUBLIC" -gt 0 ]; then
            check "llms.txt updated for new APIs" "WARN" "$NEW_PUBLIC new public API(s) — llms.txt not updated"
        else
            check "No new public APIs" "PASS" "llms.txt update not needed"
        fi
    else
        check "llms.txt updated with Android changes" "PASS" ""
    fi
fi

echo ""

# ─── Summary ───────────────────────────────────────────────────────────
echo -e "${CYAN}=== Quality Gate Summary ===${NC}"
echo ""

if [ "$BLOCKERS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}ALL CLEAR — safe to push${NC}"
    exit 0
elif [ "$BLOCKERS" -eq 0 ]; then
    echo -e "${YELLOW}PASS with $WARNINGS warning(s) — review before pushing${NC}"
    exit 0
else
    echo -e "${RED}BLOCKED — $BLOCKERS issue(s) must be fixed before pushing${NC}"
    exit 1
fi
