import Foundation
import CoreGraphics

// MARK: - Sidebar inputs (presentation-only)
//
// Figma frames **A2 · Sidebar open (Claude-style)** (`2702:2479`) and **A2 · Chat (sidebar closed)**
// (`2702:2351`), account pill `2709:2648`. Approved decisions (Samuel):
//
// - **Search is the only top action.** No destination rows above the agents.
// - Two sections: **Main agent** (Rem, always present) and **Side agents**; every row shows the
//   agent name and its lane subtitle.
// - **New agent** is the one creation entry.
// - The **account pill** ("S · Samuel": initial + name) at the bottom *is* the Settings entry — there
//   is no separate gear.
//
// The DS renders what the host reports and emits typed actions; it never reads, stores or routes
// conversations. Selection is the host's current conversation identity, never inferred from order.
// Contract: `docs/contracts/sidebar-new-agent.md`.
//
// Compose twin: `navigation/RemSidebarModel.kt`. Tests: `RemSidebarModelTests.swift` / `RemSidebarModelTest.kt`.

/// One agent row: the agent's name and its lane subtitle.
public struct RemSidebarAgent: Identifiable, Equatable, Sendable {
    public var id: String
    public var name: String
    /// The lane subtitle, e.g. "Appointments & meds" (Main agent: host status such as "Daily Brief ready").
    public var lane: String
    /// Unread activity in this agent's chat (trailing dot).
    public var hasUnread: Bool

    public init(id: String, name: String, lane: String, hasUnread: Bool = false) {
        self.id = id
        self.name = name
        self.lane = lane
        self.hasUnread = hasUnread
    }
}

/// The signed-in account shown in the bottom pill.
public struct RemSidebarAccount: Equatable, Sendable {
    public var name: String

    public init(name: String) {
        self.name = name
    }

    /// The trimmed display name.
    public var displayName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    /// The pill's initial: the first character of the display name, uppercased (`"?"` when blank).
    public var initial: String {
        guard let first = displayName.first else { return "?" }
        return String(first).uppercased()
    }
}

/// Which conversation the host currently shows. `nil` in the display means none is highlighted.
public enum RemSidebarSelection: Hashable, Sendable {
    case main
    case side(id: String)
}

/// Everything the sidebar renders.
public struct RemSidebarDisplay: Equatable, Sendable {
    public var mainAgent: RemSidebarAgent
    public var sideAgents: [RemSidebarAgent]
    public var account: RemSidebarAccount
    public var selection: RemSidebarSelection?

    public init(
        mainAgent: RemSidebarAgent,
        sideAgents: [RemSidebarAgent],
        account: RemSidebarAccount,
        selection: RemSidebarSelection? = .main
    ) {
        self.mainAgent = mainAgent
        self.sideAgents = sideAgents
        self.account = account
        self.selection = selection
    }

    /// Whether `selection` names the Main agent.
    public var isMainSelected: Bool { selection == .main }

    /// Whether `selection` names this side agent. A selection naming an id that is not in
    /// `sideAgents` highlights nothing (a stale id never falls back to another row).
    public func isSelected(_ agent: RemSidebarAgent) -> Bool { selection == .side(id: agent.id) }

    /// The Side agents section only draws when there is at least one side agent — never a lone header.
    public var showsSideAgents: Bool { !sideAgents.isEmpty }

    public static let title = "Rem"
    public static let mainAgentHeader = "Main agent"
    public static let sideAgentsHeader = "Side agents"
    public static let newAgent = "New agent"
    public static let search = "Search"
    /// VoiceOver hint on the account pill — it is the Settings entry.
    public static let accountHint = "Opens Settings"
}

/// What the person asked the sidebar to do.
public enum RemSidebarAction: Equatable, Sendable {
    case search
    case selectMain
    case selectSide(id: String)
    case newAgent
    /// The account pill — opens Settings.
    case openSettings
}

/// Geometry for the Claude-style push (`2702:2479`): opening slides the chat right and leaves a
/// rounded card of it visible (tap to close). Pure so both platforms agree and it is testable.
public enum RemSidebarLayout {
    /// How much of the pushed chat stays visible (402pt frame − 332pt sidebar).
    public static let visibleContentPeek: CGFloat = 70
    /// The sidebar never grows past the Figma width on wide containers.
    public static let maximumSidebarWidth: CGFloat = 332
    /// Corner radius of the pushed chat card.
    public static let contentCornerRadius: CGFloat = 44

    /// Sidebar width for a container: leaves the peek visible, capped at the Figma width.
    public static func sidebarWidth(containerWidth: CGFloat) -> CGFloat {
        max(0, min(maximumSidebarWidth, containerWidth - visibleContentPeek))
    }

    /// Horizontal offset of the chat content.
    public static func contentOffset(isOpen: Bool, containerWidth: CGFloat) -> CGFloat {
        isOpen ? sidebarWidth(containerWidth: containerWidth) : 0
    }
}
