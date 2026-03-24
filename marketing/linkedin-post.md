# LinkedIn Post — SceneView 3.3.0 Cross-Platform Announcement

Copy-paste the text below. Attach the video (sceneview-3.3.0-video.webm converted to MP4).

---

SceneView 3.3.0 is here — and it's now cross-platform.

3D & AR as Jetpack Compose composables on Android. 3D & AR as SwiftUI views on iOS, macOS, and visionOS.

Same concepts. Native on both platforms.

Android (Compose):
Scene(modifier = Modifier.fillMaxSize()) {
    ModelNode(rememberModelInstance(modelLoader, "model.glb"), scaleToUnits = 1.0f)
}

iOS (SwiftUI):
SceneView {
    ModelNode(named: "model.usdz")
        .scaleToUnits(1.0)
}

What's new in 3.3.0:

-> Cross-platform: Android + iOS + macOS + visionOS
-> 26+ node types on Android (Filament), 16 on iOS (RealityKit)
-> PhysicsNode, DynamicSkyNode, FogNode, TextNode — on both platforms
-> Same developer experience: nodes are declarative UI
-> AI-first SDK with MCP server for both platforms
-> Kotlin Multiplatform core shares math, collision, geometry, animation

The architecture: KMP shares logic, not rendering. Each platform uses its native renderer — Filament on Android, RealityKit on Apple. Best performance, native tooling, native debugging.

This is the only open-source, declarative-UI-native 3D/AR SDK for both Android and Apple platforms.

Android: implementation("io.github.sceneview:sceneview:3.3.0")
iOS: .package(url: "https://github.com/SceneView/sceneview", from: "3.3.0")

Open source. Apache 2.0.

-> github.com/SceneView/sceneview
-> sceneview.github.io

#Android #iOS #JetpackCompose #SwiftUI #3D #AR #AugmentedReality #OpenSource #AndroidDev #iOSDev #Kotlin #Swift #SceneView #Filament #RealityKit #ARCore #ARKit #CrossPlatform #visionOS
