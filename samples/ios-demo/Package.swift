// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "SceneViewDemo",
    platforms: [.iOS(.v18)],
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
