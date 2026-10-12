import SwiftUI

/// The approved Agent settings route contract. Automations opens its own page (`1833:5080`).
public enum AgentSettingsDestination: String, CaseIterable, Hashable, Sendable {
    case pairedDevices, connectors, automations, cloudBrowser, memory, models, wallet, voice
}

/// Value routes for hosts that own one path for the complete Settings hierarchy: Agent settings
/// plus the Billing & Usage, Permissions, About and Help & Support pages.
public enum SettingsEntryDestination: String, CaseIterable, Hashable, Sendable {
    case agentSettings, billing, permissions, about, helpSupport
}

/// Reusable Settings row content. Native containers own padding, separators and disclosure.
/// Standalone callers can opt into the legacy padded layout and an explicit divider.
public struct SettingsRowLabel: View {
    private let title: String
    private let subtitle: String?
    private let symbol: String
    private let layout: ListRowLayout
    private let showsDivider: Bool
    public init(_ title: String, subtitle: String? = nil, symbol: String,
                layout: ListRowLayout = .nativeList, showsDivider: Bool = false) {
        self.title = title; self.subtitle = subtitle; self.symbol = symbol
        self.layout = layout; self.showsDivider = showsDivider
    }
    public var body: some View {
        ListRow(showsDivider: showsDivider, layout: layout, leading: {
            ContainedIcon(symbol, fill: .subtle, size: .settings, glyphWeight: .regular)
                .accessibilityHidden(true)
        }, content: { ListRowLabel(title, subtitle: subtitle) }, trailing: { EmptyView() })
    }
}

/// Settings New entry 1964:86819. Owns its native List; never nest inside a ScrollView.
public struct SettingsEntryContent: View {
    private let openAgent: (() -> Void)?
    private let agentDestination: AnyView?
    private let agentRoute: SettingsEntryDestination?
    private let availableRoutes: Set<SettingsEntryDestination>
    private let onShare: (() -> Void)?
    /// Compatibility for action-driven hosts. The native destination initializer is preferred.
    public init(onShare: (() -> Void)? = nil, openAgent: @escaping () -> Void) {
        self.onShare = onShare
        self.openAgent = openAgent; self.agentDestination = nil; self.agentRoute = nil
        self.availableRoutes = []
    }
    public init<Destination: View>(onShare: (() -> Void)? = nil, @ViewBuilder agentDestination: () -> Destination) {
        self.onShare = onShare
        self.openAgent = nil; self.agentDestination = AnyView(agentDestination()); self.agentRoute = nil
        self.availableRoutes = []
    }
    /// Keeps native navigation in the host's value-driven path across Settings and its children.
    /// Rows whose route is in `availableRoutes` push that value; the rest stay static references.
    public init(onShare: (() -> Void)? = nil, agentRoute: SettingsEntryDestination,
                availableRoutes: Set<SettingsEntryDestination> = []) {
        self.onShare = onShare
        self.openAgent = nil; self.agentDestination = nil; self.agentRoute = agentRoute
        self.availableRoutes = availableRoutes
    }
    public var body: some View {
        List {
            Section {
                ListRow(layout: .nativeList, leading: {
                    Circle().fill(DesignTokens.Color.fillTertiary).frame(width: 29, height: 29)
                }, content: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Avery Diaz").font(.body.weight(.bold))
                        Text(verbatim: "avery@example.com").font(.footnote).foregroundStyle(.secondary)
                    }
                }, trailing: { EmptyView() })
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                if let agentRoute {
                    NavigationLink(value: agentRoute) { agentLabel }
                        .accessibilityIdentifier("openAgent")
                } else if let agentDestination {
                    NavigationLink { agentDestination } label: { agentLabel }
                        .accessibilityIdentifier("openAgent")
                } else if let openAgent {
                    Button(action: openAgent) { agentLabel }
                        .buttonStyle(.plain).accessibilityIdentifier("openAgent")
                }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                pageRow(.billing, "Billing & Usage", symbol: "creditcard.fill")
                pageRow(.permissions, "Permissions", symbol: "hand.raised.fill")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section { pageRow(.about, "About", symbol: "info.circle.fill") }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                if let onShare {
                    Button(action: onShare) { SettingsRowLabel("Share Rem", symbol: "square.and.arrow.up") }
                        .buttonStyle(.plain).accessibilityIdentifier("shareRem")
                } else { referenceRow("Share Rem", symbol: "square.and.arrow.up") }
                pageRow(.helpSupport, "Help & Support", symbol: "questionmark.circle.fill")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section { referenceAction("Sign Out") }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section { referenceAction("Delete Account") }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsListStyle()
        .accessibilityIdentifier("settingsEntry")
    }
    private var agentLabel: some View {
        SettingsRowLabel("Agent settings", symbol: "info.circle.fill")
    }
    @ViewBuilder private func pageRow(_ route: SettingsEntryDestination, _ title: String, symbol: String) -> some View {
        if availableRoutes.contains(route) {
            NavigationLink(value: route) { SettingsRowLabel(title, symbol: symbol) }
                .accessibilityIdentifier("settingsDestination.\(route.rawValue)")
        } else {
            referenceRow(title, symbol: symbol)
        }
    }
    private func referenceRow(_ title: String, symbol: String) -> some View {
        SettingsRowLabel(title, symbol: symbol)
            .accessibilityHint("Visual reference; unavailable in this playground")
    }
    private func referenceAction(_ title: String) -> some View {
        Text(title).font(.body).foregroundStyle(.red).frame(maxWidth: .infinity)
            .accessibilityHint("Visual reference; unavailable in this playground")
    }
}

