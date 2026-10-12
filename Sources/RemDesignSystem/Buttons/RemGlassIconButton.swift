import SwiftUI

/// **RemGlassIconButton** — a round, icon-only button in system glass: the floating chrome control of iOS 26 (a top
/// bar's menu or back-to-chat button, jump to latest). Compose twin: `buttons/RemGlassIconButton.kt`, which
/// reproduces the look on Android with a backdrop blur (Haze) over a `remGlassSource`.
///
/// - iOS / macOS 26 (built with Xcode 26): the system Liquid Glass, `.glassEffect(.regular.interactive(), in: Circle())`.
/// - Earlier: an `.ultraThinMaterial` circle with a 0.5pt `separator` hairline and a soft shadow, so the edge stays
///   visible on matching backgrounds.
///
/// Both follow Reduce Transparency and Increase Contrast through the system material. The glyph is `labelPrimary`
/// (`labelTertiary` when disabled with `.disabled(true)`). The touch target is at least 44pt at every `size`; a
/// smaller circle stays centred in it. Fixed icon-only chrome: iOS offers the Large Content Viewer at large text.
/// Contract: `components/GlassIconButton.md`.
public struct RemGlassIconButton: View {
    /// Default circle diameter: the 44pt minimum touch target.
    public static let defaultSize: CGFloat = 44
    /// Touch target floor at every size.
    public static let minimumTouchTarget: CGFloat = 44

    private enum Glyph {
        case symbol(String)
        case image(Image)
    }

    private let glyph: Glyph
    private let accessibilityLabel: String
    private let size: CGFloat
    private let action: () -> Void
    @Environment(\.isEnabled) private var isEnabled

    /// A glass button showing the SF Symbol `systemName`.
    public init(systemName: String, accessibilityLabel: String, size: CGFloat = RemGlassIconButton.defaultSize,
                action: @escaping () -> Void) {
        self.glyph = .symbol(systemName)
        self.accessibilityLabel = accessibilityLabel
        self.size = size
        self.action = action
    }

    /// A glass button showing `image` (a product's own asset), fitted to the glyph box and tinted as a template.
    public init(image: Image, accessibilityLabel: String, size: CGFloat = RemGlassIconButton.defaultSize,
                action: @escaping () -> Void) {
        self.glyph = .image(image)
        self.accessibilityLabel = accessibilityLabel
        self.size = size
        self.action = action
    }

    /// Glyph size for a circle of `size`: 17pt in the default 44 (as jump to latest), in proportion otherwise.
    static func glyphSize(for size: CGFloat) -> CGFloat { size * 17 / 44 }

    public var body: some View {
        let target = max(size, Self.minimumTouchTarget)
        Button(action: action) {
            glyphView
                .foregroundStyle(isEnabled ? DesignTokens.Color.labelPrimary : DesignTokens.Color.labelTertiary)
                .frame(width: size, height: size)
                .modifier(RemGlassCircle())
                .frame(width: target, height: target)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    #if os(iOS)
        .accessibilityShowsLargeContentViewer()
    #endif
    }

    @ViewBuilder private var glyphView: some View {
        let glyphSize = Self.glyphSize(for: size)
        switch glyph {
        case .symbol(let name):
            Image(systemName: name)
                .font(.system(size: glyphSize, weight: .semibold))
        case .image(let image):
            image
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: glyphSize, height: glyphSize)
        }
    }
}

/// The glass circle: system glass where the SDK and OS have it, otherwise `.ultraThinMaterial` with a hairline and
/// a soft shadow. The SDK check keeps the package building with pre-26 Xcode (CI runs macOS 14 runners).
private struct RemGlassCircle: ViewModifier {
    func body(content: Content) -> some View {
    #if compiler(>=6.2)
        if #available(iOS 26.0, macOS 26.0, *) {
            content.glassEffect(.regular.interactive(), in: Circle())
        } else {
            fallback(content)
        }
    #else
        fallback(content)
    #endif
    }

    private func fallback(_ content: Content) -> some View {
        content
            .background(.ultraThinMaterial, in: Circle())
            .overlay(Circle().strokeBorder(DesignTokens.Color.separator, lineWidth: 0.5))
            .shadow(color: .black.opacity(0.12), radius: 8, x: 0, y: 2)
    }
}

#Preview {
    HStack(spacing: 24) {
        RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}
        RemGlassIconButton(systemName: "chevron.right.2", accessibilityLabel: "Back to chat") {}
        RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}.disabled(true)
        RemGlassIconButton(systemName: "chevron.right.2", accessibilityLabel: "Back to chat") {}
            .environment(\.colorScheme, .dark)
    }
    .padding(32)
    .background(LinearGradient(colors: [DesignTokens.Color.systemBlue, DesignTokens.Color.systemOrange], startPoint: .leading, endPoint: .trailing))
}
