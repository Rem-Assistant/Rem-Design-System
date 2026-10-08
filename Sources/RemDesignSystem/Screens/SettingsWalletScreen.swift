import SwiftUI

/// Settings Wallet `1833:51817`, provider pre-consent `1898:54528` (original Link `1898:54429`
/// and Shop Pay `1898:54474` copy), and blank external boundaries `1926:53854/53931`.
/// Native List/Section/sheet geometry is the approved platform adaptation. Provider marks are exact
/// exported assets. The utility wallet.pass hero deliberately uses the approved Subtle amendment
/// (labelPrimary/backgroundSecondary), overriding the final Figma blue utility hero; provider brands
/// keep their source colors. No external URL opens,
/// authentication, payment, storage, or successful connection is performed by this prototype.
public struct SettingsWalletScreen: View {
    @State private var fixture = SettingsWalletFixture()
    public init() {}

    public var body: some View {
        List {
            Section {
                VStack(spacing: DesignTokens.Spacing.md) {
                    ContainedIcon("wallet.pass", fill: .subtle, size: .large)
                    Text("Wallet")
                        .font(DesignTokens.Typography.title1Bold)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text(SettingsWalletFixture.body)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, DesignTokens.Spacing.lg)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }
            Section {
                ForEach(SettingsWalletProvider.allCases) { provider in
                    Button { fixture.open(provider) } label: {
                        HStack(spacing: DesignTokens.Spacing.md) {
                            WalletProviderMark(provider: provider, isHero: false)
                            ListRowLabel(provider.title)
                            Spacer(minLength: DesignTokens.Spacing.sm)
                            DisclosureChevron()
                        }
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("wallet.provider.\(provider.rawValue)")
                    .accessibilityHint("Review connection access")
                    .listRowBackground(DesignTokens.Color.backgroundSecondary)
                }
            }
        }
        .walletListStyle()
        .navigationTitle("Wallet")
        .walletInlineTitle()
        .accessibilityIdentifier("settingsWallet")
        .accessibilityHint(PlaygroundMockData.hint)
        .sheet(isPresented: Binding(
            get: { fixture.stage != .root },
            set: { if !$0 { fixture.dismiss() } }
        ), onDismiss: { fixture.dismiss() }) {
            if let provider = fixture.provider {
                Group {
                    if fixture.stage == .external {
                        WalletExternalBoundary(provider: provider, onClose: { fixture.dismiss() })
                    } else {
                        WalletProviderConsent(provider: provider,
                                              onConnect: { fixture.connect() },
                                              onCancel: { fixture.dismiss() })
                    }
                }
                .walletSheetStyle()
            }
        }
    }
}

private struct WalletProviderMark: View {
    let provider: SettingsWalletProvider
    let isHero: Bool
    private var asset: String {
        provider == .link ? "SettingsWalletLink" : (isHero ? "SettingsWalletShopPayConsent" : "SettingsWalletShopPayRow")
    }
    var body: some View {
        Image(asset, bundle: .module)
            .resizable()
            .scaledToFit()
            .frame(width: isHero ? 60 : 29, height: isHero ? 60 : 29)
            .accessibilityHidden(true)
    }
}

private struct WalletProviderConsent: View {
    let provider: SettingsWalletProvider
    let onConnect: () -> Void
    let onCancel: () -> Void
    var body: some View {
        ProviderPreConsentContent(
            payload: ProviderPreConsentPayload(
                title: provider.title, purpose: provider.consentBody,
                benefits: provider.benefits.map { .init(id: $0.id, title: $0.title, body: $0.body) },
                disclosure: provider.disclosure
            ),
            accessibilityIdentifier: "wallet.consent.\(provider.rawValue)",
            actionAccessibilityPrefix: "wallet.consent", onConnect: onConnect, onCancel: onCancel,
            providerMark: { WalletProviderMark(provider: provider, isHero: true) },
            benefitIcon: { benefit in
                if let source = provider.benefits.first(where: { $0.id == benefit.id }) {
                    Image(systemName: source.symbol)
                }
            }
        )
    }
}

/// The source owns no provider web content. Retain a blank surface and a working native Close;
/// the domain is a presentation label, never a URL loaded by a WebView or external browser.
private struct WalletExternalBoundary: View {
    let provider: SettingsWalletProvider
    let onClose: () -> Void
    var body: some View {
        NavigationStack {
            DesignTokens.Color.backgroundPrimary
                .ignoresSafeArea()
                .navigationTitle(provider.domain)
                .walletInlineTitle()
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close", action: onClose)
                            .accessibilityIdentifier("wallet.external.close")
                    }
                }
                .accessibilityIdentifier("wallet.external.\(provider.rawValue)")
                .accessibilityLabel("External provider boundary")
                .accessibilityHint("Prototype only. No provider page is loaded.")
        }
    }
}

private extension View {
    @ViewBuilder func walletListStyle() -> some View {
        #if os(iOS)
        self.listStyle(.insetGrouped).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
        #else
        self.listStyle(.inset).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
        #endif
    }
    @ViewBuilder func walletInlineTitle() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
    @ViewBuilder func walletSheetStyle() -> some View {
        #if os(iOS)
        self.presentationDetents([.large]).presentationDragIndicator(.visible)
        #else
        self
        #endif
    }
}
