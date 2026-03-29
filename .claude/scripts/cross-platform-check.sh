#!/usr/bin/env bash
# cross-platform-check.sh — Compare Android vs iOS vs Web API surface, report gaps
#
# Usage:
#   ./cross-platform-check.sh
#
# This script extracts public API surface from each platform and compares them.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}=== SceneView Cross-Platform API Consistency Check ===${NC}"
echo ""

# ─── 1. Android Node Types ────────────────────────────────────────────────
echo -e "${CYAN}--- Android Node Types (sceneview/) ---${NC}"
ANDROID_NODES=()
while IFS= read -r line; do
    ANDROID_NODES+=("$line")
done < <(grep -rh "^class\|^open class\|^abstract class" "$REPO_ROOT/sceneview/src/" "$REPO_ROOT/arsceneview/src/" 2>/dev/null \
    | grep -i "node\|scene" \
    | sed 's/class //' | sed 's/(.*//' | sed 's/ :.*//' | sed 's/open //' | sed 's/abstract //' \
    | sort -u || true)

for n in "${ANDROID_NODES[@]}"; do
    echo "  - $n"
done
echo "  Total: ${#ANDROID_NODES[@]}"
echo ""

# ─── 2. iOS/Swift Node Types ──────────────────────────────────────────────
echo -e "${CYAN}--- iOS Node Types (SceneViewSwift/) ---${NC}"
SWIFT_NODES=()
SWIFT_SRC="$REPO_ROOT/SceneViewSwift/Sources/SceneViewSwift"
if [ -d "$SWIFT_SRC" ]; then
    while IFS= read -r line; do
        SWIFT_NODES+=("$line")
    done < <(grep -rh "^public class\|^public struct\|^open class" "$SWIFT_SRC" 2>/dev/null \
        | grep -i "node\|scene\|view" \
        | sed 's/public //' | sed 's/open //' | sed 's/class //' | sed 's/struct //' \
        | sed 's/(.*//' | sed 's/ :.*//' | sed 's/<.*//' \
        | sort -u || true)
    for n in "${SWIFT_NODES[@]}"; do
        echo "  - $n"
    done
    echo "  Total: ${#SWIFT_NODES[@]}"
else
    echo -e "  ${YELLOW}SceneViewSwift/Sources not found${NC}"
fi
echo ""

# ─── 3. Web API (sceneview-web/) ──────────────────────────────────────────
echo -e "${CYAN}--- Web API (sceneview-web/) ---${NC}"
WEB_SRC="$REPO_ROOT/sceneview-web/src"
if [ -d "$WEB_SRC" ]; then
    WEB_CLASSES=()
    while IFS= read -r line; do
        WEB_CLASSES+=("$line")
    done < <(grep -rh "^class\|^open class\|^external class" "$WEB_SRC" 2>/dev/null \
        | sed 's/class //' | sed 's/(.*//' | sed 's/ :.*//' | sed 's/open //' | sed 's/external //' \
        | sort -u || true)
    for n in "${WEB_CLASSES[@]}"; do
        echo "  - $n"
    done
    echo "  Total: ${#WEB_CLASSES[@]}"
else
    echo -e "  ${YELLOW}sceneview-web/src not found${NC}"
fi
echo ""

# ─── 4. KMP Core Types ────────────────────────────────────────────────────
echo -e "${CYAN}--- KMP Core Types (sceneview-core/) ---${NC}"
KMP_SRC="$REPO_ROOT/sceneview-core/src/commonMain"
if [ -d "$KMP_SRC" ]; then
    KMP_TYPES=()
    while IFS= read -r line; do
        KMP_TYPES+=("$line")
    done < <(grep -rh "^class\|^data class\|^object\|^interface" "$KMP_SRC" 2>/dev/null \
        | sed 's/data //' | sed 's/class //' | sed 's/object //' | sed 's/interface //' \
        | sed 's/(.*//' | sed 's/ :.*//' | sed 's/{.*//' \
        | sort -u || true)
    for n in "${KMP_TYPES[@]}"; do
        echo "  - $n"
    done
    echo "  Total: ${#KMP_TYPES[@]}"
else
    echo -e "  ${YELLOW}sceneview-core/src/commonMain not found${NC}"
fi
echo ""

# ─── 5. Node parity check ─────────────────────────────────────────────────
echo -e "${CYAN}--- Cross-Platform Node Parity ---${NC}"
echo ""

