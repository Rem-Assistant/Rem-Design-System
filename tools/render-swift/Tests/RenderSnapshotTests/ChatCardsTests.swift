import XCTest
@testable import RemDesignSystem

/// Display-model contract for the chat cards (ConnectorCard / LoginCard / PermissionCard): state →
/// control, copy rules and disclosure defaults. Paired with `ChatCardsTest.kt`; both assert the same
/// strings so the SwiftUI and Compose cards cannot drift silently.
final class ChatCardsTests: XCTestCase {

    // MARK: - ConnectorCard

    private func connector(_ state: ConnectorCardState) -> ConnectorCardModel {
        ConnectorCardModel(provider: .gmail, subtitle: "Search, read, draft, and manage email.", state: state)
    }

    func testConnectorControlPerState() {
        XCTAssertEqual(connector(.authorize).control, .authorize(label: "Authorize"))
        XCTAssertEqual(connector(.connecting).control, .progress(label: "Adding…"))
        XCTAssertEqual(connector(.added).control, .receipt(label: "Added"))
        XCTAssertEqual(connector(.error()).control, .retry(label: "Retry"))
    }

    func testConnectorTitleDefaultsToProviderAndPurposeIsPermanent() {
        for state in [ConnectorCardState.authorize, .connecting, .added, .error()] {
            XCTAssertEqual(connector(state).title, "Gmail")
            XCTAssertEqual(connector(state).subtitle, "Search, read, draft, and manage email.")
        }
    }

    func testConnectorErrorCopyOnlyInErrorState() {
        XCTAssertNil(connector(.authorize).errorMessage)
        XCTAssertNil(connector(.connecting).errorMessage)
        XCTAssertNil(connector(.added).errorMessage)
        XCTAssertEqual(connector(.error()).errorMessage, "Gmail authorization failed. Retry to connect.")
        XCTAssertEqual(connector(.error(message: "  ")).errorMessage, "Gmail authorization failed. Retry to connect.")
        XCTAssertEqual(connector(.error(message: "Access was revoked.")).errorMessage, "Access was revoked.")
    }

    // MARK: - LoginCard

    func testLoginCopyAndButtonVariantPerState() {
        let entry = LoginCardModel(title: "GitHub login details", site: "github.com", state: .entry)
        XCTAssertEqual(entry.detail, "Add the login this browser task needs.")
        XCTAssertEqual(entry.buttonLabel, "Add login")
        XCTAssertEqual(entry.buttonVariant, .rectBlue)
        let saved = LoginCardModel(title: "GitHub login details", site: "github.com", state: .saved)
        XCTAssertEqual(saved.detail, "Login saved for this site.")
        XCTAssertEqual(saved.buttonLabel, "Saved")
        XCTAssertEqual(saved.buttonVariant, .rectSecondary)
    }

    @MainActor
    func testLoginFormSaveRuleMatchesCloudBrowser() {
        let drafts = [("", ""), ("  ", "pw"), ("dev@example.com", ""), ("dev@example.com", "pw"), ("u", " ")]
        for (username, password) in drafts {
            XCTAssertEqual(LoginForm.canSave(username: username, password: password),
                           CloudBrowserModel.canAddLogin(username: username, password: password), "\(username)/\(password)")
        }
        XCTAssertFalse(LoginForm.canSave(username: "  ", password: "pw"))
        XCTAssertTrue(LoginForm.canSave(username: "dev@example.com", password: "pw"))
    }

    // MARK: - PermissionCard

    private func permission(_ state: PermissionCardState, scope: String? = "create reminders in Personal only.") -> PermissionCardModel {
        PermissionCardModel(
            title: "Reminder permission", question: "Allow Rem to create this reminder?",
            summary: "One reminder in your Personal list.",
            details: PermissionRequestDetails(title: "Send investor update", schedule: "Oct 10, 2026 · 9:00 AM UTC", source: "Reminders · Personal"),
            alwaysAllowScope: scope, state: state
        )
    }

    func testAwaitingExpandsAndResolvedStatesCollapse() {
        XCTAssertTrue(PermissionCardState.awaiting.defaultExpanded)
        XCTAssertFalse(PermissionCardState.allowed.defaultExpanded)
        XCTAssertFalse(PermissionCardState.denied.defaultExpanded)
    }

    func testReceiptIsOneConciseStatusNeverDeniedNotSet() {
        XCTAssertEqual(permission(.awaiting).subtitle, "Needs your approval")
        XCTAssertEqual(permission(.allowed).subtitle, "Allowed once")
        XCTAssertEqual(permission(.denied).subtitle, "Denied")
        for state in [PermissionCardState.awaiting, .allowed, .denied] {
            let receipt = permission(state).subtitle.lowercased()
            XCTAssertFalse(receipt.contains("not set"), receipt)
            XCTAssertFalse(receipt.contains("•"), receipt)
            XCTAssertFalse(receipt.contains("·"), receipt)
        }
    }

    func testDecisionsOnlyWhileAwaitingAndReviewAgainOnlyWhenDenied() {
        XCTAssertTrue(permission(.awaiting).showsDecisions)
        XCTAssertFalse(permission(.allowed).showsDecisions)
        XCTAssertFalse(permission(.denied).showsDecisions)
        XCTAssertTrue(permission(.denied).showsReviewAgain)
        XCTAssertFalse(permission(.awaiting).showsReviewAgain)
        XCTAssertFalse(permission(.allowed).showsReviewAgain)
    }

    func testAlwaysAllowIsAScopedProposalOnlyWhileAwaiting() {
        XCTAssertTrue(permission(.awaiting).showsAlwaysAllow)
        XCTAssertEqual(permission(.awaiting).alwaysAllowFootnote,
                       "Proposed Always allow scope: create reminders in Personal only.")
        XCTAssertFalse(permission(.awaiting, scope: nil).showsAlwaysAllow)
        XCTAssertNil(permission(.awaiting, scope: nil).alwaysAllowFootnote)
        XCTAssertNil(permission(.allowed).alwaysAllowFootnote)
        XCTAssertNil(permission(.denied).alwaysAllowFootnote)
    }

    func testRequestPayloadIsPreservedAcrossStates() {
        let details = Set([PermissionCardState.awaiting, .allowed, .denied].map { permission($0).details })
        XCTAssertEqual(details.count, 1)
    }

    func testRiskLabelOnlyWhenElevated() {
        XCTAssertEqual(permission(.awaiting).risk, .standard)
        XCTAssertNil(permission(.awaiting).riskLabel)
        var elevated = permission(.awaiting)
        elevated.risk = .elevated
        XCTAssertEqual(elevated.riskLabel, "Elevated risk")
    }

    func testFullParametersHiddenWhenEmpty() {
        XCTAssertFalse(permission(.awaiting).showsParameters)
        var withParameters = permission(.awaiting)
        withParameters.parameters = [PermissionParameter(label: "to", value: "investors@example.com")]
        XCTAssertTrue(withParameters.showsParameters)
    }

    func testDetailRowsAreLabelValuePairsOfTheRequest() {
        XCTAssertEqual(permission(.awaiting).detailRows, [
            PermissionParameter(label: "Action", value: "Send investor update"),
            PermissionParameter(label: "When", value: "Oct 10, 2026 · 9:00 AM UTC"),
            PermissionParameter(label: "Source", value: "Reminders · Personal"),
        ])
    }
}
