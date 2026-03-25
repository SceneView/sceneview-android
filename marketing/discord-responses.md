# Discord Responses — Draft for Thomas

## 1. Kamil (23 mars 2026) — v3.2.0 not on Maven Central

Hey Kamil! The latest stable version on Maven Central is **v3.3.0**, not 3.2.0. Here's the correct dependency:

```kotlin
implementation("io.github.sceneview:sceneview:3.3.0")
// or for AR:
implementation("io.github.sceneview:arsceneview:3.3.0")
```

If you've seen references to v3.4.x, those are available as GitHub Releases but haven't been published to Maven Central yet — that's coming soon. For now, stick with 3.3.0 and you'll be all set. Let me know if you run into anything else!

---

## 2. cloo (11 mars 2026) — ARCore+Filament from scratch, mentions Claude

Welcome to the SceneView community! That's seriously impressive that you built an ARCore+Filament integration from scratch — you know firsthand how much boilerplate that involves (Engine, Renderer, SwapChain, View, Scene, ARCore Session lifecycle...). SceneView handles all of that for you so you can focus on what matters: your actual 3D/AR content.

With SceneView, that whole setup becomes a single `ARScene { }` composable in Jetpack Compose. No manual engine management, no SwapChain headaches.

And since you mentioned Claude — we actually have an MCP server (`@anthropic/sceneview-mcp`) that gives Claude deep knowledge of the SceneView API. So you can literally ask Claude to build AR features and it generates correct, working code on the first try. Check it out at https://sceneview.github.io

Would love to see what you build with it!

---

## 3. An3dy (6 jan 2026) — Manipulator.Mode.MAP issues, sparse docs

Hey An3dy, you're right — the docs for camera manipulator modes are definitely lacking, and that's on us. We're actively working on improving the documentation.

Quick explanation: `MAP` mode provides a flat pan/zoom behavior (like a 2D map), as opposed to `ORBIT` which rotates around a target point. If you're working with a standard 3D scene, **ORBIT is the recommended default** and should give you the most intuitive controls out of the box.

If you specifically need MAP-style navigation (top-down view, no rotation), make sure your camera is set up looking straight down and that you're not conflicting with gesture overrides.

The new docs site at https://sceneview.github.io has more examples and recipes. We're adding more content regularly — your feedback helps us prioritize what to document next.

---

## 4. xavier.seignard (2 dec 2025) — virtual try-on glasses, face occlusion

That's an awesome use case! Virtual try-on for glasses is one of the best AR applications out there.

For face occlusion (making the glasses appear behind the ears/hair), the approach is to use a **FaceNode** that renders the face mesh as an occluder (a material that writes to the depth buffer but isn't visible). This way, parts of your glasses model that should be hidden behind the face geometry get properly occluded.

In v3.4.2 we regenerated the AR materials, which should improve occlusion behavior. I'd recommend trying the latest version — the face mesh handling is more reliable now.

The key setup:
1. Use `ARScene` with face tracking configuration
2. Add a `FaceNode` with the occlusion material for the face mesh
3. Attach your glasses model as a child node positioned on the face anchor

Let me know how it goes — would love to see a demo!

---

## 5. Nam (11 dec 2025) — zoom speed in View mode

Hey Nam! Just a heads-up: the View-based API (`SceneView` as an Android View) is deprecated in favor of the Jetpack Compose API. All active development and improvements are happening on the Compose side, so I'd really recommend migrating when you get a chance — it's a much better developer experience overall.

For the zoom speed specifically, if you're still on the View API, you can adjust it through the camera manipulator's scroll settings. But honestly, the Compose API gives you much more control over gestures and camera behavior with less code.

Migration is straightforward — instead of XML layout + View binding, you use:

```kotlin
Scene {
    // your nodes here
}
```

Check https://sceneview.github.io for updated examples. Happy to help with the migration if you need it!
