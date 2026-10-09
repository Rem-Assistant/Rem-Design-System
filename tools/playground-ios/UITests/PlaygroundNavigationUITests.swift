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

    /// On failure, print what was on screen (bounded) so it reaches the job log; the workflow repeats
    /// it at the end of a failed job, since attachments are exported only after a passing run.
    override func tearDownWithError() throws {
        if let run = testRun, run.failureCount > 0 {
            let tree = app.debugDescription
            print("[failure-diagnostic] \(name)\n\(tree.prefix(12_000))\n[/failure-diagnostic]")
        }
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

    /// Taps a text input and requires it to take keyboard focus before anything is typed, so a tap
    /// that does not focus fails here rather than inside typeText.
    private func focus(_ field: XCUIElement) {
        field.tap()
        let focused = XCTNSPredicateExpectation(predicate: NSPredicate(format: "hasKeyboardFocus == true"), object: field)
        let result = XCTWaiter.wait(for: [focused], timeout: 2)
        if result != .completed {
            // Evidence for a failure: what the tap hit and what, if anything, holds focus. Printed
            // (bounded) so it reaches the job log, since attachments are exported only after a pass.
            let tree = app.debugDescription
            let limit = 12_000
            print("""
            [focus-diagnostic] \(field.identifier) unfocused after tap; keyboards: \(app.keyboards.count); \
            frame: \(field.frame); hierarchy (\(min(tree.count, limit)) of \(tree.count) chars):
            \(tree.prefix(limit))
            [/focus-diagnostic]
            """)
            let hierarchy = XCTAttachment(string: tree)
            hierarchy.name = "\(field.identifier)-unfocused-hierarchy"
            hierarchy.lifetime = .keepAlways
            add(hierarchy)
        }
        XCTAssertEqual(result, .completed, "\(field.identifier) did not take keyboard focus on tap")
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

    private func slug(_ text: String) -> String {
        text.lowercased().map { $0.isLetter || $0.isNumber ? String($0) : "-" }.joined()
            .split(separator: "-").joined(separator: "-")
    }

    /// One overview capture: `name` names the file, `anchor` is text inside the component (never its
    /// group heading). `last` picks the final match when the page repeats the text (the trace's
    /// "Working" footer follows the "Working" status pill); `up` scrolls back toward the page top.
    private struct Shot {
        let name: String
        let anchor: String
        var last = false
        var up = false
    }

    /// Scrolls in short drags held at the end (no fling) until `element` is hittable, so it stops just
    /// inside the edge it entered from: the bottom when scrolling down, with its component above it,
    /// or the top when scrolling up, with its component below it. Drags start at the trailing margin,
    /// clear of the page's controls.
    private func scroll(to element: XCUIElement, named anchor: String, up: Bool) {
        let window = app.windows.firstMatch
        let from = window.coordinate(withNormalizedOffset: CGVector(dx: 0.985, dy: up ? 0.35 : 0.65))
        let to = window.coordinate(withNormalizedOffset: CGVector(dx: 0.985, dy: up ? 0.65 : 0.35))
        for _ in 0..<24 {
            if element.exists && element.isHittable { break }
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        XCTAssertTrue(element.exists && element.isHittable, "Expected reachable content: \(anchor)")
    }

    /// Unobscured overview captures: each page's top, then each component scrolled into view by its
    /// own content, before any interaction (so no keyboard or post-action scroll position). The
    /// execution trace is captured at its bottom, then its top, in case it outgrows the viewport.
    func testCatalogSectionCaptures() {
        tap("openComponents")
        let pages: [(id: String, title: String, slug: String, shots: [Shot])] = [
            ("openControls", "Controls", "controls", [
                Shot(name: "Buttons", anchor: "Pill \u{00B7} Secondary"),
                Shot(name: "Slider", anchor: "50%"),
                Shot(name: "Pills", anchor: "Personal"),
            ]),
            ("openRows", "Rows", "rows", [
                Shot(name: "Section", anchor: "Applies to this device."),
                Shot(name: "Connector row", anchor: "Gmail"),
                Shot(name: "Task and event rows", anchor: "Unfiled inbox task"),
            ]),
            ("openCatalogAgenda", "Agenda", "agenda", [
                Shot(name: "Suggestion rows", anchor: "Reply to the venue"),
                Shot(name: "Suggestion section", anchor: "See more"),
            ]),
            ("openChat", "Chat", "chat", [
                Shot(name: "Composer", anchor: "Auto"),
                Shot(name: "Voice bar", anchor: "Listening\u{2026}"),
            ]),
            ("openAgentCatalog", "Agent", "agent", [
                Shot(name: "Running task banner", anchor: "Needs you \u{00B7} Password rejected"),
                Shot(name: "Browser card", anchor: "Rem's browser session"),
                Shot(name: "Execution trace bottom", anchor: "Working", last: true),
                Shot(name: "Execution trace top", anchor: "IN PROGRESS", up: true),
                Shot(name: "Daily brief card", anchor: "Read latest brief"),
            ]),
            ("openBrand", "Brand & empty states", "brand", [
                Shot(name: "Provider marks", anchor: "Google"),
                Shot(name: "Empty state", anchor: "Add New"),
            ]),
        ]
        for page in pages {
            tap(page.id)
            XCTAssertTrue(app.navigationBars[page.title].waitForExistence(timeout: 3), "\(page.title) opens")
            capture("Catalog-\(page.slug)-top-light")
            for shot in page.shots {
                // Combined accessibility labels fold row text together, so match by containment.
                let matches = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", shot.anchor))
                let element = shot.last ? matches.element(boundBy: max(matches.count - 1, 0)) : matches.firstMatch
                scroll(to: element, named: shot.anchor, up: shot.up)
                capture("Catalog-\(page.slug)-\(slug(shot.name))-light")
            }
            app.navigationBars[page.title].buttons.element(boundBy: 0).tap()
            XCTAssertTrue(app.buttons[page.id].waitForExistence(timeout: 3), "Back returns to the catalog")
        }
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
        let level = app.staticTexts["50%"]
        reveal(level)
        XCTAssertTrue(level.exists, "Slider value is shown")
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
        // Suggestion section: three inline, the fourth behind See more.
        let fourth = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "Book the venue for the offsite")).firstMatch
        XCTAssertFalse(fourth.exists, "Only three suggestions render inline")
        let seeMore = app.buttons["See more"]
        reveal(seeMore)
        seeMore.tap()
        XCTAssertTrue(fourth.waitForExistence(timeout: 2), "See more reveals the rest")
        tap("catalog.section.reset")
    }

    func testCatalogChatComposerSendsMessage() {
        openCatalogPage("openChat", title: "Chat")
        // A vertical-axis TextField is exposed as a text view.
        let field = app.textViews["catalog.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        focus(field)
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
        let read = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Read latest brief")).firstMatch
        reveal(read)
        read.tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Stop reading")).firstMatch.waitForExistence(timeout: 2),
                      "The brief's read action toggles")
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
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Connectors → Check-in")
        capture("Onboarding-checkin-light")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Check-in saves → Voice")
        tap("onboardingVoice.continue")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice → completion")
        XCTAssertEqual(complete.value as? String, "continue")
        capture("Onboarding-complete-light")
        tap("onboarding.done")
        XCTAssertTrue(app.buttons["openOnboardingSignIn"].waitForExistence(timeout: 3), "Done returns to the onboarding hub")
    }

    /// Connectors and Voice offer Skip, which moves the flow on like Continue. Check-in has no Skip
    /// (it asks for at least one time), so the flow passes it with Continue.
    func testSkipAdvancesFromConnectorsToCompletion() {
        openOnboardingStep("openOnboardingConnectors")
        tap("Skip")
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Connectors Skip → Check-in")
        XCTAssertFalse(app.buttons["Skip"].exists, "Check-in has no Skip")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.skip"].waitForExistence(timeout: 5), "Check-in → Voice")
        tap("onboardingVoice.skip")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice Skip → completion")
        XCTAssertEqual(complete.value as? String, "skip")
    }

    /// Native Back walks the pushed steps in reverse.
    func testBackReturnsToPreviousStep() {
        openOnboardingStep("openOnboardingCheckIn")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5))
        app.navigationBars.element(boundBy: 0).buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Back from Voice → Check-in")
    }

    /// Check-in: a switch adds a time, the native wheel changes one, and Continue saves before moving on.
    func testCheckInSwitchTimePickerAndSave() {
        openOnboardingStep("openOnboardingCheckIn")
        XCTAssertFalse(app.buttons["Edit 12:30 PM"].exists, "Midday starts off, without a time")
        app.switches["Midday"].tap()
        XCTAssertTrue(app.buttons["Edit 12:30 PM"].waitForExistence(timeout: 2), "Turning Midday on shows its time")
        tap("Edit 8:00 AM")
        let picker = app.datePickers.firstMatch
        XCTAssertTrue(picker.waitForExistence(timeout: 3), "The time opens the native picker")
        app.pickerWheels.element(boundBy: 0).adjust(toPickerWheelValue: "9")
        capture("Onboarding-checkin-picker-light")
        // Drag the sheet down from its top margin, clear of the wheels.
        let top = picker.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0)).withOffset(CGVector(dx: 0, dy: -10))
        top.press(forDuration: 0.1, thenDragTo: app.windows.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        waitUntilGone(picker, "The picker sheet closes")
        XCTAssertTrue(app.buttons["Edit 9:00 AM"].waitForExistence(timeout: 2), "The chosen time shows in the row")
        capture("Onboarding-checkin-edited-light")
        // The one-second Saving state is not asserted here: its spinner keeps the app from idling, so
        // XCUITest may not observe it before Saved. The failure test pins the persistence states.
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Saved → Voice")
    }

    /// Check-in with the "Fails once" fixture: the first save fails with Try again, and retrying recovers.
    func testCheckInSaveFailureRecovers() {
        tap("openOnboarding")
        app.segmentedControls["checkInSaveFixture"].buttons["Fails once"].tap()
        tap("openOnboardingCheckIn")
        app.buttons["Continue"].tap()
        let retry = app.buttons["Try again"]
        XCTAssertTrue(retry.waitForExistence(timeout: 4), "A failed save offers Try again")
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "save your check-in times")).firstMatch.exists, "The failure toast explains it")
        capture("Onboarding-checkin-failure-light")
        retry.tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Retry saves → Voice")
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
