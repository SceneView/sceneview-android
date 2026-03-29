#!/usr/bin/env bash
# sync-versions.sh — Scan all version declarations, report mismatches, optionally fix them
#
# Usage:
#   ./sync-versions.sh          # Report only
#   ./sync-versions.sh --fix    # Report and fix mismatches
#
# Exit codes:
#   0 = all versions aligned
#   1 = mismatches found (or fixed if --fix)
#   2 = error

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FIX_MODE="${1:-}"
ERRORS=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== SceneView Version Sync Check ===${NC}"
echo ""

# ─── Source of truth ───────────────────────────────────────────────────────
SOURCE_VERSION=$(grep '^VERSION_NAME=' "$REPO_ROOT/gradle.properties" | cut -d= -f2)
if [ -z "$SOURCE_VERSION" ]; then
    echo -e "${RED}FATAL: Cannot read VERSION_NAME from gradle.properties${NC}"
    exit 2
fi
echo -e "Source of truth (gradle.properties): ${GREEN}$SOURCE_VERSION${NC}"
echo ""

# ─── Version locations ─────────────────────────────────────────────────────
declare -A VERSION_MAP

# Gradle properties (Android)
for module in sceneview arsceneview sceneview-core; do
    PROPS="$REPO_ROOT/$module/gradle.properties"
    if [ -f "$PROPS" ]; then
        V=$(grep '^VERSION_NAME=' "$PROPS" 2>/dev/null | cut -d= -f2 || echo "MISSING")
        VERSION_MAP["$module/gradle.properties"]="$V"
    fi
done

# npm packages
for pkg in mcp sceneview-web react-native/react-native-sceneview; do
    PKG_JSON="$REPO_ROOT/$pkg/package.json"
    if [ -f "$PKG_JSON" ]; then
        V=$(python3 -c "import json; print(json.load(open('$PKG_JSON'))['version'])" 2>/dev/null || echo "MISSING")
        VERSION_MAP["$pkg/package.json"]="$V"
    fi
done

# Flutter pubspec
PUBSPEC="$REPO_ROOT/flutter/sceneview_flutter/pubspec.yaml"
if [ -f "$PUBSPEC" ]; then
    V=$(grep '^version:' "$PUBSPEC" | awk '{print $2}' || echo "MISSING")
    VERSION_MAP["flutter/sceneview_flutter/pubspec.yaml"]="$V"
fi

