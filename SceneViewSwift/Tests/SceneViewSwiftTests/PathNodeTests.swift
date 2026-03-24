import XCTest
@testable import SceneViewSwift

#if os(iOS) || os(macOS) || os(visionOS)
import RealityKit

final class PathNodeTests: XCTestCase {

    // MARK: - Basic creation

    func testPathWithTwoPointsCreatesOneSegment() {
        let path = PathNode(
            points: [.zero, .init(x: 1, y: 0, z: 0)]
        )
        XCTAssertNotNil(path.entity)
        // One line segment = one child entity
        XCTAssertEqual(path.entity.children.count, 1)
    }

    func testPathWithThreePointsCreatesTwoSegments() {
        let path = PathNode(
            points: [
                .zero,
                .init(x: 1, y: 0, z: 0),
                .init(x: 1, y: 1, z: 0)
            ]
        )
        XCTAssertEqual(path.entity.children.count, 2)
    }

    func testClosedPathAddsExtraSegment() {
        let points: [SIMD3<Float>] = [
            .zero,
            .init(x: 1, y: 0, z: 0),
            .init(x: 0.5, y: 1, z: 0)
        ]
        let open = PathNode(points: points, closed: false)
        let closed = PathNode(points: points, closed: true)

        // Closed should have one more segment than open
        XCTAssertEqual(open.entity.children.count, 2)
        XCTAssertEqual(closed.entity.children.count, 3)
    }

    func testPathStoresPoints() {
        let points: [SIMD3<Float>] = [
            .init(x: 0, y: 0, z: 0),
            .init(x: 1, y: 0, z: 0),
            .init(x: 1, y: 1, z: 0)
        ]
        let path = PathNode(points: points)
        XCTAssertEqual(path.points.count, 3)
        XCTAssertEqual(path.points[0].x, 0.0, accuracy: 0.001)
        XCTAssertEqual(path.points[1].x, 1.0, accuracy: 0.001)
    }

    // MARK: - Edge cases

    func testSinglePointCreatesNoSegments() {
        let path = PathNode(points: [.zero])
        XCTAssertEqual(path.entity.children.count, 0)
    }

    func testEmptyPointsCreatesNoSegments() {
        let path = PathNode(points: [])
        XCTAssertEqual(path.entity.children.count, 0)
    }

    // MARK: - Circle factory

    func testCircleCreatesClosedPath() {
        let circle = PathNode.circle(radius: 0.5, segments: 16)
        // 16 segments + 1 closing segment = 17 child entities
        XCTAssertEqual(circle.entity.children.count, 17)
    }

    func testCirclePointCount() {
        let circle = PathNode.circle(segments: 8)
        XCTAssertEqual(circle.points.count, 8)
    }

    func testCirclePointsLieOnXZPlane() {
        let center = SIMD3<Float>(0, 1, 0)
        let circle = PathNode.circle(center: center, radius: 1.0, segments: 4)

        for point in circle.points {
            // All points should be at y=1 (the center's y)
            XCTAssertEqual(point.y, 1.0, accuracy: 0.001)
            // Distance from center should be the radius
            let dx = point.x - center.x
            let dz = point.z - center.z
            let dist = sqrt(dx * dx + dz * dz)
            XCTAssertEqual(dist, 1.0, accuracy: 0.01)
        }
    }

    // MARK: - Grid factory

    func testGridCreatesLines() {
        let grid = PathNode.grid(size: 2.0, divisions: 4)
        // 4 divisions => 5 lines in each direction => 10 total child entities
        XCTAssertEqual(grid.entity.children.count, 10)
    }

    // MARK: - Transform

    func testPositionSetsEntityPosition() {
        let path = PathNode(
            points: [.zero, .init(x: 1, y: 0, z: 0)]
        ).position(.init(x: 0, y: 1, z: 0))

        XCTAssertEqual(path.entity.position.y, 1.0, accuracy: 0.001)
    }
}
#endif