# Canonical node types expected on all platforms
CANONICAL_NODES=(
    "ModelNode"
    "CameraNode"
    "LightNode"
    "AnchorNode"
    "ViewNode"
    "VideoNode"
)

printf "  %-20s %-10s %-10s %-10s\n" "Node Type" "Android" "iOS" "Web"
printf "  %-20s %-10s %-10s %-10s\n" "---------" "-------" "---" "---"

GAPS=0
for node in "${CANONICAL_NODES[@]}"; do
    ANDROID_HAS="NO"
    IOS_HAS="NO"
    WEB_HAS="NO"

    for a in "${ANDROID_NODES[@]}"; do
        if [[ "$a" == *"$node"* ]]; then ANDROID_HAS="YES"; break; fi
    done
    for s in "${SWIFT_NODES[@]}"; do
        if [[ "$s" == *"$node"* ]]; then IOS_HAS="YES"; break; fi
    done
    if [ -d "$WEB_SRC" ]; then
        if grep -rq "$node" "$WEB_SRC" 2>/dev/null; then WEB_HAS="YES"; fi
    fi

    STATUS_A="${GREEN}YES${NC}"
    STATUS_I="${GREEN}YES${NC}"
    STATUS_W="${GREEN}YES${NC}"
    [ "$ANDROID_HAS" = "NO" ] && STATUS_A="${RED}NO${NC}" && GAPS=$((GAPS + 1))
    [ "$IOS_HAS" = "NO" ] && STATUS_I="${RED}NO${NC}" && GAPS=$((GAPS + 1))
    [ "$WEB_HAS" = "NO" ] && STATUS_W="${YELLOW}NO${NC}" && GAPS=$((GAPS + 1))

    printf "  %-20s ${STATUS_A}%-1s${NC}     ${STATUS_I}%-1s${NC}     ${STATUS_W}%-1s${NC}\n" "$node" "" "" ""
done

echo ""

# ─── 6. Composable parity (Android-specific) ──────────────────────────────
echo -e "${CYAN}--- Android Composable Functions ---${NC}"
COMPOSABLES=$(grep -rh "@Composable" "$REPO_ROOT/sceneview/src/" "$REPO_ROOT/arsceneview/src/" 2>/dev/null \
    | grep "fun " | sed 's/.*fun //' | sed 's/(.*//' | sort -u || true)
echo "$COMPOSABLES" | while read -r c; do
    [ -n "$c" ] && echo "  - $c"
done
COMPOSABLE_COUNT=$(echo "$COMPOSABLES" | grep -c '[a-z]' || echo 0)
echo "  Total composables: $COMPOSABLE_COUNT"
echo ""

# ─── 7. llms.txt coverage ─────────────────────────────────────────────────
echo -e "${CYAN}--- llms.txt API Coverage ---${NC}"
LLMS="$REPO_ROOT/llms.txt"
if [ -f "$LLMS" ]; then
    UNDOCUMENTED=0
    for node in "${ANDROID_NODES[@]}"; do
        if ! grep -q "$node" "$LLMS" 2>/dev/null; then
            echo -e "  ${YELLOW}UNDOCUMENTED in llms.txt: $node${NC}"
            UNDOCUMENTED=$((UNDOCUMENTED + 1))
        fi
    done
    if [ "$UNDOCUMENTED" -eq 0 ]; then
        echo -e "  ${GREEN}All Android nodes documented in llms.txt${NC}"
    else
        echo -e "  ${RED}$UNDOCUMENTED node(s) not found in llms.txt${NC}"
    fi
else
    echo -e "  ${YELLOW}llms.txt not found${NC}"
fi
echo ""

# ─── Summary ───────────────────────────────────────────────────────────────
echo -e "${CYAN}=== Summary ===${NC}"
echo "  Android nodes: ${#ANDROID_NODES[@]}"
echo "  iOS nodes: ${#SWIFT_NODES[@]}"
echo "  KMP core types: ${#KMP_TYPES[@]}"
echo "  Cross-platform gaps: $GAPS"
echo ""

if [ "$GAPS" -eq 0 ]; then
    echo -e "${GREEN}All canonical node types present on all platforms.${NC}"
else
    echo -e "${YELLOW}$GAPS gap(s) detected in cross-platform parity.${NC}"
fi
