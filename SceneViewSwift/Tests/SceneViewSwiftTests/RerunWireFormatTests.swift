import XCTest
@testable import SceneViewSwift

/// Golden-JSON tests for ``RerunWireFormat``.
///
/// These tests target the `*Json` testable overloads directly so they
/// don't need real ARKit instances (ARCamera, ARPlaneAnchor, ARPointCloud
/// are tied to live sessions and can't be constructed in a unit test).
/// The production overloads are thin adapters, so any bug in the
/// serializer is caught here.
///
/// **Cross-platform parity:** the expected JSON strings are IDENTICAL
/// to the ones in the Kotlin test suite (arsceneview
/// RerunWireFormatTest). A mismatch here means the Python sidecar would
/// see different events from Android and iOS clients for the same
/// logical input — an integration-breaking regression.
final class RerunWireFormatTests: XCTestCase {

    func testCameraPoseEmitsCanonicalJsonLine() {
        let line = RerunWireFormat.cameraPoseJson(
            timestampNanos: 123456789,
            tx: 0.1, ty: 1.7, tz: -0.2,
            qx: 0, qy: 0, qz: 0, qw: 1
        )
        let expected =
            "{\"t\":123456789,\"type\":\"camera_pose\",\"entity\":\"world/camera\"," +
            "\"translation\":[0.1,1.7,-0.2],\"quaternion\":[0.0,0.0,0.0,1.0]}\n"
        XCTAssertEqual(line, expected)
    }

    func testCameraPoseTerminatesWithNewline() {
        let line = RerunWireFormat.cameraPoseJson(
            timestampNanos: 0,
            tx: 0, ty: 0, tz: 0,
            qx: 0, qy: 0, qz: 0, qw: 1
        )
        XCTAssertTrue(line.hasSuffix("\n"), "line must end with \\n")
    }

    func testCameraPoseHonoursCustomEntityPath() {
        let line = RerunWireFormat.cameraPoseJson(
            timestampNanos: 1,
            tx: 0, ty: 0, tz: 0,
            qx: 0, qy: 0, qz: 0, qw: 1,
            entity: "world/robot/head"
        )
        XCTAssertTrue(line.contains("\"entity\":\"world/robot/head\""))
    }

    func testCameraPoseEscapesQuoteInEntityPath() {
        let line = RerunWireFormat.cameraPoseJson(
            timestampNanos: 1,
            tx: 0, ty: 0, tz: 0,
            qx: 0, qy: 0, qz: 0, qw: 1,
            entity: "world/bad\"path"
        )
        XCTAssertTrue(line.contains("world/bad\\\"path"))
    }

    func testNonFiniteFloatsClampedToZero() {
        let line = RerunWireFormat.cameraPoseJson(
            timestampNanos: 1,
            tx: .nan, ty: .infinity, tz: -.infinity,
            qx: 0, qy: 0, qz: 0, qw: 1
        )
        XCTAssertFalse(line.contains("nan"), "NaN must not leak into JSON")
        XCTAssertFalse(line.contains("NaN"), "NaN must not leak into JSON")
        XCTAssertFalse(line.contains("inf"), "Infinity must not leak into JSON")
        XCTAssertTrue(line.contains("\"translation\":[0,0,0]"))
    }

    func testAnchorEmitsEntityPathWithId() {
        let line = RerunWireFormat.anchorJson(
            timestampNanos: 1000,
            id: 42,
            tx: 1, ty: 2, tz: 3,
            qx: 0, qy: 0, qz: 0, qw: 1
        )
        XCTAssertTrue(line.contains("\"entity\":\"world/anchors/42\""))
        XCTAssertTrue(line.contains("\"translation\":[1.0,2.0,3.0]"))
        XCTAssertTrue(line.contains("\"quaternion\":[0.0,0.0,0.0,1.0]"))
    }

    func testHitResultCarriesDistance() {
        let line = RerunWireFormat.hitResultJson(
            timestampNanos: 5,
            id: 7,
            tx: 0, ty: 0, tz: -1,
            distance: 1.5
        )
        XCTAssertTrue(line.contains("\"entity\":\"world/hits/7\""))
        XCTAssertTrue(line.contains("\"distance\":1.5"))
    }

    func testPointCloudSplitsFlatPositionsIntoTriples() {
        let positions: [Float] = [1, 2, 3, 4, 5, 6]
        let confidences: [Float] = [0.9, 0.8]
        let line = RerunWireFormat.pointCloudJson(
            timestampNanos: 1,
            positions: positions,
            confidences: confidences
        )
        XCTAssertTrue(
            line.contains("\"positions\":[[1.0,2.0,3.0],[4.0,5.0,6.0]]"),
            "positions should be split into two 3-tuples"
        )
        XCTAssertTrue(
            line.contains("\"confidences\":[0.9,0.8]"),
            "confidences should be emitted alongside positions"
        )
    }

    func testPointCloudHandlesEmptyBuffer() {
        let line = RerunWireFormat.pointCloudJson(
            timestampNanos: 1,
            positions: [],
            confidences: []
        )
        XCTAssertTrue(line.contains("\"positions\":[]"))
        XCTAssertTrue(line.contains("\"confidences\":[]"))
    }

    func testPlaneEmitsWorldSpacePolygonInJsonOrder() {
        let poly: [[Float]] = [
            [0, 0, 0],
            [1, 0, 0],
            [1, 0, 1],
            [0, 0, 1],
        ]
        let line = RerunWireFormat.planeJson(
            timestampNanos: 1,
            id: 99,
            kind: "horizontal_upward",
            worldPolygon: poly
        )
        XCTAssertTrue(line.contains("\"entity\":\"world/planes/99\""))
        XCTAssertTrue(line.contains("\"kind\":\"horizontal_upward\""))
        XCTAssertTrue(
            line.contains("\"polygon\":[[0.0,0.0,0.0],[1.0,0.0,0.0],[1.0,0.0,1.0],[0.0,0.0,1.0]]")
        )
    }

    func testPlaneHandlesEmptyPolygon() {
        let line = RerunWireFormat.planeJson(
            timestampNanos: 1,
            id: 0,
            kind: "vertical",
            worldPolygon: []
        )
        XCTAssertTrue(line.contains("\"polygon\":[]"))
    }

    func testEverySerializedLineContainsExactlyOneNewlineAtTheEnd() {
        let lines = [
            RerunWireFormat.cameraPoseJson(timestampNanos: 1, tx: 0, ty: 0, tz: 0, qx: 0, qy: 0, qz: 0, qw: 1),
            RerunWireFormat.anchorJson(timestampNanos: 1, id: 1, tx: 0, ty: 0, tz: 0, qx: 0, qy: 0, qz: 0, qw: 1),
            RerunWireFormat.hitResultJson(timestampNanos: 1, id: 1, tx: 0, ty: 0, tz: 0, distance: 1),
            RerunWireFormat.pointCloudJson(timestampNanos: 1, positions: [0, 0, 0], confidences: [0]),
            RerunWireFormat.planeJson(timestampNanos: 1, id: 1, kind: "horizontal_upward", worldPolygon: []),
        ]
        for line in lines {
            XCTAssertEqual(line.filter { $0 == "\n" }.count, 1, "line must have exactly one \\n: \(line)")
            XCTAssertTrue(line.hasSuffix("\n"))
        }
    }
}
