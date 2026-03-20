# LinkedIn Video — Production Storyboard

**Title:** "3D is just Compose UI"
**Target length:** 60–75 seconds (LinkedIn sweet spot)
**Aspect ratio:** 9:16 (vertical, mobile-first) or 4:5 (square)
**Vibe:** Fast, confident, impressive. No waiting. No loading screens. Like someone who already knows the answer.

---

## Concept

Split-screen throughout: **left = AI generating code from a prompt**, **right = the 3D result rendering live**.
The code appears fast — like the AI is already done and we're just watching the replay at 2×.
The 3D appears as the code appears. Instant. Inevitable.

Between scenes: **hard cuts** with a 1-frame white flash. No fades. No transitions. Speed is the message.

Captions: **large, centered, white, bold**. One sentence at a time. Max 5 words per caption.

---

## Shot-by-shot breakdown

---

### SHOT 1 — [0:00–0:04] — The hook caption

**Visual:** Black screen. Then white text slams in center frame.

```
CAPTION:
"Adding 3D to your Android app
used to be a nightmare."
```

Then immediately cut. No pause.

---

### SHOT 2 — [0:04–0:10] — The old way (2 seconds, sped up)

**Visual:** Screen recording of the old v2.x boilerplate code scrolling — Fragment setup, XML layout, `onResume`, `addChildNode`, `destroy()`. ~60 lines visible. Red overlay tint.

**Audio:** Fast keyboard typing sound, accelerated.

```
CAPTION (bottom):
"~500 lines of boilerplate."
```

Hard cut.

---

### SHOT 3 — [0:10–0:22] — Scene 1: Model Viewer

**Layout:** Split screen 50/50
- **Left:** AI chat interface (Claude/Cursor style). Prompt appears:
  > *"Create a 3D model viewer with orbit camera and HDR lighting"*

  Then code streams in at ~3× speed:
  ```kotlin
  Scene(modifier = Modifier.fillMaxSize()) {
      rememberModelInstance(modelLoader, "models/watch.glb")?.let {
          ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
      }
  }
  ```

- **Right:** Screen recording of the ChronographWatch rotating slowly with HDR reflections. The second hand ticking. Filament quality — reflections, shadows, subsurface scattering.

**Caption (bottom, appears at 0:14):**
```
"That's the whole thing."
```

**Asset:** `ChronographWatch.glb`
Download: https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/ChronographWatch/glTF-Binary/ChronographWatch.glb

---

### SHOT 4 — [0:22–0:34] — Scene 2: Product UI integration

**Layout:** Full screen — a mock e-commerce app UI (Figma mockup quality or real app)
- Product detail page for a sneaker
- Where the `Image` composable used to be: a rotating `MaterialsVariantsShoe.glb`
- The rest of the UI is standard Compose: `TopAppBar`, price text, "Add to Cart" button
- User drags to orbit. The shoe rotates. The UI stays on top.

Code overlay (bottom third, semi-transparent dark bg):
```kotlin
// Replaced Image() with:
Scene(modifier = Modifier.height(300.dp).fillMaxWidth()) {
    ModelNode(modelInstance = shoe, scaleToUnits = 1.0f)
}
```

**Caption (top):**
```
"Your product page.
One Scene{} instead of Image()."
```

**Asset:** `MaterialsVariantsShoe.glb`
Download: https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/MaterialsVariantsShoe/glTF-Binary/MaterialsVariantsShoe.glb

---

### SHOT 5 — [0:34–0:46] — Scene 3: AR placement

**Layout:** Split screen 50/50
- **Left:** Prompt appears:
  > *"Place a 3D object on a detected AR plane"*

  Code streams in:
  ```kotlin
  ARScene(planeRenderer = true,
      onSessionUpdated = { _, frame ->
          anchor = frame.getUpdatedPlanes().firstOrNull()
              ?.let { frame.createAnchorOrNull(it.centerPose) }
      }
  ) {
      anchor?.let { AnchorNode(anchor = it) {
          ModelNode(modelInstance = watch, scaleToUnits = 0.3f)
      }}
  }
  ```

- **Right:** Screen recording: AR plane detected (grid overlay), watch model appears on the table, user pinches to scale it up, rotates with one finger. Real-world lighting matching.

**Caption (bottom, at 0:40):**
```
"AR. Same pattern."
```

---

### SHOT 6 — [0:46–0:55] — Scene 4: The Compose UI overlay in 3D

