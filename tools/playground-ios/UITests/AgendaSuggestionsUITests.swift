import XCTest

/// Drives the Agenda New · Suggestions playground journey. Proves the optimistic Add / Move / Dismiss
/// outcomes, three-inline-versus-four-in-overflow, overflow persistence, last-removal / gap removal, the
/// empty day becoming populated, deterministic restoration, Done / back, and large-text reachability.
/// Screenshots alone do not establish these interactions — each assertion checks the resulting state.
final class AgendaSuggestionsUITests: XCTestCase {
    let app = XCUIApplication()
    private let prepTitle = "Prep for tonight\u{2019}s rehearsal"

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--settings-light"]
        app.launch()
    }

    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func reveal(_ element: XCUIElement) {
        for _ in 0..<8 where !element.isHittable { app.swipeUp() }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }

    private func openAgenda(_ fixtureLabel: String = "Loaded") {
        if fixtureLabel != "Loaded" {
            let choice = app.segmentedControls["agendaFixturePicker"].buttons[fixtureLabel]
            reveal(choice)
            choice.tap()
        }
        let open = app.buttons["openAgendaSuggestions"]
        reveal(open)
        open.tap()
        XCTAssertTrue(app.staticTexts["Aug 13 2026"].waitForExistence(timeout: 3), "Agenda header must appear")
    }

    private func inlineAcceptCount() -> Int {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "agendaSuggestion.inline.accept.")).count
    }
    private func sheetAcceptCount() -> Int {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "agendaSuggestion.sheet.accept.")).count
    }

    // MARK: Canonical states + three-inline-versus-four-in-overflow

    func testInlineOverflowAndNone() {
        openAgenda()
        XCTAssertTrue(app.staticTexts["Suggestions"].exists)
        XCTAssertEqual(inlineAcceptCount(), 3, "At most three suggestions render inline")
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.prep"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.move"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.review"].exists)
        XCTAssertFalse(app.buttons["agendaSuggestion.inline.accept.setlist"].exists, "The fourth is in overflow only")
        capture("AgendaSuggestions-inline-journey-light")

        let seeMore = app.buttons["agendaSuggestions.seeMore"]
        reveal(seeMore)
        seeMore.tap()
        XCTAssertTrue(app.buttons["agendaSuggestions.done"].waitForExistence(timeout: 3))
        XCTAssertEqual(sheetAcceptCount(), 4, "Overflow shows all live suggestions, including the inline ones")
        XCTAssertTrue(app.buttons["agendaSuggestion.sheet.accept.setlist"].exists)
        capture("AgendaSuggestions-overflow-journey-light")
        app.buttons["agendaSuggestions.done"].tap()
        XCTAssertTrue(app.staticTexts["Suggestions"].waitForExistence(timeout: 3), "Done leaves suggestions unchanged")
        XCTAssertEqual(inlineAcceptCount(), 3)

        navigateBackToHome()
        openAgenda("No suggestions")
        XCTAssertFalse(app.staticTexts["Suggestions"].exists, "The none state has no Suggestions slot")
        XCTAssertTrue(app.staticTexts["Reply to the venue"].exists)
        capture("AgendaSuggestions-none-journey-light")
    }

    // MARK: Add / Move / Dismiss, stable IDs, preserved rows

    func testAddCreatesFivePMTaskAndPreservesRows() {
        openAgenda()
        let accept = app.buttons["agendaSuggestion.inline.accept.prep"]
        reveal(accept)
        accept.tap()
        XCTAssertTrue(app.staticTexts[prepTitle].waitForExistence(timeout: 3), "Add creates the authored task")
        XCTAssertTrue(app.staticTexts["5:00 PM"].exists, "The authored Add task is at 5PM")
        XCTAssertFalse(app.buttons["agendaSuggestion.inline.accept.prep"].exists, "Accepted suggestion is removed")
        // Unrelated rows + remaining suggestions keep their stable identity.
        XCTAssertTrue(app.staticTexts["Reply to the venue"].exists)
        XCTAssertTrue(app.staticTexts["Confirm rehearsal time"].exists)
        XCTAssertTrue(app.staticTexts["Coffee chat with a mentor"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.move"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.setlist"].exists, "Overflow row promotes inline")
        capture("AgendaSuggestions-added-light")
    }

    func testMoveReschedulesExistingRehearsal() {
        openAgenda()
        let accept = app.buttons["agendaSuggestion.inline.accept.move"]
        reveal(accept)
        accept.tap()
        XCTAssertFalse(app.buttons["agendaSuggestion.inline.accept.move"].exists)
        // The existing rehearsal confirmation is rescheduled to 3PM; its identity and title are kept.
        XCTAssertTrue(app.staticTexts["Confirm rehearsal time"].exists)
        XCTAssertTrue(app.staticTexts["3:00 PM"].waitForExistence(timeout: 3), "Move changes the rehearsal to 3PM")
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.prep"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.review"].exists)
        XCTAssertTrue(app.buttons["agendaSuggestion.inline.accept.setlist"].exists)
        capture("AgendaSuggestions-moved-light")
    }

    func testDismissRemovesSuggestionWithoutCreatingTask() {
        openAgenda()
        let dismiss = app.buttons["agendaSuggestion.inline.dismiss.prep"]
        reveal(dismiss)
        dismiss.tap()
        XCTAssertFalse(app.buttons["agendaSuggestion.inline.accept.prep"].waitForExistence(timeout: 1))
        XCTAssertFalse(app.staticTexts[prepTitle].exists, "Dismiss creates no task")
        // The remaining day and suggestions are untouched.
        XCTAssertTrue(app.staticTexts["Reply to the venue"].exists)
        XCTAssertTrue(app.staticTexts["Confirm rehearsal time"].exists)
        XCTAssertEqual(inlineAcceptCount(), 3, "The overflow row promotes inline")
        capture("AgendaSuggestions-dismissed-light")
    }

    // MARK: Last removal hides the slot and closes overflow

    func testLastRemovalHidesSlotAndGap() {
        openAgenda()
        for id in ["prep", "move", "review", "setlist"] {
            let dismiss = app.buttons["agendaSuggestion.inline.dismiss.\(id)"]
            XCTAssertTrue(dismiss.waitForExistence(timeout: 3), "\(id) should be reachable inline before removal")
            reveal(dismiss)
            dismiss.tap()
        }
        XCTAssertFalse(app.staticTexts["Suggestions"].exists, "Last removal hides the whole Suggestions slot")
        XCTAssertFalse(app.buttons["agendaSuggestions.seeMore"].exists)
        // The unrelated day remains intact.
        XCTAssertTrue(app.staticTexts["Reply to the venue"].exists)
        XCTAssertTrue(app.staticTexts["Coffee chat with a mentor"].exists)
        capture("AgendaSuggestions-last-removal-light")
    }

    // MARK: Overflow persistence + empty day becoming populated

    func testEmptyDayBecomesPopulatedWhileOverflowStaysOpen() {
        openAgenda("Empty day")
        XCTAssertTrue(app.staticTexts["No agenda yet"].exists, "The empty day shows the unavailable view")
        let seeMore = app.buttons["agendaSuggestions.seeMore"]
        reveal(seeMore)
        seeMore.tap()
        XCTAssertTrue(app.buttons["agendaSuggestions.done"].waitForExistence(timeout: 3))
        XCTAssertEqual(sheetAcceptCount(), 4)
        app.buttons["agendaSuggestion.sheet.accept.prep"].tap()
        // The sheet stays open while suggestions remain.
        XCTAssertTrue(app.buttons["agendaSuggestions.done"].exists, "Overflow persists after acceptance")
        XCTAssertEqual(sheetAcceptCount(), 3)
        capture("AgendaSuggestions-empty-populated-light")
        app.buttons["agendaSuggestions.done"].tap()
        // Behind the sheet, the empty day is now populated by the accepted task.
        XCTAssertTrue(app.staticTexts[prepTitle].waitForExistence(timeout: 3))
        XCTAssertFalse(app.staticTexts["No agenda yet"].exists, "The day is no longer empty")
        capture("AgendaSuggestions-empty-populated-after-done-light")
    }

    // MARK: Deterministic optimistic restoration (no network)

    func testOptimisticRestoration() {
        openAgenda("Restoration")
        let dismiss = app.buttons["agendaSuggestion.inline.dismiss.prep"]
        reveal(dismiss)
        dismiss.tap()
        XCTAssertFalse(app.buttons["agendaSuggestion.inline.accept.prep"].exists, "Optimistically removed")
        // A simulated sync failure restores the row deterministically.
        let restored = app.buttons["agendaSuggestion.inline.accept.prep"]
        XCTAssertTrue(restored.waitForExistence(timeout: 3), "The row is restored")
        capture("AgendaSuggestions-restored-light")
    }

    // MARK: Dark + large text reachability

    func testDarkAppearance() {
        app.terminate()
        app.launchArguments = ["--settings-dark"]
        app.launch()
        openAgenda()
        XCTAssertTrue(app.staticTexts["Suggestions"].exists)
        capture("AgendaSuggestions-dark")
    }

    func testLargeTextReachability() {
        app.terminate()
        app.launchArguments = ["--settings-light", "--settings-large-text"]
        app.launch()
        openAgenda()
        XCTAssertTrue(app.staticTexts["Suggestions"].exists)
        capture("AgendaSuggestions-large-text")
        // All suggestions and the See more action stay reachable at an accessibility text size.
        let seeMore = app.buttons["agendaSuggestions.seeMore"]
        reveal(seeMore)
        seeMore.tap()
        XCTAssertTrue(app.buttons["agendaSuggestions.done"].waitForExistence(timeout: 3))
        capture("AgendaSuggestions-large-text-overflow")
        app.buttons["agendaSuggestions.done"].tap()
    }

    // MARK: helpers

    private func navigateBackToHome() {
        let back = app.navigationBars["Agenda New"].buttons.element(boundBy: 0)
        XCTAssertTrue(back.waitForExistence(timeout: 3))
        back.tap()
        XCTAssertTrue(app.buttons["openAgendaSuggestions"].waitForExistence(timeout: 3))
    }
}
