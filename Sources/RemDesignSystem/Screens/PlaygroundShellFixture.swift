import Foundation

// MARK: - Playground app shell (Build 9, presentation-only)
//
// One full-screen root conversation with the sidebar (`2702:2479`) behind the chat header's sidebar
// button (`2702:2351`); New agent (name + colour) and Settings (the account pill) are presented over it.
// This value is the single source of truth for the shell's selection, sidebar and presentation; the
// hosts only render it and draw the native chrome.
//
// It is NOT an app adapter: no agent is created anywhere but in this value, nothing is persisted, and no
// backend, auth, voice or provider is touched. Compose twin: `screens/PlaygroundShellFixture.kt`. Tests:
// `PlaygroundShellFixtureTests.swift` / `PlaygroundShellFixtureTest.kt`. Route map:
// `docs/playground/route-map.md`.

/// What the shell presents over the root conversation. At most one at a time.
public enum PlaygroundShellPresentation: String, Identifiable, Sendable {
    case newAgent
    case settings

    public var id: String { rawValue }
}

public struct PlaygroundShellFixture: Equatable, Sendable {
    /// The Settings profile's name, so the account pill and Settings agree.
    public static let defaultAccount = RemSidebarAccount(name: "Avery Diaz")
    public static let defaultMainAgent = RemSidebarAgent(id: "rem", name: "Rem", lane: "Daily Brief ready")
    /// Lane subtitle of an agent created here (setup asks only for a name and a colour).
    public static let newAgentLane = "New agent"
    public static let seedSideAgents = [
        RemSidebarAgent(id: "health", name: "Health", lane: "Appointments & meds"),
        RemSidebarAgent(id: "money", name: "Money", lane: "Bills & budget", hasUnread: true),
        RemSidebarAgent(id: "travel", name: "Travel planning: Lisbon in May", lane: "Flights, stays"),
    ]

    public private(set) var mainAgent: RemSidebarAgent
    public private(set) var sideAgents: [RemSidebarAgent]
    public private(set) var account: RemSidebarAccount
    /// The conversation on screen. Never inferred from list order; always a listed agent.
    public private(set) var selection: RemSidebarSelection = .main
    public var isSidebarOpen = false
    public private(set) var presented: PlaygroundShellPresentation?
    /// The New agent sheet's host-owned draft; reset each time the sheet opens.
    public var draft = NewAgentDraft()
    private var createdCount = 0

    public init(
        mainAgent: RemSidebarAgent = Self.defaultMainAgent,
        sideAgents: [RemSidebarAgent] = Self.seedSideAgents,
        account: RemSidebarAccount = Self.defaultAccount
    ) {
        self.mainAgent = mainAgent
        self.sideAgents = sideAgents
        self.account = account
    }

    public var sidebar: RemSidebarDisplay {
        RemSidebarDisplay(mainAgent: mainAgent, sideAgents: sideAgents, account: account, selection: selection)
    }

    /// The agent whose conversation is on screen.
    public var selectedAgent: RemSidebarAgent {
        switch selection {
        case .main: return mainAgent
        case .side(let id): return sideAgents.first { $0.id == id } ?? mainAgent
        }
    }

    /// A stable key for the on-screen conversation: switching agents replaces it, nothing else does.
    public var conversationKey: String {
        switch selection {
        case .main: return "main"
        case .side(let id): return "side.\(id)"
        }
    }

    /// The header sidebar button.
    public mutating func openSidebar() { isSidebarOpen = true }

    /// Tap-to-close on the pushed chat card.
    public mutating func closeSidebar() { isSidebarOpen = false }

    /// Sidebar actions. Choosing an agent shows its conversation and closes the sidebar (choosing the one
    /// already shown keeps it, with its scroll position). New agent and the account pill close the sidebar
    /// and present their surface over the unchanged conversation. Search has no approved surface yet
    /// (`sidebar-new-agent.md` non-goal), so it changes nothing.
    public mutating func handle(_ action: RemSidebarAction) {
        switch action {
        case .selectMain:
            selection = .main
            isSidebarOpen = false
        case .selectSide(let id):
            guard sideAgents.contains(where: { $0.id == id }) else { return }
            selection = .side(id: id)
            isSidebarOpen = false
        case .newAgent:
            isSidebarOpen = false
            draft = NewAgentDraft()
            presented = .newAgent
        case .openSettings:
            isSidebarOpen = false
            presented = .settings
        case .search:
            break
        }
    }

    /// New agent: Cancel dismisses; Create adds the agent and opens its chat.
    public mutating func handle(_ action: NewAgentSetupAction) {
        guard presented == .newAgent else { return }
        switch action {
        case .cancel:
            presented = nil
            draft = NewAgentDraft()
        case .create(let created):
            guard created.canCreate else { return }
            createdCount += 1
            let id = "new.\(createdCount)"
            sideAgents.append(RemSidebarAgent(id: id, name: created.trimmedName, lane: Self.newAgentLane))
            selection = .side(id: id)
            presented = nil
            draft = NewAgentDraft()
        }
    }

    /// Settings' Done, or a sheet dismissed by a swipe: back to the same conversation.
    public mutating func dismissPresentation() { presented = nil }
}
