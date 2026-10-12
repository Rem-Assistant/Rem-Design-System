import XCTest
@testable import RemDesignSystem

/// Agenda entry routing fixture (`AgendaEntryFixture.swift`): Add New → creation (`2390:28498`) and
/// Schedule → Schedule Tasks (`2295:13691`). Paired with `AgendaEntryFixtureTest.kt`. Proves events are
/// never schedule candidates, filters are truthful, Done schedules the selection together on the viewed
/// day, Save applies only a valid draft, and labels match the authored copy. Native journeys are covered
/// by `AgendaSuggestionsUITests`.
final class AgendaEntryFixtureTests: XCTestCase {
    private let today = AgendaDay.fixtureToday

    // MARK: Schedule Tasks

    func testCandidatesExcludeEventsAndFollowTheAuthoredOrder() {
        let fixture = AgendaEntryFixture()
        XCTAssertTrue(fixture.backlog.contains { $0.kind == .event }, "The fixture carries an undated event")
        XCTAssertEqual(fixture.candidates(.all).map(\.id), ["draft", "specs", "dentist"])
        XCTAssertFalse(AgendaScheduleFilter.allCases.contains { filter in
            fixture.candidates(filter).contains { $0.kind == .event }
        }, "No filter ever lists an event")
        XCTAssertEqual(fixture.scheduleCount, 3, "The Schedule badge counts tasks only")
    }

    func testFiltersMatchTheAuthoredStates() {
        let fixture = AgendaEntryFixture()
        XCTAssertEqual(fixture.candidates(.inbox).map(\.title), ["Draft project update"])
        XCTAssertEqual(fixture.candidates(.overdue).map(\.title), ["Review design specs"])
        XCTAssertEqual(fixture.candidates(.overdue).first?.detail, "Overdue · 2d")
        XCTAssertEqual(AgendaScheduleFilter.inbox.emptyText, "No tasks in Inbox")
        XCTAssertEqual(AgendaScheduleFilter.allCases.map(\.title), ["All", "Inbox", "Overdue"])
    }

    func testAddToTodayLandsSelectedTaskAtNineOnTheViewedDay() {
        var fixture = AgendaEntryFixture()
        let rows = fixture.schedule(AgendaScheduleRequest(taskIDs: ["draft"], day: today, time: .scheduleDefault))
        XCTAssertEqual(rows.count, 1)
        XCTAssertEqual(rows.first?.title, "Draft project update")
        XCTAssertEqual(rows.first?.timeLabel, "9:00 AM")
        XCTAssertEqual(rows.first?.sortMinutes, 9 * 60)
        XCTAssertEqual(rows.first?.pills, ["Inbox"])
        XCTAssertEqual(rows.first?.kind, .task)
        XCTAssertEqual(fixture.scheduleCount, 2, "After scheduling the badge reads 2")
        XCTAssertTrue(fixture.candidates(.inbox).isEmpty, "Inbox becomes the empty filter")
    }

    func testPlanSchedulesTheSelectionTogetherAtThePickedSlot() {
        var fixture = AgendaEntryFixture()
        let rows = fixture.schedule(AgendaScheduleRequest(taskIDs: ["draft", "specs"], day: today,
                                                          time: AgendaTime(hour: 14, minute: 30)))
        XCTAssertEqual(rows.map(\.title), ["Draft project update", "Review design specs"])
        XCTAssertEqual(Set(rows.map(\.timeLabel)), ["2:30 PM"])
        XCTAssertEqual(fixture.candidates(.all).map(\.id), ["dentist"])
    }

    func testAnotherDayLeavesTheBacklogButNotTheViewedAgenda() {
        var fixture = AgendaEntryFixture()
        let rows = fixture.schedule(AgendaScheduleRequest(taskIDs: ["dentist"], day: today.adding(days: 2),
                                                          time: .scheduleDefault))
        XCTAssertTrue(rows.isEmpty, "Another day's tasks are off-screen")
        XCTAssertEqual(fixture.scheduleCount, 2)
    }

    func testEventsAndUnknownIDsAreNeverScheduled() {
        var fixture = AgendaEntryFixture()
        let rows = fixture.schedule(AgendaScheduleRequest(taskIDs: ["walkthrough", "missing"], day: today,
                                                          time: .scheduleDefault))
        XCTAssertTrue(rows.isEmpty)
        XCTAssertEqual(fixture.backlog, AgendaEntryFixture.referenceBacklog, "Nothing changes")
    }

    // MARK: Day / time labels

    func testDayArithmeticAndLabels() {
        XCTAssertEqual(today.headerText, "Aug 13 2026")
        XCTAssertEqual(today.weekday, 4, "August 13, 2026 is a Thursday")
        XCTAssertEqual(AgendaDay(epochDay: today.epochDay), today)
        XCTAssertEqual(AgendaDay(year: 1970, month: 1, day: 1).epochDay, 0)
        XCTAssertEqual(AgendaDay(year: 2026, month: 12, day: 31).adding(days: 1), AgendaDay(year: 2027, month: 1, day: 1))
        XCTAssertEqual(AgendaDay(year: 2028, month: 2, day: 28).adding(days: 1), AgendaDay(year: 2028, month: 2, day: 29))
        XCTAssertEqual(today.dateRowText(today: today), "Today, August 13")
        XCTAssertEqual(today.adding(days: 1).dateRowText(today: today), "Tomorrow, August 14")
        XCTAssertEqual(today.adding(days: 2).dateRowText(today: today), "Saturday, August 15")
        XCTAssertEqual(today.addToLabel(today: today), "Add to Today")
        XCTAssertEqual(today.adding(days: 1).addToLabel(today: today), "Add to Tomorrow")
        XCTAssertEqual(today.adding(days: 2).addToLabel(today: today), "Add to Aug 15")
        XCTAssertEqual(AgendaTime.scheduleDefault.label, "9:00 AM")
        XCTAssertEqual(AgendaTime(hour: 0, minute: 5).label, "12:05 AM")
        XCTAssertEqual(AgendaTime(hour: 12, minute: 0).label, "12:00 PM")
    }

