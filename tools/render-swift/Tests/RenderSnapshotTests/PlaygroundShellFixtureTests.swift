import XCTest
import RemDesignSystem

/// The Playground shell's state contract (`docs/playground/route-map.md`). Twin:
/// `PlaygroundShellFixtureTest.kt`.
final class PlaygroundShellFixtureTests: XCTestCase {
    func testOpensOnTheMainAgentWithTheSidebarClosed() {
        let shell = PlaygroundShellFixture()
        XCTAssertEqual(shell.selection, .main)
        XCTAssertFalse(shell.isSidebarOpen)
        XCTAssertNil(shell.presented)
        XCTAssertEqual(shell.selectedAgent.name, "Rem")
        XCTAssertEqual(shell.sidebar.account.displayName, "Avery Diaz", "The account pill names the Settings profile")
        XCTAssertTrue(shell.sidebar.isMainSelected)
    }

    func testSelectingAnAgentShowsItsConversationAndClosesTheSidebar() {
        var shell = PlaygroundShellFixture()
        shell.openSidebar()
        shell.handle(.selectSide(id: "health"))
        XCTAssertEqual(shell.selection, .side(id: "health"))
        XCTAssertFalse(shell.isSidebarOpen)
        XCTAssertEqual(shell.selectedAgent.name, "Health")
        XCTAssertEqual(shell.conversationKey, "side.health")
        shell.openSidebar()
        shell.handle(.selectMain)
        XCTAssertEqual(shell.selection, .main)
        XCTAssertEqual(shell.conversationKey, "main")
    }

    func testClosingTheSidebarKeepsTheSelection() {
        var shell = PlaygroundShellFixture()
        shell.handle(.selectSide(id: "money"))
        let before = shell
        shell.openSidebar()
        shell.closeSidebar()
        XCTAssertEqual(shell, before)
    }

    func testAnUnknownAgentIsNeverSelected() {
        var shell = PlaygroundShellFixture()
        let before = shell
        shell.handle(.selectSide(id: "missing"))
        XCTAssertEqual(shell, before)
    }

    func testSearchHasNoSurfaceYet() {
        var shell = PlaygroundShellFixture()
        shell.openSidebar()
        let before = shell
        shell.handle(.search)
        XCTAssertEqual(shell, before)
    }

    func testSettingsIsPresentedOverTheSameConversationAndDismissesBackToIt() {
        var shell = PlaygroundShellFixture()
        shell.handle(.selectSide(id: "travel"))
        let before = shell
        shell.openSidebar()
        shell.handle(.openSettings)
        XCTAssertEqual(shell.presented, .settings)
        XCTAssertFalse(shell.isSidebarOpen, "The sidebar closes behind Settings")
        shell.dismissPresentation()
        XCTAssertEqual(shell.selection, .side(id: "travel"), "Selection kept")
        XCTAssertEqual(shell, before)
    }

    func testNewAgentCancelChangesNothing() {
        var shell = PlaygroundShellFixture()
        shell.openSidebar()
        shell.handle(.newAgent)
        XCTAssertEqual(shell.presented, .newAgent)
        XCTAssertEqual(shell.draft, NewAgentDraft())
        shell.draft = NewAgentDraft(name: "Garden", color: .green)
        shell.handle(NewAgentSetupAction.cancel)
        XCTAssertNil(shell.presented)
        XCTAssertEqual(shell.sideAgents, PlaygroundShellFixture.seedSideAgents)
        XCTAssertEqual(shell.selection, .main)
        shell.handle(.newAgent)
        XCTAssertEqual(shell.draft, NewAgentDraft(), "The next sheet starts empty")
    }

    func testNewAgentCreateAddsTheAgentAndOpensItsChat() {
        var shell = PlaygroundShellFixture()
        shell.handle(.newAgent)
        shell.draft = NewAgentDraft(name: "  Garden ", color: .green)
        shell.handle(NewAgentSetupAction.create(NewAgentDraft(name: shell.draft.trimmedName, color: shell.draft.color)))
        XCTAssertNil(shell.presented)
        XCTAssertEqual(shell.sideAgents.last?.name, "Garden")
        XCTAssertEqual(shell.sideAgents.last?.lane, PlaygroundShellFixture.newAgentLane)
        XCTAssertEqual(shell.selection, .side(id: shell.sideAgents.last!.id))
        XCTAssertEqual(shell.selectedAgent.name, "Garden")
        shell.handle(.newAgent)
        shell.handle(NewAgentSetupAction.create(NewAgentDraft(name: "Garden")))
        XCTAssertEqual(Set(shell.sideAgents.suffix(2).map(\.id)).count, 2, "Ids stay unique")
    }

    func testCreateWithoutANameOrOutsideTheSheetIsIgnored() {
        var shell = PlaygroundShellFixture()
        shell.handle(.newAgent)
        let sheet = shell
        shell.handle(NewAgentSetupAction.create(NewAgentDraft(name: "   ")))
        XCTAssertEqual(shell, sheet)
        var closed = PlaygroundShellFixture()
        let before = closed
        closed.handle(NewAgentSetupAction.create(NewAgentDraft(name: "Garden")))
        XCTAssertEqual(closed, before)
    }
}
