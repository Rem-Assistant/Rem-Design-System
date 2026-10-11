package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.agenda.DateNavigationHeader
import com.rem.designsystem.primitives.RemContentUnavailableView
import com.rem.designsystem.rows.SuggestedTaskRow
import com.rem.designsystem.rows.SuggestionAccept
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.rows.TaskSuggestion
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography
import kotlinx.coroutines.delay

/** Compose sibling of the SwiftUI `AgendaSuggestionsFixture`. */
enum class AgendaSuggestionsFixture { Loaded, Empty, None, Restoration }

/** One live Agenda suggestion; `id` is stable so optimistic add/move/dismiss preserve identity. */
data class AgendaSuggestionData(
    val id: String,
    val accept: SuggestionAccept,
    val title: String,
    val metadata: String,
)

/** The four authored suggestions, in order. Three render inline; the fourth lives in overflow. */
val AgendaReferenceSuggestions: List<AgendaSuggestionData> = listOf(
    AgendaSuggestionData("prep", SuggestionAccept.Add, "Prep for tonight’s rehearsal", "Your calendar has rehearsal at 6:00 PM."),
    AgendaSuggestionData("move", SuggestionAccept.Move, "Move rehearsal check-in to 3:00 PM", "Your calendar has a conflict at 8:00 AM."),
    AgendaSuggestionData("review", SuggestionAccept.Add, "Review venue notes", "Have the details ready for tonight."),
    AgendaSuggestionData("setlist", SuggestionAccept.Add, "Bring the updated set list", "Keep the latest songs ready for rehearsal."),
)

/** One Agenda row (task or event). `pending` renders the dashed status ring of a just-accepted add. */
data class AgendaRowData(
    val id: String,
    val isEvent: Boolean,
    val title: String,
    val timeLabel: String,
    val sortMinutes: Int,
    val pills: List<String> = emptyList(),
    val pending: Boolean = false,
    /** The event's calendar (tints its bar dot); `null` keeps the default Personal blue. */
    val calendar: AgendaCreationDraft.EventCalendar? = null,
)

/** How a suggestion resolves when accepted — the authored example fixtures, not app-wide defaults. */
private sealed interface Resolution {
    data class Add(val row: AgendaRowData) : Resolution
    data class Move(
        val targetId: String,
        val timeLabel: String,
        val sortMinutes: Int,
        val fallback: AgendaRowData,
    ) : Resolution
}

internal data class RestoreRequest(val item: AgendaSuggestionData, val index: Int)

/**
 * Owns the deterministic local Agenda Suggestions model. No network, no fetch, no invented spinner /
 * success badge / error card / Retry.
 */
internal class AgendaSuggestionsState(fixture: AgendaSuggestionsFixture) {
    val rows = mutableStateListOf<AgendaRowData>()
    val suggestions = mutableStateListOf<AgendaSuggestionData>()
    var overflowOpen by mutableStateOf(false)
    var restoreRequest by mutableStateOf<RestoreRequest?>(null)
    /** Agenda entry routing: Add New → creation, Schedule → Schedule Tasks. */
    var entry by mutableStateOf(AgendaEntryFixture())
        private set
    var creationOpen by mutableStateOf(false)
    var scheduleOpen by mutableStateOf(false)

    private val resolutions: Map<String, Resolution>
    private var restorationArmed: Boolean = fixture == AgendaSuggestionsFixture.Restoration

    val hasOverflow: Boolean get() = suggestions.size > 3
    val inline: List<AgendaSuggestionData> get() = suggestions.take(3)

