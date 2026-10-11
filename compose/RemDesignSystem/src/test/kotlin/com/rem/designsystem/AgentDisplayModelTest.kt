package com.rem.designsystem

import com.rem.designsystem.agents.AgentAvatarIdentity
import com.rem.designsystem.agents.AgentColor
import com.rem.designsystem.agents.AgentGlyph
import com.rem.designsystem.agents.AgentKind
import com.rem.designsystem.agents.AgentLook
import com.rem.designsystem.agents.AgentProfileCopy
import com.rem.designsystem.agents.AgentProfileDisplay
import com.rem.designsystem.agents.AgentService
import com.rem.designsystem.agents.AgentServiceGlyph
import com.rem.designsystem.agents.AgentServicesCopy
import com.rem.designsystem.agents.NewAgentDraft
import com.rem.designsystem.agents.NewAgentSetupCopy
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
    fun swatchesKeepTheFigmaOrderAndDefaultLook() {
        assertEquals(
            listOf(AgentColor.Pink, AgentColor.Blue, AgentColor.Green, AgentColor.Purple, AgentColor.Indigo),
            AgentColor.entries.toList(),
        )
        assertEquals(AgentLook(AgentGlyph.Heart, AgentColor.Pink), AgentLook.Default)
        // Registry pairs: heart.fill ↔ favorite (FILL 1), calendar ↔ calendar_month, link ↔ link.
        assertEquals(RemMaterialSymbols.AgentHeart, AgentGlyph.Heart.symbol)
        assertEquals(1f, AgentGlyph.Heart.symbol.fill)
        assertEquals(RemMaterialSymbols.CalendarService, AgentServiceGlyph.Calendar.symbol)
        assertEquals(RemMaterialSymbols.AddService, AgentServiceGlyph.Link.symbol)
    }

    @Test
    fun setupCopyIsVerbatim() {
        assertEquals("New agent", NewAgentSetupCopy.Title)
        assertEquals("Create agent", NewAgentSetupCopy.Create)
        assertEquals("Name", NewAgentSetupCopy.NameHeader)
        assertEquals("What should this agent handle?", NewAgentSetupCopy.HandlesHeader)
        assertEquals("Connected services", AgentServicesCopy.Header)
        assertEquals("Add a service", AgentServicesCopy.AddService)
    }

    @Test
    fun namingTheLaneIsTheOnlyRequiredStep() {
        assertFalse(NewAgentDraft().canCreate)
        assertNull(NewAgentDraft().createAction)
        assertFalse("whitespace is not a name", NewAgentDraft(name = "   \n ").canCreate)
        assertNull(NewAgentDraft(name = "   \n ").createAction)
        assertTrue("handles and services are optional", NewAgentDraft(name = "Health").canCreate)
        assertNotNull(NewAgentDraft(name = "Health").createAction)
    }

    @Test
    fun createSendsATrimmedDraft() {
        val draft = NewAgentDraft(
            name = "  Health ",
            handles = "  Appointments, prescriptions  ",
            look = AgentLook(AgentGlyph.Heart, AgentColor.Indigo),
            services = listOf(AgentService("gcal", "Google Calendar", "Connected", AgentServiceGlyph.Calendar)),
        )
        val sent = requireNotNull(draft.createAction).draft
        assertEquals("Health", sent.name)
        assertEquals("Appointments, prescriptions", sent.handles)
        assertEquals(AgentLook(AgentGlyph.Heart, AgentColor.Indigo), sent.look)
        assertEquals(listOf("gcal"), sent.services.map { it.id })
    }

    @Test
    fun blankHandlesAreOmitted() {
        assertNull(NewAgentDraft(name = "Money", handles = "  ").trimmedHandles)
        assertEquals("", requireNotNull(NewAgentDraft(name = "Money", handles = "  ").createAction).draft.handles)
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
