import SwiftUI

/// Settings Permissions `1827:50821`, reached from Settings → Permissions. Three grouped sections of
/// Subtle-icon rows with a status badge and disclosure, plus the source footers. Device permissions
/// are owned by the operating system and the design system has no permission bridge or open-Settings
/// pattern, so statuses are illustrative and each row explains that boundary rather than prompting
/// or leaving the app. Compose sibling: `screens/SettingsPermissionsScreen.kt`.
public struct SettingsPermissionsScreen: View {
    @State private var boundary: String?
    public init() {}

    public var body: some View {
        List {
            ForEach(SettingsPermissionsFixture.sections) { section in
                Section {
                    ForEach(section.permissions) { permission in
                        Button { boundary = SettingsPermissionsFixture.boundary(for: permission) } label: {
                            ListRow(layout: .nativeList, leading: {
                                ContainedIcon(permission.symbol, fill: .subtle, size: .settings, glyphWeight: .regular)
                                    .accessibilityHidden(true)
                            }, content: {
                                ListRowLabel(permission.title)
                            }, trailing: {
                                HStack(spacing: 6) {
                                    PermissionStatusBadge(status: permission.status)
                                    DisclosureChevron()
                                }
                            })
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("permissions.\(permission.id)")
                        .accessibilityValue(permission.status.title)
                        .accessibilityHint("Explains what this prototype includes")
                    }
                } header: {
                    if let header = section.header { HStack { Text(header).textCase(nil) } }
                } footer: {
                    Text(section.footer)
                }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            }
        }
        .settingsDestinationList()
        .navigationTitle("Permissions")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsPermissions")
        .accessibilityHint(PlaygroundMockData.hint)
        .settingsPrototypeBoundary($boundary)
    }
}

/// PermissionStatusBadge `383:2`: an 8pt status dot and the secondary status label.
private struct PermissionStatusBadge: View {
    let status: SettingsPermissionStatus
    private var dot: Color {
        switch status {
        case .enabled: DesignTokens.Color.systemGreen
        case .notSet: DesignTokens.Color.labelTertiary
        case .denied: DesignTokens.Color.systemRed
        }
    }
    var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Circle().fill(dot).frame(width: 8, height: 8).accessibilityHidden(true)
            Text(status.title)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
    }
}

#if DEBUG
#Preview("Permissions") {
    NavigationStack { SettingsPermissionsScreen() }
}
#endif
