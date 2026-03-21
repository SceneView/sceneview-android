# Advanced guide: Dynamic sky and fog

**Time:** ~15 minutes
**Level:** Intermediate
**What you'll build:** A scene with a time-of-day sun cycle and atmospheric fog

---

## Overview

SceneView 3.2.0 adds two atmospheric composables:

- **`DynamicSkyNode`** — drives a sun light based on time-of-day (sunrise, noon, sunset, night)
- **`FogNode`** — applies volumetric fog to the scene

Both are declarative: just change a parameter and the scene reacts.

---

## Step 1 — Add a dynamic sun

```kotlin
@Composable
fun SkyScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // Animate time from 0 (midnight) to 24 over 30 seconds
    val infiniteTransition = rememberInfiniteTransition(label = "DayCycle")
    val timeOfDay by infiniteTransition.animateFloat(
        initialValue = 6f,   // sunrise
        targetValue = 18f,   // sunset
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "time"
    )

    Scene(engine = engine, modelLoader = modelLoader) {
        DynamicSkyNode(
            timeOfDay = timeOfDay,   // 0–24 hour float
            turbidity = 2f,          // atmospheric haze [1–10]
            sunIntensity = 110_000f  // max illuminance at noon (lux)
        )

        // Place your model here — it will be lit by the sun
        rememberModelInstance(modelLoader, "models/scene.glb")?.let {
            ModelNode(modelInstance = it)
        }
    }
}
```

### Sun colour model

| Time | Appearance |
|---|---|
| 0–5 (night) | Near-black, minimal light |
| 6 (sunrise) | Warm orange-red |
| 12 (noon) | Bright white (1.0, 0.98, 0.95) |
| 18 (sunset) | Warm orange-red |
| 19–24 (night) | Near-black |

---

## Step 2 — Add atmospheric fog

```kotlin
val view = rememberView(engine)
var fogEnabled by remember { mutableStateOf(true) }
var fogDensity by remember { mutableFloatStateOf(0.05f) }

Scene(engine = engine, modelLoader = modelLoader, view = view) {
    DynamicSkyNode(timeOfDay = timeOfDay)

    FogNode(
        view = view,
        enabled = fogEnabled,
        density = fogDensity,     // [0.0–1.0], higher = thicker
        height = 1.0f,            // fog is denser below this Y
        color = Color(0xFFCCDDFF) // light blue-grey
    )

    // Your scene content
}
```

### Fog parameters

| Parameter | Effect |
|---|---|
| `density` | Volumetric fog thickness `[0.0, 1.0]`. Try `0.02` for light haze, `0.15` for dense fog |
| `height` | Height falloff — fog is denser below this world-space Y |
| `color` | Fog colour. Match your sky tint for realism |
| `enabled` | Toggle fog on/off reactively |

---

## Step 3 — Combine sky + fog for atmosphere

The real power is combining both. As the sun sets, increase fog density for a moody twilight:

```kotlin
val fogDensity = if (timeOfDay in 6f..18f) 0.02f else 0.12f

Scene(engine = engine, modelLoader = modelLoader, view = view) {
    DynamicSkyNode(timeOfDay = timeOfDay, turbidity = 3f)
    FogNode(view = view, density = fogDensity, color = Color(0xFFEED8AA))
}
```

---

## What's next

- See the `dynamic-sky` sample for a full interactive implementation with a time slider
- Combine with `ReflectionProbeNode` for indoor/outdoor transitions
