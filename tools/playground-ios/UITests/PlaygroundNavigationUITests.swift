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

    /// Taps the first button whose combined label contains `text` (rows fold icons and subtitles in).
    private func tapButton(containing text: String) {
        let element = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
        XCTAssertTrue(element.waitForExistence(timeout: 3), "Missing button containing \(text)")
        reveal(element)
        element.tap()
    }

    private func waitUntilGone(_ element: XCUIElement, _ message: String) {
        let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: element)
        XCTAssertEqual(XCTWaiter().wait(for: [gone], timeout: 3), .completed, message)
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
        XCTAssertFalse(app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] %@", "prototype")).firstMatch.exists,
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
        let skeleton = app.descendants(matching: .any)["loading.skeleton"].firstMatch
        XCTAssertTrue(skeleton.waitForExistence(timeout: 2), "Content load starts on the skeleton")
        XCTAssertEqual(skeleton.label, "Loading content")
        capture("Loading-skeleton-light")
        XCTAssertTrue(app.staticTexts["Memory"].waitForExistence(timeout: 8), "Skeleton resolves to content")
        waitUntilGone(skeleton, "Skeleton is removed once content arrives")
        capture("Loading-content-light")
        tap("loading.refresh")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Refreshing")).firstMatch.waitForExistence(timeout: 2),
                      "Actions show inline progress")
        XCTAssertTrue(app.staticTexts["Memory"].exists, "Content stays visible during an action")
        capture("Loading-action-progress-light")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label == %@", "Refresh")).firstMatch.waitForExistence(timeout: 6),
                      "Action progress clears")
    }

    // MARK: Catalog pages

    private func openCatalogPage(_ identifier: String, title: String) {
        tap("openComponents")
        tap(identifier)
        XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 3), "\(title) page opens")
    }

    func testCatalogListsEveryPage() {
        tap("openComponents")
        for id in ["openControls", "openRows", "openCatalogAgenda", "openChat", "openAgentCatalog", "openBrand", "openLoading"] {
            XCTAssertTrue(app.buttons[id].waitForExistence(timeout: 3), "Catalog is missing \(id)")
        }
    }

    func testCatalogControlsButtonsSliderAndPills() {
        openCatalogPage("openControls", title: "Controls")
        let blue = app.buttons["Rect · Blue"]
        reveal(blue)
        blue.tap()
        let last = app.staticTexts["controls.lastButton"]
        XCTAssertTrue(last.waitForExistence(timeout: 2))
        XCTAssertEqual(last.label, "Tapped Rect · Blue", "Only the tapped variant fires")
        XCTAssertTrue(app.staticTexts["50%"].exists, "Slider value is shown")
        XCTAssertFalse(app.buttons["Disabled"].isEnabled, "Disabled variant is not interactive")
        capture("Catalog-controls-light")
    }

    func testCatalogRowsListAndConnectorStates() {
        openCatalogPage("openRows", title: "Rows")
        tap("catalog.listRow")
        // ListRow buttons fold their subtitle into the button label.
        let opened = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label CONTAINS %@", "Opened"),
                                               object: app.buttons["catalog.listRow"])
        XCTAssertEqual(XCTWaiter().wait(for: [opened], timeout: 3), .completed, "List row action runs")
        tapButton(containing: "Connect Gmail")
        XCTAssertTrue(app.switches.firstMatch.waitForExistence(timeout: 5), "Connect moves through Connecting to Connected")
        capture("Catalog-rows-light")
    }

    func testCatalogAgendaDateAndSuggestions() {
        openCatalogPage("openCatalogAgenda", title: "Agenda")
        XCTAssertTrue(app.staticTexts["Oct 1 2026"].exists)
        app.buttons["Next day"].tap()
        XCTAssertTrue(app.staticTexts["Oct 2 2026"].waitForExistence(timeout: 2), "Next day advances the date")
        tap("catalog.suggestion.accept.add")
        XCTAssertTrue(app.staticTexts["Added"].waitForExistence(timeout: 2), "Accepting resolves the suggestion")
        capture("Catalog-agenda-light")
        tap("catalog.suggestion.restore")
        XCTAssertTrue(app.buttons["catalog.suggestion.accept.add"].waitForExistence(timeout: 2), "Restore brings it back")
    }

    func testCatalogChatComposerSendsMessage() {
        openCatalogPage("openChat", title: "Chat")
        let field = app.descendants(matching: .any)["catalog.composerField"].firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        field.tap()
        field.typeText("Plan my afternoon")
        tap("catalog.composerSend")
        XCTAssertTrue(app.staticTexts["Plan my afternoon"].waitForExistence(timeout: 3), "Send adds a message bubble")
        capture("Catalog-chat-light")
    }

    func testCatalogAgentSurfaces() {
        openCatalogPage("openAgentCatalog", title: "Agent")
        XCTAssertTrue(app.staticTexts["Working"].firstMatch.exists)
        let ended = app.segmentedControls["catalog.browserState"].buttons["Ended"]
        reveal(ended)
        ended.tap()
        XCTAssertTrue(ended.isSelected, "Browser card state can be switched")
        capture("Catalog-agent-light")
    }

    func testCatalogBrandAndEmptyState() {
        openCatalogPage("openBrand", title: "Brand & empty states")
        tapButton(containing: "Add New")
        XCTAssertTrue(app.buttons["catalog.emptyReset"].waitForExistence(timeout: 2), "Empty-state action shows content")
        capture("Catalog-brand-light")
        tap("catalog.emptyReset")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Add New")).firstMatch.waitForExistence(timeout: 2))
        XCTAssertTrue(app.descendants(matching: .any)["catalog.providerMarks"].firstMatch.exists, "Provider marks render")
    }

    // MARK: Onboarding

    /// Continue from Sign in walks every established step, in order, to the completion state.
    func testContinueAdvancesThroughEveryStepToCompletion() {
        openOnboardingStep("openOnboardingSignIn")
        capture("Onboarding-signIn-light")
        tapButton(containing: "Continue with Apple")
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
        tapButton(containing: "Terms of Service")
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: 3), "Terms opens its document")
        capture("Onboarding-consent-terms-light")
        done.tap()
        waitUntilGone(done, "Done closes the document")
        XCTAssertTrue(app.buttons["Accept and Continue"].isHittable, "Closing returns to Consent")
    }

    func testConnectorRowTogglesLocalState() {
        openOnboardingStep("openOnboardingConnectors")
        // Gmail starts connected; Google Calendar and Slack offer Connect.
        let connect = app.buttons.matching(NSPredicate(format: "label == %@", "Connect"))
        XCTAssertTrue(connect.firstMatch.waitForExistence(timeout: 3))
        XCTAssertEqual(connect.count, 2)
        connect.firstMatch.tap()
        let oneLeft = expectation(for: NSPredicate(format: "count == 1"), evaluatedWith: connect)
        wait(for: [oneLeft], timeout: 3)
    }
}
