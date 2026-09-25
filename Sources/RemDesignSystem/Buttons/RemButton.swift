import SwiftUI

// RemButton — the design system's Button, unified as the registry's Type × Tier model.
//
// REGISTRY.md canonical: "Button — Type · Tier model (founder-directed, no gradient)".
// Figma node: Primitives `377:8`.
//
// This consolidates three shipping ButtonStyles into ONE `Shape × Tier` style,
// reproducing each one's exact metrics/tokens so there is zero visual drift from
// what ships today:
//   • .rectangular + .black  ← RemPrimaryActionButtonStyle (login / consent CTA)
//   • .text       + .accent  ← RemSettingsCTAButtonStyle(role: .primary)
//   • .text       + .destructive ← RemSettingsCTAButtonStyle(role: .destructive)
//   • .pill       + .secondary   ← RemRowConnectCTA (Connect / Install)
// `.rectangular + .blue` is the model's documented Rect·Blue variant (brand-blue
// fill); Rect·Secondary / Rect·Destructive intentionally use SwiftUI's built-in
// `.bordered` / `.borderedProminent` per the registry, so they are not re-drawn here.

public struct RemButtonStyle: ButtonStyle {
    public enum Shape: Sendable { case rectangular, pill, text }
    public enum Tier: Sendable { case black, blue, accent, destructive }
    public enum Size: Sendable { case regular, compact }

    public var shape: Shape
    public var tier: Tier
    public var size: Size

    public init(shape: Shape, tier: Tier, size: Size = .regular) {
        self.shape = shape
        self.tier = tier
        self.size = size
    }

    @Environment(\.isEnabled) private var isEnabled

    public func makeBody(configuration: Configuration) -> some View {
        switch shape {
        case .rectangular: rectangular(configuration)
        case .text:        text(configuration)
        case .pill:        pill(configuration)
        }
    }

    // MARK: - Rectangular (filled) — from RemPrimaryActionButtonStyle
    @ViewBuilder
    private func rectangular(_ configuration: Configuration) -> some View {
        let radius = DesignTokens.CornerRadius.medium
        configuration.label
            .font(DesignTokens.Typography.bodyBold)
            .foregroundStyle(rectangularLabelColor)
            .frame(maxWidth: .infinity)
            .padding(DesignTokens.Spacing.md)
            .background(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .fill(rectangularFill)
            )
            .opacity(configuration.isPressed ? 0.72 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
            .contentShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }

    private var rectangularFill: Color {
        guard isEnabled else { return DesignTokens.Color.labelTertiary.opacity(0.35) }
        switch tier {
        case .blue: return DesignTokens.Color.brandBlue
        default:    return DesignTokens.Color.buttonBackground // .black (adaptive ink)
        }
    }

    // .black uses the adaptive on-fill token (systemBackground) so it inverts in
    // dark mode; .blue is a fixed fill so it needs a fixed white label.
    private var rectangularLabelColor: Color {
        tier == .blue ? .white : DesignTokens.Color.backgroundPrimary
    }

    // MARK: - Text (tinted label only) — from RemSettingsCTAButtonStyle
    @ViewBuilder
    private func text(_ configuration: Configuration) -> some View {
        let vPad: CGFloat = size == .regular ? 6 : 4
        configuration.label
            .font(size == .regular ? .body.weight(.semibold) : .subheadline.weight(.semibold))
            .foregroundStyle(textLabelColor)
            .opacity(configuration.isPressed ? 0.55 : 1)
            .padding(.vertical, vPad)
            .padding(.horizontal, 2)
            .frame(minWidth: 0, maxWidth: size == .regular ? .infinity : nil, alignment: .center)
            .contentShape(Rectangle())
    }

    private var textLabelColor: Color {
        guard isEnabled else { return .secondary }
        return tier == .destructive ? .red : .accentColor
    }

    // MARK: - Pill (translucent capsule) — from RemRowConnectCTA
    @ViewBuilder
    private func pill(_ configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(DesignTokens.Color.brandBlueOnFill)
            .padding(.horizontal, 16)
            .padding(.vertical, 5)
            .background(Capsule().fill(DesignTokens.Color.fillTertiary))
            .opacity(configuration.isPressed ? 0.7 : 1)
            .contentShape(Capsule())
    }
}

// MARK: - Named presets (the 7 real usages from REGISTRY.md)

public extension RemButtonStyle {
    /// Rect · Black — full-width filled primary CTA (login, consent, onboarding).
    static var primary: RemButtonStyle { .init(shape: .rectangular, tier: .black) }
    /// Rect · Blue — brand-blue filled variant.
    static var primaryBlue: RemButtonStyle { .init(shape: .rectangular, tier: .blue) }
    /// Text · Accent — settings/list CTA (tinted label, no fill).
    static func settingsCTA(_ size: Size = .regular) -> RemButtonStyle {
        .init(shape: .text, tier: .accent, size: size)
    }
    /// Text · Destructive — settings/list destructive CTA.
    static func settingsDestructive(_ size: Size = .regular) -> RemButtonStyle {
        .init(shape: .text, tier: .destructive, size: size)
    }
    /// Pill · Secondary — row-trailing "Connect" / "Install" capsule.
    /// (The pill shape renders a fixed translucent style, so `tier` is unused here.)
    static var connect: RemButtonStyle { .init(shape: .pill, tier: .accent) }
}

// MARK: - View conveniences (mirror the app's current call sites for easy cutover)

public extension View {
    func remButton(_ style: RemButtonStyle) -> some View { buttonStyle(style) }
    /// Drop-in for the app's `remPrimaryActionButton()`.
    func remPrimaryActionButton() -> some View { buttonStyle(RemButtonStyle.primary) }
    /// Drop-in for the app's `remSettingsCTA(_:size:)`.
    func remSettingsCTA(destructive: Bool = false, compact: Bool = false) -> some View {
        let size: RemButtonStyle.Size = compact ? .compact : .regular
        return buttonStyle(destructive ? RemButtonStyle.settingsDestructive(size)
                                        : RemButtonStyle.settingsCTA(size))
    }
    /// Drop-in for the app's `RemRowConnectCTA` pill.
    func remConnectButton() -> some View { buttonStyle(RemButtonStyle.connect) }
}

#if DEBUG
#Preview("RemButton — all tiers") {
    VStack(spacing: 16) {
        Button("Continue") {}.remPrimaryActionButton()
        Button("Continue (blue)") {}.remButton(.primaryBlue)
        Button("Approve This Device") {}.remSettingsCTA()
        Button("Remove Gateway") {}.remSettingsCTA(destructive: true)
        Button("Connect") {}.remConnectButton()
        Button("Disabled") {}.remPrimaryActionButton().disabled(true)
    }
    .padding()
}
#endif
