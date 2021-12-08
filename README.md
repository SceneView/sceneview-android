# SceneView is a 3D/AR Android View with ARCore and Google Filament

## This is the Sceneform replacement

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
repositories {
    // ...
    mavenCentral()
}

dependencies {
    // 3D Only
    implementation 'io.github.sceneview:sceneview:0.2.0'
    // AR + 3D
    implementation 'io.github.sceneview:arsceneview:0.2.0'
}
```

## Usage


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
