import SwiftUI

/// Settings Connectors + the seven authored Gmail states (1883:7678–7684).
/// Native List/Section and outer host NavigationStack; no real account or provider is contacted.
public struct SettingsConnectorsScreen: View {
    @State private var fixture = GmailFixture()
    @State private var showingGmail = false
    @State private var boundary: String?
    public init() {}

    public var body: some View {
        List {
            Section("Connected") {
                if fixture.isConnected {
                    NavigationLink(isActive: $showingGmail) {
                        GmailSettingsView(fixture: $fixture, onDisconnected: {
                            if !fixture.isConnected { showingGmail = false }
                        })
                    } label: {
                        ConnectorRow("Gmail", state: .connected, subtitle: "Connected • Active",
                                     accessory: .disclosure, layout: .nativeList) { ConnectorProviderMark(.gmail) }
                    }
                    .accessibilityIdentifier("connectors.provider.gmail")
                }
                ForEach([ConnectorProvider.googleCalendar, .notion, .slack]) { provider in
                    Button {
                        boundary = "\(provider.title) account details are not included in this prototype. No connection is changed."
                    } label: {
                        HStack {
                            ConnectorRow(provider.title, state: .connected,
                                         subtitle: provider == .slack ? "Connected • Paused" : "Connected • Active",
                                         accessory: .disclosure, layout: .nativeList) { ConnectorProviderMark(provider) }
                            DisclosureChevron()
                        }
                    }.buttonStyle(.plain).accessibilityIdentifier("connectors.provider.\(provider.id)")
                }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section("Available") {
                if !fixture.isConnected { available(.gmail) }
                ForEach([ConnectorProvider.googleDrive, .linear, .todoist]) { available($0) }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .connectorListSurface().environment(\.defaultMinListRowHeight, 64)
        .navigationTitle("Connectors").settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsConnectors")
        .connectorBoundary($boundary)
    }
    private func available(_ provider: ConnectorProvider) -> some View {
        ConnectorRow(provider.title, state: .available, accessory: .action("Connect", {
            boundary = "Connecting \(provider.title) requires provider authorization, which is not included in this prototype. No account is connected."
        }), layout: .nativeList) { ConnectorProviderMark(provider) }
        .accessibilityIdentifier("connectors.provider.\(provider.id)")
    }
}

private struct GmailSettingsView: View {
    @Binding var fixture: GmailFixture
    let onDisconnected: () -> Void
    @State private var confirmation: GmailDisconnect?
    @State private var accountSettings: GmailAccountFixture?
    @State private var boundary: String?

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    ConnectorProviderMark(.gmail).frame(width: 64, height: 64)
                    Text("Gmail").font(DesignTokens.Typography.title1Bold)
                    Text("Read and manage Gmail").font(.body).foregroundStyle(DesignTokens.Color.labelSecondary)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, DesignTokens.Spacing.sm)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section("Connected accounts") {
                ForEach(fixture.accounts) { account in
                    ListRow(layout: .nativeList, leading: { GmailAccountAvatar() }, content: {
                        ListRowLabel(account.email, subtitle: account.role)
                    }, trailing: {
                        Menu {
                            Button("Settings") { accountSettings = account }
                            .accessibilityIdentifier("gmail.accountSettings.\(account.id)")
                            Button("Disconnect account", role: .destructive) { confirmation = .account(account) }
                                .accessibilityIdentifier("gmail.disconnectAccount.\(account.id)")
                        } label: { Image(systemName: "ellipsis").frame(minWidth: 44, minHeight: 44) }
                        .accessibilityLabel("Actions for \(account.email)")
                        .accessibilityIdentifier("gmail.accountMenu.\(account.id)")
                    })
                    .accessibilityIdentifier("gmail.account.\(account.id)")
                }
                Button("Connect another account") {
                    boundary = "Adding a Gmail account requires provider authorization, which is not included in this prototype. No account is connected."
                }.accessibilityIdentifier("gmail.connectAnother")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                NavigationLink {
                    GmailPermissionsView(fixture: $fixture, account: nil)
                } label: { ListRowLabel("Permissions", subtitle: fixture.connectorPermission.title) }
                .accessibilityIdentifier("gmail.permissions")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            actionSection("Read actions", actions: GmailFixtureCopy.readActions)
            actionSection("Write actions", actions: GmailFixtureCopy.writeActions)
            Section("Information") {
                boundaryRow("Category", subtitle: "Productivity", message: "Connector category details are not included in this prototype.")
                boundaryRow("Website", subtitle: "mail.google.com", message: "External Gmail website navigation is not included in this prototype.")
                boundaryRow("Privacy Policy", subtitle: "policies.google.com", message: "External privacy-policy navigation is not included in this prototype.")
                boundaryRow("Report an issue", message: "Issue reporting is not included in this prototype. No report is sent.")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .connectorListSurface().navigationTitle("Gmail").settingsInlineNavigationTitle()
        .accessibilityIdentifier("gmail.detail")
        .navigationDestination(item: $accountSettings) { account in
            GmailPermissionsView(fixture: $fixture, account: account)
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button("Disconnect accounts", role: .destructive) { confirmation = .all }
                        .accessibilityIdentifier("gmail.disconnectAll")
                } label: { Image(systemName: "ellipsis") }
                .accessibilityLabel("Gmail actions").accessibilityIdentifier("gmail.connectorMenu")
            }
        }
        .confirmationDialog(confirmation?.title ?? "Disconnect account?",
            isPresented: Binding(get: { confirmation != nil }, set: { if !$0 { confirmation = nil } }),
            titleVisibility: .visible, presenting: confirmation) { target in
                Button(target.action, role: .destructive) {
                    switch target {
                    case .all: fixture.disconnectAll()
                    case .account(let account): fixture.disconnect(accountID: account.id)
                    }
                    confirmation = nil
                    onDisconnected()
                }.accessibilityIdentifier("gmail.confirmDisconnect")
                Button("Cancel", role: .cancel) { confirmation = nil }
            } message: { Text($0.message) }
        .connectorBoundary($boundary)
    }
    private func actionSection(_ title: String, actions: [String]) -> some View {
        Section(title) {
            ForEach(actions, id: \.self) { action in
                boundaryRow(action, message: "\(action) is a listed Gmail capability. Executing actions is not included in this prototype; no email is read or changed.")
            }
        }.listRowBackground(DesignTokens.Color.backgroundSecondary)
    }
    private func boundaryRow(_ title: String, subtitle: String? = nil, message: String) -> some View {
        Button { boundary = message } label: {
            HStack { ListRowLabel(title, subtitle: subtitle); Spacer(minLength: 8); DisclosureChevron() }
        }.buttonStyle(.plain).accessibilityIdentifier("gmail.info.\(title)")
    }
}

private enum GmailDisconnect {
    case account(GmailAccountFixture), all
    var title: String {
        switch self {
        case .account(let account): "Disconnect \(account.email)?"
        case .all: GmailFixtureCopy.disconnectAllTitle
        }
    }
    var message: String {
        switch self {
        case .account(let account): GmailFixtureCopy.disconnectMessage(account.email)
        case .all: GmailFixtureCopy.disconnectAllMessage
        }
    }
    var action: String { if case .all = self { "Disconnect accounts" } else { "Disconnect account" } }
}

private struct GmailPermissionsView: View {
    @Binding var fixture: GmailFixture
    let account: GmailAccountFixture?
    private var selected: GmailPermission {
        if let account { fixture.permission(for: account.id) } else { fixture.connectorPermission }
    }
    private var scopeID: String { account?.id ?? "connector" }
    var body: some View {
        List {
            Section {
                ListRow(layout: .nativeList, leading: {
                    if account != nil { GmailAccountAvatar(emphasized: false) } else { ConnectorProviderMark(.gmail) }
                }, content: {
                    ListRowLabel(account?.email ?? "Gmail", subtitle: account.map { "Gmail · \($0.role) account" } ?? "All connected accounts")
                }, trailing: { EmptyView() })
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                ForEach(GmailPermission.allCases) { permission in
                    Button {
                        if let account { fixture.setAccountPermission(permission, accountID: account.id) }
                        else { fixture.setConnectorPermission(permission) }
                    } label: {
                        HStack(spacing: DesignTokens.Spacing.sm) {
                            VStack(alignment: .leading, spacing: 2) {
                                ViewThatFits(in: .horizontal) {
                                    HStack(spacing: DesignTokens.Spacing.sm) { Text(permission.title); permissionBadge(permission) }
                                    VStack(alignment: .leading, spacing: 4) { Text(permission.title); permissionBadge(permission) }
                                }
                                .font(.body).foregroundStyle(DesignTokens.Color.labelPrimary)
                                Text(permission.explanation).font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }.frame(maxWidth: .infinity, alignment: .leading)
                            Image(systemName: selected == permission ? "checkmark.circle.fill" : "circle")
                                .font(.title3).foregroundStyle(selected == permission ? DesignTokens.Color.brandBlue : DesignTokens.Color.labelSecondary)
                                .accessibilityHidden(true)
                        }.contentShape(Rectangle())
                    }.buttonStyle(.plain)
                        .accessibilityAddTraits(selected == permission ? .isSelected : [])
                        .accessibilityIdentifier("gmail.permission.\(scopeID).\(permission.id)")
                }
            } header: { Text("Access level") } footer: {
                Text(account.map { GmailFixtureCopy.accountFooter($0.email) } ?? GmailFixtureCopy.connectorFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .connectorListSurface().navigationTitle(account == nil ? "Permissions" : "Settings")
        .settingsInlineNavigationTitle().accessibilityIdentifier("gmail.permissions.\(scopeID)")
    }
    @ViewBuilder private func permissionBadge(_ permission: GmailPermission) -> some View {
        if permission == .lowRisk {
            Text("Default").font(.caption2).foregroundStyle(DesignTokens.Color.brandBlue)
                .padding(.horizontal, 8).padding(.vertical, 4)
                .background(DesignTokens.Color.brandBlue.opacity(0.12), in: Capsule())
        } else if permission == .alwaysAllow {
            HStack(spacing: 3) {
                Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(DesignTokens.Color.systemRed)
                Text("Elevated risk")
            }.font(.caption2).padding(.horizontal, 8).padding(.vertical, 4)
                .background(DesignTokens.Color.systemRed.opacity(0.12), in: Capsule())
        }
    }
}

/// Avatar185:2 is a 29pt circle. Preserve the default-detail system-blue instance override
/// and the unbound neutral master used in account permissions; no invented photo or initials.
private struct GmailAccountAvatar: View {
    var emphasized = true
    var body: some View {
        Circle().fill(emphasized ? DesignTokens.Color.systemBlue : Color("ConnectorAvatarNeutral", bundle: .module)).frame(width: 29, height: 29).accessibilityHidden(true)
    }
}
private extension View {
    func connectorListSurface() -> some View {
        settingsDestinationList().scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary)
    }
    func connectorBoundary(_ message: Binding<String?>) -> some View {
        alert("Prototype boundary", isPresented: Binding(get: { message.wrappedValue != nil }, set: {
            if !$0 { message.wrappedValue = nil }
        })) {
            Button("Done", role: .cancel) { message.wrappedValue = nil }
        } message: { Text(message.wrappedValue ?? "") }
    }
}