    // MARK: Creation

    func testBlankDraftCannotSave() {
        var fixture = AgendaEntryFixture()
        XCTAssertFalse(AgendaCreationDraft().canSave)
        XCTAssertFalse(AgendaCreationDraft(title: "   ").canSave)
        XCTAssertNil(fixture.create(AgendaCreationDraft(title: " ")))
    }

    func testSavedTaskKeepsItsListAtFivePM() {
        var fixture = AgendaEntryFixture()
        let draft = AgendaCreationDraft(mode: .task, title: " Prepare rehearsal notes ", notes: "Bring the revised set list.",
                                        taskList: .work)
        XCTAssertEqual(draft.chooserLabel, "Work")
        XCTAssertEqual(draft.whenSummary, "Today, 5:00 PM")
        XCTAssertEqual(draft.durationSummary, "30 min")
        let row = fixture.create(draft)
        XCTAssertEqual(row?.kind, .task)
        XCTAssertEqual(row?.title, "Prepare rehearsal notes")
        XCTAssertEqual(row?.timeLabel, "5:00 PM")
        XCTAssertEqual(row?.pills, ["Work"])
        XCTAssertEqual(row?.pending, true, "A just-saved task shows the pending ring")
        XCTAssertEqual(fixture.scheduleCount, 3, "Creating never touches the schedule backlog")
    }

    func testNoListTaskHasNoPill() {
        var fixture = AgendaEntryFixture()
        XCTAssertEqual(AgendaCreationDraft().chooserLabel, "Select task list")
        XCTAssertEqual(fixture.create(AgendaCreationDraft(title: "Prepare rehearsal notes"))?.pills, [])
    }

    func testSavedEventKeepsItsCalendarAtSixPM() {
        var fixture = AgendaEntryFixture()
        let draft = AgendaCreationDraft(mode: .event, title: "Evening rehearsal", calendar: .work)
        XCTAssertEqual(draft.whenSummary, "Today, 6:00–7:00 PM")
        XCTAssertEqual(draft.durationSummary, "1 hr")
        let row = fixture.create(draft)
        XCTAssertEqual(row?.kind, .event)
        XCTAssertEqual(row?.timeLabel, "6:00 PM")
        XCTAssertEqual(row?.pills, ["Work"])
        XCTAssertEqual(row?.calendar, .work)
        XCTAssertEqual(row?.pending, false)
    }

    func testCreatedRowsHaveDistinctIDs() {
        var fixture = AgendaEntryFixture()
        let first = fixture.create(AgendaCreationDraft(title: "One"))
        let second = fixture.create(AgendaCreationDraft(title: "One"))
        XCTAssertNotEqual(first?.id, second?.id)
    }

    // MARK: Playground host

    func testPlaygroundHostAppliesResultsInMemory() {
        let model = AgendaSuggestionsModel(fixture: .loaded)
        model.scheduleOpen = true
        model.schedule(AgendaScheduleRequest(taskIDs: ["draft"], day: today, time: .scheduleDefault))
        XCTAssertFalse(model.scheduleOpen, "Done closes Schedule Tasks")
        XCTAssertEqual(model.rows.count, 4)
        XCTAssertEqual(model.rows.map(\.title).suffix(2), ["Draft project update", "Coffee chat with a mentor"],
                       "The 9:00 AM task sorts between the 8:00 AM rows and the 7:00 PM event")
        XCTAssertEqual(model.rows.first(where: { $0.title == "Draft project update" })?.timeLabel, "9:00 AM")
        XCTAssertEqual(model.entry.scheduleCount, 2)

        model.creationOpen = true
        model.create(AgendaCreationDraft(title: ""))
        XCTAssertTrue(model.creationOpen, "A blank draft is not saved")
        model.create(AgendaCreationDraft(mode: .event, title: "Evening rehearsal", calendar: .work))
        XCTAssertFalse(model.creationOpen, "Save closes the creation sheet")
        XCTAssertEqual(model.rows.map(\.title).suffix(2), ["Evening rehearsal", "Coffee chat with a mentor"])
    }

    func testEmptyDayBecomesPopulatedBySchedulingAndCreation() {
        let model = AgendaSuggestionsModel(fixture: .empty)
        XCTAssertTrue(model.rows.isEmpty)
        model.schedule(AgendaScheduleRequest(taskIDs: ["dentist"], day: today, time: .scheduleDefault))
        model.create(AgendaCreationDraft(title: "Prepare rehearsal notes"))
        XCTAssertEqual(model.rows.map(\.title), ["Book dentist appointment", "Prepare rehearsal notes"])
    }
}
