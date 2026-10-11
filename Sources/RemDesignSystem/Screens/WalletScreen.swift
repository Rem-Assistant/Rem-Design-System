import SwiftUI

/// **WalletScreen** — a **payment-methods** screen (NOT a balance/credits ledger): reached from
/// Settings → Wallet, it lets the user securely save payment methods for the agent to use when making
/// purchases. A centered hero (neutral wallet tile + title + subtitle) over a grouped list of payment
/// providers, each a logo tile + name + trailing action (**Add** when linkable, **Coming soon** when
/// not). Chrome (status bar, nav bar) comes from the platform.
///
/// Authority: the reference Wallet screen (Settings → Wallet; "Securely save payment methods for
/// {agent} to use when making purchases for you"; Link by Stripe / Shop Pay). Compose sibling:
/// `screens/WalletScreen.kt`. Brand provider glyphs/colors are placeholders until real logo assets
/// land (logo debt).
public enum WalletProviderAction {
    /// The provider can be linked now.
    case add(() -> Void)
    /// The provider is announced but not yet available.
    case comingSoon

    var isComingSoon: Bool { if case .comingSoon = self { return true } else { return false } }
}

public struct PaymentProvider: Identifiable {
    public let id = UUID()
    public let name: String
    public let tileColor: Color
    public let symbol: String
    public let action: WalletProviderAction

    public init(name: String, tileColor: Color, symbol: String, action: WalletProviderAction) {
        self.name = name
        self.tileColor = tileColor
        self.symbol = symbol
        self.action = action
    }
}

public struct WalletScreen: View {
    private let providers: [PaymentProvider]
    private let agentName: String
    private let onBack: (() -> Void)?

    public init(providers: [PaymentProvider], agentName: String = "Rem", onBack: (() -> Void)? = nil) {
        self.providers = providers
        self.agentName = agentName
        self.onBack = onBack
    }

    public var body: some View {
        VStack(spacing: 0) {
            if let onBack {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .frame(width: 36, height: 36)
                            .background(DesignTokens.Color.backgroundSecondary, in: Circle())
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.md)
            }

            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    ContainedIcon("wallet.pass", fill: .subtle, size: .large)
                        .padding(.top, DesignTokens.Spacing.xl)

                    VStack(spacing: DesignTokens.Spacing.sm) {
                        Text("Wallet")
                            .font(DesignTokens.Typography.title1Bold)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                        Text("Securely save payment methods for \(agentName) to use when making purchases for you.")
                            .font(DesignTokens.Typography.subheadline)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .multilineTextAlignment(.center)
                    }

                    RemSection {
                        ForEach(providers) { provider in
                            ProviderRow(provider: provider)
                            if provider.id != providers.last?.id {
                                Divider().padding(.leading, DesignTokens.Spacing.xxl + DesignTokens.Spacing.md)
                            }
                        }
                    }
                    .padding(.top, DesignTokens.Spacing.md)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.xl)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

private struct ProviderRow: View {
    let provider: PaymentProvider

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous)
                .fill(provider.tileColor)
                .frame(width: 32, height: 32)
                .overlay(
                    Image(systemName: provider.symbol)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                )
            Text(provider.name)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(provider.action.isComingSoon
                                 ? DesignTokens.Color.labelSecondary
                                 : DesignTokens.Color.labelPrimary)
            Spacer()
            switch provider.action {
            case .add(let onAdd):
                Button("Add", action: onAdd)
                    .font(DesignTokens.Typography.body.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.brandBlue)
                    .buttonStyle(.plain)
            case .comingSoon:
                Text("Coming soon")
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelTertiary)
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, DesignTokens.Spacing.md)
    }
}

public extension WalletScreen {
    /// The two reference providers. Tile colors approximate the brands; glyphs are placeholders until
    /// real logo assets land (logo debt).
    static func referenceProviders(onAddStripe: @escaping () -> Void = {}) -> [PaymentProvider] {
        [
            PaymentProvider(name: "Link by Stripe",
                            tileColor: Color(red: 0, green: 0.84, blue: 0.44),
                            symbol: "link",
                            action: .add(onAddStripe)),
            PaymentProvider(name: "Shop Pay",
                            tileColor: Color(red: 0.35, green: 0.19, blue: 0.96),
                            symbol: "bag.fill",
                            action: .comingSoon),
        ]
    }
}

#if DEBUG
#Preview {
    WalletScreen(providers: WalletScreen.referenceProviders(), onBack: {})
}
#endif