    init {
        val loadedRows = listOf(
            AgendaRowData("reply", false, "Reply to the venue", "8:00 AM", 8 * 60, listOf("Follow-ups")),
            AgendaRowData("confirm", false, "Confirm rehearsal time", "8:00 AM", 8 * 60, listOf("Follow-ups")),
            AgendaRowData("coffee", true, "Coffee chat with a mentor", "7:00 PM", 19 * 60, listOf("Personal")),
        )
        val allSuggestions = AgendaReferenceSuggestions
        resolutions = mapOf(
            "prep" to Resolution.Add(AgendaRowData("prep-task", false, "Prep for tonight’s rehearsal", "5:00 PM", 17 * 60, pending = true)),
            "move" to Resolution.Move("confirm", "3:00 PM", 15 * 60,
                AgendaRowData("move-task", false, "Rehearsal check-in", "3:00 PM", 15 * 60, pending = true)),
            "review" to Resolution.Add(AgendaRowData("review-task", false, "Review venue notes", "6:30 PM", 18 * 60 + 30, pending = true)),
            "setlist" to Resolution.Add(AgendaRowData("setlist-task", false, "Bring the updated set list", "6:00 PM", 18 * 60, pending = true)),
        )
        when (fixture) {
            AgendaSuggestionsFixture.Loaded, AgendaSuggestionsFixture.Restoration -> {
                rows.addAll(loadedRows); suggestions.addAll(allSuggestions)
            }
            AgendaSuggestionsFixture.None -> rows.addAll(loadedRows)
            AgendaSuggestionsFixture.Empty -> suggestions.addAll(allSuggestions)
        }
    }

    fun accept(s: AgendaSuggestionData) {
        val index = suggestions.indexOfFirst { it.id == s.id }
        if (index < 0) return
        resolutions[s.id]?.let(::apply)
        removeAt(index, s)
    }

    fun dismiss(s: AgendaSuggestionData) {
        val index = suggestions.indexOfFirst { it.id == s.id }
        if (index < 0) return
        removeAt(index, s)
    }

    fun completeRestore(request: RestoreRequest) {
        if (suggestions.none { it.id == request.item.id }) {
            suggestions.add(request.index.coerceIn(0, suggestions.size), request.item)
        }
    }

    private fun removeAt(index: Int, original: AgendaSuggestionData) {
        suggestions.removeAt(index)
        if (restorationArmed) {
            restorationArmed = false
            restoreRequest = RestoreRequest(original, minOf(index, suggestions.size))
        }
        if (suggestions.isEmpty()) overflowOpen = false
    }

    private fun apply(resolution: Resolution) {
        when (resolution) {
            is Resolution.Add -> insert(resolution.row)
            is Resolution.Move -> {
                val i = rows.indexOfFirst { it.id == resolution.targetId }
                if (i >= 0) {
                    rows[i] = rows[i].copy(timeLabel = resolution.timeLabel, sortMinutes = resolution.sortMinutes)
                    resort()
                } else {
                    insert(resolution.fallback)
                }
            }
        }
    }

    /** Save from the creation sheet: a valid draft lands on the day and the sheet closes. */
    fun create(draft: AgendaCreationDraft) {
        val (next, row) = entry.create(draft)
        if (row == null) return
        entry = next
        insert(row)
        creationOpen = false
    }

    /** Done from Schedule Tasks: the selected tasks are scheduled together and the sheet closes. */
    fun schedule(request: AgendaScheduleRequest) {
        val (next, scheduled) = entry.schedule(request)
        entry = next
        scheduled.forEach(::insert)
        scheduleOpen = false
    }

    private fun insert(row: AgendaRowData) {
        if (rows.any { it.id == row.id }) return
        rows.add(row)
        resort()
    }

    private fun resort() {
        val sorted = rows.sortedBy { it.sortMinutes }
        rows.clear(); rows.addAll(sorted)
    }
}

