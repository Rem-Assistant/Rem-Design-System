package com.rem.designsystem.demo

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class SettingsPlaygroundTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val file = File(compose.activity.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun openSettings(fixture: String = "Success") {
        compose.onNodeWithText(fixture).performClick()
        compose.onNodeWithTag("openSettings").performClick()
        compose.onNodeWithTag("openAgent").assertExists()
    }
    private fun waitForText(text: String) {
        compose.waitUntil(5000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun navigationAndBack() {
        openSettings()
        capture("android-settings-entry")
        compose.onNodeWithTag("openAgent").performClick()
        waitForText("Capabilities")
        compose.onNodeWithText("Scheduled and triggered work").assertExists()
        capture("android-agent-settings")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("openAgent").assertExists()
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
