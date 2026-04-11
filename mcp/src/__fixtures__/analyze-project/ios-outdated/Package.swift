// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "DemoApp",
    platforms: [.iOS(.v17)],
    dependencies: [
        .package(url: "https://github.com/sceneview/sceneview", from: "3.0.0")
    ],
    targets: [
        .executableTarget(
            name: "DemoApp",
            dependencies: [
                .product(name: "SceneViewSwift", package: "sceneview")
            ]
        )
    ]
)
