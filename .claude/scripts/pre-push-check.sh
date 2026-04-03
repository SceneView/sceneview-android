#!/bin/bash
# Pre-push quality gate — run before every push to main
# Usage: bash .claude/scripts/pre-push-check.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0

echo "═══════════════════════════════════════════"
echo "  SceneView Pre-Push Quality Gate"
echo "═══════════════════════════════════════════"

# 1. Android compilation
echo -e "\n${YELLOW}[1/6] Compiling sceneview...${NC}"
if ./gradlew :sceneview:compileReleaseKotlin --quiet 2>/dev/null; then
    echo -e "${GREEN}  ✓ sceneview compiles${NC}"
else
    echo -e "${RED}  ✗ sceneview FAILED to compile${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo -e "${YELLOW}[2/6] Compiling arsceneview...${NC}"
if ./gradlew :arsceneview:compileReleaseKotlin --quiet 2>/dev/null; then
    echo -e "${GREEN}  ✓ arsceneview compiles${NC}"
else
    echo -e "${RED}  ✗ arsceneview FAILED to compile${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 2. Unit tests
echo -e "\n${YELLOW}[3/6] Running sceneview unit tests...${NC}"
if ./gradlew :sceneview:test --quiet 2>/dev/null; then
    echo -e "${GREEN}  ✓ sceneview tests pass${NC}"
else
    echo -e "${RED}  ✗ sceneview tests FAILED${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo -e "${YELLOW}[4/6] Running arsceneview unit tests...${NC}"
if ./gradlew :arsceneview:testDebugUnitTest --quiet 2>/dev/null; then
    echo -e "${GREEN}  ✓ arsceneview tests pass${NC}"
else
    echo -e "${RED}  ✗ arsceneview tests FAILED${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 3. Version sync
echo -e "\n${YELLOW}[5/6] Checking version sync...${NC}"
MISMATCHES=$(bash .claude/scripts/sync-versions.sh 2>/dev/null | grep "MISMATCH" | grep -v "migration.md" | grep -v "Errors" | wc -l | tr -d ' ')
if [ "$MISMATCHES" = "0" ]; then
    echo -e "${GREEN}  ✓ All versions aligned${NC}"
else
    echo -e "${RED}  ✗ $MISMATCHES version mismatch(es)${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 4. Website JS syntax
echo -e "\n${YELLOW}[6/6] Validating website JS...${NC}"
NODE_CMD=$(which node 2>/dev/null || which /opt/homebrew/bin/node 2>/dev/null || which /usr/local/bin/node 2>/dev/null || echo "")
if [ -n "$NODE_CMD" ]; then
    if "$NODE_CMD" -c website-static/js/sceneview.js 2>/dev/null; then
        echo -e "${GREEN}  ✓ sceneview.js syntax OK${NC}"
    else
        echo -e "${RED}  ✗ sceneview.js has syntax errors${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${YELLOW}  ⚠ node not found, skipping JS validation${NC}"
fi

# Summary
echo -e "\n═══════════════════════════════════════════"
if [ "$ERRORS" -eq 0 ]; then
    echo -e "${GREEN}  ✓ ALL CHECKS PASSED — safe to push${NC}"
    exit 0
else
    echo -e "${RED}  ✗ $ERRORS CHECK(S) FAILED — DO NOT PUSH${NC}"
    exit 1
fi
