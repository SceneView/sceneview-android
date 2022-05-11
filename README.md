# SceneView Android
### 3D/AR Android View with ARCore and Google Filament

This is a Sceneform replacement in Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.sceneview%22)
[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)

## Features

- Use `SceneView` for 3D only or `ArSceneView` for 3D and ARCore.
- Everything is accessible at the `SceneView`/`ArSceneView` level. For example, no more ~`ArFragment`~ and code like ~`arFragment.arSceneView.scene`~, ~`arFragment.session.config`~, etc.
- Just add the `<ArSceneView>` tag to your layout or call the `ArSceneview(context: Context)` constructor in your code. *Compose is coming next ...*
- Requesting the camera permission and installing/updating the Google Play Services for AR is handled automatically in the `ArSceneView`.
- Support for the latest ARCore features (the upcoming features will be integrated quicker thanks to Kotlin).
- Lifecycle-aware components = Better memory management and performance.
- Resources are loaded using coroutines launched in the `LifecycleCoroutineScope` of the `SceneView`/`ArSceneView`. This means that loading is started when the view is created and cancelled when it is destroyed.
- Multiple instances are now possible.
- Much easier to use. For example, the local and world `position`, `rotation` and `scale` of the `Node` are now directly accessible without creating ~`Vector3`~ objects (`position.x = 1f`, `rotation = Rotation(90f, 180f, 0f)`, `scale = Scale(0.5f)`, etc.).

## Dependency

*app/build.gradle*
- 3D only
```gradle
dependencies {
    // 3D only
    implementation 'io.github.sceneview:sceneview:0.6.0'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/sceneview/)

- 3D and ARCore
```gradle
dependencies {
    // 3D and ARCore
    implementation 'io.github.sceneview:arsceneview:0.6.0'
}
```
[API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

## Usage

*res/layout/main_fragment.xml*
- 3D only
```xml
<io.github.sceneview.SceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
[![](https://yt-embed.herokuapp.com/embed?v=mtoTqRREnmM)](https://www.youtube.com/watch?v=mtoTqRREnmM)
- 3D and ARCore
```xml
<io.github.sceneview.ar.ArSceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## ARCore Geospatial API

- Configure session
```kotlin
arSceneView.configureSession { session, config ->
    // Enable Geospatial Mode.
    config.geospatialMode = Config.GeospatialMode.ENABLED
}
```
- Create an Anchor
```kotlin
val earth = arSceneView.session?.earth ?: return
if (earth.trackingState == TrackingState.TRACKING) {
    // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
    val altitude = earth.cameraGeospatialPose.altitudeMeters - 1
    // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
    val qx = 0f
    val qy = 0f
    val qz = 0f
    val qw = 1f
    earthAnchor = earth.createAnchor(latLng.latitude, latLng.longitude, altitude, qx, qy, qz, qw)
}
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
