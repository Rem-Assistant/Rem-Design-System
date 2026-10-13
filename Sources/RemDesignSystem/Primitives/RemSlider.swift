import SwiftUI

/// **RemSlider** — the canonical slider: a thin wrapper over the **platform-native `Slider`**, tinted
/// to `brandBlue`. Per the design-system rule, native controls are wrapped (never hand-drawn), so a
/// screen never reconstructs a track + thumb by hand — it uses this. The Figma counterpart wraps the
/// forked **iOS 26 `Sliders`** / **Material 3 `Standard slider`** kit components (plain: no min/max
/// symbols, no ticks); this is the code twin. Compose sibling: `primitives/RemSlider.kt`.
public struct RemSlider: View {
    @Binding private var value: Double
    private let range: ClosedRange<Double>

    public init(value: Binding<Double>, in range: ClosedRange<Double> = 0...1) {
        self._value = value
        self.range = range
    }

    public var body: some View {
        Slider(value: $value, in: range)
            .tint(DesignTokens.Color.brandBlue)
    }
}

#Preview {
    struct Demo: View {
        @State private var speed = 0.45
        @State private var consistency = 0.7
        var body: some View {
            VStack(spacing: 20) {
                RemSlider(value: $speed)
                RemSlider(value: $consistency)
            }
            .padding(24)
            .background(DesignTokens.Color.backgroundPrimary)
        }
    }
    return Demo()
}
