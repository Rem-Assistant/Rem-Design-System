package com.rem.designsystem

import com.rem.designsystem.rows.TaskEventClockFace
import com.rem.designsystem.screens.AgendaScheduleFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app-parity time column's pure contract: RemClaw `TaskEventRowView.swift` `formatHour` (two-digit
 * 12-hour, 0 → 12), `formatMinutes` and `formatAMPM`, and its 64pt column. Paired with the SwiftUI
 * `TaskEventClockFaceTests.swift`, which asserts the same cases.
 */
class TaskEventClockFaceTest {
    private fun face(label: String) = TaskEventClockFace.parse(label)

    @Test fun columnIsTheAppsSixtyFourPoints() {
        assertEquals(64f, TaskEventClockFace.AppColumnWidth.value)
    }

    @Test fun twentyFourHourLabelsBecomeTheAppsTwelveHourFace() {
        assertEquals(TaskEventClockFace("09", "00", "AM"), face("9:00"))
        assertEquals(TaskEventClockFace("02", "30", "PM"), face("14:30"))
        assertEquals(TaskEventClockFace("12", "05", "AM"), face("0:05"))
        assertEquals(TaskEventClockFace("12", "00", "PM"), face("12:00"))
        assertEquals(TaskEventClockFace("11", "59", "PM"), face("23:59"))
    }

    @Test fun meridiemLabelsKeepTheirHalfOfTheDay() {
        assertEquals(TaskEventClockFace("08", "00", "AM"), face("8:00 AM"))
        assertEquals(TaskEventClockFace("07", "00", "PM"), face("7:00 pm"))
        assertEquals(TaskEventClockFace("12", "15", "AM"), face("12:15 AM"))
        assertEquals(TaskEventClockFace("12", "15", "PM"), face("12:15PM"))
        // iOS / ICU formatters put a narrow no-break space before AM/PM.
        assertEquals(TaskEventClockFace("05", "00", "PM"), face("5:00 PM"))
    }

    @Test fun unreadableLabelsFallBackToTheCompactLabel() {
        listOf("Oct 14", "", "9", "24:00", "9:60", "13:00 PM", "0:30 AM", "9:5", "+9:00", "9:00 noon").forEach {
            assertNull("'$it' is not a clock time", face(it))
        }
    }

    @Test fun spokenTimeIsOnePhrase() {
        assertEquals("9:00 AM", face("9:00")!!.spoken)
        assertEquals("2:30 PM", face("14:30")!!.spoken)
    }

    @Test fun everyScheduleFilterHasAnEmptyStateMessage() {
        AgendaScheduleFilter.entries.forEach { assertTrue(it.name, it.emptyMessage.isNotBlank()) }
        assertEquals("Tasks you capture without a date land here.", AgendaScheduleFilter.Inbox.emptyMessage)
    }
}
