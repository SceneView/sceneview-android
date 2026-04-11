"""Rerun sidecar for SceneView's "AR Debug (Rerun)" sample demo.

Listens on TCP :9876 for the JSON-lines wire format emitted by the SceneView
Android RerunBridge (arsceneview.rerun.RerunBridge) and re-logs each event
into the Rerun viewer as the matching archetype:

  camera_pose  -> rr.Transform3D
  plane        -> rr.LineStrips3D  (closed world-space polygon)
  point_cloud  -> rr.Points3D
  anchor       -> rr.Transform3D
  hit_result   -> rr.Points3D      (single highlighted point)

The Rerun viewer is spawned automatically via rr.init(spawn=True) — no need
to run a separate `rerun` process.

Setup on your dev machine:
    pip install rerun-sdk numpy
    python samples/android-demo/tools/rerun-bridge.py

Then on your Android device (connected via USB with adb):
    adb reverse tcp:9876 tcp:9876
    # launch the SceneView demo app
    # open the Samples tab -> AR Debug (Rerun)

This script is generated in spirit by the `rerun-3d-mcp` package's
`generate_python_sidecar` tool — you can regenerate it with:
    npx rerun-3d-mcp  # then ask Claude to call generate_python_sidecar

You can also send this file to a collaborator to reproduce your AR session.
"""
from __future__ import annotations

import json
import socket
import sys
from typing import Any

import numpy as np
import rerun as rr

HOST = "0.0.0.0"
PORT = 9876
APPLICATION_ID = "sceneview-ar-debug"


def _quat(xyzw: list[float]) -> rr.Quaternion:
    return rr.Quaternion(xyzw=xyzw)


def handle_event(ev: dict[str, Any]) -> None:
    """Dispatch a single JSON event to the matching Rerun archetype."""
    t = int(ev.get("t", 0))
    rr.set_time_nanos("device_clock", t)
    kind = ev.get("type")
    entity = ev.get("entity", "world/unknown")

    if kind == "camera_pose":
        rr.log(
            entity,
            rr.Transform3D(
                translation=ev["translation"],
                rotation=_quat(ev["quaternion"]),
            ),
        )
    elif kind == "plane":
        poly = np.array(ev["polygon"], dtype=np.float32)
        if len(poly) >= 3:
            # Close the loop so the viewer draws a full outline.
            closed = np.vstack([poly, poly[:1]])
            rr.log(entity, rr.LineStrips3D([closed]))
    elif kind == "point_cloud":
        positions = np.array(ev["positions"], dtype=np.float32)
        if positions.size > 0:
            rr.log(entity, rr.Points3D(positions, radii=0.005))
    elif kind == "anchor":
        rr.log(
            entity,
            rr.Transform3D(
                translation=ev["translation"],
                rotation=_quat(ev["quaternion"]),
            ),
        )
    elif kind == "hit_result":
        rr.log(
            entity,
            rr.Points3D(
                np.array([ev["translation"]], dtype=np.float32),
                radii=0.015,
                colors=[(255, 200, 0)],
            ),
        )
    else:
        print(f"[rerun-bridge] unknown event type: {kind}", file=sys.stderr)


def main() -> int:
    rr.init(APPLICATION_ID, spawn=True)

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((HOST, PORT))
    srv.listen(1)
    print(f"[rerun-bridge] listening on {HOST}:{PORT}", flush=True)
    print("[rerun-bridge] run 'adb reverse tcp:9876 tcp:9876' on your Mac, then", flush=True)
    print("[rerun-bridge] launch the SceneView demo app -> Samples -> AR Debug (Rerun)", flush=True)

    try:
        while True:
            conn, addr = srv.accept()
            print(f"[rerun-bridge] client connected: {addr}", flush=True)
            buf = b""
            events = 0
            try:
                while True:
                    chunk = conn.recv(65536)
                    if not chunk:
                        break
                    buf += chunk
                    while b"\n" in buf:
                        line, buf = buf.split(b"\n", 1)
                        if not line.strip():
                            continue
                        try:
                            handle_event(json.loads(line.decode("utf-8")))
                            events += 1
                        except Exception as exc:
                            print(f"[rerun-bridge] skip: {exc}", file=sys.stderr)
            finally:
                conn.close()
                print(
                    f"[rerun-bridge] client disconnected (logged {events} events)",
                    flush=True,
                )
    except KeyboardInterrupt:
        print("\n[rerun-bridge] shutting down", flush=True)
    finally:
        srv.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
