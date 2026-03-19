# LinkedIn Post — SceneView 3.0 Video Announcement

Copy-paste the text below. Attach the video (sceneview-3.0-video.webm converted to MP4).

---

Introducing SceneView 3.0 — 3D & AR as Jetpack Compose composables.

After years of development, I'm thrilled to announce SceneView 3.0: a ground-up rewrite built entirely around Jetpack Compose.

What if adding a 3D model viewer to your Android app was as simple as writing a composable?

Scene(
    modifier = Modifier.fillMaxSize(),
    cameraManipulator = rememberCameraManipulator()
) {
    ModelNode(
        rememberModelInstance(modelLoader, "model.glb"),
        scaleToUnits = 1.0f,
        autoAnimate = true
    )
}

That's it. Full 3D viewer with orbit camera. No boilerplate.

What's new in 3.0:

→ Scene { } for 3D, ARScene { } for AR
→ Nodes are composables — declare them, don't manage them
→ State drives the scene — recomposition handles updates
→ Lifecycle is automatic — no manual cleanup
→ ViewNode: embed Compose UI directly in 3D/AR space
→ MCP server for AI-assisted development

Built on Google Filament for rendering and ARCore for augmented reality.
Open source · MIT licensed.

implementation("io.github.sceneview:sceneview:3.0.0")
implementation("io.github.sceneview:arsceneview:3.0.0")

It's not a wrapper. It's not a bridge. It's just Compose.

→ github.com/SceneView

#Android #JetpackCompose #3D #AR #AugmentedReality #OpenSource #AndroidDev #Kotlin #SceneView #Filament #ARCore
