// ─── Python sidecar generator ─────────────────────────────────────────────────
//
// Emits a standalone Python script that receives JSON-lines events from the
// Kotlin/Swift RerunBridge and re-logs them into the Rerun viewer.
export function generatePythonSidecar(options = {}) {
    const port = options.port ?? 9876;
    const recordingName = options.recordingName ?? "sceneview-bridge";
    const spawnViewer = options.spawnViewer ?? true;
    if (port < 1 || port > 65535) {
        throw new Error(`Invalid port "${port}". Must be between 1 and 65535.`);
    }
    if (!recordingName.trim()) {
        throw new Error("recordingName cannot be empty");
    }
    return `"""Rerun sidecar for SceneView AR sessions.

Listens on TCP :${port} for JSON-lines events emitted by a SceneView Android
(arsceneview.rerun.RerunBridge) or iOS (SceneViewSwift.RerunBridge) client,
and re-logs each event into the Rerun viewer as the matching archetype.

Setup:
    pip install rerun-sdk numpy
    python rerun-bridge.py
    # then on your device:
    # Android: adb reverse tcp:${port} tcp:${port}
    # iOS:     point the bridge at this machine's LAN IP
"""
from __future__ import annotations

import json
import socket
import sys
from typing import Any

import numpy as np
import rerun as rr

HOST = "0.0.0.0"
PORT = ${port}
APPLICATION_ID = "${recordingName}"


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
    rr.init(APPLICATION_ID, spawn=${spawnViewer ? "True" : "False"})

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((HOST, PORT))
    srv.listen(1)
    print(f"[rerun-bridge] listening on {HOST}:{PORT}", flush=True)

    try:
        while True:
            conn, addr = srv.accept()
            print(f"[rerun-bridge] client connected: {addr}", flush=True)
            buf = b""
            try:
                while True:
                    chunk = conn.recv(65536)
                    if not chunk:
                        break
                    buf += chunk
                    while b"\\n" in buf:
                        line, buf = buf.split(b"\\n", 1)
                        if not line.strip():
                            continue
                        try:
                            handle_event(json.loads(line.decode("utf-8")))
                        except Exception as exc:
                            print(f"[rerun-bridge] skip: {exc}", file=sys.stderr)
            finally:
                conn.close()
                print("[rerun-bridge] client disconnected", flush=True)
    except KeyboardInterrupt:
        print("\\n[rerun-bridge] shutting down", flush=True)
    finally:
        srv.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
`;
}
