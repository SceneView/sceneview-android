# SceneView Android

### 3D and AR Android Composable and View with Google Filament and ARCore

This is a Sceneform replacement in Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)

## Dependency

*app/build.gradle*

- 3D (Filament included)

```gradle
dependencies {
    // 3D only
    implementation 'io.github.sceneview:sceneview:1.2.2'
}
```

[API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)

- AR (Filament + ARCore included)

```gradle
dependencies {
    // 3D and ARCore
    implementation 'io.github.sceneview:arsceneview:1.2.2'
}
```

[API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

## Usage

### 3D

- Compose

```kotlin
@Composable
fun ModelScreen() {
    val nodes = rememberNodes()

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier,
            activity = LocalContext.current as? ComponentActivity,
            lifecycle = LocalLifecycleOwner.current.lifecycle,
            /**
            * List of the scene's nodes that can be linked to a `mutableStateOf<List<Node>>()`
            */
            childNodes = nodes,
            /**
            * Provide your own instance if you want to share Filament resources between multiple views.
            */
            engine = rememberEngine(),
            /**
            * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
            * a bundle of Filament textures, vertex buffers, index buffers, etc.
            *
            * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
            */
            modelLoader = rememberModelLoader(engine),
            /**
            * A Filament Material defines the visual appearance of an object.
            *
            * Materials function as a templates from which [MaterialInstance]s can be spawned.
            */
            materialLoader = rememberMaterialLoader(engine),
            /**
            * Provide your own instance if you want to share [Node]s' scene between multiple views.
            */
            scene = rememberScene(engine),
            /**
             * Encompasses all the state needed for rendering a {@link Scene}.
             *
             * [View] instances are heavy objects that internally cache a lot of data needed for
             * rendering. It is not advised for an application to use many View objects.
             *
             * For example, in a game, a [View] could be used for the main scene and another one for the
             * game's user interface. More <code>View</code> instances could be used for creating special
             * effects (e.g. a [View] is akin to a rendering pass).
             */
            view = rememberView(engine),
            /**
            * A [Renderer] instance represents an operating system's window.
            *
            * Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
            * commands for the render thread and manages frame latency.
            */
            renderer = rememberRenderer(engine),
            /**
             * Represents a virtual camera, which determines the perspective through which the scene is
             * viewed.
             *
             * All other functionality in Node is supported. You can access the position and rotation of the
             * camera, assign a collision shape to it, or add children to it.
             */
            camera = rememberCamera(engine),
            /**
             * Always add a direct light source since it is required for shadowing.
             *
             * We highly recommend adding an [IndirectLight] as well.
             */
            mainLight = rememberMainLight(engine),
            /**
             * IndirectLight is used to simulate environment lighting.
             *
             * Environment lighting has a two components:
             * - irradiance
             * - reflections (specular component)
             *
             * @see IndirectLight
             * @see Scene.setIndirectLight
             */
            indirectLight = rememberIndirectLight(engine),
            /**
             * The Skybox is drawn last and covers all pixels not touched by geometry.
             *
             * When added to a [SceneView], the `Skybox` fills all untouched pixels.
             *
             * The Skybox to use to fill untouched pixels, or null to unset the Skybox.
             *
             * @see Skybox
             * @see Scene.setSkybox
             */
            skybox = rememberSkybox(engine),
            /**
             * Invoked when an frame is processed.
             *
             * Registers a callback to be invoked when a valid Frame is processing.
             *
             * The callback to be invoked once per frame **immediately before the scene is updated.
             *
             * The callback will only be invoked if the Frame is considered as valid.
             */
            onFrame = null,
            /**
             * Invoked when the `SceneView` is tapped.
             *
             * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
             * used to find a touched node. The ID of the Filament renderable can be used to determine what
             * part of a model is tapped.
             */
            onTap = null,
            onCreate = null
        )
    }
}
```

- Layout

```xml

<io.github.sceneview.SceneView android:id="@+id/sceneView" android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### AR

- Compose

```kotlin
@Composable
fun ARScreen() {
    val nodes = rememberNodes()
  
    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
          modifier = Modifier,
          activity = LocalContext.current as? ComponentActivity,
          lifecycle = LocalLifecycleOwner.current.lifecycle,
          /**
           * List of the scene's nodes that can be linked to a `mutableStateOf<List<Node>>()`
           */
          childNodes = nodes,
          /**
           * Provide your own instance if you want to share Filament resources between multiple views.
           */
          engine = rememberEngine(),
          /**
           * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
           * a bundle of Filament textures, vertex buffers, index buffers, etc.
           *
           * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
           */
          modelLoader = rememberModelLoader(engine),
          /**
           * A Filament Material defines the visual appearance of an object.
           *
           * Materials function as a templates from which [MaterialInstance]s can be spawned.
           */
          materialLoader = rememberMaterialLoader(engine),
          /**
           * Provide your own instance if you want to share [Node]s' scene between multiple views.
           */
          scene = rememberScene(engine),
          /**
           * Encompasses all the state needed for rendering a {@link Scene}.
           *
           * [View] instances are heavy objects that internally cache a lot of data needed for
           * rendering. It is not advised for an application to use many View objects.
           *
           * For example, in a game, a [View] could be used for the main scene and another one for the
           * game's user interface. More <code>View</code> instances could be used for creating special
           * effects (e.g. a [View] is akin to a rendering pass).
           */
          view = rememberView(engine),
          /**
           * A [Renderer] instance represents an operating system's window.
           *
           * Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
           * commands for the render thread and manages frame latency.
           */
          renderer = rememberRenderer(engine),
          /**
           * Represents a virtual camera, which determines the perspective through which the scene is
           * viewed.
           *
           * All other functionality in Node is supported. You can access the position and rotation of the
           * camera, assign a collision shape to it, or add children to it.
           */
          camera = rememberARCamera(engine),
          /**
           * Always add a direct light source since it is required for shadowing.
           *
           * We highly recommend adding an [IndirectLight] as well.
           */
          mainLight = rememberMainLight(engine),
          /**
           * IndirectLight is used to simulate environment lighting.
           *
           * Environment lighting has a two components:
           * - irradiance
           * - reflections (specular component)
           *
           * @see IndirectLight
           * @see Scene.setIndirectLight
           */
          indirectLight = rememberIndirectLight(engine),
          /**
           * The Skybox is drawn last and covers all pixels not touched by geometry.
           *
           * When added to a [SceneView], the `Skybox` fills all untouched pixels.
           *
           * The Skybox to use to fill untouched pixels, or null to unset the Skybox.
           *
           * @see Skybox
           * @see Scene.setSkybox
           */
          skybox = rememberSkybox(engine),
          /**
           * Invoked when an frame is processed.
           *
           * Registers a callback to be invoked when a valid Frame is processing.
           *
           * The callback to be invoked once per frame **immediately before the scene is updated.
           *
           * The callback will only be invoked if the Frame is considered as valid.
           */
          sessionFeatures = setOf<Session.Feature>(),
          cameraConfig = null,
          planeRenderer = true,
          /**
           * The [ARCameraStream] to render the camera texture.
           *
           * Use it to control if the occlusion should be enabled or disabled
           */
          cameraStream = rememberCameraStream(engine, materialLoader),
          onSessionConfiguration = null,
          onSessionCreated = null,
          /**
           * Updates of the state of the ARCore system.
           *
           * Callback for [onSessionUpdated].
           *
           * This includes: receiving a new camera frame, updating the location of the device, updating
           * the location of tracking anchors, updating detected planes, etc.
           *
           * This call may update the pose of all created anchors and detected planes. The set of updated
           * objects is accessible through [Frame.getUpdatedTrackables].
           *
           * Invoked once per [Frame] immediately before the Scene is updated.
           */
          onSessionUpdate = null,
          onSessionResumed = null,
          /**
           * Invoked when an ARCore error occurred.
           *
           * Registers a callback to be invoked when the ARCore Session cannot be initialized because
           * ARCore is not available on the device or the camera permission has been denied.
           */
          onSessionFailed = null,
          onSessionConfigChanged = null,
          onTap = null,
          /**
           * Invoked when an ARCore trackable is tapped.
           *
           * Depending on the session configuration the [HitResult.getTrackable] can be:
           * - A [Plane] if [Config.setPlaneFindingMode] is enable.
           * - An [InstantPlacementPoint] if [Config.setInstantPlacementMode] is enable.
           * - A [DepthPoint] and [Point] if [Config.setDepthMode] is enable.
           */
          onTapAR = null,
          onCreate = null 
        )
    }
}
```

- Layout

```xml

