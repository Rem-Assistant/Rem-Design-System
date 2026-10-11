package com.rem.designsystem.demo

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Acceptance for the shell's shared entry route (`docs/playground/route-map.md`): launch on the root
 * conversation → header sidebar button → account pill → Settings. Every Developer-hub journey's `@Before`
 * walks this route, so it stands alone here, with no `@Before`, and dumps the semantics trees (merged and
 * unmerged, log tag `SHELL`) at each step so a failure shows what was on screen.
 */
class PlaygroundShellRouteTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun dump(step: String) {
        compose.onRoot().printToLog("SHELL-$step-merged")
        compose.onRoot(useUnmergedTree = true).printToLog("SHELL-$step-unmerged")
    }

    @Test fun launchSidebarAccountOpensSettings() {
        compose.waitForTag("chat.header.menu")
        dump("launch")
        compose.onNodeWithTag("chat.header.menu").assert(hasContentDescription("Open sidebar")).performClick()
        compose.waitForTag("sidebar.close")
        dump("sidebar")
        compose.onNodeWithTag("sidebar.account").assertIsDisplayed()
            .assert(hasContentDescription("Avery Diaz", substring = true))
            .performClick()
        compose.waitForTag("shell.settings")
        dump("settings")
        compose.onNodeWithTag("settings.developer").assertIsDisplayed()
        compose.onNodeWithTag("openAgent").assertIsDisplayed()
    }

    /** chat.md rule 12: at rest, the latest message sits wholly above the composer dock. */
    @Test fun rootConversationLatestMessageClearsTheComposer() {
        compose.waitForTag("message.a2.bubble")
        compose.waitForIdle()
        val bubble = compose.onNodeWithTag("message.a2.bubble", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val field = compose.onNodeWithTag("chat.composerField").fetchSemanticsNode().boundsInRoot
        if (bubble.bottom > field.top) dump("latest-under-dock")
        assertTrue("Latest bubble bottom ${bubble.bottom} must sit above the composer field top ${field.top}", bubble.bottom <= field.top)
    }
}
