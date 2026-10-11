import SwiftUI

/// Settings Billing & Usage `1827:50803`, reached from Settings → Billing & Usage. Current plan,
/// two usage meters and the Rect · Blue "Upgrade to Pro" RemButton. Plan and usage are illustrative
/// fixture values (screen-level hint). There is no billing account or purchase flow, so Upgrade
/// states that boundary; no payment or plan change is simulated.
/// Compose sibling: `screens/SettingsBillingScreen.kt`.
public struct SettingsBillingScreen: View {
    @State private var boundary: String?
    public init() {}

    public var body: some View {
        List {
            Section {
                ListRow(layout: .nativeList, leading: { EmptyView() }, content: {
                    ListRowLabel(SettingsBillingFixture.planTitle)
                }, trailing: {
                    Text(SettingsBillingFixture.plan)
                        .font(.callout)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                })
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("billing.plan")
            } header: {
                HStack { Text(SettingsBillingFixture.planHeader).textCase(nil) }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                ForEach(SettingsBillingFixture.usage) { meter in
                    BillingUsageMeterRow(meter: meter)
                }
            } header: {
                HStack { Text(SettingsBillingFixture.usageHeader).textCase(nil) }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                Button(SettingsBillingFixture.upgradeTitle) {
                    boundary = SettingsBillingFixture.upgradeBoundary
                }
                .remButton(.rectBlue)
                .accessibilityIdentifier("billing.upgrade")
                .accessibilityHint("Explains what this prototype includes")
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
        }
        .settingsDestinationList()
        .navigationTitle(SettingsBillingFixture.title)
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsBilling")
        .accessibilityHint(PlaygroundMockData.hint)
        .settingsPrototypeBoundary($boundary)
    }
}

/// Usage meter `1827:50788`: title, footnote count and a 4pt brand-blue progress track.
private struct BillingUsageMeterRow: View {
    let meter: SettingsUsageMeter
    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                Text(meter.title)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Text(meter.label)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    // Source: 8pt gap, 10pt spacer, 8pt gap between title and count.
                    .padding(.leading, DesignTokens.Spacing.sm + 10)
                Spacer(minLength: 0)
            }
            ProgressView(value: meter.fraction)
                .progressViewStyle(.linear)
                .tint(DesignTokens.Color.brandBlue)
                .accessibilityHidden(true)
        }
        .padding(.vertical, DesignTokens.Spacing.xs)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(meter.title)
        .accessibilityValue(meter.label)
        .accessibilityIdentifier("billing.usage.\(meter.id)")
    }
}

#if DEBUG
#Preview("Billing & Usage") {
    NavigationStack { SettingsBillingScreen() }
}
#endif