/// Agent Settings master 1827:50855. Hosts can supply direct destinations or register value routes.
/// Unavailable routes remain references, without a misleading disclosure or no-op button.
public struct AgentSettingsContent: View {
    private let availableDestinations: Set<AgentSettingsDestination>
    private let destinationContent: ((AgentSettingsDestination) -> AnyView)?
    public init(availableDestinations: Set<AgentSettingsDestination> = []) {
        self.availableDestinations = availableDestinations
        self.destinationContent = nil
    }
    /// Use direct native links when this screen is itself reached through view-based navigation.
    public init<Destination: View>(availableDestinations: Set<AgentSettingsDestination>,
                                  @ViewBuilder destination: @escaping (AgentSettingsDestination) -> Destination) {
        self.availableDestinations = availableDestinations
        self.destinationContent = { AnyView(destination($0)) }
    }
    public var body: some View {
        List {
            Section {
                destination(.pairedDevices, "Paired Devices", subtitle: "2", symbol: "macbook.and.iphone")
                destination(.connectors, "Connectors", symbol: "link.circle.fill")
                destination(.cloudBrowser, "Cloud browser", symbol: "globe")
                destination(.automations, "Automations", subtitle: "Scheduled and triggered work", symbol: "bell.badge.fill")
            } header: { HStack { Text("Capabilities").textCase(nil) } } footer: {
                Text("Manage connected surfaces and how your agent can perform.")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                destination(.memory, "Memory", symbol: "brain.head.profile")
                destination(.models, "Models", subtitle: "Automatic", symbol: "cpu")
                destination(.wallet, "Wallet", symbol: "wallet.pass")
            } header: { HStack { Text("Intelligence").textCase(nil) } }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                destination(.voice, "Voice", subtitle: "Aria", symbol: "waveform")
            } header: { HStack { Text("Experience").textCase(nil) } }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsListStyle()
        .accessibilityIdentifier("agentSettings")
    }
    @ViewBuilder private func destination(_ route: AgentSettingsDestination, _ title: String,
                                         subtitle: String? = nil, symbol: String) -> some View {
        if availableDestinations.contains(route) {
            if let destinationContent {
                NavigationLink { destinationContent(route) } label: {
                    SettingsRowLabel(title, subtitle: subtitle, symbol: symbol)
                }.accessibilityIdentifier("agentDestination.\(route.rawValue)")
            } else {
                NavigationLink(value: route) { SettingsRowLabel(title, subtitle: subtitle, symbol: symbol) }
                    .accessibilityIdentifier("agentDestination.\(route.rawValue)")
            }
        } else {
            SettingsRowLabel(title, subtitle: subtitle, symbol: symbol)
                .accessibilityIdentifier("agentDestination.\(route.rawValue).unavailable")
                .accessibilityHint("Visual reference; unavailable in this playground")
        }
    }
}

private extension View {
    @ViewBuilder func settingsListStyle() -> some View {
        #if os(iOS)
        self.listStyle(.insetGrouped).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
            .environment(\.defaultMinListRowHeight, 60).textCase(nil)
        #else
        self.listStyle(.inset).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
            .environment(\.defaultMinListRowHeight, 60).textCase(nil)
        #endif
    }
}
