#!/usr/bin/env bash
# sync-assets.sh — Distribute shared assets to all platform demo apps
#
# Usage:
#   bash .claude/scripts/sync-assets.sh           # Check what needs syncing
#   bash .claude/scripts/sync-assets.sh --fix      # Copy assets to all platforms
#   bash .claude/scripts/sync-assets.sh --discover  # Search for new free assets online
#
# The shared assets directory (assets/) is the source of truth.
# Platform-specific copies are derived from it.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ASSETS_DIR="$REPO_ROOT/assets"
CATALOG="$ASSETS_DIR/catalog.json"

# Platform asset directories
ANDROID_MODELS="$REPO_ROOT/samples/android-demo/src/main/assets/models"
ANDROID_ENVS="$REPO_ROOT/samples/android-demo/src/main/assets/environments"
ANDROID_TV_MODELS="$REPO_ROOT/samples/android-tv-demo/src/main/assets/models"
ANDROID_TV_ENVS="$REPO_ROOT/samples/android-tv-demo/src/main/assets/environments"
IOS_MODELS="$REPO_ROOT/samples/ios-demo/SceneViewDemo/Models"
WEB_MODELS="$REPO_ROOT/samples/web-demo/public/models"
FLUTTER_ANDROID_MODELS="$REPO_ROOT/samples/flutter-demo/example/android/app/src/main/assets/models"
FLUTTER_ANDROID_ENVS="$REPO_ROOT/samples/flutter-demo/example/android/app/src/main/assets/environments"
FLUTTER_IOS_MODELS="$REPO_ROOT/samples/flutter-demo/example/ios/Runner/Models"
RN_ANDROID_MODELS="$REPO_ROOT/samples/react-native-demo/android/app/src/main/assets/models"
RN_ANDROID_ENVS="$REPO_ROOT/samples/react-native-demo/android/app/src/main/assets/environments"
RN_IOS_MODELS="$REPO_ROOT/samples/react-native-demo/ios/Models"
WEBSITE_MODELS="$REPO_ROOT/website-static/models/platforms"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

errors=0
synced=0
missing=0

check_or_fix() {
    local src="$1"
    local dst="$2"
    local mode="${3:-check}"

    if [ ! -f "$src" ]; then
        return 0  # Source doesn't exist, skip
    fi

    if [ ! -f "$dst" ]; then
        missing=$((missing + 1))
        if [ "$mode" = "fix" ]; then
            mkdir -p "$(dirname "$dst")"
            cp "$src" "$dst"
            echo -e "  ${GREEN}COPIED${NC} $(basename "$src") → $(echo "$dst" | sed "s|$REPO_ROOT/||")"
            synced=$((synced + 1))
        else
            echo -e "  ${YELLOW}MISSING${NC} $(echo "$dst" | sed "s|$REPO_ROOT/||")"
        fi
    else
        # Check if files differ
        if ! cmp -s "$src" "$dst"; then
            missing=$((missing + 1))
            if [ "$mode" = "fix" ]; then
                cp "$src" "$dst"
                echo -e "  ${GREEN}UPDATED${NC} $(basename "$src") → $(echo "$dst" | sed "s|$REPO_ROOT/||")"
                synced=$((synced + 1))
            else
                echo -e "  ${YELLOW}OUTDATED${NC} $(echo "$dst" | sed "s|$REPO_ROOT/||")"
            fi
        fi
    fi
}

echo ""
echo -e "${BLUE}═══ SceneView Asset Sync ═══${NC}"
echo ""

MODE="check"
if [ "${1:-}" = "--fix" ]; then
    MODE="fix"
    echo -e "${BLUE}Mode: FIX (copying assets to platforms)${NC}"
elif [ "${1:-}" = "--discover" ]; then
    MODE="discover"
    echo -e "${BLUE}Mode: DISCOVER (searching for new assets)${NC}"
else
    echo -e "${BLUE}Mode: CHECK (dry run — use --fix to sync)${NC}"
fi
echo ""

