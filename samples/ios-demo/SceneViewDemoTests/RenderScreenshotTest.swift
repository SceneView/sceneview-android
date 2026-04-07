import XCTest
import RealityKit
import SwiftUI

/// Visual verification tests for SceneView iOS — captures screenshots of basic
/// RealityKit scenes and attaches them as XCTAttachments for CI artifact collection.
///
/// Run with:
/// ```
/// xcodebuild test -scheme SceneViewDemo -destination 'platform=iOS Simulator,name=iPhone 16'
/// ```
///
/// Screenshots are saved as test attachments and can be collected from the
/// `xcresult` bundle using `xcresulttool`.
final class RenderScreenshotTest: XCTestCase {

    // MARK: - Helpers

    /// Creates a RealityKit `ARView` (non-AR mode), renders content, and captures a screenshot.
    @MainActor
    private func captureScreenshot(
        name: String,
        size: CGSize = CGSize(width: 512, height: 512),
        configure: (ARView) -> Void
    ) throws {
        let arView = ARView(frame: CGRect(origin: .zero, size: size))
        arView.environment.background = .color(.black)
        // Disable AR session — we only want non-AR rendering
        arView.cameraMode = .nonAR

        configure(arView)

        // Allow a few run-loop ticks for RealityKit to render
        let expectation = expectation(description: "Render \(name)")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 5.0)

        // Snapshot the view
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            arView.drawHierarchy(in: arView.bounds, afterScreenUpdates: true)
        }

        // Attach to test results
        let attachment = XCTAttachment(image: image)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        // Basic sanity: image should exist and have the right size
        XCTAssertEqual(image.size.width, size.width, accuracy: 1.0, "\(name): width mismatch")
        XCTAssertEqual(image.size.height, size.height, accuracy: 1.0, "\(name): height mismatch")
    }

    // MARK: - Tests

    @MainActor
    func testCubeNode() throws {
        try captureScreenshot(name: "ios_01_cube") { arView in
            let anchor = AnchorEntity(world: [0, 0, -1.5])

            let mesh = MeshResource.generateBox(size: 0.4)
            let material = SimpleMaterial(color: .red, isMetallic: false)
            let cube = ModelEntity(mesh: mesh, materials: [material])
            anchor.addChild(cube)

            arView.scene.addAnchor(anchor)
        }
    }

    @MainActor
    func testSphereNode() throws {
        try captureScreenshot(name: "ios_02_sphere") { arView in
            let anchor = AnchorEntity(world: [0, 0, -1.5])

            let mesh = MeshResource.generateSphere(radius: 0.3)
            let material = SimpleMaterial(color: .green, isMetallic: true)
            let sphere = ModelEntity(mesh: mesh, materials: [material])
            anchor.addChild(sphere)

            arView.scene.addAnchor(anchor)
        }
    }

    @MainActor
    func testLightAndCube() throws {
        try captureScreenshot(name: "ios_03_light_cube") { arView in
            let anchor = AnchorEntity(world: [0, 0, -2.0])

            // White cube
            let mesh = MeshResource.generateBox(size: 0.5)
            let material = SimpleMaterial(color: .white, isMetallic: false)
            let cube = ModelEntity(mesh: mesh, materials: [material])
            anchor.addChild(cube)

            // Point light
            let light = PointLight()
            light.light.intensity = 5000
            light.light.color = .orange
            light.position = [0.5, 0.5, 0.5]
            anchor.addChild(light)

            arView.scene.addAnchor(anchor)
        }
    }

    @MainActor
    func testMultipleShapes() throws {
        try captureScreenshot(name: "ios_04_multi_shapes") { arView in
            let anchor = AnchorEntity(world: [0, 0, -2.5])

            // Red cube
            let cubeMesh = MeshResource.generateBox(size: 0.3)
            let cubeEntity = ModelEntity(
                mesh: cubeMesh,
                materials: [SimpleMaterial(color: .red, isMetallic: false)]
            )
            cubeEntity.position = [-0.5, 0, 0]
            anchor.addChild(cubeEntity)

            // Green sphere
            let sphereMesh = MeshResource.generateSphere(radius: 0.2)
            let sphereEntity = ModelEntity(
                mesh: sphereMesh,
                materials: [SimpleMaterial(color: .green, isMetallic: true)]
            )
            sphereEntity.position = [0, 0, 0]
            anchor.addChild(sphereEntity)

            // Blue cylinder
            let cylMesh = MeshResource.generateCylinder(height: 0.4, radius: 0.15)
            let cylEntity = ModelEntity(
                mesh: cylMesh,
                materials: [SimpleMaterial(color: .blue, isMetallic: false)]
            )
            cylEntity.position = [0.5, 0, 0]
            anchor.addChild(cylEntity)

            // Directional light
            let dirLight = DirectionalLight()
            dirLight.light.intensity = 3000
            dirLight.look(at: [0, 0, 0], from: [1, 2, 1], relativeTo: nil)
            anchor.addChild(dirLight)

            arView.scene.addAnchor(anchor)
        }
    }

    @MainActor
    func testPlaneNode() throws {
        try captureScreenshot(name: "ios_05_plane") { arView in
            let anchor = AnchorEntity(world: [0, -0.5, -2.0])

            let mesh = MeshResource.generatePlane(width: 1.0, depth: 1.0)
            let material = SimpleMaterial(color: .yellow, isMetallic: false)
            let plane = ModelEntity(mesh: mesh, materials: [material])
            anchor.addChild(plane)

            arView.scene.addAnchor(anchor)
        }
    }
}
