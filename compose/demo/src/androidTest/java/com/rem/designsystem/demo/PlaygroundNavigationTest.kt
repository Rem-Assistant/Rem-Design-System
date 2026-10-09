package com.rem.designsystem.demo

import android.graphics.Bitmap
import android.os.Build
import android.view.inspector.WindowInspector
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Root structure, the component catalog's loading demo, and the onboarding flow's step routing
 * (Android). All state is local fixture state — no account, service, audio, or persistence.
 */
class PlaygroundNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

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
                root.viewTreeObserver.registerFrameCommitCallback {
                    root.postOnAnimation { committed.countDown() }
                }
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

    private fun waitForTag(tag: String) {
        compose.waitUntil(5000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun systemBack() {
        Espresso.pressBack()
        compose.waitForIdle()
    }

    private fun openOnboardingStep(tag: String) {
        compose.onNodeWithTag("openOnboarding").performScrollTo().performClick()
        compose.onNodeWithTag(tag).performClick()
    }

    @Test fun rootOffersComponentsAndScreensOnly() {
        listOf("openComponents", "openSettings", "openAgendaSuggestions", "openOnboarding").forEach {
            compose.onNodeWithTag(it).assertExists()
        }
        compose.onNodeWithText("Shared controls").assertDoesNotExist()
        compose.onAllNodes(hasText("prototype", substring = true, ignoreCase = true)).assertCountEquals(0)
        compose.onNodeWithTag("playground.build").assertExists()
        capture("Playground-root-light")
    }

    @Test fun catalogLoadingShowsSkeletonThenContentAndActionProgress() {
        compose.onNodeWithTag("openComponents").performClick()
        compose.onNodeWithTag("openControls").assertExists()
        capture("Playground-components-light")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("openLoading").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("loading.skeleton").assertExists().assertContentDescriptionEquals("Loading content")
        compose.mainClock.autoAdvance = true
        capture("Loading-skeleton-light")
        compose.waitUntil(8000) { compose.onAllNodesWithText("Memory").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("loading.skeleton").assertDoesNotExist()
        capture("Loading-content-light")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("loading.refresh").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Refreshing").assertExists()
        compose.onNodeWithText("Memory").assertExists()
        compose.mainClock.autoAdvance = true
        capture("Loading-action-progress-light")
        compose.waitUntil(6000) { compose.onAllNodesWithText("Refresh").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openCatalogPage(tag: String) {
        compose.onNodeWithTag("openComponents").performClick()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun slug(text: String) = text.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("").split('-').filter { it.isNotEmpty() }.joinToString("-")

    /** One overview capture: [name] names the file, [anchor] is content inside the component (never its
     *  group heading). Scrolling moves only as far as needed, so an anchor reached by scrolling down
     *  lands at the bottom edge with its component above it, and one reached by scrolling back up
     *  lands at the top edge with its component below it. [last] picks the final match when the
     *  page repeats the text (the trace's "Working" footer follows the "Working" status pill). */
    private data class Shot(val name: String, val anchor: SemanticsMatcher, val last: Boolean = false)

    /** Unobscured overview captures: each page's top, then each component scrolled into view by its
     *  own content, before any interaction (so no keyboard or post-action scroll position). The
     *  execution trace is taller than the viewport, so it is captured at its bottom, then its top. */
    @Test fun catalogSectionCaptures() {
        compose.onNodeWithTag("openComponents").performClick()
        val pages = listOf(
            Triple("openControls", "controls", listOf(
                Shot("Buttons", hasText("Disabled")),
                Shot("Slider", hasText("50%")),
                Shot("Pills", hasText("Personal")),
            )),
            Triple("openRows", "rows", listOf(
                Shot("Section", hasText("Applies to this device.")),
                Shot("Connector row", hasText("Gmail")),
                Shot("Task and event rows", hasText("Unfiled inbox task")),
            )),
            Triple("openCatalogAgenda", "agenda", listOf(
                Shot("Suggestion rows", hasText("Reply to the venue")),
                Shot("Suggestion section", hasText("See more")),
            )),
            Triple("openChat", "chat", listOf(
                Shot("Composer", hasText("Auto")),
                Shot("Voice bar", hasText("Listening\u2026")),
            )),
            Triple("openAgentCatalog", "agent", listOf(
                Shot("Running task banner", hasText("Needs you \u00b7 Password rejected")),
                Shot("Browser card", hasText("Rem's browser session")),
                Shot("Execution trace bottom", hasText("Working"), last = true),
                Shot("Execution trace top", hasText("IN PROGRESS")),
                Shot("Daily brief card", hasText("Read latest brief")),
            )),
            Triple("openBrand", "brand", listOf(
                Shot("Provider marks", hasTestTag("catalog.providerMarks")),
                Shot("Empty state", hasText("Add New")),
            )),
        )
        pages.forEach { (tag, page, shots) ->
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.waitForIdle()
            capture("Catalog-$page-top-light")
            shots.forEach { shot ->
                val anchor = if (shot.last) compose.onAllNodes(shot.anchor).onLast() else compose.onNode(shot.anchor)
                anchor.performScrollTo().assertIsDisplayed()
                capture("Catalog-$page-${slug(shot.name)}-light")
            }
            compose.onNodeWithTag("back").performClick()
            compose.onNodeWithTag(tag).assertExists()
        }
    }

    @Test fun catalogListsEveryPage() {
        compose.onNodeWithTag("openComponents").performClick()
        listOf("openControls", "openRows", "openCatalogAgenda", "openChat", "openAgentCatalog", "openBrand", "openLoading").forEach {
            compose.onNodeWithTag(it).assertExists()
        }
    }

    @Test fun catalogControlsSliderAndPills() {
        openCatalogPage("openControls")
        compose.onNodeWithText("50%").performScrollTo().assertExists()
        // Drive the slider through its accessibility action (deterministic, unlike a touch swipe).
        compose.onNodeWithTag("controls.slider").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.onNodeWithText("100%").assertExists()
        compose.onNodeWithText("50%").assertDoesNotExist()
        compose.onNodeWithText("Personal").performScrollTo().assertExists()
        capture("Catalog-controls-light")
    }

    @Test fun catalogRowsListAndConnectorStates() {
        openCatalogPage("openRows")
        compose.onNodeWithTag("catalog.listRow").performClick()
        compose.onNodeWithText("Opened").assertExists()
        compose.onNodeWithText("Connect").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Connected").fetchSemanticsNodes().size >= 2 }
        capture("Catalog-rows-light")
    }

    @Test fun catalogAgendaDateAndSuggestions() {
        openCatalogPage("openCatalogAgenda")
        compose.onNodeWithText("Oct 1 2026").assertExists()
        compose.onNodeWithContentDescription("Next day").performClick()
        compose.onNodeWithText("Oct 2 2026").assertExists()
        compose.onNodeWithTag("catalog.suggestion.accept.add").performClick()
        compose.onNodeWithText("Added").assertExists()
        capture("Catalog-agenda-light")
        compose.onNodeWithTag("catalog.suggestion.restore").performClick()
        compose.onNodeWithTag("catalog.suggestion.accept.add").assertExists()
        // SuggestionSection bounds inline rows at three; See more reveals the fourth.
        compose.onNodeWithText("Book the venue for the offsite").assertDoesNotExist()
        compose.onNodeWithText("See more").performScrollTo().performClick()
        compose.onNodeWithText("Book the venue for the offsite").performScrollTo().assertExists()
    }

    @Test fun catalogChatComposerSendsMessage() {
        openCatalogPage("openChat")
        compose.onNodeWithTag("catalog.composerField").performTextInput("Plan my afternoon")
        compose.onNodeWithTag("catalog.composerSend").performClick()
        compose.onNodeWithText("Plan my afternoon").assertExists()
        capture("Catalog-chat-light")
    }

    @Test fun catalogAgentSurfaces() {
        openCatalogPage("openAgentCatalog")
        compose.onAllNodesWithText("Working")[0].assertExists()
        compose.onNodeWithTag("catalog.browserState.Ended").performScrollTo().performClick()
        compose.onNodeWithTag("catalog.browserState.Ended").assertIsSelected()
        compose.onNodeWithText("Your morning brief").performScrollTo().assertExists()
        capture("Catalog-agent-light")
    }

    @Test fun catalogBrandAndEmptyState() {
        openCatalogPage("openBrand")
        compose.onNodeWithTag("catalog.faceThinking").performClick()
        compose.onNodeWithText("Add New").performScrollTo().performClick()
        compose.onNodeWithTag("catalog.emptyReset").assertExists()
        capture("Catalog-brand-light")
        compose.onNodeWithTag("catalog.emptyReset").performClick()
        compose.onNodeWithText("Add New").assertExists()
        compose.onNodeWithContentDescription("Google").performScrollTo().assertExists()
    }

    /** Continue from Sign in walks every established step, in order, to the completion state. */
    @Test fun continueAdvancesThroughEveryStepToCompletion() {
        openOnboardingStep("openOnboardingSignIn")
        capture("Onboarding-signIn-light")
        compose.onNodeWithText("Continue with Apple").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Accept and Continue").fetchSemanticsNodes().isNotEmpty() }
        capture("Onboarding-consent-light")
        compose.onNodeWithText("Accept and Continue").performClick()
        compose.onNodeWithText("Connectors").assertExists()
        capture("Onboarding-connectors-light")
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Check-in").assertExists()
        capture("Onboarding-checkin-light")
        compose.onNodeWithText("Continue").performClick()
        waitForTag("onboardingVoice")
        compose.onNodeWithTag("onboardingVoice.continue").performClick()
        compose.onNodeWithTag("onboarding.complete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "continue"))
        capture("Onboarding-complete-light")
        compose.onNodeWithTag("onboarding.done").performClick()
        compose.onNodeWithTag("openOnboardingSignIn").assertExists()
    }

    /** Skip is available from Connectors onward and moves the flow on exactly like Continue. */
    @Test fun skipAdvancesFromConnectorsToCompletion() {
        openOnboardingStep("openOnboardingConnectors")
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Check-in").assertExists()
        compose.onNodeWithText("Skip").performClick()
        waitForTag("onboardingVoice")
        compose.onNodeWithTag("onboardingVoice.skip").performClick()
        compose.onNodeWithTag("onboarding.complete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "skip"))
    }

    /** System Back walks the pushed steps in reverse. */
    @Test fun backReturnsToPreviousStep() {
        openOnboardingStep("openOnboardingCheckIn")
        compose.onNodeWithText("Continue").performClick()
        waitForTag("onboardingVoice")
        systemBack()
        compose.onNodeWithText("Check-in").assertExists()
        systemBack()
        compose.onNodeWithTag("openOnboardingCheckIn").assertExists()
    }

    @Test fun consentLegalRowsOpenDocuments() {
        openOnboardingStep("openOnboardingConsent")
        compose.onNodeWithText("Terms of Service").performClick()
        // The document is a modal dialog; Done exists only there.
        compose.onNodeWithText("Done").assertExists()
        compose.onAllNodesWithText("How Rem accounts, subscriptions, and approved actions work.", useUnmergedTree = true).assertCountEquals(2)
        capture("Onboarding-consent-terms-light")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Done").assertDoesNotExist()
        compose.onAllNodesWithText("How Rem accounts, subscriptions, and approved actions work.", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Accept and Continue").assertExists()
    }

    @Test fun connectorRowTogglesLocalState() {
        openOnboardingStep("openOnboardingConnectors")
        // Gmail starts connected; Google Calendar and Slack offer Connect.
        compose.onAllNodesWithText("Connected").assertCountEquals(1)
        compose.onAllNodesWithText("Connect").assertCountEquals(2)
        compose.onAllNodesWithText("Connect")[1].performClick()
        compose.onAllNodesWithText("Connected").assertCountEquals(2)
        compose.onAllNodesWithText("Connect").assertCountEquals(1)
    }
}
