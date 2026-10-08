package com.rem.designsystem.demo

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
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
    private fun waitForTag(tag: String) {
        compose.waitUntil(5000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openDestination(destination: String, rootTag: String) {
        openSettings()
        compose.onNodeWithTag("openAgent").performClick()
        waitForTag("agentSettings")
        compose.onNodeWithTag("agentDestination.$destination").performScrollTo().performClick()
        waitForTag(rootTag)
    }

    @Test fun pairedDevicesBoundaryCancelRemovalRefreshAndRecreation() {
        openDestination("PairedDevices", "pairedDevices")
        compose.onNodeWithTag("pairedDevices.peer.mac-studio").assertExists()
        capture("PairedDevices-light")
        compose.onNodeWithTag("pairedDevices.add").performClick()
        compose.onNodeWithTag("pairedDevices.addBoundary").assertExists()
        compose.onNodeWithText("Pair a new device").assertExists()
        capture("PairedDevices-add-boundary-light")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag("pairedDevices.peer.mac-studio").assertExists().performClick()
        compose.onNodeWithTag("pairedDevices.removeAccess").assertExists()
        capture("PairedDevices-detail-light")
        compose.onNodeWithTag("pairedDevices.back").performClick()
        compose.onNodeWithTag("pairedDevices.peer.mac-studio").assertExists().performClick()
        compose.onNodeWithTag("pairedDevices.removeAccess").performScrollTo().performClick()
        compose.onNodeWithTag("pairedDevices.removeConfirmation").assertExists()
        capture("PairedDevices-remove-confirmation-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("pairedDevices.removeConfirmation").assertDoesNotExist()
        compose.onNodeWithTag("pairedDevices.removeAccess").assertExists().performClick()
        compose.onNodeWithTag("pairedDevices.confirmRemove").performClick()
        compose.onNodeWithText("No paired devices").assertExists()
        compose.onNodeWithTag("pairedDevices.peer.mac-studio").assertDoesNotExist()
        compose.onNodeWithTag("pairedDevices.add").assertDoesNotExist()
        compose.onNodeWithTag("pairedDevices.refresh").performClick()
        compose.onNodeWithText("No paired devices").assertExists()
        capture("PairedDevices-empty-light")
        compose.activityRule.scenario.recreate()
        waitForTag("pairedDevices.refresh")
        compose.onNodeWithText("No paired devices").assertExists()
        compose.onNodeWithTag("pairedDevices.peer.mac-studio").assertDoesNotExist()
        compose.onNodeWithTag("pairedDevices.refresh").performClick()
        compose.onNodeWithText("No paired devices").assertExists()
        capture("PairedDevices-empty-recreated-light")
        compose.onNodeWithTag("pairedDevices.back").performClick()
        waitForTag("agentSettings")
    }

    @Test fun memoryControlsSurviveSummaryAndComposerGivesLocalFeedback() {
        openDestination("Memory", "settingsMemory")
        compose.onNodeWithTag("memory.toggle.SearchAndReference").assertIsOn()
        compose.onNodeWithTag("memory.toggle.GenerateMemory").assertIsOn()
        compose.onNodeWithTag("memory.toggle.SensitiveTopics").assertIsOff()
        capture("Memory-light")
        compose.onNodeWithTag("memory.toggle.SearchAndReference").performClick().assertIsOff()
        compose.onNodeWithTag("memory.toggle.GenerateMemory").performClick().assertIsOff()
        compose.onNodeWithTag("memory.toggle.SensitiveTopics").performClick().assertIsOn()
        compose.onNodeWithTag("memory.summaryRow").performScrollTo().performClick()
        waitForTag("memorySummary")
        capture("Memory-summary-light")
        compose.onNodeWithTag("memory.composerSend").assertIsNotEnabled()
        compose.onNodeWithTag("memory.composerField").performTextInput("Use short answers in this prototype.")
        compose.onNodeWithTag("memory.composerSend").assertIsEnabled().performClick()
        compose.onNodeWithTag("memory.composerFeedback")
            .assertTextEquals("Noted in this prototype session. Rem doesn’t reply or change memory here.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("memory.composerField").assertTextEquals("")
        compose.onNodeWithTag("memory.composerSend").assertIsNotEnabled()
        capture("Memory-composer-feedback-light")
        compose.onNodeWithTag("back").performClick()
        waitForTag("settingsMemory")
        compose.onNodeWithTag("memory.toggle.SearchAndReference").assertIsOff()
        compose.onNodeWithTag("memory.toggle.GenerateMemory").assertIsOff()
        compose.onNodeWithTag("memory.toggle.SensitiveTopics").assertIsOn()
        compose.onNodeWithTag("back").performClick()
        waitForTag("agentSettings")
    }

    @Test fun modelsPickerMaskedDraftBackDiscardAndSingleSave() {
        openDestination("Models", "settingsModels")
        capture("Models-light")
        compose.onNodeWithTag("models.addProviderKey").performScrollTo().performClick()
        waitForTag("modelsAddKey")
        compose.onAllNodesWithTag("models.saveKey").assertCountEquals(1)
        compose.onAllNodesWithText("Save").assertCountEquals(1)
        compose.onNodeWithTag("models.saveKey").assertIsNotEnabled()
        capture("Models-add-key-empty-light")
        compose.onNodeWithContentDescription("Choose provider").performClick()
        val providers = listOf("Anthropic", "OpenAI", "Google", "Mistral", "OpenRouter")
        providers.forEach { compose.onNodeWithTag("models.providerOption.$it").assertExists() }
        val optionMatcher = providers.map { hasTestTag("models.providerOption.$it") }.reduce { left, right -> left or right }
        compose.onAllNodes(optionMatcher).assertCountEquals(5)
        capture("Models-provider-picker-light")
        compose.onNodeWithTag("models.providerOption.OpenAI").performClick()
        compose.onNodeWithContentDescription("Provider OpenAI").assertExists()
        compose.onNodeWithTag("models.keyField")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .performTextInput("prototype-only-placeholder")
        compose.onNodeWithTag("models.saveKey").assertIsEnabled()
        capture("Models-key-masked-light")
        compose.onNodeWithTag("back").performClick()
        waitForTag("settingsModels")
        compose.onNodeWithTag("models.addProviderKey").performScrollTo().performClick()
        compose.onNodeWithTag("models.keyField").assertTextEquals("")
        compose.onNodeWithTag("models.saveKey").assertIsNotEnabled()
        compose.onNodeWithTag("models.keyField").performTextInput("another-prototype-placeholder")
        compose.onAllNodesWithTag("models.saveKey").assertCountEquals(1)
        compose.onAllNodesWithText("Save").assertCountEquals(1)
        compose.onNodeWithTag("models.saveKey").performClick()
        waitForTag("settingsModels")
        compose.onNodeWithTag("models.addProviderKey").performScrollTo().performClick()
        compose.onNodeWithTag("models.keyField").assertTextEquals("")
        compose.onNodeWithTag("models.saveKey").assertIsNotEnabled()
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("back").performClick()
        waitForTag("agentSettings")
    }

    private fun captureDestinationAppearance(dark: Boolean = false, largeText: Boolean = false) {
        appearance(dark = dark, largeText = largeText)
        val suffix = if (largeText) "large-text" else "dark"
        val destinations = listOf(
            listOf("PairedDevices", "pairedDevices", "pairedDevices.peer.mac-studio"),
            listOf("Memory", "settingsMemory", "memory.summaryRow"),
            listOf("Models", "settingsModels", "models.addProviderKey"),
        )
        destinations.forEach { (route, rootTag, finalControl) ->
            openDestination(route, rootTag)
            capture("$route-$suffix")
            compose.onNodeWithTag(finalControl).performScrollTo().assertIsDisplayed()
            if (largeText) capture("$route-large-text-bottom")
            compose.onNodeWithTag(if (route == "PairedDevices") "pairedDevices.back" else "back").performClick()
            waitForTag("agentSettings")
            compose.onNodeWithTag("back").performClick() // Agent settings -> Settings.
            compose.onNodeWithTag("back").performClick() // Settings -> gallery.
            waitForTag("openSettings")
        }
    }
    @Test fun destinationsDarkAppearance() = captureDestinationAppearance(dark = true)
    @Test fun destinationsLargeTextReachability() = captureDestinationAppearance(largeText = true)

}
