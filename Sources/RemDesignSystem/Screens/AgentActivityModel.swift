import Foundation

// MARK: - Agent activity screen inputs (presentation-only)
//
// Figma master `Rem/Chat/Agent activity` (`2002:76914`), reached from the agent identity in the Chat
// header (`ChatScreenAction.activityDetails`). The screen shows **who the agent is and what it is doing
// now**, then browsable history. Two inputs, deliberately separate:
//
// - `current` is the agent-level state the host reports right now — the same value the Chat header
//   shows. It is never derived from the timeline: a historical event cannot overwrite it.
// - `days` is host-formatted history (day labels and times are host strings; the DS never reads a clock
//   or formats dates). Rows summarize human-facing work; execution traces belong in task detail.
//
// This screen is not agent settings, a card outcome or the voice session state. Approvals have no
// verified design (`2002:76915` only repeats timeline rows), so the Approvals tab presents a labeled
// data gap instead of invented approvals. Rows have no verified destination and are not interactive.
//
// Compose twin: `screens/AgentActivityModel.kt`. Tests: `AgentActivityFixtureTests.swift` /
// `AgentActivityFixtureTest.kt`.

/// The agent's **current** state, as the host reports it now (identity line under the face).
public struct AgentActivityCurrent: Equatable, Sendable {
    public var name: String
    /// Host copy for what the agent is doing now, e.g. "Connected".
    public var activity: String
    public var status: ChatHeader.Status
    /// The face shows the thinking mode while the host reports the agent as working.
    public var isWorking: Bool

    public init(name: String = "Rem", activity: String, status: ChatHeader.Status = .connected, isWorking: Bool = false) {
        self.name = name
        self.activity = activity
        self.status = status
        self.isWorking = isWorking
    }

    /// The same current state the Chat header renders, so the two surfaces never disagree.
    public init(_ header: ChatHeaderDisplay) {
        self.init(name: header.name, activity: header.activity, status: header.status, isWorking: header.isWorking)
    }
}

/// One completed piece of work in the timeline: an action title and its outcome at a host time.
/// Carries no status — history never speaks for the agent's current state.
public struct AgentActivityEvent: Identifiable, Equatable, Sendable {
    public var id: String
    /// The action, e.g. "Review open tasks".
    public var title: String
    /// What came of it, e.g. "Listed next steps".
    public var outcome: String
    /// Host-formatted time, e.g. "3:42 PM".
    public var time: String

    public init(id: String, title: String, outcome: String, time: String) {
        self.id = id
        self.title = title
        self.outcome = outcome
        self.time = time
    }

    /// The row subtitle: "Outcome · time".
    public var summary: String { "\(outcome) · \(time)" }
}

/// A day group in the timeline, e.g. "Today" / "Yesterday" (host-formatted label).
public struct AgentActivityDay: Identifiable, Equatable, Sendable {
    public var id: String
    public var title: String
    public var events: [AgentActivityEvent]

    public init(id: String, title: String, events: [AgentActivityEvent]) {
        self.id = id
        self.title = title
        self.events = events
    }
}

/// The Activity / Approvals segmented control.
public enum AgentActivityTab: String, CaseIterable, Identifiable, Sendable {
    case activity = "Activity"
    case approvals = "Approvals"

    public var id: String { rawValue }
    public var title: String { rawValue }
}

/// Everything the Agent activity screen renders.
public struct AgentActivityDisplay: Equatable, Sendable {
    public var current: AgentActivityCurrent
    public var days: [AgentActivityDay]

    public init(current: AgentActivityCurrent, days: [AgentActivityDay]) {
        self.current = current
        self.days = days
    }

    /// Day groups that have rows; an empty day never draws a lone header.
    public var visibleDays: [AgentActivityDay] { days.filter { !$0.events.isEmpty } }

    public static let title = "Agent activity"
    /// The screen Back returns to.
    public static let backTitle = "Chat"
    /// Approvals have no verified design or data: say so instead of implying there are none.
    public static let approvalsGapTitle = "Approvals not shown yet"
    public static let approvalsGapMessage =
        "Approval details aren't available on this screen yet. This doesn't mean nothing needs your approval."
    public static let activityEmptyTitle = "No activity yet"
    public static let activityEmptyMessage = "Work Rem finishes for you will appear here."
}
