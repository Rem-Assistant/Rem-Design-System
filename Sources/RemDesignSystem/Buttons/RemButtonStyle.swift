import SwiftUI

/// The design system's Button, as a SwiftUI `ButtonStyle` — the Fluent
/// `FluentButtonStyle` pattern: **thin**. It builds a `RemButtonTokenSet` for the
/// `(variant, size)`, selects rest/pressed/disabled, and renders the container.
/// No values live here — they live in the token set. Figma: Button `377:8`, property
/// `Style` ↔ `RemButtonVariant`.
public struct RemButtonStyle: SwiftUI.ButtonStyle {
    public var variant: RemButtonVariant
    public var size: RemButtonSize

    public init(_ variant: RemButtonVariant, size: RemButtonSize = .regular) {
        self.variant = variant
        self.size = size
    }

    @Environment(\.isEnabled) private var isEnabled

    public func makeBody(configuration: Configuration) -> some View {
        let tokens = RemButtonTokenSet(variant: variant, size: size)
        let foreground = isEnabled ? tokens.foreground : tokens.foregroundDisabled
        let background = isEnabled ? tokens.background : tokens.backgroundDisabled

        return configuration.label
            .font(tokens.font)
            .foregroundStyle(foreground)
            .frame(maxWidth: tokens.fillsWidth ? .infinity : nil)
            .padding(.horizontal, tokens.horizontalPadding)
            .padding(.vertical, tokens.verticalPadding)
            .background { fill(tokens, color: background) }
            .opacity(configuration.isPressed ? tokens.pressedOpacity : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
            .contentShape(shape(tokens))
    }

    @ViewBuilder
    private func fill(_ tokens: RemButtonTokenSet, color: Color) -> some View {
        switch tokens.container {
        case .roundedRect:
            RoundedRectangle(cornerRadius: tokens.cornerRadius, style: .continuous).fill(color)
        case .capsule:
            Capsule().fill(color)
        case .plain:
            Color.clear
        }
    }

    private func shape(_ tokens: RemButtonTokenSet) -> AnyShape {
        switch tokens.container {
        case .roundedRect: return AnyShape(RoundedRectangle(cornerRadius: tokens.cornerRadius, style: .continuous))
        case .capsule:     return AnyShape(Capsule())
        case .plain:       return AnyShape(Rectangle())
        }
    }
}

// MARK: - Presets (the 7 registry / Figma variants, by intent name)

public extension RemButtonStyle {
    static var primary: RemButtonStyle { .init(.rectBlack) }        // Rect · Black
    static var primaryBlue: RemButtonStyle { .init(.rectBlue) }     // Rect · Blue
    static var secondary: RemButtonStyle { .init(.rectSecondary) }  // Rect · Secondary
    static var destructive: RemButtonStyle { .init(.rectDestructive) } // Rect · Destructive
    static func settingsCTA(_ size: RemButtonSize = .regular) -> RemButtonStyle { .init(.textAccent, size: size) }
    static func settingsDestructive(_ size: RemButtonSize = .regular) -> RemButtonStyle { .init(.textDestructive, size: size) }
    static var connect: RemButtonStyle { .init(.pillSecondary) }    // Pill · Secondary
}

// MARK: - View conveniences (mirror the app's current call sites for cutover)

public extension View {
    func remButton(_ variant: RemButtonVariant, size: RemButtonSize = .regular) -> some View {
        buttonStyle(RemButtonStyle(variant, size: size))
    }
    func remButton(_ style: RemButtonStyle) -> some View { buttonStyle(style) }
    func remPrimaryActionButton() -> some View { buttonStyle(RemButtonStyle.primary) }
    func remSettingsCTA(destructive: Bool = false, compact: Bool = false) -> some View {
        let size: RemButtonSize = compact ? .compact : .regular
        return buttonStyle(destructive ? RemButtonStyle.settingsDestructive(size)
                                        : RemButtonStyle.settingsCTA(size))
    }
    func remConnectButton() -> some View { buttonStyle(RemButtonStyle.connect) }
}

#if DEBUG
#Preview("RemButton — all 7 variants") {
    VStack(spacing: 14) {
        ForEach(RemButtonVariant.allCases, id: \.self) { variant in
            Button(variant.figmaStyleName) {}
                .remButton(variant)
        }
        Button("Disabled") {}.remButton(.rectBlack).disabled(true)
    }
    .padding()
}
#endif
