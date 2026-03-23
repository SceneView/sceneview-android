// swift-tools-version: 5.10

import PackageDescription

let package = Package(
    name: "SceneViewSwift",
    platforms: [
        .iOS(.v17),
        .visionOS(.v1)
    ],
    products: [
        .library(
            name: "SceneViewSwift",
            targets: ["SceneViewSwift"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "SceneViewSwift",
            dependencies: [],
            path: "Sources/SceneViewSwift"
        ),
        .testTarget(
            name: "SceneViewSwiftTests",
            dependencies: ["SceneViewSwift"],
            path: "Tests/SceneViewSwiftTests"
        )
    ]
)
