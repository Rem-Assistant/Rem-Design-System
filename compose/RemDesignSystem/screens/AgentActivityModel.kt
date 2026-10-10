package com.rem.designsystem.screens

import com.rem.designsystem.chat.ChatHeaderStatus

// Agent activity screen inputs (presentation-only) — Compose twin of `Screens/AgentActivityModel.swift`.
//
// Figma master `Rem/Chat/Agent activity` (`2002:76914`), reached from the agent identity in the Chat
// header ([ChatScreenAction.ActivityDetails]). Two inputs, deliberately separate:
// - [AgentActivityDisplay.current] is the agent-level state the host reports now — the same value the
//   Chat header shows. It is never derived from the timeline: a historical event cannot overwrite it.
// - [AgentActivityDisplay.days] is host-formatted history (day labels and times are host strings; the
//   DS never reads a clock or formats dates). Rows summarize human-facing work.
// Not agent settings, a card outcome or the voice session state. Approvals have no verified design
// (`2002:76915` only repeats timeline rows), so that tab presents a labeled data gap; rows have no
// verified destination and are not interactive. Tests: `AgentActivityFixtureTest.kt`.

/** The agent's **current** state, as the host reports it now (identity line under the face). */
data class AgentActivityCurrent(
    /** Host copy for what the agent is doing now, e.g. "Connected". */
    val activity: String,
    val name: String = "Rem",
    val status: ChatHeaderStatus = ChatHeaderStatus.Connected,
    /** The face shows the thinking mode while the host reports the agent as working. */
    val isWorking: Boolean = false,
) {
    companion object {
        /** The same current state the Chat header renders, so the two surfaces never disagree. */
        fun of(header: ChatHeaderDisplay) = AgentActivityCurrent(
            activity = header.activity, name = header.name, status = header.status, isWorking = header.isWorking,
        )
    }
}

/**
 * One completed piece of work: an action [title] and its [outcome] at a host-formatted [time]. Carries
 * no status — history never speaks for the agent's current state.
 */
data class AgentActivityEvent(val id: String, val title: String, val outcome: String, val time: String) {
    /** The row subtitle: "Outcome · time". */
    val summary: String get() = "$outcome · $time"
}

/** A day group in the timeline, e.g. "Today" / "Yesterday" (host-formatted label). */
data class AgentActivityDay(val id: String, val title: String, val events: List<AgentActivityEvent>)

/** The Activity / Approvals segmented control. */
enum class AgentActivityTab(val label: String) { Activity("Activity"), Approvals("Approvals") }

/** Everything the Agent activity screen renders. */
data class AgentActivityDisplay(val current: AgentActivityCurrent, val days: List<AgentActivityDay>) {
    /** Day groups that have rows; an empty day never draws a lone header. */
    val visibleDays: List<AgentActivityDay> get() = days.filter { it.events.isNotEmpty() }

    companion object {
        const val Title = "Agent activity"
        /** The screen Back returns to. */
        const val BackTitle = "Chat"
        /** Approvals have no verified design or data: say so instead of implying there are none. */
        const val ApprovalsGapTitle = "Approvals not shown yet"
        const val ApprovalsGapMessage =
            "Approval details aren't available on this screen yet. This doesn't mean nothing needs your approval."
        const val ActivityEmptyTitle = "No activity yet"
        const val ActivityEmptyMessage = "Work Rem finishes for you will appear here."
    }
}
