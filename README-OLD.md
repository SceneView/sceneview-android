# SceneView is a 3D/AR Android View with ARCore and Google Filament integrated

## This is the coming next replacement of Sceneform Maintained only accessible to contributors and sponsors for now here: https://github.com/ThomasGorisse/sceneview-android

## Features

- Use SceneView for 3D only or ArSceneView for ARCore + 3D
- Everything is accessible at the SceneView/ArSceneview level = No more `ArFragment`, no more `sceneFragment.sceneview.scene`, `sceneFragment.session.config`,...
- Just add the `<ArSceneView>` to your layout or create an `ArSceneview(context)`. *Compose coming next*
- Camera Permission and ARCore install/update are handled automaticly by the view
- Latest ARCore features (the coming next will be integrated quicker thanks to Kotlin). *DepthHit and InstantPlacement very soon*
- Lifecycle aware components = Increased memory cleanup and performances
- LifecycleScope for ressource loading in coroutines = Started on view created and cancelled on destroy.
- Mutliple instances is now possible


## Dependency

### Create an access token for downloading the dependency
From GitHub: [Settings / Developer settings / Personal access tokens / Generate new token](https://github.com/settings/tokens/new)
![image](https://user-images.githubusercontent.com/6597529/137579930-037007f5-2f08-48b0-98d2-aae0d859dea8.png)
- Note = `GitHub Packages - Read`
- Expiration = `no expiration` (It's only for reading package and you can delete at anytime)


- Select scope = `read:packages`

![image](https://user-images.githubusercontent.com/6597529/137579847-6a7acadb-c4dd-4d6a-9712-02dd29532502.png)

- Generate token

![image](https://user-images.githubusercontent.com/6597529/137580809-48fc0d68-f885-4aa5-99f3-b4ae919ab291.png)

- Copy the access token

### Gradle dependency

- *build.gradle*
```gradle
allprojects {
    repositories {
        ...
        maven {
            name = "SceneView"
            url = uri("https://maven.pkg.github.com/thomasgorisse/sceneview")
            credentials {
                username = "YOUR_GITHUB_USERNAME_HERE"
                password = "PASTE_THE_GITHUB_ACCESS_TOKEN_HERE"
            }
        }
    }
}
```

- *app/build.gradle*
```gradle
dependencies {
     implementation "com.gorisse.thomas:sceneview:1.0.0"
}
```

## Migration

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

By default, thoses nodes are made of `ViewRenderable` with a `TextView` or `ImageView` that displays ARCore infos:

- "Searching Plane", "Not enougth light",..."Tap on plane to add object."
> Personal note for later: when access to the flash light is finally available with an ARCore shared CameraManager, it will be great to add an enable flash button in here when ArCore returns to less light.

- `tapArPlaneInfoNode` is a centered DepthNode. Which means it follows the orientation of the center ARHitTest and guide user for taping on a plane.

- Augmented Image tracking: Displays a target corners drawable when no augmented image is currently tracked.


*NOTE#01 - Those states can be overrided at the res level (strings.xml, drawable,...)*

*NOTE#02 - You can use your own node and possibly define an animated model renderable for it*
