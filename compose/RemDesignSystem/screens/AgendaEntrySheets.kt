package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonSize
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

private const val MillisPerDay = 86_400_000L

/** Personal → system blue, Work → system orange (the saved-event bar colors in `2390:28498`). */
@Composable
internal fun calendarColor(calendar: AgendaCreationDraft.EventCalendar?): Color {
    val colors = RemColors.current
    return if (calendar == AgendaCreationDraft.EventCalendar.Work) colors.systemOrange else colors.systemBlue
}

/** Cancel / title / confirm, the sheet header shared by creation and Schedule Tasks. */
@Composable
private fun SheetHeader(
    title: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { leading() }
        Text(title, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

// MARK: - Add New · creation sheet

/**
 * **AgendaCreationSheet** — Compose twin of the SwiftUI `AgendaCreationSheet`. Add New's destination:
 * the authored creation form (Creation `2390:28498`; the create menu's "New Task or Event" `2049:10336`)
 * in a native modal bottom sheet. Save returns the draft to the host, which applies it in memory; Cancel
 * (or dismissing the sheet) leaves the Agenda unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaCreationSheet(
    onCancel: () -> Unit,
    onSave: (AgendaCreationDraft) -> Unit,
    initialDraft: AgendaCreationDraft = AgendaCreationDraft(),
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RemColors.current.backgroundPrimary,
    ) {
        AgendaCreationContent(initialDraft = initialDraft, onCancel = onCancel, onSave = onSave)
    }
}

/**
 * The creation form: Cancel / **New Task or Event** / Save, a New Task · New Event segmented control,
 * the title with its marker (task ring or event bar), the task-list or calendar chooser, the inline
 * date/time, duration, alert and repeat summaries, and notes. The summaries are read-only here.
 */
@Composable
fun AgendaCreationContent(
    modifier: Modifier = Modifier,
    initialDraft: AgendaCreationDraft = AgendaCreationDraft(),
    onCancel: () -> Unit = {},
    onSave: (AgendaCreationDraft) -> Unit = {},
) {
    val colors = RemColors.current
    var draft by remember { mutableStateOf(initialDraft) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = RemSpacing.xl)) {
        SheetHeader(
            title = "New Task or Event",
            leading = {
                RemButton("Cancel", onCancel, variant = RemButtonVariant.TextAccent, size = RemButtonSize.Compact,
                    modifier = Modifier.testTag("agendaCreate.cancel"))
            },
            trailing = {
                RemButton("Save", { onSave(draft) }, variant = RemButtonVariant.TextAccent, size = RemButtonSize.Compact,
                    enabled = draft.canSave, modifier = Modifier.testTag("agendaCreate.save"))
            },
        )
        Column(
            modifier = Modifier.padding(horizontal = RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            Segmented(
                options = AgendaCreationDraft.Mode.entries,
                selected = draft.mode,
                label = { it.title },
                tag = { "agendaCreate.mode.${it.name}" },
                onSelect = { draft = draft.copy(mode = it) },
            )
            Row(
                modifier = Modifier.padding(top = RemSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
            ) {
                if (draft.mode == AgendaCreationDraft.Mode.Task) {
                    Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = colors.labelSecondary,
                        modifier = Modifier.size(28.dp))
                } else {
                    Box(Modifier.width(4.dp).height(30.dp).background(calendarColor(draft.calendar), RoundedCornerShape(2.dp)))
                }
                BasicTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    singleLine = true,
                    textStyle = RemTypography.title1Bold.copy(color = colors.labelPrimary),
                    cursorBrush = SolidColor(colors.brandBlue),
                    modifier = Modifier.weight(1f).testTag("agendaCreate.title"),
                    decorationBox = { inner ->
                        if (draft.title.isEmpty()) {
                            Text(
                                if (draft.mode == AgendaCreationDraft.Mode.Task) "Task title" else "Event title",
                                style = RemTypography.title1Bold,
                                color = colors.labelTertiary,
                            )
                        }
                        inner()
                    },
                )
            }
            CreationChooser(draft = draft, onDraftChange = { draft = it })
            Column(
                modifier = Modifier.padding(vertical = RemSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                Summary(Icons.Filled.Schedule, draft.whenSummary)
                Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                    Summary(Icons.Filled.Alarm, draft.durationSummary)
                    Summary(Icons.Filled.Notifications, "No alert")
                    Summary(Icons.Filled.Repeat, "No repeat")
                }
            }
            BasicTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                textStyle = RemTypography.body.copy(color = colors.labelPrimary),
                cursorBrush = SolidColor(colors.brandBlue),
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = RemSpacing.xs).testTag("agendaCreate.notes"),
                decorationBox = { inner ->
                    if (draft.notes.isEmpty()) Text("Notes", style = RemTypography.body, color = colors.labelTertiary)
                    inner()
                },
            )
        }
    }
}

