# AR Cloud Anchors

Host and resolve ARCore Cloud Anchors so that multiple users or separate sessions can share the same AR content at the same physical location.

## What it demonstrates
- Hosting a Cloud Anchor via `session.hostCloudAnchorAsync` and persisting the returned Cloud Anchor ID
- Resolving a previously hosted anchor by ID using `session.resolveCloudAnchorAsync`
- Placing shared 3D content on a `AnchorNode` that maps to the resolved Cloud Anchor
- Basic UI flow: host mode vs. resolve mode

## Key code

```kotlin
@Composable
fun CloudAnchorScreen(engine: Engine, modelLoader: ModelLoader) {
    var sharedAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        onSessionCreated = { session ->
            // Resolve a previously hosted anchor by its cloud ID
            session.resolveCloudAnchorAsync(savedCloudAnchorId) { anchor, state ->
                if (state == CloudAnchorState.SUCCESS) {
                    sharedAnchorNode = AnchorNode(engine, anchor)
                }
            }
        },
        onTapOnPlane = { hitResult, _, _ ->
            // Host a new anchor at the tapped location
            session.hostCloudAnchorAsync(hitResult.createAnchor(), ttlDays = 1) { anchor, state ->
                if (state == CloudAnchorState.SUCCESS) {
                    saveCloudAnchorId(anchor.cloudAnchorId)
                    sharedAnchorNode = AnchorNode(engine, anchor)
                }
            }
        },
    ) {
        LightNode(type = LightManager.Type.SUN)

        sharedAnchorNode?.let { anchor ->
            rememberModelInstance(modelLoader, "models/damaged_helmet.glb")?.let {
                anchor.addChildNode(ModelNode(modelInstance = it, scaleToUnits = 0.5f))
            }
        }
    }
}
```

## Running the sample
Open the project in Android Studio and run the `:samples:ar-cloud-anchor` configuration.
