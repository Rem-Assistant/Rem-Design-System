import SwiftUI

/// **Paired devices** — the Agent-settings `pairedDevices` destination, reproduced from the approved
/// masters: populated list (`1833:5031`), empty state (`1833:52316`), device detail (`1833:52344`),
/// and the remove-access confirmation (`1839:52547`).
///
/// A **public, zero-argument, deterministic** screen entry point. It owns its *nested* navigation
/// (list → detail) with `NavigationLink`, but never its own `NavigationStack`: the playground host's
/// outer stack owns navigation, so this view just drops into the registered `pairedDevices`
/// destination. All state is in-memory for the session; see `PairedDevicesState`.
///
/// Native iOS semantics throughout: grouped `List`/`Section`, `NavigationLink` disclosure, a
/// `confirmationDialog` action sheet for removal, and a page sheet for the Add boundary. `Add` has no
/// authored pairing flow, so it opens an explicit prototype boundary that states pairing is not
/// simulated — it never fabricates a device.
public struct SettingsPairedDevicesScreen: View {
    @State private var state = PairedDevicesState()
    @State private var showingAddBoundary = false

    public init() {}

    public var body: some View {
        Group {
            if state.isEmpty {
                emptyState
            } else {
                deviceList
            }
        }
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationTitle(PairedDevicesCopy.navigationTitle)
        .settingsInlineNavigationTitle()
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                if !state.isEmpty {
                    Button(PairedDevicesCopy.addAction) { showingAddBoundary = true }
                        .accessibilityIdentifier("pairedDevices.add")
                }
            }
        }
        .sheet(isPresented: $showingAddBoundary) { addBoundarySheet }
        .accessibilityIdentifier("pairedDevices")
    }

    // MARK: Populated list (1833:5031)

    private var deviceList: some View {
        List {
            Section {
                ForEach(state.peers) { peer in
                    NavigationLink {
                        PairedDeviceDetailView(peer: peer) { removed in
                            state.remove(id: removed)
                        }
                    } label: {
                        deviceRow(peer)
                    }
                    .accessibilityIdentifier("pairedDevices.peer.\(peer.id)")
                }
            } header: {
                HStack { Text(PairedDevicesCopy.sectionHeader).textCase(nil) }
            } footer: {
                Text(PairedDevicesCopy.listFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
    }

    private func deviceRow(_ peer: PairedDevicePeer) -> some View {
        ListRow(layout: .nativeList, leading: {
            Image(systemName: peer.heroSymbol)
                .font(.system(size: 20, weight: .regular))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(width: 29, height: 29)
                .accessibilityHidden(true)
        }, content: {
            VStack(alignment: .leading, spacing: 3) {
                Text(peer.name)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                HStack(spacing: DesignTokens.Spacing.xs) {
                    Circle()
                        .fill(peer.isConnected ? DesignTokens.Color.systemGreen : DesignTokens.Color.labelTertiary)
                        .frame(width: 8, height: 8)
                        .accessibilityHidden(true)
                    Text(peer.connectionSummary)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
            }
        }, trailing: { EmptyView() })
        .accessibilityElement(children: .combine)
    }

    // MARK: Empty state (1833:52316)

    private var emptyState: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    Spacer()
                    ContainedIcon(PairedDevicesCopy.emptySymbol, fill: .subtle, size: .large, glyphWeight: .regular)
                        .accessibilityHidden(true)
                    VStack(spacing: DesignTokens.Spacing.sm) {
                        Text(PairedDevicesCopy.emptyTitle)
                            .font(DesignTokens.Typography.title1Bold)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .multilineTextAlignment(.center)
                        Text(PairedDevicesCopy.emptyMessage)
                            .font(DesignTokens.Typography.subheadline)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .multilineTextAlignment(.center)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    Button(PairedDevicesCopy.refreshAction) { state.refresh() }
                        .remButton(.rectBlue)
                        .accessibilityIdentifier("pairedDevices.refresh")
                    Spacer()
                    Spacer()
                }
                .padding(.horizontal, DesignTokens.Spacing.xl)
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
            }
        }
    }

    // MARK: Add boundary (explicit, no authored pairing destination)

    private var addBoundarySheet: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    ContainedIcon(PairedDevicesCopy.emptySymbol, fill: .subtle, size: .large, glyphWeight: .regular)
                        .accessibilityHidden(true)
                    Text(PairedDevicesCopy.addMessage)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(DesignTokens.Spacing.xl)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.vertical, DesignTokens.Spacing.xl)
            }
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle(PairedDevicesCopy.addTitle)
            .settingsInlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(PairedDevicesCopy.addDismiss) { showingAddBoundary = false }
                }
            }
        }
        .accessibilityIdentifier("pairedDevices.addBoundary")
    }
}

/// Device detail (`1833:52344`) + remove-access confirmation (`1839:52547`). Pushed by the list; the
/// confirmation's destructive action removes the fixture peer via `onRemove` and pops back, so the
/// list reaches its designed empty state after the last peer. Cancel preserves the peer and detail.
struct PairedDeviceDetailView: View {
    let peer: PairedDevicePeer
    let onRemove: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showingRemoveConfirmation = false

    var body: some View {
        List {
            Section {
                VStack(spacing: DesignTokens.Spacing.xs) {
                    ContainedIcon(peer.heroSymbol, fill: .subtle, size: .large, glyphWeight: .regular)
                        .accessibilityHidden(true)
                    Text(peer.name)
                        .font(DesignTokens.Typography.title1Bold)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text(peer.kind)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, DesignTokens.Spacing.sm)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }

            Section {
                ForEach(peer.connectionRows) { row in
                    HStack {
                        Text(row.label)
                            .font(DesignTokens.Typography.body)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                        Spacer(minLength: DesignTokens.Spacing.md)
                        Text(row.value)
                            .font(DesignTokens.Typography.body)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                    }
                }
            } header: {
                HStack { Text(PairedDevicesCopy.connectionSectionHeader).textCase(nil) }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                Button(role: .destructive) {
                    showingRemoveConfirmation = true
                } label: {
                    Text(PairedDevicesCopy.removeAccess)
                        .frame(maxWidth: .infinity)
                }
                .accessibilityIdentifier("pairedDevices.removeAccess")
            } footer: {
                Text(PairedDevicesCopy.detailFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle(peer.name)
        .settingsInlineNavigationTitle()
        .confirmationDialog(
            PairedDevicesCopy.removeConfirmationTitle(peer.name),
            isPresented: $showingRemoveConfirmation,
            titleVisibility: .visible
        ) {
            Button(PairedDevicesCopy.removeAccess, role: .destructive) {
                onRemove(peer.id)
                dismiss()
            }
            .accessibilityIdentifier("pairedDevices.confirmRemove")
            Button(PairedDevicesCopy.cancel, role: .cancel) {}
        } message: {
            Text(PairedDevicesCopy.removeConfirmationMessage)
        }
    }
}

#if DEBUG
#Preview("Paired devices — populated") {
    NavigationStack { SettingsPairedDevicesScreen() }
}
#endif