**Layout:** Full screen AR scene
- A room with a piece of furniture (sofa from Kenney's kit)
- Floating next to the sofa: a `ViewNode` showing a Compose `Card` with price, rating, "Buy Now"
- The card is a 3D billboard — it faces the camera as you move

Code overlay:
```kotlin
AnchorNode(anchor = sofaAnchor) {
    ModelNode(modelInstance = sofa)
    ViewNode(windowManager = windowManager) {
        // This is a real Compose Card — in 3D space
        Card { Column {
            Text("Sofa Pro", style = MaterialTheme.typography.titleMedium)
            Text("€ 599", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = {}) { Text("Buy in AR") }
        }}
    }
}
```

**Caption (top):**
```
"Compose UI.
Inside the 3D scene."
```

**Asset:** Kenney Furniture Kit sofa
Download: https://kenney.nl/assets/furniture-kit

---

### SHOT 7 — [0:55–1:05] — The key insight caption

**Visual:** Black screen. White text, one line at a time, hard cuts between each:

```
"Nodes are composables."
```
*(cut)*
```
"State drives the scene."
```
*(cut)*
```
"Lifecycle is automatic."
```
*(cut)*
```
"It's just Compose."
```

**Pacing:** Each line stays for exactly 2 seconds.

---

### SHOT 8 — [1:05–1:12] — CTA

**Visual:** SceneView logo + GitHub stars badge + Discord member count

Large text:
```
SceneView 3.0
github.com/SceneView/sceneview
```

Small text below:
```
Open source · Filament · ARCore · Compose
```

**Caption:**
```
"Link in comments. Free. Open source."
```

---

## Technical production notes

### Recording the code side
- Use a dark IDE theme (One Dark Pro or Dracula)
- Record at native resolution, export at 1080p
- Play the recording at ~3× speed — it should feel like the AI *just finished*
- Add a subtle "streaming cursor" blinking animation on the last typed character
- Font: JetBrains Mono, 18pt minimum for readability on mobile

### Recording the 3D side
- Use a Pixel 8 Pro or similar (good camera + ARCore depth)
- Record at 60fps, export at 30fps for smooth motion
- The 3D rendering must be SceneView — don't fake it with prerendered video
- For AR shots: good ambient light, a textured surface (wood table, carpet) — ARCore tracks better
- Keep camera movement slow and intentional

### Editing
- Software: CapCut (mobile), DaVinci Resolve, or Premiere Pro
- Color grade: slightly desaturated except the 3D renders (let them pop)
- Audio: no music under the code/3D shots — let the keyboard SFX breathe
- Optional: light lo-fi beat at -20dB under shots 3–6, fade out at shot 7
- Captions: white, bold, centered, 48pt, black outline 2px

### The AI chat UI
Options for the left panel:
1. **Real**: Use Claude Code / Cursor with the actual prompt, record it
2. **Simulated**: Build a simple Compose screen that mimics a chat UI and type-animates the code
   - Easier to control timing, looks cleaner at 3× speed
   - Use a `LaunchedEffect` + `delay(20)` per character for the streaming effect

---

## LinkedIn post copy (to accompany the video)

```
3D used to be hard on Android.

Not anymore.

SceneView 3.0 makes 3D nodes composable functions.
ModelNode, LightNode, AnchorNode — same pattern as Column and Box.
State drives the scene. Lifecycle is automatic.

You don't need to build an AR app to use it.
Replace an Image() with a Scene{}.
Add a rotating product to your detail page.
Float a Compose Card next to an AR-placed object.

10 extra lines. Noticeably better.

→ github.com/SceneView/sceneview
→ Open source · Filament · ARCore · Compose

#AndroidDev #JetpackCompose #AR #3D #SceneView #Kotlin #MobileDev
```

---

## Assets checklist

| Asset | Purpose | Download |
|---|---|---|
| `ChronographWatch.glb` | Shot 3 — hero 3D render | https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/ChronographWatch/glTF-Binary/ChronographWatch.glb |
| `MaterialsVariantsShoe.glb` | Shot 4 — product viewer | https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/MaterialsVariantsShoe/glTF-Binary/MaterialsVariantsShoe.glb |
| Kenney Furniture Kit | Shot 6 — AR furniture | https://kenney.nl/assets/furniture-kit |
| `sky_2k.hdr` | Environment lighting | Already in project |
| SceneView logo | Shot 8 — CTA | In repo assets |

---

## Filming checklist

- [ ] Dark IDE theme configured
- [ ] JetBrains Mono font ≥18pt
- [ ] All 3 GLB assets downloaded and working in the app
- [ ] Physical device with ARCore support (Pixel 8 recommended)
- [ ] Textured filming surface (wood table, not glass)
- [ ] Good ambient light (near a window, no direct sun)
- [ ] Screen recording app ready (Scrcpy + OBS, or Android built-in)
- [ ] Record each shot separately, combine in edit
- [ ] Export: H.264, 1080×1920 (9:16), 30fps, target 50MB max for LinkedIn
