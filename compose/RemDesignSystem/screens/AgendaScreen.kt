package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.agentsurfaces.BriefCounts
import com.rem.designsystem.agentsurfaces.DailyBriefCard
import com.rem.designsystem.agenda.DateNavigationHeader
import com.rem.designsystem.rows.SuggestionSection
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.rows.TaskSuggestion
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **AgendaScreen** — Compose sibling of the SwiftUI [AgendaScreen]. A [DateNavigationHeader], a
 * divider, then the day's content in a scroll — supplied by the host through [content]. Platform chrome
 * (status bar, nav bar) comes from the OS, matching the shipping `SharedAgendaView`.
 *
 * For the full Today composition — Daily Brief, task/event rows, the add-a-task field, and the
 * Suggestions section assembled from DS components — see `AgendaScreenTodayPreview` below; it is the
 * worked reference the Android app mirrors (and the Paparazzi evidence). Authority: Agenda screen
 * (Figma page "Agenda") + `SharedAgendaView.swift`.
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

/**
 * **AddTaskField** — the inline "Add a task" composer at the foot of the day: a rounded field + a
 * brandBlue **Add** action. Token-only. Stateless — the host owns [text].
 */
@Composable
fun AddTaskField(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add a task",
) {
    val colors = RemColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.large))
                .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = RemTypography.body.copy(color = colors.labelPrimary),
                cursorBrush = SolidColor(colors.brandBlue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        androidx.compose.material3.Text(
                            placeholder,
                            style = RemTypography.body,
                            color = colors.labelTertiary,
                        )
                    }
                    inner()
                },
            )
        }
        androidx.compose.material3.Text(
            text = "Add",
            style = RemTypography.body.copy(fontWeight = FontWeight.Bold),
            color = colors.brandBlue,
            modifier = Modifier.clickable(onClick = onAdd).padding(RemSpacing.xs),
        )
    }
}

@Preview(name = "AgendaScreen — Today", showBackground = true, widthDp = 402, heightDp = 860)
@Composable
private fun AgendaScreenTodayPreview() {
    RemTheme {
        var draft by remember { mutableStateOf("") }
        val events = RemColors.current.systemBlue
        AgendaScreen(dateText = "Oct 2 2026", onPrevious = {}, onNext = {}) {
            DailyBriefCard(
                title = "Daily brief",
                summary = "Sent to damilola.ogunnaike@gmail.com at 9:37 am — “Hi Damilola, I’ll send " +
                    "you the notes from yesterday’s call before 2pm. Best, Larissa.” 1 task overdue needs attention.",
                counts = BriefCounts(done = 0, total = 0),
                onTap = {},
                modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            )
            TaskEventRow(
                kind = TaskEventKind.Task,
                title = "Send yesterday’s call notes",
                pills = listOf("Overdue"),
            )
            TaskEventRow(
                kind = TaskEventKind.Event(events),
                title = "National Day",
                pills = listOf("Holidays in Nigeria"),
            )
            TaskEventRow(
                kind = TaskEventKind.Event(events),
                title = "National Day",
                pills = listOf("Holidays in Nigeria"),
            )
            AddTaskField(
                text = draft,
                onTextChange = { draft = it },
                onAdd = {},
                modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
            )
            SuggestionSection(
                suggestions = listOf(
                    TaskSuggestion("Set up the TestFlight pipeline using ASC CLI", "Samuel · Granola · 8h ago"),
                    TaskSuggestion("Pull the Claude/DSFlows branch and confirm the Compose UI renders", "Damilola · Granola · 8h ago"),
                    TaskSuggestion("Continue applying to other programs", "Larissa · Gmail · 1d ago"),
                ),
                onAccept = {},
                onDismiss = {},
                modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            )
        }
    }
}
