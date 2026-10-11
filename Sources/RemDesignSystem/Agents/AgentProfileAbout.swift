import SwiftUI

/// **AgentProfileHeader** — the identity block at the top of an agent's profile: avatar (Rem face for
/// the Main agent, the chosen look for a side agent), name, and a green status dot with
/// "Side agent · <lane>" / "Main agent · <lane>".
///
/// Figma: `2718:3060` (Health) and the same block in `2718:3396` (Rem). Compose twin:
/// `agents/AgentProfileAbout.kt`.
public struct AgentProfileHeader: View {
    private let display: AgentProfileDisplay

    public init(_ display: AgentProfileDisplay) {
        self.display = display
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            AgentAvatar(display.avatar, size: 96)
            VStack(spacing: DesignTokens.Spacing.xs) {
                Text(display.name)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .multilineTextAlignment(.center)
                HStack(spacing: 6) {
                    Circle()
                        .fill(DesignTokens.Color.systemGreen)
                        .frame(width: 6, height: 6)
                    Text(display.statusLine)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("agentProfile.identity")
    }
}

/// **AgentProfileAbout** — the **About** tab of an agent's profile: **Lane** (Name, Handles),
/// **Connected services** (each service + Add a service) and, for side agents only, **Pause agent** /
/// **Delete agent**. Rem's About lists its scope read-only and has no lifecycle actions.
///
/// Figma: About block `2718:3205` in **E3 · Agent profile — Health · About** (`2718:3055`). This is
/// the tab *body*; the tab control and Back-to-Chat chrome belong to Agent activity (see
/// `AgentProfileModel.swift` for the reconciliation note). Display + callbacks only.
/// Compose twin: `agents/AgentProfileAbout.kt`.
public struct AgentProfileAbout: View {
    private let display: AgentProfileDisplay
    private let onAction: (AgentProfileAction) -> Void

    public init(_ display: AgentProfileDisplay, onAction: @escaping (AgentProfileAction) -> Void) {
        self.display = display
        self.onAction = onAction
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            laneSection
            servicesSection
            if display.showsLifecycleActions { lifecycleSection }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("agentProfile.about")
    }

    // MARK: Lane

    private var laneSection: some View {
        AgentFormSection(header: AgentProfileCopy.laneHeader) {
            laneRow(AgentProfileCopy.nameRow, value: display.name, action: .editName, id: "name")
            AgentRowDivider(leadingInset: DesignTokens.Spacing.lg)
            laneRow(AgentProfileCopy.handlesRow, value: display.handles, action: .editHandles, id: "handles")
        }
    }

    @ViewBuilder
    private func laneRow(_ title: String, value: String, action: AgentProfileAction, id: String) -> some View {
        let subtitle = value.isEmpty ? nil : value
        Group {
            if display.isLaneEditable {
                ListRow(title, subtitle: subtitle, action: { onAction(action) },
                        leading: { EmptyView() }, trailing: { DisclosureChevron() })
                    .accessibilityAddTraits(.isButton)
            } else {
                ListRow(title, subtitle: subtitle, leading: { EmptyView() }, trailing: { EmptyView() })
            }
        }
        .frame(minHeight: 60)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("agentProfile.lane.\(id)")
    }

    // MARK: Services

    private var servicesSection: some View {
        AgentFormSection(header: AgentServicesCopy.header) {
            ForEach(display.services) { service in
                AgentServiceRow(title: service.name, subtitle: service.status, glyph: service.glyph) {
                    onAction(.openService(id: service.id))
                }
                .accessibilityIdentifier("agentProfile.service.\(service.id)")
                AgentRowDivider()
            }
            AgentServiceRow(title: AgentServicesCopy.addService, subtitle: nil, glyph: .link) {
                onAction(.addService)
            }
            .accessibilityIdentifier("agentProfile.addService")
        }
    }

    // MARK: Pause / Delete (side agents only)

    private var lifecycleSection: some View {
        RemSection {
            Button(AgentProfileCopy.pause) { onAction(.pause) }
                .remButton(.textAccent)
                .frame(maxWidth: .infinity, minHeight: 44)
                .accessibilityIdentifier("agentProfile.pause")
            Button(AgentProfileCopy.delete, role: .destructive) { onAction(.delete) }
                .remButton(.textDestructive)
                .frame(maxWidth: .infinity, minHeight: 44)
                .accessibilityIdentifier("agentProfile.delete")
        }
    }
}

#if DEBUG
#Preview("AgentProfileAbout — side agent") {
    ScrollView {
        VStack(spacing: DesignTokens.Spacing.xl) {
            AgentProfileHeader(.init(kind: .side, name: "Health", laneSummary: "Appointments and refills",
                                     handles: "Appointments, prescriptions, insurance claims"))
            AgentProfileAbout(.init(kind: .side, name: "Health", laneSummary: "Appointments and refills",
                                    handles: "Appointments, prescriptions, insurance claims",
                                    services: [.init(id: "gcal", name: "Google Calendar", status: "Connected", glyph: .calendar)])) { _ in }
        }
        .padding(DesignTokens.Spacing.lg)
    }
}
#endif
