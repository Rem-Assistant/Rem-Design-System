package com.rem.designsystem

import com.rem.designsystem.navigation.RemSidebarAccount
import com.rem.designsystem.navigation.RemSidebarAction
import com.rem.designsystem.navigation.RemSidebarAgent
import com.rem.designsystem.navigation.RemSidebarDisplay
import com.rem.designsystem.navigation.RemSidebarLayout
import com.rem.designsystem.navigation.RemSidebarSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Display-model contract for `RemSidebar` — twin of `RemSidebarModelTests.swift`. */
class RemSidebarModelTest {
    private val main = RemSidebarAgent("rem", "Rem", "Daily Brief ready")
    private val health = RemSidebarAgent("health", "Health", "Appointments & meds")
    private val money = RemSidebarAgent("money", "Money", "Bills & budget", hasUnread = true)

    @Test
    fun approvedCopyIsExact() {
        assertEquals("Rem", RemSidebarDisplay.Title)
        assertEquals("Main agent", RemSidebarDisplay.MainAgentHeader)
        assertEquals("Side agents", RemSidebarDisplay.SideAgentsHeader)
        assertEquals("New agent", RemSidebarDisplay.NewAgent)
        assertEquals("Search", RemSidebarDisplay.Search)
        assertEquals("Opens Settings", RemSidebarDisplay.AccountHint)
    }

    @Test
    fun selectionHighlightsExactlyTheNamedConversation() {
        val display = RemSidebarDisplay(main, listOf(health, money), RemSidebarAccount("Samuel"))
        assertTrue("Main agent is the default selection", display.isMainSelected)
        assertFalse(display.isSelected(health))

        val moneySelected = display.copy(selection = RemSidebarSelection.Side("money"))
        assertFalse(moneySelected.isMainSelected)
        assertFalse(moneySelected.isSelected(health))
        assertTrue(moneySelected.isSelected(money))
    }

    @Test
    fun staleOrMissingSelectionHighlightsNothing() {
        val stale = RemSidebarDisplay(main, listOf(health), RemSidebarAccount("Samuel"), RemSidebarSelection.Side("deleted"))
        assertFalse(stale.isMainSelected)
        assertFalse(stale.isSelected(health))

        val none = stale.copy(selection = null)
        assertFalse(none.isMainSelected)
        assertFalse(none.isSelected(health))
    }

    @Test
    fun sideAgentsSectionNeverDrawsALoneHeader() {
        assertFalse(RemSidebarDisplay(main, emptyList(), RemSidebarAccount("S")).showsSideAgents)
        assertTrue(RemSidebarDisplay(main, listOf(health), RemSidebarAccount("S")).showsSideAgents)
    }

    @Test
    fun accountPillInitialAndName() {
        assertEquals("S", RemSidebarAccount("Samuel").initial)
        assertEquals("S", RemSidebarAccount("  samuel  ").initial)
        assertEquals("samuel", RemSidebarAccount("  samuel  ").displayName)
        assertEquals("?", RemSidebarAccount("   ").initial)
        assertEquals("É", RemSidebarAccount("Élodie").initial)
    }

    @Test
    fun pushGeometryLeavesThePeekVisibleAndCapsWidth() {
        assertEquals(332f, RemSidebarLayout.sidebarWidth(402f), 0f)
        assertEquals(332f, RemSidebarLayout.contentOffset(isOpen = true, containerWidth = 402f), 0f)
        assertEquals(0f, RemSidebarLayout.contentOffset(isOpen = false, containerWidth = 402f), 0f)
        assertEquals(305f, RemSidebarLayout.sidebarWidth(375f), 0f)
        assertEquals(332f, RemSidebarLayout.sidebarWidth(1024f), 0f)
        assertEquals(0f, RemSidebarLayout.sidebarWidth(40f), 0f)
    }

    @Test
    fun actionsAreTyped() {
        assertEquals(RemSidebarAction.SelectSide("health"), RemSidebarAction.SelectSide("health"))
        assertNotEquals(RemSidebarAction.SelectSide("health"), RemSidebarAction.SelectSide("money"))
        assertNotEquals(RemSidebarAction.OpenSettings as RemSidebarAction, RemSidebarAction.NewAgent as RemSidebarAction)
    }
}
