# SceneView Android
### 3D and AR Android Composable and View with Google Filament and ARCore

This is a Sceneform replacement in Kotlin

[![Maven Central - SceneView](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Maven%20Central%20-%20SceneView)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=Maven%20Central%20-%20ARSceneView)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)

## Support our work

- [Buy a SceneView T-Shirt](https://sceneview.threadless.com/designs/sceneview)

[![Shop](https://user-images.githubusercontent.com/6597529/229289239-beabba4a-b368-4667-b68a-b49b9729cd56.png)](https://sceneview.threadless.com/designs/sceneview)
[![Shop](https://user-images.githubusercontent.com/6597529/229322274-1842af45-a328-4b8c-b51a-9fc2402c1fc8.png)](https://sceneview.threadless.com/designs/sceneview)

- [Contribute $9.99](https://opencollective.com/sceneview/contribute/say-thank-you-ask-a-question-ask-for-features-and-fixes-33651)

[![Open Collective](https://user-images.githubusercontent.com/6597529/229289721-bdecf986-1b83-46bd-92cb-433114f03429.png)](https://opencollective.com/sceneview)

## Features

- Use `sceneview` dependency for 3D only or `arsceneview` for 3D and ARCore.
- Compose: Use the `Scene` or `ARScene` `@Composable` 
- Layout: Add the `<SceneView>` or `<ArSceneView>` tag to your layout or call the `ArSceneview(context: Context)` constructor in your code.
- Requesting the camera permission and installing/updating the Google Play Services for AR is handled automatically in the `ArSceneView`.
- Support for the latest ARCore features (the upcoming features will be integrated quicker thanks to Kotlin).
- Lifecycle-aware components = Better memory management and performance.
- Resources are loaded using coroutines launched in the `LifecycleCoroutineScope` of the `SceneView`/`ArSceneView`. This means that loading is started when the view is created and cancelled when it is destroyed.
- Multiple instances are now possible.
- Much easier to use. For example, the local and world `position`, `rotation` and `scale` of the `Node` are now directly accessible without creating ~`Vector3`~ objects (`position.x = 1f`, `rotation = Rotation(90f, 180f, 0f)`, `scale = Scale(0.5f)`, etc.).

## Architecture

[![](https://markdown-videos.deta.dev/youtube/00vj8AttWO4)](https://www.youtube.com/watch?v=00vj8AttWO4)

## Dependency

*app/build.gradle*
- 3D (Filament included)
```gradle
dependencies {
    // 3D only
    implementation 'io.github.sceneview:sceneview:1.0.10'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)

- AR (Filament + ARCore included)
```gradle
dependencies {
    // 3D and ARCore
    implementation 'io.github.sceneview:arsceneview:0.10.0'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

## Usage

### 3D
- Compose
```kotlin
@Composable
fun ModelScreen() {
    val nodes = remember { mutableStateListOf<Node>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            onCreate = { sceneView ->
                // Apply your configuration
            }
        )
    }
}
```
- Layout
```xml
<io.github.sceneview.SceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### AR

- Compose
```kotlin
@Composable
fun ARScreen() {
    val nodes = remember { mutableStateListOf<ArNode>() }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            planeRenderer = true,
            onCreate = { arSceneView ->
              // Apply your configuration
            },
            onSessionCreate = { session ->
              // Configure the ARCore session
            },
            onFrame = { arFrame ->
              // Retrieve ARCore frame update
            },
            onTap = { hitResult ->
              // User tapped in the AR view
            }
        )
    }
}
```
- Layout
```xml
<io.github.sceneview.ar.ArSceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## 3D Model Viewer

[![](https://markdown-videos.deta.dev/youtube/GDCy_bUdggg)](https://www.youtube.com/watch?v=GDCy_bUdggg)

```kotlin
ModelNode(
    position = Position(x = 0.0f, y = 0.0f, z = -4.0f),
    rotation = Rotation(y = 90.0f),
    scale = Scale(0.5f)
)
```

#### Parameters

- `position` The node position to locate it within the coordinate system of its parent  
Default is `Position(x = 0.0f, y = 0.0f, z = 0.0f)`, indicating that the node is placed at the origin of the parent node's coordinate system.  
![image](https://user-images.githubusercontent.com/6597529/175493300-c1ff1647-8ab1-4c71-b938-4b04acf2c702.png)
- `rotation` The node orientation in Euler Angles Degrees per axis from `0.0f` to `360.0f`
The three-component rotation vector specifies the direction of the rotation axis in degrees. Rotation is applied relative to the node's origin property.  
Default is `Rotation(x = 0.0f, y = 0.0f, z = 0.0f)`, specifying no rotation.
- `scale` The node scale on each axis  
Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`)

## AR Model Viewer

[![](https://markdown-videos.deta.dev/youtube/HVqAvGJROWk)](https://www.youtube.com/watch?v=HVqAvGJROWk)

```kotlin
ArModelNode(
    placementMode = PlacementMode.BEST_AVAILABLE, 
    hitPosition = Position(0.0f, 0.0f, -2.0f),
    followHitPosition = true,
    instantAnchor = false
)
```

#### Parameters

- `placementMode` Define the [AR Placement Mode](#ar-placement-mode) depending on your need  
You can change it to adjust between a quick (`PlacementMode.INSTANT`), more accurate (`PlacementMode.DEPTH`), only on planes/walls (`PlacementMode.PLANE_HORIZONTAL`, `PlacementMode.PLANE_VERTICAL`, `PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL`) or with auto refining accuracy placement (`PlacementMode.BEST_AVAILABLE`).  
The `hitTest`, `pose` and `anchor` will be influenced by this choice.  
- `hitPosition` The node camera/screen/view position where the hit will be made to find an AR position  
Until it is anchored, the `Node` will try to find the real world position/orientation of the screen coordinate and constantly place/orientate himself accordingly `followHitPosition` is `true`.  
The Z value is only used when no surface is actually detected or when `followHitPosition` and `instantAnchor` is set to `false` or when instant placement is enabled.
- `followHitPosition` Make the node follow the camera/screen matching real world positions  
Controls if an unanchored node should be moved together with the camera.  
The node `position` is updated with the realtime ARCore `pose` at the corresponding `hitPosition` until it is anchored (`isAnchored`) or until this this value is set to `false`.
    - While there is no AR tracking information available, the node is following the camera moves so it stays at this camera/screen relative position but without adjusting its position and orientation to the real world
    - Then ARCore will try to find the real world position of the node at the `hitPosition` by looking at its `hitTest` on each `onArFrame`.
    - In case of instant placement disabled, the z position (distance from the camera) will be estimated by the AR surface distance at the `(x,y)`.
    - The node rotation will be also adjusted in case of `PlacementMode.DEPTH` or depending on the detected planes orientations in case of `PlacementMode.PLANE_HORIZONTAL`, `PlacementMode.PLANE_VERTICAL`, `PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL`
- `instantAnchor` Anchor the node as soon as an AR position/rotation is found/available  
If `true`, the node will be anchored in the real world at the first suitable place available

## AR Placement Mode

[![](https://markdown-videos.deta.dev/youtube/rxzLfTCsm_o)](https://www.youtube.com/watch?v=rxzLfTCsm_o)

Choose how an object is placed within the real world
- `DISABLED` Disable every AR placement preview and handle it by yourself (`onTap`, `onAugmentedFace`, `onAugmentedImage`
- `PLANE_HORIZONTAL` Place and orientate nodes only on horizontal planes
- `PLANE_VERTICAL` Place and orientate nodes only on vertical planes
- `PLANE_HORIZONTAL_AND_VERTICAL` Place and orientate nodes on both horizontal and vertical planes
- `DEPTH` Place and orientate nodes on every detected depth surfaces. Not all devices support this mode. In case on non depth enabled device the placement mode will automatically fallback to `PLANE_HORIZONTAL_AND_VERTICAL`.
- `INSTANT` Instantly place only nodes at a fixed orientation and an approximate distance. No AR orientation will be provided = fixed +Y pointing upward, against gravity. This mode is currently intended to be used with hit tests against horizontal surfaces.
- `BEST_AVAILABLE` Place nodes on every detected surfaces. The node will be placed instantly and then adjusted to fit the best accurate, precise, available placement.

#### Parameters

- `instantPlacementDistance` Distance in meters at which to create an InstantPlacementPoint. This is only used while the tracking method for the returned point is InstantPlacementPoint.  
Default: `2.0f` (2 meters)
- `instantPlacementFallback` Fallback to instantly place nodes at a fixed orientation and an approximate distance when the base placement type is not available yet or at all.

## Load a glb/glTF Model

### Asynchronously

```kotlin
modelNode.loadModelAsync(
    context = context,
    lifecycle = lifecycle,
    glbFileLocation = "models/mymodel.glb",
    autoAnimate = true,
    autoScale = false,
    centerOrigin = null,
    onError = { exception -> },
    onLoaded = { modelInstance -> }
)
```

### Within a Coroutine Scope

```kotlin
lifecycleScope.launchWhenCreated {
    val modelInstance = modelNode.loadModel(
        context = context,
        glbFileLocation = "https://sceneview.github.io/assets/models/MaterialSuite.glb",
        autoAnimate = true,
        autoScale = true,
        centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f),
        onError = { exception -> }
    )
}
```

#### Parameters

- `lifecycle` Provide your lifecycle in order to load your model instantly and to destroy it (and its resources) when the lifecycle goes to destroy state  
Passing `null` means the model loading will be done when the `Node` is added to the `SceneView` and the destroy will be done when the `SceneView` is detached.
- `modelFileLocation` The model glb/gltf file location
    - A relative asset file location (models/mymodel.glb)
    - An Android resource from the res folder (context.getResourceUri(R.raw.mymodel)
    - A File path (Uri.fromFile(myModelFile).path)
    - An http or https url (https://mydomain.com/mymodel.glb)
- `autoAnimate` Plays the animations automatically if the model has one
- `autoScale` Scale the model to fit a unit cube so it will better fit your SceneView
- `centerOrigin` Center point origin position within the model  
Float cube position values between -1.0 and 1.0 corresponding to percents from  model sizes.  
    - `null` = Keep the origin point where it was at the model export time 
    - `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and vertically
    - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom
    - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top
    - ...
- `onError` An exception has been thrown during model loading

## AR Cloud Anchors

[![](https://markdown-videos.deta.dev/youtube/iptk8jsWyw4)](https://www.youtube.com/watch?v=iptk8jsWyw4)

[Sources](https://github.com/Gebort/FESTU.Navigator)

```kotlin

sceneView.cloudAnchorEnabled = true

// Host/Record a Cloud Anchor
node.onAnchorChanged = { node: ArNode, anchor: Anchor? ->
    if(anchor != null) {
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
This will process the incoming ARCore `DepthImage` to occlude virtual objects behind real world objects.  
If the AR `Session` is not configured properly the standard camera material is used.  
Valid `Session.Config` for the Depth occlusion are `Config.DepthMode.AUTOMATIC` and `Config.DepthMode.RAW_DEPTH_ONLY`  
Disable this value to apply the standard camera material to the CameraStream.

## AR Geospatial API

[![](https://markdown-videos.deta.dev/youtube/QZYg9WU5wSA)](https://www.youtube.com/watch?v=QZYg9WU5wSA)

Follow the [official developer guide](https://developers.google.com/ar/develop/java/geospatial/developer-guide)
to enable Geospatial in your application. For configuring the ARCore session, you just need to enable
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
Everything is proceed when the attached view Activity/Fragment is resumed but you can also add your `ArSceneView` at any time, the prompt will then occure when first `addView(arSceneView)` is called.

If you need it, you can add a listener on both ARCore success or failed session creation (including camera permission denied since a session cannot be created without it)

- Camera permission has been granted and latest ARCore Services version are already installed or have been installed during the auto check
```kotlin
sceneView.onArSessionCreated = { arSession: ArSession ->
}
```

- Handle a fallback in case of camera permission denied or AR unavailable and possibly move to 3D only usage

```kotlin
sceneView.onArSessionFailed = { exception: Exception ->
    // If AR is not available, we add the model directly to the scene for a 3D only usage
    sceneView.addChild(modelNode)
}
```
The exception contains the failure reason. *e.g. SecurityException in case of camera permission denied*

## Customizing the instructions

- The default instruction nodes have a `ViewRenderable` with a `TextView` or `ImageView`
- The text and images of the instruction nodes can be overridden at the resource level (in the `strings.xml` file and `drawable` directory of your project).
- Custom instruction nodes can have an arbitrary number of child nodes with `ModelRenderable`s and `ViewRenderable`s. It is even possible to play animation for a `ModelRenderable` if it is defined in a `.glb` file or a video using the `VideoNode`
- The `infoNode` can have one of the following values depending on the ARCore features used and the current ARCore state: `searchPlaneInfoNode`, `tapArPlaneInfoNode` and `augmentedImageInfoNode`. Alternatively, it is possible to create your own instruction nodes.
- The `SearchPlaneInfoNode` displays messages related to the ARCore state. For example, `Searching for surfaces...`, `Too dark. Try moving to a well-lit area`, `Moving too fast. Slow down`, etc.
- The `TapArPlaneInfoNode` displays a message that helps users to understand how an object can be placed in AR when no objects are currently present in the scene.
- The `AugmentedImageInfoNode` displays a frame with white corners when no augmented image is currently tracked.

:bulb: **Idea for future:** when access to the flashlight is finally available with the ARCore shared `CameraManager`, it will be great to add a button to the `SearchPlaneInfoNode` to enable the flashlight when there isn't enough light.

## Why have we included the Kotlin-Math library in SceneView?

Earlier versions of OpenGL had a fixed rendering pipeline and provided an API for setting positions of vertices, transformation and projection matrices, etc. However, with the new rendering pipeline it is required to prepare this data before passing it to GLSL shaders and OpenGL doesn't provide any mathematical functions to do that.

It is possible to implement the required functions yourself like in [Sceneform](https://github.com/SceneView/sceneform-android) or use an existing library. For example, C++ supports operator overloading and benefits from the excellent [GLM library](https://glm.g-truc.net/0.9.9/) that allows to use the same syntax and features as GLSL.

We use the [Kotlin-Math library](https://github.com/romainguy/kotlin-math) to rely on a well-tested functions and get an advantage of using Kotlin operators for vector, matrix and quaternion operations too.

## Migration from Sceneform

You will have a little work to do if you are using the `ArFragment` in Sceneform. However, there is the [Deprecated.kt](https://github.com/SceneView/sceneview-android/blob/main/arsceneview/src/main/kotlin/io/github/sceneview/ar/Deprecated.kt) file to help you with the migration.

#### Using the migration suggestions

1. Remove the Sceneform import for the class you want to migrate.
2. Import this class from the `io.github.sceneview.ar` package.
3. Use `Alt+Enter`/the light bulb icon to view and apply the suggestions for replacing the deprecated method calls.

After the migration you should get cleaner code and all of the benefits described in the [Features](#Features) section :tada:

#### Requesting the camera permission and installing/updating the Google Play Services for AR
This is handled automatically in the `ArSceneView`. You can use the `ArSceneView.onArSessionFailed` property to register a callback to be invoked when the ARCore Session cannot be initialized because ARCore is not available on the device or the camera permission has been denied.

#### Instructions for AR
The `InstructionsController` in the `BaseArFragment` has been replaced with the `Instructions` in the `ArSceneView`.

The `Instructions` use a `Node` that is a part of the scene instead of a `View`, as opposed to the `InstructionsController`. This provides more flexibility for customizing the instructions. The `Instructions` have the main `Node` that can be accessed through the `Instructions.infoNode` property.
