import SwiftUI

/// **RemSidebar** — the app's navigation drawer: "Rem" title with **Search** (the only top action),
/// a **Main agent** section (Rem), a **Side agents** section (one row per agent with its lane
/// subtitle), and floating bottom actions: the **account pill** (opens Settings) and **New agent**.
/// The agent list scrolls under the bottom actions.
///
/// Figma: sidebar column `2702:2514` + bottom actions `2702:2682` in **A2 · Sidebar open
/// (Claude-style)** (`2702:2479`); account pill `2709:2648`. Host it in `RemSidebarContainer` for the
/// push-open behaviour. Display + callbacks only. Compose sibling: `navigation/RemSidebar.kt`.
public struct RemSidebar: View {
    private let display: RemSidebarDisplay
    private let onAction: (RemSidebarAction) -> Void

    public init(_ display: RemSidebarDisplay, onAction: @escaping (RemSidebarAction) -> Void) {
        self.display = display
        self.onAction = onAction
    }

    public var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    section(RemSidebarDisplay.mainAgentHeader, id: "main") {
                        SidebarAgentRow(
                            agent: display.mainAgent,
                            isMain: true,
                            isSelected: display.isMainSelected,
                            action: { onAction(.selectMain) }
                        )
                        .accessibilityIdentifier("sidebar.agent.main")
                    }
                    if display.showsSideAgents {
                        section(RemSidebarDisplay.sideAgentsHeader, id: "side") {
                            ForEach(display.sideAgents) { agent in
                                SidebarAgentRow(
                                    agent: agent,
                                    isMain: false,
                                    isSelected: display.isSelected(agent),
                                    action: { onAction(.selectSide(id: agent.id)) }
                                )
                                .accessibilityIdentifier("sidebar.agent.\(agent.id)")
                            }
                        }
                    }
                }
                .padding(.top, DesignTokens.Spacing.md)
                // Room for the floating bottom actions so the last row can scroll clear of them.
                .padding(.bottom, 128)
            }
            .scrollIndicators(.hidden)
        }
        .padding(.leading, DesignTokens.Spacing.lg)
        .padding(.trailing, DesignTokens.Spacing.md)
        .overlay(alignment: .bottom) { bottomActions }
        .background(DesignTokens.Color.backgroundSecondary.ignoresSafeArea())
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("sidebar")
    }

    // MARK: Header — title + Search (only)

    private var header: some View {
        HStack {
            Text(RemSidebarDisplay.title)
                .font(DesignTokens.Typography.title1Bold)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: DesignTokens.Spacing.sm)
            Button { onAction(.search) } label: {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 19, weight: .medium))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .frame(width: 44, height: 44)
                    .background(DesignTokens.Color.backgroundPrimary, in: Circle())
                    .shadow(color: .black.opacity(0.12), radius: 20, y: 8)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(RemSidebarDisplay.search)
            .accessibilityIdentifier("sidebar.search")
        }
        .padding(.leading, DesignTokens.Spacing.xs)
        .padding(.vertical, 6)
        .padding(.top, DesignTokens.Spacing.sm)
    }

    private func section<Rows: View>(_ title: String, id: String, @ViewBuilder rows: () -> Rows) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            AgentSectionHeader(title: title)
            rows()
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("sidebar.section.\(id)")
    }

    // MARK: Bottom actions — account pill (Settings) + New agent

    private var bottomActions: some View {
        HStack {
            RemAccountPill(display.account) { onAction(.openSettings) }
            Spacer(minLength: DesignTokens.Spacing.sm)
            Button { onAction(.newAgent) } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                        .font(.system(size: 20, weight: .semibold))
                        .frame(width: 28, height: 28)
                    Text(RemSidebarDisplay.newAgent)
                        .font(DesignTokens.Typography.bodyBold)
                }
                .foregroundStyle(DesignTokens.Color.backgroundPrimary)
                .padding(.leading, 20)
                .padding(.trailing, DesignTokens.Spacing.xl)
                .frame(height: 56)
                .background(DesignTokens.Color.labelPrimary, in: Capsule())
                .shadow(color: .black.opacity(0.18), radius: 9, y: 6)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("sidebar.newAgent")
        }
        .padding(.leading, DesignTokens.Spacing.lg)
        .padding(.trailing, DesignTokens.Spacing.md)
        .padding(.top, 32)
        .padding(.bottom, DesignTokens.Spacing.sm)
        .background(
            LinearGradient(
                stops: [
                    .init(color: DesignTokens.Color.backgroundSecondary.opacity(0), location: 0),
                    .init(color: DesignTokens.Color.backgroundSecondary, location: 0.45),
                    .init(color: DesignTokens.Color.backgroundSecondary, location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .bottom)
            .allowsHitTesting(false)
        )
    }
}

/// One sidebar agent row (`2702:2560` Rem, `2702:2581` Health …): leading mark, name, lane subtitle,
/// optional unread dot. Selected rows get the `fillTertiary` rounded highlight; the Main agent's name
/// is emphasized (semibold). The row is one button.
struct SidebarAgentRow: View {
    let agent: RemSidebarAgent
    let isMain: Bool
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DesignTokens.Spacing.md) {
                leading
                    .frame(width: 29, height: 29)
                VStack(alignment: .leading, spacing: 0) {
                    Text(agent.name)
                        .font(isMain ? DesignTokens.Typography.body.weight(.semibold) : DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    if !agent.lane.isEmpty {
                        Text(agent.lane)
                            .font(DesignTokens.Typography.footnote)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .lineLimit(1)
                    }
                }
                Spacer(minLength: DesignTokens.Spacing.sm)
                if agent.hasUnread {
                    Circle()
                        .fill(DesignTokens.Color.brandBlue)
                        .frame(width: 8, height: 8)
                        .accessibilityLabel("Unread")
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.vertical, 14)
            .frame(minHeight: 60)
            .background(
                RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                    .fill(isSelected ? DesignTokens.Color.fillTertiary : Color.clear)
            )
            .contentShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : [.isButton])
    }

    @ViewBuilder private var leading: some View {
        if isMain {
            RemFaceMark(mode: .idle, tint: DesignTokens.Color.brandBlue, size: 24)
                .accessibilityHidden(true)
        } else {
            Image(systemName: "bubble.left")
                .font(.system(size: 17, weight: .regular))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .accessibilityHidden(true)
        }
    }
}

#if DEBUG
#Preview("RemSidebar") {
    RemSidebar(
        RemSidebarDisplay(
            mainAgent: .init(id: "rem", name: "Rem", lane: "Daily Brief ready"),
            sideAgents: [
                .init(id: "health", name: "Health", lane: "Appointments & meds"),
                .init(id: "money", name: "Money", lane: "Bills & budget", hasUnread: true),
            ],
            account: .init(name: "Samuel")
        )
    ) { _ in }
    .frame(width: 332)
}
#endif
