# LinkedIn Post Copy

*Multiple variants — pick the one that fits the content you're posting with*

---

## Variant A — With the video

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

→ github.com/SceneView/sceneview-android
Open source · MIT · Filament · ARCore · Compose

#AndroidDev #JetpackCompose #AR #3D #SceneView #Kotlin #MobileDev
```

---

## Variant B — Code drop (text post)

```
Here's something that used to take 500 lines of Android boilerplate:

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let {
        ModelNode(modelInstance = it, scaleToUnits = 1.0f, autoAnimate = true)
    }
}
```

That's a photorealistic 3D model with HDR lighting and orbit/zoom camera gestures.

Same for AR:

```kotlin
ARScene(planeRenderer = true, onSessionUpdated = { _, frame ->
    anchor = frame.getUpdatedPlanes().firstOrNull()
        ?.let { frame.createAnchorOrNull(it.centerPose) }
}) {
    anchor?.let { AnchorNode(anchor = it) {
        ModelNode(modelInstance = helmet, scaleToUnits = 0.3f)
    }}
}
```

Anchor is state. When the plane is detected, Compose recomposes, the model appears.

SceneView 3.0 is a ground-up rewrite of the library around a single idea:
3D is just more Compose UI.

→ github.com/SceneView/sceneview-android

#AndroidDev #JetpackCompose #Kotlin #AR #3D #OpenSource
```

---

## Variant C — The subtle 3D angle (for product/UX focused audience)

```
Most 3D demos show a rotating helmet on a black background.

That's impressive. But the real opportunity is different:

What if 3D was easy enough to add to a screen where it's not the main feature?

→ Your product page shows a static PNG. Replace it with a 3D model the user can orbit.
→ Your profile has a flat avatar. Give it a subtle breathing animation.
→ Your dashboard has 2D charts. One of them is a rotating sphere now.

SceneView 3.0 makes this ~10 extra lines per screen.
You don't build an AR app. You just... add depth where it fits.

```kotlin
Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
    Scene(modifier = Modifier.fillMaxSize()) {
        ModelNode(modelInstance = productModel, scaleToUnits = 1.0f)
    }
    // Your existing Compose UI overlays on top — unchanged
    PriceTag(modifier = Modifier.align(Alignment.BottomStart))
}
```

→ github.com/SceneView/sceneview-android — open source, MIT

#AndroidDev #UX #ProductDesign #JetpackCompose #3D #MobileUI
```

---

## Hashtags to always include

```
#AndroidDev #JetpackCompose #Kotlin #ARCore #Filament #3D #AR #OpenSource #SceneView #MobileDev
```

## Best posting time

LinkedIn: Tuesday–Thursday, 8–10am or 12–1pm in your target timezone.
Video posts outperform image+text by ~3–5× in reach on LinkedIn — post the video first.
