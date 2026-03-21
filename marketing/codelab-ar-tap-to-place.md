# Codelab: Your First AR App with SceneView

**Duration:** ~30 minutes
**Level:** Beginner
**Prerequisites:** Android Studio, basic Kotlin/Compose knowledge

---

## What you'll build

An AR app where you tap the floor to place a 3D model in the real world. The model persists, can be moved by tapping again, and scales/rotates with pinch/two-finger gestures.

By the end of this codelab, you'll have:
- ✅ AR camera view with plane detection
- ✅ Tap-to-place a 3D model on any flat surface
- ✅ Pinch-to-scale and two-finger-rotate gestures
- ✅ Visual plane indicator (animated reticle)

---

## Step 1: Create a new project

In Android Studio, create a new **Empty Activity** project:
- **Language:** Kotlin
- **Minimum SDK:** API 24
- **Build configuration:** Kotlin DSL

---

## Step 2: Add SceneView dependency

In `app/build.gradle.kts`, add:

```kotlin
dependencies {
    implementation("io.github.sceneview:arsceneview:3.2.0")
}
```

In `app/src/main/AndroidManifest.xml`, add AR permissions:

```xml
<manifest>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera.ar" android:required="true" />

    <application>
        <!-- Required: tells the Play Store this app requires ARCore -->
        <meta-data android:name="com.google.ar.core" android:value="required" />
        ...
    </application>
</manifest>
```

---

## Step 3: Add your 3D model

Download a free GLB model from [Sketchfab](https://sketchfab.com/features/gltf) or [Google's model library](https://arvr.google.com/intl/en_us/objects/).

Place it in `app/src/main/assets/models/object.glb`.

> **Tip:** Keep models under 5MB for fast AR loading. Use [gltf-transform](https://gltf-transform.dev/) to optimize.

---

## Step 4: Write the AR screen

Create `ARPlacementScreen.kt`:

```kotlin
package com.example.myarapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

@Composable
fun ARPlacementScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // The anchor node where the model is placed
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }

    // Load the model (returns null while loading)
    val modelInstance = rememberModelInstance(modelLoader, "models/object.glb")

    // Hint text — disappears once the model is placed
    val hint = if (anchorNode == null) "Move your phone to detect surfaces, then tap to place" else ""

    Box(modifier = Modifier.fillMaxSize()) {

        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            planeRenderer = true,           // Show animated plane grid
            onSingleTapConfirmed = { hitResult ->
                // Destroy previous anchor (replace on tap)
                anchorNode?.destroy()

                // Create a new anchor at the tapped position
                anchorNode = AnchorNode(
                    engine = engine,
                    anchor = hitResult.createAnchor()
                ).apply {
                    isEditable = true  // Enable pinch-scale + two-finger-rotate
                }
            }
        ) {
            // Place the model at the anchor when both are ready
            anchorNode?.let { anchor ->
                Node(parent = anchor) {
                    modelInstance?.let { instance ->
                        ModelNode(
                            modelInstance = instance,
                            scaleToUnits = 0.5f,      // Scale to 50cm
                            autoAnimate = true,        // Play model animations
                            animationLoop = true
                        )
                    }
                }
            }
        }

        // Hint text overlay
        if (hint.isNotEmpty()) {
            Text(
                text = hint,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }
}
```

---

## Step 5: Set it as the main screen

In `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ARPlacementScreen()
            }
        }
    }
}
```

---

## Step 6: Run the app

Connect a physical Android device (AR requires a real camera — emulators don't work).

1. Enable **Developer Options** and **USB Debugging** on your device
2. Click **Run** in Android Studio
3. Point your phone at a floor or table
4. Move slowly to detect the surface (animated grid appears)
5. Tap to place the model

---

## What's happening under the hood

### `ARScene { }` composable

The `ARScene` composable handles:
- Camera permission request
- ARCore session initialization
- Surface tracking and plane detection
- The camera feed as background

You provide the `onSingleTapConfirmed` callback and the content block. ARCore does the rest.

### `AnchorNode`

An `AnchorNode` locks a coordinate to a real-world position. As ARCore refines its understanding of the scene, the anchor updates automatically — keeping your model locked to the surface even as you move.

Setting `isEditable = true` enables:
- **Pinch** → scale the model
- **Two-finger drag** → rotate around the Y axis
- **Single drag** → no effect (anchor prevents position change)

### `rememberModelInstance`

`rememberModelInstance` loads the GLB file asynchronously on the main thread (required by Filament). It returns `null` while loading. The `?.let { }` null check means nothing renders until loading is complete — no crash, no flash.

---

## Step 7: Add a "Remove" button

Let users delete the placed model:

```kotlin
// Add this inside your Box, below the ARScene
if (anchorNode != null) {
    Button(
        onClick = {
            anchorNode?.destroy()
            anchorNode = null
        },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
    ) {
        Text("Remove")
    }
}
```

---

## Step 8: Add model switching (optional)

Let users choose from multiple models:

```kotlin
val models = listOf("Chair" to "models/chair.glb", "Car" to "models/car.glb")
var selectedModel by remember { mutableStateOf("models/chair.glb") }
val modelInstance = rememberModelInstance(modelLoader, selectedModel)

// In your Box, add chip row:
Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
    models.forEach { (label, path) ->
        FilterChip(
            selected = selectedModel == path,
            onClick = { selectedModel = path },
            label = { Text(label) }
        )
    }
}
```

---

## Summary

You built a tap-to-place AR app in ~50 lines of Compose. Here's what you used:

| Component | Purpose |
|---|---|
| `ARScene { }` | AR camera + plane detection |
| `AnchorNode` | Lock 3D content to a real-world surface |
| `ModelNode` | Render the GLB model |
| `rememberModelInstance` | Async model loading with automatic cleanup |
| `isEditable = true` | Pinch-scale + two-finger-rotate gestures |

## Next steps

- **[3D Model Viewer codelab →](#)** — Orbit camera, HDR environment, model switching
- **[Cloud Anchors →](#)** — Share AR scenes between devices
- **[TextNode →](#)** — Add 3D labels to your scene
- **[GitHub samples →](https://github.com/SceneView/sceneview-android/tree/main/samples)** — 14 complete sample apps

---

*Built with [SceneView](https://github.com/SceneView/sceneview-android) — Apache License 2.0*
