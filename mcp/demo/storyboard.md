# SceneView MCP — Video Storyboard

**Target length:** 90 seconds
**Format:** Screen recording + voiceover, no webcam needed
**Vibe:** Fast, technical, impressive — like a magic trick

---

## Shot 1 — Hook (0:00–0:08)
**Screen:** Final result — phone running AR tap-to-place
**Narration:** *"What if you could build a full AR app just by describing it to Claude?"*
**Note:** Record the AR demo on a real device first, use as opener

---

## Shot 2 — The Setup (0:08–0:18)
**Screen:** Terminal
**Action:** Type and run:
```bash
# Open Claude Desktop config
code ~/Library/Application\ Support/Claude/claude_desktop_config.json
```
Paste in:
```json
{
  "mcpServers": {
    "sceneview": {
      "command": "npx",
      "args": ["-y", "sceneview-mcp"]
    }
  }
}
```
**Narration:** *"One config line. SceneView MCP gives Claude the full 3D and AR SDK as a tool."*

---

## Shot 3 — Open Claude (0:18–0:28)
**Screen:** Claude Desktop, fresh chat
**Action:** Type slowly so it's readable:
> "Build me an AR tap-to-place app with SceneView. When I tap a horizontal plane, place a 3D helmet model. Add pinch to scale."

**Narration:** *"Describe what you want. No boilerplate. No docs."*

---

## Shot 4 — Claude Works (0:28–0:50)
**Screen:** Claude Desktop response streaming
**What Claude does:**
1. Calls `get_setup("ar")` → shows Gradle block
2. Calls `get_sample("ar-tap-to-place")` → shows full Kotlin
3. Adapts the code to add pinch-to-scale

**Narration:** *"Claude calls the SceneView MCP tools — gets the exact API, the right dependencies, a working sample — then writes your app."*
**Note:** Let the response stream in real time, don't cut

---

## Shot 5 — Paste & Run (0:50–1:10)
**Screen:** Split — Claude on left, Android Studio on right
**Action:**
1. Copy the generated `MainActivity.kt`
2. Paste into Android Studio (new empty project)
3. Copy the Gradle dependencies, paste into `build.gradle`
4. Hit Run ▶

**Narration:** *"Copy. Paste. Run."*
**Note:** Have the project pre-created and pre-configured to save time — just paste and hit run

---

## Shot 6 — It Works (1:10–1:25)
**Screen:** Phone / emulator running the AR scene
**Action:** Slowly pan over a flat surface → planes detected → tap → helmet appears → pinch to scale it
**Narration:** *"That's a production-ready AR app. Written by Claude. Powered by SceneView."*

---

## Shot 7 — CTA (1:25–1:30)
**Screen:** Terminal, fast
```
npx sceneview-mcp
```
Text overlay: **npmjs.com/package/sceneview-mcp**
**Narration:** *"sceneview-mcp — on npm now."*

---

## Recording checklist
- [ ] Record AR demo on device first (for Shot 1)
- [ ] Set terminal font size to 18+ for readability
- [ ] Use a dark terminal theme (Tokyo Night / Dracula)
- [ ] Claude Desktop window: 1200×800, no other tabs visible
- [ ] Android Studio: hide all tool windows except editor
- [ ] Record at 2× → export at 1× for crisp text
- [ ] Add subtle background music (lo-fi, not distracting)

## Tools needed
- `asciinema` + `agg` (for terminal GIF if needed)
- QuickTime or OBS for screen recording
- DaVinci Resolve / CapCut for final cut
