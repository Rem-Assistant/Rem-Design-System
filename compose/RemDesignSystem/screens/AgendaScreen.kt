package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.agenda.DateNavigationHeader
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **AgendaScreen** — Compose sibling of the SwiftUI [AgendaScreen]. A [DateNavigationHeader], a
 * divider, then the day's [TaskEventRow]s in a scroll (or an empty state), supplied by the host through
 * [content]. Platform chrome comes from the OS, matching the shipping `SharedAgendaView`. Authority:
 * Agenda screen (Figma page "Agenda", scenarios `530:22`) + `SharedAgendaView.swift`.
 */
@Composable
fun AgendaScreen(
    dateText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Today",
    onCalendarTap: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        DateNavigationHeader(
            dateText = dateText,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            title = title,
            onCalendarTap = onCalendarTap,
        )
        Column(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator)) {}
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content()
        }
    }
}

@Preview(name = "AgendaScreen", showBackground = true, widthDp = 402, heightDp = 700)
@Composable
private fun AgendaScreenPreview() {
    RemTheme {
        AgendaScreen(dateText = "Oct 1 2026", onPrevious = {}, onNext = {}) {
            TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"))
            TaskEventRow(kind = TaskEventKind.Event(RemColors.current.systemBlue), title = "Team standup", leading = TaskEventLeading.Time("10:30"), pills = listOf("Work"))
            TaskEventRow(kind = TaskEventKind.Task, title = "Draft the investor update", leading = TaskEventLeading.Time("14:00"), pills = listOf("Fundraise"))
        }
    }
}
