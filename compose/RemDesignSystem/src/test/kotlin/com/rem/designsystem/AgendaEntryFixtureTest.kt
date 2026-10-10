package com.rem.designsystem

import com.rem.designsystem.screens.AgendaCreationDraft
import com.rem.designsystem.screens.AgendaDay
import com.rem.designsystem.screens.AgendaEntryFixture
import com.rem.designsystem.screens.AgendaScheduleFilter
import com.rem.designsystem.screens.AgendaScheduleRequest
import com.rem.designsystem.screens.AgendaSuggestionsFixture
import com.rem.designsystem.screens.AgendaSuggestionsState
import com.rem.designsystem.screens.AgendaTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Twin of `AgendaEntryFixtureTests.swift`: Add New → creation (`2390:28498`) and Schedule → Schedule
 * Tasks (`2295:13691`). Events are never schedule candidates, filters are truthful, Done schedules the
 * selection together on the viewed day, Save applies only a valid draft, and labels match the authored
 * copy. Native journeys are covered by `PlaygroundNavigationTest`.
 */
class AgendaEntryFixtureTest {
    private val today = AgendaDay.FixtureToday

    // Schedule Tasks

    @Test fun candidatesExcludeEventsAndFollowTheAuthoredOrder() {
        val fixture = AgendaEntryFixture()
        assertTrue("The fixture carries an undated event", fixture.backlog.any { it.isEvent })
        assertEquals(listOf("draft", "specs", "dentist"), fixture.candidates(AgendaScheduleFilter.All).map { it.id })
        assertTrue("No filter ever lists an event", AgendaScheduleFilter.entries.none { f -> fixture.candidates(f).any { it.isEvent } })
        assertEquals("The Schedule badge counts tasks only", 3, fixture.scheduleCount)
    }

    @Test fun filtersMatchTheAuthoredStates() {
        val fixture = AgendaEntryFixture()
        assertEquals(listOf("Draft project update"), fixture.candidates(AgendaScheduleFilter.Inbox).map { it.title })
        assertEquals(listOf("Review design specs"), fixture.candidates(AgendaScheduleFilter.Overdue).map { it.title })
        assertEquals("Overdue · 2d", fixture.candidates(AgendaScheduleFilter.Overdue).first().detail)
        assertEquals("No tasks in Inbox", AgendaScheduleFilter.Inbox.emptyText)
        assertEquals(listOf("All", "Inbox", "Overdue"), AgendaScheduleFilter.entries.map { it.title })
    }

    @Test fun addToTodayLandsSelectedTaskAtNineOnTheViewedDay() {
        val (fixture, rows) = AgendaEntryFixture().schedule(AgendaScheduleRequest(listOf("draft"), today, AgendaTime.ScheduleDefault))
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("Draft project update", row.title)
        assertEquals("9:00 AM", row.timeLabel)
        assertEquals(9 * 60, row.sortMinutes)
        assertEquals(listOf("Inbox"), row.pills)
        assertFalse(row.isEvent)
        assertEquals("After scheduling the badge reads 2", 2, fixture.scheduleCount)
        assertTrue("Inbox becomes the empty filter", fixture.candidates(AgendaScheduleFilter.Inbox).isEmpty())
    }

    @Test fun planSchedulesTheSelectionTogetherAtThePickedSlot() {
        val (fixture, rows) = AgendaEntryFixture().schedule(AgendaScheduleRequest(listOf("draft", "specs"), today, AgendaTime(14, 30)))
        assertEquals(listOf("Draft project update", "Review design specs"), rows.map { it.title })
        assertEquals(setOf("2:30 PM"), rows.map { it.timeLabel }.toSet())
        assertEquals(listOf("dentist"), fixture.candidates(AgendaScheduleFilter.All).map { it.id })
    }

    @Test fun anotherDayLeavesTheBacklogButNotTheViewedAgenda() {
        val (fixture, rows) = AgendaEntryFixture().schedule(AgendaScheduleRequest(listOf("dentist"), today.plusDays(2), AgendaTime.ScheduleDefault))
        assertTrue("Another day's tasks are off-screen", rows.isEmpty())
        assertEquals(2, fixture.scheduleCount)
    }

    @Test fun eventsAndUnknownIdsAreNeverScheduled() {
        val (fixture, rows) = AgendaEntryFixture().schedule(AgendaScheduleRequest(listOf("walkthrough", "missing"), today, AgendaTime.ScheduleDefault))
        assertTrue(rows.isEmpty())
        assertEquals("Nothing changes", AgendaEntryFixture.ReferenceBacklog, fixture.backlog)
    }

    // Day / time labels

