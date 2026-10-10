import SwiftUI

/// Settings Automations `1833:5080`, reached from Agent settings → Automations. A native grouped
/// List with the "Built in" section and its footer. The Daily Brief detail (schedule, instructions,
/// inputs, outputs) needs an automation runner the design system does not have, so the row states
/// that boundary instead of inventing behavior. Compose sibling: `screens/SettingsAutomationsScreen.kt`.
public struct SettingsAutomationsScreen: View {
    @State private var boundary: String?
    public init() {}

    public var body: some View {
        List {
            Section {
                ForEach(SettingsAutomationsFixture.builtIn) { automation in
                    Button { boundary = SettingsAutomationsFixture.boundary(for: automation) } label: {
                        HStack(spacing: DesignTokens.Spacing.md) {
                            ListRowLabel(automation.title, subtitle: automation.subtitle)
                            Spacer(minLength: DesignTokens.Spacing.sm)
                            DisclosureChevron()
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("automations.\(automation.id)")
                    .accessibilityHint("Explains what this prototype includes")
                }
            } header: {
                HStack { Text(SettingsAutomationsFixture.builtInHeader).textCase(nil) }
            } footer: {
                Text(SettingsAutomationsFixture.builtInFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle("Automations")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsAutomations")
        .accessibilityHint(PlaygroundMockData.hint)
        .settingsPrototypeBoundary($boundary)
    }
}

#if DEBUG
#Preview("Automations") {
    NavigationStack { SettingsAutomationsScreen() }
}
#endif
