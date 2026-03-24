#if os(iOS) || os(macOS) || os(visionOS)
import XCTest
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
}
#endif
