// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "SceneViewDemo",
    platforms: [.iOS(.v17)],
    dependencies: [
        .package(url: "https://github.com/SceneView/sceneview.git", from: "3.3.0")
    ],
    targets: [
        .executableTarget(
            name: "SceneViewDemo",
            dependencies: [
                .product(name: "SceneViewSwift", package: "sceneview")
            ],
            path: "SceneViewDemo"
        )
    ]
)
