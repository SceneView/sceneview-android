#if os(iOS)
import SwiftUI
import SceneViewSwift
import ARKit

/// Streams an ARKit session (camera pose, plane anchors, raw feature
/// points) to a Rerun viewer running on the developer's Mac over TCP
/// using the JSON-lines wire format defined by `RerunWireFormat`.
///
/// ## Setup (one-time on your Mac)
///
/// 1. `pip install rerun-sdk numpy`
/// 2. Run the Python sidecar:
///    `python samples/android-demo/tools/rerun-bridge.py`
///    (the script is shared between Android and iOS — same wire format)
/// 3. The Rerun viewer window opens automatically via `rr.init(spawn=True)`.
/// 4. Make sure your iPhone and Mac are on the same Wi-Fi network, and
///    replace the `macLanIp` below with your Mac's LAN IP (e.g. from
///    System Settings -> Wi-Fi -> Details).
///
/// Alternatively, use `usbmuxd` / `iproxy` to tunnel the port over USB
/// and keep `127.0.0.1` as the host.
///
/// **This is a dev-only tool** — production builds should gate it with
/// `#if DEBUG`. Shipping the bridge in release builds is not harmful
/// (`setEnabled(false)` short-circuits the hot path), but the socket
/// attempt alone wastes CPU.
struct RerunDebugDemo: View {
    // TODO: replace with your Mac's LAN IP, or use a USB port forwarder
    // and keep this as 127.0.0.1.
    private static let macLanIp = "192.168.1.42"

    @StateObject private var bridge = RerunBridge(
        host: RerunDebugDemo.macLanIp,
        port: RerunBridge.defaultPort,
        rateHz: 10
    )

    var body: some View {
        ZStack {
            #if !targetEnvironment(simulator)
            ARSceneView(
                planeDetection: .both,
                showCoachingOverlay: true
            )
            .onFrame { frame, _ in
                bridge.logFrame(frame)
            }
            .ignoresSafeArea()
            #else
            simulatorPlaceholder
            #endif

            VStack {
                statusPill
                Spacer()
                infoPanel
            }
            .padding()
        }
        .onAppear { bridge.connect() }
        .onDisappear { bridge.disconnect() }
    }

    private var statusPill: some View {
        HStack(spacing: 8) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .foregroundStyle(.purple)
            Text("Rerun → \(RerunDebugDemo.macLanIp):\(RerunBridge.defaultPort) · \(bridge.eventCount) events")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.black.opacity(0.65))
        .clipShape(Capsule())
    }

    private var infoPanel: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("AR Debug — streaming to Rerun viewer")
                .font(.headline)
                .foregroundStyle(.white)
            Text("On your Mac: pip install rerun-sdk, then run \"python samples/android-demo/tools/rerun-bridge.py\". Point this view's host at your Mac's LAN IP (currently \(RerunDebugDemo.macLanIp)).")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.78))
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.black.opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var simulatorPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "iphone.gen3")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
            Text("ARKit is not available in the simulator.")
                .font(.headline)
            Text("Run this demo on a physical iPhone or iPad with a camera.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(32)
    }
}
#endif
