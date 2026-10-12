import SwiftUI

/// Presentational template for the onboarding **"Connectors"** step (Connect your apps).
/// This is the **onboarding treatment**, NOT the Settings connectors list: it mirrors the shape of
/// `OnboardingConsentTemplate` (hero lockup → grouped card on a white flip background → bottom CTA), so
/// it stays consistent with the other onboarding steps. Figma authority: `tasks/refs/onboarding/03-connectors.png`.
/// Pure: the app supplies connector data + actions and owns navigation/coach-mark.
///
/// Rows use the onboarding controls (a **toggle** for a connected app, a **Connect** pill for an
/// available one) — not the Settings chevron rows. Reuses `ContainedIcon`, `ListRow`, `RemSection`,
/// `RemButton`.
public struct OnboardingConnectorsTemplate: View {
    public struct Connector: Identifiable {
        public let id = UUID()
        public let symbol: String
        public let tint: Color
        public let name: String
        public let status: String
        public let isConnected: Bool
        public let action: () -> Void
        public init(symbol: String, tint: Color, name: String, status: String, isConnected: Bool, action: @escaping () -> Void) {
            self.symbol = symbol; self.tint = tint; self.name = name; self.status = status
            self.isConnected = isConnected; self.action = action
        }
    }

    var heroSymbol: String
    var title: String
    var message: String
    var connectors: [Connector]
    var showSeeMore: Bool
    var onSeeMore: () -> Void
    var onContinue: () -> Void
    var onSkip: () -> Void

    public init(
        heroSymbol: String = "link",
        title: String = "Connectors",
        message: String = "Connect Rem to the tools you use so it can keep you up to date and surface what needs doing.",
        connectors: [Connector],
        showSeeMore: Bool = true,
        onSeeMore: @escaping () -> Void = {},
        onContinue: @escaping () -> Void,
        onSkip: @escaping () -> Void
    ) {
        self.heroSymbol = heroSymbol; self.title = title; self.message = message
        self.connectors = connectors; self.showSeeMore = showSeeMore
        self.onSeeMore = onSeeMore; self.onContinue = onContinue; self.onSkip = onSkip
    }

    public var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    Spacer(minLength: DesignTokens.Spacing.xxl)
                    hero
                    card
                }
                .padding(DesignTokens.Spacing.lg)
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

    private var card: some View {
        RemSection {
            VStack(spacing: 0) {
                ForEach(connectors) { c in
                    ListRow(
                        c.name,
                        subtitle: c.status,
                        leading: { ContainedIcon(c.symbol, fill: .tint(c.tint)) },
                        trailing: { control(for: c) }
                    )
                    if c.id != connectors.last?.id || showSeeMore {
                        Divider().overlay(DesignTokens.Color.separator).padding(.leading, 60)
                    }
                }
                if showSeeMore {
                    Button(action: onSeeMore) {
                        HStack(spacing: DesignTokens.Spacing.sm) {
                            Image(systemName: "chevron.down").font(.system(size: 13, weight: .semibold))
                            Text("See more")
                            Spacer()
                        }
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                        .padding(.horizontal, DesignTokens.Spacing.md)
                        .padding(.vertical, DesignTokens.Spacing.sm)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder private func control(for c: Connector) -> some View {
        if c.isConnected {
            Toggle(isOn: .constant(true)) { EmptyView() }
                .labelsHidden()
                .tint(DesignTokens.Color.systemGreen)
        } else {
            Button("Connect", action: c.action)
                .remButton(.pillSecondary)
        }
    }

    private var bottomBar: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            Button("Continue", action: onContinue)
                .remPrimaryActionButton()
            Button("Skip", action: onSkip)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.brandBlue)
                .buttonStyle(.plain)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
        .padding(.bottom, DesignTokens.Spacing.md)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

#if DEBUG
#Preview("OnboardingConnectorsTemplate") {
    OnboardingConnectorsTemplate(
        connectors: [
            .init(symbol: "envelope.fill", tint: .red, name: "Gmail", status: "Connected", isConnected: true, action: {}),
            .init(symbol: "calendar", tint: .blue, name: "Google Calendar", status: "Not connected", isConnected: false, action: {}),
            .init(symbol: "number", tint: .purple, name: "Slack", status: "Not connected", isConnected: false, action: {}),
        ],
        onContinue: {}, onSkip: {}
    )
}
#endif
