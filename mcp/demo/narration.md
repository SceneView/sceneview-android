# SceneView MCP — Narration Script

**Total: ~90 seconds**
Speak at a comfortable pace. Pauses marked with `[beat]`.

---

**[0:00 — over AR demo footage]**
> "What if you could build a full AR app just by describing it to Claude?"

`[beat]`

---

**[0:08 — terminal, adding config]**
> "SceneView MCP is a single config line."

`[beat]`

> "It gives Claude the full SceneView SDK — every composable, every API — as a tool it can call on demand."

---

**[0:18 — Claude Desktop, typing the prompt]**
> "You just describe what you want."

`[beat]`

> "AR tap-to-place. Horizontal plane detection. A 3D model. Pinch to scale."

`[beat]`

> "No boilerplate. No digging through docs."

---

**[0:28 — Claude's response streaming]**
> "Claude calls the SceneView MCP tools."

`[beat]`

> "It gets the right Gradle dependencies. The correct ARCore configuration. A working sample it adapts to your exact request."

`[beat]`

> "Everything compiles. The threading rules are respected. The API calls are correct."

---

**[0:50 — paste into Android Studio]**
> "Copy. Paste. Run."

---

**[1:10 — AR running on device]**
> "Planes detected. Tap. Your model appears."

`[beat]`

> "That's a production-ready AR app. Written by Claude. Powered by SceneView."

---

**[1:25 — CTA]**
> "sceneview-mcp — on npm now."

---

## YouTube description

```
Build a full AR Android app with a single Claude prompt.

SceneView MCP gives Claude direct access to the SceneView 3D/AR SDK —
so it can generate correct, compilable Kotlin code for your app without
hallucinating APIs or missing dependencies.

→ npm: npmjs.com/package/sceneview-mcp
→ SDK: github.com/SceneView/sceneview-android

Setup (30 seconds):
Add to ~/Library/Application Support/Claude/claude_desktop_config.json:
{
  "mcpServers": {
    "sceneview": { "command": "npx", "args": ["-y", "sceneview-mcp"] }
  }
}

Then ask Claude:
"Build me an AR tap-to-place app with SceneView"
"Add a 3D model viewer with HDR environment"
"Create an AR face filter using the front camera"

#AndroidDev #AR #ARCore #JetpackCompose #Claude #MCP #SceneView
```

## Tweet / X thread

**Tweet 1:**
> I just published sceneview-mcp — an MCP server that gives Claude the full SceneView 3D/AR SDK.
>
> Ask Claude to "build an AR app" and it actually works.
>
> No hallucinated APIs. No missing imports. Just paste and run. 🧵

**Tweet 2:**
> How it works:
>
> 1. Add sceneview-mcp to Claude Desktop (1 line of JSON)
> 2. Ask Claude to build your AR/3D feature
> 3. Claude calls the MCP tools → gets real API docs + working samples
> 4. Paste the generated Kotlin into Android Studio → run

**Tweet 3:**
> Supports:
> • Tap-to-place AR
> • 3D model viewer + HDR environments
> • AR placement cursor
> • Augmented image tracking
> • AR face filters (front camera)
>
> npm install sceneview-mcp
> npmjs.com/package/sceneview-mcp

**Discord announcement:**
> **SceneView 3.0 + Claude AI integration is here**
>
> We just published `sceneview-mcp` — an MCP server that lets Claude build SceneView apps correctly.
>
> Add it to Claude Desktop and just describe what you want:
> *"Build an AR tap-to-place scene with a helmet model and pinch to scale"*
>
> Claude will generate working, compilable Kotlin using the real SceneView API.
>
> `npx sceneview-mcp` | npmjs.com/package/sceneview-mcp
