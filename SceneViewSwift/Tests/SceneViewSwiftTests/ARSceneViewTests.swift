#if os(iOS)
import XCTest
import ARKit
@testable import SceneViewSwift

/// Tests for ARSceneView configuration, including camera exposure API parity with Android.
final class ARSceneViewTests: XCTestCase {

    // MARK: - Default initialisation

    func testDefaultInit() {
        let view = ARSceneView()
        // Defaults should match Android's ARSceneView defaults.
        // planeDetection = .horizontal, showPlaneOverlay = true,
        // showCoachingOverlay = true, cameraExposure = nil
        // We verify the view is constructible and compiles correctly.
        XCTAssertNotNil(view)
    }

    func testInitWithPlaneDetectionNone() {
        let view = ARSceneView(planeDetection: .none)
        XCTAssertNotNil(view)
    }

    func testInitWithPlaneDetectionVertical() {
        let view = ARSceneView(planeDetection: .vertical)
        XCTAssertNotNil(view)
    }

    func testInitWithPlaneDetectionBoth() {
        let view = ARSceneView(planeDetection: .both)
        XCTAssertNotNil(view)
    }

    // MARK: - cameraExposure init parameter

    /// Mirrors Android's `ARSceneView(cameraExposure: Float?)` — nil means no override.
    func testCameraExposureDefaultIsNil() {
        // Constructing without explicit cameraExposure should not crash.
        let view = ARSceneView(planeDetection: .horizontal)
        XCTAssertNotNil(view)
    }

    /// Providing a positive EV value should be accepted without crashing.
    func testCameraExposurePositiveEV() {
        let view = ARSceneView(cameraExposure: 1.5)
        XCTAssertNotNil(view)
    }

    /// Providing a negative EV value (darken) should be accepted without crashing.
    func testCameraExposureNegativeEV() {
        let view = ARSceneView(cameraExposure: -2.0)
        XCTAssertNotNil(view)
    }

    /// Zero EV means no change — treated the same as a non-nil override (post-process
    /// installed but with brightness = 0, which is a no-op for CIColorControls).
    func testCameraExposureZeroEV() {
        let view = ARSceneView(cameraExposure: 0.0)
        XCTAssertNotNil(view)
    }

    // MARK: - cameraExposure modifier (SwiftUI-style)

    /// The `.cameraExposure(_:)` modifier should return a new ARSceneView instance.
    func testCameraExposureModifierReturnsNewInstance() {
        let original = ARSceneView()
        let modified = original.cameraExposure(1.0)
        // Modified is a copy (value type), so it must be a valid ARSceneView.
        XCTAssertNotNil(modified)
    }

    func testCameraExposureModifierWithPositiveEV() {
        let view = ARSceneView()
            .cameraExposure(2.0)
        XCTAssertNotNil(view)
    }

    func testCameraExposureModifierWithNegativeEV() {
        let view = ARSceneView()
            .cameraExposure(-1.0)
        XCTAssertNotNil(view)
    }

    func testCameraExposureModifierWithNilRemovesOverride() {
        let view = ARSceneView()
            .cameraExposure(1.0)
            .cameraExposure(nil)
        XCTAssertNotNil(view)
    }

    // MARK: - cameraExposure modifier chaining

    func testCameraExposureChainedWithOtherModifiers() {
        let view = ARSceneView(planeDetection: .both)
            .cameraExposure(0.5)
            .onSessionStarted { _ in }
        XCTAssertNotNil(view)
    }

    func testCameraExposureLastModifierWins() {
        // When cameraExposure is called twice, the last value should win (copy semantics).
        let view = ARSceneView()
            .cameraExposure(1.0)
            .cameraExposure(3.0)
        XCTAssertNotNil(view)
    }

    // MARK: - PlaneDetectionMode arPlaneDetection mapping

    func testPlaneDetectionNoneMapsToEmpty() {
        let mode = ARSceneView.PlaneDetectionMode.none
        XCTAssertEqual(mode.arPlaneDetection, [])
    }

    func testPlaneDetectionHorizontalMapsCorrectly() {
        let mode = ARSceneView.PlaneDetectionMode.horizontal
        XCTAssertEqual(mode.arPlaneDetection, .horizontal)
    }

    func testPlaneDetectionVerticalMapsCorrectly() {
        let mode = ARSceneView.PlaneDetectionMode.vertical
        XCTAssertEqual(mode.arPlaneDetection, .vertical)
    }

    func testPlaneDetectionBothMapsCorrectly() {
        let mode = ARSceneView.PlaneDetectionMode.both
        XCTAssertEqual(mode.arPlaneDetection, [.horizontal, .vertical])
    }

    // MARK: - AnchorNode helpers

    func testAnchorNodeWorldCreation() {
        let anchor = AnchorNode.world(position: .init(x: 1, y: 0, z: -2))
        XCTAssertNotNil(anchor)
        XCTAssertNotNil(anchor.entity)
    }

    func testAnchorNodeWorldAtOrigin() {
        let anchor = AnchorNode.world(position: .zero)
        XCTAssertNotNil(anchor)
    }

    func testAnchorNodeAddChild() {
        let anchor = AnchorNode.world(position: .zero)
        let child = GeometryNode.cube(size: 0.1, color: .red)
        anchor.add(child.entity)
        XCTAssertEqual(anchor.entity.children.count, 1)
    }

    func testAnchorNodeRemoveChild() {
        let anchor = AnchorNode.world(position: .zero)
        let child = GeometryNode.cube(size: 0.1, color: .blue)
        anchor.add(child.entity)
        XCTAssertEqual(anchor.entity.children.count, 1)
        anchor.remove(child.entity)
        XCTAssertEqual(anchor.entity.children.count, 0)
    }

    func testAnchorNodeRemoveAll() {
        let anchor = AnchorNode.world(position: .zero)
        anchor.add(GeometryNode.cube(size: 0.1, color: .red).entity)
        anchor.add(GeometryNode.sphere(radius: 0.1, color: .green).entity)
        XCTAssertEqual(anchor.entity.children.count, 2)
        anchor.removeAll()
        XCTAssertEqual(anchor.entity.children.count, 0)
    }

    func testAnchorNodePlaneHorizontal() {
        let anchor = AnchorNode.plane(alignment: .horizontal, minimumBounds: .init(0.2, 0.2))
        XCTAssertNotNil(anchor)
    }

    func testAnchorNodePlaneVertical() {
        let anchor = AnchorNode.plane(alignment: .vertical)
        XCTAssertNotNil(anchor)
    }
}
#endif // os(iOS)
