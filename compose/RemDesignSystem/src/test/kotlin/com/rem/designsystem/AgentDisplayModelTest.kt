package com.rem.designsystem

import androidx.compose.ui.unit.dp
import com.rem.designsystem.agents.AgentAvatarIdentity
import com.rem.designsystem.agents.AgentColor
import com.rem.designsystem.agents.AgentGlyph
import com.rem.designsystem.agents.AgentKind
import com.rem.designsystem.agents.AgentLook
import com.rem.designsystem.agents.AgentProfileCopy
import com.rem.designsystem.agents.AgentProfileDisplay
import com.rem.designsystem.agents.AgentServiceGlyph
import com.rem.designsystem.agents.AgentServicesCopy
import com.rem.designsystem.agents.NewAgentDraft
import com.rem.designsystem.agents.NewAgentSetupAction
import com.rem.designsystem.agents.NewAgentSetupCopy
import com.rem.designsystem.agents.NewAgentSetupLayout
import com.rem.designsystem.icons.RemMaterialSymbols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Display-model contract for New agent setup + Agent profile — twin of `AgentDisplayModelTests.swift`. */
class AgentDisplayModelTest {

    @Test
    fun kindLabelsAreTheApprovedCopy() {
        assertEquals("Main agent", AgentKind.Main.label)
        assertEquals("Side agent", AgentKind.Side.label)
    }

    @Test
    fun paletteIsElevenColoursInSpectrumOrderWithTheDefaultLook() {
        assertEquals(
            listOf(
                AgentColor.Red, AgentColor.Orange, AgentColor.Yellow, AgentColor.Green, AgentColor.Mint,
                AgentColor.Teal, AgentColor.Cyan, AgentColor.Blue, AgentColor.Indigo, AgentColor.Purple, AgentColor.Pink,
            ),
            AgentColor.entries.toList(),
        )
        // Ids match the SwiftUI raw values (used in `newAgent.color.<id>`).
        assertEquals(
            listOf("red", "orange", "yellow", "green", "mint", "teal", "cyan", "blue", "indigo", "purple", "pink"),
            AgentColor.entries.map { it.id },
        )
        assertEquals("Blue", AgentColor.Blue.accessibilityName)
        assertEquals(AgentLook(AgentGlyph.Heart, AgentColor.Pink), AgentLook.Default)
        // Registry pairs: heart.fill ↔ favorite (FILL 1), calendar ↔ calendar_month, link ↔ link.
        assertEquals(RemMaterialSymbols.AgentHeart, AgentGlyph.Heart.symbol)
        assertEquals(1f, AgentGlyph.Heart.symbol.fill)
        assertEquals(RemMaterialSymbols.CalendarService, AgentServiceGlyph.Calendar.symbol)
        assertEquals(RemMaterialSymbols.AddService, AgentServiceGlyph.Link.symbol)
    }

    @Test
    fun swatchesWrapIntoTwoRows() {
        assertEquals(listOf(6, 5), NewAgentSetupLayout.swatchRows.map { it.size })
        assertEquals(AgentColor.entries.toList(), NewAgentSetupLayout.swatchRows.flatten())
        assertTrue("44dp touch target", NewAgentSetupLayout.SwatchTarget >= 44.dp)
    }

    @Test
    fun setupCopyIsVerbatim() {
        assertEquals("New agent", NewAgentSetupCopy.Title)
        assertEquals("Create agent", NewAgentSetupCopy.Create)
        assertEquals("Close", NewAgentSetupCopy.Close)
        assertEquals("Name", NewAgentSetupCopy.NameLabel)
        assertEquals("Agent name", NewAgentSetupCopy.NamePlaceholder)
        assertEquals("Color", NewAgentSetupCopy.ColorsLabel)
        assertEquals("Connected services", AgentServicesCopy.Header)
        assertEquals("Add a service", AgentServicesCopy.AddService)
    }

    @Test
    fun draftStartsEmptyOnTheDefaultColour() {
        val draft = NewAgentDraft()
        assertEquals("", draft.name)
        assertEquals(AgentColor.Pink, draft.color)
        assertEquals(AgentLook.Default.color, draft.color)
        assertEquals(AgentLook.Default, draft.look)
    }

    @Test
    fun createIsDisabledUntilTheAgentHasAName() {
        assertFalse(NewAgentDraft().canCreate)
        assertNull(NewAgentDraft().createAction)
        assertFalse("whitespace is not a name", NewAgentDraft(name = "   \n ").canCreate)
        assertNull(NewAgentDraft(name = "   \n ").createAction)
        assertFalse("a colour alone is not enough", NewAgentDraft(color = AgentColor.Teal).canCreate)
        assertTrue("the colour is optional (it has a default)", NewAgentDraft(name = "Health").canCreate)
        assertNotNull(NewAgentDraft(name = "Health").createAction)
    }

    @Test
    fun createSendsATrimmedName() {
        assertEquals("Health", requireNotNull(NewAgentDraft(name = "  Health \n").createAction).draft.name)
    }

    @Test
    fun createCarriesTheSelectedColour() {
        val draft = NewAgentDraft(name = "Money").copy(color = AgentColor.Indigo)
        assertEquals("the preview follows the selection", AgentLook(AgentGlyph.Heart, AgentColor.Indigo), draft.look)
        assertEquals(NewAgentSetupAction.Create(NewAgentDraft(name = "Money", color = AgentColor.Indigo)), draft.createAction)
        val sent = requireNotNull(draft.createAction).draft
        assertEquals(AgentColor.Indigo, sent.color)
        assertEquals(AgentLook(AgentGlyph.Heart, AgentColor.Indigo), sent.look)
    }

    @Test
    fun createWithoutPickingKeepsTheDefaultColour() {
        assertEquals(AgentLook.Default.color, requireNotNull(NewAgentDraft(name = "Travel").createAction).draft.color)
    }

    private val health = AgentProfileDisplay(
        kind = AgentKind.Side, name = "Health", laneSummary = "Appointments and refills",
        handles = "Appointments, prescriptions, insurance claims",
    )
    private val rem = AgentProfileDisplay(
        kind = AgentKind.Main, name = "Rem", laneSummary = "Daily Brief, sees all your lanes",
        handles = "Daily Brief and anything outside a side agent's lane",
    )

    @Test
    fun statusLineJoinsKindAndLane() {
        assertEquals("Side agent · Appointments and refills", health.statusLine)
        assertEquals("Main agent · Daily Brief, sees all your lanes", rem.statusLine)
        assertEquals("Side agent", health.copy(laneSummary = "  ").statusLine)
    }

    @Test
    fun remCannotBePausedDeletedOrRenamed() {
        assertTrue(health.showsLifecycleActions)
        assertTrue(health.isLaneEditable)
        assertFalse(rem.showsLifecycleActions)
        assertFalse(rem.isLaneEditable)
    }

    @Test
    fun avatarIdentityFollowsKind() {
        assertEquals(AgentAvatarIdentity.Main, rem.avatar)
        assertEquals(AgentAvatarIdentity.Side(AgentLook.Default), health.avatar)
    }

    @Test
    fun profileCopyIsVerbatim() {
        assertEquals("Lane", AgentProfileCopy.LaneHeader)
        assertEquals("Name", AgentProfileCopy.NameRow)
        assertEquals("Handles", AgentProfileCopy.HandlesRow)
        assertEquals("Pause agent", AgentProfileCopy.Pause)
        assertEquals("Delete agent", AgentProfileCopy.Delete)
    }
}
