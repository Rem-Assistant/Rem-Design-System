import SwiftUI

/// Presentational template for the onboarding **"Privacy by design"** consent screen.
/// Pure: no auth, no store, no sheets — the app wraps it and supplies actions + sheet presentation.
/// Mirrors the shipping `AIDataSharingConsentView` (Rem/Sources/Onboarding). Figma: `Screen/Privacy`
/// (`410:16`). Composes design-system components: `ContainedIcon` (hero + row leading), `ListRow`
/// (legal rows), `RemButton` (primary CTA). iOS-canonical; renders adaptively on iPadOS/macOS.
public struct OnboardingConsentTemplate: View {
    public enum State {
        case idle
        case loading
        /// A failed consent submission that can be retried through the original accept action.
        /// Terminal, authorization, and recovery failures require distinct states.
        case retryableError(message: String)
    }

    /// A tappable legal/disclosure row (Terms, Privacy, …).
    public struct LegalItem: Identifiable {
        public let id = UUID()
        public let symbol: String
        public let title: String
        public let subtitle: String
        public let action: () -> Void
        public init(symbol: String, title: String, subtitle: String, action: @escaping () -> Void) {
            self.symbol = symbol; self.title = title; self.subtitle = subtitle; self.action = action
        }
    }

    var heroSymbol: String
    var title: String
    var message: String
    var legalItems: [LegalItem]
    var footnote: String
    var state: State
    var onPrimary: () -> Void

    public init(
        heroSymbol: String = "lock.shield.fill",
        title: String = "Privacy by design",
        message: String,
        legalItems: [LegalItem],
        footnote: String,
        state: State = .idle,
        onPrimary: @escaping () -> Void
    ) {
        self.heroSymbol = heroSymbol
        self.title = title
        self.message = message
        self.legalItems = legalItems
        self.footnote = footnote
        self.state = state
        self.onPrimary = onPrimary
    }

    public var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    Spacer(minLength: DesignTokens.Spacing.xxl)
                    hero
                    legalCard
                }
                .padding(DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.xl)
                .frame(maxWidth: 560)
            }
            bottomBar
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    }

    private var hero: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ContainedIcon(heroSymbol, fill: .tint(DesignTokens.Color.brandBlue), size: .large)
            VStack(spacing: DesignTokens.Spacing.sm) {
                Text(title)
                    .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var legalCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(legalItems.enumerated()), id: \.element.id) { index, item in
                if index > 0 {
                    Divider().padding(.leading, 60)
                }
                ListRow(
                    item.title,
                    subtitle: item.subtitle,
                    action: item.action,
                    leading: { ContainedIcon(item.symbol, fill: .subtle) },
                    trailing: { DisclosureChevron() }  // Button-based sheet opener (not a List/NavigationLink)
                )
            }
        }
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                .stroke(DesignTokens.Color.separator.opacity(0.35), lineWidth: 1)
        }
    }

    /// Bottom-pinned region — error notice (when present) directly ABOVE the CTA, the primary CTA, then
    /// the legal footnote BELOW it. Matches `docs/contracts/onboarding-consent.md` and the Compose
    /// scaffold's bottom bar so the paired render diffs clean.
    private var bottomBar: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            if case let .retryableError(message) = state { notice(message) }
            Button(action: onPrimary) {
                if case .loading = state {
                    ProgressView().tint(DesignTokens.Color.backgroundPrimary)
                } else {
                    Text(primaryTitle)
                }
            }
            .remPrimaryActionButton()
            .disabled(isLoading)
            .opacity(isLoading ? 0.4 : 1)
            Text(footnote)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
        .padding(.bottom, DesignTokens.Spacing.md)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    private var primaryTitle: String {
        if case .retryableError = state { return "Try again" }
        return "Accept and Continue"
    }

    private var isLoading: Bool {
        if case .loading = state { return true }
        return false
    }

    /// Inline error notice — mirrors the Compose `OnboardingNotice` (systemRed at 12% on a
    /// `medium`-radius surface). Leading glyph resolves through the icon registry by meaning + FILL:
    /// `exclamationmark.triangle.fill` (`error`, FILL 1).
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
#Preview("OnboardingConsentTemplate") {
    OnboardingConsentTemplate(
        message: "Rem uses your data to answer you and act on the things you ask. You can review or delete it anytime in Settings.",
        legalItems: [
            .init(symbol: "doc.text", title: "Terms of Service",
                  subtitle: "How Rem accounts, subscriptions, and approved actions work.", action: {}),
            .init(symbol: "shield", title: "Privacy Policy",
                  subtitle: "What Rem, your gateway, and AI or voice providers process.", action: {}),
        ],
        footnote: "By tapping \u{201C}Accept and Continue,\u{201D} you agree to our Terms of Service and Privacy Policy.",
        onPrimary: {}
    )
}
#endif
