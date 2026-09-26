// swift-tools-version: 5.9
import PackageDescription

// RemDesignSystem — shippable Swift package for the Rem design system.
//
// Layout (idiomatic SPM): Sources/RemDesignSystem/
//   • DesignTokens.generated.swift  — the token spine, emitted by
//     tools/generate-tokens.mjs from tokens.json (DO NOT EDIT by hand).
//   • Buttons/, etc.                — canonical components extracted from the
//     shipping app per REGISTRY.md, token-driven, with state variants.
let package = Package(
    name: "RemDesignSystem",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "RemDesignSystem", targets: ["RemDesignSystem"]),
    ],
    targets: [
        .target(
            name: "RemDesignSystem",
            // Code Connect files are co-located with their components but excluded from the
            // build so the shipping library never links the Figma Code Connect package.
            // The `figma connect` CLI reads them directly; `figma connect check` validates drift.
            exclude: [
                "Buttons/RemButton.figma.swift",
                "Primitives/ContainedIcon.figma.swift",
            ]
        ),
        // Screenshot-evidence snapshots. Run on an iOS Simulator via `xcodebuild test` so the
        // render uses real iOS UIColor semantics (`.systemBackground` is white on iOS, grey on
        // macOS) and captures ScrollView/List content that `ImageRenderer` cannot. The file is
        // UIKit-guarded, so `swift test` on macOS compiles it to an empty target. Writes PNGs to
        // SNAPSHOT_OUT_DIR. NOT part of the library product — consumers never build it.
        .testTarget(
            name: "RenderSnapshotTests",
            dependencies: ["RemDesignSystem"],
            path: "tools/render-swift/Tests/RenderSnapshotTests"
        ),
    ]
)
