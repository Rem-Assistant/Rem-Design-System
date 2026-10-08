package com.rem.designsystem.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import com.rem.designsystem.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rem.designsystem.rows.*
import com.rem.designsystem.tokens.*

/** Native Material controls around the shared provider rows. Session remains in memory only. */
@Composable
fun SettingsConnectorsScreen(onBack: () -> Unit, session: SettingsConnectorsSession = viewModel()) {
    var boundary by remember { mutableStateOf<String?>(null) }
    fun back() { if (!session.back()) onBack() }
    BackHandler { back() }
    when (val route = session.route) {
        ConnectorRoute.Root -> ConnectorsRoot(session, ::back, { boundary = it })
        ConnectorRoute.Gmail -> GmailDetail(session, ::back, { boundary = it })
        is ConnectorRoute.Permissions -> GmailPermissions(session, route.accountId, ::back)
    }
    boundary?.let { message ->
        AlertDialog(onDismissRequest = { boundary = null }, title = { Text("Prototype boundary") },
            text = { Text(message) }, confirmButton = {
                TextButton(onClick = { boundary = null }) { Text("Done") }
            }, modifier = Modifier.testTag("connectors.boundary"))
    }
}

@Composable
private fun ConnectorsRoot(session: SettingsConnectorsSession, onBack: () -> Unit, boundary: (String) -> Unit) {
    ConnectorsScaffold("Connectors", "settingsConnectors", onBack) {
        RemSection(header = "Connected", settingsHeader = true) {
            val connected = buildList {
                if (session.fixture.isConnected) add(ConnectorProvider.Gmail)
                addAll(listOf(ConnectorProvider.GoogleCalendar, ConnectorProvider.Notion, ConnectorProvider.Slack))
            }
            connected.forEachIndexed { index, provider ->
                ConnectorRow(provider.title, ConnectorRowState.Connected, ConnectorRowAccessory.Disclosure,
                    subtitle = if (provider == ConnectorProvider.Slack) "Connected • Paused" else "Connected • Active",
                    modifier = Modifier.testTag("connectors.provider.${provider.name}"),
                    showsDivider = index < connected.lastIndex,
                    onClick = {
                        if (provider == ConnectorProvider.Gmail) session.openGmail()
                        else boundary("${provider.title} account details are not included in this prototype. No connection is changed.")
                    }, leading = { ConnectorProviderMark(provider) })
            }
        }
        RemSection(header = "Available", settingsHeader = true) {
            val available = buildList {
                if (!session.fixture.isConnected) add(ConnectorProvider.Gmail)
                addAll(listOf(ConnectorProvider.GoogleDrive, ConnectorProvider.Linear, ConnectorProvider.Todoist))
            }
            available.forEachIndexed { index, provider ->
                ConnectorRow(provider.title, ConnectorRowState.Available, ConnectorRowAccessory.Action("Connect") {
                    boundary("Connecting ${provider.title} requires provider authorization, which is not included in this prototype. No account is connected.")
                }, modifier = Modifier.testTag("connectors.provider.${provider.name}"), showsDivider = index < available.lastIndex,
                    leading = { ConnectorProviderMark(provider) })
            }
        }
    }
}

private sealed interface GmailDisconnect {
    data object All : GmailDisconnect
    data class Account(val account: GmailAccountFixture) : GmailDisconnect
}

@Composable
private fun GmailDetail(session: SettingsConnectorsSession, onBack: () -> Unit, boundary: (String) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<GmailDisconnect?>(null) }
    ConnectorsScaffold("Gmail", "gmail.detail", onBack, actions = {
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("gmail.connectorMenu")) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "Gmail actions")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Disconnect accounts", color = RemColors.current.systemRed) },
                    onClick = { menuExpanded = false; confirmation = GmailDisconnect.All },
                    modifier = Modifier.testTag("gmail.disconnectAll"))
            }
        }
    }) {
        RemSection {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { ConnectorProviderMark(ConnectorProvider.Gmail) }
                Text("Gmail", style = RemTypography.title1Bold, color = RemColors.current.labelPrimary)
                Text("Read and manage Gmail", style = RemTypography.body, color = RemColors.current.labelSecondary)
            }
        }
        RemSection(header = "Connected accounts", settingsHeader = true) {
            session.fixture.accounts.forEach { account ->
                GmailAccountRow(account,
                    onSettings = { session.openPermissions(account.id) },
                    onDisconnect = { confirmation = GmailDisconnect.Account(account) })
            }
            TextButton(onClick = {
                boundary("Adding a Gmail account requires provider authorization, which is not included in this prototype. No account is connected.")
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("gmail.connectAnother")) {
                Text("Connect another account", style = RemTypography.body, modifier = Modifier.fillMaxWidth())
            }
        }
        RemSection {
            GmailNavRow("Permissions", session.fixture.connectorPermission.title, "gmail.permissions") {
                session.openPermissions()
            }
        }
        GmailActionSection("Read actions", GmailFixtureCopy.readActions, boundary)
        GmailActionSection("Write actions", GmailFixtureCopy.writeActions, boundary)
        RemSection(header = "Information", settingsHeader = true) {
            GmailNavRow("Category", "Productivity", "gmail.info.Category") {
                boundary("Connector category details are not included in this prototype.")
            }
            GmailNavRow("Website", "mail.google.com", "gmail.info.Website") {
                boundary("External Gmail website navigation is not included in this prototype.")
            }
            GmailNavRow("Privacy Policy", "policies.google.com", "gmail.info.Privacy Policy") {
                boundary("External privacy-policy navigation is not included in this prototype.")
            }
            GmailNavRow("Report an issue", null, "gmail.info.Report an issue") {
                boundary("Issue reporting is not included in this prototype. No report is sent.")
            }
        }
    }
    confirmation?.let { target ->
        val account = (target as? GmailDisconnect.Account)?.account
        val title = account?.let { "Disconnect ${it.email}?" } ?: GmailFixtureCopy.disconnectAllTitle
        val message = account?.let { GmailFixtureCopy.disconnectMessage(it.email) } ?: GmailFixtureCopy.disconnectAllMessage
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(title) }, text = { Text(message) },
            modifier = Modifier.testTag("gmail.disconnectConfirmation"),
            confirmButton = {
                TextButton(onClick = { session.disconnect(account?.id); confirmation = null },
                    modifier = Modifier.testTag("gmail.confirmDisconnect")) {
                    Text(if (account == null) "Disconnect accounts" else "Disconnect account", color = RemColors.current.systemRed)
                }
            }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } })
    }
}

