package com.rem.designsystem.agents

import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.icons.RemMaterialSymbols

// Shared agent identity inputs (presentation-only) — the Compose twin of `Agents/AgentModels.swift`.
//
// Figma page `Proposed · Navigation & Daily Brief` (`2693:1474`): the sidebar (`2702:2479`), New agent
// setup (`2714:3538`, superseded by the name + colour sheet) and agent profile (`2718:3055` / `2718:3396`) show the same agent identity. The DS
// never creates, persists or routes agents. Contract: `docs/contracts/sidebar-new-agent.md`.

/** Exactly one **Main agent** (Rem) exists; every user-created agent is a **Side agent**. */
enum class AgentKind(val label: String) {
    Main("Main agent"),
    Side("Side agent"),
}

/**
 * The fixed agent colour palette offered by New agent setup — eleven Apple system colours in spectrum
 * order, each bound to a `RemColors.system*` token ([swatch]). The chosen colour is the agent's
 * identity colour (avatar now; its chat theme later). Same cases, order and tokens as the SwiftUI
 * `AgentColor`.
 */
enum class AgentColor(val id: String, val accessibilityName: String) {
    Red("red", "Red"),
    Orange("orange", "Orange"),
    Yellow("yellow", "Yellow"),
    Green("green", "Green"),
    Mint("mint", "Mint"),
    Teal("teal", "Teal"),
    Cyan("cyan", "Cyan"),
    Blue("blue", "Blue"),
    Indigo("indigo", "Indigo"),
    Purple("purple", "Purple"),
    Pink("pink", "Pink"),
}

/**
 * The glyph on a side agent's avatar. New agent setup offers no glyph chooser (name + colour only), so
 * every new agent starts with the default glyph. Each entry is a researched row in `docs/contracts/icon-registry.md` (SF `heart.fill` ↔ `favorite`, FILL 1).
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
        /** The look a new agent starts with: the default glyph on the default colour (pink). */
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
