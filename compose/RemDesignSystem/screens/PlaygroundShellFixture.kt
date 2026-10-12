package com.rem.designsystem.screens

import com.rem.designsystem.agents.NewAgentDraft
import com.rem.designsystem.agents.NewAgentSetupAction
import com.rem.designsystem.navigation.RemSidebarAccount
import com.rem.designsystem.navigation.RemSidebarAction
import com.rem.designsystem.navigation.RemSidebarAgent
import com.rem.designsystem.navigation.RemSidebarDisplay
import com.rem.designsystem.navigation.RemSidebarSelection

// The Playground's app shell (Build 9, presentation-only) — Compose twin of
// `Screens/PlaygroundShellFixture.swift`. One full-screen root conversation with the sidebar
// (`2702:2479`) behind the chat header's sidebar button (`2702:2351`); New agent (name + colour) and
// Settings (the account pill) are presented over it. This value is the single source of truth for the
// shell's selection, sidebar and presentation; the hosts only render it and draw the native chrome.
//
// It is NOT an app adapter: no agent is created anywhere but in this value, nothing is persisted, and no
// backend, auth, voice or provider is touched. Route map: `docs/playground/route-map.md`.

/** What the shell presents over the root conversation. At most one at a time. */
enum class PlaygroundShellPresentation { NewAgent, Settings }

data class PlaygroundShellFixture(
    val mainAgent: RemSidebarAgent = MainAgent,
    val sideAgents: List<RemSidebarAgent> = SeedSideAgents,
    val account: RemSidebarAccount = Account,
    /** The conversation on screen. Never inferred from list order; always a listed agent. */
    val selection: RemSidebarSelection = RemSidebarSelection.Main,
    val isSidebarOpen: Boolean = false,
    val presented: PlaygroundShellPresentation? = null,
    /** The New agent sheet's host-owned draft; reset each time the sheet opens. */
    val draft: NewAgentDraft = NewAgentDraft(),
    private val createdCount: Int = 0,
) {
    val sidebar: RemSidebarDisplay
        get() = RemSidebarDisplay(mainAgent, sideAgents, account, selection)

    /** The agent whose conversation is on screen. */
    val selectedAgent: RemSidebarAgent
        get() = when (val s = selection) {
            RemSidebarSelection.Main -> mainAgent
            is RemSidebarSelection.Side -> sideAgents.firstOrNull { it.id == s.id } ?: mainAgent
        }

    /** A stable key for the on-screen conversation: switching agents replaces it, nothing else does. */
    val conversationKey: String
        get() = when (val s = selection) {
            RemSidebarSelection.Main -> "main"
            is RemSidebarSelection.Side -> "side.${s.id}"
        }

    /** The header sidebar button. */
    fun openSidebar() = copy(isSidebarOpen = true)

    /** Tap-to-close on the pushed chat card, or system Back while the sidebar is open. */
    fun closeSidebar() = copy(isSidebarOpen = false)

    /**
     * Sidebar actions. Choosing an agent shows its conversation and closes the sidebar (choosing the one
     * already shown keeps it, with its scroll position). New agent and the account pill close the sidebar
     * and present their surface over the unchanged conversation. Search has no approved surface yet
     * (`sidebar-new-agent.md` non-goal), so it changes nothing.
     */
    fun handle(action: RemSidebarAction): PlaygroundShellFixture = when (action) {
        RemSidebarAction.SelectMain -> copy(selection = RemSidebarSelection.Main, isSidebarOpen = false)
        is RemSidebarAction.SelectSide ->
            if (sideAgents.any { it.id == action.id }) copy(selection = RemSidebarSelection.Side(action.id), isSidebarOpen = false)
            else this
        RemSidebarAction.NewAgent -> copy(isSidebarOpen = false, presented = PlaygroundShellPresentation.NewAgent, draft = NewAgentDraft())
        RemSidebarAction.OpenSettings -> copy(isSidebarOpen = false, presented = PlaygroundShellPresentation.Settings)
        RemSidebarAction.Search -> this
    }

    /** New agent: the draft edits in place; Cancel dismisses; Create adds the agent and opens its chat. */
    fun editDraft(draft: NewAgentDraft) = if (presented == PlaygroundShellPresentation.NewAgent) copy(draft = draft) else this

    fun handle(action: NewAgentSetupAction): PlaygroundShellFixture {
        if (presented != PlaygroundShellPresentation.NewAgent) return this
        return when (action) {
            NewAgentSetupAction.Cancel -> copy(presented = null, draft = NewAgentDraft())
            is NewAgentSetupAction.Create -> {
                if (!action.draft.canCreate) return this
                val id = "new.${createdCount + 1}"
                copy(
                    sideAgents = sideAgents + RemSidebarAgent(id, action.draft.trimmedName, NewAgentLane),
                    selection = RemSidebarSelection.Side(id),
                    presented = null,
                    draft = NewAgentDraft(),
                    createdCount = createdCount + 1,
                )
            }
        }
    }

    /** Settings' Done / system Back at its root: back to the same conversation. */
    fun dismissPresentation() = copy(presented = null)

    companion object {
        /** The Settings profile's name, so the account pill and Settings agree. */
        val Account = RemSidebarAccount("Avery Diaz")
        val MainAgent = RemSidebarAgent("rem", "Rem", "Daily Brief ready")
        /** Lane subtitle of an agent created here (setup asks only for a name and a colour). */
        const val NewAgentLane = "New agent"
        val SeedSideAgents = listOf(
            RemSidebarAgent("health", "Health", "Appointments & meds"),
            RemSidebarAgent("money", "Money", "Bills & budget", hasUnread = true),
            RemSidebarAgent("travel", "Travel planning: Lisbon in May", "Flights, stays"),
        )
    }
}
