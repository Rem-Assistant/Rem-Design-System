package com.rem.designsystem.screens

// Agent activity fixture (presentation-only) — Compose twin of `Screens/AgentActivityFixture.swift`.
// The deterministic host behind the Playground's Agent activity screen, opened from the Chat header
// identity. The current state is taken from the Chat header that was tapped; the timeline is neutral
// sample history, not anyone's real work (the Figma master's sample rows are replaced). Nothing is
// fetched, stored or routed. Approvals stay a labeled data gap. Immutable: [select] returns the next
// fixture.

data class AgentActivityFixture(
    val display: AgentActivityDisplay,
    val selectedTab: AgentActivityTab = AgentActivityTab.Activity,
) {
    /** Switch the segmented control. Only the visible section changes. */
    fun select(tab: AgentActivityTab): AgentActivityFixture = copy(selectedTab = tab)

    companion object {
        /**
         * Neutral sample history, newest first within each day. Times sit around the Chat fixture's
         * illustrative 10:20–10:24 conversation.
         */
        val Days: List<AgentActivityDay> = listOf(
            AgentActivityDay("today", "Today", listOf(
                AgentActivityEvent("reminder", "Prepare a reminder", "Outlined the details", "10:24 AM"),
                AgentActivityEvent("plan-day", "Plan the rest of the day", "Suggested an order", "10:21 AM"),
            )),
            AgentActivityDay("yesterday", "Yesterday", listOf(
                AgentActivityEvent("shared-notes", "Summarize shared notes", "Shared a summary", "4:10 PM"),
                AgentActivityEvent("open-tasks", "Review open tasks", "Listed next steps", "3:42 PM"),
            )),
        )

        fun of(current: AgentActivityCurrent, days: List<AgentActivityDay> = Days) =
            AgentActivityFixture(AgentActivityDisplay(current, days))

        /** The screen for the Chat header the person tapped: same current state, fixture history. */
        fun of(header: ChatHeaderDisplay) = of(AgentActivityCurrent.of(header))
    }
}