<io.github.sceneview.ar.ArSceneView android:id="@+id/sceneView" android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## 3D - Model Viewer

[![](https://markdown-videos.deta.dev/youtube/GDCy_bUdggg)](https://www.youtube.com/watch?v=GDCy_bUdggg)

```kotlin
ModelNode(
  modelInstance = modelLoader.createModelInstance("myModel.glb"),
  autoAnimate = true,
  scaleToUnits = null,
  centerOrigin = null
)
```

## AR - Anchor Node on Tap

```kotlin
val engine = rememberEngine()
val modelLoader = rememberModelLoader(engine)
val childNodes = rememberNodes()
ARScene(
    modifier = Modifier.fillMaxSize(),
    childNodes = childNodes,
    engine = engine,
    modelLoader = modelLoader,
    onTapAR = { motionEvent: MotionEvent, hitResult: HitResult ->
        childNodes += AnchorNode(
            engine = engine,
            anchor = hitResult.createAnchor()
        ).apply {
            // Make the anchor node editable for AR moving it
            isEditable = true
        }.addChildNode(
            ModelNode(
                modelInstance = modelLoader.createModelInstance(rawResId = R.raw.my_model),
                // Scale to fit in a 0.5 meters cube
                scaleToUnits = 0.5f,
                // Bottom origin instead of center so the model base is on the floor
                centerOrigin = Position(y = -1.0f)
            ).apply {
                // Make the node editable for rotation and scale
                isEditable = true
            }
        )
    }
)
```

## AR - Cloud Anchors

[![](https://markdown-videos.deta.dev/youtube/iptk8jsWyw4)](https://www.youtube.com/watch?v=iptk8jsWyw4)

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

