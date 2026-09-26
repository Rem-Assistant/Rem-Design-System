import SwiftUI

/// A rounded, filled square holding an SF Symbol — the settings / hero icon primitive.
/// **Token-driven** via `ContainedIconTokenSet` (the same pattern as `RemButton`): this view is
/// thin and reads every value from the token set for `(fill, size)`. Figma canonical:
/// **ContainedIcon** `110:54` / variant set `614:8` (Fill × Size). SF Symbols render natively via
/// `Image(systemName:)`. Two fills:
/// - `.tint(color)` — solid color square + on-color (white) glyph (hero, colored settings icons).
/// - `.subtle` — translucent `fill/tertiary` square + `label/secondary` glyph (inline row leading).
public struct ContainedIcon: View {
    public enum Fill: Sendable {
        case tint(Color)
        case subtle
    }

    let symbol: String
    var fill: Fill
    var size: ContainedIconSize
    var glyphWeight: Font.Weight

    public init(
        _ symbol: String,
        fill: Fill = .subtle,
        size: ContainedIconSize = .small,
        glyphWeight: Font.Weight = .semibold
    ) {
        self.symbol = symbol
        self.fill = fill
        self.size = size
        self.glyphWeight = glyphWeight
    }

    public var body: some View {
        let tokens = ContainedIconTokenSet(fill: fill, size: size)
        Image(systemName: symbol)
            .font(.system(size: tokens.glyphPointSize, weight: glyphWeight))
            .foregroundStyle(tokens.foreground)
            .frame(width: tokens.dimension, height: tokens.dimension)
            .background(tokens.background, in: RoundedRectangle(cornerRadius: tokens.cornerRadius, style: .continuous))
    }
}

#if DEBUG
#Preview("ContainedIcon") {
    HStack(spacing: 16) {
        ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
        ContainedIcon("doc.text", fill: .subtle)
        ContainedIcon("shield", fill: .subtle)
    }
    .padding()
}
#endif
