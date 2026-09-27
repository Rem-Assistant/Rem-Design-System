import SwiftUI

// MARK: - Google Provider Glyph

/// The multicolor **Google "G"** provider mark for the sign-in treatment. This is the real vendor
/// asset (Google's four-color G), transferred verbatim from the shipping app
/// (`rem-assistant/remclaw` `Rem/Assets.xcassets/google-icon.imageset`) into this package's asset
/// catalog with its vector representation preserved.
///
/// It renders at its own colors (never tinted) — the "Continue with Google" button places it on the
/// filled `buttonBackground`, exactly as `SignInButton` does in the app. Kept as a dedicated glyph so
/// the sign-in template composes a real provider mark instead of the earlier empty slot (the Google
/// icon "not transferring" gap).
public struct RemGoogleGlyph: View {
    /// Rendered edge length (square). Sign-in uses 18pt, matching `providerButtons`.
    var size: CGFloat

    public init(size: CGFloat = 18) {
        self.size = size
    }

    public var body: some View {
        Image("RemGoogleIcon", bundle: .module)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

#if DEBUG
#Preview("Google glyph") {
    RemGoogleGlyph(size: 32).padding(40)
}
#endif
