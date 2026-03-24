import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class ModelNodeTests: XCTestCase {

    // MARK: - Initialization

    func testInitWithModelEntity() {
        let entity = ModelEntity()
        let node = ModelNode(entity)
        XCTAssertNotNil(node.entity)
    }

    // MARK: - Position

    func testPositionHelperSetsPosition() {
        let entity = ModelEntity()
        let node = ModelNode(entity)
            .position(.init(x: 1, y: 2, z: 3))

        XCTAssertEqual(node.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.position.z, 3.0, accuracy: 0.001)
    }

    func testPositionPropertyGetSet() {
        var node = ModelNode(ModelEntity())
        node.position = SIMD3<Float>(4, 5, 6)

        XCTAssertEqual(node.position.x, 4.0, accuracy: 0.001)
        XCTAssertEqual(node.position.y, 5.0, accuracy: 0.001)
        XCTAssertEqual(node.position.z, 6.0, accuracy: 0.001)
    }

    // MARK: - Scale

    func testUniformScaleHelper() {
        let node = ModelNode(ModelEntity())
            .scale(2.0)

        XCTAssertEqual(node.entity.scale.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.z, 2.0, accuracy: 0.001)
    }

    func testPerAxisScaleHelper() {
        let node = ModelNode(ModelEntity())
            .scale(.init(x: 1, y: 2, z: 3))

        XCTAssertEqual(node.entity.scale.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.z, 3.0, accuracy: 0.001)
    }

    func testScalePropertyGetSet() {
        var node = ModelNode(ModelEntity())
        node.scale = SIMD3<Float>(0.5, 0.5, 0.5)

        XCTAssertEqual(node.scale.x, 0.5, accuracy: 0.001)
        XCTAssertEqual(node.scale.y, 0.5, accuracy: 0.001)
        XCTAssertEqual(node.scale.z, 0.5, accuracy: 0.001)
    }

    // MARK: - Rotation

    func testRotationWithQuaternion() {
        let quat = simd_quatf(angle: .pi / 2, axis: .init(x: 0, y: 1, z: 0))
        let node = ModelNode(ModelEntity())
            .rotation(quat)

        XCTAssertEqual(node.entity.orientation.angle, Float.pi / 2, accuracy: 0.01)
    }

    func testRotationWithAngleAndAxis() {
        let node = ModelNode(ModelEntity())
            .rotation(angle: .pi / 4, axis: .init(x: 1, y: 0, z: 0))

        XCTAssertEqual(node.entity.orientation.angle, Float.pi / 4, accuracy: 0.01)
    }

    func testRotationPropertyGetSet() {
        var node = ModelNode(ModelEntity())
        let quat = simd_quatf(angle: .pi, axis: .init(x: 0, y: 0, z: 1))
        node.rotation = quat

        XCTAssertEqual(node.rotation.angle, Float.pi, accuracy: 0.01)
    }

    // MARK: - scaleToUnits

    func testScaleToUnitsWithGeometry() {
        // Create a known-size box and wrap it in a ModelNode
        let mesh = MeshResource.generateBox(size: 2.0)
        let material = SimpleMaterial(color: .white, isMetallic: false)
        let entity = ModelEntity(mesh: mesh, materials: [material])
        let node = ModelNode(entity)

        node.scaleToUnits(1.0)

        // The box was 2m, scaled to 1m means scale factor = 0.5
        XCTAssertEqual(node.entity.scale.x, 0.5, accuracy: 0.05)
        XCTAssertEqual(node.entity.scale.y, 0.5, accuracy: 0.05)
        XCTAssertEqual(node.entity.scale.z, 0.5, accuracy: 0.05)
    }

    // MARK: - Animation properties

    func testAnimationCountOnEmptyEntity() {
        let node = ModelNode(ModelEntity())
        XCTAssertEqual(node.animationCount, 0)
    }

    // MARK: - Chaining

    func testChainingTransforms() {
        let node = ModelNode(ModelEntity())
            .position(.init(x: 1, y: 0, z: -1))
            .scale(0.5)
            .rotation(angle: .pi / 6, axis: .init(x: 0, y: 1, z: 0))

        XCTAssertEqual(node.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.x, 0.5, accuracy: 0.001)
        XCTAssertEqual(node.entity.orientation.angle, Float.pi / 6, accuracy: 0.01)
    }
}
#endif
