# SceneView is a 3D/AR Android View with ARCore and Google Filament

## This is a Sceneform replacement

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.sceneview%22)
[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)

## Features

- Use `SceneView` for 3D only or `ArSceneView` for 3D and ARCore.
- Everything is accessible at the `SceneView`/`ArSceneView` level. For example, no more `ArFragment` and code like `arFragment.arSceneView.scene`, `arFragment.session.config`, etc.
- Just add the `<ArSceneView>` tag to your layout or call the `ArSceneview(context: Context)` constructor in your code. *Compose is coming next ...*
- Requesting the camera permission and installing/updating the Google Play Services for AR is handled automatically in the `ArSceneView`.
- Support for the latest ARCore features (the upcoming features will be integrated quicker thanks to Kotlin).
- Lifecycle-aware components = Better memory management and performance.
- Resources are loaded using coroutines launched in the `LifecycleCoroutineScope` of the `SceneView`/`ArSceneView`. This means that loading is started when the view is created and cancelled when it is destroyed.
- Multiple instances are now possible.
- Much easier to use. For example, the local and world `position`, `rotation` and `scale` of the `Node` are now directly accessible without creating new `Vector3` objects (`position.x = 1f`, `rotation.xy = Float2(90f, 180f)`, `scale = Scale(0.5f)`, etc.).

## Dependency

*app/build.gradle*
```gradle
dependencies {
    // 3D only
    implementation 'io.github.sceneview:sceneview:0.0.3'
}
```

```gradle
dependencies {
    // 3D and ARCore
    implementation 'io.github.sceneview:arsceneview:0.0.3'
}
```

## Usage

*res/layout/main_fragment.xml*
```xml
// 3D only
<io.github.sceneview.SceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```


```xml
// 3D and ARCore
<io.github.sceneview.ar.ArSceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

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
This is handled automatically in the `ArSceneView`. You can use the `ArSceneView.onARCoreException` property to register a callback to be invoked when the ARCore Session cannot be initialized because ARCore is not available on the device or the camera permission has been denied.

#### Instructions for AR
The `InstructionsController` in the `BaseArFragment` has been replaced with the `Instructions` in the `ArSceneView`.

The `Instructions` use a `Node` that is a part of the scene instead of a `View`, as opposed to the `InstructionsController`. This provides more flexibility for customizing the instructions. The `Instructions` have the main `Node` that can be accessed through the `Instructions.infoNode` property.

The `infoNode` can have one of the following values depending on the ARCore features used and the current ARCore state: `searchPlaneInfoNode`, `tapArPlaneInfoNode` and `augmentedImageInfoNode`. Alternatively, it is possible to create your own instruction nodes.

The default instruction nodes have a `ViewRenderable` with a `TextView` or `ImageView`:

- The `SearchPlaneInfoNode` displays messages related to the ARCore state. For example, `Searching for surfaces...`, `Too dark. Try moving to a well-lit area`, `Moving too fast. Slow down`, etc.
- The `TapArPlaneInfoNode` displays a message that helps users to understand how an object can be placed in AR when no objects are currently present in the scene.
- The `AugmentedImageInfoNode` displays a frame with white corners when no augmented image is currently tracked.

:bulb: **Idea for future:** when access to the flashlight is finally available with the ARCore shared `CameraManager`, it will be great to add a button to the `SearchPlaneInfoNode` to enable the flashlight when there isn't enough light.

#### Customizing the instructions

- The text and images of the instruction nodes can be overridden at the resource level (in the `strings.xml` file and `drawable` directory of your project).
- Custom instruction nodes can have an arbitrary number of child nodes with `ModelRenderable`s and `ViewRenderable`s. It is even possible to play animation for a `ModelRenderable` if it is defined in a `.glb` file.
