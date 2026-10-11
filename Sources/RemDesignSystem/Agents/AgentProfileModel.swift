import Foundation

// MARK: - Agent profile inputs (presentation-only)
//
// Figma frames **E3 · Agent profile — Health · About** (`2718:3055`) and **E3b · Agent profile — Rem ·
// Activity** (`2718:3396`). Tapping an agent in its chat header opens *Agent activity* — now for any
// agent — with Activity / Approvals / **About** tabs. This file covers what the existing Agent activity
// screen does not: the per-agent identity header and the **About** tab.
//
// Reconciliation (not done here): `AgentActivityScreen` and its `AgentActivityTab` live on PR #106,
// not on main. When both land, `AgentActivityTab` gains `.about`, its identity block renders
// `AgentProfileHeader`, and the About case renders `AgentProfileAbout`. See
// `docs/contracts/sidebar-new-agent.md` § Reconciliation.
//
// Compose twin: `agents/AgentProfileModel.kt`.

/// Everything the profile header and About tab render for one agent.
public struct AgentProfileDisplay: Equatable, Sendable {
    public var kind: AgentKind
    public var name: String
    /// Side agents only; the Main agent always renders the Rem face.
    public var look: AgentLook
    /// Host copy for the lane, shown after the kind in the status line, e.g. "Appointments and refills"
    /// or, for Rem, "Daily Brief, sees all your lanes".
    public var laneSummary: String
    /// "What should this agent handle?" as saved, shown in the About tab's **Handles** row.
    public var handles: String
    public var services: [AgentService]

    public init(
        kind: AgentKind,
        name: String,
        look: AgentLook = .default,
        laneSummary: String,
        handles: String,
        services: [AgentService] = []
    ) {
        self.kind = kind
        self.name = name
        self.look = look
        self.laneSummary = laneSummary
        self.handles = handles
        self.services = services
    }

    /// The avatar identity for this agent.
    public var avatar: AgentAvatar.Identity { kind == .main ? .main : .side(look) }

    /// The line under the name: "Side agent · Appointments and refills".
    public var statusLine: String {
        let lane = laneSummary.trimmingCharacters(in: .whitespacesAndNewlines)
        return lane.isEmpty ? kind.label : "\(kind.label) · \(lane)"
    }

    /// Rem can't be paused or deleted; only side agents show Pause agent / Delete agent.
    public var showsLifecycleActions: Bool { kind == .side }

    /// Only a side agent's lane (name / handles) is editable from About.
    public var isLaneEditable: Bool { kind == .side }
}

/// What the person asked the About tab to do. The host performs every one of these — including
/// confirming before `.delete`.
public enum AgentProfileAction: Equatable, Sendable {
    case editName
    case editHandles
    case openService(id: String)
    case addService
    case pause
    case delete
}

/// Approved copy for the About tab (Figma `2718:3055`, verbatim).
public enum AgentProfileCopy {
    public static let laneHeader = "Lane"
    public static let nameRow = "Name"
    public static let handlesRow = "Handles"
    public static let pause = "Pause agent"
    public static let delete = "Delete agent"
}
