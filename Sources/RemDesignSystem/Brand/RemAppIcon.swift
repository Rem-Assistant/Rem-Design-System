import SwiftUI

// MARK: - Rem App Icon

/// The **Rem app-icon mark** — the brand-blue squircle bloom the shipping app shows in its sign-in
/// lockup, About screen, and Live Activities. This is the *app icon*, not the `RemFaceMark`: the
/// sign-in brand lockup uses the icon (blue tile + white bloom), while the face mark (blue outline
/// bloom + eyes + smile) is the chat/thinking identity. They are different marks — see the founder
/// reference `tasks/refs/onboarding/01-sign-in.png`.
///
/// Source of truth: the shipping raster `AppIcon` (`rem-assistant/remclaw`
/// `Rem/Assets.xcassets/AppIcon.appiconset/Logo.png`), transferred verbatim into this package's
/// asset catalog. The bloom has no vector source in the app — it ships as a raster — so the design
/// system carries the real asset rather than a re-drawn approximation that would drift from it.
///
/// Treatment reproduced from `OnboardingLogoView` (`Rem/Sources/Onboarding/OnboardingFlow.swift`):
/// `scaledToFit` in a square frame, clipped to the `small` corner radius. Defaults match the sign-in
/// lockup (40pt).
public struct RemAppIcon: View {
    /// Rendered edge length (square).
    var size: CGFloat
    /// Corner radius of the squircle. Defaults to the app's sign-in treatment (`CornerRadius.small`).
    var cornerRadius: CGFloat

    public init(size: CGFloat = 40, cornerRadius: CGFloat = DesignTokens.CornerRadius.small) {
        self.size = size
        self.cornerRadius = cornerRadius
    }

    public var body: some View {
        // `Image(_:bundle:)` resolves the imageset from the package's compiled asset catalog
        // (`Resources/Media.xcassets`) on both iOS and macOS — no platform image type needed.
        Image("RemAppIcon", bundle: .module)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .accessibilityHidden(true)
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Rem App Icon") {
    HStack(spacing: 24) {
        RemAppIcon(size: 40)
        RemAppIcon(size: 56)
        RemAppIcon(size: 88, cornerRadius: DesignTokens.CornerRadius.large)
    }
    .padding(40)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
