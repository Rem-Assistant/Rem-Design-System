package com.rem.designsystem

import com.rem.designsystem.agents.AgentColor
import com.rem.designsystem.agents.NewAgentDraft
import com.rem.designsystem.agents.NewAgentSetupAction
import com.rem.designsystem.navigation.RemSidebarAction
import com.rem.designsystem.navigation.RemSidebarSelection
import com.rem.designsystem.screens.PlaygroundShellFixture
import com.rem.designsystem.screens.PlaygroundShellPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Playground shell's state contract (`docs/playground/route-map.md`) — twin of `PlaygroundShellFixtureTests.swift`. */
class PlaygroundShellFixtureTest {
    private val shell = PlaygroundShellFixture()

    @Test fun opensOnTheMainAgentWithTheSidebarClosed() {
        assertEquals(RemSidebarSelection.Main, shell.selection)
        assertFalse(shell.isSidebarOpen)
        assertNull(shell.presented)
        assertEquals("Rem", shell.selectedAgent.name)
        assertEquals("The account pill names the Settings profile", "Avery Diaz", shell.sidebar.account.displayName)
        assertTrue(shell.sidebar.isMainSelected)
    }

    @Test fun selectingAnAgentShowsItsConversationAndClosesTheSidebar() {
        val side = shell.openSidebar().handle(RemSidebarAction.SelectSide("health"))
        assertEquals(RemSidebarSelection.Side("health"), side.selection)
        assertFalse(side.isSidebarOpen)
        assertEquals("Health", side.selectedAgent.name)
        assertEquals("side.health", side.conversationKey)
        val back = side.openSidebar().handle(RemSidebarAction.SelectMain)
        assertEquals(RemSidebarSelection.Main, back.selection)
        assertEquals("main", back.conversationKey)
    }

    @Test fun closingTheSidebarKeepsTheSelection() {
        val side = shell.handle(RemSidebarAction.SelectSide("money"))
        val closed = side.openSidebar().closeSidebar()
        assertEquals(side, closed)
    }

    @Test fun anUnknownAgentIsNeverSelected() {
        assertEquals(shell, shell.handle(RemSidebarAction.SelectSide("missing")))
    }

    @Test fun searchHasNoSurfaceYet() {
        val open = shell.openSidebar()
        assertEquals(open, open.handle(RemSidebarAction.Search))
    }

    @Test fun settingsIsPresentedOverTheSameConversationAndDismissesBackToIt() {
        val side = shell.handle(RemSidebarAction.SelectSide("travel"))
        val settings = side.openSidebar().handle(RemSidebarAction.OpenSettings)
        assertEquals(PlaygroundShellPresentation.Settings, settings.presented)
        assertFalse("The sidebar closes behind Settings", settings.isSidebarOpen)
        val returned = settings.dismissPresentation()
        assertEquals("Selection kept", RemSidebarSelection.Side("travel"), returned.selection)
        assertEquals(side, returned)
    }

    @Test fun newAgentCancelChangesNothing() {
        val sheet = shell.openSidebar().handle(RemSidebarAction.NewAgent)
        assertEquals(PlaygroundShellPresentation.NewAgent, sheet.presented)
        assertEquals(NewAgentDraft(), sheet.draft)
        val edited = sheet.editDraft(NewAgentDraft("Garden", AgentColor.Green))
        val cancelled = edited.handle(NewAgentSetupAction.Cancel)
        assertNull(cancelled.presented)
        assertEquals(shell.sideAgents, cancelled.sideAgents)
        assertEquals(RemSidebarSelection.Main, cancelled.selection)
        assertEquals("The next sheet starts empty", NewAgentDraft(), cancelled.handle(RemSidebarAction.NewAgent).draft)
    }

    @Test fun newAgentCreateAddsTheAgentAndOpensItsChat() {
        val sheet = shell.handle(RemSidebarAction.NewAgent).editDraft(NewAgentDraft("  Garden ", AgentColor.Green))
        val created = sheet.handle(sheet.draft.createAction!!)
        assertNull(created.presented)
        assertEquals("Garden", created.sideAgents.last().name)
        assertEquals(PlaygroundShellFixture.NewAgentLane, created.sideAgents.last().lane)
        assertEquals(RemSidebarSelection.Side(created.sideAgents.last().id), created.selection)
        assertEquals("Garden", created.selectedAgent.name)
        val second = created.handle(RemSidebarAction.NewAgent).editDraft(NewAgentDraft("Garden"))
        val twice = second.handle(second.draft.createAction!!)
        assertEquals("Ids stay unique", 2, twice.sideAgents.takeLast(2).map { it.id }.toSet().size)
    }

    @Test fun createWithoutANameOrOutsideTheSheetIsIgnored() {
        val sheet = shell.handle(RemSidebarAction.NewAgent)
        assertEquals(sheet, sheet.handle(NewAgentSetupAction.Create(NewAgentDraft("   "))))
        assertEquals(shell, shell.handle(NewAgentSetupAction.Create(NewAgentDraft("Garden"))))
        assertEquals(shell, shell.editDraft(NewAgentDraft("Garden")))
    }
}
