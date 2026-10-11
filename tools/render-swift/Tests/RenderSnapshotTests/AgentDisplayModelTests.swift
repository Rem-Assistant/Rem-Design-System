import XCTest
import RemDesignSystem

/// Display-model contract for New agent setup + Agent profile (`docs/contracts/sidebar-new-agent.md`).
/// Twin: `AgentDisplayModelTest.kt`.
final class AgentDisplayModelTests: XCTestCase {

    // MARK: Shared identity

    func testKindLabelsAreTheApprovedCopy() {
        XCTAssertEqual(AgentKind.main.label, "Main agent")
        XCTAssertEqual(AgentKind.side.label, "Side agent")
    }

    func testPaletteIsElevenColoursInSpectrumOrderWithTheDefaultLook() {
        XCTAssertEqual(AgentColor.allCases, [.red, .orange, .yellow, .green, .mint, .teal, .cyan, .blue, .indigo, .purple, .pink])
        XCTAssertEqual(AgentColor.blue.accessibilityName, "Blue")
        XCTAssertEqual(AgentColor.allCases.map(\.id), AgentColor.allCases.map(\.rawValue))
        XCTAssertEqual(AgentLook.default, AgentLook(glyph: .heart, color: .pink))
        XCTAssertEqual(AgentGlyph.heart.systemName, "heart.fill")
        XCTAssertEqual(AgentServiceGlyph.calendar.systemName, "calendar")
        XCTAssertEqual(AgentServiceGlyph.link.systemName, "link")
    }

    func testSwatchesWrapIntoTwoRows() {
        XCTAssertEqual(NewAgentSetupLayout.swatchRows.map(\.count), [6, 5])
        XCTAssertEqual(NewAgentSetupLayout.swatchRows.flatMap { $0 }, AgentColor.allCases)
        XCTAssertGreaterThanOrEqual(NewAgentSetupLayout.swatchTarget, 44, "44pt touch target")
    }

    // MARK: New agent setup

    func testSetupCopyIsVerbatim() {
        XCTAssertEqual(NewAgentSetupCopy.title, "New agent")
        XCTAssertEqual(NewAgentSetupCopy.create, "Create agent")
        XCTAssertEqual(NewAgentSetupCopy.close, "Close")
        XCTAssertEqual(NewAgentSetupCopy.nameLabel, "Name")
        XCTAssertEqual(NewAgentSetupCopy.namePlaceholder, "Agent name")
        XCTAssertEqual(NewAgentSetupCopy.colorsLabel, "Color")
        XCTAssertEqual(AgentServicesCopy.header, "Connected services")
        XCTAssertEqual(AgentServicesCopy.addService, "Add a service")
    }

    func testDraftStartsEmptyOnTheDefaultColour() {
        let draft = NewAgentDraft()
        XCTAssertEqual(draft.name, "")
        XCTAssertEqual(draft.color, .pink)
        XCTAssertEqual(draft.color, AgentLook.default.color)
        XCTAssertEqual(draft.look, AgentLook.default)
    }

    func testCreateIsDisabledUntilTheAgentHasAName() {
        var draft = NewAgentDraft()
        XCTAssertFalse(draft.canCreate)
        XCTAssertNil(draft.createAction)

        draft.name = "   \n "
        XCTAssertFalse(draft.canCreate, "whitespace is not a name")
        XCTAssertNil(draft.createAction)

        draft.color = .teal
        XCTAssertFalse(draft.canCreate, "a colour alone is not enough")

        draft.name = "Health"
        XCTAssertTrue(draft.canCreate, "the colour is optional (it has a default)")
        XCTAssertNotNil(draft.createAction)
    }

    func testCreateSendsATrimmedName() {
        guard case .create(let sent)? = NewAgentDraft(name: "  Health \n").createAction else {
            return XCTFail("expected a create action")
        }
        XCTAssertEqual(sent.name, "Health")
    }

    func testCreateCarriesTheSelectedColour() {
        var draft = NewAgentDraft(name: "Money")
        draft.color = .indigo
        XCTAssertEqual(draft.look, AgentLook(glyph: .heart, color: .indigo), "the preview follows the selection")
        XCTAssertEqual(draft.createAction, .create(NewAgentDraft(name: "Money", color: .indigo)))
        guard case .create(let sent)? = draft.createAction else {
            return XCTFail("expected a create action")
        }
        XCTAssertEqual(sent.color, .indigo)
        XCTAssertEqual(sent.look, AgentLook(glyph: .heart, color: .indigo))
    }

    func testCreateWithoutPickingKeepsTheDefaultColour() {
        guard case .create(let sent)? = NewAgentDraft(name: "Travel").createAction else {
            return XCTFail("expected a create action")
        }
        XCTAssertEqual(sent.color, AgentLook.default.color)
    }

    // MARK: Agent profile

    private let health = AgentProfileDisplay(
        kind: .side, name: "Health", laneSummary: "Appointments and refills",
        handles: "Appointments, prescriptions, insurance claims"
    )
    private let rem = AgentProfileDisplay(
        kind: .main, name: "Rem", laneSummary: "Daily Brief, sees all your lanes",
        handles: "Daily Brief and anything outside a side agent's lane"
    )

    func testStatusLineJoinsKindAndLane() {
        XCTAssertEqual(health.statusLine, "Side agent · Appointments and refills")
        XCTAssertEqual(rem.statusLine, "Main agent · Daily Brief, sees all your lanes")
        var blank = health
        blank.laneSummary = "  "
        XCTAssertEqual(blank.statusLine, "Side agent")
    }

    func testRemCannotBePausedDeletedOrRenamed() {
        XCTAssertTrue(health.showsLifecycleActions)
        XCTAssertTrue(health.isLaneEditable)
        XCTAssertFalse(rem.showsLifecycleActions)
        XCTAssertFalse(rem.isLaneEditable)
    }

    func testAvatarIdentityFollowsKind() {
        XCTAssertEqual(rem.avatar, .main)
        XCTAssertEqual(health.avatar, .side(.default))
    }

    func testProfileCopyIsVerbatim() {
        XCTAssertEqual(AgentProfileCopy.laneHeader, "Lane")
        XCTAssertEqual(AgentProfileCopy.nameRow, "Name")
        XCTAssertEqual(AgentProfileCopy.handlesRow, "Handles")
        XCTAssertEqual(AgentProfileCopy.pause, "Pause agent")
        XCTAssertEqual(AgentProfileCopy.delete, "Delete agent")
    }
}
