import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class ReflectionProbeNodeTests: XCTestCase {

    // MARK: - Factory methods

    func testBoxFactoryCreatesEntity() {
        let probe = ReflectionProbeNode.box(size: [2, 3, 4])
        XCTAssertNotNil(probe.entity)
        XCTAssertTrue(probe.entity.name.contains("Box"))
    }

    func testBoxFactoryDefaultSize() {
        let probe = ReflectionProbeNode.box()
        XCTAssertNotNil(probe.entity)
        if case .box(let size) = probe.shape {
            XCTAssertEqual(size.x, 1.0, accuracy: 0.001)
            XCTAssertEqual(size.y, 1.0, accuracy: 0.001)
            XCTAssertEqual(size.z, 1.0, accuracy: 0.001)
        } else {
            XCTFail("Expected box shape")
        }
    }

    func testSphereFactoryCreatesEntity() {
        let probe = ReflectionProbeNode.sphere(radius: 3.0)
        XCTAssertNotNil(probe.entity)
        XCTAssertTrue(probe.entity.name.contains("Sphere"))
    }

    func testSphereFactoryDefaultRadius() {
        let probe = ReflectionProbeNode.sphere()
        if case .sphere(let radius) = probe.shape {
            XCTAssertEqual(radius, 1.0, accuracy: 0.001)
        } else {
            XCTFail("Expected sphere shape")
        }
    }

    // MARK: - Shape enum

    func testShapeBoxStoresSize() {
        let probe = ReflectionProbeNode.box(size: [5, 6, 7])
        if case .box(let size) = probe.shape {
            XCTAssertEqual(size.x, 5.0, accuracy: 0.001)
            XCTAssertEqual(size.y, 6.0, accuracy: 0.001)
            XCTAssertEqual(size.z, 7.0, accuracy: 0.001)
        } else {
            XCTFail("Expected box shape")
        }
    }

    func testShapeSphereStoresRadius() {
        let probe = ReflectionProbeNode.sphere(radius: 2.5)
        if case .sphere(let radius) = probe.shape {
            XCTAssertEqual(radius, 2.5, accuracy: 0.001)
        } else {
            XCTFail("Expected sphere shape")
        }
    }

    // MARK: - Position

    func testPositionBuilderSetsEntityPosition() {
        let probe = ReflectionProbeNode.box()
            .position(.init(x: 1, y: 2, z: 3))

        XCTAssertEqual(probe.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(probe.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(probe.entity.position.z, 3.0, accuracy: 0.001)
    }

    func testPositionPropertyGetSet() {
        var probe = ReflectionProbeNode.sphere(radius: 1.0)
        probe.position = SIMD3<Float>(4, 5, 6)

        XCTAssertEqual(probe.position.x, 4.0, accuracy: 0.001)
        XCTAssertEqual(probe.position.y, 5.0, accuracy: 0.001)
        XCTAssertEqual(probe.position.z, 6.0, accuracy: 0.001)
    }

    func testScalePropertyGetSet() {
        var probe = ReflectionProbeNode.box()
        probe.scale = SIMD3<Float>(2, 2, 2)

        XCTAssertEqual(probe.scale.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(probe.scale.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(probe.scale.z, 2.0, accuracy: 0.001)
    }

    // MARK: - Intensity

    func testIntensityBuilderUpdatesEntityName() {
        let probe = ReflectionProbeNode.box()
            .intensity(0.5)

        XCTAssertTrue(probe.entity.name.contains("0.5"))
    }

    func testDefaultIntensityIsOne() {
        let probe = ReflectionProbeNode.box()
        XCTAssertTrue(probe.entity.name.contains("1.0"))
    }

    // MARK: - Volume size

    func testVolumeSizeForBox() {
        let probe = ReflectionProbeNode.box(size: [2, 3, 4])
        XCTAssertEqual(probe.volumeSize.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(probe.volumeSize.y, 3.0, accuracy: 0.001)
        XCTAssertEqual(probe.volumeSize.z, 4.0, accuracy: 0.001)
    }

    func testVolumeSizeForSphere() {
        let probe = ReflectionProbeNode.sphere(radius: 3.0)
        XCTAssertEqual(probe.volumeSize.x, 6.0, accuracy: 0.001)
        XCTAssertEqual(probe.volumeSize.y, 6.0, accuracy: 0.001)
        XCTAssertEqual(probe.volumeSize.z, 6.0, accuracy: 0.001)
    }

    // MARK: - Contains

    func testBoxContainsPointInside() {
        let probe = ReflectionProbeNode.box(size: [2, 2, 2])
        XCTAssertTrue(probe.contains(.init(x: 0.5, y: 0.5, z: 0.5)))
    }

    func testBoxDoesNotContainPointOutside() {
        let probe = ReflectionProbeNode.box(size: [2, 2, 2])
        XCTAssertFalse(probe.contains(.init(x: 2.0, y: 0, z: 0)))
    }

    func testBoxContainsPointOnBoundary() {
        let probe = ReflectionProbeNode.box(size: [2, 2, 2])
        XCTAssertTrue(probe.contains(.init(x: 1.0, y: 0, z: 0)))
    }

    func testBoxContainsAccountsForPosition() {
        let probe = ReflectionProbeNode.box(size: [2, 2, 2])
            .position(.init(x: 5, y: 0, z: 0))
        XCTAssertFalse(probe.contains(.init(x: 0, y: 0, z: 0)))
        XCTAssertTrue(probe.contains(.init(x: 5, y: 0, z: 0)))
    }

    func testSphereContainsPointInside() {
        let probe = ReflectionProbeNode.sphere(radius: 2.0)
        XCTAssertTrue(probe.contains(.init(x: 1.0, y: 0, z: 0)))
    }

    func testSphereDoesNotContainPointOutside() {
        let probe = ReflectionProbeNode.sphere(radius: 2.0)
        XCTAssertFalse(probe.contains(.init(x: 3.0, y: 0, z: 0)))
    }

    func testSphereContainsPointOnBoundary() {
        let probe = ReflectionProbeNode.sphere(radius: 2.0)
        XCTAssertTrue(probe.contains(.init(x: 2.0, y: 0, z: 0)))
    }

    func testSphereContainsAccountsForPosition() {
        let probe = ReflectionProbeNode.sphere(radius: 1.0)
            .position(.init(x: 10, y: 0, z: 0))
        XCTAssertFalse(probe.contains(.init(x: 0, y: 0, z: 0)))
        XCTAssertTrue(probe.contains(.init(x: 10.5, y: 0, z: 0)))
    }

    // MARK: - Builder chaining

    func testBuilderMethodsChain() {
        let probe = ReflectionProbeNode.sphere(radius: 2.0)
            .position(.init(x: 1, y: 2, z: 3))
            .intensity(0.8)

        XCTAssertEqual(probe.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(probe.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(probe.entity.position.z, 3.0, accuracy: 0.001)
        XCTAssertTrue(probe.entity.name.contains("0.8"))
    }

    func testBoxBuilderMethodsChain() {
        let probe = ReflectionProbeNode.box(size: [4, 3, 4])
            .position(.init(x: 0, y: 1.5, z: 0))
            .intensity(1.2)

        XCTAssertEqual(probe.entity.position.y, 1.5, accuracy: 0.001)
        XCTAssertTrue(probe.entity.name.contains("1.2"))
    }
}
#endif
