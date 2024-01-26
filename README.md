# ![logo](https://github.com/SceneView/sceneview-android/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05)

### 3D and AR Android `@Composable` and layout view with Google Filament and ARCore

A Sceneform replacement in Kotlin and Jetpack Compose

[![Sceneview](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Sceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![ARSceneview](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=ARSceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)

[![Filament](https://img.shields.io/badge/Filament-v1.49.3-yellow)](https://github.com/google/filament)
[![ARCore](https://img.shields.io/badge/ARCore-v1.40.0-c961cb)](https://github.com/google-ar/arcore-android-sdk)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)

## Dependency

*app/build.gradle*

### 3D (Filament included)

```gradle
dependencies {
    // Sceneview
    implementation 'io.github.sceneview:sceneview:2.0.3'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)

### AR (Filament + ARCore included)

```gradle
dependencies {
    // ARSceneview
    implementation 'io.github.sceneview:arsceneview:2.0.3'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

## Usage

### 3D Model Viewer

```kotlin
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val environmentLoader = rememberEnvironmentLoader(engine)
Scene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    childNodes = rememberNodes {
        add(ModelNode(modelLoader.createModelInstance("model.glb")).apply {
            // Move the node 4 units in Camera front direction
            position = Position(z = -4.0f)
        })
    },
    environment = environmentLoader.createHDREnvironment("environment.hdr")!!
)
```
[Sample](https://github.com/SceneView/sceneview-android/tree/main/samples/model-viewer-compose)

### AR Model Viewer

```kotlin
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val model = modelLoader.createModel("model.glb")
var frame by remember { mutableStateOf<Frame?>(null) }
val childNodes = rememberNodes()
ARScene(
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    modelLoader = modelLoader,
    onSessionUpdated = { session, updatedFrame ->
        frame = updatedFrame
    },
    onGestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { motionEvent, node ->
            val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
            val anchor = hitResults?.firstOrNull {
                it.isValid(depthPoint = false, point = false)
            }?.createAnchorOrNull()

            if (anchor != null) {
                val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                anchorNode.addChildNode(
                    ModelNode(modelInstance = modelLoader.createInstance(model)!!)
                )
                childNodes += anchorNode
            }
        }
    )
)
```
[Sample](https://github.com/SceneView/sceneview-android/tree/main/samples/ar-model-viewer-compose)

## AR - Cloud Anchors

[Sources](https://github.com/Gebort/FESTU.Navigator)

```kotlin

sceneView.cloudAnchorEnabled = true

// Host/Record a Cloud Anchor
node.onAnchorChanged = { node: ArNode, anchor: Anchor? ->
    if (anchor != null) {
        node.hostCloudAnchor { anchor: Anchor, success: Boolean ->
            if (success) {
                // Save the hosted Cloud Anchor Id
                val cloudAnchorId = anchor.cloudAnchorId
            }
        }
    }
}

// Resolve/Restore the Cloud Anchor
node.resolveCloudAnchor(cloudAnchorId) { anchor: Anchor, success: Boolean ->
    if (success) {
        node.isVisible = true
    }
}
```

## AR Depth/Objects Occlusion

[![](https://markdown-videos.deta.dev/youtube/bzyoR3ugGFA)](https://www.youtube.com/watch?v=bzyoR3ugGFA)

```kotlin
sceneView.isDepthOcclusionEnabled = true
```

This will process the incoming ARCore `DepthImage` to occlude virtual objects behind real world
objects.  
If the AR `Session` is not configured properly the standard camera material is used.  
Valid `Session.Config` for the Depth occlusion are `Config.DepthMode.AUTOMATIC`
and `Config.DepthMode.RAW_DEPTH_ONLY`  
Disable this value to apply the standard camera material to the CameraStream.

## AR Geospatial API

[![](https://markdown-videos.deta.dev/youtube/QZYg9WU5wSA)](https://www.youtube.com/watch?v=QZYg9WU5wSA)

Follow
the [official developer guide](https://developers.google.com/ar/develop/java/geospatial/developer-guide)
to enable Geospatial in your application. For configuring the ARCore session, you just need to
enable
Geospatial via ArSceneView.

- Enable Geospatial via ArSceneView

```kotlin
arSceneView.geospatialEnabled = true
```

- Create an Anchor

```kotlin
val earth = arSceneView.session?.earth ?: return
if (earth.trackingState == TrackingState.TRACKING) {
    // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
    val altitude = earth.cameraGeospatialPose.altitudeMeters - 1
    val rotation = Rotation(0f, 0f, 0f)
    // Put the anchor somewhere around the user.
    val latitude = earth.cameraGeospatialPose.latitude + 0.0004
    val longitude = earth.cameraGeospatialPose.longitude + 0.0004
    earthAnchor = earth.createAnchor(latitude, longitude, altitude, rotation)
}
// Attach the anchor to the arModelNode.
arModelNode.anchor = earthAnchor
```

## Camera Permission and ARCore install/update/unavailable

`ArSceneView` automatically handles the camera permission prompt and the ARCore requirements checks.
Everything is proceed when the attached view Activity/Fragment is resumed but you can also add
your `ArSceneView` at any time, the prompt will then occure when first `addView(arSceneView)` is
called.

If you need it, you can add a listener on both ARCore success or failed session creation (including
camera permission denied since a session cannot be created without it)

- Camera permission has been granted and latest ARCore Services version are already installed or
  have been installed during the auto check

```kotlin
sceneView.onArSessionCreated = { arSession: ArSession ->
}
```

- Handle a fallback in case of camera permission denied or AR unavailable and possibly move to 3D
  only usage

```kotlin
sceneView.onArSessionFailed = { exception: Exception ->
    // If AR is not available, we add the model directly to the scene for a 3D only usage
    sceneView.addChild(modelNode)
}
```

The exception contains the failure reason. *e.g. SecurityException in case of camera permission
denied*

## Features

- Use `sceneview` dependency for 3D only or `arsceneview` for 3D and ARCore.
- Compose: Use the `Scene` or `ARScene` `@Composable`
- Layout: Add the `<SceneView>` or `<ArSceneView>` tag to your layout or call
  the `ArSceneview(context: Context)` constructor in your code.
- Requesting the camera permission and installing/updating the Google Play Services for AR is
  handled automatically in the `ArSceneView`.
- Support for the latest ARCore features (the upcoming features will be integrated quicker thanks to
  Kotlin).
- Lifecycle-aware components = Better memory management and performance.
- Resources are loaded using coroutines launched in the `LifecycleCoroutineScope` of the `SceneView`
  /`ArSceneView`. This means that loading is started when the view is created and cancelled when it
  is destroyed.
- Multiple instances are now possible.
- Much easier to use. For example, the local and world `position`, `rotation` and `scale` of
  the `Node` are now directly accessible without creating ~`Vector3`~ objects (`position.x = 1f`
  , `rotation = Rotation(90f, 180f, 0f)`, `scale = Scale(0.5f)`, etc.).

## Architecture

[![](https://markdown-videos.deta.dev/youtube/00vj8AttWO4)](https://www.youtube.com/watch?v=00vj8AttWO4)

## Why have we included the Kotlin-Math library in SceneView?

Earlier versions of OpenGL had a fixed rendering pipeline and provided an API for setting positions
of vertices, transformation and projection matrices, etc. However, with the new rendering pipeline
it is required to prepare this data before passing it to GLSL shaders and OpenGL doesn't provide any
mathematical functions to do that.

It is possible to implement the required functions yourself like
in [Sceneform](https://github.com/SceneView/sceneform-android) or use an existing library. For
example, C++ supports operator overloading and benefits from the
excellent [GLM library](https://glm.g-truc.net/0.9.9/) that allows to use the same syntax and
features as GLSL.

We use the [Kotlin-Math library](https://github.com/romainguy/kotlin-math) to rely on a well-tested
functions and get an advantage of using Kotlin operators for vector, matrix and quaternion
operations too.

## Support our work

- [Buy a SceneView T-Shirt](https://sceneview.threadless.com/designs/sceneview)

[![Shop](https://user-images.githubusercontent.com/6597529/229289239-beabba4a-b368-4667-b68a-b49b9729cd56.png)](https://sceneview.threadless.com/designs/sceneview)
[![Shop](https://user-images.githubusercontent.com/6597529/229322274-1842af45-a328-4b8c-b51a-9fc2402c1fc8.png)](https://sceneview.threadless.com/designs/sceneview)

- [Contribute $9.99](https://opencollective.com/sceneview/contribute/say-thank-you-ask-a-question-ask-for-features-and-fixes-33651)

[![Open Collective](https://user-images.githubusercontent.com/6597529/229289721-bdecf986-1b83-46bd-92cb-433114f03429.png)](https://opencollective.com/sceneview)
