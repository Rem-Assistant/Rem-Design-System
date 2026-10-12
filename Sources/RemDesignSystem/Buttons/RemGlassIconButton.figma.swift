import Figma
import SwiftUI

// Code Connect for RemGlassIconButton — PENDING A FIGMA MASTER. The component is code-led (May adoption,
// 2026-10-12): there is no Rem Figma node for a glass icon button yet, so `figmaNodeUrl` carries the
// scaffold's `node-id=TODO` placeholder and NOTHING may publish this mapping until the node exists. When the
// master lands (Button family, a `Glyph` instance-swap + `State` Enabled/Disabled + `Size`), replace the node
// id, add parserless twins under `code-connect/swiftui/` and `code-connect/compose/`, and update REGISTRY.md.
//
// Archived native mapping (the scaffold shape, `tools/scaffold-component.mjs`): EXCLUDED from the
// RemDesignSystem SPM target (see Package.swift) and not read by the parserless Code Connect configs.
struct RemGlassIconButton_connection: FigmaConnect {
    let component = RemGlassIconButton.self
    let figmaNodeUrl = "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=TODO"

    var body: some View {
        RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}
    }
}
