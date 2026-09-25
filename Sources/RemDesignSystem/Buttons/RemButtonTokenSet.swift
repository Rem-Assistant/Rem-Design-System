import SwiftUI

/// The flat set of button variants — 1:1 with the Figma `Style` property on the
/// Button component (node `377:8`). Keeping this flat (rather than a Shape × Tier
/// cross-product) reconciles **code ↔ Figma ↔ Fluent**: Figma's component uses a
/// single `Style` property, and Fluent's `ButtonStyle` is likewise a flat enum. One
/// property ↔ one enum ⇒ Code Connect binds cleanly and `generate-library` has an
/// unambiguous target.
public enum RemButtonVariant: String, CaseIterable, Sendable {
    case rectBlack        // filled, adaptive ink   — login / consent CTA
    case rectBlue         // filled, brand blue
    case rectSecondary    // subtle filled          — system `.bordered` intent
    case rectDestructive  // filled red             — system `.borderedProminent .red`
    case textAccent       // tinted label only, accent
    case textDestructive  // tinted label only, red
    case pillSecondary    // translucent capsule    — Connect / Install

    /// The Figma `Style` value this maps to (Code Connect + generate parity).
    public var figmaStyleName: String {
        switch self {
        case .rectBlack:       return "Rect · Black"
        case .rectBlue:        return "Rect · Blue"
        case .rectSecondary:   return "Rect · Secondary"
        case .rectDestructive: return "Rect · Destructive"
        case .textAccent:      return "Text · Accent"
        case .textDestructive: return "Text · Destructive"
        case .pillSecondary:   return "Pill · Secondary"
        }
    }
}

public enum RemButtonSize: Sendable { case regular, compact }

/// Resolves every styleable value for a `(variant, size)` into one value type — the
/// Fluent **`ButtonTokenSet`** pattern. Every value comes from `DesignTokens`; the
/// SwiftUI `RemButtonStyle` only selects rest/pressed/disabled from here. This is the
/// layer that makes per-state/per-variant values *tokens*, not literals scattered
/// through `makeBody`. Metrics reproduce the shipping styles exactly (zero drift):
/// `RemPrimaryActionButtonStyle`, `RemSettingsCTAButtonStyle`, `RemRowConnectCTA`.
struct RemButtonTokenSet {
    enum Container { case roundedRect, capsule, plain }

    let container: Container
    let cornerRadius: CGFloat
    let horizontalPadding: CGFloat
    let verticalPadding: CGFloat
    let fillsWidth: Bool
    let font: Font

    let background: Color
    let backgroundDisabled: Color
    let foreground: Color
    let foregroundDisabled: Color
    let pressedOpacity: Double

    init(variant: RemButtonVariant, size: RemButtonSize) {
        switch variant {
        case .rectBlack, .rectBlue, .rectSecondary, .rectDestructive:
            container = .roundedRect
            cornerRadius = DesignTokens.CornerRadius.medium
            horizontalPadding = DesignTokens.Spacing.md
            verticalPadding = DesignTokens.Spacing.md
            fillsWidth = true
            font = DesignTokens.Typography.bodyBold
            backgroundDisabled = DesignTokens.Color.labelTertiary.opacity(0.35)
            foregroundDisabled = DesignTokens.Color.backgroundPrimary
            pressedOpacity = 0.72
            switch variant {
            case .rectBlue:
                background = DesignTokens.Color.brandBlue
                foreground = .white
            case .rectSecondary:
                background = DesignTokens.Color.fillTertiary
                foreground = DesignTokens.Color.labelPrimary
            case .rectDestructive:
                background = DesignTokens.Color.systemRed
                foreground = .white
            default: // rectBlack
                background = DesignTokens.Color.buttonBackground
                foreground = DesignTokens.Color.backgroundPrimary
            }

        case .textAccent, .textDestructive:
            container = .plain
            cornerRadius = 0
            horizontalPadding = 2
            verticalPadding = size == .regular ? 6 : 4
            fillsWidth = size == .regular
            font = size == .regular ? .body.weight(.semibold) : .subheadline.weight(.semibold)
            background = .clear
            backgroundDisabled = .clear
            foreground = variant == .textDestructive ? .red : .accentColor
            foregroundDisabled = .secondary
            pressedOpacity = 0.55

        case .pillSecondary:
            container = .capsule
            cornerRadius = 0
            horizontalPadding = 16
            verticalPadding = 5
            fillsWidth = false
            font = .subheadline.weight(.semibold)
            background = DesignTokens.Color.fillTertiary
            backgroundDisabled = DesignTokens.Color.fillTertiary
            foreground = DesignTokens.Color.brandBlueOnFill
            foregroundDisabled = .secondary
            pressedOpacity = 0.7
        }
    }
}
