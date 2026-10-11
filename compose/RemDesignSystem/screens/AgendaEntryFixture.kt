package com.rem.designsystem.screens

// Agenda entry routing · fixture model. Compose twin of `Screens/AgendaEntryFixture.swift`.
//
// Presentation-only model behind the Playground Agenda's two entries in `AgendaAddSchedule` (`2189:11919`):
// **Add New** → task/event creation (Creation section `2390:28498`, reached like the create menu's
// "New Task or Event" item `2049:10336`) and **Schedule** → existing-task selection (Schedule Tasks
// `2295:13691`, section `2295:13690`). The fixture host applies results in memory; nothing is
// persisted, synced or sent.

/**
 * A calendar day, independent of time zone. Plain civil-calendar math (no `java.time`, which needs
 * API 26 while the library's minSdk is 24) so both platforms produce identical labels.
 */
data class AgendaDay(val year: Int, val month: Int, val day: Int) : Comparable<AgendaDay> {
    /** Days since 1970-01-01 (proleptic Gregorian). */
    val epochDay: Int
        get() {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097 + doe - 719_468
        }

    fun plusDays(days: Int): AgendaDay = ofEpochDay(epochDay + days)

    /** 0 = Sunday … 6 = Saturday. */
    val weekday: Int get() = ((epochDay % 7) + 7 + 4) % 7

    override fun compareTo(other: AgendaDay): Int = epochDay.compareTo(other.epochDay)

    /** "Aug 13 2026" — the `DateNavigationHeader` date line. */
    val headerText: String get() = "${MonthNames[month - 1].take(3)} $day $year"

    /** The Pick a Time **Date** row: "Today, August 13", "Tomorrow, August 14", "Saturday, August 15". */
    fun dateRowText(today: AgendaDay): String {
        val prefix = when (epochDay - today.epochDay) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> WeekdayNames[weekday]
        }
        return "$prefix, ${MonthNames[month - 1]} $day"
    }

    /** The Schedule Tasks primary action: "Add to Today", "Add to Tomorrow" or "Add to Aug 15". */
    fun addToLabel(today: AgendaDay): String = when (epochDay - today.epochDay) {
        0 -> "Add to Today"
        1 -> "Add to Tomorrow"
        else -> "Add to ${MonthNames[month - 1].take(3)} $day"
    }

    companion object {
        /** The authored fixture date ("Sample fixture: August 13, 2026"), viewed and treated as today. */
        val FixtureToday = AgendaDay(2026, 8, 13)

        fun ofEpochDay(epochDay: Int): AgendaDay {
            val z = epochDay + 719_468
            val era = (if (z >= 0) z else z - 146_096) / 146_097
            val doe = z - era * 146_097
            val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = if (mp < 10) mp + 3 else mp - 9
            return AgendaDay(yoe + era * 400 + if (m <= 2) 1 else 0, m, d)
        }

        private val MonthNames = listOf(
            "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December",
        )
        private val WeekdayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    }
}

/** A time of day on the fixture's 12-hour clock. */
data class AgendaTime(val hour: Int, val minute: Int) {
    val sortMinutes: Int get() = hour * 60 + minute

    /** "9:00 AM" — the `TaskEventRow` time label used by the Agenda fixtures. */
    val label: String
        get() {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            val mm = if (minute < 10) "0$minute" else "$minute"
            return "$h12:$mm ${if (hour < 12) "AM" else "PM"}"
        }

    companion object {
        /** Schedule Tasks default time ("Default time 09:00"). */
        val ScheduleDefault = AgendaTime(9, 0)
    }
}

/** Schedule Tasks filters (`ScheduleFilters` `2301:11644`). Changing a filter never clears selection. */
enum class AgendaScheduleFilter(val title: String, val emptyText: String) {
    All("All", "No tasks to schedule"),
    Inbox("Inbox", "No tasks in Inbox"),
    Overdue("Overdue", "No overdue tasks"),
}

/** An existing, unscheduled backlog item. Only tasks are schedule candidates; events are excluded. */
data class AgendaBacklogItem(
    val id: String,
    val title: String,
    /** The row's metadata line ("Inbox", "Overdue · 2d", "No date"). */
    val detail: String,
    val bucket: Bucket,
    val isEvent: Boolean = false,
    /** Pills the task keeps once it lands on the Agenda. */
    val pills: List<String> = emptyList(),
) {
    enum class Bucket { Inbox, Overdue, Undated }

    fun matches(filter: AgendaScheduleFilter): Boolean = when (filter) {
        AgendaScheduleFilter.All -> true
        AgendaScheduleFilter.Inbox -> bucket == Bucket.Inbox
        AgendaScheduleFilter.Overdue -> bucket == Bucket.Overdue
    }
}

/** What Schedule Tasks hands back on Done: the selected task IDs (scheduled together) and one slot. */
data class AgendaScheduleRequest(val taskIds: List<String>, val day: AgendaDay, val time: AgendaTime)

/**
 * The creation draft (`2390:28498`): New Task / New Event, title, chooser value and notes. Date, time,
 * duration, alert and repeat are the authored inline summaries — their inspector is separate work.
 */
