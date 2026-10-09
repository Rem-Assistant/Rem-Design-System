import XCTest

/// Root structure, the component catalog's loading demo, and the onboarding flow's step routing.
/// Everything is local fixture state — no account, service, audio, or persistence.
final class PlaygroundNavigationUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--settings-light"]
        app.launch()
    }

    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.5)) // Let native navigation chrome settle.
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func reveal(_ element: XCUIElement) {
        for _ in 0..<8 where !element.isHittable { app.swipeUp() }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }

    private func tap(_ identifier: String) {
        let element = app.buttons[identifier]
        XCTAssertTrue(element.waitForExistence(timeout: 3), "Missing \(identifier)")
        reveal(element)
        element.tap()
    }

    private func openOnboardingStep(_ identifier: String) {
        tap("openOnboarding")
        tap(identifier)
    }

    // MARK: Root

    func testRootOffersComponentsAndScreensOnly() {
        for id in ["openComponents", "openSettings", "openAgendaSuggestions", "openOnboarding"] {
            XCTAssertTrue(app.buttons[id].waitForExistence(timeout: 3), "Root is missing \(id)")
        }
        XCTAssertFalse(app.buttons["Shared controls"].exists, "Controls live in the component catalog")
        XCTAssertFalse(app.staticTexts.containing(NSPredicate(format: "label CONTAINS[c] %@", "prototype")).firstMatch.exists,
                       "Root carries functional labels only")
        XCTAssertTrue(app.staticTexts["playground.build"].exists, "Build revision stays visible")
        capture("Playground-root-light")
    }

    // MARK: Components

    func testCatalogLoadingShowsSkeletonThenContentAndActionProgress() {
        tap("openComponents")
        XCTAssertTrue(app.buttons["openControls"].waitForExistence(timeout: 3))
        capture("Playground-components-light")
        tap("openLoading")
        let skeleton = app.descendants(matching: .any)["loading.skeleton"]
        XCTAssertTrue(skeleton.waitForExistence(timeout: 1), "Content load starts on the skeleton")
        XCTAssertEqual(skeleton.label, "Loading content")
        capture("Loading-skeleton-light")
        XCTAssertTrue(app.staticTexts["Memory"].waitForExistence(timeout: 5), "Skeleton resolves to content")
        XCTAssertFalse(skeleton.exists, "Skeleton is removed once content arrives")
        capture("Loading-content-light")
        tap("loading.refresh")
        XCTAssertTrue(app.buttons["loading.refresh"].staticTexts["Refreshing"].waitForExistence(timeout: 1)
            || app.staticTexts["Refreshing"].exists, "Actions show inline progress")
        XCTAssertTrue(app.staticTexts["Memory"].exists, "Content stays visible during an action")
        capture("Loading-action-progress-light")
        XCTAssertTrue(app.staticTexts["Refresh"].waitForExistence(timeout: 4), "Action progress clears")
    }

    // MARK: Onboarding

    /// Continue from Sign in walks every established step, in order, to the completion state.
    func testContinueAdvancesThroughEveryStepToCompletion() {
        openOnboardingStep("openOnboardingSignIn")
        capture("Onboarding-signIn-light")
        tap("Continue with Apple")
        XCTAssertTrue(app.buttons["Accept and Continue"].waitForExistence(timeout: 3), "Sign in → Consent")
        capture("Onboarding-consent-light")
        tap("Accept and Continue")
        XCTAssertTrue(app.staticTexts["Connectors"].waitForExistence(timeout: 3), "Consent → Connectors")
        capture("Onboarding-connectors-light")
        tap("Continue")
        XCTAssertTrue(app.buttons["onboardingCheckIn.continue"].waitForExistence(timeout: 3), "Connectors → Check-in")
        capture("Onboarding-checkin-light")
        tap("onboardingCheckIn.continue")
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 3), "Check-in → Voice")
        tap("onboardingVoice.continue")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice → completion")
        XCTAssertEqual(complete.value as? String, "continue")
        capture("Onboarding-complete-light")
        tap("onboarding.done")
        XCTAssertTrue(app.buttons["openOnboardingSignIn"].waitForExistence(timeout: 3), "Done returns to the onboarding hub")
    }

    /// Skip is available from Connectors onward and moves the flow on exactly like Continue.
    func testSkipAdvancesFromConnectorsToCompletion() {
        openOnboardingStep("openOnboardingConnectors")
        tap("Skip")
        XCTAssertTrue(app.buttons["onboardingCheckIn.skip"].waitForExistence(timeout: 3), "Connectors Skip → Check-in")
        tap("onboardingCheckIn.skip")
        XCTAssertTrue(app.buttons["onboardingVoice.skip"].waitForExistence(timeout: 3), "Check-in Skip → Voice")
        tap("onboardingVoice.skip")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice Skip → completion")
        XCTAssertEqual(complete.value as? String, "skip")
    }

    /// Native Back walks the pushed steps in reverse.
    func testBackReturnsToPreviousStep() {
        openOnboardingStep("openOnboardingCheckIn")
        tap("onboardingCheckIn.continue")
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 3))
        app.navigationBars.element(boundBy: 0).buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["onboardingCheckIn.continue"].waitForExistence(timeout: 3), "Back from Voice → Check-in")
    }

    func testConsentLegalRowsOpenDocuments() {
        openOnboardingStep("openOnboardingConsent")
        tap("Terms of Service")
        XCTAssertTrue(app.navigationBars["Terms of Service"].waitForExistence(timeout: 3), "Terms opens its document")
        capture("Onboarding-consent-terms-light")
        app.navigationBars["Terms of Service"].buttons.firstMatch.tap()
        XCTAssertTrue(app.buttons["Accept and Continue"].waitForExistence(timeout: 3), "Closing returns to Consent")
    }

    func testConnectorRowTogglesLocalState() {
        openOnboardingStep("openOnboardingConnectors")
        let slack = app.buttons.containing(NSPredicate(format: "label CONTAINS %@", "Slack")).firstMatch
        XCTAssertTrue(slack.waitForExistence(timeout: 3))
        slack.tap()
        XCTAssertTrue(app.buttons.containing(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "Slack", "Connected")).firstMatch
            .waitForExistence(timeout: 2) || app.staticTexts.matching(NSPredicate(format: "label == %@", "Connected")).count >= 2,
            "Tapping a connector updates its status")
    }
}
