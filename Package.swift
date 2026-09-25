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
        .target(name: "RemDesignSystem"),
    ]
)
