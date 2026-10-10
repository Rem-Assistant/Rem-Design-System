import SwiftUI

/// **AgentActivityScreen** — agent detail reached from the agent identity in Chat: the Rem face with
/// the agent's **current** activity, an Activity / Approvals segmented control, and the day-grouped
/// Activity timeline (action title, outcome summary and time per row).
///
/// Figma master: **Rem/Chat/Agent activity** (`2002:76914`). Chrome is the platform navigation bar:
/// the host pushes this screen onto its `NavigationStack`, so Back returns to Chat natively (title
/// "Agent activity"; the host's Chat title becomes the back label). The current state comes only from
/// `display.current`; timeline rows never change it. Rows have no verified destination, so they are
/// not interactive, and the Approvals tab shows a labeled data gap rather than invented approvals.
/// Kept distinct from agent settings, card outcomes and voice state.
///
/// Composed from canonical pieces: `RemFaceMark`, `RemSection` (Plain, Settings header), `ListRow`
/// content slot + `ListRowLabel`, `RemContentUnavailableView`, and the native segmented `Picker`.
/// Compose sibling: `screens/AgentActivityScreen.kt`. Inputs: `AgentActivityModel.swift`.
public struct AgentActivityScreen: View {
    private let display: AgentActivityDisplay
    @Binding private var selection: AgentActivityTab

    public init(_ display: AgentActivityDisplay, selection: Binding<AgentActivityTab>) {
        self.display = display
        self._selection = selection
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.xl) {
                identity
                Picker("Agent activity section", selection: $selection) {
                    ForEach(AgentActivityTab.allCases) { tab in
                        Text(tab.title).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("agentActivity.tabs")

                switch selection {
                case .activity: timeline
                case .approvals: approvalsGap
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, 20)
            .padding(.bottom, 32)
            .frame(maxWidth: .infinity)
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .navigationTitle(AgentActivityDisplay.title)
        .settingsInlineNavigationTitle()
    #if os(iOS)
        // Chat hides the bar for its own header; this screen uses the native one.
        .toolbar(.visible, for: .navigationBar)
    #endif
    }

    // MARK: Identity (current state only)

    private var identity: some View {
        let current = display.current
        let faceMode: RemFaceMark.Mode = current.isWorking ? .thinking : .idle
        return VStack(spacing: DesignTokens.Spacing.md) {
            RemFaceMark(mode: faceMode, tint: DesignTokens.Color.brandBlue, size: 72)
                .id(faceMode)
                .frame(width: 96, height: 96)
                .background(DesignTokens.Color.backgroundSecondary, in: Circle())
                .accessibilityHidden(true)
            VStack(spacing: DesignTokens.Spacing.xs) {
                Text(current.name)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                HStack(spacing: 6) {
                    Circle()
                        .fill(current.status == .connected ? DesignTokens.Color.systemGreen : DesignTokens.Color.systemOrange)
                        .frame(width: 6, height: 6)
                    Text(current.activity)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("agentActivity.identity")
    }

    // MARK: Activity timeline

    @ViewBuilder private var timeline: some View {
        let days = display.visibleDays
        if days.isEmpty {
            RemContentUnavailableView(
                symbol: "clock",
                title: AgentActivityDisplay.activityEmptyTitle,
                message: AgentActivityDisplay.activityEmptyMessage
            )
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("agentActivity.empty")
        } else {
            VStack(spacing: DesignTokens.Spacing.lg) {
                ForEach(days) { day in
                    RemSection(header: day.title, style: .plain) {
                        ForEach(day.events) { event in row(event) }
                    }
                    .settingsHeader()
                    .accessibilityIdentifier("agentActivity.day.\(day.id)")
                }
            }
        }
    }

    /// A timeline row: not a button — no verified destination exists.
    private func row(_ event: AgentActivityEvent) -> some View {
        ListRow(
            leading: {
                RemFaceMark(mode: .idle, tint: DesignTokens.Color.brandBlue, size: 18)
                    .frame(width: 29, height: 29)
                    .background(
                        DesignTokens.Color.backgroundSecondary,
                        in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                    )
                    .accessibilityHidden(true)
            },
            content: { ListRowLabel(event.title, subtitle: event.summary) },
            trailing: { EmptyView() }
        )
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("agentActivity.event.\(event.id)")
    }

    // MARK: Approvals (data gap)

    private var approvalsGap: some View {
        RemContentUnavailableView(
            symbol: "checkmark.shield",
            title: AgentActivityDisplay.approvalsGapTitle,
            message: AgentActivityDisplay.approvalsGapMessage
        )
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("agentActivity.approvals.gap")
    }
}

#if DEBUG
private struct AgentActivityScreenPreview: View {
    @State private var tab = AgentActivityTab.activity
    var body: some View {
        NavigationStack {
            AgentActivityScreen(AgentActivityFixture(header: ChatHeaderDisplay(activity: "Connected")).display, selection: $tab)
        }
    }
}

#Preview("AgentActivityScreen") {
    AgentActivityScreenPreview()
}
#endif
