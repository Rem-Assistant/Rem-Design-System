import Figma
import SwiftUI

// Code Connect for ContainedIcon — binds the Figma ContainedIcon variant set `614:8`
// (properties `Fill` × `Size`, plus the swapped `Icon`) to the real Swift types
// (`ContainedIcon`, `ContainedIcon.Fill`, `ContainedIconSize`). Co-located with the component so a
// rename here surfaces drift immediately, and `figma connect check` validates it against the node.
//
// EXCLUDED from the RemDesignSystem SPM target (see Package.swift), so the shipping library never
// links `github.com/figma/code-connect`. The `figma connect` CLI reads it directly. Publishing is
// plan-gated (Org/Enterprise + Dev/Full seat), so this stays dormant-but-ready — same as RemButton.
struct ContainedIcon_connection: FigmaConnect {
    let component = ContainedIcon.self
    let figmaNodeUrl =
        "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=614-8"

    @FigmaEnum("Size", mapping: [
        "Small": ContainedIconSize.small,
        "Large": ContainedIconSize.large,
    ])
    var size: ContainedIconSize = .small

    // Figma "Tinted" defaults to the brand blue fill; "Subtle" is the translucent list-row style.
    @FigmaEnum("Fill", mapping: [
        "Tinted": ContainedIcon.Fill.tint(DesignTokens.Color.brandBlue),
        "Subtle": ContainedIcon.Fill.subtle,
    ])
    var fill: ContainedIcon.Fill = .subtle

    // The swapped SF Symbol (instance-swap / text property on the Figma component).
    @FigmaString("Icon")
    var symbol: String = "gearshape.fill"

    var body: some View {
        ContainedIcon(symbol, fill: fill, size: size)
    }
}
