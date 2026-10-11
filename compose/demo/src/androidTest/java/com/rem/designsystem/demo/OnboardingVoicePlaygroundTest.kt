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
 * Interactive journeys for the Onboarding New → Voice playground shell (Android). These prove the
 * behavior a static render cannot: default → chooser → preview → select Sol → Back reflecting Sol,
 * independent preview/selection, three persistent slider changes, preview stop, the distinct
 * Back / Continue / Skip host callbacks, and scroll + large-text reachability of the pinned actions.
 * All state is local fixture state — no audio, account, service, or persistence.
 */
class OnboardingVoicePlaygroundTest {
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

    private fun appearance(dark: Boolean = false, largeText: Boolean = false) {
        compose.activityRule.scenario.onActivity {
            it.intent.putExtra("settingsDark", dark)
            it.intent.putExtra("settingsLargeText", largeText)
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    private fun systemBack() {
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        compose.waitForIdle()
    }

    private fun openOnboardingVoice() {
        compose.onNodeWithTag("openOnboarding").performScrollTo().performClick()
        compose.onNodeWithTag("openOnboardingVoice").performClick()
        waitForTag("onboardingVoice")
    }

    private fun assertVoiceSlider(id: String, expected: Float) {
        compose.onNodeWithTag("voice.slider.$id").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(expected, 0f..1f),
            ),
        )
    }

    @Test fun defaultChooserPreviewSelectionBackAndSliders() {
        openOnboardingVoice()
        compose.onNodeWithText("Choose how Rem sounds").assertExists()
        compose.onNodeWithText("Aria (Warm)").assertExists()
        capture("OnboardingVoice-light")

        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        waitForTag("onboardingVoiceChooser")
        val voices = listOf("aria", "sol", "rowan", "juniper", "vale")
        voices.forEach { compose.onNodeWithTag("voice.select.$it").assertExists() }
        compose.onNodeWithTag("voice.select.aria").assertIsSelected()
        capture("OnboardingVoice-chooser-light")

        // Preview is a separate target from selection.
        compose.onNodeWithTag("voice.preview.aria").performClick()
        compose.onNodeWithTag("voice.preview.aria").assert(hasContentDescription("Pause Aria", substring = true))
        capture("OnboardingVoice-chooser-preview-light")

        compose.onNodeWithTag("voice.preview.aria").performClick()
        compose.onNodeWithTag("voice.preview.rowan").performClick()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Pause Rowan", substring = true))
        compose.onNodeWithTag("voice.select.aria").assertIsSelected()
        compose.onNodeWithTag("voice.select.rowan").assertIsNotSelected()

        // Selecting Sol must not change the running Rowan preview (independent state).
        compose.onNodeWithTag("voice.select.sol").performClick().assertIsSelected()
        compose.onNodeWithTag("voice.select.aria").assertIsNotSelected()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Pause Rowan", substring = true))

        // Native Back returns to the shell reflecting Sol with preview stopped.
        systemBack()
        waitForTag("onboardingVoice")
        compose.onNodeWithText("Sol (Bright)").assertExists()
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Preview Sol", substring = true))
        capture("OnboardingVoice-selected-light")

        // Preview can independently start and stop from the shell.
        compose.onNodeWithTag("voice.previewSelected").performClick()
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Pause Sol", substring = true))
        compose.onNodeWithTag("voice.previewSelected").performClick()
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Preview Sol", substring = true))

        // Three persistent slider changes via the native slider semantics.
        val values = listOf("speed" to 0.75f, "consistency" to 0.50f, "likeness" to 0.75f)
        values.forEach { (id, target) ->
            compose.onNodeWithTag("voice.slider.$id").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(target) }
            assertVoiceSlider(id, target)
        }
        capture("OnboardingVoice-sliders-adjusted-light")

        // Values persist across chooser navigation.
        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        waitForTag("onboardingVoiceChooser")
        compose.onNodeWithTag("voice.select.sol").assertIsSelected()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Preview Rowan", substring = true))
        systemBack()
        waitForTag("onboardingVoice")
        values.forEach { (id, target) -> assertVoiceSlider(id, target) }
    }

    @Test fun continueCallback() {
        openOnboardingVoice()
        compose.onNodeWithTag("onboardingVoice.continue").performClick()
        // Voice is the last onboarding step, so Continue completes the flow.
        compose.onNodeWithTag("onboarding.complete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "continue"))
        compose.onNodeWithTag("onboardingVoice.skip").assertDoesNotExist()
        capture("OnboardingVoice-continue-light")
    }

    @Test fun skipCallback() {
        openOnboardingVoice()
        compose.onNodeWithTag("onboardingVoice.skip").performClick()
        compose.onNodeWithTag("onboarding.complete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "skip"))
        compose.onNodeWithTag("onboardingVoice.continue").assertDoesNotExist()
        capture("OnboardingVoice-skip-light")
    }

    @Test fun outerBackExitsToHost() {
        openOnboardingVoice()
        compose.onNodeWithTag("back").performClick()
        waitForTag("openOnboardingVoice")
        compose.onNodeWithText("Choose how Rem sounds").assertDoesNotExist()
        capture("OnboardingVoice-back-light")
    }

    @Test fun darkAppearance() {
        appearance(dark = true)
        openOnboardingVoice()
        capture("OnboardingVoice-dark")
        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        waitForTag("onboardingVoiceChooser")
        capture("OnboardingVoice-chooser-dark")
    }

    @Test fun largeTextReachability() {
        appearance(largeText = true)
        openOnboardingVoice()
        capture("OnboardingVoice-large-text")
        compose.onNodeWithText(
            "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice.",
        ).performScrollTo().assertIsDisplayed()
        // The pinned actions stay reachable even while the content scrolls at large Dynamic Type.
        compose.onNodeWithTag("onboardingVoice.continue").assertIsDisplayed()
        compose.onNodeWithTag("onboardingVoice.skip").assertIsDisplayed()
        capture("OnboardingVoice-large-text-bottom")
    }
}
