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
        )
        // iOS snapshot tests are run via generate-ios-goldens.py + verify-ios-goldens.py
        // which use xcrun simctl screenshots + Python Pillow for pixel comparison.
        // See .claude/scripts/generate-ios-goldens.py
    ]
)
