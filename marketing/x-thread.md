# Twitter/X Thread — SceneView 3.3.0: Cross-Platform 3D & AR

*Copy-paste each numbered block as a separate tweet. Thread format.*

---

## Thread

**1/14 — Hook**

3D on mobile used to require 500+ lines of boilerplate, lifecycle management, and platform-specific knowledge.

SceneView 3.3.0 reduced it to this — on BOTH platforms:

Android:
```
Scene {
    ModelNode(modelInstance = helmet, scaleToUnits = 1.0f)
}
```

iOS:
```
SceneView {
    ModelNode(named: "helmet.usdz")
}
```

A thread on why it's the #1 3D & AR SDK for Android and iOS:

---

**2/14 — The core idea**

The insight: 3D nodes should work like your UI framework.

Android (Compose):
- `ModelNode` = like `Image()` but 3D
- `if/else` = controls what's in the scene
- `State<T>` = drives animations

iOS (SwiftUI):
- `ModelNode` = like `Image()` but 3D
- `if/else` = controls what's in the scene
- `@State` = drives animations

No new paradigm. Just your UI framework — with depth.

---

**3/14 — Now cross-platform**

SceneView 3.3.0 ships on:
- Android: Jetpack Compose + Filament + ARCore
- iOS: SwiftUI + RealityKit + ARKit
- macOS: SwiftUI + RealityKit
- visionOS: SwiftUI + RealityKit

Same concepts. Native renderers. Native performance.

One SDK, four platforms.

---

**4/14 — 16 iOS node types**

All new in v3.3.0:

ModelNode, GeometryNode, LightNode, CameraNode, MeshNode, DynamicSkyNode, FogNode, ReflectionProbeNode, PhysicsNode, LineNode, PathNode, TextNode, BillboardNode, ImageNode, VideoNode, AugmentedImageNode

Every major category covered: models, geometry, lighting, atmosphere, physics, text, drawing, media, AR.

---

**5/14 — The architecture**

KMP shares LOGIC (math, collision, geometry, animation).
Each platform uses its NATIVE RENDERER.

Android → Filament (OpenGL ES / Vulkan)
Apple → RealityKit (Metal)

No cross-compiled renderers. No WASM bridges. Native performance, native debugging, native tooling.

---

**6/14 — Android: 26+ node types**

All composable:

Models: ModelNode
Geometry: CubeNode, SphereNode, CylinderNode, PlaneNode
Lighting: LightNode, DynamicSkyNode
Atmosphere: FogNode, ReflectionProbeNode
Media: ImageNode, VideoNode, ViewNode
Text: TextNode, BillboardNode
Drawing: LineNode, PathNode
Physics: PhysicsNode
AR: AnchorNode, AugmentedImageNode, AugmentedFaceNode, CloudAnchorNode

---

**7/14 — The rendering engines**

Android: Google Filament — the same PBR engine Google uses in Search and Play Store.
iOS: RealityKit — Apple's native 3D engine, the only path to visionOS.

Both: physically-based rendering, HDR lighting, dynamic shadows, 60fps on mid-range devices.

Production quality on both platforms.

---

**8/14 — AR on both platforms**

Android:
```
ARScene(planeRenderer = true) {
    AnchorNode(anchor = a) {
        ModelNode(modelInstance = sofa)
    }
}
```

iOS:
```
ARSceneView { anchor in
    ModelNode(named: "sofa.usdz")
}
```

Same concept: declare AR content, the framework handles tracking.

---

**9/14 — Physics on both platforms**

Android: pure Kotlin Euler integration
iOS: RealityKit's native Metal-accelerated physics

Same API pattern:
- PhysicsNode wraps any node
- Gravity, collision, bounce
- Works with any geometry or model

---

**10/14 — The killer feature: ViewNode (Android)**

Render ANY Compose UI inside 3D space.

A Card with price and "Buy Now" button floating next to an AR-placed product. A tooltip hovering over a 3D model.

No other mobile 3D library does this.

---

**11/14 — vs. the alternatives**

| | SceneView | Unity | RealityKit | Raw ARCore |
|---|---|---|---|---|
| Cross-platform | Android+Apple | All | Apple only | Android only |
| Declarative | Compose+SwiftUI | No | SwiftUI | No |
| Size | ~5 MB | 40-80 MB | N/A | ~1 MB |
| Open source | Apache 2.0 | No | No | No |

---

**12/14 — AI-first SDK**

SceneView ships with:
- llms.txt — machine-readable API reference
- MCP server — real-time API for AI assistants
- Both include Android AND iOS docs

Ask Claude "build me a 3D viewer" — it generates correct code for either platform.

---

**13/14 — The use case that matters most**

Most apps won't be "3D apps."

But replacing Image() with Scene {} on a product page? That's 10 extra lines for a noticeably better experience.

Works the same on Android and iOS now. One skill, both platforms.

---

**14/14 — Get started**

Android:
```
implementation("io.github.sceneview:sceneview:3.3.0")
```

iOS:
```
.package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")
```

15 Android samples. iOS examples. Full docs. MCP server.

Open source. Apache 2.0.

github.com/SceneView/sceneview

#AndroidDev #iOSDev #JetpackCompose #SwiftUI #3D #AR #Kotlin #Swift #SceneView #CrossPlatform

---

## Posting tips

- **Best time:** Tuesday-Thursday, 9-11 AM EST (US dev audience) or 3-5 PM CET (EU)
- **Thread format:** Post tweet 1, then reply chain for 2-14
- **Engagement:** Quote-tweet #1 with the code screenshot from tweet 3 (cross-platform comparison) for visual appeal
- **Pin:** Pin tweet 1 to your profile for the week
- **Cross-post:** Copy to Bluesky and Mastodon (Android AND iOS dev communities are active there)
- **Follow-up:** Reply to your own thread 24h later with a link to the Medium article
- **iOS angle:** Share tweets 3-4 in iOS developer communities separately
