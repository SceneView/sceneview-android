#!/usr/bin/env bash
# AR emulator screenshot capture — uses unified android-demo app.
# Tests AR features integrated in the demo app's AR tab.
set -euo pipefail

# ---------------------------------------------------------------------------
# Boot / unlock
# ---------------------------------------------------------------------------
sleep 20
adb shell input keyevent 82 || true
sleep 5
adb shell input keyevent 4 || true

# ---------------------------------------------------------------------------
# Diagnostics
# ---------------------------------------------------------------------------
echo "=== CAMERA DIAGNOSTICS ==="
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.release
adb emu avd name || true

# ---------------------------------------------------------------------------
# Install ARCore and demo APK
# ---------------------------------------------------------------------------
adb install -r arcore-emulator.apk

# Find the android-demo APK
DEMO_APK=$(find samples/android-demo/build/outputs/apk/debug -name "*.apk" | head -1)
if [ -z "$DEMO_APK" ]; then
  echo "ERROR: android-demo APK not found"
  exit 1
fi
adb install -r "$DEMO_APK"

# Grant camera permission
adb shell pm grant io.github.sceneview.demo android.permission.CAMERA || true

# ---------------------------------------------------------------------------
# Sensor motion injection
# ---------------------------------------------------------------------------
inject_motion() {
  echo "[motion] injecting sensor deltas"
  adb emu sensor set acceleration 0.4:9.5:0.3;   sleep 1
  adb emu sensor set gyroscope 0.25:0.10:0.20;   sleep 1
  adb emu sensor set acceleration -0.3:9.7:0.5;  sleep 1
  adb emu sensor set gyroscope -0.15:0.20:-0.10; sleep 1
  adb emu sensor set acceleration 0.2:9.8:-0.2;  sleep 1
  adb emu sensor set acceleration 0:9.8:0;        sleep 1
  adb emu sensor set gyroscope 0:0:0
  echo "[motion] done"
}

# ---------------------------------------------------------------------------
# Launch demo app and capture AR tab
# ---------------------------------------------------------------------------
echo "=== ANDROID DEMO — AR TAB ==="
adb shell am start -S --activity-clear-task -n "io.github.sceneview.demo/.MainActivity"
sleep 10
inject_motion
sleep 20

adb logcat -d -s "ArCamera:*" "ArSession:*" "ARCore:*" "sceneview:*" \
  | tail -30 > ar-demo-logcat.txt || true

adb exec-out screencap -p > ar-demo-screenshot.png
echo "  ar-demo-screenshot.png: $(wc -c < ar-demo-screenshot.png) bytes"

echo "AR screenshot captured."
