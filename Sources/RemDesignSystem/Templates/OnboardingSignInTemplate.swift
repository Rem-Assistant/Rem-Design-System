import SwiftUI

/// Presentational template for the onboarding **sign-in** screen — the brand lockup + provider CTAs.
/// Pure: no auth, no Keychain, no `GoogleSignIn` — the app wraps it, maps its real auth flow to a
/// ``State``, and supplies the action callbacks. The template only renders the state it is handed
/// (the boundary that keeps sign-in "wired to real auth, no mock").
///
/// Reproduced from the shipping `signInContent` / `OnboardingLogoView` / `SignInButton`
/// (`Rem/Sources/Onboarding/OnboardingFlow.swift`, `Rem/Sources/Components/SignInButton.swift`) and
/// the founder reference frame `tasks/refs/onboarding/01-sign-in.png`: a left-aligned lockup
/// (``RemAppIcon`` → "Rem" → "Turn your thoughts into actions") centered vertically, with the provider
/// buttons directly beneath it. The mark is the **app icon**, not ``RemFaceMark`` — the sign-in lockup
/// has always used the app icon (verified against the reference frame).
///
/// The Compose sibling is `OnboardingSignInScreen` (`compose/RemDesignSystem/onboarding/SignInStep.kt`);
/// the two render the same states — one block centered in the safe area, never bottom-pinned — so the
/// side-by-side evidence compares the same screen. Built to `docs/contracts/onboarding-sign-in.md`.
public struct OnboardingSignInTemplate: View {
    /// The sign-in state, driven by the host's real auth. Mirrors the Compose `SignInState`.
    public enum State {
        /// A credential is already present (Sign-in-with-Apple returning). Primary = "Continue as <name>".
        case returning(accountName: String)
        /// No credential yet. Offers Apple + Google provider buttons.
        case new
        /// Auth in flight after a provider tap. Primary shows the disabled spinner ("Signing in…").
        case checking
        /// Auth failed. Shows the inline error notice + "Try again" and the different-account escape.
        case error(message: String)
        /// Account-recovery path (credential revoked / needs re-auth). Primary = "Try again" (re-auth);
        /// "Sign in with a different account" stays the quiet escape (emphasis rule — never filled).
        case recovery(message: String)
    }

    var state: State
    var title: String
    var tagline: String
    var onPrimary: () -> Void
    var onGoogle: () -> Void
    var onUseDifferentAccount: () -> Void
    var onRetry: () -> Void
    var legalFootnote: String

    public init(
        state: State,
        title: String = "Rem",
        tagline: String = "Turn your thoughts\ninto actions",
        onPrimary: @escaping () -> Void,
        onGoogle: @escaping () -> Void = {},
        onUseDifferentAccount: @escaping () -> Void = {},
        onRetry: @escaping () -> Void = {},
        legalFootnote: String = "By continuing, you agree to our Terms of Service and Privacy Policy."
    ) {
        self.state = state
        self.title = title
        self.tagline = tagline
        self.onPrimary = onPrimary
        self.onGoogle = onGoogle
        self.onUseDifferentAccount = onUseDifferentAccount
        self.onRetry = onRetry
        self.legalFootnote = legalFootnote
    }

