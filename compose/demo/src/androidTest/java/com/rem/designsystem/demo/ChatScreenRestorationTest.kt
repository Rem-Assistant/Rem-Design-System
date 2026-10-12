package com.rem.designsystem.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ChatHeaderDisplay
import com.rem.designsystem.screens.ChatJumpToLatest
import com.rem.designsystem.screens.ChatScreen
import com.rem.designsystem.tokens.RemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * `ChatScreen` across a saved-state round trip that also changes the viewport (the shape of a rotation that
 * recreates the activity): at or near the latest message it re-pins to the latest; scrolled up it keeps the
 * same distance from the bottom. `StateRestorationTester` disposes the composition and recreates it from the
 * saved state. The viewport height is a plain field, not snapshot state: changing it does not relayout the old
 * composition, so the save sees the old layout and only the recreated composition sees the new one, as when a
 * rotation destroys and recreates the activity. Both heights fit the smallest hosted emulator screen.
 */
class ChatScreenRestorationTest {
    @get:Rule val compose = createComposeRule()

    private var viewport = PORTRAIT

    private fun StateRestorationTester.showLongConversation() = setContent {
        RemTheme {
            Box(Modifier.fillMaxWidth().height(viewport)) {
                ChatScreen(
                    header = ChatHeaderDisplay(activity = "Ready"),
                    composer = ChatComposerState(),
                    onAction = {},
                ) {
                    repeat(ROWS) { i -> Text("Message $i", Modifier.height(48.dp).testTag("row.$i")) }
                }
            }
        }
    }

    /** px between the latest row's top and the composer field's top: the distance from the bottom. */
    private fun latestFromDock(): Float {
        val latest = compose.onNodeWithTag("row.${ROWS - 1}", useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y
        val field = compose.onNodeWithTag("chat.composerField").fetchSemanticsNode().boundsInRoot.top
        return latest - field
    }

    private fun assertAtLatest(step: String) {
        val jump = compose.onAllNodes(hasTestTag(ChatJumpToLatest.TEST_TAG)).fetchSemanticsNodes()
        assertTrue("$step: jump to latest must be hidden at the latest message", jump.isEmpty())
        val latest = compose.onNodeWithTag("row.${ROWS - 1}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val field = compose.onNodeWithTag("chat.composerField").fetchSemanticsNode().boundsInRoot
        assertTrue("$step: latest row bottom ${latest.bottom} must sit above the composer field top ${field.top}", latest.bottom <= field.top)
        assertTrue("$step: latest row must be on screen (height ${latest.height})", latest.height > 0f)
    }

    @Test fun atLatestRePinsAfterRecreationWithATallerScrollRange() {
        val restoration = StateRestorationTester(compose)
        restoration.showLongConversation()
        compose.waitForIdle()
        assertAtLatest("before")

        viewport = LANDSCAPE // a shorter viewport grows the scroll range, as rotation to landscape does
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertAtLatest("after recreation")
    }

    @Test fun scrolledUpKeepsItsDistanceFromTheBottomAfterRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.showLongConversation()
        compose.waitForIdle()
        compose.onNodeWithTag("chat.content").performScrollToNode(hasTestTag("row.20"))
        compose.waitForIdle()
        compose.onNodeWithTag(ChatJumpToLatest.TEST_TAG).assertExists()
        val before = latestFromDock()

        viewport = LANDSCAPE
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag(ChatJumpToLatest.TEST_TAG).assertExists()
        val after = latestFromDock()
        assertEquals("distance of the latest row below the dock is kept", before, after, 2f)
    }

    private companion object {
        const val ROWS = 60
        val PORTRAIT = 560.dp
        val LANDSCAPE = 360.dp
    }
}
