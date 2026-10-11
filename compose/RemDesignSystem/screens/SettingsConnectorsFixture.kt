package com.rem.designsystem.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** The four source policies; these are local fixture choices, never real authorization. */
enum class GmailPermission(val title: String, val explanation: String) {
    AlwaysAsk("Always ask", "Ask before reading messages or taking any Gmail action."),
    LowRisk("Allow low-risk actions", "Read messages and manage drafts or labels. Ask before sending or deleting email."),
    AlwaysAllow("Always allow", "Allow every Gmail action without approval, including sending or deleting email."),
    NeverAllow("Never allow", "Block Gmail access until you change this permission."),
}

data class GmailAccountFixture(val id: String, val email: String, val role: String = "Primary") {
    companion object { val primary = GmailAccountFixture("avery", "avery@example.com") }
}

/** Value fixture: disconnect removes only memberships and overrides, never email. */
data class GmailFixture(
    val accounts: List<GmailAccountFixture> = listOf(GmailAccountFixture.primary),
    val connectorPermission: GmailPermission = GmailPermission.LowRisk,
    val accountOverrides: Map<String, GmailPermission> = emptyMap(),
) {
    val isConnected: Boolean get() = accounts.isNotEmpty()
    fun account(id: String) = accounts.firstOrNull { it.id == id }
    fun permission(accountId: String) = accountOverrides[accountId] ?: connectorPermission
    fun withConnectorPermission(value: GmailPermission) = copy(connectorPermission = value)
    fun withAccountPermission(accountId: String, value: GmailPermission) =
        if (account(accountId) == null) this else copy(accountOverrides = accountOverrides + (accountId to value))
    fun disconnect(accountId: String) = copy(
        accounts = accounts.filterNot { it.id == accountId }, accountOverrides = accountOverrides - accountId,
    )
    fun disconnectAll() = copy(accounts = emptyList(), accountOverrides = emptyMap())
}

/** Retained only in memory through activity recreation. No SavedStateHandle, storage or network. */
class SettingsConnectorsSession : ViewModel() {
    var fixture by mutableStateOf(GmailFixture())
        private set
    var route by mutableStateOf<ConnectorRoute>(ConnectorRoute.Root)
        private set
    fun openGmail() { if (fixture.isConnected) route = ConnectorRoute.Gmail }
    fun openPermissions(accountId: String? = null) {
        if (accountId == null || fixture.account(accountId) != null) route = ConnectorRoute.Permissions(accountId)
    }
    fun selectPermission(value: GmailPermission, accountId: String? = null) {
        fixture = if (accountId == null) fixture.withConnectorPermission(value)
            else fixture.withAccountPermission(accountId, value)
    }
    fun disconnect(accountId: String? = null) {
        fixture = if (accountId == null) fixture.disconnectAll() else fixture.disconnect(accountId)
        route = if (fixture.isConnected) ConnectorRoute.Gmail else ConnectorRoute.Root
    }
    fun back(): Boolean = when (route) {
        ConnectorRoute.Root -> false
        ConnectorRoute.Gmail -> { route = ConnectorRoute.Root; true }
        is ConnectorRoute.Permissions -> { route = ConnectorRoute.Gmail; true }
    }
}
sealed interface ConnectorRoute {
    data object Root : ConnectorRoute
    data object Gmail : ConnectorRoute
    data class Permissions(val accountId: String?) : ConnectorRoute
}

object GmailFixtureCopy {
    val readActions = listOf("Batch read email", "Batch read email threads", "Search emails", "Search email ids", "Search thread ids")
    val writeActions = listOf("Apply labels to emails", "Archive emails", "Batch modify email", "Create draft", "Send email", "Send draft", "Update draft")
    const val connectorFooter = "Controls the level of access Rem has across all Gmail accounts connected to this connector."
    fun accountFooter(email: String) = "Controls what Rem can do with $email. Overrides the connector-wide setting for this account."
    const val disconnectAllTitle = "Disconnect all Gmail accounts?"
    const val disconnectAllMessage = "Rem will lose Gmail access for every connected account. Your emails are not deleted. You can reconnect accounts at any time."
    fun disconnectMessage(email: String) = "Rem will lose Gmail access for $email. Your emails are not deleted. You can reconnect this account at any time."
}
