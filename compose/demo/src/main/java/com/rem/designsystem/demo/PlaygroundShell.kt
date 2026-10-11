package com.rem.designsystem.demo

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rem.designsystem.agents.NewAgentSetupScreen
import com.rem.designsystem.navigation.RemSidebar
import com.rem.designsystem.navigation.RemSidebarContainer
import com.rem.designsystem.navigation.RemSidebarSelection
import com.rem.designsystem.screens.AgentSettingsDestination
import com.rem.designsystem.screens.ChatPlaygroundFixture
import com.rem.designsystem.screens.PlaygroundShellFixture
import com.rem.designsystem.screens.PlaygroundShellPresentation
import com.rem.designsystem.screens.SettingsAboutFixture
import com.rem.designsystem.screens.SettingsAboutScreen
import com.rem.designsystem.screens.SettingsBillingScreen
import com.rem.designsystem.screens.SettingsEntryContent
import com.rem.designsystem.screens.SettingsEntryDestination
import com.rem.designsystem.screens.SettingsHelpScreen
import com.rem.designsystem.screens.SettingsPermissionsScreen
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

// The Playground app shell (Build 9) — twin of the iOS `PlaygroundShell`. Root: one full-screen
// conversation (`2702:2351`); the header's sidebar button opens the sidebar (`2702:2479`): Main agent,
// Side agents, New agent, and the account pill, which opens Settings. Every state transition goes
// through the DS `PlaygroundShellFixture`; this file draws the native chrome only. Fixtures only: no
// backend, auth, voice, telephony or provider. The Developer hub (catalog, fixture pickers, every
// screen) is reached from Settings' "Developer" action. Route map: `docs/playground/route-map.md`.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playground() {
    // The shell stays composed under Settings and New agent, so returning finds the same agent,
    // conversation (sent messages included) and scroll position; Developer keeps the selected agent.
    var shell by remember { mutableStateOf(PlaygroundShellFixture()) }
    var developerOpen by rememberSaveable { mutableStateOf(false) }
    // Developer-owned fixtures the shell reads; changed only in the Developer hub.
    var settingsFixture by rememberSaveable { mutableStateOf(LoadFixture.Success) }
    var chatConversation by rememberSaveable { mutableStateOf(ChatPlaygroundFixture.Conversation.Populated) }
    val settingsOpen = shell.presented == PlaygroundShellPresentation.Settings && !developerOpen
    val covered = developerOpen || settingsOpen

    Box(Modifier.fillMaxSize()) {
        // Under Settings the shell stays composed (same agent, conversation and scroll position on return) and
        // leaves the merged semantics tree. The Developer hub hosts its own Chat with the same test tags, and
        // clearAndSetSemantics does not remove nodes from the unmerged tree, so the shell is not composed under
        // Developer: returning keeps the selected agent (state above) and opens its conversation afresh.
        if (!developerOpen) Box(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            RemSidebarContainer(
                isOpen = shell.isSidebarOpen,
                onOpenChange = { open -> shell = if (open) shell.openSidebar() else shell.closeSidebar() },
                sidebar = {
                    RemSidebar(
                        display = shell.sidebar,
                        onAction = { shell = shell.handle(it) },
                        modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                    )
                },
            ) {
                // A new agent (or a different fixture chosen in Developer) is a different conversation.
                key(shell.conversationKey, chatConversation) {
                    val agent = shell.selectedAgent
                    val initial = if (shell.selection == RemSidebarSelection.Main) ChatPlaygroundFixture.root(chatConversation)
                    else ChatPlaygroundFixture.root(ChatPlaygroundFixture.Conversation.Empty, agentName = agent.name)
                    PlaygroundChatScreen(initial, onExit = {}, onOpenSidebar = { shell = shell.openSidebar() })
                }
            }
        }
        // System Back closes an open sidebar before it leaves the app.
        BackHandler(shell.isSidebarOpen && !covered) { shell = shell.closeSidebar() }

        if (shell.presented == PlaygroundShellPresentation.NewAgent && !developerOpen) {
            val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { shell = shell.handle(com.rem.designsystem.agents.NewAgentSetupAction.Cancel) },
                sheetState = sheet,
                dragHandle = null,
                containerColor = RemColors.current.backgroundPrimary,
                modifier = Modifier.testTag("shell.newAgentSheet"),
            ) {
                NewAgentSetupScreen(
                    draft = shell.draft,
                    onDraftChange = { shell = shell.editDraft(it) },
                    onAction = { shell = shell.handle(it) },
                )
            }
        }
        if (settingsOpen) {
            CoveringLayer {
                ShellSettings(
                    fixture = settingsFixture,
                    onDone = { shell = shell.dismissPresentation() },
                    onDeveloper = { shell = shell.dismissPresentation(); developerOpen = true },
                )
            }
        }
        if (developerOpen) {
            CoveringLayer {
                DeveloperHub(
                    fixture = settingsFixture,
                    onFixture = { settingsFixture = it },
                    chatConversation = chatConversation,
                    onChatConversation = { chatConversation = it },
                    onClose = { developerOpen = false },
                )
            }
        }
    }
}

/** An opaque full-screen layer that takes every touch, so nothing below it reacts. */
@Composable
private fun CoveringLayer(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(RemColors.current.backgroundPrimary)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) { content() }
}

/**
 * Settings opened from the account pill: the Build 8 Settings entry and its linked destinations, over the
 * unchanged conversation. Back pops one level; at the entry it returns to the chat. The top bar's
 * **Developer** action (`settings.developer`) is the one route into the Developer hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellSettings(fixture: LoadFixture, onDone: () -> Unit, onDeveloper: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf<SettingsEntryDestination?>(null) }
    var agent by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf<AgentSettingsDestination?>(null) }
    // Destinations and entry pages draw their own chrome and own Back.
    val fullScreen = page != null || destination != null
    val back = {
        when {
            agent -> agent = false
            else -> onDone()
        }
    }
    BackHandler(!fullScreen) { back() }
    Scaffold(containerColor = RemColors.current.backgroundPrimary, topBar = {
        if (!fullScreen) {
            CenterAlignedTopAppBar(
                title = { Text(if (agent) "Agent settings" else "Settings", style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = back, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (agent) "Back" else "Back to chat")
                    }
                },
                actions = {
                    if (!agent) TextButton(onClick = onDeveloper, modifier = Modifier.testTag("settings.developer")) { Text("Developer") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RemColors.current.backgroundPrimary),
            )
        }
    }) { padding ->
        Box(Modifier.then(if (!fullScreen) Modifier.padding(padding) else Modifier).fillMaxSize()) {
            when {
                page == SettingsEntryDestination.Billing -> SettingsBillingScreen(onBack = { page = null })
                page == SettingsEntryDestination.Permissions -> SettingsPermissionsScreen(onBack = { page = null })
                page == SettingsEntryDestination.About -> SettingsAboutScreen(onBack = { page = null },
                    version = SettingsAboutFixture.versionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()))
                page == SettingsEntryDestination.HelpSupport -> SettingsHelpScreen(onBack = { page = null })
                agent -> AgentPreview(fixture, destination = destination,
                    onOpenDestination = { destination = it }, onDestinationBack = { destination = null },
                    onCancel = { agent = false })
                else -> Column(Modifier.verticalScroll(rememberScrollState()).testTag("shell.settings")) {
                    SettingsEntryContent(openAgent = { agent = true }, onShare = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Rem — a personal AI assistant.")
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    }, availableDestinations = SettingsEntryDestination.entries.toSet(),
                        openDestination = { page = it })
                }
            }
        }
    }
}