@Composable
private fun GmailAccountRow(account: GmailAccountFixture, onSettings: () -> Unit, onDisconnect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListRow(modifier = Modifier.testTag("gmail.account.${account.id}"), showsDivider = true,
        leading = { GmailAccountAvatar() }, content = { ListRowLabel(account.email, account.role) }, trailing = {
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("gmail.accountMenu.${account.id}")) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = "Actions for ${account.email}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Settings") },
                        onClick = { expanded = false; onSettings() }, modifier = Modifier.testTag("gmail.accountSettings.${account.id}"))
                    DropdownMenuItem(text = { Text("Disconnect account", color = RemColors.current.systemRed) },
                        onClick = { expanded = false; onDisconnect() }, modifier = Modifier.testTag("gmail.disconnectAccount.${account.id}"))
                }
            }
        })
}

@Composable
private fun GmailPermissions(session: SettingsConnectorsSession, accountId: String?, onBack: () -> Unit) {
    val account = accountId?.let { session.fixture.account(it) }
    val selected = accountId?.let { session.fixture.permission(it) } ?: session.fixture.connectorPermission
    val scope = accountId ?: "connector"
    ConnectorsScaffold(if (account == null) "Permissions" else "Settings", "gmail.permissions.$scope", onBack) {
        RemSection {
            ListRow(leading = {
                if (account == null) ConnectorProviderMark(ConnectorProvider.Gmail) else GmailAccountAvatar(emphasized = false)
            }, content = {
                ListRowLabel(account?.email ?: "Gmail", account?.let { "Gmail · ${it.role} account" } ?: "All connected accounts")
            }, trailing = {})
        }
        RemSection(header = "Access level", settingsHeader = true,
            footer = account?.let { GmailFixtureCopy.accountFooter(it.email) } ?: GmailFixtureCopy.connectorFooter) {
            Column(Modifier.selectableGroup()) {
                GmailPermission.entries.forEachIndexed { index, permission ->
                    GmailPermissionRow(permission, selected == permission, "gmail.permission.$scope.${permission.name}") {
                        session.selectPermission(permission, accountId)
                    }
                    if (index < GmailPermission.entries.lastIndex) GmailDivider()
                }
            }
        }
    }
}

@Composable
private fun GmailPermissionRow(permission: GmailPermission, selected: Boolean, tag: String, onSelect: () -> Unit) {
    val colors = RemColors.current
    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
        .padding(horizontal = 16.dp, vertical = 12.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(permission.title, style = RemTypography.body, color = colors.labelPrimary, modifier = Modifier.weight(1f, fill = false))
                if (permission == GmailPermission.LowRisk || permission == GmailPermission.AlwaysAllow) {
                    val risk = permission == GmailPermission.AlwaysAllow
                    Row(Modifier.background((if (risk) colors.systemRed else colors.brandBlue).copy(alpha = .12f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (risk) Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.systemRed, modifier = Modifier.size(12.dp))
                        Text(if (risk) "Elevated risk" else "Default", style = RemTypography.caption1,
                            color = if (risk) colors.labelPrimary else colors.brandBlue)
                    }
                }
            }
            Text(permission.explanation, style = RemTypography.footnote, color = colors.labelSecondary)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun GmailNavRow(title: String, subtitle: String?, tag: String, onClick: () -> Unit) {
    ListRow(modifier = Modifier.testTag(tag), onClick = onClick,
        leading = {}, content = { ListRowLabel(title, subtitle) }, trailing = { DisclosureChevron() })
}
@Composable
private fun GmailActionSection(title: String, actions: List<String>, boundary: (String) -> Unit) {
    RemSection(header = title, settingsHeader = true) {
        actions.forEachIndexed { index, action ->
            GmailNavRow(action, null, "gmail.action.$action") {
                boundary("$action is a listed Gmail capability. Executing actions is not included in this prototype; no email is read or changed.")
            }
            if (index < actions.lastIndex) GmailDivider()
        }
    }
}
@Composable private fun GmailDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = .5.dp, color = RemColors.current.separator)
}
/** Source geometry: blue detail override and neutral permissions master, no photo/initials. */
@Composable private fun GmailAccountAvatar(emphasized: Boolean = true) {
    Box(Modifier.size(29.dp).background(if (emphasized) RemColors.current.systemBlue else colorResource(R.color.connector_avatar_neutral), CircleShape))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectorsScaffold(title: String, tag: String, onBack: () -> Unit,
                               actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = RemColors.current.backgroundPrimary, topBar = {
        CenterAlignedTopAppBar(title = { Text(title, style = RemTypography.bodyBold) }, navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("connectors.back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, actions = actions, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RemColors.current.backgroundPrimary))
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp).testTag(tag),
            verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
    }
}
