import SwiftUI

/// The approved route contract. Automations has no settled destination yet.
public enum AgentSettingsDestination: String, CaseIterable, Hashable, Sendable {
    case pairedDevices, connectors, cloudBrowser, memory, models, wallet, voice
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
    private let onShare: (() -> Void)?
    /// Compatibility for action-driven hosts. The native destination initializer is preferred.
    public init(onShare: (() -> Void)? = nil, openAgent: @escaping () -> Void) {
        self.onShare = onShare
        self.openAgent = openAgent; self.agentDestination = nil
    }
    public init<Destination: View>(onShare: (() -> Void)? = nil, @ViewBuilder agentDestination: () -> Destination) {
        self.onShare = onShare
        self.openAgent = nil; self.agentDestination = AnyView(agentDestination())
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
            }
            Section {
                if let agentDestination {
                    NavigationLink { agentDestination } label: { agentLabel }
                        .accessibilityIdentifier("openAgent")
                } else if let openAgent {
                    Button(action: openAgent) { agentLabel }
                        .buttonStyle(.plain).accessibilityIdentifier("openAgent")
                }
            }
            Section {
                referenceRow("Billing & Usage", symbol: "creditcard.fill")
                referenceRow("Permissions", symbol: "hand.raised.fill")
            }
            Section { referenceRow("About", symbol: "info.circle.fill") }
            Section {
                if let onShare {
                    Button(action: onShare) { SettingsRowLabel("Share Rem", symbol: "square.and.arrow.up") }
                        .buttonStyle(.plain).accessibilityIdentifier("shareRem")
                } else { referenceRow("Share Rem", symbol: "square.and.arrow.up") }
                referenceRow("Help & Support", symbol: "square.and.arrow.up")
            }
            Section { referenceAction("Sign Out") }
            Section { referenceAction("Delete Account") }
        }
        .settingsListStyle()
        .accessibilityIdentifier("settingsEntry")
    }
    private var agentLabel: some View {
        SettingsRowLabel("Rem", subtitle: "Connected", symbol: "info.circle.fill")
            .frame(minHeight: 58)
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

/// Agent Settings master 1827:50855. The host registers destinations using navigationDestination.
/// Unavailable routes remain references, without a misleading disclosure or no-op button.
public struct AgentSettingsContent: View {
    private let availableDestinations: Set<AgentSettingsDestination>
    public init(availableDestinations: Set<AgentSettingsDestination> = []) {
        self.availableDestinations = availableDestinations
    }
    public var body: some View {
        List {
            Section {
                destination(.pairedDevices, "Paired Devices", subtitle: "2", symbol: "macbook.and.iphone")
                destination(.connectors, "Connectors", symbol: "link.circle.fill")
                destination(.cloudBrowser, "Cloud browser", symbol: "globe")
                SettingsRowLabel("Automations", subtitle: "Scheduled and triggered work", symbol: "bell.badge.fill")
                    .accessibilityIdentifier("automationsUnavailable")
                    .accessibilityHint("Unavailable. Automations design is awaiting a decision.")
            } header: { Text("Capabilities") } footer: {
                Text("Manage connected surfaces and how your agent can perform.")
            }
            Section("Intelligence") {
                destination(.memory, "Memory", symbol: "brain.head.profile")
                destination(.models, "Models", subtitle: "Automatic", symbol: "cpu")
                destination(.wallet, "Wallet", symbol: "wallet.pass")
            }
            Section("Experience") {
                destination(.voice, "Voice", subtitle: "Aria", symbol: "waveform")
            }
        }
        .settingsListStyle()
        .accessibilityIdentifier("agentSettings")
    }
    @ViewBuilder private func destination(_ route: AgentSettingsDestination, _ title: String,
                                         subtitle: String? = nil, symbol: String) -> some View {
        if availableDestinations.contains(route) {
            NavigationLink(value: route) { SettingsRowLabel(title, subtitle: subtitle, symbol: symbol) }
                .accessibilityIdentifier("agentDestination.\(route.rawValue)")
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
        self.listStyle(.insetGrouped).environment(\.defaultMinListRowHeight, 60).textCase(nil)
        #else
        self.listStyle(.inset).environment(\.defaultMinListRowHeight, 60).textCase(nil)
        #endif
    }
}
