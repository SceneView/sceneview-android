import XCTest
import SnapshotTesting
import SwiftUI
@testable import SceneViewDemoLib

/// Screenshot tests (swift-snapshot-testing) — s'exécutent sur simulateur iOS.
///
/// Générer les goldens :  swift test --filter ScreenshotTests (première exécution avec record: true)
/// Vérifier les goldens : swift test --filter ScreenshotTests
///
/// Les goldens sont dans : SceneViewDemoTests/__Snapshots__/
final class ScreenshotTests: XCTestCase {

    // Mettre à true uniquement pour régénérer les goldens
    private let record = false

    // ── About ─────────────────────────────────────────────────────────────────

    func test_aboutTab_iPhone_light() {
        let view = NavigationStack { AboutTab() }
            .preferredColorScheme(.light)
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPhone13Pro), traits: .init(userInterfaceStyle: .light)),
            record: record
        )
    }

    func test_aboutTab_iPhone_dark() {
        let view = NavigationStack { AboutTab() }
            .preferredColorScheme(.dark)
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPhone13Pro), traits: .init(userInterfaceStyle: .dark)),
            record: record
        )
    }

    func test_aboutTab_iPad() {
        let view = NavigationStack { AboutTab() }
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPadPro12_9), traits: .init(userInterfaceStyle: .light)),
            record: record
        )
    }

    func test_aboutTab_largeFont() {
        let view = NavigationStack { AboutTab() }
        assertSnapshot(
            of: view,
            as: .image(
                layout: .device(config: .iPhone13Pro),
                traits: .init(preferredContentSizeCategory: .accessibilityExtraExtraLarge)
            ),
            record: record
        )
    }

    // ── Explore ───────────────────────────────────────────────────────────────

    func test_exploreTab_iPhone_light() {
        let view = NavigationStack { ExploreTab() }
            .preferredColorScheme(.light)
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPhone13Pro), traits: .init(userInterfaceStyle: .light)),
            record: record
        )
    }

    func test_exploreTab_iPhone_dark() {
        let view = NavigationStack { ExploreTab() }
            .preferredColorScheme(.dark)
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPhone13Pro), traits: .init(userInterfaceStyle: .dark)),
            record: record
        )
    }

    // ── Samples ───────────────────────────────────────────────────────────────

    func test_samplesTab_iPhone_light() {
        let view = NavigationStack { SamplesTab() }
            .preferredColorScheme(.light)
        assertSnapshot(
            of: view,
            as: .image(layout: .device(config: .iPhone13Pro), traits: .init(userInterfaceStyle: .light)),
            record: record
        )
    }
}
