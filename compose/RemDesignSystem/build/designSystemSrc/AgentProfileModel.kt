package com.rem.designsystem.agents

// Agent profile inputs (presentation-only) — the Compose twin of `Agents/AgentProfileModel.swift`.
//
// Figma frames **E3 · Agent profile — Health · About** (`2718:3055`) and **E3b · Agent profile — Rem ·
// Activity** (`2718:3396`). This covers what the existing Agent activity screen does not: the per-agent
// identity header and the **About** tab body.
//
// Reconciliation (not done here): `AgentActivityScreen` / `AgentActivityTab` live on PR #106, not on
// main. When both land, `AgentActivityTab` gains `About`, its identity block renders
// [AgentProfileHeader], and the About case renders [AgentProfileAbout].

/** The avatar identity: the Rem face, or a side agent's chosen look. */
sealed interface AgentAvatarIdentity {
    data object Main : AgentAvatarIdentity
    data class Side(val look: AgentLook) : AgentAvatarIdentity
}

/** Everything the profile header and About tab render for one agent. */
data class AgentProfileDisplay(
    val kind: AgentKind,
    val name: String,
    /** Side agents only; the Main agent always renders the Rem face. */
    val look: AgentLook = AgentLook.Default,
    /** Host copy for the lane, shown after the kind in the status line. */
    val laneSummary: String,
    /** "What should this agent handle?" as saved, shown in the **Handles** row. */
    val handles: String,
    val services: List<AgentService> = emptyList(),
) {
    val avatar: AgentAvatarIdentity
        get() = if (kind == AgentKind.Main) AgentAvatarIdentity.Main else AgentAvatarIdentity.Side(look)

    /** The line under the name: "Side agent · Appointments and refills". */
    val statusLine: String
        get() {
            val lane = laneSummary.trim()
            return if (lane.isEmpty()) kind.label else "${kind.label} · $lane"
        }

    /** Rem can't be paused or deleted; only side agents show Pause agent / Delete agent. */
    val showsLifecycleActions: Boolean get() = kind == AgentKind.Side

    /** Only a side agent's lane (name / handles) is editable from About. */
    val isLaneEditable: Boolean get() = kind == AgentKind.Side
}

/** What the person asked the About tab to do. The host performs each — including confirming Delete. */
sealed interface AgentProfileAction {
    data object EditName : AgentProfileAction
    data object EditHandles : AgentProfileAction
    data class OpenService(val id: String) : AgentProfileAction
    data object AddService : AgentProfileAction
    data object Pause : AgentProfileAction
    data object Delete : AgentProfileAction
}

/** Approved copy for the About tab (Figma `2718:3055`, verbatim). */
object AgentProfileCopy {
    const val LaneHeader = "Lane"
    const val NameRow = "Name"
    const val HandlesRow = "Handles"
    const val Pause = "Pause agent"
    const val Delete = "Delete agent"
}
