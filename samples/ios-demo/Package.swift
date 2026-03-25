// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "SceneViewDemo",
    platforms: [.iOS(.v17)],
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
        )
    ]
)
