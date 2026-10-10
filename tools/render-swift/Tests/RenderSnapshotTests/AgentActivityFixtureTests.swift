import XCTest
@testable import RemDesignSystem

/// Agent activity screen inputs and fixture (`AgentActivityModel.swift` / `AgentActivityFixture.swift`).
/// Paired with `AgentActivityFixtureTest.kt`. Proves the current state comes only from the host's
/// current value (never a timeline event), the history is neutral and well-formed, and Approvals stays
/// a labeled data gap. The native journey is covered by the Playground UI tests.
final class AgentActivityFixtureTests: XCTestCase {
    func testCurrentStateMirrorsTheChatHeader() {
        let header = ChatHeaderDisplay(activity: "Reading the shared notes", status: .needsYou, isWorking: true)
        let current = AgentActivityCurrent(header)
        XCTAssertEqual(current, AgentActivityCurrent(name: "Rem", activity: "Reading the shared notes", status: .needsYou, isWorking: true))
        XCTAssertEqual(AgentActivityFixture(header: header).display.current, current)
    }

    func testHistoryNeverOverwritesTheCurrentState() {
        let current = AgentActivityCurrent(activity: "Connected")
        let newest = AgentActivityEvent(id: "new", title: "Send the weekly summary", outcome: "Needs you", time: "11:00 AM")
        var days = AgentActivityFixture.days
        days[0].events.insert(newest, at: 0)
        var fixture = AgentActivityFixture(current: current, days: days)
        XCTAssertEqual(fixture.display.current, current, "A newer event does not become the current activity")
        for tab in AgentActivityTab.allCases {
            fixture.select(tab)
            XCTAssertEqual(fixture.display.current, current, "Switching sections keeps the current state")
        }
        XCTAssertEqual(fixture.display.days, days, "Selecting a tab never rewrites history")
    }

    func testFixtureHistoryIsNeutralDayGroupedAndComplete() {
        let days = AgentActivityFixture.days
        XCTAssertEqual(days.map(\.title), ["Today", "Yesterday"])
        let events = days.flatMap(\.events)
        XCTAssertEqual(Set(events.map(\.id)).count, events.count, "Unique row ids")
        for event in events {
            XCTAssertFalse(event.title.isEmpty)
            XCTAssertFalse(event.outcome.isEmpty)
            XCTAssertFalse(event.time.isEmpty)
            XCTAssertEqual(event.summary, "\(event.outcome) · \(event.time)")
        }
        // The Figma master's personal sample copy is not reproduced.
        let copy = events.flatMap { [$0.title, $0.outcome] }.joined(separator: " ")
        for sample in ["RFE", "Gmail", "Build RFE checklist", "Prepare next steps", "Check project status"] {
            XCTAssertFalse(copy.contains(sample), sample)
        }
    }

    func testTabsDefaultToActivityAndMatchTheSegmentedControl() {
        XCTAssertEqual(AgentActivityTab.allCases.map(\.title), ["Activity", "Approvals"])
        var fixture = AgentActivityFixture(current: AgentActivityCurrent(activity: "Connected"))
        XCTAssertEqual(fixture.selectedTab, .activity)
        fixture.select(.approvals)
        XCTAssertEqual(fixture.selectedTab, .approvals)
    }

    func testApprovalsIsALabeledDataGapNotAnEmptyList() {
        XCTAssertFalse(AgentActivityDisplay.approvalsGapTitle.isEmpty)
        XCTAssertTrue(AgentActivityDisplay.approvalsGapMessage.contains("doesn't mean nothing needs your approval"))
    }

    func testEmptyDaysDrawNoHeaders() {
        let display = AgentActivityDisplay(current: AgentActivityCurrent(activity: "Connected"), days: [
            AgentActivityDay(id: "today", title: "Today", events: []),
            AgentActivityFixture.days[1],
        ])
        XCTAssertEqual(display.visibleDays.map(\.id), ["yesterday"])
        XCTAssertTrue(AgentActivityDisplay(current: display.current, days: []).visibleDays.isEmpty)
    }

    func testScreenTitleAndBackDestination() {
        XCTAssertEqual(AgentActivityDisplay.title, "Agent activity")
        XCTAssertEqual(AgentActivityDisplay.backTitle, "Chat")
    }
}