data class AgendaCreationDraft(
    val mode: Mode = Mode.Task,
    val title: String = "",
    val notes: String = "",
    val taskList: TaskList = TaskList.NoList,
    val calendar: EventCalendar = EventCalendar.Personal,
) {
    enum class Mode(val title: String) { Task("New Task"), Event("New Event") }

    /** The designed task-list chooser (`CreationChooserMenu/Task list`). */
    enum class TaskList(val title: String) { NoList("No List"), FollowUps("Follow-ups"), Work("Work") }

    /** The calendar chooser (`CreationChooserMenu/Calendar`). */
    enum class EventCalendar(val title: String) { Personal("Personal"), Work("Work") }

    /** Save is enabled only for a non-blank title. */
    val canSave: Boolean get() = title.isNotBlank()

    /** Authored slots: a task lands at 5:00 PM (30 min); an event spans 6:00–7:00 PM (1 hr). */
    val start: AgendaTime get() = if (mode == Mode.Task) AgendaTime(17, 0) else AgendaTime(18, 0)

    val whenSummary: String get() = if (mode == Mode.Task) "Today, 5:00 PM" else "Today, 6:00–7:00 PM"
    val durationSummary: String get() = if (mode == Mode.Task) "30 min" else "1 hr"

    /** The chooser's current label ("Select task list" until a list is picked). */
    val chooserLabel: String
        get() = when (mode) {
            Mode.Task -> if (taskList == TaskList.NoList) "Select task list" else taskList.title
            Mode.Event -> calendar.title
        }
}

/** The in-memory fixture that applies creation and scheduling results to the viewed day. */
data class AgendaEntryFixture(
    val today: AgendaDay = AgendaDay.FixtureToday,
    val viewedDay: AgendaDay = AgendaDay.FixtureToday,
    val backlog: List<AgendaBacklogItem> = ReferenceBacklog,
    private val createdCount: Int = 0,
) {
    /** Schedule candidates for a filter, in backlog order. Events are excluded. */
    fun candidates(filter: AgendaScheduleFilter): List<AgendaBacklogItem> =
        backlog.filter { !it.isEvent && it.matches(filter) }

    /** The Schedule badge count: every task still waiting to be scheduled. */
    val scheduleCount: Int get() = candidates(AgendaScheduleFilter.All).size

    /**
     * Applies Done: the selected tasks leave the backlog together; those scheduled on the viewed day are
     * returned as Agenda rows at the chosen time (other days are off-screen). Unknown IDs and events are
     * ignored.
     */
    fun schedule(request: AgendaScheduleRequest): Pair<AgendaEntryFixture, List<AgendaRowData>> {
        val chosen = backlog.filter { !it.isEvent && it.id in request.taskIds }
        if (chosen.isEmpty()) return this to emptyList()
        val ids = chosen.map { it.id }.toSet()
        val next = copy(backlog = backlog.filterNot { it.id in ids })
        if (request.day != viewedDay) return next to emptyList()
        return next to chosen.map {
            AgendaRowData("scheduled-${it.id}", isEvent = false, title = it.title, timeLabel = request.time.label,
                sortMinutes = request.time.sortMinutes, pills = it.pills)
        }
    }

    /**
     * Applies Save: a valid draft becomes a new Agenda row on the viewed day; a blank one is ignored.
     * Tasks keep the chosen list (none for No List) and show the pending ring; events keep the calendar.
     */
    fun create(draft: AgendaCreationDraft): Pair<AgendaEntryFixture, AgendaRowData?> {
        if (!draft.canSave) return this to null
        val count = createdCount + 1
        val start = draft.start
        val row = when (draft.mode) {
            AgendaCreationDraft.Mode.Task -> AgendaRowData(
                "created-task-$count", isEvent = false, title = draft.title.trim(), timeLabel = start.label,
                sortMinutes = start.sortMinutes,
                pills = if (draft.taskList == AgendaCreationDraft.TaskList.NoList) emptyList() else listOf(draft.taskList.title),
                pending = true,
            )
            AgendaCreationDraft.Mode.Event -> AgendaRowData(
                "created-event-$count", isEvent = true, title = draft.title.trim(), timeLabel = start.label,
                sortMinutes = start.sortMinutes, pills = listOf(draft.calendar.title), calendar = draft.calendar,
            )
        }
        return copy(createdCount = count) to row
    }

    companion object {
        /** The authored Schedule Tasks list, plus one undated event that must never appear there. */
        val ReferenceBacklog: List<AgendaBacklogItem> = listOf(
            AgendaBacklogItem("draft", "Draft project update", "Inbox", AgendaBacklogItem.Bucket.Inbox, pills = listOf("Inbox")),
            AgendaBacklogItem("specs", "Review design specs", "Overdue · 2d", AgendaBacklogItem.Bucket.Overdue),
            AgendaBacklogItem("dentist", "Book dentist appointment", "No date", AgendaBacklogItem.Bucket.Undated),
            AgendaBacklogItem("walkthrough", "Venue walkthrough", "No date", AgendaBacklogItem.Bucket.Undated, isEvent = true),
        )
    }
}
