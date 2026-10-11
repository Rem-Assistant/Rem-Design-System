package com.rem.designsystem.agents

import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.icons.RemMaterialSymbols

// Shared agent identity inputs (presentation-only) — the Compose twin of `Agents/AgentModels.swift`.
//
// Figma page `Proposed · Navigation & Daily Brief` (`2693:1474`): the sidebar (`2702:2479`), New agent
// setup (`2714:3538`) and agent profile (`2718:3055` / `2718:3396`) show the same agent identity. The DS
// never creates, persists or routes agents. Contract: `docs/contracts/sidebar-new-agent.md`.

/** Exactly one **Main agent** (Rem) exists; every user-created agent is a **Side agent**. */
enum class AgentKind(val label: String) {
    Main("Main agent"),
    Side("Side agent"),
}

/** The colour swatches offered by New agent setup, in the Figma order (`2714:3590`). */
enum class AgentColor(val id: String, val accessibilityName: String) {
    Pink("pink", "Pink"),
    Blue("blue", "Blue"),
    Green("green", "Green"),
    Purple("purple", "Purple"),
    Indigo("indigo", "Indigo"),
}

/**
 * The glyph on a side agent's avatar. Only the glyph the approved frames show is registered; the icon
 * chooser behind "Tap to change icon" is not designed yet, so the host owns that picker. Each entry is a
 * researched row in `docs/contracts/icon-registry.md` (SF `heart.fill` ↔ `favorite`, FILL 1).
 */
enum class AgentGlyph(val symbol: RemMaterialSymbol) {
    Heart(RemMaterialSymbols.AgentHeart),
}

/** A side agent's look: glyph + colour. The Main agent always renders the Rem face instead. */
data class AgentLook(
    val glyph: AgentGlyph = AgentGlyph.Heart,
    val color: AgentColor = AgentColor.Pink,
) {
    companion object {
        /** The look a new agent starts with (the E frame's default selection). */
        val Default = AgentLook()
    }
}

/** The glyph on a connected-service row's tinted tile. */
enum class AgentServiceGlyph(val symbol: RemMaterialSymbol) {
    /** A calendar connector, e.g. Google Calendar (SF `calendar` ↔ `calendar_month`). */
    Calendar(RemMaterialSymbols.CalendarService),
    /** Generic service / the "Add a service" row (SF `link` ↔ `link`). */
    Link(RemMaterialSymbols.AddService),
}

/** One service an agent may use (host-supplied name and status copy, e.g. "Connected"). */
data class AgentService(
    val id: String,
    val name: String,
    /** Host copy for the service state; `null` hides the subtitle. */
    val status: String? = null,
    val glyph: AgentServiceGlyph = AgentServiceGlyph.Link,
)

/** Copy shared by every surface that lists an agent's services. */
object AgentServicesCopy {
    const val Header = "Connected services"
    const val AddService = "Add a service"
}