    public var body: some View {
        VStack(spacing: 0) {
            Spacer()
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                RemAppIcon(size: 40)

                VStack(alignment: .leading, spacing: 0) {
                    Text(title)
                        .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text(tagline)
                        .font(DesignTokens.Typography.title1)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, DesignTokens.Spacing.xs)
                }

                actions

                if case .error(let message) = state { notice(message) }
                if case .recovery(let message) = state { notice(message) }
            }
            .padding(DesignTokens.Spacing.md)
            .frame(maxWidth: 560)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    }

    // MARK: - Provider / continue actions (per state)

    @ViewBuilder private var actions: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            switch state {
            case .returning(let name):
                signInButton(title: "Continue as \(name)", action: onPrimary, icon: appleMark)
                differentAccountLink

            case .new:
                signInButton(title: "Continue with Google", action: onGoogle, icon: AnyView(RemGoogleGlyph(size: 18)))
                signInButton(title: "Continue with Apple", action: onPrimary, icon: appleMark)
                legalDisclosure

            case .checking:
                signInButton(
                    title: "Signing in…",
                    action: {},
                    enabled: false,
                    icon: AnyView(ProgressView().tint(DesignTokens.Color.backgroundPrimary))
                )

            case .error:
                signInButton(title: "Try again", action: onRetry)
                differentAccountLink

            case .recovery:
                // Recovery is a re-auth path, so "Try again" is the filled primary action — like the
                // error state. "Sign in with a different account" stays the quiet labelSecondary link
                // (emphasis rule: it is *never* filled or emphasized, in any state).
                signInButton(title: "Try again", action: onRetry)
                differentAccountLink
            }
        }
    }

    /// The filled provider button — reproduces `SignInButton`: `buttonBackground` fill, `medium`
    /// corner radius, an inverted `bodyBold` label, and an optional 18pt leading glyph (untinted, so
    /// the multicolor Google mark reads at its own colors). No glyph → a centered label.
    private func signInButton(
        title: String,
        action: @escaping () -> Void,
        enabled: Bool = true,
        icon: AnyView? = nil
    ) -> some View {
        Button(action: action) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                if let icon { icon.frame(width: 18, height: 18) }
                Text(title)
                    .font(DesignTokens.Typography.bodyBold)
                    .foregroundStyle(DesignTokens.Color.backgroundPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(DesignTokens.Spacing.md)
            .background(DesignTokens.Color.buttonBackground)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }

    private var appleMark: AnyView {
        AnyView(
            Image(systemName: "apple.logo")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(DesignTokens.Color.backgroundPrimary)
        )
    }

    private var differentAccountLink: some View {
        // `.buttonStyle(.plain)` pins this to a quiet `labelSecondary` link in every state — without it
        // the automatic button style would tint the label with the ambient accent color, which is the
        // exact "different account gets emphasized on one platform" drift #27 exists to kill. Matches
        // the Compose `DifferentAccountLink` (a plain, untinted `labelSecondary` text link).
        Button("Sign in with a different account", action: onUseDifferentAccount)
            .buttonStyle(.plain)
            .font(DesignTokens.Typography.body)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(maxWidth: .infinity)
    }

    private var legalDisclosure: some View {
        Text(legalFootnote)
            .font(DesignTokens.Typography.caption1)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
    }

    /// Inline error/recovery notice — mirrors the Compose `SignInNotice` (systemRed at 12% on a
    /// rounded surface) so the paired evidence reads as the same treatment on both platforms. The
    /// leading glyph resolves through the icon registry by meaning + FILL: `exclamationmark.triangle.fill`
    /// (`error`, FILL 1) — the Android side renders the Material Symbols `error` glyph at FILL 1.
    private func notice(_ message: String) -> some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 14))
                .foregroundStyle(DesignTokens.Color.systemRed)
            Text(message)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(DesignTokens.Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                .fill(DesignTokens.Color.systemRed.opacity(0.12))
        )
    }
}

#if DEBUG
#Preview("Sign-in · returning") {
    OnboardingSignInTemplate(state: .returning(accountName: "Sam"), onPrimary: {})
}
#Preview("Sign-in · new") {
    OnboardingSignInTemplate(state: .new, onPrimary: {})
}
#Preview("Sign-in · checking") {
    OnboardingSignInTemplate(state: .checking, onPrimary: {})
}
#Preview("Sign-in · error") {
    OnboardingSignInTemplate(
        state: .error(message: "We couldn't sign you in. Check your connection and try again."),
        onPrimary: {}
    )
    .preferredColorScheme(.dark)
}
#Preview("Sign-in · recovery") {
    OnboardingSignInTemplate(
        state: .recovery(message: "Your session expired. Sign in again to pick up where you left off."),
        onPrimary: {}
    )
}
#endif
