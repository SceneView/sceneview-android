// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "SceneViewDemo",
    platforms: [.iOS(.v18), .macOS(.v15)],
    dependencies: [
        .package(name: "SceneViewSwift", path: "../../SceneViewSwift"),
        .package(
            url: "https://github.com/pointfreeco/swift-snapshot-testing",
            from: "1.17.0"
        )
    ],
    targets: [
        .executableTarget(
            name: "SceneViewDemo",
            dependencies: [
                .product(name: "SceneViewSwift", package: "SceneViewSwift")
            ],
            path: "SceneViewDemo"
        ),
        // NOTE: Snapshot tests are in SceneViewDemoTests/ but require an Xcode test target,
        // not a SPM test target, because the main target is an executable.
        // To generate iOS goldens: add SceneViewDemoTests as an XCTest target in the Xcode project,
        // set record = true in ScreenshotTests.swift, run tests, then set record = false.
    ]
)
