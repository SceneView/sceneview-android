#if os(iOS) || os(macOS) || os(visionOS)
import XCTest
import RealityKit
@testable import SceneViewSwift

final class CameraNodeTests: XCTestCase {

    func testInit() {
        let camera = CameraNode()
        XCTAssertEqual(camera.position, .zero)
        XCTAssertEqual(camera.entity.name, "CameraNode")
    }

    func testPosition() {
        let camera = CameraNode()
            .position(.init(x: 1, y: 2, z: 3))
        XCTAssertEqual(camera.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(camera.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(camera.position.z, 3.0, accuracy: 0.001)
    }

    func testPositionChaining() {
        let camera = CameraNode()
            .position(.init(x: 1, y: 0, z: 0))
        XCTAssertEqual(camera.position.x, 1.0, accuracy: 0.001)
    }

    func testRotation() {
        let quat = simd_quatf(angle: .pi / 4, axis: SIMD3<Float>(0, 1, 0))
        let camera = CameraNode()
            .rotation(quat)
        XCTAssertEqual(camera.rotation.angle, quat.angle, accuracy: 0.001)
    }

    func testLookAt() {
        let camera = CameraNode()
            .position(.init(x: 0, y: 0, z: 5))
            .lookAt(.zero)
        // After lookAt, the camera should have a non-identity orientation
        let angle = camera.rotation.angle
        XCTAssertGreaterThan(abs(angle), 0.001)
    }

    func testTransformMatrix() {
        let camera = CameraNode()
            .position(.init(x: 1, y: 2, z: 3))
        let matrix = camera.transform
        // Translation should be in the last column
        XCTAssertEqual(matrix.columns.3.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(matrix.columns.3.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(matrix.columns.3.z, 3.0, accuracy: 0.001)
    }

    #if !os(macOS)
    func testClipPlanes() {
        let camera = CameraNode()
            .clipPlanes(near: 0.1, far: 500)
        XCTAssertEqual(camera.nearClip, 0.1, accuracy: 0.001)
        XCTAssertEqual(camera.farClip, 500.0, accuracy: 0.001)
    }
    #endif

    func testWrapExistingEntity() {
        let entity = RealityKit.Entity()
        entity.position = .init(x: 5, y: 0, z: 0)
        let camera = CameraNode(entity)
        XCTAssertEqual(camera.position.x, 5.0, accuracy: 0.001)
    }

    // MARK: - Field of view

    #if !os(macOS)
    func testFieldOfView() {
        let camera = CameraNode()
            .fieldOfView(60.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
        XCTAssertEqual(component?.fieldOfViewInDegrees, 60.0, accuracy: 0.001)
    }
    #endif

    // MARK: - Depth of field

    #if !os(macOS)
    func testDepthOfField() {
        let camera = CameraNode()
            .depthOfField(focusDistance: 2.0, aperture: 1.4)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
    }
    #endif

    // MARK: - Exposure

    #if !os(macOS)
    func testExposure() {
        let camera = CameraNode()
            .exposure(2.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
    }
    #endif

    // MARK: - Chaining

    #if !os(macOS)
    func testFullChaining() {
        let camera = CameraNode()
            .position(.init(x: 0, y: 1.5, z: 3))
            .lookAt(.zero)
            .fieldOfView(75.0)
            .clipPlanes(near: 0.1, far: 500)
            .exposure(1.5)
        XCTAssertEqual(camera.position.y, 1.5, accuracy: 0.001)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
        XCTAssertEqual(component?.fieldOfViewInDegrees, 75.0, accuracy: 0.001)
        XCTAssertEqual(component?.near, 0.1, accuracy: 0.001)
        XCTAssertEqual(component?.far, 500.0, accuracy: 0.001)
    }
    #endif

    // MARK: - Property get/set

    func testPositionPropertyGetSet() {
        var camera = CameraNode()
        camera.position = SIMD3<Float>(10, 20, 30)
        XCTAssertEqual(camera.position.x, 10.0, accuracy: 0.001)
        XCTAssertEqual(camera.position.y, 20.0, accuracy: 0.001)
        XCTAssertEqual(camera.position.z, 30.0, accuracy: 0.001)
    }

    func testRotationPropertyGetSet() {
        var camera = CameraNode()
        let quat = simd_quatf(angle: .pi / 6, axis: SIMD3<Float>(1, 0, 0))
        camera.rotation = quat
        XCTAssertEqual(camera.rotation.angle, quat.angle, accuracy: 0.001)
    }

    // MARK: - Default clipping planes

    #if !os(macOS)
    func testDefaultNearClip() {
        let camera = CameraNode()
        XCTAssertEqual(camera.nearClip, 0.01, accuracy: 0.001)
    }

    func testDefaultFarClip() {
        let camera = CameraNode()
        XCTAssertEqual(camera.farClip, 1000.0, accuracy: 0.001)
    }

    func testNearClipSetGet() {
        let camera = CameraNode()
        camera.nearClip = 0.5
        XCTAssertEqual(camera.nearClip, 0.5, accuracy: 0.001)
    }

    func testFarClipSetGet() {
        let camera = CameraNode()
        camera.farClip = 200.0
        XCTAssertEqual(camera.farClip, 200.0, accuracy: 0.001)
    }
    #endif

    // MARK: - FOV edge cases

    #if !os(macOS)
    func testFieldOfViewZeroDegrees() {
        let camera = CameraNode().fieldOfView(0.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
    }

    func testFieldOfViewWideAngle() {
        let camera = CameraNode().fieldOfView(120.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
        XCTAssertEqual(component?.fieldOfViewInDegrees, 120.0, accuracy: 0.001)
    }

    func testFieldOfViewNarrow() {
        let camera = CameraNode().fieldOfView(10.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertEqual(component?.fieldOfViewInDegrees, 10.0, accuracy: 0.001)
    }
    #endif

    // MARK: - Look at with custom up

    func testLookAtWithCustomUp() {
        let camera = CameraNode()
            .position(.init(x: 0, y: 0, z: 5))
            .lookAt(.zero, up: SIMD3<Float>(0, 1, 0))
        XCTAssertNotNil(camera.entity)
        XCTAssertGreaterThan(abs(camera.rotation.angle), 0.001)
    }

    // MARK: - Depth of field chaining

    #if !os(macOS)
    func testDepthOfFieldChaining() {
        let camera = CameraNode()
            .depthOfField(focusDistance: 5.0, aperture: 2.8)
            .fieldOfView(60.0)
        let component = camera.entity.components[PerspectiveCameraComponent.self]
        XCTAssertNotNil(component)
        XCTAssertEqual(component?.fieldOfViewInDegrees, 60.0, accuracy: 0.001)
    }
    #endif
}
#endif
