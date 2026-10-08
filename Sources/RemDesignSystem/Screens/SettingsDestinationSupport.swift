import SwiftUI

/// Shared helpers for the Settings prototype destination screens (Memory, Models). Owned by the
/// destination lane; the central `SettingsEntryContent` / `AgentSettingsContent` are untouched.

/// The playground-wide mock-data explanation. It is surfaced as a screen-level accessibility hint so
/// the illustrative nature is explained once per destination, rather than as extra implementation
/// text injected into the designed rows.
public enum PlaygroundMockData {
    public static let hint =
        "Illustrative prototype data. No accounts, keys, services, or persistence are used."
}

extension View {
    /// Inline navigation titles are iOS chrome. Keep shared destination views valid on macOS.
    @ViewBuilder func settingsInlineNavigationTitle() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    /// Native grouped-list chrome for a destination screen — insetGrouped on iOS, inset on macOS,
    /// with the Settings row-height minimum and no automatic header upper-casing.
    @ViewBuilder func settingsDestinationList() -> some View {
        #if os(iOS)
        self.listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
            .environment(\.defaultMinListRowHeight, 60)
            .textCase(nil)
        #else
        self.listStyle(.inset)
            .scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
            .environment(\.defaultMinListRowHeight, 60)
            .textCase(nil)
        #endif
    }
}
