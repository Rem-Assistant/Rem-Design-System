package com.rem.designsystem.agents

// New agent setup inputs (presentation-only) — the Compose twin of `Agents/NewAgentSetupModel.swift`.
//
// Figma frame **E · New agent — set up** (`2714:3538`), opened from the sidebar's **New agent** button.
// Like Grok's bot setup, naming the lane comes first: Name is the first focused field and **Create
// agent** stays disabled until it has a non-blank name. The DS never creates the agent: the host receives
// [NewAgentSetupAction.Create] and owns the request, its pending/failed states and navigation to the new
// agent's first chat (`E2`, `2717:2725`).

/** The editable draft behind the setup form. */
data class NewAgentDraft(
    val name: String = "",
    /** "What should this agent handle?" — the outcome / lane description. */
    val handles: String = "",
    val look: AgentLook = AgentLook.Default,
    /** Services the host has already attached to this draft (from "Add a service"). */
    val services: List<AgentService> = emptyList(),
) {
    /** The name as it will be shown in the sidebar and chat header. */
    val trimmedName: String get() = name.trim()

    /** The handles copy without surrounding whitespace; `null` when left blank (it is optional). */
    val trimmedHandles: String? get() = handles.trim().ifEmpty { null }

    /** Naming the lane is the one required step. */
    val canCreate: Boolean get() = trimmedName.isNotEmpty()

    /**
     * The action Create should emit, or `null` when the lane has no name yet. Trims what is sent so a
     * stray space never becomes part of the agent's name.
     */
    val createAction: NewAgentSetupAction.Create?
        get() = if (!canCreate) null else NewAgentSetupAction.Create(
            copy(name = trimmedName, handles = trimmedHandles ?: ""),
        )
}

/** What the person asked the setup screen to do. The host performs every one of these. */
sealed interface NewAgentSetupAction {
    data object Cancel : NewAgentSetupAction
    /** "Tap to change icon" — the host presents its icon chooser. */
    data object ChangeIcon : NewAgentSetupAction
    data object AddService : NewAgentSetupAction
    data class OpenService(val id: String) : NewAgentSetupAction
    /** Emitted only when [NewAgentDraft.canCreate]; carries the trimmed draft. */
    data class Create(val draft: NewAgentDraft) : NewAgentSetupAction
}

/** Approved copy for the setup screen (Figma `2714:3538`, verbatim). */
object NewAgentSetupCopy {
    const val Title = "New agent"
    const val Cancel = "Cancel"
    const val ChangeIcon = "Tap to change icon"
    const val NameHeader = "Name"
    const val NamePlaceholder = "e.g. Health"
    const val NameFooter = "Shown in your sidebar and at the top of its chat."
    const val HandlesHeader = "What should this agent handle?"
    const val HandlesPlaceholder = "Appointments, prescriptions, insurance claims…"
    const val HandlesFooter = "Describe the outcome you want. Rem routes anything in this lane here."
    const val ServicesFooter = "Optional. You can connect more later."
    const val Create = "Create agent"
}
