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

    @Test fun catalogListsEveryPage() {
        compose.onNodeWithTag("openComponents").performClick()
        listOf("openControls", "openRows", "openCatalogAgenda", "openChat", "openAgent", "openBrand", "openLoading").forEach {
            compose.onNodeWithTag(it).assertExists()
        }
    }

    @Test fun catalogControlsSliderAndPills() {
        openCatalogPage("openControls")
        compose.onNodeWithText("50%").performScrollTo().assertExists()
        compose.onNodeWithTag("controls.slider").performScrollTo().performTouchInput { swipeRight() }
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
        openCatalogPage("openAgent")
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
