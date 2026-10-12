import XCTest
import RemDesignSystem

/// Display-model contract for `RemSidebar` (`docs/contracts/sidebar-new-agent.md`). Twin:
/// `RemSidebarModelTest.kt`.
final class RemSidebarModelTests: XCTestCase {
    private let main = RemSidebarAgent(id: "rem", name: "Rem", lane: "Daily Brief ready")
    private let health = RemSidebarAgent(id: "health", name: "Health", lane: "Appointments & meds")
    private let money = RemSidebarAgent(id: "money", name: "Money", lane: "Bills & budget", hasUnread: true)

    func testApprovedCopyIsExact() {
        XCTAssertEqual(RemSidebarDisplay.title, "Rem")
        XCTAssertEqual(RemSidebarDisplay.mainAgentHeader, "Main agent")
        XCTAssertEqual(RemSidebarDisplay.sideAgentsHeader, "Side agents")
        XCTAssertEqual(RemSidebarDisplay.newAgent, "New agent")
        XCTAssertEqual(RemSidebarDisplay.search, "Search")
        XCTAssertEqual(RemSidebarDisplay.accountHint, "Opens Settings")
    }

    func testSelectionHighlightsExactlyTheNamedConversation() {
        var display = RemSidebarDisplay(mainAgent: main, sideAgents: [health, money], account: .init(name: "Samuel"))
        XCTAssertTrue(display.isMainSelected, "Main agent is the default selection")
        XCTAssertFalse(display.isSelected(health))

        display.selection = .side(id: "money")
        XCTAssertFalse(display.isMainSelected)
        XCTAssertFalse(display.isSelected(health))
        XCTAssertTrue(display.isSelected(money))
    }

    func testStaleOrMissingSelectionHighlightsNothing() {
        var display = RemSidebarDisplay(mainAgent: main, sideAgents: [health], account: .init(name: "Samuel"),
                                        selection: .side(id: "deleted"))
        XCTAssertFalse(display.isMainSelected)
        XCTAssertFalse(display.isSelected(health))

        display.selection = nil
        XCTAssertFalse(display.isMainSelected)
        XCTAssertFalse(display.isSelected(health))
    }

    func testSideAgentsSectionNeverDrawsALoneHeader() {
        XCTAssertFalse(RemSidebarDisplay(mainAgent: main, sideAgents: [], account: .init(name: "S")).showsSideAgents)
        XCTAssertTrue(RemSidebarDisplay(mainAgent: main, sideAgents: [health], account: .init(name: "S")).showsSideAgents)
    }

    func testAccountPillInitialAndName() {
        XCTAssertEqual(RemSidebarAccount(name: "Samuel").initial, "S")
        XCTAssertEqual(RemSidebarAccount(name: "  samuel  ").initial, "S")
        XCTAssertEqual(RemSidebarAccount(name: "  samuel  ").displayName, "samuel")
        XCTAssertEqual(RemSidebarAccount(name: "   ").initial, "?")
        XCTAssertEqual(RemSidebarAccount(name: "Élodie").initial, "É")
    }

    func testPushGeometryLeavesThePeekVisibleAndCapsWidth() {
        // The Figma frame: 402pt wide, 332pt sidebar, 70pt of chat visible.
        XCTAssertEqual(RemSidebarLayout.sidebarWidth(containerWidth: 402), 332)
        XCTAssertEqual(RemSidebarLayout.contentOffset(isOpen: true, containerWidth: 402), 332)
        XCTAssertEqual(RemSidebarLayout.contentOffset(isOpen: false, containerWidth: 402), 0)
        // Narrower phones keep the 70pt peek; wide containers cap at the Figma width.
        XCTAssertEqual(RemSidebarLayout.sidebarWidth(containerWidth: 375), 305)
        XCTAssertEqual(RemSidebarLayout.sidebarWidth(containerWidth: 1024), 332)
        XCTAssertEqual(RemSidebarLayout.sidebarWidth(containerWidth: 40), 0)
    }

    func testActionsAreTyped() {
        XCTAssertEqual(RemSidebarAction.selectSide(id: "health"), .selectSide(id: "health"))
        XCTAssertNotEqual(RemSidebarAction.selectSide(id: "health"), .selectSide(id: "money"))
        XCTAssertNotEqual(RemSidebarAction.openSettings, .newAgent)
    }
}
