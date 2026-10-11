package com.rem.designsystem.demo

import android.graphics.Bitmap
import android.os.Build
import android.view.inspector.WindowInspector
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives the Android Agenda New · Suggestions playground. Proves the optimistic Add / Move / Dismiss
 * outcomes, three-inline-versus-four-in-overflow, overflow persistence, last-removal / gap removal, the
 * empty day becoming populated, deterministic restoration, Done, and large-text reachability. The
 * paired screenshots alone do not prove these interactions — each assertion checks resulting state.
 */
class AgendaSuggestionsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val prepTitle = "Prep for tonight’s rehearsal"

    private fun capture(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5000)
        if (Build.VERSION.SDK_INT >= 29) {
            val committed = CountDownLatch(1)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val root = WindowInspector.getGlobalWindowViews().lastOrNull { it.isShown && it.hasWindowFocus() }
                    ?: compose.activity.window.decorView
                root.viewTreeObserver.registerFrameCommitCallback { root.postOnAnimation { committed.countDown() } }
                root.invalidate()
            }
            check(committed.await(5, TimeUnit.SECONDS)) { "No rendered frame committed before screenshot $name" }
        }
        val resolver = compose.activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RemSettingsPlayground")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        checkNotNull(resolver.openOutputStream(uri)).use {
            check(automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }

    private fun appearance(dark: Boolean = false, largeText: Boolean = false) {
        compose.activityRule.scenario.onActivity {
            it.intent.putExtra("settingsDark", dark)
            it.intent.putExtra("settingsLargeText", largeText)
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    private fun openAgenda(fixture: String = "Loaded") {
        // Select the Agenda data chip by its stable tag: other root pickers share labels such as "Empty".
        if (fixture != "Loaded") compose.onNodeWithTag("agendaFixture.$fixture").performScrollTo().performClick()
        compose.onNodeWithTag("openAgendaSuggestions").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Aug 13 2026").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun textExists(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test fun inlineOverflowAndNone() {
        openAgenda()
        compose.onNodeWithText("Suggestions").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.prep").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.move").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.review").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.setlist").assertDoesNotExist()
        capture("AgendaSuggestions-inline-journey-light")

        compose.onNodeWithTag("agendaSuggestions.seeMore").performScrollTo().performClick()
        compose.waitUntil(5000) { exists("agendaSuggestions.done") }
        compose.onNodeWithTag("agendaSuggestion.sheet.accept.setlist").assertExists()
        capture("AgendaSuggestions-overflow-journey-light")
        compose.onNodeWithTag("agendaSuggestions.done").performClick()
        compose.waitUntil(5000) { !exists("agendaSuggestions.done") }
        compose.onNodeWithText("Suggestions").assertExists()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5000) { exists("openAgendaSuggestions") }
        openAgenda("None")
        compose.onNodeWithText("Suggestions").assertDoesNotExist()
        compose.onNodeWithText("Reply to the venue").assertExists()
        capture("AgendaSuggestions-none-journey-light")
    }

    @Test fun addCreatesFivePMTaskAndPreservesRows() {
        openAgenda()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.prep").performScrollTo().performClick()
        compose.waitUntil(5000) { textExists(prepTitle) }
        compose.onNodeWithText("5:00 PM").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.prep").assertDoesNotExist()
        compose.onNodeWithText("Reply to the venue").assertExists()
        compose.onNodeWithText("Confirm rehearsal time").assertExists()
        compose.onNodeWithText("Coffee chat with a mentor").assertExists()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.setlist").assertExists()
        capture("AgendaSuggestions-added-light")
    }

    @Test fun moveReschedulesExistingRehearsal() {
        openAgenda()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.move").performScrollTo().performClick()
        compose.onNodeWithTag("agendaSuggestion.inline.accept.move").assertDoesNotExist()
        compose.onNodeWithText("Confirm rehearsal time").assertExists()
        compose.waitUntil(5000) { textExists("3:00 PM") }
        compose.onNodeWithTag("agendaSuggestion.inline.accept.prep").assertExists()
        capture("AgendaSuggestions-moved-light")
    }

    @Test fun dismissRemovesSuggestionWithoutCreatingTask() {
        openAgenda()
        compose.onNodeWithTag("agendaSuggestion.inline.dismiss.prep").performScrollTo().performClick()
        compose.waitUntil(5000) { !exists("agendaSuggestion.inline.accept.prep") }
        compose.onNodeWithText(prepTitle).assertDoesNotExist()
        compose.onNodeWithText("Reply to the venue").assertExists()
        compose.onNodeWithText("Confirm rehearsal time").assertExists()
        capture("AgendaSuggestions-dismissed-light")
    }

    @Test fun lastRemovalHidesSlotAndGap() {
        openAgenda()
        listOf("prep", "move", "review", "setlist").forEach { id ->
            compose.waitUntil(5000) { exists("agendaSuggestion.inline.dismiss.$id") }
            compose.onNodeWithTag("agendaSuggestion.inline.dismiss.$id").performScrollTo().performClick()
        }
        compose.onNodeWithText("Suggestions").assertDoesNotExist()
        compose.onNodeWithTag("agendaSuggestions.seeMore").assertDoesNotExist()
        compose.onNodeWithText("Reply to the venue").assertExists()
        capture("AgendaSuggestions-last-removal-light")
    }

    @Test fun emptyDayBecomesPopulatedWhileOverflowStaysOpen() {
        openAgenda("Empty")
        compose.onNodeWithText("No agenda yet").assertExists()
        compose.onNodeWithTag("agendaSuggestions.seeMore").performScrollTo().performClick()
        compose.waitUntil(5000) { exists("agendaSuggestions.done") }
        compose.onNodeWithTag("agendaSuggestion.sheet.accept.prep").performClick()
        compose.onNodeWithTag("agendaSuggestions.done").assertExists() // overflow persists
        compose.onNodeWithTag("agendaSuggestion.sheet.accept.setlist").assertExists()
        capture("AgendaSuggestions-empty-populated-light")
        compose.onNodeWithTag("agendaSuggestions.done").performClick()
        compose.waitUntil(5000) { textExists(prepTitle) }
        compose.onNodeWithText("No agenda yet").assertDoesNotExist()
        capture("AgendaSuggestions-empty-populated-after-done-light")
    }

    @Test fun optimisticRestoration() {
        openAgenda("Restoration")
        compose.onNodeWithTag("agendaSuggestion.inline.dismiss.prep").performScrollTo().performClick()
        compose.waitUntil(5000) { !exists("agendaSuggestion.inline.accept.prep") }
        compose.waitUntil(5000) { exists("agendaSuggestion.inline.accept.prep") }
        capture("AgendaSuggestions-restored-light")
    }

    @Test fun darkAppearance() {
        appearance(dark = true)
        openAgenda()
        compose.onNodeWithText("Suggestions").assertExists()
        capture("AgendaSuggestions-dark")
    }

    @Test fun largeTextReachability() {
        appearance(largeText = true)
        openAgenda()
        compose.onNodeWithText("Suggestions").assertExists()
        capture("AgendaSuggestions-large-text")
        compose.onNodeWithTag("agendaSuggestions.seeMore").performScrollTo().performClick()
        compose.waitUntil(5000) { exists("agendaSuggestions.done") }
        capture("AgendaSuggestions-large-text-overflow")
        compose.onNodeWithTag("agendaSuggestions.done").performClick()
    }
}
