import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class LightNodeTests: XCTestCase {

    // MARK: - Factory methods

    func testDirectionalLightCreatesEntity() {
        let light = LightNode.directional()
        XCTAssertNotNil(light.entity)
        XCTAssertTrue(light.entity is DirectionalLight)
    }

    func testPointLightCreatesEntity() {
        let light = LightNode.point()
        XCTAssertNotNil(light.entity)
        XCTAssertTrue(light.entity is PointLight)
    }

    func testSpotLightCreatesEntity() {
        let light = LightNode.spot()
        XCTAssertNotNil(light.entity)
        XCTAssertTrue(light.entity is SpotLight)
    }

    // MARK: - Color enum

    func testColorEnumWhite() {
        let color = LightNode.Color.white
        // Verify the enum case exists and can be used
        let light = LightNode.directional(color: color)
        XCTAssertNotNil(light.entity)
    }

    func testColorEnumWarm() {
        let light = LightNode.directional(color: .warm)
        XCTAssertNotNil(light.entity)
    }

    func testColorEnumCool() {
        let light = LightNode.directional(color: .cool)
        XCTAssertNotNil(light.entity)
    }

    func testColorEnumCustom() {
        let light = LightNode.point(color: .custom(r: 1.0, g: 0.5, b: 0.0))
        XCTAssertNotNil(light.entity)
    }

    // MARK: - Position & lookAt

    func testPositionSetsEntityPosition() {
        let light = LightNode.directional()
            .position(.init(x: 2, y: 4, z: 2))

        XCTAssertEqual(light.entity.position.x, 2.0, accuracy: 0.001)
        XCTAssertEqual(light.entity.position.y, 4.0, accuracy: 0.001)
        XCTAssertEqual(light.entity.position.z, 2.0, accuracy: 0.001)
    }

    func testLookAtChangesOrientation() {
        let light = LightNode.directional()
            .position(.init(x: 0, y: 5, z: 0))

        let initialOrientation = light.entity.orientation
        light.lookAt(.zero)

        // Orientation should have changed after lookAt
        let newOrientation = light.entity.orientation
        let changed = initialOrientation.real != newOrientation.real ||
            initialOrientation.imag != newOrientation.imag
        XCTAssertTrue(changed, "lookAt should change the orientation")
    }

    // MARK: - Properties

    func testPositionPropertyGetSet() {
        var light = LightNode.point()
        light.position = SIMD3<Float>(3, 2, 1)

        XCTAssertEqual(light.position.x, 3.0, accuracy: 0.001)
        XCTAssertEqual(light.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(light.position.z, 1.0, accuracy: 0.001)
    }

    func testRotationPropertyGetSet() {
        var light = LightNode.spot()
        let quat = simd_quatf(angle: .pi / 4, axis: .init(x: 0, y: 1, z: 0))
        light.rotation = quat

        XCTAssertEqual(light.rotation.angle, Float.pi / 4, accuracy: 0.01)
    }
}
#endif
