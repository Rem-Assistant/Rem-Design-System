import SwiftUI

/// Copy payload for the shared Pre-consent/Provider composition (`1898:54528`). It has no Wallet,
/// connector, navigation, authentication, or persistence dependency. Providers retain their own copy.
public struct ProviderPreConsentPayload: Sendable {
    public let title: String
    public let purpose: String
    public let benefits: [ProviderPreConsentBenefit]
    public let disclosure: String
    public let connectTitle: String
    public let cancelTitle: String
    public init(title: String, purpose: String, benefits: [ProviderPreConsentBenefit], disclosure: String,
                connectTitle: String = "Connect", cancelTitle: String = "Cancel") {
        self.title = title; self.purpose = purpose; self.benefits = benefits; self.disclosure = disclosure
        self.connectTitle = connectTitle; self.cancelTitle = cancelTitle
    }
}

public struct ProviderPreConsentBenefit: Identifiable, Sendable {
    public let id: String
    public let title: String
    public let body: String
    public init(id: String, title: String, body: String) {
        self.id = id; self.title = title; self.body = body
    }
}

/// Shared provider consent content and native actions. The caller owns the native sheet, provider
/// mark, semantic benefit icons, exact provider copy, and callbacks. Link, Shop Pay and Notion can
/// use this same structure without sharing their purpose/disclosure or inventing an auth flow.
public struct ProviderPreConsentContent<ProviderMark: View, BenefitIcon: View>: View {
    private let payload: ProviderPreConsentPayload
    private let accessibilityIdentifier: String
    private let actionAccessibilityPrefix: String
    private let onConnect: () -> Void
    private let onCancel: () -> Void
    private let providerMark: () -> ProviderMark
    private let benefitIcon: (ProviderPreConsentBenefit) -> BenefitIcon

    public init(payload: ProviderPreConsentPayload, accessibilityIdentifier: String = "providerConsent",
                actionAccessibilityPrefix: String = "providerConsent",
                onConnect: @escaping () -> Void, onCancel: @escaping () -> Void,
                @ViewBuilder providerMark: @escaping () -> ProviderMark,
                @ViewBuilder benefitIcon: @escaping (ProviderPreConsentBenefit) -> BenefitIcon) {
        self.payload = payload; self.accessibilityIdentifier = accessibilityIdentifier
        self.actionAccessibilityPrefix = actionAccessibilityPrefix
        self.onConnect = onConnect; self.onCancel = onCancel
        self.providerMark = providerMark; self.benefitIcon = benefitIcon
    }
    public var body: some View {
        List {
            Section {
                VStack(spacing: DesignTokens.Spacing.md) {
                    providerMark()
                    Text(payload.title)
                        .font(DesignTokens.Typography.title1Bold)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text(payload.purpose)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, DesignTokens.Spacing.md)
                ForEach(payload.benefits) { benefit in
                    HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                        benefitIcon(benefit)
                            .font(.body)
                            .frame(width: 26, height: 26)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                            Text(benefit.title).font(DesignTokens.Typography.body.weight(.semibold))
                            Text(benefit.body)
                                .font(DesignTokens.Typography.subheadline)
                                .foregroundStyle(DesignTokens.Color.labelSecondary)
                        }
                        .fixedSize(horizontal: false, vertical: true)
                    }
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .accessibilityElement(children: .combine)
                }
                Text(payload.disclosure)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity)
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(DesignTokens.Color.backgroundPrimary)
        .accessibilityIdentifier(accessibilityIdentifier)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: DesignTokens.Spacing.sm) {
                Button(payload.connectTitle, action: onConnect)
                    .remButton(.rectBlue)
                    .accessibilityIdentifier("\(actionAccessibilityPrefix).connect")
                Button(payload.cancelTitle, action: onCancel)
                    .remSettingsCTA()
                    .accessibilityIdentifier("\(actionAccessibilityPrefix).cancel")
            }
            .padding(DesignTokens.Spacing.lg)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
            .background(DesignTokens.Color.backgroundPrimary)
        }
    }
}
