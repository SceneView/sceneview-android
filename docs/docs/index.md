# SceneView — 3D and AR with Compose

![SceneView Logo](https://github.com/SceneView/sceneview/assets/6597529/ad382001-a771-4484-9746-3ad200d00f05){ width=280 }

## 3D is just Compose UI.

SceneView 3.0 brings the full power of Google Filament and ARCore into Jetpack Compose.
Write a `Scene { }` the same way you write a `Column { }`. Nodes are composables.
Lifecycle is automatic. State drives everything.

```kotlin
Scene(modifier = Modifier.fillMaxSize()) {
    rememberModelInstance(modelLoader, "models/helmet.glb")?.let { instance ->
        ModelNode(modelInstance = instance, scaleToUnits = 1.0f, autoAnimate = true)
    }
    LightNode(type = LightManager.Type.SUN)
}
```

---

## Install

=== "3D only"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:sceneview:3.0.0")
    }
    ```

=== "3D + AR"

    ```kotlin
    dependencies {
        implementation("io.github.sceneview:arsceneview:3.0.0")
    }
    ```

---

## Get started

<div class="grid cards" markdown>

-   :material-cube-outline: **3D with Compose**

    ---

    Build your first 3D scene with a rotating glTF model, HDR lighting, and orbit camera gestures.

    **~25 minutes · Beginner**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-3d-compose.md)

-   :material-augmented-reality: **AR with Compose**

    ---

    Place 3D objects in the real world using ARCore plane detection and anchor tracking.

    **~20 minutes · Beginner**

    [:octicons-arrow-right-24: Start the codelab](codelabs/codelab-ar-compose.md)

</div>

---

## Key concepts

### Nodes are composables

Every 3D object — models, lights, geometry, cameras — is a `@Composable` function inside `Scene { }`. No manual `addChildNode()` or `destroy()` calls.

### State drives the scene

Pass Compose state into node parameters. The scene updates on the next frame. Toggle a `Boolean` to show/hide a node. Update a `mutableStateOf<Anchor?>` to place content in AR.

### Everything is `remember`

The Filament engine, model loaders, environment, camera — all are `remember`-ed values with automatic cleanup. Create them, use them, forget about them.

---

## Upgrading from v2.x?

See the [Migration guide](migration.md) for a step-by-step walkthrough of every breaking change.

---

## Community

[:simple-discord: Discord](https://discord.gg/UbNDDBTNqb){ .md-button }
[:simple-github: GitHub](https://github.com/SceneView/sceneview){ .md-button .md-button--primary }