# llms.txt (check for version string near top)
LLMS="$REPO_ROOT/llms.txt"
if [ -f "$LLMS" ]; then
    # Look for the Maven artifact version pattern
    V=$(grep -m1 'io\.github\.sceneview:sceneview:' "$LLMS" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    VERSION_MAP["llms.txt"]="$V"
fi

# CLAUDE.md (check the "When writing any SceneView code" section)
CLAUDE_MD="$REPO_ROOT/CLAUDE.md"
if [ -f "$CLAUDE_MD" ]; then
    V=$(grep -m1 'io\.github\.sceneview:sceneview:' "$CLAUDE_MD" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    VERSION_MAP["CLAUDE.md (code examples)"]="$V"
fi

# README.md
README="$REPO_ROOT/README.md"
if [ -f "$README" ]; then
    V=$(grep -m1 'io\.github\.sceneview:sceneview:' "$README" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "NOT FOUND")
    VERSION_MAP["README.md"]="$V"
fi

# CHANGELOG.md (top entry version)
CHANGELOG="$REPO_ROOT/CHANGELOG.md"
if [ -f "$CHANGELOG" ]; then
    V=$(grep -m1 '^## ' "$CHANGELOG" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "MISSING")
    VERSION_MAP["CHANGELOG.md (latest entry)"]="$V"
fi

# ─── Report ────────────────────────────────────────────────────────────────
echo -e "${CYAN}Version Locations Report:${NC}"
echo ""
printf "  %-50s %-12s %-10s\n" "Location" "Version" "Status"
printf "  %-50s %-12s %-10s\n" "--------" "-------" "------"

for location in $(echo "${!VERSION_MAP[@]}" | tr ' ' '\n' | sort); do
    V="${VERSION_MAP[$location]}"
    if [ "$V" = "$SOURCE_VERSION" ]; then
        printf "  %-50s %-12s ${GREEN}%-10s${NC}\n" "$location" "$V" "OK"
    elif [ "$V" = "MISSING" ] || [ "$V" = "NOT FOUND" ]; then
        printf "  %-50s %-12s ${YELLOW}%-10s${NC}\n" "$location" "$V" "SKIP"
    else
        printf "  %-50s %-12s ${RED}%-10s${NC}\n" "$location" "$V" "MISMATCH"
        ERRORS=$((ERRORS + 1))
    fi
done

echo ""

# ─── Website check ─────────────────────────────────────────────────────────
WEBSITE_DIR="$REPO_ROOT/../sceneview.github.io"
if [ -d "$WEBSITE_DIR" ]; then
    echo -e "${CYAN}Website Version Check:${NC}"
    WEBSITE_INDEX="$WEBSITE_DIR/index.html"
    if [ -f "$WEBSITE_INDEX" ]; then
        WV=$(grep -oE 'sceneview:[0-9]+\.[0-9]+\.[0-9]+' "$WEBSITE_INDEX" | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' || echo "NOT FOUND")
        if [ "$WV" = "$SOURCE_VERSION" ]; then
            printf "  %-50s %-12s ${GREEN}%-10s${NC}\n" "website index.html" "$WV" "OK"
        elif [ "$WV" != "NOT FOUND" ]; then
            printf "  %-50s %-12s ${RED}%-10s${NC}\n" "website index.html" "$WV" "MISMATCH"
            ERRORS=$((ERRORS + 1))
        fi
    fi
    echo ""
fi

# ─── Fix mode ──────────────────────────────────────────────────────────────
if [ "$FIX_MODE" = "--fix" ] && [ "$ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}Applying fixes...${NC}"

    # Fix module gradle.properties
    for module in sceneview arsceneview sceneview-core; do
        PROPS="$REPO_ROOT/$module/gradle.properties"
        if [ -f "$PROPS" ]; then
            CURRENT=$(grep '^VERSION_NAME=' "$PROPS" | cut -d= -f2)
            if [ "$CURRENT" != "$SOURCE_VERSION" ]; then
                sed -i '' "s/^VERSION_NAME=.*/VERSION_NAME=$SOURCE_VERSION/" "$PROPS"
                echo -e "  Fixed: $module/gradle.properties ($CURRENT -> $SOURCE_VERSION)"
            fi
        fi
    done

    # Fix npm package.json files
    for pkg in mcp sceneview-web react-native/react-native-sceneview; do
        PKG_JSON="$REPO_ROOT/$pkg/package.json"
        if [ -f "$PKG_JSON" ]; then
            CURRENT=$(python3 -c "import json; print(json.load(open('$PKG_JSON'))['version'])" 2>/dev/null)
            if [ "$CURRENT" != "$SOURCE_VERSION" ]; then
                python3 -c "
import json
with open('$PKG_JSON', 'r') as f:
    data = json.load(f)
data['version'] = '$SOURCE_VERSION'
with open('$PKG_JSON', 'w') as f:
    json.dump(data, f, indent=2)
    f.write('\n')
"
                echo -e "  Fixed: $pkg/package.json ($CURRENT -> $SOURCE_VERSION)"
            fi
        fi
    done

    # Fix Flutter pubspec.yaml
    if [ -f "$PUBSPEC" ]; then
        CURRENT=$(grep '^version:' "$PUBSPEC" | awk '{print $2}')
        if [ "$CURRENT" != "$SOURCE_VERSION" ]; then
            sed -i '' "s/^version: .*/version: $SOURCE_VERSION/" "$PUBSPEC"
            echo -e "  Fixed: flutter/sceneview_flutter/pubspec.yaml ($CURRENT -> $SOURCE_VERSION)"
        fi
    fi

    echo ""
    echo -e "${GREEN}Fixes applied. Re-run without --fix to verify.${NC}"
fi

# ─── Summary ───────────────────────────────────────────────────────────────
if [ "$ERRORS" -eq 0 ]; then
    echo -e "${GREEN}All versions are aligned to $SOURCE_VERSION${NC}"
    exit 0
else
    echo -e "${RED}$ERRORS version mismatch(es) found.${NC}"
    if [ "$FIX_MODE" != "--fix" ]; then
        echo -e "Run with ${YELLOW}--fix${NC} to auto-fix where possible."
    fi
    exit 1
fi
