import SwiftUI

/// A rounded, filled square holding an SF Symbol — the settings / hero icon primitive.
/// Figma canonical: **ContainedIcon** (`110:54`). Two fills:
/// - `.tint(color)` — solid color square + on-color (white) glyph (hero, colored settings icons).
/// - `.subtle` — translucent `fill/tertiary` square + `label/secondary` glyph (inline list-row leading).
/// All colors come from `DesignTokens`; SF Symbols render natively via `Image(systemName:)`.
public struct ContainedIcon: View {
    public enum Fill: Sendable {
        case tint(Color)
        case subtle
    }

    let symbol: String
    var fill: Fill
    var size: CGFloat
    var cornerRadius: CGFloat
    var glyphWeight: Font.Weight

    public init(
        _ symbol: String,
        fill: Fill = .subtle,
        size: CGFloat = 38,
        cornerRadius: CGFloat = DesignTokens.CornerRadius.small,
        glyphWeight: Font.Weight = .semibold
    ) {
        self.symbol = symbol
        self.fill = fill
        self.size = size
        self.cornerRadius = cornerRadius
        self.glyphWeight = glyphWeight
    }

    public var body: some View {
        Image(systemName: symbol)
            .font(.system(size: size * 0.46, weight: glyphWeight))
            .foregroundStyle(foreground)
            .frame(width: size, height: size)
            .background(background, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var background: Color {
        switch fill {
        case .tint(let color): return color
        case .subtle: return DesignTokens.Color.fillTertiary
        }
    }

    private var foreground: Color {
        switch fill {
        case .tint: return DesignTokens.Color.labelOnColor
        case .subtle: return DesignTokens.Color.labelSecondary
        }
    }
}

#if DEBUG
#Preview("ContainedIcon") {
    HStack(spacing: 16) {
        ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: 64, cornerRadius: 18)
        ContainedIcon("doc.text", fill: .subtle, size: 38)
        ContainedIcon("shield", fill: .subtle, size: 38)
    }
    .padding()
}
#endif
