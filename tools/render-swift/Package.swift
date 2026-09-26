// swift-tools-version: 5.9
import PackageDescription

// Screenshot-evidence renderer for the SwiftUI design system. A tiny macOS executable that
// depends on the shippable RemDesignSystem package (path dependency) and renders each component
// to a PNG via SwiftUI `ImageRenderer` — no simulator, no running app. Kept OUT of the shippable
// package so the library target stays clean (see ../../Package.swift).
let package = Package(
    name: "RenderGallery",
    platforms: [.macOS(.v14)],
    dependencies: [
        .package(path: "../..")
    ],
    targets: [
        .executableTarget(
            name: "RenderGallery",
            dependencies: [
                .product(name: "RemDesignSystem", package: "RemDesignSystem")
            ]
        )
    ]
)
