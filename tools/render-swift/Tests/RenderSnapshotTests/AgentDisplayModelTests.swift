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

    func testSwatchesKeepTheFigmaOrderAndDefaultLook() {
        XCTAssertEqual(AgentColor.allCases, [.pink, .blue, .green, .purple, .indigo])
        XCTAssertEqual(AgentLook.default, AgentLook(glyph: .heart, color: .pink))
        XCTAssertEqual(AgentGlyph.heart.systemName, "heart.fill")
        XCTAssertEqual(AgentServiceGlyph.calendar.systemName, "calendar")
        XCTAssertEqual(AgentServiceGlyph.link.systemName, "link")
    }

    // MARK: New agent setup

    func testSetupCopyIsVerbatim() {
        XCTAssertEqual(NewAgentSetupCopy.title, "New agent")
        XCTAssertEqual(NewAgentSetupCopy.create, "Create agent")
        XCTAssertEqual(NewAgentSetupCopy.nameHeader, "Name")
        XCTAssertEqual(NewAgentSetupCopy.handlesHeader, "What should this agent handle?")
        XCTAssertEqual(AgentServicesCopy.header, "Connected services")
        XCTAssertEqual(AgentServicesCopy.addService, "Add a service")
    }

    func testNamingTheLaneIsTheOnlyRequiredStep() {
        var draft = NewAgentDraft()
        XCTAssertFalse(draft.canCreate)
        XCTAssertNil(draft.createAction)

        draft.name = "   \n "
        XCTAssertFalse(draft.canCreate, "whitespace is not a name")
        XCTAssertNil(draft.createAction)

        draft.name = "Health"
        XCTAssertTrue(draft.canCreate, "handles and services are optional")
        XCTAssertNotNil(draft.createAction)
    }

    func testCreateSendsATrimmedDraft() {
        let draft = NewAgentDraft(
            name: "  Health ",
            handles: "  Appointments, prescriptions  ",
            look: AgentLook(glyph: .heart, color: .indigo),
            services: [AgentService(id: "gcal", name: "Google Calendar", status: "Connected", glyph: .calendar)]
        )
        guard case .create(let sent)? = draft.createAction else {
            return XCTFail("expected a create action")
        }
        XCTAssertEqual(sent.name, "Health")
        XCTAssertEqual(sent.handles, "Appointments, prescriptions")
        XCTAssertEqual(sent.look, AgentLook(glyph: .heart, color: .indigo))
        XCTAssertEqual(sent.services.map(\.id), ["gcal"])
    }

    func testBlankHandlesAreOmitted() {
        XCTAssertNil(NewAgentDraft(name: "Money", handles: "  ").trimmedHandles)
        guard case .create(let sent)? = NewAgentDraft(name: "Money", handles: "  ").createAction else {
            return XCTFail("expected a create action")
        }
        XCTAssertEqual(sent.handles, "")
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
