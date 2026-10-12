package com.rem.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.brand.RemAvatarSlot
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.chat.ChatHeader
import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.navigation.RemSidebar
import com.rem.designsystem.primitives.RemSegmentedPicker
import com.rem.designsystem.primitives.RemSegmentedPickerStyle
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventLeadingStyle
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.screens.AgendaCreationContent
import com.rem.designsystem.screens.AgendaCreationDraft
import com.rem.designsystem.screens.AgendaEntryFixture
import com.rem.designsystem.screens.AgendaScheduleContent
import com.rem.designsystem.screens.AgendaScheduleFilter
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * Evidence for the May adoption-parity changes (TaskEventRow app time column + completed ring, the
 * Schedule sheet's empty states, RemSegmentedPicker, the avatar slot). Paired with the SwiftUI
 * `AdoptionParityRenderSnapshots.swift` renders of the same names where the component exists on iOS.
 */
internal class AdoptionParityEvidence(private val paparazzi: Paparazzi) {
    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    fun taskEventRowApp() {
        shot("TaskEventRow-app-light") { RemTheme { taskEventRows() } }
        shot("TaskEventRow-app-dark") { RemTheme(darkTheme = true) { taskEventRows() } }
    }

    @Composable
    private fun taskEventRows() {
        val colors = RemColors.current
        Column(Modifier.fillMaxSize().background(colors.backgroundPrimary).padding(16.dp)) {
            caption("Compact (default, unchanged)")
            TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"))
            caption("App parity (leadingStyle = App)")
            val app = TaskEventLeadingStyle.App
            TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"), leadingStyle = app)
            TaskEventRow(kind = TaskEventKind.Event(colors.systemBlue), title = "Team standup", leading = TaskEventLeading.Time("14:30"), pills = listOf("Work"), leadingStyle = app)
            TaskEventRow(kind = TaskEventKind.Task, title = "Send the venue deposit", leading = TaskEventLeading.Time("8:00 AM"), completed = true, leadingStyle = app)
            TaskEventRow(kind = TaskEventKind.Task, title = "Unfiled inbox task", leading = TaskEventLeading.Schedule, showPills = false, leadingStyle = app)
            TaskEventRow(kind = TaskEventKind.Task, title = "Anytime task", leading = TaskEventLeading.Clock, showPills = false, leadingStyle = app)
            TaskEventRow(kind = TaskEventKind.Task, title = "Unparseable label falls back", leading = TaskEventLeading.Time("Oct 14"), showPills = false, leadingStyle = app)
            caption("Completed, compact")
            TaskEventRow(kind = TaskEventKind.Task, title = "Book the rehearsal room", leading = TaskEventLeading.Time("10:00"), completed = true)
        }
    }

    fun segmentedPicker() {
        shot("RemSegmentedPicker-light") { RemTheme { segmentedGallery() } }
        shot("RemSegmentedPicker-dark") { RemTheme(darkTheme = true) { segmentedGallery() } }
    }

    @Composable
    private fun segmentedGallery() {
        Column(
            Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            caption("Ios (the parity alternate)")
            RemSegmentedPicker(AgendaCreationDraft.Mode.entries, AgendaCreationDraft.Mode.Task, {}, { it.title })
            RemSegmentedPicker(AgendaScheduleFilter.entries, AgendaScheduleFilter.Inbox, {}, { it.title })
            RemSegmentedPicker(AgendaScheduleFilter.entries, AgendaScheduleFilter.Overdue, {}, { it.title }, enabled = false)
            caption("Material (Rem's sheets, default)")
            RemSegmentedPicker(AgendaScheduleFilter.entries, AgendaScheduleFilter.Inbox, {}, { it.title }, style = RemSegmentedPickerStyle.Material)
        }
    }

    fun agendaScheduleEmpty() {
        AgendaScheduleFilter.entries.forEach { filter ->
            shot("AgendaScheduleEmpty-${filter.name.lowercase()}-light") {
                RemTheme {
                    Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
                        AgendaScheduleContent(
                            fixture = AgendaEntryFixture(backlog = emptyList()),
                            initialFilter = filter,
                            segmentedStyle = RemSegmentedPickerStyle.Ios,
                        )
                    }
                }
            }
        }
        // Rem's default (Material segmented buttons), unchanged apart from the empty state.
        shot("AgendaScheduleEmpty-inbox-material-light") {
            RemTheme {
                Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
                    AgendaScheduleContent(fixture = AgendaEntryFixture(backlog = emptyList()), initialFilter = AgendaScheduleFilter.Inbox)
                }
            }
        }
    }

    fun agendaSheetsIos() {
        shot("AgendaCreation-ios-light") { RemTheme { creation() } }
        shot("AgendaCreation-ios-dark") { RemTheme(darkTheme = true) { creation() } }
        shot("AgendaSchedule-ios-light") {
            RemTheme {
                Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
                    AgendaScheduleContent(fixture = AgendaEntryFixture(), segmentedStyle = RemSegmentedPickerStyle.Ios)
                }
            }
        }
    }

    @Composable
    private fun creation() {
        Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
            AgendaCreationContent(
                initialDraft = AgendaCreationDraft(title = "Prepare rehearsal notes", notes = "Bring the revised set list."),
                segmentedStyle = RemSegmentedPickerStyle.Ios,
            )
        }
    }

    fun customAvatar() {
        shot("ChatHeader-customAvatar-light") {
            RemTheme {
                Column(
                    Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    caption("Default: the Rem face")
                    ChatHeader(activity = "Connected")
                    caption("avatar = sample mascot (idle, thinking)")
                    ChatHeader(activity = "Connected", name = "May", avatar = SampleMascot, onMenu = {}, onOverflow = {})
                    ChatHeader(activity = "Reading the shared notes", name = "May", faceMode = RemFaceMarkMode.Thinking,
                        status = ChatHeaderStatus.Connected, avatar = SampleMascot)
                }
            }
        }
        shot("Sidebar-customAvatar-light") {
            RemTheme {
                Box(Modifier.width(332.dp)) {
                    RemSidebar(display = SidebarFixtures.sidebar, onAction = {}, mainAgentAvatar = SampleMascot)
                }
            }
        }
    }

    @Composable
    private fun caption(text: String) {
        Text(text, style = RemTypography.caption1, color = RemColors.current.labelSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = RemSpacing.lg, vertical = RemSpacing.xs))
    }

    companion object {
        /**
         * Test-only stand-in for an adopting product's mascot (a gray two-humped figure with black oval
         * eyes, after May's): it proves the slot, not the art. Thinking glances up.
         */
        val SampleMascot: RemAvatarSlot = { mode, size -> SampleMascotDrawing(mode, size) }

        @Composable
        private fun SampleMascotDrawing(mode: RemFaceMarkMode, size: Dp) {
            Canvas(Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height
                val gray = Color(0xFF9A9AA0)
                drawCircle(gray, radius = w * 0.24f, center = Offset(w * 0.32f, h * 0.36f))
                drawCircle(gray, radius = w * 0.24f, center = Offset(w * 0.68f, h * 0.36f))
                drawRoundRect(gray, topLeft = Offset(w * 0.08f, h * 0.36f), size = Size(w * 0.84f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f))
                val eyeY = if (mode == RemFaceMarkMode.Thinking) h * 0.46f else h * 0.54f
                listOf(0.38f, 0.62f).forEach { x ->
                    drawOval(Color.Black, topLeft = Offset(w * x - w * 0.05f, eyeY - h * 0.08f), size = Size(w * 0.1f, h * 0.16f))
                }
            }
        }
    }
}
