#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import RealityKit

/// A SwiftUI view for rendering 3D content using RealityKit.
///
/// Mirrors SceneView Android's `Scene { }` composable — declare nodes declaratively
/// inside the content builder, with built-in orbit camera, environment lighting,
/// and gesture handling.
///
/// ```swift
/// @State private var model: ModelNode?
///
/// SceneView { root in
///     if let model {
///         root.addChild(model.entity)
///     }
/// }
/// .environment(.studio)
/// .cameraControls(.orbit)
/// .onEntityTapped { entity in
///     print("Tapped: \(entity)")
/// }
/// .task {
///     model = try? await ModelNode.load("models/car.usdz")
/// }
/// ```
public struct SceneView: View {
    let content: (Entity) -> Void
    var sceneEnvironment: SceneEnvironment?
    var cameraControlMode: CameraControlMode?
    var onEntityTapped: ((Entity) -> Void)?
    var enableAutoRotate: Bool = false
    var autoRotateSpeed: Float = 0.3

    /// Creates a 3D scene with declarative content.
    ///
    /// - Parameter content: A closure that populates the scene. Receives a root
    ///   entity — add your content as children of this entity.
    public init(_ content: @escaping (Entity) -> Void) {
        self.content = content
    }

    public var body: some View {
        SceneViewRepresentation(
            content: content,
            sceneEnvironment: sceneEnvironment,
            cameraControlMode: cameraControlMode ?? .orbit,
            onEntityTapped: onEntityTapped,
            enableAutoRotate: enableAutoRotate,
            autoRotateSpeed: autoRotateSpeed
        )
    }

    // MARK: - View Modifiers

    /// Sets the IBL environment for the scene.
    public func environment(_ environment: SceneEnvironment) -> SceneView {
        var copy = self
        copy.sceneEnvironment = environment
        return copy
    }

    /// Sets the camera control mode (orbit, pan, first-person).
    public func cameraControls(_ mode: CameraControlMode) -> SceneView {
        var copy = self
        copy.cameraControlMode = mode
        return copy
    }

    /// Called when an entity in the scene is tapped.
    public func onEntityTapped(_ handler: @escaping (Entity) -> Void) -> SceneView {
        var copy = self
        copy.onEntityTapped = handler
        return copy
    }

    /// Enables automatic camera rotation around the scene.
    ///
    /// - Parameter speed: Rotation speed in radians per second. Default 0.3.
    public func autoRotate(speed: Float = 0.3) -> SceneView {
        var copy = self
        copy.enableAutoRotate = true
        copy.autoRotateSpeed = speed
        return copy
    }
}

// MARK: - Internal implementation

/// Internal view that manages the RealityView, camera, gestures, and environment.
private struct SceneViewRepresentation: View {
    let content: (Entity) -> Void
    let sceneEnvironment: SceneEnvironment?
    let cameraControlMode: CameraControlMode
    let onEntityTapped: ((Entity) -> Void)?
    let enableAutoRotate: Bool
    let autoRotateSpeed: Float

    @State private var camera = CameraControls(mode: .orbit)
    @State private var rootEntity = Entity()
    @State private var iblEntity = Entity()
    @State private var lastDragTranslation: CGSize = .zero
    @State private var initialPinchRadius: Float? = nil
    @State private var isDragging = false
    @State private var isSetUp = false

    var body: some View {
        RealityView { realityContent in
            setupScene(realityContent)
        } update: { _ in
            applyCamera()
        }
        .gesture(dragGesture)
        .gesture(pinchGesture)
        .simultaneousGesture(tapGesture)
        .task {
            // Auto-rotation loop
            guard enableAutoRotate else { return }
            camera.isAutoRotating = true
            camera.autoRotateSpeed = autoRotateSpeed
            var lastTime = CFAbsoluteTimeGetCurrent()
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 16_666_667) // ~60 fps
                let now = CFAbsoluteTimeGetCurrent()
                let dt = Float(now - lastTime)
                lastTime = now
                if !isDragging {
                    camera.applyAutoRotation(dt: dt)
                }
            }
        }
        .task(id: sceneEnvironment?.name) {
            // Load and apply IBL environment when it changes
            guard let env = sceneEnvironment else { return }
            await loadEnvironment(env)
        }
    }

    // MARK: - Scene Setup

    private func setupScene(_ realityContent: RealityViewContent) {
        guard !isSetUp else { return }
        isSetUp = true

        // Root entity holds all user content
        realityContent.add(rootEntity)

        // IBL entity (will be configured when environment loads)
        realityContent.add(iblEntity)

        // Default directional light (sun)
        let sun = DirectionalLight()
        sun.light = DirectionalLightComponent(
            color: .white,
            intensity: 1000,
            isRealWorldProxy: false
        )
        sun.shadow = DirectionalLightComponent.Shadow(
            maximumDistance: 8,
            depthBias: 5.0
        )
        sun.look(at: .zero, from: [2, 4, 3], relativeTo: nil)
        realityContent.add(sun)

        // Fill light to soften shadows
        let fill = DirectionalLight()
        fill.light = DirectionalLightComponent(
            color: .white,
            intensity: 300,
            isRealWorldProxy: false
        )
        fill.look(at: .zero, from: [-1, -2, -1], relativeTo: nil)
        realityContent.add(fill)

        // Populate user content
        content(rootEntity)
    }

    // MARK: - Environment IBL

    @MainActor
    private func loadEnvironment(_ env: SceneEnvironment) async {
        do {
            let resource = try await env.load()
            // Apply IBL to the scene via ImageBasedLightComponent
            iblEntity.components.set(
                ImageBasedLightComponent(
                    source: .single(resource),
                    intensityExponent: env.intensity
                )
            )
            // Make root entity receive IBL
            rootEntity.components.set(
                ImageBasedLightReceiverComponent(imageBasedLight: iblEntity)
            )
        } catch {
            // Environment loading failed — scene continues with default lighting
            print("[SceneViewSwift] Failed to load environment '\(env.name)': \(error)")
        }
    }

    // MARK: - Camera

    private func applyCamera() {
        let yaw = simd_quatf(angle: -camera.azimuth, axis: [0, 1, 0])
        let pitch = simd_quatf(angle: -camera.elevation, axis: [1, 0, 0])
        rootEntity.orientation = yaw * pitch

        let zoomScale = 5.0 / camera.orbitRadius
        rootEntity.scale = SIMD3<Float>(repeating: zoomScale)
    }

    // MARK: - Gestures

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                let delta = CGSize(
                    width: value.translation.width - lastDragTranslation.width,
                    height: value.translation.height - lastDragTranslation.height
                )
                camera.handleDrag(delta)
                lastDragTranslation = value.translation
                isDragging = true
            }
            .onEnded { _ in
                lastDragTranslation = .zero
                isDragging = false
            }
    }

    private var pinchGesture: some Gesture {
        MagnifyGesture()
            .onChanged { value in
                if initialPinchRadius == nil {
                    initialPinchRadius = camera.orbitRadius
                }
                if let initial = initialPinchRadius {
                    let newRadius = initial / Float(value.magnification)
                    camera.orbitRadius = min(
                        max(newRadius, camera.minRadius),
                        camera.maxRadius
                    )
                }
            }
            .onEnded { _ in
                initialPinchRadius = nil
            }
    }

    private var tapGesture: some Gesture {
        SpatialTapGesture()
            .onEnded { _ in
                onEntityTapped?(rootEntity)
            }
    }
}
#endif // os(iOS) || os(macOS) || os(visionOS)
