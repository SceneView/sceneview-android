#!/bin/bash
# Run with: asciinema rec demo.cast --overwrite && asciinema play demo.cast
# Or just: bash record-terminal.sh (to preview)

DELAY=0.04

type_text() {
  local text="$1"
  local delay="${2:-$DELAY}"
  for (( i=0; i<${#text}; i++ )); do
    printf '%s' "${text:$i:1}"
    sleep "$delay"
  done
}

print_line() {
  echo -e "\e[2m$1\e[0m"
  sleep 0.3
}

clear
sleep 0.5

# --- STEP 1: Show mcp.json config ---
echo -e "\e[1;36m# Step 1 — Add SceneView MCP to Claude Desktop\e[0m"
sleep 0.8
echo ""
type_text "cat ~/Library/Application\ Support/Claude/claude_desktop_config.json"
echo ""
sleep 0.4
cat << 'JSON'
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "sceneview-mcp"]
    }
  }
}
JSON
sleep 1.5

echo ""
echo -e "\e[1;32m✓ SceneView MCP server configured\e[0m"
sleep 1.2

# --- STEP 2: Test npx install ---
echo ""
echo -e "\e[1;36m# Step 2 — Verify the MCP server runs\e[0m"
sleep 0.8
echo ""
type_text "npx sceneview-mcp --version"
echo ""
sleep 0.5
echo "sceneview-mcp 3.0.0"
sleep 0.3
echo -e "\e[2mSceneView MCP server ready — exposes sceneview://api + 2 tools\e[0m"
sleep 1.5

# --- STEP 3: Show what tools the server exposes ---
echo ""
echo -e "\e[1;36m# Step 3 — What Claude sees from this MCP server\e[0m"
sleep 0.8
echo ""
echo -e "\e[33mResources:\e[0m"
sleep 0.2
echo "  sceneview://api  →  Full SDK reference (llms.txt)"
sleep 0.5
echo ""
echo -e "\e[33mTools:\e[0m"
sleep 0.2
echo "  get_sample(scenario)  →  Complete Kotlin sample code"
sleep 0.2
echo "    scenarios: model-viewer | ar-tap-to-place | ar-placement-cursor"
sleep 0.2
echo "               ar-augmented-image | ar-face-filter"
sleep 0.4
echo "  get_setup(type)       →  Gradle + Manifest snippet"
sleep 0.2
echo "    types: 3d | ar"
sleep 1.5

# --- STEP 4: Simulate get_sample call ---
echo ""
echo -e "\e[1;36m# Step 4 — Claude calls get_sample(\"ar-tap-to-place\")\e[0m"
sleep 0.8
echo ""
echo -e "\e[2m[MCP tool call: get_sample]\e[0m"
sleep 0.5
echo -e "\e[2m  scenario: \"ar-tap-to-place\"\e[0m"
sleep 0.8
echo ""
echo -e "\e[32m→ Returns complete MainActivity.kt (ARScene + AnchorNode + gestures)\e[0m"
sleep 0.3
echo -e "\e[32m→ Returns Gradle dependencies block\e[0m"
sleep 0.3
echo -e "\e[32m→ Returns suggested prompt for follow-up\e[0m"
sleep 1.5

# --- STEP 5: Show the generated code snippet ---
echo ""
echo -e "\e[1;36m# Step 5 — Generated Kotlin (excerpt)\e[0m"
sleep 0.8
echo ""
cat << 'KOTLIN'
ARScene(
    modifier = Modifier.fillMaxSize(),
    sessionConfiguration = { session, config ->
        config.planeFindingMode = PlaneFindingMode.HORIZONTAL
    }
) {
    onSessionUpdated = { _, frame ->
        frame.hitResults.firstOrNull()?.let { hit ->
            HitResultNode(hitResult = hit) {
                // Tap to place model here
            }
        }
    }
}
KOTLIN
sleep 2.0

echo ""
echo -e "\e[1;32m✓ Paste into Android Studio → Run → AR works\e[0m"
sleep 1.0
echo ""
echo -e "\e[1;35m  sceneview-mcp — npm.im/sceneview-mcp\e[0m"
sleep 2.0
