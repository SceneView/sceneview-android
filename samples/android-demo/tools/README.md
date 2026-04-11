# android-demo tools

Developer utilities that ship next to the `android-demo` sample app but are
not part of the app itself.

## rerun-bridge.py — Rerun sidecar for the "AR Debug (Rerun)" demo

Streams ARCore frame data (camera pose, detected planes, point cloud) from
a running Android device into the [Rerun](https://rerun.io) viewer, using
the JSON-lines wire format emitted by
`io.github.sceneview.ar.rerun.RerunBridge`.

### Install

```bash
pip install rerun-sdk numpy
```

### Run

1. Plug your Android device into your Mac / Linux dev box via USB (with
   USB debugging enabled).
2. Forward the device's `127.0.0.1:9876` to your host's port 9876:
   ```bash
   adb reverse tcp:9876 tcp:9876
   ```
3. Start the sidecar (this also spawns the Rerun viewer window):
   ```bash
   python samples/android-demo/tools/rerun-bridge.py
   ```
4. Launch the SceneView demo app on the device, go to the **Samples** tab,
   then open **AR Debug (Rerun)**. The viewer should light up with the
   camera trajectory, detected planes, and the point cloud in real time.

### Troubleshooting

- **"client disconnected (logged 0 events)"** — the demo app couldn't
  reach the sidecar. Double-check that `adb reverse tcp:9876 tcp:9876`
  is running and that you're using the same USB cable the device was
  plugged into when you ran the command.
- **Rerun viewer opens but is empty** — move the phone so ARCore starts
  detecting planes; the camera pose alone is rarely visible without a
  reference frame. The status chip at the top of the demo screen shows
  a frame counter — if it stays at 0, the AR session hasn't started.
- **"ARCore not available"** — the demo needs a physical ARCore-capable
  Android device. Emulators and devices without Play Services for AR
  will show a banner instead of the AR view.

### Regenerating this script

The sidecar is a hand-checked snapshot of what the `rerun-3d-mcp` package
produces when you call its `generate_python_sidecar` tool. To regenerate
with different settings (custom port, different Rerun application id,
manual viewer control), ask Claude:

> Use rerun-3d-mcp's generate_python_sidecar tool with port 9877 and
> spawnViewer = false

or run the MCP directly:

```bash
npx rerun-3d-mcp
```
