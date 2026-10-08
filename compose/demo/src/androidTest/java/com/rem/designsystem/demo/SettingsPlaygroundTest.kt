package com.rem.designsystem.demo

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.compose.ui.text.AnnotatedString
import org.junit.Rule
import org.junit.Test

class SettingsPlaygroundTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Compose semantics can expose the new route before the device capture shows it.
        // Wait for the platform accessibility stream to settle, including dialogs/IME.
        automation.waitForIdle(500, 5000)
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
    @Test fun connectorsGmailMenusAndScopedPermissionsSurviveRecreation() {
        openDestination("Connectors", "settingsConnectors")
        capture("Connectors-light")
        listOf("Gmail", "GoogleCalendar", "Notion", "Slack", "GoogleDrive", "Linear", "Todoist").forEach {
            compose.onNodeWithTag("connectors.provider.$it").performScrollTo().assertIsDisplayed()
        }
        capture("Connectors-brand-rows-bottom-light")
        compose.onNodeWithTag("connectors.provider.Notion").performScrollTo().performClick()
        compose.onNodeWithTag("connectors.boundary").assertExists()
        compose.onNodeWithText("Notion account details are not included in this prototype. No connection is changed.").assertExists()
        capture("Connectors-unsupported-provider-light")
        systemBack() // Dismisses only the native dialog.
        waitForTag("settingsConnectors")
        compose.onNodeWithTag("connectors.boundary").assertDoesNotExist()
        compose.onNodeWithTag("connectors.provider.Gmail").performScrollTo().performClick()
        waitForTag("gmail.detail")
        capture("Gmail-default-light")
        compose.onNodeWithTag("gmail.accountMenu.avery").performScrollTo().performClick()
        compose.onNodeWithTag("gmail.accountSettings.avery").assertExists()
        capture("Gmail-account-menu-light")
        compose.onNodeWithTag("gmail.accountSettings.avery").performClick()
        waitForTag("gmail.permissions.avery")
        compose.onNodeWithTag("gmail.permission.avery.LowRisk").assertIsSelected()
        capture("Gmail-account-permissions-light")
        systemBack()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.connectorMenu").performClick()
        compose.onNodeWithTag("gmail.disconnectAll").assertExists()
        capture("Gmail-connector-menu-light")
        systemBack() // Dismisses the popup without leaving Gmail.
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.disconnectAll").assertDoesNotExist()
        compose.onNodeWithTag("gmail.permissions").performScrollTo().performClick()
        waitForTag("gmail.permissions.connector")
        compose.onNodeWithTag("gmail.permission.connector.LowRisk").assertIsSelected()
        capture("Gmail-connector-permissions-light")
        val policies = listOf("AlwaysAsk", "LowRisk", "AlwaysAllow", "NeverAllow")
        policies.forEach { policy ->
            compose.onNodeWithTag("gmail.permission.connector.$policy").performScrollTo().performClick().assertIsSelected()
            policies.filter { it != policy }.forEach {
                compose.onNodeWithTag("gmail.permission.connector.$it").assertIsNotSelected()
            }
            capture("Gmail-connector-policy-$policy-light")
        }
        systemBack()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.accountMenu.avery").performScrollTo().performClick()
        compose.onNodeWithTag("gmail.accountSettings.avery").assertExists()
        compose.onNodeWithTag("gmail.disconnectAccount.avery").assertExists()
        capture("Gmail-account-menu-after-policy-light")
        compose.onNodeWithTag("gmail.accountSettings.avery").performClick()
        waitForTag("gmail.permissions.avery")
        compose.onNodeWithTag("gmail.permission.avery.NeverAllow").assertIsSelected()
        capture("Gmail-account-permissions-inherited-light")
        policies.forEach { policy ->
            compose.onNodeWithTag("gmail.permission.avery.$policy").performScrollTo().performClick().assertIsSelected()
            policies.filter { it != policy }.forEach {
                compose.onNodeWithTag("gmail.permission.avery.$it").assertIsNotSelected()
            }
        }
        compose.onNodeWithTag("gmail.permission.avery.AlwaysAsk").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        waitForTag("gmail.permissions.avery")
        compose.onNodeWithTag("gmail.permission.avery.AlwaysAsk").assertIsSelected()
        capture("Gmail-account-permissions-recreated-light")
        systemBack()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.permissions").performScrollTo().performClick()
        compose.onNodeWithTag("gmail.permission.connector.NeverAllow").assertIsSelected()
        compose.onNodeWithTag("gmail.permission.connector.AlwaysAllow").performScrollTo().performClick()
        systemBack()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.accountMenu.avery").performScrollTo().performClick()
        compose.onNodeWithTag("gmail.accountSettings.avery").performClick()
        compose.onNodeWithTag("gmail.permission.avery.AlwaysAsk").assertIsSelected()
        capture("Gmail-account-override-retained-light")
        systemBack()
        waitForTag("gmail.detail")
        systemBack()
        waitForTag("settingsConnectors")
        systemBack()
        waitForTag("agentSettings")
    }

    @Test fun gmailSingleDisconnectCancelConfirmAndRecreation() {
        openDestination("Connectors", "settingsConnectors")
        compose.onNodeWithTag("connectors.provider.Gmail").performScrollTo().performClick()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.accountMenu.avery").performScrollTo().performClick()
        compose.onNodeWithTag("gmail.disconnectAccount.avery").performClick()
        compose.onNodeWithText("Disconnect avery@example.com?").assertExists()
        capture("Gmail-disconnect-account-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("gmail.account.avery").assertExists()
        compose.onNodeWithTag("gmail.accountMenu.avery").performClick()
        compose.onNodeWithTag("gmail.disconnectAccount.avery").performClick()
        systemBack() // Native dialog Back must cancel without popping the Gmail route.
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.disconnectConfirmation").assertDoesNotExist()
        compose.onNodeWithTag("gmail.account.avery").assertExists()
        compose.onNodeWithTag("gmail.accountMenu.avery").performClick()
        compose.onNodeWithTag("gmail.disconnectAccount.avery").performClick()
        compose.onNodeWithTag("gmail.confirmDisconnect").performClick()
        waitForTag("settingsConnectors")
        compose.onNodeWithContentDescription("Connect Gmail").assertExists()
        listOf("GoogleCalendar", "Notion", "Slack").forEach {
            compose.onNodeWithTag("connectors.provider.$it").assertHasClickAction()
        }
        capture("Gmail-disconnect-one-result-light")
        compose.activityRule.scenario.recreate()
        waitForTag("settingsConnectors")
        compose.onNodeWithContentDescription("Connect Gmail").assertExists()
        capture("Gmail-disconnect-one-recreated-light")
    }

    @Test fun gmailAllDisconnectCancelThenConfirm() {
        openDestination("Connectors", "settingsConnectors")
        compose.onNodeWithTag("connectors.provider.Gmail").performScrollTo().performClick()
        waitForTag("gmail.detail")
        compose.onNodeWithTag("gmail.connectorMenu").performClick()
        compose.onNodeWithTag("gmail.disconnectAll").performClick()
        compose.onNodeWithText("Disconnect all Gmail accounts?").assertExists()
        capture("Gmail-disconnect-accounts-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("gmail.account.avery").assertExists()
        compose.onNodeWithTag("gmail.connectorMenu").performClick()
        compose.onNodeWithTag("gmail.disconnectAll").performClick()
        compose.onNodeWithTag("gmail.confirmDisconnect").performClick()
        waitForTag("settingsConnectors")
        compose.onNodeWithContentDescription("Connect Gmail").assertExists()
        listOf("GoogleCalendar", "Notion", "Slack").forEach {
            compose.onNodeWithTag("connectors.provider.$it").assertHasClickAction()
        }
        capture("Gmail-disconnect-all-result-light")
        compose.activityRule.scenario.recreate()
        waitForTag("settingsConnectors")
        compose.onNodeWithContentDescription("Connect Gmail").assertExists()
    }

    @Test fun connectorsGmailDarkAndLargeTextReachability() {
        listOf(false, true).forEach { large ->
            appearance(dark = !large, largeText = large)
            val suffix = if (large) "large-text" else "dark"
            openDestination("Connectors", "settingsConnectors")
            capture("Connectors-$suffix")
            compose.onNodeWithTag("connectors.provider.Todoist").performScrollTo().assertIsDisplayed()
            capture("Connectors-bottom-$suffix")
            compose.onNodeWithTag("connectors.provider.Gmail").performScrollTo().performClick()
            waitForTag("gmail.detail")
            capture("Gmail-default-$suffix")
            compose.onNodeWithTag("gmail.info.Report an issue").performScrollTo().assertIsDisplayed()
            capture("Gmail-information-$suffix")
            compose.onNodeWithTag("gmail.permissions").performScrollTo().performClick()
            waitForTag("gmail.permissions.connector")
            capture("Gmail-connector-permissions-$suffix")
            compose.onNodeWithTag("gmail.permission.connector.NeverAllow").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Controls the level of access Rem has across all Gmail accounts connected to this connector.")
                .performScrollTo().assertIsDisplayed()
            capture("Gmail-connector-permissions-bottom-$suffix")
            systemBack()
            waitForTag("gmail.detail")
            compose.onNodeWithTag("gmail.accountMenu.avery").performScrollTo().performClick()
            compose.onNodeWithTag("gmail.accountSettings.avery").performClick()
            compose.onNodeWithText("Controls what Rem can do with avery@example.com. Overrides the connector-wide setting for this account.")
                .performScrollTo().assertIsDisplayed()
            capture("Gmail-account-permissions-bottom-$suffix")
            systemBack() // Account permissions -> Gmail.
            systemBack() // Gmail -> Connectors.
            systemBack() // Connectors -> Agent settings.
            waitForTag("agentSettings")
            compose.onNodeWithTag("back").performClick() // Agent settings -> Settings.
            compose.onNodeWithTag("back").performClick() // Settings -> gallery.
            waitForTag("openSettings")
        }
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
        assertEmptyInput("memory.composerField")
        compose.onNodeWithTag("memory.composerSend").assertIsNotEnabled()
        capture("Memory-composer-feedback-light")
        systemBack()
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
        systemBack()
        waitForTag("settingsModels")
        compose.onNodeWithTag("models.addProviderKey").performScrollTo().performClick()
        assertEmptyInput("models.keyField")
        compose.onNodeWithTag("models.saveKey").assertIsNotEnabled()
        compose.onNodeWithTag("models.keyField").performTextInput("another-prototype-placeholder")
        compose.onAllNodesWithTag("models.saveKey").assertCountEquals(1)
        compose.onAllNodesWithText("Save").assertCountEquals(1)
        compose.onNodeWithTag("models.saveKey").performClick()
        waitForTag("settingsModels")
        compose.onNodeWithTag("models.addProviderKey").performScrollTo().performClick()
        assertEmptyInput("models.keyField")
        compose.onNodeWithTag("models.saveKey").assertIsNotEnabled()
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("back").performClick()
        waitForTag("agentSettings")
    }

    private fun systemBack() {
        // The first platform Back would otherwise only hide the keyboard.
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        compose.waitForIdle()
    }
    private fun assertEmptyInput(tag: String) {
        compose.onNodeWithTag(tag).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
    }
    private fun openCloudSites() {
        openDestination("CloudBrowser", "cloudBrowser.root")
        capture("CloudBrowser-light")
        compose.onNodeWithTag("cloudBrowser.seeAllSites").performScrollTo().performClick()
        waitForTag("cloudBrowser.sitesList")
    }

    @Test fun cloudAddSiteEmptyFocusAndSystemBackDiscards() {
        openCloudSites()
        capture("CloudBrowser-sites-light")
        compose.onNodeWithTag("cloudBrowser.sites.addSite").performClick()
        waitForTag("cloudBrowser.addSiteForm")
        compose.onNodeWithTag("cloudBrowser.addSite.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.addSite.domainField").assertIsNotFocused()
        capture("CloudBrowser-add-site-empty-light")
        compose.onNodeWithTag("cloudBrowser.addSite.domainField").performClick().assertIsFocused()
        capture("CloudBrowser-add-site-focused-light")
        compose.onNodeWithTag("cloudBrowser.addSite.domainField").performTextInput("discarded.example")
        compose.onNodeWithTag("cloudBrowser.addSite.save").assertIsEnabled()
        systemBack()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.discarded.example").assertDoesNotExist()
        listOf("github.com", "notion.so", "linear.app", "openai.com").forEach {
            compose.onNodeWithTag("cloudBrowser.site.$it").assertExists()
        }
        compose.onNodeWithTag("cloudBrowser.sites.addSite").performClick()
        assertEmptyInput("cloudBrowser.addSite.domainField")
        compose.onNodeWithTag("cloudBrowser.addSite.save").assertIsNotEnabled()
        systemBack()
        waitForTag("cloudBrowser.sitesList")
        systemBack()
        waitForTag("cloudBrowser.root")
        systemBack()
        waitForTag("agentSettings")
    }

    @Test fun cloudLoginEditsCookieClearAndRecreationPreserveLocalMutation() {
        openCloudSites()
        compose.onNodeWithTag("cloudBrowser.site.github.com").performClick()
        waitForTag("cloudBrowser.siteDetail")
        capture("CloudBrowser-site-detail-light")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performScrollTo().performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithText("samuel@example.com").assertExists()
        capture("CloudBrowser-saved-login-light")
        compose.onNodeWithTag("cloudBrowser.savedLogin.editUsername.pencil").performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin.usernameField").performTextReplacement("discarded@example.com")
        capture("CloudBrowser-login-edit-username-light")
        compose.onNodeWithTag("cloudBrowser.savedLogin.cancel").performClick()
        compose.onNodeWithText("samuel@example.com").assertExists()
        compose.onNodeWithTag("cloudBrowser.savedLogin.editUsername.pencil").performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin.usernameField").performTextReplacement("prototype@example.com")
        compose.onNodeWithTag("cloudBrowser.savedLogin.save").performClick()
        compose.onNodeWithText("prototype@example.com").assertExists()
        compose.onNodeWithTag("cloudBrowser.savedLogin.save").assertDoesNotExist()
        compose.onNodeWithTag("cloudBrowser.savedLogin.editPassword.pencil").performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin.passwordField")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .performTextInput("discarded-password-placeholder")
        systemBack() // Cancels editing before it may pop Saved login.
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithTag("cloudBrowser.savedLogin.save").assertDoesNotExist()
        compose.onNodeWithTag("cloudBrowser.savedLogin.editPassword.pencil").performClick()
        assertEmptyInput("cloudBrowser.savedLogin.passwordField")
        compose.onNodeWithTag("cloudBrowser.savedLogin.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.savedLogin.passwordField").performTextInput("prototype-password-placeholder")
        capture("CloudBrowser-login-edit-password-masked-light")
        compose.onNodeWithTag("cloudBrowser.savedLogin.save").performClick()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").performScrollTo().performClick()
        waitForTag("cloudBrowser.cookies")
        compose.onNodeWithText("12 illustrative cookies").assertExists()
        capture("CloudBrowser-cookies-light")
        compose.onNodeWithTag("cloudBrowser.cookies.clearSiteData").performScrollTo().performClick()
        compose.onNodeWithTag("cloudBrowser.cookies.confirmClear").assertExists()
        capture("CloudBrowser-cookies-confirmation-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("12 illustrative cookies").assertExists()
        compose.onNodeWithText("Signed in").assertExists()
        compose.onNodeWithTag("cloudBrowser.cookies.clearSiteData").performClick()
        compose.onNodeWithTag("cloudBrowser.cookies.confirmClear").performClick()
        compose.onNodeWithText("0 illustrative cookies").assertExists()
        compose.onNodeWithText("Signed out").assertExists()
        capture("CloudBrowser-cookies-cleared-light")
        compose.activityRule.scenario.recreate()
        waitForTag("cloudBrowser.cookies")
        compose.onNodeWithText("0 illustrative cookies").assertExists()
        compose.onNodeWithText("Signed out").assertExists()
        capture("CloudBrowser-cookies-cleared-recreated-light")
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performScrollTo().performClick()
        compose.onNodeWithText("prototype@example.com").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        capture("CloudBrowser-login-retained-light")
    }

    @Test fun cloudRootAddSiteSaveAndPermissionScopes() {
        openDestination("CloudBrowser", "cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.defaultPermission").assertTextContains("Ask").performClick()
        capture("CloudBrowser-default-permission-menu-light")
        compose.onNodeWithText("Allow").performClick()
        compose.onNodeWithTag("cloudBrowser.defaultPermission").assertTextContains("Allow").performClick()
        compose.onNodeWithText("Ask").performClick()
        compose.onNodeWithTag("cloudBrowser.defaultPermission").assertTextContains("Ask")
        // The root header action is distinct from the Sites toolbar Add entry.
        compose.onNodeWithTag("cloudBrowser.addSite").performScrollTo().performClick()
        waitForTag("cloudBrowser.addSiteForm")
        compose.onNodeWithTag("cloudBrowser.addSite.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.addSite.domainField")
            .performClick().performTextInput("HTTPS://ROOT-FIXTURE.EXAMPLE/PATH")
        compose.onNodeWithTag("cloudBrowser.permissionMenu").performScrollTo().performClick()
        compose.onNodeWithText("Allow").performClick()
        compose.onNodeWithTag("cloudBrowser.addSite.username")
            .performScrollTo().performTextInput("ROOT-FIXTURE@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.addSite.password")
            .performScrollTo().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .performTextInput("ROOT-FIXTURE-ONLY")
        capture("CloudBrowser-add-site-filled-light")
        compose.onNodeWithTag("cloudBrowser.addSite.save").assertIsEnabled().performClick()
        waitForTag("cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.defaultPermission").assertTextContains("Ask")
        compose.onNodeWithTag("cloudBrowser.seeAllSites").performScrollTo().performClick()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.root-fixture.example")
            .performScrollTo().assertTextContains("Allow · 1 saved login").performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.permission").assertTextContains("Allow").performClick()
        compose.onNodeWithText("Ask").performClick()
        compose.onNodeWithTag("cloudBrowser.siteDetail.permission").assertTextContains("Ask")
        capture("CloudBrowser-added-site-permission-light")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performScrollTo().performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithText("ROOT-FIXTURE@EXAMPLE.COM").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        systemBack()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.root-fixture.example").assertTextContains("Ask · 1 saved login")
        compose.onNodeWithTag("cloudBrowser.site.notion.so").assertTextContains("Allow · Signed in")
        systemBack()
        waitForTag("cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.defaultPermission").assertTextContains("Ask")
    }

    @Test fun cloudAddLoginEmptyFocusFilledSaveAndSystemBackDiscard() {
        openCloudSites()
        compose.onNodeWithTag("cloudBrowser.site.linear.app").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.addLogin").performScrollTo().performClick()
        waitForTag("cloudBrowser.addLoginForm")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.addLogin.username").assertIsNotFocused()
        compose.onNodeWithTag("cloudBrowser.addLogin.password").assertIsNotFocused()
        capture("CloudBrowser-add-login-empty-light")
        compose.onNodeWithTag("cloudBrowser.addLogin.username").performClick().assertIsFocused()
            .performTextInput("DISCARDED-LOGIN@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsNotEnabled()
        capture("CloudBrowser-add-login-focused-light")
        compose.onNodeWithTag("cloudBrowser.addLogin.password")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .performTextInput("DISCARDED-LOGIN-ONLY")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsEnabled()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").assertDoesNotExist()
        compose.onNodeWithTag("cloudBrowser.siteDetail.addLogin").performClick()
        waitForTag("cloudBrowser.addLoginForm")
        assertEmptyInput("cloudBrowser.addLogin.username")
        assertEmptyInput("cloudBrowser.addLogin.password")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.addLogin.username").performTextInput("SAVED-LOGIN@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsNotEnabled()
        compose.onNodeWithTag("cloudBrowser.addLogin.password").performTextInput("SAVED-LOGIN-ONLY")
        compose.onNodeWithTag("cloudBrowser.addLogin.password").performImeAction()
        compose.onNodeWithTag("cloudBrowser.addLogin.password").assertIsNotFocused()
        compose.onNodeWithTag("cloudBrowser.addLogin.username").assertIsNotFocused()
        compose.onNodeWithText("Username or email").assertExists()
        compose.onNodeWithText("Password").assertExists()
        capture("CloudBrowser-add-login-filled-light")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsEnabled().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(1)
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithText("SAVED-LOGIN@EXAMPLE.COM").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.addLogin").performClick()
        waitForTag("cloudBrowser.addLoginForm")
        assertEmptyInput("cloudBrowser.addLogin.username")
        assertEmptyInput("cloudBrowser.addLogin.password")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").assertIsNotEnabled()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(1)
    }

    @Test fun cloudRemoveLoginCancelAndConfirmPreservesOtherCredential() {
        openCloudSites()
        compose.onNodeWithTag("cloudBrowser.site.github.com").performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.addLogin").performScrollTo().performClick()
        waitForTag("cloudBrowser.addLoginForm")
        compose.onNodeWithTag("cloudBrowser.addLogin.username").performTextInput("RETAINED-LOGIN@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.addLogin.password").performTextInput("RETAINED-LOGIN-ONLY")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(2)
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login")[0]
            .performScrollTo().assertTextContains("samuel@example.com").performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithTag("cloudBrowser.savedLogin.remove").performScrollTo().performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin.confirmRemove").assertExists()
        capture("CloudBrowser-remove-login-confirmation-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin").assertExists()
        compose.onNodeWithText("samuel@example.com").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        compose.onNodeWithTag("cloudBrowser.savedLogin.remove").performClick()
        compose.onNodeWithTag("cloudBrowser.savedLogin.confirmRemove").performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(1)
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").assertTextContains("RETAINED-LOGIN@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("12 cookies · Signed in")
        capture("CloudBrowser-login-removed-scoped-light")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithText("RETAINED-LOGIN@EXAMPLE.COM").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        systemBack()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.notion.so").performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("8 cookies · Signed in")
    }

    @Test fun cloudDetailAndAllClearCancelConfirmKeepCredentials() {
        openDestination("CloudBrowser", "cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.site.github.com").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.clearSiteData").performScrollTo().performClick()
        compose.onNodeWithTag("cloudBrowser.siteDetail.confirmClear").assertExists()
        capture("CloudBrowser-detail-clear-confirmation-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("12 cookies · Signed in")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(1)
        compose.onNodeWithTag("cloudBrowser.siteDetail.clearSiteData").performClick()
        compose.onNodeWithTag("cloudBrowser.siteDetail.confirmClear").performClick()
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("0 cookies · Signed out")
        compose.onNodeWithTag("cloudBrowser.siteDetail.login").performScrollTo().performClick()
        waitForTag("cloudBrowser.savedLogin")
        compose.onNodeWithText("samuel@example.com").assertExists()
        compose.onNodeWithText("•".repeat(12)).assertExists()
        systemBack()
        waitForTag("cloudBrowser.siteDetail")
        systemBack()
        waitForTag("cloudBrowser.root")
        // Preserve credentials on two sites, including the site still signed in before clear-all.
        compose.onNodeWithTag("cloudBrowser.site.notion.so").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("8 cookies · Signed in")
        compose.onNodeWithTag("cloudBrowser.siteDetail.addLogin").performClick()
        waitForTag("cloudBrowser.addLoginForm")
        compose.onNodeWithTag("cloudBrowser.addLogin.username").performTextInput("NOTION-FIXTURE@EXAMPLE.COM")
        compose.onNodeWithTag("cloudBrowser.addLogin.password").performTextInput("NOTION-FIXTURE-ONLY")
        compose.onNodeWithTag("cloudBrowser.addLogin.save").performClick()
        waitForTag("cloudBrowser.siteDetail")
        systemBack()
        waitForTag("cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.clearAllData").performScrollTo().performClick()
        compose.onNodeWithTag("cloudBrowser.confirmClearAll").assertExists()
        capture("CloudBrowser-clear-all-confirmation-light")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("cloudBrowser.site.notion.so").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("8 cookies · Signed in")
        compose.onAllNodesWithTag("cloudBrowser.siteDetail.login").assertCountEquals(1)
        systemBack()
        waitForTag("cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.seeAllSites").performScrollTo().performClick()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.openai.com").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("5 cookies · Signed in")
        systemBack()
        waitForTag("cloudBrowser.sitesList")
        systemBack()
        waitForTag("cloudBrowser.root")
        compose.onNodeWithTag("cloudBrowser.clearAllData").performScrollTo().performClick()
        compose.onNodeWithTag("cloudBrowser.confirmClearAll").performClick()
        listOf("github.com" to "samuel@example.com", "notion.so" to "NOTION-FIXTURE@EXAMPLE.COM")
            .forEach { (domain, username) ->
                compose.onNodeWithTag("cloudBrowser.site.$domain").performScrollTo().performClick()
                waitForTag("cloudBrowser.siteDetail")
                compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("0 cookies · Signed out")
                compose.onNodeWithTag("cloudBrowser.siteDetail.login").performClick()
                waitForTag("cloudBrowser.savedLogin")
                compose.onNodeWithText(username).assertExists()
                compose.onNodeWithText("•".repeat(12)).assertExists()
                systemBack()
                waitForTag("cloudBrowser.siteDetail")
                systemBack()
                waitForTag("cloudBrowser.root")
            }
        compose.onNodeWithTag("cloudBrowser.seeAllSites").performScrollTo().performClick()
        waitForTag("cloudBrowser.sitesList")
        compose.onNodeWithTag("cloudBrowser.site.openai.com").performScrollTo().performClick()
        waitForTag("cloudBrowser.siteDetail")
        compose.onNodeWithTag("cloudBrowser.siteDetail.cookies").assertTextContains("0 cookies · Signed out")
        capture("CloudBrowser-clear-all-retains-credentials-light")
    }

    private fun captureDestinationAppearance(dark: Boolean = false, largeText: Boolean = false) {
        appearance(dark = dark, largeText = largeText)
        val suffix = if (largeText) "large-text" else "dark"
        val destinations = listOf(
            listOf("PairedDevices", "pairedDevices", "pairedDevices.peer.mac-studio"),
            listOf("Memory", "settingsMemory", "memory.summaryRow"),
            listOf("Models", "settingsModels", "models.addProviderKey"),
            listOf("CloudBrowser", "cloudBrowser.root", "cloudBrowser.seeAllSites"),
        )
        destinations.forEach { (route, rootTag, finalControl) ->
            openDestination(route, rootTag)
            capture("$route-$suffix")
            compose.onNodeWithTag(finalControl).performScrollTo().assertIsDisplayed()
            if (largeText) capture("$route-large-text-bottom")
            val backTag = when (route) {
                "PairedDevices" -> "pairedDevices.back"
                "CloudBrowser" -> "cloudBrowser.back"
                else -> "back"
            }
            compose.onNodeWithTag(backTag).performClick()
            waitForTag("agentSettings")
            compose.onNodeWithTag("back").performClick() // Agent settings -> Settings.
            compose.onNodeWithTag("back").performClick() // Settings -> gallery.
            waitForTag("openSettings")
        }
    }
    @Test fun destinationsDarkAppearance() = captureDestinationAppearance(dark = true)
    @Test fun destinationsLargeTextReachability() = captureDestinationAppearance(largeText = true)

    private fun waitForWalletModalDismissed() {
        compose.waitUntil(5000) { compose.onAllNodesWithTag("wallet.consent.connect").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("wallet.provider.link").assertIsDisplayed()
    }
    @Test fun walletBothProvidersCancelBoundaryCloseRecreationAndSwipeDismiss() {
        openDestination("Wallet", "settingsWallet")
        capture("Wallet-light")
        listOf("link" to "app.link.com", "shopPay" to "shop.app").forEach { (provider, domain) ->
            compose.onNodeWithTag("wallet.provider.$provider").performClick()
            waitForTag("wallet.consent.$provider")
            capture("Wallet-$provider-consent-light")
            compose.onNodeWithTag("wallet.consent.cancel").performScrollTo().performClick()
            waitForWalletModalDismissed()
            compose.onNodeWithTag("wallet.provider.$provider").performClick()
            waitForTag("wallet.consent.$provider")
            compose.activityRule.scenario.recreate()
            waitForTag("wallet.consent.$provider")
            compose.onNodeWithTag("wallet.consent.connect").performScrollTo().performClick()
            waitForTag("wallet.external.$provider")
            compose.onNodeWithText(domain).assertIsDisplayed()
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
            capture("Wallet-$provider-boundary-light")
            compose.onNodeWithTag("wallet.external.close").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("wallet.external.close").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("wallet.provider.$provider").performClick()
            waitForTag("wallet.consent.$provider")
            // Native nested scrolling hands the downward gesture to the modal sheet at its top.
            compose.onNodeWithTag("wallet.consent.$provider").performTouchInput { swipeDown() }
            waitForWalletModalDismissed()
            compose.onNodeWithTag("wallet.provider.$provider").performClick()
            waitForTag("wallet.consent.$provider")
            systemBack()
            waitForWalletModalDismissed()
        }
        compose.onNodeWithTag("wallet.back").performClick()
        waitForTag("agentSettings")
    }

    private fun assertVoiceSlider(id: String, expected: Float) {
        compose.onNodeWithTag("voice.slider.$id").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            androidx.compose.ui.semantics.ProgressBarRangeInfo(expected, 0f..1f),
        ))
    }
    @Test fun voiceSelectionPreviewMenuAndSlidersSurviveBackAndRecreation() {
        openDestination("Voice", "settingsVoice")
        capture("Voice-light")
        compose.onNodeWithTag("voice.conversationEntry").performClick()
        compose.onNodeWithTag("voice.entry.VoiceSession").assertExists()
        compose.onNodeWithTag("voice.entry.Chat").assertExists()
        capture("Voice-menu-light")
        compose.onNodeWithTag("voice.entry.Chat").performClick()
        compose.onNodeWithTag("settingsVoice").assertExists()
        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        waitForTag("voiceChooser")
        val voices = listOf("aria", "sol", "rowan", "juniper", "vale")
        voices.forEach { compose.onNodeWithTag("voice.select.$it").assertExists() }
        val matcher = voices.map { hasTestTag("voice.select.$it") }.reduce { left, right -> left or right }
        compose.onAllNodes(matcher).assertCountEquals(5)
        compose.onNodeWithTag("voice.select.aria").assertIsSelected()
        capture("Voice-chooser-light")
        compose.onNodeWithTag("voice.preview.aria").performClick()
        compose.onNodeWithTag("voice.preview.aria").assert(hasContentDescription("Pause Aria", substring = true))
        capture("Voice-chooser-preview-light")
        compose.onNodeWithTag("voice.preview.aria").performClick()
        compose.onNodeWithTag("voice.preview.rowan").performClick()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Pause Rowan", substring = true))
        compose.onNodeWithTag("voice.select.aria").assertIsSelected()
        compose.onNodeWithTag("voice.select.rowan").assertIsNotSelected()
        capture("Voice-independent-preview-light")
        compose.onNodeWithTag("voice.select.sol").performClick().assertIsSelected()
        compose.onNodeWithTag("voice.select.aria").assertIsNotSelected()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Pause Rowan", substring = true))
        compose.onNodeWithTag("voiceChooser").assertExists()
        capture("Voice-independent-selection-light")
        compose.onNodeWithTag("voice.preview.rowan").performClick()
        capture("Voice-chooser-selected-light")
        systemBack()
        waitForTag("settingsVoice")
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Preview Sol", substring = true)).performClick()
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Pause Sol", substring = true))
        capture("Voice-preview-selected-light")
        compose.onNodeWithTag("voice.previewSelected").performClick()
        val values = listOf("speed" to 0.75f, "consistency" to 0.50f, "likeness" to 0.75f)
        values.forEach { (id, target) ->
            compose.onNodeWithTag("voice.slider.$id").performScrollTo()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(target) }
            assertVoiceSlider(id, target)
        }
        capture("Voice-sliders-adjusted-light")
        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        compose.onNodeWithTag("voice.select.sol").assertIsSelected()
        compose.onNodeWithTag("voice.preview.rowan").assert(hasContentDescription("Preview Rowan", substring = true))
        systemBack()
        waitForTag("settingsVoice")
        values.forEach { (id, target) -> assertVoiceSlider(id, target) }
        compose.activityRule.scenario.recreate()
        waitForTag("settingsVoice")
        values.forEach { (id, target) -> assertVoiceSlider(id, target) }
        compose.onNodeWithTag("voice.previewSelected").assert(hasContentDescription("Preview Sol", substring = true))
        compose.onNodeWithTag("voice.conversationEntry").assertTextContains("Chat")
        capture("Voice-preferences-recreated-light")
        systemBack()
        waitForTag("agentSettings")
    }

    private fun captureWalletVoiceAppearance(dark: Boolean = false, largeText: Boolean = false) {
        appearance(dark = dark, largeText = largeText)
        val suffix = if (largeText) "large-text" else "dark"
        openDestination("Wallet", "settingsWallet")
        capture("Wallet-$suffix")
        listOf("link" to "Link", "shopPay" to "Shop Pay").forEach { (provider, name) ->
            compose.onNodeWithTag("wallet.provider.$provider").performClick()
            waitForTag("wallet.consent.$provider")
            capture("Wallet-$provider-consent-$suffix")
            compose.onNodeWithText("Next, continue to $name to sign in and review access. Rem will exchange info with $name; see its terms and privacy policy.")
                .performScrollTo().assertIsDisplayed()
            if (largeText) capture("Wallet-$provider-consent-large-text-footer")
            compose.onNodeWithTag("wallet.consent.connect").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("wallet.consent.cancel").performScrollTo().assertIsDisplayed()
            capture("Wallet-$provider-consent-$suffix-actions")
            compose.onNodeWithTag("wallet.consent.cancel").performClick()
            waitForWalletModalDismissed()
        }
        compose.onNodeWithTag("wallet.back").performClick()
        waitForTag("agentSettings")
        compose.onNodeWithTag("agentDestination.Voice").performScrollTo().performClick()
        waitForTag("settingsVoice")
        capture("Voice-$suffix")
        compose.onNodeWithText("Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice.")
            .performScrollTo().assertIsDisplayed()
        capture("Voice-$suffix-footer")
        compose.onNodeWithTag("voice.chooseVoice").performScrollTo().performClick()
        waitForTag("voiceChooser")
        capture("Voice-chooser-$suffix")
        compose.onNodeWithText("Your choice follows this agent across your devices. Tap a play button to hear a preview.")
            .performScrollTo().assertIsDisplayed()
        capture("Voice-chooser-$suffix-footer")
    }
    @Test fun walletVoiceDarkAppearance() = captureWalletVoiceAppearance(dark = true)
    @Test fun walletVoiceLargeTextReachability() = captureWalletVoiceAppearance(largeText = true)

}
