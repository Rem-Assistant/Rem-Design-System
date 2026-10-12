import Foundation

// MARK: - Agent activity fixture (presentation-only)
//
// The deterministic, in-memory host behind the Playground's Agent activity screen, opened from the Chat
// header identity. The current state is taken from the Chat header that was tapped, so both surfaces
// show the same agent state; the timeline is neutral sample history, not anyone's real work (the
// Figma master's sample rows are replaced). Nothing is fetched, stored or routed. Approvals stay a
// labeled data gap (`AgentActivityDisplay.approvalsGap…`).
//
// Compose twin: `screens/AgentActivityFixture.kt`.

public struct AgentActivityFixture: Equatable, Sendable {
    /// Neutral sample history, newest first within each day. Times sit around the Chat fixture's
    /// illustrative 10:20–10:24 conversation.
    public static let days: [AgentActivityDay] = [
        AgentActivityDay(id: "today", title: "Today", events: [
            AgentActivityEvent(id: "reminder", title: "Prepare a reminder", outcome: "Outlined the details", time: "10:24 AM"),
            AgentActivityEvent(id: "plan-day", title: "Plan the rest of the day", outcome: "Suggested an order", time: "10:21 AM"),
        ]),
        AgentActivityDay(id: "yesterday", title: "Yesterday", events: [
            AgentActivityEvent(id: "shared-notes", title: "Summarize shared notes", outcome: "Shared a summary", time: "4:10 PM"),
            AgentActivityEvent(id: "open-tasks", title: "Review open tasks", outcome: "Listed next steps", time: "3:42 PM"),
        ]),
    ]

    public private(set) var display: AgentActivityDisplay
    public private(set) var selectedTab: AgentActivityTab

    public init(current: AgentActivityCurrent, days: [AgentActivityDay] = AgentActivityFixture.days, selectedTab: AgentActivityTab = .activity) {
        display = AgentActivityDisplay(current: current, days: days)
        self.selectedTab = selectedTab
    }

    /// The screen for the Chat header the person tapped: same current state, fixture history.
    public init(header: ChatHeaderDisplay) {
        self.init(current: AgentActivityCurrent(header))
    }

    /// Switch the segmented control. Only the visible section changes.
    public mutating func select(_ tab: AgentActivityTab) {
        selectedTab = tab
    }
}
