package com.rem.designsystem.demo

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class SettingsPlaygroundTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // AGP uninstalls the app after connected tests, removing app-scoped files.
        // Test-owned MediaStore output survives that cleanup on the dedicated emulator.
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
    private fun openSettings(fixture: String = "Success") {
        compose.onNodeWithText(fixture).performScrollTo().performClick()
        compose.onNodeWithTag("openSettings").performScrollTo().performClick()
        compose.onNodeWithTag("openAgent").assertExists()
    }
    private fun waitForText(text: String) {
        compose.waitUntil(5000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun navigationAndBack() {
        openSettings()
        capture("SettingsEntry-light")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Capabilities")
        compose.onNodeWithText("Scheduled and triggered work").assertExists()
        capture("AgentSettings-light")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("openAgent").assertExists()
    }
    private fun appearance(dark: Boolean = false, largeText: Boolean = false) {
        compose.activityRule.scenario.onActivity {
            it.intent.putExtra("settingsDark", dark)
            it.intent.putExtra("settingsLargeText", largeText)
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }
    @Test fun darkSettingsFoundation() {
        appearance(dark = true)
        openSettings()
        capture("SettingsEntry-dark")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Capabilities")
        capture("AgentSettings-dark")
    }
    @Test fun largeTextSettingsScrolls() {
        appearance(largeText = true)
        openSettings()
        capture("SettingsEntry-large-text")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Capabilities")
        capture("AgentSettings-large-text")
        compose.onNodeWithText("Voice").performScrollTo().assertIsDisplayed()
        capture("AgentSettings-large-text-bottom")
    }
    @Test fun automationsHasNoNavigationAction() {
        openSettings()
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Capabilities")
        compose.onNodeWithTag("automationsUnavailable").assertHasNoClickAction()
    }
    @Test fun errorRetryAndCancel() {
        openSettings("Error")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Retry")
        capture("android-load-error")
        compose.onNodeWithTag("cancelLoad").performClick()
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Retry")
        compose.onNodeWithTag("retry").performClick()
        waitForText("Capabilities")
    }
    @Test fun cancelLoadingDoesNotNavigateLater() {
        openSettings("Slow")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Loading agent settings…")
        compose.onNodeWithText("Loading agent settings…").assertIsDisplayed()
        capture("android-loading")
        compose.onNodeWithTag("cancelLoad").performClick()
        Thread.sleep(10300) // Pass the cancelled fixture deadline; it must not navigate later.
        compose.onNodeWithTag("openAgent").assertExists()
        compose.onNodeWithText("Capabilities").assertDoesNotExist()
    }
    @Test fun controlCancelRollbackAndSave() {
        compose.onNodeWithText("Shared controls").performClick()
        compose.onNodeWithTag("editName").performClick()
        compose.onNodeWithTag("nameField").performTextReplacement("Discard me")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Display name: Avery Diaz").assertExists()
        compose.onNodeWithTag("editName").performClick()
        compose.onNodeWithTag("nameField").assertTextContains("Avery Diaz")
        compose.onNodeWithTag("nameField").performTextReplacement("Avery Diaz Saved")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Display name: Avery Diaz Saved").assertExists()
        compose.onNodeWithContentDescription("Notifications").assertExists()
        compose.onNodeWithTag("notifications").performClick().assertIsOff()
        capture("android-controls")
    }
}
