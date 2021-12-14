# SceneView is a 3D/AR Android View with ARCore and Google Filament

## This is Sceneform replacement

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.sceneview%22)
[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)

## Features

- Use SceneView for 3D only or ArSceneView for ARCore + 3D
- Everything is accessible at the SceneView/ArSceneview level = No more `ArFragment`, no more `sceneFragment.sceneview.scene`, `sceneFragment.session.config`,...
- Just add the `<ArSceneView>` to your layout or create an `ArSceneview(context)`. *Compose coming next*
- Camera Permission and ARCore install/update are handled automatically by the view
- Latest ARCore features (the coming next will be integrated quicker thanks to Kotlin)
- Lifecycle aware components = Increased memory cleanup and performances
- LifecycleScope for resource loading in coroutines = Started on view created and cancelled on destroy
- Multiple instances is now possible
- Much easier to use. Example: localPosition, worldPosition,...localRotation, worldScale are now directly accessible with a unique property (no more local/world Vector3) `positionX`,...,`rotationY`,...`scale = 0.5`,...)

## Dependency

*app/build.gradle*
```gradle
dependencies {
    // 3D Only
    implementation 'io.github.sceneview:sceneview:0.0.3'
}
```

```gradle
dependencies {
    // AR + 3D
    implementation 'io.github.sceneview:arsceneview:0.0.3'
}
```

## Usage

*res/layout/main_fragment.xml*
```xml
// 3D Only
<io.github.sceneview.SceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```


```xml
// AR + 3D
<io.github.sceneview.ar.ArSceneView
    android:id="@+id/sceneView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Migration from Sceneform

Sorry guys you will have a little work to do if coming from the ArFragment way.

#### BUT#01 - The Deprecated.kt is there to help you migrating

- **Just `Alt+Enter` / `Import` to point to the single Deprecated.kt file**
- **`Alt+Enter` to apply suggestion on deprecated calls.**

#### BUT#02 - You will gain a lot of code cleaning/removing on your app sources:

- `ArSceneView` and `SceneView` are now your best friends for ARCore+Filament or Filament only
- Access directly from the view everything you previously accessed from `ArFragement`, `Scene`, `Session` and `Session.Config`.

#### Permissions and ARCore install/update are handled automatically 
Set the `sceneView.onException` lambda property for overriding default permission refused, ARCore unavailable or any `SceneView` exceptions.

#### Instructions controller is replaced by `sceneView.instructions`
Contains a main `infoNode: Node?` which can be one of `searchPlaneInfoNode`, `tapArPlaneInfoNode` and `augmentedImageInfoNode` or your custom one.

By default, those nodes are made of `ViewRenderable` with a `TextView` or `ImageView` that displays ARCore infos:

- "Searching Plane", "Not enough light",..."Tap on plane to add object."
> Personal note for later: when access to the flash light is finally available with an ARCore shared CameraManager, it will be great to add an enable flash button in here when ArCore returns to less light.

- `tapArPlaneInfoNode` is a centered DepthNode. Which means it follows the orientation of the center ARHitTest and guide user for taping on a plane.

- Augmented Image tracking: Displays a target corners drawable when no augmented image is currently tracked.


*NOTE#01 - Those states can be overrided at the res level (strings.xml, drawable,...)*

*NOTE#02 - You can use your own node and possibly define an animated model renderable for it*
