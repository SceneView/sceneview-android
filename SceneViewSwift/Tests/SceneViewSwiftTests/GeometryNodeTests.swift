import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class GeometryNodeTests: XCTestCase {

    // MARK: - Factory methods

    func testCubeCreatesEntity() {
        let cube = GeometryNode.cube(size: 0.5, color: .red)
        XCTAssertNotNil(cube.entity)
        XCTAssertNotNil(cube.entity.model)
    }

    func testSphereCreatesEntity() {
        let sphere = GeometryNode.sphere(radius: 0.3, color: .blue)
        XCTAssertNotNil(sphere.entity)
        XCTAssertNotNil(sphere.entity.model)
    }

    func testCylinderCreatesEntity() {
        let cyl = GeometryNode.cylinder(radius: 0.2, height: 0.8, color: .green)
        XCTAssertNotNil(cyl.entity)
        XCTAssertNotNil(cyl.entity.model)
    }

    func testPlaneCreatesEntity() {
        let plane = GeometryNode.plane(width: 2.0, depth: 1.5, color: .gray)
        XCTAssertNotNil(plane.entity)
        XCTAssertNotNil(plane.entity.model)
    }

    func testConeCreatesEntity() {
        let cone = GeometryNode.cone(height: 1.0, radius: 0.5, color: .yellow)
        XCTAssertNotNil(cone.entity)
        XCTAssertNotNil(cone.entity.model)
    }

    // MARK: - Materials

    func testCubeWithPBRMaterial() {
        let cube = GeometryNode.cube(
            size: 1.0,
            material: .pbr(color: .gray, metallic: 1.0, roughness: 0.2)
        )
        XCTAssertNotNil(cube.entity.model)
        XCTAssertEqual(cube.entity.model?.materials.count, 1)
    }

    func testCubeWithSimpleMaterial() {
        let cube = GeometryNode.cube(
            size: 1.0,
            material: .simple(color: .red)
        )
        XCTAssertNotNil(cube.entity.model)
    }

    func testSphereWithUnlitMaterial() {
        let sphere = GeometryNode.sphere(
            radius: 0.5,
            material: .unlit(color: .cyan)
        )
        XCTAssertNotNil(sphere.entity.model)
        XCTAssertEqual(sphere.entity.model?.materials.count, 1)
    }

    // MARK: - Transform helpers

    func testPositionSetsEntityPosition() {
        let cube = GeometryNode.cube(size: 1.0)
        let pos = SIMD3<Float>(1, 2, 3)
        cube.position(pos)

        XCTAssertEqual(cube.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(cube.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(cube.entity.position.z, 3.0, accuracy: 0.001)
    }

    func testScaleSetsUniformScale() {
        let sphere = GeometryNode.sphere(radius: 0.5)
        sphere.scale(2.0)

        XCTAssertEqual(sphere.entity.scale.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(sphere.entity.scale.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(sphere.entity.scale.z, 2.0, accuracy: 0.001)
    }

    func testPositionPropertyGetSet() {
        var node = GeometryNode.cube()
        node.position = SIMD3<Float>(5, 6, 7)

        XCTAssertEqual(node.position.x, 5.0, accuracy: 0.001)
        XCTAssertEqual(node.position.y, 6.0, accuracy: 0.001)
        XCTAssertEqual(node.position.z, 7.0, accuracy: 0.001)
    }

    func testScalePropertyGetSet() {
        var node = GeometryNode.sphere()
        node.scale = SIMD3<Float>(2, 3, 4)

        XCTAssertEqual(node.scale.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.y, 3.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.z, 4.0, accuracy: 0.001)
    }

    // MARK: - Default values

    func testCubeDefaultSize() {
        let cube = GeometryNode.cube()
        // Default position should be zero
        XCTAssertEqual(cube.entity.position.x, 0.0, accuracy: 0.001)
        XCTAssertEqual(cube.entity.position.y, 0.0, accuracy: 0.001)
        XCTAssertEqual(cube.entity.position.z, 0.0, accuracy: 0.001)
    }

    func testChainingReturnsModifiedNode() {
        let node = GeometryNode.cube(size: 0.5, color: .red)
            .position(.init(x: 1, y: 2, z: 3))
            .scale(0.5)

        XCTAssertEqual(node.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.x, 0.5, accuracy: 0.001)
    }
}
#endif
