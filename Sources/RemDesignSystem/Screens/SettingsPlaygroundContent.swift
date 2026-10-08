import SwiftUI

/// Settled Settings New entry (1964:86819). Navigation and data belong to the host.
/// Rows without actions are visual references; the playground identifies that limited scope.
public struct SettingsEntryContent: View {
    private let openAgent: () -> Void
    public init(openAgent: @escaping () -> Void) { self.openAgent = openAgent }
    public var body: some View {
        VStack(spacing: 22) {
            RemSection {
                ListRow(leading: {
                    Circle().fill(Color(red: 219/255, green: 219/255, blue: 229/255)).frame(width: 29, height: 29)
                }, content: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Avery Diaz").font(.body.weight(.bold))
                        Text(verbatim: "avery@example.com").font(.system(size: 13)).foregroundStyle(.secondary)
                    }
                }, trailing: { EmptyView() })
            }
            RemSection {
                Button(action: openAgent) {
                    SettingsReferenceRow("Rem", subtitle: "Connected", symbol: "info.circle.fill", color: .blue, height: 82)
                }.buttonStyle(.plain).accessibilityIdentifier("openAgent")
            }
            RemSection {
                SettingsReferenceRow("Billing & Usage", symbol: "creditcard.fill", color: .blue, divider: true)
                SettingsReferenceRow("Permissions", symbol: "hand.raised.fill", color: .blue)
            }
            RemSection { SettingsReferenceRow("About", symbol: "info.circle.fill", color: .gray) }
            RemSection {
                SettingsReferenceRow("Share Rem", symbol: "square.and.arrow.up", color: .green, divider: true, disclosure: false)
                SettingsReferenceRow("Help & Support", symbol: "square.and.arrow.up", color: .blue)
            }
            RemSection { referenceAction("Sign Out") }
            RemSection { referenceAction("Delete Account") }
        }.padding(.horizontal, 16).padding(.top, 10).padding(.bottom, 24)
    }
    private func referenceAction(_ title: String) -> some View {
        Text(title).font(.body).foregroundStyle(.red).frame(maxWidth: .infinity, minHeight: 60)
            .accessibilityHint("Visual reference; unavailable in this playground")
    }
}

/// Agent Settings master 1827:50855. Automations is preserved as a visual reference only.
public struct AgentSettingsContent: View {
    public init() {}
    public var body: some View {
        VStack(spacing: 22) {
            RemSection(header: "Capabilities", footer: "Manage connected surfaces and how your agent can perform.") {
                SettingsReferenceRow("Paired Devices", subtitle: "2", symbol: "macbook.and.iphone", color: .indigo, divider: true)
                SettingsReferenceRow("Connectors", symbol: "link.circle.fill", color: .purple, divider: true)
                SettingsReferenceRow("Cloud browser", symbol: "globe", color: .blue, divider: true)
                SettingsReferenceRow("Automations", subtitle: "Scheduled and triggered work", symbol: "bell.badge.fill", color: .pink)
            }.settingsHeader()
            RemSection(header: "Intelligence") {
                SettingsReferenceRow("Memory", symbol: "brain.head.profile", color: .pink, divider: true)
                SettingsReferenceRow("Models", subtitle: "Automatic", symbol: "cpu", color: .indigo, divider: true)
                SettingsReferenceRow("Wallet", symbol: "wallet.pass", color: .blue)
            }.settingsHeader()
            RemSection(header: "Experience") {
                SettingsReferenceRow("Voice", subtitle: "Aria", symbol: "waveform", color: .blue)
            }.settingsHeader()
        }.padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 24)
    }
}

private struct SettingsReferenceRow: View {
    let title: String
    let subtitle: String?
    let symbol: String
    let color: Color
    let divider: Bool
    let disclosure: Bool
    let height: CGFloat
    init(_ title: String, subtitle: String? = nil, symbol: String, color: Color, divider: Bool = false, disclosure: Bool = true, height: CGFloat = 60) {
        self.title = title; self.subtitle = subtitle; self.symbol = symbol; self.color = color; self.divider = divider; self.disclosure = disclosure; self.height = height
    }
    var body: some View {
        ListRow(showsDivider: divider, leading: {
            ContainedIcon(symbol, fill: .tint(color), size: .settings, glyphWeight: .regular).accessibilityHidden(true)
        }, content: {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.body).foregroundStyle(DesignTokens.Color.labelPrimary)
                if let subtitle { Text(subtitle).font(.system(size: 13)).foregroundStyle(.secondary) }
            }.frame(minHeight: height - 24, alignment: .leading)
        }, trailing: { if disclosure { DisclosureChevron().accessibilityHidden(true) } })
    }
}