/** The designed chooser: No List / Follow-ups / Work for tasks; Personal / Work for events. */
@Composable
private fun CreationChooser(draft: AgendaCreationDraft, onDraftChange: (AgendaCreationDraft) -> Unit) {
    val colors = RemColors.current
    var expanded by remember { mutableStateOf(false) }
    val isTask = draft.mode == AgendaCreationDraft.Mode.Task
    Box {
        Row(
            modifier = Modifier
                .background(colors.fillTertiary, CircleShape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = RemSpacing.sm, vertical = RemSpacing.xs)
                .testTag("agendaCreate.chooser"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            val tint = if (isTask && draft.taskList == AgendaCreationDraft.TaskList.NoList) colors.labelSecondary else colors.labelPrimary
            if (isTask) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            } else {
                Box(Modifier.size(10.dp).background(calendarColor(draft.calendar), CircleShape))
            }
            Text(draft.chooserLabel, style = RemTypography.subheadline, color = tint)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isTask) {
                AgendaCreationDraft.TaskList.entries.forEach { list ->
                    ChooserItem(list.title, list == draft.taskList, "agendaCreate.option.${list.name}") {
                        onDraftChange(draft.copy(taskList = list)); expanded = false
                    }
                }
            } else {
                AgendaCreationDraft.EventCalendar.entries.forEach { calendar ->
                    ChooserItem(calendar.title, calendar == draft.calendar, "agendaCreate.option.${calendar.name}",
                        dot = calendarColor(calendar)) {
                        onDraftChange(draft.copy(calendar = calendar)); expanded = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooserItem(title: String, checked: Boolean, tag: String, dot: Color? = null, onClick: () -> Unit) {
    val colors = RemColors.current
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                if (dot != null) Box(Modifier.size(10.dp).background(dot, CircleShape))
                Text(title, style = RemTypography.body, color = colors.labelPrimary)
            }
        },
        leadingIcon = {
            if (checked) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.labelPrimary)
            else Spacer(Modifier.size(24.dp))
        },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun Summary(icon: ImageVector, text: String) {
    val colors = RemColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
        Icon(icon, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = RemTypography.body, color = colors.labelSecondary)
    }
}

/** Material 3 single-choice segmented buttons, the Android counterpart of the iOS `.segmented` picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    tag: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.testTag(tag(option)),
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

// MARK: - Schedule · Schedule Tasks sheet

private enum class ScheduleStep(val title: String) { Select("Schedule Tasks"), PickDate("Pick a Date"), PickTime("Pick a Time") }

/**
 * **AgendaScheduleSheet** — Compose twin of the SwiftUI `AgendaScheduleSheet`. Schedule's destination:
 * **Schedule Tasks** (`2295:13691`) in a native modal bottom sheet. All / Inbox / Overdue filters over
 * existing tasks (events are never listed); selection survives filter changes. **Add to Today** (or
 * Tomorrow / a date) opens **Pick a Time** for the viewed day; **Plan** opens **Pick a Date**, then Next →
 * **Pick a Time**, whose Date row expands the calendar. Done schedules every selected task together
 * (default 9:00 AM); close leaves the Agenda unchanged. Material 3 `DatePicker` / `TimePicker` render
 * the native calendar and clock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScheduleSheet(
    fixture: AgendaEntryFixture,
    onCancel: () -> Unit,
    onDone: (AgendaScheduleRequest) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RemColors.current.backgroundPrimary,
    ) {
        AgendaScheduleContent(fixture = fixture, onCancel = onCancel, onDone = onDone)
    }
}

/** The Schedule Tasks steps. Rendered inside [AgendaScheduleSheet]; also the headless snapshot surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScheduleContent(
    fixture: AgendaEntryFixture,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onDone: (AgendaScheduleRequest) -> Unit = {},
) {
    val colors = RemColors.current
    var filter by remember { mutableStateOf(AgendaScheduleFilter.All) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var step by remember { mutableStateOf(ScheduleStep.Select) }
    var dateExpanded by remember { mutableStateOf(false) }
    // Material's DatePicker speaks UTC-midnight millis; AgendaDay is the zone-free day it names.
    val dateState = rememberDatePickerState(initialSelectedDateMillis = fixture.viewedDay.epochDay * MillisPerDay)
    val timeState = rememberTimePickerState(
        initialHour = AgendaTime.ScheduleDefault.hour,
        initialMinute = AgendaTime.ScheduleDefault.minute,
        is24Hour = false,
    )
    val day = dateState.selectedDateMillis?.let { AgendaDay.ofEpochDay(Math.floorDiv(it, MillisPerDay).toInt()) }
        ?: fixture.viewedDay
    val request = {
        AgendaScheduleRequest(
            taskIds = fixture.candidates(AgendaScheduleFilter.All).map { it.id }.filter { it in selected },
            day = day,
            time = AgendaTime(timeState.hour, timeState.minute),
        )
    }

    Column(modifier.fillMaxWidth().testTag("agendaSchedule.sheet")) {
        SheetHeader(
            title = step.title,
            leading = {
                IconButton(onClick = onCancel, modifier = Modifier.testTag("agendaSchedule.close")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = colors.labelSecondary)
                }
            },
            trailing = {
                when (step) {
                    ScheduleStep.Select -> Unit
                    ScheduleStep.PickDate -> RemButton("Next", { step = ScheduleStep.PickTime },
                        variant = RemButtonVariant.TextAccent, size = RemButtonSize.Compact,
                        modifier = Modifier.testTag("agendaSchedule.next"))
                    ScheduleStep.PickTime -> RemButton("Done", { onDone(request()) },
                        variant = RemButtonVariant.TextAccent, size = RemButtonSize.Compact,
                        modifier = Modifier.testTag("agendaSchedule.done"))
                }
            },
        )
        when (step) {
            ScheduleStep.Select -> Column(
                modifier = Modifier.padding(horizontal = RemSpacing.lg).padding(top = RemSpacing.sm, bottom = RemSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
            ) {
                Segmented(
                    options = AgendaScheduleFilter.entries,
                    selected = filter,
                    label = { it.title },
                    tag = { "agendaSchedule.filter.${it.name}" },
                    onSelect = { filter = it },
                )
                val tasks = fixture.candidates(filter)
                if (tasks.isEmpty()) {
                    Text(
                        filter.emptyText,
                        style = RemTypography.footnote,
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.medium))
                            .padding(vertical = 35.dp)
                            .testTag("agendaSchedule.empty"),
                    )
                } else {
                    Column(Modifier.fillMaxWidth().background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.medium))) {
                        tasks.forEachIndexed { index, task ->
                            if (index > 0) HorizontalDivider(Modifier.padding(start = RemSpacing.lg), color = colors.separator)
                            ScheduleTaskRow(task, task.id in selected) {
                                selected = if (task.id in selected) selected - task.id else selected + task.id
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                    RemButton(
                        text = fixture.viewedDay.addToLabel(fixture.today),
                        onClick = {
                            dateState.selectedDateMillis = fixture.viewedDay.epochDay * MillisPerDay
                            step = ScheduleStep.PickTime
                        },
                        variant = RemButtonVariant.RectBlue,
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("agendaSchedule.addTo"),
                    )
                    RemButton(
                        text = "Plan",
                        onClick = { step = ScheduleStep.PickDate },
                        variant = RemButtonVariant.RectSecondary,
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("agendaSchedule.plan"),
                    )
                }
            }
            ScheduleStep.PickDate -> Column(Modifier.verticalScroll(rememberScrollState())) {
                DatePicker(
                    state = dateState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    modifier = Modifier.testTag("agendaSchedule.datePicker"),
                )
            }
            ScheduleStep.PickTime -> Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.medium))
                        .clickable(role = Role.Button) { dateExpanded = !dateExpanded }
                        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md)
                        .testTag("agendaSchedule.dateRow"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Date", style = RemTypography.body, color = colors.labelPrimary, modifier = Modifier.weight(1f))
                    Text(day.dateRowText(fixture.today), style = RemTypography.body, color = colors.labelSecondary)
                }
                if (dateExpanded) {
                    DatePicker(
                        state = dateState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        modifier = Modifier.testTag("agendaSchedule.datePicker"),
                    )
                }
                TimePicker(state = timeState, modifier = Modifier.testTag("agendaSchedule.timePicker"))
            }
        }
    }
}

/** `ScheduleTaskRow` (`2302:11932` / `2302:11938`): title, metadata, selection glyph. */
@Composable
private fun ScheduleTaskRow(task: AgendaBacklogItem, isSelected: Boolean, onToggle: () -> Unit) {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { selected = isSelected }
            .padding(horizontal = RemSpacing.lg, vertical = 11.dp)
            .testTag("agendaSchedule.task.${task.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(task.title, style = RemTypography.body, color = colors.labelPrimary)
            Text(task.detail, style = RemTypography.subheadline, color = colors.labelSecondary)
        }
        Icon(
            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) colors.brandBlue else colors.labelTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(name = "AgendaCreation — task", showBackground = true, widthDp = 402)
@Composable
private fun AgendaCreationPreview() {
    RemTheme {
        AgendaCreationContent(initialDraft = AgendaCreationDraft(title = "Prepare rehearsal notes", notes = "Bring the revised set list."))
    }
}

@Preview(name = "AgendaSchedule — select", showBackground = true, widthDp = 402)
@Composable
private fun AgendaSchedulePreview() {
    RemTheme { AgendaScheduleContent(fixture = AgendaEntryFixture()) }
}
