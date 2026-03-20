#!/usr/bin/env bash
# AR emulator screenshot capture — diagnostic version.
# Checks whether the virtualscene camera renders at all in headless mode,
# then tests the Camera app before the AR apps.
set -euo pipefail

# ---------------------------------------------------------------------------
# Boot / unlock
# ---------------------------------------------------------------------------
sleep 20
adb shell input keyevent 82 || true
sleep 5
adb shell input keyevent 4 || true
adb shell input keyevent 4 || true
adb shell input keyevent 4 || true

# ---------------------------------------------------------------------------
# Diagnostics — camera state before installing anything
# ---------------------------------------------------------------------------
echo "=== CAMERA DIAGNOSTICS ==="
echo "--- adb emu camera list ---"
adb emu camera list || echo "(camera list not supported)"

echo "--- emulator build info ---"
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.release
adb emu avd name || true

# ---------------------------------------------------------------------------
# Install ARCore and AR sample APKs
# ---------------------------------------------------------------------------
adb install -r arcore-emulator.apk
adb install -r samples/ar-model-viewer/build/outputs/apk/debug/ar-model-viewer-debug.apk
adb install -r samples/ar-point-cloud/build/outputs/apk/debug/ar-point-cloud-debug.apk
adb install -r samples/ar-augmented-image/build/outputs/apk/debug/ar-augmented-image-debug.apk

# Grant camera permissions
adb shell pm grant io.github.sceneview.sample.armodelviewer android.permission.CAMERA || true
adb shell pm grant io.github.sceneview.sample.arpointcloud android.permission.CAMERA || true
adb shell pm grant io.github.sceneview.sample.araugmentedimage android.permission.CAMERA || true

# ---------------------------------------------------------------------------
# DIAGNOSTIC: open the system Camera app first.
# If the virtualscene renders at all in headless mode, the Camera app will
# show the 3D room. If it's also white/black, the issue is swiftshader/gfxstream
# not rendering the virtualscene — not an ARCore problem.
# ---------------------------------------------------------------------------
echo "=== CAMERA APP TEST ==="
adb shell am force-stop com.google.android.apps.nexuslauncher || true
# Launch the default camera app (varies by image, try both package names)
adb shell am start -a android.media.action.STILL_IMAGE_CAMERA || true
sleep 8
adb exec-out screencap -p > camera-app-test.png
echo "  camera-app-test.png: $(wc -c < camera-app-test.png) bytes"

# ---------------------------------------------------------------------------
# Sensor motion injection via adb emu
# ---------------------------------------------------------------------------
inject_motion() {
  echo "[motion] injecting sensor deltas via adb emu"
  adb emu sensor set acceleration 0.4:9.5:0.3;   sleep 1
  adb emu sensor set gyroscope 0.25:0.10:0.20;   sleep 1
  adb emu sensor set acceleration -0.3:9.7:0.5;  sleep 1
  adb emu sensor set gyroscope -0.15:0.20:-0.10; sleep 1
  adb emu sensor set acceleration 0.2:9.8:-0.2;  sleep 1
  adb emu sensor set gyroscope 0.10:-0.10:0.15;  sleep 1
  adb emu sensor set acceleration 0:9.8:0;        sleep 1
  adb emu sensor set gyroscope 0:0:0
  echo "[motion] done"
}

capture() {
  local pkg="$1" activity="$2" out="$3"
  echo "=== $pkg ==="
  adb shell am force-stop com.google.android.apps.nexuslauncher || true
  adb shell am start -S --activity-clear-task -n "${pkg}/${activity}"
  sleep 10
  inject_motion
  sleep 20

  # Capture ARCore logcat for this app to understand tracking state
  adb logcat -d -s "ArCamera:*" "ArSession:*" "ARCore:*" "sceneview:*" \
    | tail -30 > "${out%.png}-logcat.txt" || true

  adb exec-out screencap -p > "$out"
  echo "  $out: $(wc -c < "$out") bytes"
}

capture io.github.sceneview.sample.armodelviewer   .MainActivity ar-model-viewer.png
capture io.github.sceneview.sample.arpointcloud     .MainActivity ar-point-cloud.png
capture io.github.sceneview.sample.araugmentedimage .MainActivity ar-augmented-image.png

echo "All AR screenshots captured."