if [ "$MODE" = "discover" ]; then
    echo -e "${BLUE}Searching free asset sources...${NC}"
    echo ""

    # Check Poly Haven for new HDR environments
    echo -e "${BLUE}[Poly Haven] Checking HDR environments...${NC}"
    if command -v curl &>/dev/null; then
        HDRIS=$(curl -s "https://api.polyhaven.com/assets?t=hdris&categories=studio,outdoor,urban" 2>/dev/null | python3 -c "
import json, sys
try:
    data = json.load(sys.stdin)
    for name in sorted(data.keys())[:20]:
        info = data[name]
        cats = ','.join(info.get('categories', []))
        print(f'  {name} [{cats}]')
except:
    print('  (failed to parse)')
" 2>/dev/null || echo "  (API unavailable)")
        echo "$HDRIS"
    fi
    echo ""

    # Check Sketchfab for popular free downloadable models
    echo -e "${BLUE}[Sketchfab] Popular free downloadable models...${NC}"
    if command -v curl &>/dev/null; then
        curl -s "https://api.sketchfab.com/v3/search?type=models&downloadable=true&sort_by=-likeCount&count=10" 2>/dev/null | python3 -c "
import json, sys
try:
    data = json.load(sys.stdin)
    for r in data.get('results', []):
        name = r.get('name', '?')
        likes = r.get('likeCount', 0)
        faces = r.get('faceCount', 0)
        uid = r.get('uid', '')
        print(f'  {name} ({likes} likes, {faces:,} faces)')
        print(f'    https://sketchfab.com/3d-models/{uid}')
except:
    print('  (failed to parse)')
" 2>/dev/null || echo "  (API unavailable)"
    fi
    echo ""
    exit 0
fi

# --- Sync GLB models to Android ---
echo -e "${BLUE}[Android] GLB models → $( echo "$ANDROID_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$ANDROID_MODELS/$(basename "$glb")" "$MODE"
done

# --- Sync GLB models to Android TV ---
echo -e "${BLUE}[Android TV] GLB models → $( echo "$ANDROID_TV_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$ANDROID_TV_MODELS/$(basename "$glb")" "$MODE"
done

# --- Sync USDZ models to iOS ---
echo -e "${BLUE}[iOS] USDZ models → $( echo "$IOS_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for usdz in "$ASSETS_DIR"/models/usdz/*.usdz; do
    [ -f "$usdz" ] || continue
    check_or_fix "$usdz" "$IOS_MODELS/$(basename "$usdz")" "$MODE"
done

# --- Sync GLB models to Web ---
echo -e "${BLUE}[Web] GLB models → $( echo "$WEB_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$WEB_MODELS/$(basename "$glb")" "$MODE"
done

# --- Sync GLB models to Flutter (Android side) ---
echo -e "${BLUE}[Flutter/Android] GLB models → $( echo "$FLUTTER_ANDROID_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$FLUTTER_ANDROID_MODELS/$(basename "$glb")" "$MODE"
done

# --- Sync USDZ models to Flutter (iOS side) ---
echo -e "${BLUE}[Flutter/iOS] USDZ models → $( echo "$FLUTTER_IOS_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for usdz in "$ASSETS_DIR"/models/usdz/*.usdz; do
    [ -f "$usdz" ] || continue
    check_or_fix "$usdz" "$FLUTTER_IOS_MODELS/$(basename "$usdz")" "$MODE"
done

# --- Sync GLB models to React Native (Android side) ---
echo -e "${BLUE}[RN/Android] GLB models → $( echo "$RN_ANDROID_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$RN_ANDROID_MODELS/$(basename "$glb")" "$MODE"
done

# --- Sync USDZ models to React Native (iOS side) ---
echo -e "${BLUE}[RN/iOS] USDZ models → $( echo "$RN_IOS_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for usdz in "$ASSETS_DIR"/models/usdz/*.usdz; do
    [ -f "$usdz" ] || continue
    check_or_fix "$usdz" "$RN_IOS_MODELS/$(basename "$usdz")" "$MODE"
done

# --- Sync GLB models to Website ---
echo -e "${BLUE}[Website] GLB models → $( echo "$WEBSITE_MODELS" | sed "s|$REPO_ROOT/||" )${NC}"
for glb in "$ASSETS_DIR"/models/glb/*.glb; do
    [ -f "$glb" ] || continue
    check_or_fix "$glb" "$WEBSITE_MODELS/$(basename "$glb")" "$MODE"
done

# --- Summary ---
echo ""
if [ "$MODE" = "fix" ]; then
    echo -e "${GREEN}Synced $synced assets across platforms.${NC}"
else
    if [ "$missing" -eq 0 ]; then
        echo -e "${GREEN}All assets are in sync across platforms.${NC}"
    else
        echo -e "${YELLOW}$missing assets need syncing. Run with --fix to update.${NC}"
    fi
fi

# --- Catalog stats ---
echo ""
if [ -f "$CATALOG" ] && command -v python3 &>/dev/null; then
    python3 -c "
import json
with open('$CATALOG') as f:
    cat = json.load(f)
models = len(cat.get('models', []))
envs = len(cat.get('environments', []))
pending = len(cat.get('pendingReview', []))
print(f'Catalog: {models} models, {envs} environments, {pending} pending review')
" 2>/dev/null
fi
