#!/bin/bash
# SceneView Demo iOS — Build & Test
# Usage: ./build-and-test.sh
# Requires: Xcode 16+

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$PROJECT_DIR/SceneViewDemo.xcodeproj"
SCHEME="SceneViewDemo"
SIMULATOR="platform=iOS Simulator,name=iPhone 16 Pro,OS=latest"

echo "🔨 Build sur simulateur iOS..."
xcodebuild build \
  -project "$PROJECT" \
  -scheme "$SCHEME" \
  -destination "$SIMULATOR" \
  -skipMacroValidation \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  | xcpretty 2>/dev/null || grep -E "error:|warning:|BUILD"

echo ""
echo "✅ Build terminé."
echo ""
echo "📱 Pour lancer sur simulateur :"
echo "  xcodebuild -project $PROJECT -scheme $SCHEME -destination '$SIMULATOR' build"
echo ""
echo "📦 Pour archiver (App Store) :"
echo "  cd $PROJECT_DIR"
echo "  xcodebuild archive \\"
echo "    -project SceneViewDemo.xcodeproj \\"
echo "    -scheme SceneViewDemo \\"
echo "    -destination 'generic/platform=iOS' \\"
echo "    -archivePath ./build/SceneViewDemo.xcarchive"
echo ""
echo "  xcodebuild -exportArchive \\"
echo "    -archivePath ./build/SceneViewDemo.xcarchive \\"
echo "    -exportOptionsPlist ExportOptions.plist \\"
echo "    -exportPath ./build/export"
