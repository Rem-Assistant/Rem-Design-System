import Figma
import SwiftUI

// Code Connect for RemButton — binds Figma Button `377:8` (variant property `Style`)
// to `RemButtonStyle`. Co-located with the component so the mapping references the REAL
// Swift types (`RemButtonStyle`, `RemButtonVariant`) — renaming a case here shows the
// drift immediately, and `figma connect check` in CI validates it against the node.
//
// This file is EXCLUDED from the RemDesignSystem SPM target (see Package.swift), so the
// shipping library never links `github.com/figma/code-connect`. The `figma connect` CLI
// reads it directly. Publish with `figma connect publish` (needs a Figma token).
struct RemButton_connection: FigmaConnect {
    let component = RemButtonStyle.self
    let figmaNodeUrl =
        "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=377-8"

    @FigmaEnum("Style", mapping: [
        "Rect · Black":       RemButtonVariant.rectBlack,
        "Rect · Blue":        RemButtonVariant.rectBlue,
        "Rect · Secondary":   RemButtonVariant.rectSecondary,
        "Rect · Destructive": RemButtonVariant.rectDestructive,
        "Text · Accent":      RemButtonVariant.textAccent,
        "Text · Destructive": RemButtonVariant.textDestructive,
        "Pill · Secondary":   RemButtonVariant.pillSecondary,
    ])
    var variant: RemButtonVariant = .rectBlack

    var body: some View {
        Button("Label") {}
            .remButton(variant)
    }
}
