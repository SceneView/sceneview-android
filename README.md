# ![logo](https://github.com/SceneView/sceneview-android/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05)

# 3D and AR Android Jetpack Compose and Layout View based on Google Filament and ARCore

[![Sceneview](https://img.shields.io/maven-central/v/io.github.sceneview/sceneview.svg?label=Sceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/sceneview)
[![ARSceneview](https://img.shields.io/maven-central/v/io.github.sceneview/arsceneview.svg?label=ARSceneview&color=6c35aa)](https://search.maven.org/artifact/io.github.sceneview/arsceneview)

[![Filament](https://img.shields.io/badge/Filament-v1.51.0-yellow)](https://github.com/google/filament)
[![ARCore](https://img.shields.io/badge/ARCore-v1.42.0-c961cb)](https://github.com/google-ar/arcore-android-sdk)

[![Discord](https://img.shields.io/discord/893787194295222292?color=7389D8&label=Discord&logo=Discord&logoColor=ffffff&style=flat-square)](https://discord.gg/UbNDDBTNqb)
[![Open Collective](https://opencollective.com/sceneview/tiers/badge.svg?label=Donators%20)](https://opencollective.com/sceneview)


## 3D Scene (Filament)

### Dependency
*app/build.gradle*
```gradle
dependencies {
    // Sceneview
    implementation("io.github.sceneview:sceneview:2.2.1")
}
```

### API
https://sceneview.github.io/api/sceneview-android/sceneview/

### Usage
https://github.com/SceneView/sceneview-android/blob/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/samples/model-viewer-compose/src/main/java/io/github/sceneview/sample/modelviewer/compose/MainActivity.kt#L50-L89

### Current Version
https://github.com/SceneView/sceneview-android/blob/2bed398b3e10e8e9737d6e4a38933e783c1ee75e/sceneview/gradle.properties#L6

### Filament
#### Included Dependencies
https://github.com/SceneView/sceneview-android/blob/3abb70cc4362100a2232a2fd1ade9f850fe83096/sceneview/build.gradle#L113-L117

## AR ARScene (Scene + ARCore)

### Dependency
*app/build.gradle*
```gradle
dependencies {
    // ARSceneview
    implementation 'io.github.sceneview:arsceneview:2.2.1'
}
```

[API Reference](https://sceneview.github.io/api/sceneview-android/arsceneview/)

## Usage



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



### 3D `@Composable Scene()`
https://github.com/SceneView/sceneview-android/blob/8be5d205d4b168e5d6e8ba0521e4bf71f3d93bcd/sceneview/src/main/java/io/github/sceneview/Scene.kt#L50-L200
### AR `@Composable ARScene()`
https://github.com/SceneView/sceneview-android/blob/8be5d205d4b168e5d6e8ba0521e4bf71f3d93bcd/arsceneview/src/main/java/io/github/sceneview/ar/ARScene.kt#L62-L244
### Samples
https://github.com/SceneView/sceneview-android/tree/main/samples

## Links

### Tutorials
[Youtube Videos](https://www.youtube.com/results?search_query=SceneView+android)

## Filament 
### GitHub
https://github.com/google/filament

### Dependencies
https://github.com/SceneView/sceneview-android/blob/3abb70cc4362100a2232a2fd1ade9f850fe83096/sceneview/build.gradle#L113-L117

## Filament Dependency
https://github.com/SceneView/sceneview-android/blob/8be5d205d4b168e5d6e8ba0521e4bf71f3d93bcd/arsceneview/build.gradle#L92-L93

## Support our work

### Help us
- Buy devices to test the SDK on
- Equipment for decent video recording Tutorials and Presentations
- Sceneview Hosting Fees

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
