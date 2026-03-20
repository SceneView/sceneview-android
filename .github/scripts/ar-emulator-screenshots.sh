#!/usr/bin/env bash
# AR emulator screenshot capture with ARCore motion injection.
#
# android-emulator-runner executes its inline `script:` block via /usr/bin/sh
# line-by-line, which breaks multi-line function definitions. This standalone
# file is executed as a real bash script, so functions and local variables work.
set -euo pipefail

# ---------------------------------------------------------------------------
# Boot / unlock
# ---------------------------------------------------------------------------
sleep 20
adb shell input keyevent 82 || true   # unlock screen
sleep 5
adb shell input keyevent 4 || true    # dismiss ANR / permission dialogs
adb shell input keyevent 4 || true
adb shell input keyevent 4 || true

# ---------------------------------------------------------------------------
# Install ARCore emulator APK and all AR sample APKs
# ---------------------------------------------------------------------------
adb install -r arcore-emulator.apk

adb install -r samples/ar-model-viewer/build/outputs/apk/debug/ar-model-viewer-debug.apk
adb install -r samples/ar-point-cloud/build/outputs/apk/debug/ar-point-cloud-debug.apk
adb install -r samples/ar-augmented-image/build/outputs/apk/debug/ar-augmented-image-debug.apk

# Grant camera permissions upfront so no runtime dialog blocks the camera feed
adb shell pm grant io.github.sceneview.sample.armodelviewer android.permission.CAMERA || true
adb shell pm grant io.github.sceneview.sample.arpointcloud android.permission.CAMERA || true
adb shell pm grant io.github.sceneview.sample.araugmentedimage android.permission.CAMERA || true

# ---------------------------------------------------------------------------
# inject_motion — simulate phone movement so ARCore transitions from
# INITIALIZING → TRACKING and the virtualscene camera feed becomes visible.
#
# With -camera-back virtualscene the emulator renders a 3D room as the back
# camera. The virtualscene viewpoint only changes when sensor values change.
# Without motion ARCore sees identical frames, SLAM never bootstraps, and the
# camera surface stays white. `adb emu sensor set` changes sensor readings
# without requiring auth tokens or netcat.
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
  sleep 10          # wait for ARCore to initialise
  inject_motion     # move the virtualscene camera → ARCore reaches TRACKING
  sleep 20          # wait for plane detection
  adb exec-out screencap -p > "$out"
  echo "  saved $out ($(wc -c < "$out") bytes)"
}

capture io.github.sceneview.sample.armodelviewer   .MainActivity ar-model-viewer.png
capture io.github.sceneview.sample.arpointcloud     .MainActivity ar-point-cloud.png
capture io.github.sceneview.sample.araugmentedimage .MainActivity ar-augmented-image.png

echo "All AR screenshots captured."
