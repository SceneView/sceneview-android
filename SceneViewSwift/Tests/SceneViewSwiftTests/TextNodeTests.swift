import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class TextNodeTests: XCTestCase {

    // MARK: - Creation

    func testTextNodeCreatesEntity() {
        let node = TextNode(text: "Hello")
        XCTAssertNotNil(node.entity)
        XCTAssertNotNil(node.entity.model)
    }

    func testTextPropertyStored() {
        let node = TextNode(text: "Test String")
        XCTAssertEqual(node.text, "Test String")
    }

    func testTextWithCustomFontSize() {
        let node = TextNode(text: "Big", fontSize: 0.2)
        XCTAssertNotNil(node.entity.model)
        XCTAssertEqual(node.text, "Big")
    }

    func testTextWithCustomFont() {
        let font = MeshResource.Font.systemFont(ofSize: 0.1)
        let node = TextNode(text: "Custom Font", font: font, color: .blue)
        XCTAssertNotNil(node.entity.model)
        XCTAssertEqual(node.text, "Custom Font")
    }

    // MARK: - Transform helpers

    func testPositionSetsEntityPosition() {
        let node = TextNode(text: "Pos")
            .position(.init(x: 1, y: 2, z: -3))

        XCTAssertEqual(node.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.position.z, -3.0, accuracy: 0.001)
    }

    func testScaleSetsUniformScale() {
        let node = TextNode(text: "Scale")
            .scale(3.0)

        XCTAssertEqual(node.entity.scale.x, 3.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.y, 3.0, accuracy: 0.001)
        XCTAssertEqual(node.entity.scale.z, 3.0, accuracy: 0.001)
    }

    func testPositionPropertyGetSet() {
        var node = TextNode(text: "Props")
        node.position = SIMD3<Float>(4, 5, 6)

        XCTAssertEqual(node.position.x, 4.0, accuracy: 0.001)
        XCTAssertEqual(node.position.y, 5.0, accuracy: 0.001)
        XCTAssertEqual(node.position.z, 6.0, accuracy: 0.001)
    }

    // MARK: - withText

    func testWithTextCreatesNewNodeWithUpdatedText() {
        let original = TextNode(text: "Original")
            .position(.init(x: 1, y: 2, z: 3))
            .scale(2.0)

        let updated = original.withText("Updated")

        XCTAssertEqual(updated.text, "Updated")
        // Position should be preserved
        XCTAssertEqual(updated.entity.position.x, 1.0, accuracy: 0.001)
        XCTAssertEqual(updated.entity.position.y, 2.0, accuracy: 0.001)
        XCTAssertEqual(updated.entity.position.z, 3.0, accuracy: 0.001)
    }

    // MARK: - Centering

    func testCenteredDoesNotCrash() {
        let node = TextNode(text: "Center me")
            .centered()
        XCTAssertNotNil(node.entity)
    }

    // MARK: - Chaining

    func testChainingTransforms() {
        let node = TextNode(text: "Chain", fontSize: 0.08, color: .red)
            .position(.init(x: 0, y: 1, z: -2))
            .scale(0.5)

        XCTAssertEqual(node.position.y, 1.0, accuracy: 0.001)
        XCTAssertEqual(node.scale.x, 0.5, accuracy: 0.001)
    }
}
#endif
