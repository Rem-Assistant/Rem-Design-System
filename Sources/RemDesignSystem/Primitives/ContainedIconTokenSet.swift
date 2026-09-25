import SwiftUI

/// The size axis of `ContainedIcon` — 1:1 with the Figma **Size** property on the
/// ContainedIcon variant set (`614:8`). Kept a public top-level enum (not nested in the
/// internal token set) so it can appear in `ContainedIcon`'s public API — the same shape as
/// `RemButtonSize`.
public enum ContainedIconSize: Sendable {
    case small   // 38pt — inline list-row leading
    case large   // 64pt — hero
}

/// Resolves every styleable value for a `ContainedIcon` `(fill, size)` into one value type —
/// the same **TokenSet** pattern as `RemButtonTokenSet`. Every value comes from `DesignTokens`
/// (one documented exception, below), so `ContainedIcon` itself stays thin and no literals live
/// in the view. Figma canonical: ContainedIcon variant set `614:8` (property **Fill**:
/// Tinted/Subtle × **Size**: Small/Large).
///
/// There is deliberately **no** `ContainedIconStyle`: unlike `RemButton`, a `ContainedIcon` is a
/// decorative `View`, not a control, so it has no rest/pressed/disabled states and no SwiftUI
/// `ButtonStyle` hook. The design-system parallel for a plain view is *TokenSet + View*; the
/// parallel for a control is *TokenSet + Style*.
struct ContainedIconTokenSet {
    let dimension: CGFloat
    let cornerRadius: CGFloat
    let glyphPointSize: CGFloat
    let background: Color
    let foreground: Color

    init(fill: ContainedIcon.Fill, size: ContainedIconSize) {
        switch size {
        case .small:
            dimension = 38
            cornerRadius = DesignTokens.CornerRadius.small          // 8
        case .large:
            dimension = 64
            // 18 is the 64pt hero squircle radius the founder approved. It is intentionally OFF
            // the 8/12/16/24 scale — flagged for token reconciliation; centralized here so it
            // lives in exactly one place rather than at call sites.
            cornerRadius = 18
        }
        glyphPointSize = dimension * 0.46

        switch fill {
        case .tint(let color):
            background = color
            foreground = DesignTokens.Color.labelOnColor
        case .subtle:
            background = DesignTokens.Color.fillTertiary
            foreground = DesignTokens.Color.labelSecondary
        }
    }
}
