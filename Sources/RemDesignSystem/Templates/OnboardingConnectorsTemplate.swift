import SwiftUI

/// Presentational template for the onboarding **"Connect your apps"** (Connectors) screen.
/// Pure: no Composio client, no OAuth, no navigation — the app supplies the connector data + row
/// actions and owns navigation/first-run coach-mark. Mirrors the shipping `SharedComposioConnectionsView`
/// (Rem/Shared/Views/Settings). Figma: `Screen/Connectors` (`133:192`). Composes design-system
/// components: `ContainedIcon` (brand-tinted row leading), `ListRow` (connector rows), `RemSection`
/// (grouped CONNECTED / AVAILABLE surfaces). iOS-canonical; renders adaptively on iPadOS/macOS.
///
/// Brand tiles use `ContainedIcon(.tint(brand))` with a representative SF Symbol per provider — brand
/// *logo* assets are a follow-up (icon registry); the tile color + glyph read as the provider today.
public struct OnboardingConnectorsTemplate: View {
    /// A single connector row: brand tile + name + connection status + tap target.
    public struct Connector: Identifiable {
        public let id = UUID()
        public let symbol: String
        public let tint: Color
        public let name: String
        public let status: String
        public let action: () -> Void
        public init(symbol: String, tint: Color, name: String, status: String, action: @escaping () -> Void) {
            self.symbol = symbol; self.tint = tint; self.name = name; self.status = status; self.action = action
        }
    }

    var title: String
    var connected: [Connector]
    var available: [Connector]

    public init(
        title: String = "Connectors",
        connected: [Connector],
        available: [Connector]
    ) {
        self.title = title
        self.connected = connected
        self.available = available
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.lg) {
                if !connected.isEmpty {
                    RemSection(header: "Connected", rows: connected) { c in row(c) }
                }
                if !available.isEmpty {
                    RemSection(header: "Available", rows: available) { c in row(c) }
                }
            }
            .padding(DesignTokens.Spacing.lg)
            .frame(maxWidth: 560)
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .navigationTitle(title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.large)
        #endif
    }

    private func row(_ c: Connector) -> some View {
        ListRow(
            c.name,
            subtitle: c.status,
            action: c.action,
            leading: { ContainedIcon(c.symbol, fill: .tint(c.tint)) },
            trailing: { DisclosureChevron() }
        )
    }
}

#if DEBUG
#Preview("OnboardingConnectorsTemplate") {
    NavigationStack {
        OnboardingConnectorsTemplate(
            connected: [
                .init(symbol: "envelope.fill", tint: .red, name: "Gmail", status: "Connected · Active", action: {}),
                .init(symbol: "calendar", tint: .blue, name: "Google Calendar", status: "Connected · Active", action: {}),
                .init(symbol: "note.text", tint: .primary, name: "Notion", status: "Connected · Active", action: {}),
                .init(symbol: "number", tint: .purple, name: "Slack", status: "Connected · Paused", action: {}),
            ],
            available: [
                .init(symbol: "externaldrive.fill", tint: .green, name: "Google Drive", status: "Not connected", action: {}),
                .init(symbol: "list.bullet.rectangle", tint: .indigo, name: "Linear", status: "Not connected", action: {}),
                .init(symbol: "checklist", tint: .red, name: "Todoist", status: "Not connected", action: {}),
            ]
        )
    }
}
#endif
