# ![logo](https://github.com/SceneView/sceneview-android/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05)

# 3D and AR Android Jetpack Compose and Layout View based on Google Filament and ARCore

[![Sceneview](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Sceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![ARSceneview](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=ARSceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)

[![Filament](https://img.shields.io/badge/Filament-v1.51.0-yellow)](https://github.com/google/filament)
[![ARCore](https://img.shields.io/badge/ARCore-v1.42.0-c961cb)](https://github.com/google-ar/arcore-android-sdk)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)


# 3D - Scene (Filament)

### Dependency
*app/build.gradle*
```gradle
dependencies {
    // Sceneview
    implementation("io.github.sceneview:sceneview:2.2.1")
}
```

### Usage
```kotlin
// An Engine instance main function is to keep track of all resources created by the user and manage
// the rendering thread as well as the hardware renderer.
// To use filament, an Engine instance must be created first.
val engine = rememberEngine()
// Encompasses all the state needed for rendering a [Scene].
// [View] instances are heavy objects that internally cache a lot of data needed for
// rendering. It is not advised for an application to use many View objects.
// For example, in a game, a [View] could be used for the main scene and another one for the
// game's user interface. More [View] instances could be used for creating special
// effects (e.g. a [View] is akin to a rendering pass).
val view = rememberView(engine)
// A [Renderer] instance represents an operating system's window.
// Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
// commands for the render thread and manages frame latency.
val renderer = rememberRenderer(engine),
// Provide your own instance if you want to share [Node]s' scene between multiple views.
val scene = rememberScene(engine)
// Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
// a bundle of Filament textures, vertex buffers, index buffers, etc.
// A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
val modelLoader = rememberModelLoader(engine)
// A Filament Material defines the visual appearance of an object.
// Materials function as a templates from which [MaterialInstance]s can be spawned.
val materialLoader = rememberMaterialLoader(engine)
// Utility for decoding an HDR file or consuming KTX1 files and producing Filament textures,
// IBLs, and sky boxes.
// KTX is a simple container format that makes it easy to bundle miplevels and cubemap faces
// into a single file.
val environmentLoader = rememberEnvironmentLoader(engine)
// Physics system to handle collision between nodes, hit testing on a nodes,...
val collisionSystem = rememberCollisionSystem(view)

Scene(
    // The modifier to be applied to the layout.
    modifier = Modifier.fillMaxSize(),
    engine = engine,
    view = view,
    renderer = renderer,
    scene = scene,
    modelLoader = modelLoader,
    materialLoader = materialLoader,
    environmentLoader = environmentLoader,
    collisionSystem = collisionSystem,
    // Controls whether the render target (SurfaceView) is opaque or not.
    isOpaque = true,
    // Always add a direct light source since it is required for shadowing.
    // We highly recommend adding an [IndirectLight] as well.
    mainLightNode = rememberMainLightNode(engine) {
        intensity = 100_000.0f
    },
    // Load the environement lighting and skybox from an .hdr asset file
    environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment(
            assetFileLocation = "environments/sky_2k.hdr"
        )!!
    },
    // Represents a virtual camera, which determines the perspective through which the scene is
    // viewed.
    // All other functionality in Node is supported. You can access the position and rotation of the
    // camera, assign a collision shape to it, or add children to it.
    cameraNode = rememberCameraNode(engine) {
        // Position the camera 4 units away from the object
        position = Position(z = 4.0f)
    },
    // Helper that enables camera interaction similar to sketchfab or Google Maps.
    // Needs to be a callable function because it can be reinitialized in case of viewport change
    // or camera node manual position changed.
    // The first onTouch event will make the first manipulator build. So you can change the camera
    // position before any user gesture.
    // Clients notify the camera manipulator of various mouse or touch events, then periodically
    // call its getLookAt() method so that they can adjust their camera(s). Three modes are
    // supported: ORBIT, MAP, and FREE_FLIGHT. To construct a manipulator instance, the desired mode
    // is passed into the create method.
    cameraManipulator = rememberCameraManipulator(),
    // Scene nodes
    childNodes = rememberNodes {
        // Add a glTF model
        add(
            ModelNode(
                // Load it from a binary .glb in the asset files
                modelInstance = modelLoader.createModelInstance(
                    assetFileLocation = "models/damaged_helmet.glb"
                ),
                scaleToUnits = 1.0f
            )
        )
        // Add a Cylinder geometry
        add(CylinderNode(
            engine = engine,
            radius = 0.2f,
            height = 2.0f,
            // Choose the basic material appearance
            materialInstance = materialLoader.createColorInstance(
                color = Color.Blue,
                metallic = 0.5f,
                roughness = 0.2f,
                reflectance = 0.4f
            )
        ).apply {
            // Position it on top of the model and rotate it
            transform(
                position = Position(y = 1.0f),
                rotation = Rotation(x = 90.0f)
            )
        })
        // ...See all available nodes in the nodes packagage
    },
    // The listener invoked for all the gesture detector callbacks.
    // Detects various gestures and events.
    // The gesture listener callback will notify users when a particular motion event has occurred.
    // Responds to Android touch events with listeners.
    onGestureListener = rememberOnGestureListener(
        onDoubleTapEvent = { event, tapedNode ->
            // Scale up the tap node (if any) on double tap
            tapedNode?.let { it.scale *= 2.0f }
        }),
    // Receive basics on touch event on the view
    onTouchEvent = { event: MotionEvent, hitResult: HitResult? ->
        hitResult?.let { println("World tapped : ${it.worldPosition}") }
        // The touch event is not consumed
        false
    },
    // Invoked when an frame is processed.
    // Registers a callback to be invoked when a valid Frame is processing.
    // The callback to be invoked once per frame **immediately before the scene is updated.
    // The callback will only be invoked if the Frame is considered as valid.
    onFrame = { frameTimeNanos ->
    }
)
```

### Samples
- [Model Viewer Compose](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/model-viewer-compose)
- [Model Viewer Layout](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/model-viewer)

### Documentation
[3D API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)

---

## AR - ARScene (Filament + ARCore)

### Dependency
*app/build.gradle*
```gradle
dependencies {
    // ARSceneview
    implementation 'io.github.sceneview:arsceneview:2.2.1'
}
```

### Usage

```kotlin
ARScene(
    
    //...
    //  Everything from a Scene
    //...
    
    // Fundamental session features that can be requested.
    sessionFeatures = setOf(),
    // The camera config to use.
    // The config must be one returned by [Session.getSupportedCameraConfigs].
    // Provides details of a camera configuration such as size of the CPU image and GPU texture.
    sessionCameraConfig = null,
    // Configures the session and verifies that the enabled features in the specified session config
    // are supported with the currently set camera config.
    sessionConfiguration = { session, config ->
        config.depthMode =
            when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                true -> Config.DepthMode.AUTOMATIC
                else -> Config.DepthMode.DISABLED
            }
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.lightEstimationMode =
            Config.LightEstimationMode.ENVIRONMENTAL_HDR
    },
    planeRenderer = true,
    // The [ARCameraStream] to render the camera texture.
    // Use it to control if the occlusion should be enabled or disabled.
    cameraStream = rememberARCameraStream(materialLoader),
    // The session is ready to be accessed.
    onSessionCreated = { session ->
    },
    // The session has been resumed.
    onSessionResumed = { session ->
    },
    // The session has been paused
    onSessionPaused = { session ->
    },
    // Updates of the state of the ARCore system.
    // This includes: receiving a new camera frame, updating the location of the device, updating
    // the location of tracking anchors, updating detected planes, etc.
    // This call may update the pose of all created anchors and detected planes. The set of updated
    // objects is accessible through [Frame.getUpdatedTrackables].
    // Invoked once per [Frame] immediately before the Scene is updated.
    onSessionUpdated = { session, updatedFrame ->
    },
    // Invoked when an ARCore error occurred.
    // Registers a callback to be invoked when the ARCore Session cannot be initialized because
    // ARCore is not available on the device or the camera permission has been denied.
    onSessionFailed = { exception ->
    },
    // Listen for camera tracking failure.
    // The reason that [Camera.getTrackingState] is [TrackingState.PAUSED] or `null` if it is
    // [TrackingState.TRACKING]
    onTrackingFailureChanged = { trackingFailureReason ->
    }
)
```

### Samples
- [AR Augmented Image](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/ar-augmented-image)
- [AR Cloud Anchors](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/ar-cloud-anchor)
- [AR Model Viewer Compose](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/ar-model-viewer-compose)
- [AR Model Viewer Layout](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/ar-model-viewer)
- [AR Point Cloud](https://github.com/SceneView/sceneview-android/tree/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/ar-point-cloud)

### Documentation
[AR API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

---

## Links

### Website
[SceneView Website](https://sceneview.github.io/)

### Discord
[Sceneview Server](https://discord.gg/UbNDDBTNqb)

### Videos
[SceneView on Youtube](https://www.youtube.com/results?search_query=Sceneview+android)

## Repositories
- [Filament](https://github.com/google/filament)
- [ARCore](https://github.com/google-ar/arcore-android-sdk)

## Contribute
- [Open Collective](https://opencollective.com/sceneview)
- [GitHub Sponsor](https://github.com/sponsors/ThomasGorisse)

## Support our work

### Help us to
- Buy devices to test the SDK on
- Buy equipment for decent video recording Tutorials and Presentations
- Pay Sceneview Hosting Fees

### How To Contribute
- [Send $9.99 on Open Collective (CB, Paypal, Google Pay,..)](https://opencollective.com/sceneview/contribute/say-thank-you-ask-a-question-ask-for-features-and-fixes-33651)
- [Buy a SceneView T-Shirt](https://sceneview.threadless.com/designs/sceneview)

[![Shop](https://user-images.githubusercontent.com/6597529/229289239-beabba4a-b368-4667-b68a-b49b9729cd56.png)](https://sceneview.threadless.com/designs/sceneview)
[![Shop](https://user-images.githubusercontent.com/6597529/229322274-1842af45-a328-4b8c-b51a-9fc2402c1fc8.png)](https://sceneview.threadless.com/designs/sceneview)
- Create a Pull Request

[![Open Collective](https://user-images.githubusercontent.com/6597529/229289721-bdecf986-1b83-46bd-92cb-433114f03429.png)](https://opencollective.com/sceneview)

---
⚠️ Geospatial API: Be sure to follow the official [Google Geospatial Developer guide](https://developers.google.com/ar/develop/java/geospatial/developer-guide)
to enable Geospatial API in your application.
