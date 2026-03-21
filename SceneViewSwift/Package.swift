// swift-tools-version: 6.0

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
    // TODO: Add GLTFKit2 dependency for glTF/GLB support alongside USDZ
    // .package(url: "https://github.com/magicien/GLTFKit2.git", from: "0.3.0")
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