/**
 * **AgendaSuggestionsPlayground** — Compose sibling of the SwiftUI `AgendaSuggestionsPlaygroundView`.
 * The bounded Agenda New Suggestions journey: the reused [DateNavigationHeader], the day's
 * [TaskEventRow]s (or the empty [RemContentUnavailableView]), the Add New / Schedule bar, and the
 * Suggestions slot which follows the bar with exactly 24dp spacing while present. **Add New** opens
 * [AgendaCreationSheet] and **Schedule** opens [AgendaScheduleSheet]; their results are applied to this
 * fixture in memory ([AgendaEntryFixture]). [onAddNew] / [onSchedule] are notified when an entry opens.
 * Date paging and sort still call host callbacks only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaSuggestionsPlayground(
    fixture: AgendaSuggestionsFixture,
    modifier: Modifier = Modifier,
    onAddNew: () -> Unit = {},
    onSchedule: () -> Unit = {},
    onSort: () -> Unit = {},
) {
    val colors = RemColors.current
    val state = remember(fixture) { AgendaSuggestionsState(fixture) }

    state.restoreRequest?.let { request ->
        LaunchedEffect(request) {
            delay(600)
            state.completeRestore(request)
            state.restoreRequest = null
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        DateNavigationHeader(
            dateText = "Aug 13 2026",
            onPrevious = {},
            onNext = {},
            modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg)
                .padding(top = RemSpacing.sm),
        ) {
            if (state.rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    RemContentUnavailableView(
                        icon = Icons.Filled.CalendarMonth,
                        title = "No agenda yet",
                        message = "Create a new task or schedule existing ones",
                    )
                }
            } else {
                SortTrigger(onSort)
                state.rows.forEach { row ->
                    TaskEventRow(
                        kind = if (row.isEvent) TaskEventKind.Event(calendarColor(row.calendar)) else TaskEventKind.Task,
                        title = row.title,
                        leading = TaskEventLeading.Time(row.timeLabel),
                        pills = row.pills,
                        pending = row.pending,
                        modifier = Modifier.testTag("agenda.row.${row.id}"),
                    )
                }
            }
            AddScheduleBar(
                scheduleCount = state.entry.scheduleCount,
                onAddNew = { state.creationOpen = true; onAddNew() },
                onSchedule = { state.scheduleOpen = true; onSchedule() },
            )
            if (state.suggestions.isNotEmpty()) {
                AgendaSuggestionsSection(
                    suggestions = state.inline,
                    showSeeMore = state.hasOverflow,
                    onAccept = state::accept,
                    onDismiss = state::dismiss,
                    onSeeMore = { state.overflowOpen = true },
                )
            }
        }
    }

    if (state.overflowOpen) {
        ModalBottomSheet(
            onDismissRequest = { state.overflowOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.backgroundPrimary,
            dragHandle = null,
        ) {
            AgendaSuggestionsOverflowContent(
                suggestions = state.suggestions,
                onAccept = state::accept,
                onDismiss = state::dismiss,
                onDone = { state.overflowOpen = false },
            )
        }
    }

    if (state.creationOpen) {
        AgendaCreationSheet(
            onCancel = { state.creationOpen = false },
            onSave = state::create,
        )
    }

    if (state.scheduleOpen) {
        AgendaScheduleSheet(
            fixture = state.entry,
            onCancel = { state.scheduleOpen = false },
            onDone = state::schedule,
        )
    }
}

/**
 * The overflow presentation content: the authored **Suggestions / Done** header, a divider, and a
 * scrollable list of all live suggestions. Rendered inside a native `ModalBottomSheet` at runtime; the
 * same content is the paired snapshot evidence (a modal sheet does not render headlessly).
 */
