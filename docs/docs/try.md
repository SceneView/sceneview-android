# Try SceneView

Get the demo app running on your device in under a minute.

---

## Option 1 — One command (recommended)

Clone the repo and run:

```bash
git clone https://github.com/SceneView/sceneview-android.git
cd sceneview-android
./try-demo
```

That's it. The script builds the demo app and installs it on your connected Android device.

!!! tip "Requirements"
    - Android device with **USB debugging** enabled
    - **Java 17+** installed
    - **adb** on your PATH (comes with Android Studio)

### Try a specific sample

```bash
./try-demo --sample ar-model-viewer    # AR tap-to-place
./try-demo --sample physics-demo       # Bouncing spheres
./try-demo --sample autopilot-demo     # Automotive HUD
./try-demo --sample dynamic-sky        # Day/night cycle
./try-demo --sample post-processing    # Bloom, SSAO, vignette
```

All 15 samples are available. Run `./try-demo --help` for the full list.

---

## Option 2 — Download the APK

<div class="try-download-grid">

<div class="try-download-card">
<h3>SceneView Demo</h3>
<p>Full showcase: 11 models, 6 HDR environments, 26+ node types, animations, physics, post-processing.</p>
<a href="https://github.com/SceneView/sceneview-android/releases/latest/download/sceneview-demo.apk" class="md-button md-button--primary">
Download APK
</a>
<p class="try-download-note">Debug-signed — works on any device, no Play Store needed.</p>
</div>

<div class="try-download-card">
<h3>All 15 samples</h3>
<p>Individual APKs for each sample: model viewer, AR, physics, camera, sky, path, text, effects.</p>
<a href="https://github.com/SceneView/sceneview-android/releases/latest" class="md-button">
Browse all APKs
</a>
</div>

</div>

### Install from terminal

```bash
# Download and install in one line
curl -fSL -o /tmp/sceneview-demo.apk \
  https://github.com/SceneView/sceneview-android/releases/latest/download/sceneview-demo.apk \
  && adb install -r /tmp/sceneview-demo.apk
```

Or use the script's download mode (no build required):

```bash
./try-demo --download
```

---

## Option 3 — Android Studio

1. Open the project in Android Studio
2. Select the **`samples:sceneview-demo`** run configuration
3. Click **Run**

---

## What's in the demo

| Tab | What it shows |
|---|---|
| **Explore** | Full-screen 3D viewer — orbit camera, 8 models, 6 HDR environments |
| **Showcase** | Live previews of every node type with code snippets |
| **Gallery** | 14 curated cards — realistic models, animated characters, procedural geometry |
| **Effects** | Post-processing: bloom, vignette, tone mapping, FXAA, SSAO |
| **QA** | Stress tests, spring animations, edge cases |

<div class="try-demo-features">

<div class="try-feature">
<strong>11 models</strong><br>
Khronos PBR demos, Sketchfab realistic & animated characters
</div>

<div class="try-feature">
<strong>6 HDR environments</strong><br>
Night, studio, warm, sunset, outdoor, autumn
</div>

<div class="try-feature">
<strong>26+ node types</strong><br>
Model, Light, Cube, Sphere, Text, Path, Video, View...
</div>

<div class="try-feature">
<strong>60fps on mid-range</strong><br>
Filament rendering engine, optimized for mobile
</div>

</div>

---

## Build from source

```bash
# Clone
git clone https://github.com/SceneView/sceneview-android.git
cd sceneview-android

# Build just the demo
./gradlew :samples:sceneview-demo:assembleDebug

# Build all samples
./gradlew assembleDebug

# Run lint
./gradlew :samples:sceneview-demo:lint
```

The APK lands in `samples/sceneview-demo/build/outputs/apk/debug/`.
