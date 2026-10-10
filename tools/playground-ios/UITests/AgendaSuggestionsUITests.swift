import XCTest

/// Drives the Agenda New · Suggestions playground journey. Proves the optimistic Add / Move / Dismiss
/// outcomes, three-inline-versus-four-in-overflow, overflow persistence, last-removal / gap removal, the
/// empty day becoming populated, deterministic restoration, Done / back, and large-text reachability.
/// Also drives the two entries in the Add New / Schedule bar: creation (Save / Cancel) and Schedule Tasks
/// (Add to Today, Plan with retained selection, close).
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

    // MARK: Entry routing · Add New → creation (2390:28498)

    private func openCreation() -> XCUIElement {
        let addNew = app.buttons["agenda.addNew"]
        reveal(addNew)
        addNew.tap()
        let save = app.buttons["agendaCreate.save"]
        XCTAssertTrue(save.waitForExistence(timeout: 3), "Add New opens New Task or Event")
        return save
    }

    private func typeTitle(_ text: String) {
        let field = app.textFields["agendaCreate.title"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        field.tap()
        field.typeText(text)
    }

    func testAddNewSavesTaskAndEventAndCancelChangesNothing() {
        openAgenda()
        var save = openCreation()
        XCTAssertFalse(save.isEnabled, "Save waits for a title")
        typeTitle("Prepare rehearsal notes")
        app.buttons["agendaCreate.chooser"].tap()
        let work = app.buttons["Work"]
        XCTAssertTrue(work.waitForExistence(timeout: 3), "The task-list chooser offers No List / Follow-ups / Work")
        XCTAssertTrue(app.buttons["Follow-ups"].exists)
        work.tap()
        XCTAssertTrue(save.isEnabled)
        capture("AgendaEntry-new-task-light")
        save.tap()
        XCTAssertTrue(app.staticTexts["Prepare rehearsal notes"].waitForExistence(timeout: 3), "Save adds the task")
        XCTAssertTrue(app.staticTexts["5:00 PM"].exists, "The authored task slot is 5PM")
        XCTAssertTrue(app.staticTexts["Work"].exists, "The chosen list is kept")
        XCTAssertTrue(app.staticTexts["Reply to the venue"].exists, "The rest of the day is preserved")

        save = openCreation()
        app.segmentedControls["agendaCreate.mode"].buttons["New Event"].tap()
        typeTitle("Evening rehearsal")
        capture("AgendaEntry-new-event-light")
        save.tap()
        XCTAssertTrue(app.staticTexts["Evening rehearsal"].waitForExistence(timeout: 3), "Save adds the event")
        XCTAssertTrue(app.staticTexts["6:00 PM"].exists, "The authored event slot is 6PM")

        _ = openCreation()
        typeTitle("Discarded draft")
        app.buttons["agendaCreate.cancel"].tap()
        XCTAssertTrue(app.staticTexts["Aug 13 2026"].waitForExistence(timeout: 3))
        XCTAssertFalse(app.staticTexts["Discarded draft"].exists, "Cancel leaves the Agenda unchanged")
        capture("AgendaEntry-created-light")
    }

    // MARK: Entry routing · Schedule → Schedule Tasks (2295:13691)

    private func openSchedule() {
        let schedule = app.buttons["agenda.schedule"]
        reveal(schedule)
        schedule.tap()
        XCTAssertTrue(app.segmentedControls["agendaSchedule.filter"].waitForExistence(timeout: 3),
                      "Schedule opens Schedule Tasks")
    }

    func testScheduleAddToTodayThenPlanWithRetainedSelection() {
        openAgenda()
        XCTAssertTrue(app.buttons["agenda.schedule"].label.contains("3"), "Three tasks wait to be scheduled")
        openSchedule()
        XCTAssertTrue(app.buttons["agendaSchedule.task.draft"].exists)
        XCTAssertTrue(app.buttons["agendaSchedule.task.specs"].exists)
        XCTAssertTrue(app.buttons["agendaSchedule.task.dentist"].exists)
        XCTAssertFalse(app.buttons["agendaSchedule.task.walkthrough"].exists, "Events are never listed")
        XCTAssertFalse(app.buttons["agendaSchedule.addTo"].isEnabled, "Nothing selected yet")

        // Add to Today → Pick a Time (default 9:00) → Done.
        app.buttons["agendaSchedule.task.draft"].tap()
        XCTAssertTrue(app.buttons["agendaSchedule.task.draft"].isSelected)
        capture("AgendaEntry-schedule-selected-light")
        app.buttons["agendaSchedule.addTo"].tap()
        let done = app.buttons["agendaSchedule.done"]
        XCTAssertTrue(done.waitForExistence(timeout: 3), "Add to Today opens Pick a Time")
        XCTAssertTrue(app.buttons["agendaSchedule.dateRow"].label.contains("Today, August 13"))
        capture("AgendaEntry-pick-time-light")
        done.tap()
        XCTAssertTrue(app.staticTexts["Draft project update"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["9:00 AM"].exists, "Scheduled at the default 9:00 AM")
        XCTAssertTrue(app.buttons["agenda.schedule"].label.contains("2"), "The badge counts the remaining tasks")

        // Plan: selection survives filter changes, then Pick a Date → Next → Pick a Time → Done.
        openSchedule()
        XCTAssertFalse(app.buttons["agendaSchedule.task.draft"].exists, "Scheduled tasks leave the list")
        app.buttons["agendaSchedule.task.dentist"].tap()
        let filter = app.segmentedControls["agendaSchedule.filter"]
        filter.buttons["Overdue"].tap()
        XCTAssertFalse(app.buttons["agendaSchedule.task.dentist"].exists)
        app.buttons["agendaSchedule.task.specs"].tap()
        filter.buttons["Inbox"].tap()
        XCTAssertEqual(app.staticTexts["agendaSchedule.empty"].label, "No tasks in Inbox", "The empty filter state")
        XCTAssertTrue(app.buttons["agendaSchedule.plan"].isEnabled, "Selection is retained across filters")
        app.buttons["agendaSchedule.plan"].tap()
        let next = app.buttons["agendaSchedule.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 3), "Plan opens Pick a Date")
        capture("AgendaEntry-pick-date-light")
        next.tap()
        XCTAssertTrue(done.waitForExistence(timeout: 3), "Next opens Pick a Time")
        app.buttons["agendaSchedule.dateRow"].tap()
        XCTAssertTrue(app.datePickers["agendaSchedule.datePicker"].waitForExistence(timeout: 3), "The Date row expands")
        done.tap()
        XCTAssertTrue(app.staticTexts["Book dentist appointment"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Review design specs"].exists, "Done schedules the selection together")
        capture("AgendaEntry-scheduled-light")
    }

    func testScheduleCloseChangesNothing() {
        openAgenda()
        openSchedule()
        app.buttons["agendaSchedule.task.draft"].tap()
        app.buttons["agendaSchedule.close"].tap()
        XCTAssertTrue(app.staticTexts["Aug 13 2026"].waitForExistence(timeout: 3))
        XCTAssertFalse(app.staticTexts["Draft project update"].exists, "Close leaves the Agenda unchanged")
        XCTAssertTrue(app.buttons["agenda.schedule"].label.contains("3"))
    }

    // MARK: helpers

    private func navigateBackToHome() {
        let back = app.navigationBars["Agenda New"].buttons.element(boundBy: 0)
        XCTAssertTrue(back.waitForExistence(timeout: 3))
        back.tap()
        XCTAssertTrue(app.buttons["openAgendaSuggestions"].waitForExistence(timeout: 3))
    }
}
