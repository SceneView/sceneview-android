# Free 3D Models for SceneView

Curated list of free glTF/GLB 3D models suitable for SceneView demos and development.
All models listed here have been verified to load correctly in SceneView.

## Khronos glTF Sample Assets (Recommended)

The official Khronos Group sample models. All are public domain or CC-licensed for
unrestricted use. GLB format includes textures bundled in a single file.

**Base URL:** `https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models`

**URL pattern:** `{BASE}/{ModelName}/glTF-Binary/{ModelName}.glb`

### Showcase Models (Used in SceneView Demos)

| Model | Size | Description | Features |
|---|---|---|---|
| DamagedHelmet | ~3.7 MB | Flight helmet with battle damage | PBR, normal maps, emissive |
| SheenChair | 3.93 MB | Purple damask chair | KHR_materials_sheen |
| ToyCar | 5.17 MB | Detailed toy car | PBR, clearcoat, transmission |
| GlassVaseFlowers | 1.74 MB | Glass vase with flowers | Transmission, volume |
| Fox | 159 KB | Animated fox | Skinned animation, very lightweight |
| Duck | 118 KB | Classic rubber duck | Core glTF 2.0, tiny file |

### Other Impressive Khronos Models

| Model | Size | Description | Features |
|---|---|---|---|
| CesiumMilkTruck | 361 KB | Animated milk truck | Animation, multiple meshes |
| Avocado | ~1 MB | Cut avocado | PBR materials |
| Lantern | ~1.8 MB | Vintage lantern | PBR, emissive |
| FlightHelmet | ~4 MB | Clean flight helmet | PBR, multiple textures |
| BoomBox | 10.1 MB | Retro boom box | PBR, emissive panel |
| DragonAttenuation | 6.11 MB | Translucent dragon | Volume attenuation, variants |
| IridescentDishWithOlives | 5.48 MB | Dish with olives | Iridescence, specular |
| MaterialsVariantsShoe | 7.47 MB | Sneaker shoe | KHR_materials_variants |
| Corset | 12.9 MB | Victorian corset mannequin | High detail PBR |
| AntiqueCamera | 16.7 MB | Old camera on tripod | High detail PBR |
| BarramundiFish | 11.9 MB | Barramundi fish | PBR, organic surfaces |
| MosquitoInAmber | 23.1 MB | Mosquito in amber | Transmission, IOR, volume |
| WaterBottle | 8.55 MB | Water bottle | PBR, clearcoat |

### Khronos Models Under 1 MB (Great for Mobile/AR)

| Model | Size | Description |
|---|---|---|
| Duck | 118 KB | Classic rubber duck |
| Fox | 159 KB | Animated fox with walk/run |
| CesiumMilkTruck | 361 KB | Animated truck |
| CesiumMan | ~500 KB | Animated character |
| RiggedFigure | ~200 KB | Simple rigged character |
| Suzanne | ~50 KB | Blender monkey head |

### Quick Copy URLs

```
# Lightweight models (< 1 MB) -- great for demos and tests
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Fox/glTF-Binary/Fox.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Duck/glTF-Binary/Duck.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/CesiumMilkTruck/glTF-Binary/CesiumMilkTruck.glb

# Medium models (1-5 MB) -- good for showcases
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/DamagedHelmet/glTF-Binary/DamagedHelmet.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/GlassVaseFlowers/glTF-Binary/GlassVaseFlowers.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/SheenChair/glTF-Binary/SheenChair.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/ToyCar/glTF-Binary/ToyCar.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Lantern/glTF-Binary/Lantern.glb
https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Avocado/glTF-Binary/Avocado.glb
```

## Other Free Sources

### Sketchfab

- Over 800,000 downloadable models, many under CC licenses
- Built-in glTF export for all downloadable models
- Filter by license: CC0, CC-BY, CC-BY-SA
- URL: https://sketchfab.com/search?type=models&downloadable=true&sort_by=-likeCount

### Poly Haven

- CC0 (public domain) models, HDRIs, and textures
- All assets free for commercial use
- URL: https://polyhaven.com/models

### Kenney Assets

- CC0 low-poly game assets
- Hundreds of themed packs (vehicles, buildings, characters)
- URL: https://kenney.nl/assets

### Google Poly Archive

- Google Poly was shut down in 2021
- Archive available via poly.pizza (community-maintained)
- Many CC-BY models from the original collection
- URL: https://poly.pizza

### TurboSquid

- 600+ free glTF models
- Mixed licenses -- check each model
- URL: https://www.turbosquid.com/Search/3D-Models/free/gltf

## Usage in SceneView

### Android (Jetpack Compose)

```kotlin
// Load from URL
val modelInstance = rememberModelInstance(
    modelLoader,
    "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Fox/glTF-Binary/Fox.glb"
)

// Load from assets folder
val modelInstance = rememberModelInstance(
    modelLoader,
    "models/fox.glb"  // from app/src/main/assets/models/
)
```

### iOS (SwiftUI)

```swift
SceneView {
    ModelNode(
        url: URL(string: "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Fox/glTF-Binary/Fox.glb")!
    )
}
```

### Web

```javascript
SceneView.modelViewer("canvas",
    "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Fox/glTF-Binary/Fox.glb"
);
```

## Tips for Choosing Models

1. **For demos**: Use models under 5 MB for fast loading
2. **For AR**: Use models under 1 MB (Fox, Duck, CesiumMilkTruck)
3. **For showcases**: DamagedHelmet, SheenChair, and ToyCar are the most visually impressive
4. **For testing**: Duck (118 KB) is the fastest to load
5. **Always check licenses** when using models from Sketchfab or other third-party sources