@Composable
fun AgendaSuggestionsOverflowContent(
    suggestions: List<AgendaSuggestionData>,
    modifier: Modifier = Modifier,
    onAccept: (AgendaSuggestionData) -> Unit = {},
    onDismiss: (AgendaSuggestionData) -> Unit = {},
    onDone: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(modifier.testTag("agendaSuggestions.overflow")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RemSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Suggestions", style = RemTypography.title3Bold, color = colors.labelPrimary, modifier = Modifier.weight(1f))
            Text(
                text = "Done",
                style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.brandBlue,
                modifier = Modifier.testTag("agendaSuggestions.done").clickable(onClick = onDone),
            )
        }
        HorizontalDivider(color = colors.separator)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            suggestions.forEach { suggestion ->
                SuggestedTaskRow(
                    suggestion = TaskSuggestion(suggestion.title, suggestion.metadata, suggestion.accept),
                    onAccept = { onAccept(suggestion) },
                    onDismiss = { onDismiss(suggestion) },
                    acceptTag = "agendaSuggestion.sheet.accept.${suggestion.id}",
                    dismissTag = "agendaSuggestion.sheet.dismiss.${suggestion.id}",
                )
            }
        }
    }
}

/**
 * The inline Suggestions slot: the "Suggestions" header (17sp semibold, labelSecondary), up to three
 * rows, then a "See more" plain action when there is overflow. The region owns a 24dp top gap so
 * removing it (last suggestion gone) removes the gap too.
 */
@Composable
private fun AgendaSuggestionsSection(
    suggestions: List<AgendaSuggestionData>,
    showSeeMore: Boolean,
    onAccept: (AgendaSuggestionData) -> Unit,
    onDismiss: (AgendaSuggestionData) -> Unit,
    onSeeMore: () -> Unit,
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier.padding(top = RemSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Text(
            text = "Suggestions",
            style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.labelSecondary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        suggestions.forEach { suggestion ->
            SuggestedTaskRow(
                suggestion = TaskSuggestion(suggestion.title, suggestion.metadata, suggestion.accept),
                onAccept = { onAccept(suggestion) },
                onDismiss = { onDismiss(suggestion) },
                acceptTag = "agendaSuggestion.inline.accept.${suggestion.id}",
                dismissTag = "agendaSuggestion.inline.dismiss.${suggestion.id}",
            )
        }
        if (showSeeMore) {
            Text(
                text = "See more",
                style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.brandBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 34.dp)
                    .testTag("agendaSuggestions.seeMore")
                    .clickable(onClick = onSeeMore)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SortTrigger(onSort: () -> Unit) {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .testTag("agenda.sortTrigger")
            .clickable(onClick = onSort)
            .padding(vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.SwapVert, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.width(18.dp))
        Text("Sort by: Time", style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelSecondary)
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun AddScheduleBar(scheduleCount: Int, onAddNew: () -> Unit, onSchedule: () -> Unit) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.padding(top = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Row(
            modifier = Modifier.testTag("agenda.addNew").clickable(onClick = onAddNew),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.width(18.dp))
            Text("Add New", style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelSecondary)
        }
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(colors.separator))
        Row(
            modifier = Modifier.testTag("agenda.schedule").clickable(onClick = onSchedule),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.width(18.dp))
            Text("Schedule", style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelSecondary)
            // The badge counts tasks still waiting to be scheduled; it leaves with the last one.
            if (scheduleCount > 0) {
                Box(
                    modifier = Modifier
                        .background(colors.labelTertiary, RoundedCornerShape(999.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "$scheduleCount",
                        style = RemTypography.caption1.copy(fontWeight = FontWeight.Bold),
                        color = colors.backgroundPrimary,
                        modifier = Modifier.testTag("agenda.scheduleCount"),
                    )
                }
            }
        }
    }
}

@Preview(name = "AgendaSuggestions — loaded", showBackground = true, widthDp = 402, heightDp = 860)
@Composable
private fun AgendaSuggestionsLoadedPreview() {
    RemTheme { AgendaSuggestionsPlayground(fixture = AgendaSuggestionsFixture.Loaded) }
}

@Preview(name = "AgendaSuggestions — empty", showBackground = true, widthDp = 402, heightDp = 860)
@Composable
private fun AgendaSuggestionsEmptyPreview() {
    RemTheme { AgendaSuggestionsPlayground(fixture = AgendaSuggestionsFixture.Empty) }
}
