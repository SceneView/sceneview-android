#if os(iOS) || os(macOS) || os(visionOS)
import XCTest
@testable import SceneViewSwift

final class ImageNodeTests: XCTestCase {

    func testColorFactory() {
        let node = ImageNode.color(.red, width: 2.0, height: 1.5)
        // Entity should be created with a mesh
        XCTAssertNotNil(node.entity.model)
        XCTAssertEqual(node.position, .zero)
    }

    func testPosition() {
        let node = ImageNode.color(.blue)
            .position(.init(x: 1, y: 2, z: 3))
        XCTAssertEqual(node.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.position.z, 3.0, accuracy: 0.001)
    }

    func testScale() {
        let node = ImageNode.color(.green)
            .scale(2.0)
        XCTAssertEqual(node.scale.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.z, 2.0, accuracy: 0.001)
    }

    func testRotation() {
        let quat = simd_quatf(angle: .pi / 2, axis: SIMD3<Float>(1, 0, 0))
        let node = ImageNode.color(.white)
            .rotation(quat)
        XCTAssertEqual(node.rotation.angle, quat.angle, accuracy: 0.001)
    }

    func testRotationAngleAxis() {
        let node = ImageNode.color(.white)
            .rotation(angle: .pi / 4, axis: SIMD3<Float>(0, 1, 0))
        XCTAssertEqual(node.rotation.angle, .pi / 4, accuracy: 0.001)
    }

    func testChaining() {
        let node = ImageNode.color(.red, width: 1.0, height: 0.75)
            .position(.init(x: 0, y: 1, z: -2))
            .scale(0.5)
        XCTAssertEqual(node.position.y, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.x, 0.5, accuracy: 0.001)
    }

    func testSize() {
        let node = ImageNode.color(.white)
            .size(width: 3.0, height: 2.0)
        // After resize, the mesh should have been regenerated
        XCTAssertNotNil(node.entity.model?.mesh)
    }

    func testCollisionGenerated() {
        let node = ImageNode.color(.red)
        // Collision should be auto-generated
        XCTAssertNotNil(node.entity.components[RealityKit.CollisionComponent.self])
    }

    func testLoadFailsWithBadName() async {
        do {
            _ = try await ImageNode.load("nonexistent_image_12345.png")
            XCTFail("Should have thrown an error")
        } catch {
            XCTAssertTrue(error is ImageNode.ImageNodeError)
        }
    }
}
#endif
