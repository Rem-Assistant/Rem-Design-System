package com.rem.designsystem.agents

import androidx.compose.ui.unit.dp

// New agent setup inputs (presentation-only) — the Compose twin of `Agents/NewAgentSetupModel.swift`.
//
// The **New agent** sheet, opened from the sidebar's **New agent** button. Samuel (2026-10-11): "just a
// name and some color is fine." The person names the agent (the first, focused field) and picks one
// colour from the fixed [AgentColor] palette; a large avatar previews the colour live. **Create agent**
// stays disabled until the trimmed name is non-blank. What the agent handles, services and its icon are
// not part of setup. The DS never creates the agent: the host receives [NewAgentSetupAction.Create] and
// owns the request, its pending/failed states and navigation to the new agent's first chat.

/** The editable draft behind the setup sheet: a name and a colour. */
data class NewAgentDraft(
    val name: String = "",
    val color: AgentColor = AgentLook.Default.color,
) {
    /** The name as it will be shown in the sidebar and chat header. */
    val trimmedName: String get() = name.trim()

    /** Naming the agent is the one required step. */
    val canCreate: Boolean get() = trimmedName.isNotEmpty()

    /** The look the new agent gets: the default glyph on the chosen colour (no glyph chooser). */
    val look: AgentLook get() = AgentLook(glyph = AgentLook.Default.glyph, color = color)

    /**
     * The action Create should emit, or `null` when the agent has no name yet. Trims what is sent so a
     * stray space never becomes part of the agent's name.
     */
    val createAction: NewAgentSetupAction.Create?
        get() = if (!canCreate) null else NewAgentSetupAction.Create(copy(name = trimmedName))
}

/** What the person asked the setup sheet to do. The host performs every one of these. */
sealed interface NewAgentSetupAction {
    /** The top-leading close (X) button — dismiss without creating. */
    data object Cancel : NewAgentSetupAction
    /** Emitted only when [NewAgentDraft.canCreate]; carries the draft with its name trimmed. */
    data class Create(val draft: NewAgentDraft) : NewAgentSetupAction
}

/** Approved copy for the setup sheet (recorded in `docs/contracts/sidebar-new-agent.md`). */
object NewAgentSetupCopy {
    const val Title = "New agent"
    /** Content description of the X button. */
    const val Close = "Close"
    /** Content description of the name field (the placeholder carries the visible prompt). */
    const val NameLabel = "Name"
    const val NamePlaceholder = "Agent name"
    /** Content description of the swatch group. */
    const val ColorsLabel = "Color"
    const val Create = "Create agent"
    /** TalkBack state description on Create while it is disabled. */
    const val CreateHint = "Name your agent to continue."
}

/** Metrics shared with the SwiftUI twin (`NewAgentSetupLayout` in `NewAgentSetupModel.swift`). */
object NewAgentSetupLayout {
    /** The live avatar preview. */
    val AvatarSize = 120.dp
    /** Each swatch's touch target (and the selected ring's outer diameter). */
    val SwatchTarget = 44.dp
    /** The coloured disc inside a swatch; the gap to the ring shows the selection. */
    val SwatchDisc = 34.dp
    /** The palette wraps into rows of this many swatches (11 colours → 6 + 5). */
    const val SwatchesPerRow = 6
    /** Minimum height of the name pill; it grows with font scale. */
    val NameFieldMinHeight = 56.dp

    /** The palette split into rows, in [AgentColor.entries] order. */
    val swatchRows: List<List<AgentColor>> get() = AgentColor.entries.chunked(SwatchesPerRow)
}
