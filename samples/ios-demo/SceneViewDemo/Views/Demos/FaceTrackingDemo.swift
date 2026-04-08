#if os(iOS)
import SwiftUI
import RealityKit
import ARKit
import SceneViewSwift

/// Face tracking demo -- AugmentedFaceNode with blend shape visualization.
///
/// Uses ARFaceSceneView (front camera) to detect faces and display a colored
/// mesh overlay. Shows real-time blend shape coefficients for expressions.
struct FaceTrackingDemo: View {
    @State private var isTracking = false
    @State private var smileValue: Float = 0
    @State private var browValue: Float = 0
    @State private var jawValue: Float = 0
    @State private var meshColor: Color = .blue

    private let colorOptions: [(String, Color, UIColor)] = [
        ("Blue", .blue, UIColor.systemBlue.withAlphaComponent(0.3)),
        ("Green", .green, UIColor.systemGreen.withAlphaComponent(0.3)),
        ("Purple", .purple, UIColor.systemPurple.withAlphaComponent(0.3)),
        ("Orange", .orange, UIColor.systemOrange.withAlphaComponent(0.3)),
    ]

    var body: some View {
        ZStack {
            #if !targetEnvironment(simulator)
            if AugmentedFaceNode.isSupported {
                faceSceneView
                    .ignoresSafeArea()
            } else {
                unsupportedView
            }
            #else
            simulatorPlaceholder
            #endif

            VStack {
                // Tracking indicator
                HStack(spacing: 6) {
                    Circle()
                        .fill(isTracking ? Color.green : Color.red)
                        .frame(width: 8, height: 8)
                    Text(isTracking ? "Tracking" : "Looking for face...")
                        .font(.caption.weight(.medium))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .padding(.top, 8)

                Spacer()

                // Expression meters
                if isTracking {
                    expressionMeters
                }

                // Color selector
                colorSelector
            }
        }
        .background(Color.black)
    }

    // MARK: - Face Scene View

    #if !targetEnvironment(simulator)
    @State private var selectedColorIndex = 0

    private var faceSceneView: some View {
        ARFaceSceneView(
            onFaceDetected: { faceAnchor, arView in
                isTracking = true
                let node = AugmentedFaceNode(faceAnchor: faceAnchor)
                let (_, _, uiColor) = colorOptions[selectedColorIndex]
                node.updateMesh(
                    material: SimpleMaterial(
                        color: uiColor,
                        isMetallic: false
                    )
                )
                arView.scene.anchors.append(node.anchorEntity)
                return node
            },
            onFaceUpdated: { faceAnchor, node in
                node.updateMesh()
                // Read blend shapes
                let smileL = node.blendShape(.mouthSmileLeft) ?? 0
                let smileR = node.blendShape(.mouthSmileRight) ?? 0
                smileValue = (smileL + smileR) / 2.0
                browValue = (node.blendShape(.browInnerUp) ?? 0)
                jawValue = (node.blendShape(.jawOpen) ?? 0)
            },
            onFaceLost: { _ in
                isTracking = false
                smileValue = 0
                browValue = 0
                jawValue = 0
            }
        )
    }
    #endif

    // MARK: - Expression Meters

    private var expressionMeters: some View {
        VStack(spacing: 8) {
            expressionBar(name: "Smile", value: smileValue, color: .green)
            expressionBar(name: "Brow Up", value: browValue, color: .blue)
            expressionBar(name: "Jaw Open", value: jawValue, color: .orange)
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
        .transition(.move(edge: .bottom).combined(with: .opacity))
        .animation(.easeInOut(duration: 0.3), value: isTracking)
    }

    private func expressionBar(name: String, value: Float, color: Color) -> some View {
        HStack(spacing: 8) {
            Text(name)
                .font(.caption)
                .foregroundStyle(.white.opacity(0.7))
                .frame(width: 60, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(.white.opacity(0.1))
                    RoundedRectangle(cornerRadius: 4)
                        .fill(color)
                        .frame(width: max(0, geo.size.width * CGFloat(value)))
                }
            }
            .frame(height: 12)

            Text("\(Int(value * 100))%")
                .font(.caption2.monospacedDigit())
                .foregroundStyle(.white.opacity(0.7))
                .frame(width: 36)
        }
        .accessibilityLabel("\(name): \(Int(value * 100)) percent")
    }

    // MARK: - Color Selector

    private var colorSelector: some View {
        HStack(spacing: 12) {
            ForEach(Array(colorOptions.enumerated()), id: \.offset) { index, option in
                Button {
                    meshColor = option.1
                    #if os(iOS)
                    HapticManager.selectionChanged()
                    #endif
                } label: {
                    Circle()
                        .fill(option.1)
                        .frame(width: 36, height: 36)
                        .overlay {
                            if meshColor == option.1 {
                                Circle()
                                    .strokeBorder(.white, lineWidth: 3)
                            }
                        }
                }
                .accessibilityLabel("\(option.0) face mesh color")
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding()
    }

    // MARK: - Fallbacks

    private var unsupportedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "face.smiling")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
            Text("Face Tracking Not Supported")
                .font(.headline)
            Text("This device does not have a TrueDepth camera required for face tracking.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }

    private var simulatorPlaceholder: some View {
        VStack(spacing: 16) {
            Image(systemName: "face.smiling.fill")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
            Text("Face tracking requires a physical device")
                .font(.headline)
            Text("Run on an iPhone with TrueDepth camera to see face mesh and blend shape tracking.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }
}
#endif
