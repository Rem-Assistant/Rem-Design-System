import SwiftUI

/// Shared helpers for the Settings prototype destination screens (Memory, Models, and the
/// Automations, Billing & Usage, Permissions, About and Help & Support pages).

/// The playground-wide mock-data explanation. It is surfaced as a screen-level accessibility hint so
/// the illustrative nature is explained once per destination, rather than as extra implementation
/// text injected into the designed rows.
public enum PlaygroundMockData {
    public static let hint =
        "Illustrative prototype data. No accounts, keys, services, or persistence are used."
}

extension View {
    /// Explains a control whose behavior needs a service the design system does not have. The row
    /// stays interactive and honest: tapping it states the limitation instead of doing nothing.
    func settingsPrototypeBoundary(_ message: Binding<String?>) -> some View {
        alert("Prototype boundary", isPresented: Binding(get: { message.wrappedValue != nil }, set: {
            if !$0 { message.wrappedValue = nil }
        })) {
            Button("Done", role: .cancel) { message.wrappedValue = nil }
        } message: { Text(message.wrappedValue ?? "") }
    }

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
