package com.rem.designsystem.demo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.rem.designsystem.chat.AddToChatMaxPhotoSelection
import com.rem.designsystem.chat.AddToChatSheet
import com.rem.designsystem.chat.ChatModelMenu
import com.rem.designsystem.chat.ChatModelSelection
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.ThinkingLevel
import com.rem.designsystem.screens.AgentActivityScreen
import com.rem.designsystem.screens.AgentActivityTab
import com.rem.designsystem.screens.ChatPlaygroundEffect
import com.rem.designsystem.screens.ChatPlaygroundFixture
import com.rem.designsystem.screens.ChatScreen
import com.rem.designsystem.screens.ChatScreenAction
import com.rem.designsystem.screens.ChatTranscriptList
import com.rem.designsystem.screens.InboxAction
import com.rem.designsystem.screens.InboxPlaygroundFixture
import com.rem.designsystem.screens.InboxScreen
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

// Full-screen Chat, task reply and Inbox (Playground 7 candidate) — twin of the iOS
// `PlaygroundChatScreen` / `PlaygroundInboxScreen`. Canonical compositions driven by the DS
// `ChatPlaygroundFixture` / `InboxPlaygroundFixture`. The header owns Back and overflow; overflow opens
// the fixture-host controls, the only source of receipts here. Nothing leaves the page. The header
// identity opens Agent activity (`2002:76914`) over the same remembered fixture, so its Back (or system
// Back) returns to the unchanged conversation.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundChatScreen(initial: ChatPlaygroundFixture, onExit: () -> Unit) {
    var fixture by remember { mutableStateOf(initial) }
    var model by remember { mutableStateOf<ChatModelSelection>(ChatModelSelection.Automatic) }
    var thinking by remember { mutableStateOf(ThinkingLevel.Medium) }
    var showHostControls by remember { mutableStateOf(false) }
    var showAddToChat by remember { mutableStateOf(false) }
    var showActivity by remember { mutableStateOf(false) }
    var activityTab by remember { mutableStateOf(AgentActivityTab.Activity) }

    val handle: (ChatScreenAction) -> Unit = { action ->
        val (next, effect) = fixture.handle(action)
        fixture = next
        when (effect) {
            ChatPlaygroundEffect.Exit -> onExit()
            ChatPlaygroundEffect.PresentHostControls -> showHostControls = true
            ChatPlaygroundEffect.PresentAddToChat -> showAddToChat = true
            ChatPlaygroundEffect.PresentActivity -> { activityTab = AgentActivityTab.Activity; showActivity = true }
            null -> Unit
        }
    }
    val attach: (ComposerAttachment) -> Unit = {
        fixture = fixture.copy(composer = fixture.composer.attach(it))
        showAddToChat = false
    }
    // Real system pickers, as in the Chat catalog. The Photo Picker and the document picker need no
    // runtime permission: each grants access only to what the person picks. Only the pick count and a
    // document's display name are used; no content is opened, read or uploaded.
    val context = LocalContext.current
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(AddToChatMaxPhotoSelection)) { uris ->
        if (uris.isNotEmpty()) {
            fixture = fixture.copy(composer = fixture.composer.attachPickedPhotos(uris.size))
            showAddToChat = false
        }
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            fixture = fixture.copy(composer = fixture.composer.attachPickedFiles(uris.map { displayName(context, it) }))
            showAddToChat = false
        }
    }

    if (showActivity) {
        // Current state is read from the live Chat header; history never replaces it. This handler is
        // registered after the Playground's Chat one, so system Back returns to Chat, not the root.
        BackHandler { showActivity = false }
        AgentActivityScreen(
            display = fixture.activity.display,
            selectedTab = activityTab,
            onSelectTab = { activityTab = it },
            onBack = { showActivity = false },
        )
        return
    }

    ChatScreen(
        header = fixture.header,
        composer = fixture.composer.state,
        onAction = handle,
        replyContext = fixture.replyContext,
        emptyState = fixture.emptyState,
        modelMenu = { enabled ->
            ChatModelMenu(
                providers = ChatFixture.Providers, selection = model, onSelect = { model = it },
                enabled = enabled, accessibilityPrefix = "chat",
                onManageModels = { fixture = fixture.copy(composer = fixture.composer.show(ChatFixture.ManageModelsNote)) },
            )
        },
    ) {
        ChatTranscriptList(fixture.entries, onAction = { handle(ChatScreenAction.Transcript(it)) })
        (fixture.note ?: fixture.composer.note)?.let {
            Text(it, style = RemTypography.footnote, color = RemColors.current.labelSecondary,
                modifier = Modifier.fillMaxWidth().testTag("chat.fixtureNote"))
        }
    }

    if (showHostControls) {
        ModalBottomSheet(onDismissRequest = { showHostControls = false }) {
            Column(Modifier.padding(RemSpacing.lg), verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                Text("Fixture host", style = RemTypography.bodyBold, color = RemColors.current.labelPrimary)
                Text("Stand-ins for evidence the app receives from its runtime. Fixture only.",
                    style = RemTypography.footnote, color = RemColors.current.labelSecondary)
                listOf(
                    Triple("Host accepted the message", "chat.host.accept") { fixture = fixture.simulateHostAcceptance() },
                    Triple("Recipient acknowledged (Read)", "chat.host.read") { fixture = fixture.simulateReadAcknowledgement() },
                    Triple("Host reported not delivered", "chat.host.fail") { fixture = fixture.simulateDeliveryFailure() },
                    Triple("Reply complete", "chat.host.reply") { fixture = fixture.simulateReplyComplete() },
                ).forEach { (label, tag, run) ->
                    TextButton(onClick = { run(); showHostControls = false }, modifier = Modifier.fillMaxWidth().testTag(tag)) { Text(label) }
                }
            }
        }
    }
    if (showAddToChat) {
        ModalBottomSheet(onDismissRequest = { showAddToChat = false }) {
            AddToChatSheet(
                showsCamera = false,
                browserAvailable = true,
                thinking = thinking,
                onThinkingChange = { thinking = it },
                onPhotos = { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onFiles = { files.launch(arrayOf("image/*")) },
                onCloudBrowser = { attach(ComposerAttachment.CloudBrowser) },
                onDone = { showAddToChat = false },
                accessibilityPrefix = "chat.addToChat",
            )
        }
    }
}

/** A picked document's display name (metadata only; the file itself is never opened). */
private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "File"

/** The unified Inbox fixture: host-reported item states; tapping an item opens its task reply chat. */
@Composable
fun PlaygroundInboxScreen(inbox: InboxPlaygroundFixture, open: (String) -> Unit) {
    InboxScreen(items = inbox.items, onAction = { action -> if (action is InboxAction.Open) open(action.itemId) }) {
        Text(
            InboxPlaygroundFixture.EmptyMessage, style = RemTypography.body, color = RemColors.current.labelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = RemSpacing.xl).testTag("inbox.empty"),
        )
    }
}
