// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "SceneViewDemo",
    platforms: [.iOS(.v18), .macOS(.v15)],
    dependencies: [
        .package(name: "SceneViewSwift", path: "../../SceneViewSwift")
    ],
    targets: [
        .executableTarget(
            name: "SceneViewDemo",
            dependencies: [
                .product(name: "SceneViewSwift", package: "SceneViewSwift")
            ],
            path: "SceneViewDemo"
        ),
        .testTarget(
            name: "SceneViewDemoTests",
            dependencies: ["SceneViewDemo"],
            path: "SceneViewDemoTests"
        )
    ]
)
