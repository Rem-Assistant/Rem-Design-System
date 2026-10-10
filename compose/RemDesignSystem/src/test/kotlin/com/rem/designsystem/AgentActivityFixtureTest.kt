package com.rem.designsystem

import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.screens.AgentActivityCurrent
import com.rem.designsystem.screens.AgentActivityDay
import com.rem.designsystem.screens.AgentActivityDisplay
import com.rem.designsystem.screens.AgentActivityEvent
import com.rem.designsystem.screens.AgentActivityFixture
import com.rem.designsystem.screens.AgentActivityTab
import com.rem.designsystem.screens.ChatHeaderDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Twin of `AgentActivityFixtureTests.swift`: the current state comes only from the host's current
 * value (never a timeline event), the history is neutral and well-formed, and Approvals stays a labeled
 * data gap. The native journey is covered by the Playground navigation test.
 */
class AgentActivityFixtureTest {
    @Test fun currentStateMirrorsTheChatHeader() {
        val header = ChatHeaderDisplay(activity = "Reading the shared notes", status = ChatHeaderStatus.NeedsYou, isWorking = true)
        val current = AgentActivityCurrent.of(header)
        assertEquals(AgentActivityCurrent("Reading the shared notes", "Rem", ChatHeaderStatus.NeedsYou, isWorking = true), current)
        assertEquals(current, AgentActivityFixture.of(header).display.current)
    }

    @Test fun historyNeverOverwritesTheCurrentState() {
        val current = AgentActivityCurrent("Connected")
        val newest = AgentActivityEvent("new", "Send the weekly summary", "Needs you", "11:00 AM")
        val days = AgentActivityFixture.Days.mapIndexed { i, day -> if (i == 0) day.copy(events = listOf(newest) + day.events) else day }
        var fixture = AgentActivityFixture.of(current, days)
        assertEquals("A newer event does not become the current activity", current, fixture.display.current)
        for (tab in AgentActivityTab.entries) {
            fixture = fixture.select(tab)
            assertEquals("Switching sections keeps the current state", current, fixture.display.current)
        }
        assertEquals("Selecting a tab never rewrites history", days, fixture.display.days)
    }

    @Test fun fixtureHistoryIsNeutralDayGroupedAndComplete() {
        val days = AgentActivityFixture.Days
        assertEquals(listOf("Today", "Yesterday"), days.map { it.title })
        val events = days.flatMap { it.events }
        assertEquals("Unique row ids", events.size, events.map { it.id }.toSet().size)
        for (event in events) {
            assertTrue(event.title.isNotEmpty() && event.outcome.isNotEmpty() && event.time.isNotEmpty())
            assertEquals("${event.outcome} · ${event.time}", event.summary)
        }
        // The Figma master's personal sample copy is not reproduced.
        val copy = events.joinToString(" ") { "${it.title} ${it.outcome}" }
        for (sample in listOf("RFE", "Gmail", "Build RFE checklist", "Prepare next steps", "Check project status")) {
            assertFalse(sample, copy.contains(sample))
        }
    }

    @Test fun tabsDefaultToActivityAndMatchTheSegmentedControl() {
        assertEquals(listOf("Activity", "Approvals"), AgentActivityTab.entries.map { it.label })
        val fixture = AgentActivityFixture.of(AgentActivityCurrent("Connected"))
        assertEquals(AgentActivityTab.Activity, fixture.selectedTab)
        assertEquals(AgentActivityTab.Approvals, fixture.select(AgentActivityTab.Approvals).selectedTab)
    }

    @Test fun approvalsIsALabeledDataGapNotAnEmptyList() {
        assertTrue(AgentActivityDisplay.ApprovalsGapTitle.isNotEmpty())
        assertTrue(AgentActivityDisplay.ApprovalsGapMessage.contains("doesn't mean nothing needs your approval"))
    }

    @Test fun emptyDaysDrawNoHeaders() {
        val display = AgentActivityDisplay(AgentActivityCurrent("Connected"), listOf(
            AgentActivityDay("today", "Today", emptyList()),
            AgentActivityFixture.Days[1],
        ))
        assertEquals(listOf("yesterday"), display.visibleDays.map { it.id })
        assertTrue(AgentActivityDisplay(display.current, emptyList()).visibleDays.isEmpty())
    }

    @Test fun screenTitleAndBackDestination() {
        assertEquals("Agent activity", AgentActivityDisplay.Title)
        assertEquals("Chat", AgentActivityDisplay.BackTitle)
    }
}
