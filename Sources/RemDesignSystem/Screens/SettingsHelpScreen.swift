import SwiftUI

/// Settings Help & Support `2014:68798`, reached from Settings → Help & Support. Send Feedback and
/// Report a Bug stay separate destinations; Shake to report is a local switch. The source marks the
/// forms, shake detection and submission as unsettled implementation decisions, so both rows state
/// that boundary and the footer says shake detection is not included. Leading icons use the approved
/// Settings Subtle amendment rather than the source's blue tint.
/// Compose sibling: `screens/SettingsHelpScreen.kt`.
public struct SettingsHelpScreen: View {
    @State private var fixture = SettingsHelpFixture()
    @State private var boundary: String?
    public init() {}

    public var body: some View {
        List {
            Section {
                ForEach(SettingsHelpDestination.allCases) { destination in
                    Button { boundary = destination.boundary } label: {
                        ListRow(layout: .nativeList, leading: {
                            ContainedIcon(destination.symbol, fill: .subtle, size: .settings, glyphWeight: .regular)
                                .accessibilityHidden(true)
                        }, content: {
                            ListRowLabel(destination.title)
                        }, trailing: { DisclosureChevron() })
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("help.\(destination.rawValue)")
                    .accessibilityHint("Explains what this prototype includes")
                }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                Toggle(isOn: $fixture.shakeToReport) {
                    ListRowLabel(SettingsHelpFixture.shakeTitle)
                }
                .tint(DesignTokens.Color.systemGreen)
                .accessibilityIdentifier("help.shakeToReport")
            } footer: {
                Text(SettingsHelpFixture.shakeFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle("Help & Support")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsHelp")
        .settingsPrototypeBoundary($boundary)
    }
}

#if DEBUG
#Preview("Help & Support") {
    NavigationStack { SettingsHelpScreen() }
}
#endif
