// swift-tools-version: 5.9
import PackageDescription

// RemDesignSystem — shippable Swift package for the Rem design system.
// Step 1 (scaffold): exposes the generated token spine (DesignTokens) as an
// importable library. Components get extracted into Sources/RemDesignSystem/
// in later steps, per REGISTRY.md. The token file stays where the generator
// (tools/generate-tokens.mjs) writes it, so the existing pipeline/CI is untouched.
let package = Package(
    name: "RemDesignSystem",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "RemDesignSystem", targets: ["RemDesignSystem"]),
    ],
    targets: [
        .target(
            name: "RemDesignSystem",
            path: "tokens/generated",
            sources: ["DesignTokens.generated.swift"]
        ),
    ]
)
