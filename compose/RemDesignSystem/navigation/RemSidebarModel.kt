package com.rem.designsystem.navigation

// Sidebar inputs (presentation-only) — the Compose twin of `Navigation/RemSidebarModel.swift`.
//
// Figma frames **A2 · Sidebar open (Claude-style)** (`2702:2479`) and **A2 · Chat (sidebar closed)**
// (`2702:2351`), account pill `2709:2648`. Approved decisions (Samuel): Search is the only top action;
// sections **Main agent** (Rem) and **Side agents** with lane subtitles; **New agent** is the creation
// entry; the account pill ("S · Samuel") *is* the Settings entry — no separate gear. Selection is the
// host's current conversation identity, never inferred from order.

/** One agent row: the agent's name and its lane subtitle. */
data class RemSidebarAgent(
    val id: String,
    val name: String,
    /** The lane subtitle, e.g. "Appointments & meds". */
    val lane: String,
    /** Unread activity in this agent's chat (trailing dot). */
    val hasUnread: Boolean = false,
)

/** The signed-in account shown in the bottom pill. */
data class RemSidebarAccount(val name: String) {
    val displayName: String get() = name.trim()

    /** The pill's initial: the first character of the display name, uppercased (`"?"` when blank). */
    val initial: String
        get() {
            val name = displayName
            if (name.isEmpty()) return "?"
            val end = name.offsetByCodePoints(0, 1)
            return name.substring(0, end).uppercase()
        }
}

/** Which conversation the host currently shows. `null` in the display means none is highlighted. */
sealed interface RemSidebarSelection {
    data object Main : RemSidebarSelection
    data class Side(val id: String) : RemSidebarSelection
}

/** Everything the sidebar renders. */
data class RemSidebarDisplay(
    val mainAgent: RemSidebarAgent,
    val sideAgents: List<RemSidebarAgent>,
    val account: RemSidebarAccount,
    val selection: RemSidebarSelection? = RemSidebarSelection.Main,
) {
    val isMainSelected: Boolean get() = selection == RemSidebarSelection.Main

    /** A selection naming an id that is not listed highlights nothing (no fallback to another row). */
    fun isSelected(agent: RemSidebarAgent): Boolean = selection == RemSidebarSelection.Side(agent.id)

    /** The Side agents section only draws when there is at least one side agent — never a lone header. */
    val showsSideAgents: Boolean get() = sideAgents.isNotEmpty()

    companion object {
        const val Title = "Rem"
        const val MainAgentHeader = "Main agent"
        const val SideAgentsHeader = "Side agents"
        const val NewAgent = "New agent"
        const val Search = "Search"
        /** Accessibility hint on the account pill — it is the Settings entry. */
        const val AccountHint = "Opens Settings"
    }
}

/** What the person asked the sidebar to do. */
sealed interface RemSidebarAction {
    data object Search : RemSidebarAction
    data object SelectMain : RemSidebarAction
    data class SelectSide(val id: String) : RemSidebarAction
    data object NewAgent : RemSidebarAction
    /** The account pill — opens Settings. */
    data object OpenSettings : RemSidebarAction
}

/**
 * Geometry for the Claude-style push (`2702:2479`): opening slides the chat right and leaves a rounded
 * card of it visible (tap to close). Values in dp; pure so both platforms agree and it is testable.
 */
object RemSidebarLayout {
    /** How much of the pushed chat stays visible (402 frame − 332 sidebar). */
    const val VisibleContentPeek = 70f
    /** The sidebar never grows past the Figma width on wide containers. */
    const val MaximumSidebarWidth = 332f
    /** Corner radius of the pushed chat card. */
    const val ContentCornerRadius = 44f

    fun sidebarWidth(containerWidth: Float): Float =
        (containerWidth - VisibleContentPeek).coerceIn(0f, MaximumSidebarWidth)

    fun contentOffset(isOpen: Boolean, containerWidth: Float): Float =
        if (isOpen) sidebarWidth(containerWidth) else 0f
}