    @Test fun dayArithmeticAndLabels() {
        assertEquals("Aug 13 2026", today.headerText)
        assertEquals("August 13, 2026 is a Thursday", 4, today.weekday)
        assertEquals(today, AgendaDay.ofEpochDay(today.epochDay))
        assertEquals(0, AgendaDay(1970, 1, 1).epochDay)
        assertEquals(AgendaDay(2027, 1, 1), AgendaDay(2026, 12, 31).plusDays(1))
        assertEquals(AgendaDay(2028, 2, 29), AgendaDay(2028, 2, 28).plusDays(1))
        assertEquals("Today, August 13", today.dateRowText(today))
        assertEquals("Tomorrow, August 14", today.plusDays(1).dateRowText(today))
        assertEquals("Saturday, August 15", today.plusDays(2).dateRowText(today))
        assertEquals("Add to Today", today.addToLabel(today))
        assertEquals("Add to Tomorrow", today.plusDays(1).addToLabel(today))
        assertEquals("Add to Aug 15", today.plusDays(2).addToLabel(today))
        assertEquals("9:00 AM", AgendaTime.ScheduleDefault.label)
        assertEquals("12:05 AM", AgendaTime(0, 5).label)
        assertEquals("12:00 PM", AgendaTime(12, 0).label)
    }

    // Creation

    @Test fun blankDraftCannotSave() {
        assertFalse(AgendaCreationDraft().canSave)
        assertFalse(AgendaCreationDraft(title = "   ").canSave)
        assertNull(AgendaEntryFixture().create(AgendaCreationDraft(title = " ")).second)
    }

    @Test fun savedTaskKeepsItsListAtFivePM() {
        val draft = AgendaCreationDraft(
            mode = AgendaCreationDraft.Mode.Task, title = " Prepare rehearsal notes ", notes = "Bring the revised set list.",
            taskList = AgendaCreationDraft.TaskList.Work,
        )
        assertEquals("Work", draft.chooserLabel)
        assertEquals("Today, 5:00 PM", draft.whenSummary)
        assertEquals("30 min", draft.durationSummary)
        val (fixture, row) = AgendaEntryFixture().create(draft)
        assertFalse(row!!.isEvent)
        assertEquals("Prepare rehearsal notes", row.title)
        assertEquals("5:00 PM", row.timeLabel)
        assertEquals(listOf("Work"), row.pills)
        assertTrue("A just-saved task shows the pending ring", row.pending)
        assertEquals("Creating never touches the schedule backlog", 3, fixture.scheduleCount)
    }

    @Test fun noListTaskHasNoPill() {
        assertEquals("Select task list", AgendaCreationDraft().chooserLabel)
        assertEquals(emptyList<String>(), AgendaEntryFixture().create(AgendaCreationDraft(title = "Prepare rehearsal notes")).second!!.pills)
    }

    @Test fun savedEventKeepsItsCalendarAtSixPM() {
        val draft = AgendaCreationDraft(mode = AgendaCreationDraft.Mode.Event, title = "Evening rehearsal",
            calendar = AgendaCreationDraft.EventCalendar.Work)
        assertEquals("Today, 6:00–7:00 PM", draft.whenSummary)
        assertEquals("1 hr", draft.durationSummary)
        val row = AgendaEntryFixture().create(draft).second!!
        assertTrue(row.isEvent)
        assertEquals("6:00 PM", row.timeLabel)
        assertEquals(listOf("Work"), row.pills)
        assertEquals(AgendaCreationDraft.EventCalendar.Work, row.calendar)
        assertFalse(row.pending)
    }

    @Test fun createdRowsHaveDistinctIds() {
        val (once, first) = AgendaEntryFixture().create(AgendaCreationDraft(title = "One"))
        val (_, second) = once.create(AgendaCreationDraft(title = "One"))
        assertNotEquals(first!!.id, second!!.id)
    }

    // Playground host

    @Test fun playgroundHostAppliesResultsInMemory() {
        val state = AgendaSuggestionsState(AgendaSuggestionsFixture.Loaded)
        state.scheduleOpen = true
        state.schedule(AgendaScheduleRequest(listOf("draft"), today, AgendaTime.ScheduleDefault))
        assertFalse("Done closes Schedule Tasks", state.scheduleOpen)
        assertEquals(
            listOf("Reply to the venue", "Confirm rehearsal time", "Draft project update", "Coffee chat with a mentor"),
            state.rows.map { it.title },
        )
        assertEquals(2, state.entry.scheduleCount)

        state.creationOpen = true
        state.create(AgendaCreationDraft(title = ""))
        assertTrue("A blank draft is not saved", state.creationOpen)
        state.create(AgendaCreationDraft(mode = AgendaCreationDraft.Mode.Event, title = "Evening rehearsal",
            calendar = AgendaCreationDraft.EventCalendar.Work))
        assertFalse("Save closes the creation sheet", state.creationOpen)
        assertEquals(listOf("Evening rehearsal", "Coffee chat with a mentor"), state.rows.map { it.title }.takeLast(2))
    }

    @Test fun emptyDayBecomesPopulatedBySchedulingAndCreation() {
        val state = AgendaSuggestionsState(AgendaSuggestionsFixture.Empty)
        assertTrue(state.rows.isEmpty())
        state.schedule(AgendaScheduleRequest(listOf("dentist"), today, AgendaTime.ScheduleDefault))
        state.create(AgendaCreationDraft(title = "Prepare rehearsal notes"))
        assertEquals(listOf("Book dentist appointment", "Prepare rehearsal notes"), state.rows.map { it.title })
    }
}
