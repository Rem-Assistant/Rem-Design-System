package com.rem.designsystem.demo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.agenda.DateNavigationHeader
import com.rem.designsystem.agentsurfaces.AgentStatusPill
import com.rem.designsystem.agentsurfaces.AgentStatusTone
import com.rem.designsystem.agentsurfaces.BriefCounts
import com.rem.designsystem.agentsurfaces.BrowserLiveCard
import com.rem.designsystem.agentsurfaces.BrowserLiveCardState
import com.rem.designsystem.agentsurfaces.DailyBriefCard
import com.rem.designsystem.agentsurfaces.ExecutionStep
import com.rem.designsystem.agentsurfaces.ExecutionStepStatus
import com.rem.designsystem.agentsurfaces.ExecutionTrace
import com.rem.designsystem.agentsurfaces.ExecutionTraceLane
import com.rem.designsystem.agentsurfaces.ExecutionTraceStatus
import com.rem.designsystem.agentsurfaces.RunningTaskBanner
import com.rem.designsystem.agentsurfaces.RunningTaskTone
import com.rem.designsystem.brand.RemAppIcon
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonSize
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.chat.ActionReceipt
import com.rem.designsystem.chat.ActionReceiptOutcome
import com.rem.designsystem.chat.AddToChatMaxPhotoSelection
import com.rem.designsystem.chat.AddToChatSheet
import com.rem.designsystem.chat.ChatHeader
import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.chat.ChatModelMenu
import com.rem.designsystem.chat.ChatModelOption
import com.rem.designsystem.chat.ChatModelProvider
import com.rem.designsystem.chat.ChatModelSelection
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageDraft
import com.rem.designsystem.chat.MessageDraftCard
import com.rem.designsystem.chat.MessageDraftCardState
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageReactionPicker
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.PollCard
import com.rem.designsystem.chat.PollOption
import com.rem.designsystem.chat.PollPurpose
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.ThinkingLevel
import com.rem.designsystem.chat.VoiceBar
import com.rem.designsystem.chat.VoiceBarState
import com.rem.designsystem.chat.ConnectorCard
import com.rem.designsystem.chat.ConnectorCardModel
import com.rem.designsystem.chat.ConnectorCardState
import com.rem.designsystem.chat.LoginCard
import com.rem.designsystem.chat.LoginCardModel
import com.rem.designsystem.chat.LoginCardState
import com.rem.designsystem.chat.LoginForm
import com.rem.designsystem.chat.LoginSiteBrand
import com.rem.designsystem.chat.LoginSiteMark
import com.rem.designsystem.chat.PermissionCard
import com.rem.designsystem.chat.PermissionCardModel
import com.rem.designsystem.chat.PermissionCardState
import com.rem.designsystem.chat.PermissionRequestDetails
import com.rem.designsystem.chat.loginFormCanSave
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.RemContentUnavailableView
import com.rem.designsystem.rows.AgendaSuggestionRow
import com.rem.designsystem.rows.ConnectorProvider
import com.rem.designsystem.rows.ConnectorProviderMark
import com.rem.designsystem.rows.ConnectorRow
import com.rem.designsystem.rows.ConnectorRowAccessory
import com.rem.designsystem.rows.ConnectorRowState
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.SuggestionAccept
import com.rem.designsystem.rows.SuggestionSection
import com.rem.designsystem.rows.TaskSuggestion
import com.rem.designsystem.onboarding.RemBrandGlyphs
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Component catalog pages (Android). Each page renders the shared RemDesignSystem components with
// the states and interactions they already support; nothing here adds component behaviour.

@Composable
internal fun CatalogPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        content = content,
    )
}

@Composable
internal fun CatalogGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
        Text(title, style = RemTypography.footnote, color = RemColors.current.labelSecondary, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(RemRadius.xlarge)).background(RemColors.current.backgroundSecondary),
        content = content,
    )
}

@Composable
internal fun CatalogRows() {
    var opened by rememberSaveable { mutableIntStateOf(0) }
    var connector by rememberSaveable { mutableStateOf(ConnectorRowState.Available) }
    var connectorOn by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(connector) {
        if (connector == ConnectorRowState.Connecting) { delay(1500); connector = ConnectorRowState.Connected }
    }
    CatalogPage {
        CatalogGroup("List row") {
            Card {
                ListRow(
                    title = "Terms of Service",
                    subtitle = if (opened == 0) "How Rem accounts, subscriptions, and approved actions work." else "Opened",
                    onClick = { opened += 1 },
                    leading = { ContainedIcon(symbol = RemMaterialSymbols.TermsDocument, fill = ContainedIconFill.Subtle) },
                    trailing = { DisclosureChevron() },
                    modifier = Modifier.testTag("catalog.listRow"),
                )
            }
        }
        CatalogGroup("Section") {
            RemSection(header = "Notifications", footer = "Applies to this device.") {
                ListRow(
                    title = "Daily brief",
                    subtitle = "8:00 AM",
                    // Pairs with iOS `sun.max.fill` (icon registry: sun.max ↔ wb_sunny, provisional vector).
                    leading = { ContainedIcon(Icons.Filled.WbSunny, fill = ContainedIconFill.Tint(RemColors.current.systemOrange)) },
                )
            }
        }
        CatalogGroup("Connector row") {
            SegmentedPicker(ConnectorRowState.entries, connector, { connector = it }, { it.name },
                tag = { "catalog.connectorState.${it.name}" })
            Card {
                ConnectorRow(
                    title = "Gmail",
                    state = connector,
                    accessory = when (connector) {
                        ConnectorRowState.Available -> ConnectorRowAccessory.Action("Connect") { connector = ConnectorRowState.Connecting }
                        ConnectorRowState.Connecting -> ConnectorRowAccessory.Progress
                        ConnectorRowState.Connected -> ConnectorRowAccessory.Toggle(connectorOn) { connectorOn = it }
                        ConnectorRowState.Error -> ConnectorRowAccessory.Action("Retry") { connector = ConnectorRowState.Connecting }
                    },
                    leading = { ConnectorProviderMark(ConnectorProvider.Gmail) },
                )
            }
        }
        CatalogGroup("Task and event rows") {
            Card {
                TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"))
                HorizontalDivider(Modifier.padding(start = 60.dp), color = RemColors.current.separator)
                TaskEventRow(kind = TaskEventKind.Event(RemColors.current.systemBlue), title = "Team standup", leading = TaskEventLeading.Time("10:30"), pills = listOf("Work"))
                HorizontalDivider(Modifier.padding(start = 60.dp), color = RemColors.current.separator)
                TaskEventRow(kind = TaskEventKind.Task, title = "Unfiled inbox task", leading = TaskEventLeading.Schedule, showPills = false)
            }
        }
    }
}

private data class CatalogSuggestion(val id: String, val action: SuggestionAccept, val title: String, val metadata: String)

private val catalogSuggestions = listOf(
    CatalogSuggestion("add", SuggestionAccept.Add, "Confirm rehearsal time", "From Slack · Today"),
    CatalogSuggestion("move", SuggestionAccept.Move, "Reply to the venue", "Due today · Move to 3:00 PM"),
)

private val catalogSectionSuggestions = listOf(
    TaskSuggestion("Set up the TestFlight pipeline", "From Granola · 8h ago"),
    TaskSuggestion("Confirm the Compose UI renders", "From Granola · 8h ago"),
    TaskSuggestion("Reply to the venue about the deposit", "Overdue 3d", accept = SuggestionAccept.Move),
    TaskSuggestion("Book the venue for the offsite", "From Slack · Today"),
)

@Composable
internal fun CatalogAgenda() {
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    var resolved by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    // java.util so the demo stays within minSdk 24 without desugaring.
    val dateText = Calendar.getInstance(Locale.US).run {
        clear(); set(2026, Calendar.OCTOBER, 1); add(Calendar.DAY_OF_MONTH, dayOffset)
        SimpleDateFormat("MMM d yyyy", Locale.US).format(time)
    }
    CatalogPage {
        CatalogGroup("Date navigation") {
            DateNavigationHeader(dateText = dateText, onPrevious = { dayOffset -= 1 }, onNext = { dayOffset += 1 })
        }
        // The row specimen stands alone (dashed outline, 4dp apart), as in region frame 2336:19714:
        // no catalog card around it.
        CatalogGroup("Suggestion rows") {
            Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
                catalogSuggestions.forEach { item ->
                    val outcome = resolved[item.id]
                    if (outcome != null) {
                        ListRow(title = item.title, subtitle = outcome)
                    } else {
                        AgendaSuggestionRow(
                            action = item.action, title = item.title, metadata = item.metadata,
                            onAccept = { resolved = resolved + (item.id to if (item.action == SuggestionAccept.Add) "Added" else "Moved") },
                            onDismiss = { resolved = resolved + (item.id to "Dismissed") },
                            acceptTag = "catalog.suggestion.accept.${item.id}",
                            dismissTag = "catalog.suggestion.dismiss.${item.id}",
                        )
                    }
                }
            }
            if (resolved.isNotEmpty()) {
                TextButton(onClick = { resolved = emptyMap() }, modifier = Modifier.testTag("catalog.suggestion.restore")) { Text("Restore") }
            }
        }
        CatalogGroup("Suggestion section") {
            var remaining by remember { mutableStateOf(catalogSectionSuggestions) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            SuggestionSection(
                suggestions = remaining,
                onAccept = { accepted -> remaining = remaining - accepted },
                onDismiss = { dismissed -> remaining = remaining - dismissed },
                inlineLimit = if (expanded) remaining.size else 3,
                onSeeMore = { expanded = true },
                modifier = Modifier.testTag("catalog.suggestionSection"),
            )
            if (remaining.size < catalogSectionSuggestions.size || expanded) {
                TextButton(onClick = { remaining = catalogSectionSuggestions; expanded = false }, modifier = Modifier.testTag("catalog.section.reset")) { Text("Reset") }
            }
        }
    }
}

/**
 * Neutral fictional Chat fixture. Paired with `ChatFixture` in the iOS `CatalogPages.swift`: keep the copy,
 * ids and catalog identical. Providers and models are placeholders, never a production catalog;
 * reactions, delivery and attachments live only in this page's local state.
 */
internal object ChatFixture {
    const val Outgoing = "Can you move the planning sync to Thursday?"
    const val Incoming = "Done \u2014 the planning sync is now Thursday at 10:00, and both attendees have the update."
    const val ReadMessage = "Thanks, that works."
    const val FailedMessage = "Please share the agenda with the group as well."
    /** Illustrative only: the fixture's delivery time, kept when the message is marked Read. */
    const val DeliveredAt = "10:24"
    val Activities = listOf(
        "Connected" to ChatHeaderStatus.Connected,
        "Reading the shared notes" to ChatHeaderStatus.Connected,
        "Needs you" to ChatHeaderStatus.NeedsYou,
    )
    val Providers = listOf(
        ChatModelProvider("provider-a", "Provider A", listOf(ChatModelOption("model-a1", "Model A1"), ChatModelOption("model-a2", "Model A2"))),
        ChatModelProvider("provider-b", "Provider B", listOf(ChatModelOption("model-b1", "Model B1"))),
    )
    const val ManageModelsNote = "Manage Models opens model settings in the app."
}

/** Neutral display data for the chat card specimens (the Figma sample copy is private). */
internal object ChatCardFixture {
    val Draft = MessageDraft(
        from = "me@example.com", to = "alex@example.com", subject = "Re: Product Designer - Next Steps",
        body = "Hi Alex,\n\nThanks for reaching out \u2014 I\u2019ve put time on the calendar. Looking forward to chatting.\n\nBest",
    )
    const val SendNote = "Send Email requested. The app sends; the card shows Sent only on provider confirmation."
    const val DiscardNote = "Discard requested. The app removes the draft."
    const val ChoiceQuestion = "Add the Notion connector so I can use your shared workspace?"
    val ChoiceOptions = listOf(PollOption("add", "Add Notion"), PollOption("later", "Not now"))
    const val SuggestionQuestion = "What would you like to do next?"
    val SuggestionOptions = listOf(
        PollOption("review", "Review the draft"),
        PollOption("calendar", "Check my calendar"),
        PollOption("remind", "Remind me later"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogChat() {
    var draft by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(listOf<String>()) }
    var voice by rememberSaveable { mutableStateOf(VoiceBarState.Listening) }
    var activity by rememberSaveable { mutableIntStateOf(0) }
    var headerCall by rememberSaveable { mutableStateOf(false) }
    var reactions by remember { mutableStateOf(mapOf("chat.incoming" to MessageReaction.ThumbsUp)) }
    var reactingTo by remember { mutableStateOf<String?>(null) }
    var failedDelivered by rememberSaveable { mutableStateOf(false) }
    var model by remember { mutableStateOf<ChatModelSelection>(ChatModelSelection.Automatic) }
    var attachments by remember { mutableStateOf(listOf<ComposerAttachment>()) }
    var showAddToChat by rememberSaveable { mutableStateOf(false) }
    var browserAvailable by rememberSaveable { mutableStateOf(true) }
    var thinking by rememberSaveable { mutableStateOf(ThinkingLevel.Medium) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var cardFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var choiceSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestionSelection by rememberSaveable { mutableStateOf<String?>(null) }
    // Same rule as the composer: text, or a content attachment alone (fixture chips, nothing is read).
    val send = {
        val text = draft.trim()
        val content = attachments.filter { it.kind != ComposerAttachment.Kind.Capability }
        if (text.isNotEmpty() || content.isNotEmpty()) {
            sent = sent + text.ifEmpty { content.joinToString(", ") { it.title } }
            draft = ""
            attachments = attachments - content.toSet()
        }
    }
    // System pickers only report a count here: picked content stays on the device and is never read.
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(AddToChatMaxPhotoSelection)) { uris ->
        if (uris.isNotEmpty()) {
            attachments = attachments.filter { it.kind != ComposerAttachment.Kind.Image } +
                uris.indices.map { ComposerAttachment("photo.$it", "Photo ${it + 1}", ComposerAttachment.Kind.Image) }
            showAddToChat = false
        }
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val start = attachments.count { it.kind == ComposerAttachment.Kind.File }
            attachments = attachments + uris.indices.map {
                ComposerAttachment("file.${start + it}", "Image file ${start + it + 1}", ComposerAttachment.Kind.File)
            }
            showAddToChat = false
        }
    }
    @Composable
    fun message(text: String, role: MessageRole, id: String, delivery: MessageDelivery = MessageDelivery.None) {
        MessageBubble(
            text, role = role, delivery = delivery, reaction = reactions[id], accessibilityPrefix = id,
            onRetry = { failedDelivered = true },
            onLongPress = { reactingTo = id },
        )
    }
    CatalogPage {
        CatalogGroup("Header") {
            val (activityText, status) = ChatFixture.Activities[activity]
            ChatHeader(activity = activityText, status = status, accessibilityPrefix = "chat.header")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatFixture.Activities.forEachIndexed { index, (text, _) ->
                    FilterChip(selected = activity == index, onClick = { activity = index }, label = { Text(text) },
                        modifier = Modifier.testTag("chat.headerActivity.$index"))
                }
            }
        }
        CatalogGroup("Message bubbles") {
            Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                message(ChatFixture.Outgoing, MessageRole.User, "chat.outgoing")
                message(ChatFixture.Incoming, MessageRole.Assistant, "chat.incoming")
                message(ChatFixture.ReadMessage, MessageRole.User, "chat.read", MessageDelivery.Read(ChatFixture.DeliveredAt))
                message(
                    ChatFixture.FailedMessage, MessageRole.User, "chat.failed",
                    if (failedDelivered) MessageDelivery.Delivered(ChatFixture.DeliveredAt) else MessageDelivery.Failed,
                )
                sent.forEachIndexed { index, text -> message(text, MessageRole.User, "chat.sent.$index") }
            }
        }
        CatalogGroup("Composer") {
            RemComposerBar(
                text = draft,
                state = if (draft.isBlank()) ComposerSendState.Idle else ComposerSendState.Active,
                onTextChange = { draft = it },
                onSend = send,
                onAdd = { showAddToChat = true },
                accessibilityPrefix = "catalog",
                attachments = attachments,
                onRemoveAttachment = { removed -> attachments = attachments.filter { it.id != removed.id } },
                modelMenu = { enabled ->
                    ChatModelMenu(
                        providers = ChatFixture.Providers, selection = model, onSelect = { model = it },
                        enabled = enabled, accessibilityPrefix = "catalog",
                        onManageModels = { feedback = ChatFixture.ManageModelsNote },
                    )
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cloud browser available", style = RemTypography.footnote, color = RemColors.current.labelPrimary, modifier = Modifier.weight(1f))
                Switch(checked = browserAvailable, onCheckedChange = { browserAvailable = it }, modifier = Modifier.testTag("chat.browserAvailable"))
            }
            feedback?.let {
                Text(it, style = RemTypography.footnote, color = RemColors.current.labelSecondary, modifier = Modifier.testTag("chat.feedback"))
            }
        }
        CatalogGroup("Composer \u00b7 sending") {
            RemComposerBar(text = "Plan the rest of my day", state = ComposerSendState.Sending)
        }
        CatalogGroup("Voice bar") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceBarState.entries.forEach { state ->
                    FilterChip(selected = voice == state, onClick = { voice = state }, label = { Text(state.name) })
                }
            }
            VoiceBar(state = voice)
        }
        CatalogGroup("Card \u00b7 message draft") {
            // Presentation only: Send/Discard report intent; nothing is sent from the catalog.
            MessageDraftCardState.entries.forEach { state ->
                MessageDraftCard(
                    ChatCardFixture.Draft, state = state,
                    accessibilityPrefix = "catalog.card.draft.${state.name.lowercase()}",
                    onSend = { cardFeedback = ChatCardFixture.SendNote },
                    onDiscard = { cardFeedback = ChatCardFixture.DiscardNote },
                )
            }
            cardFeedback?.let {
                Text(it, style = RemTypography.footnote, color = RemColors.current.labelSecondary, modifier = Modifier.testTag("catalog.card.draft.feedback"))
            }
        }
        CatalogGroup("Card \u00b7 poll") {
            PollCard(
                ChatCardFixture.ChoiceQuestion, ChatCardFixture.ChoiceOptions,
                selection = choiceSelection, accessibilityPrefix = "catalog.card.poll.choice",
                onSelect = { choiceSelection = it },
            )
            // Three options extend the A/B master pattern to C; there is no verified C master.
            PollCard(
                ChatCardFixture.SuggestionQuestion, ChatCardFixture.SuggestionOptions,
                purpose = PollPurpose.Suggestion, selection = suggestionSelection,
                accessibilityPrefix = "catalog.card.poll.suggestion",
                onSelect = { suggestionSelection = it },
            )
            RemButton(
                "Reset polls", onClick = { choiceSelection = null; suggestionSelection = null },
                variant = RemButtonVariant.TextAccent, size = RemButtonSize.Compact,
                modifier = Modifier.testTag("catalog.card.poll.reset"),
            )
        }
        CatalogGroup("Card \u00b7 action receipt") {
            ActionReceiptOutcome.entries.forEach { outcome ->
                ActionReceipt(
                    outcome, if (outcome == ActionReceiptOutcome.Confirmed) "Sent" else "Unconfirmed",
                    accessibilityPrefix = "catalog.card.receipt",
                )
            }
        }
        CatalogConnectorCardGroup()
        CatalogLoginCardGroup()
        CatalogPermissionCardGroup()
        // Last on the page so the established groups above keep their geometry. The trailing slot holds
        // one action: the in-app call entry replaces More (WS1e).
        CatalogGroup("Header trailing action") {
            ChatHeader(activity = "Connected", accessibilityPrefix = "catalog.headerTrailing",
                onOverflow = {}, onCall = if (headerCall) ({}) else null)
            SegmentedPicker(listOf(false, true), headerCall, { headerCall = it }, { if (it) "Call" else "More" },
                tag = { "chat.headerTrailing.${if (it) "Call" else "More"}" })
        }
    }
    reactingTo?.let { target ->
        ModalBottomSheet(onDismissRequest = { reactingTo = null }) {
            Column(Modifier.padding(RemSpacing.lg), verticalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                Text("Reactions", style = RemTypography.footnote, color = RemColors.current.labelSecondary)
                MessageReactionPicker(
                    selection = reactions[target],
                    onSelect = { choice ->
                        reactions = if (choice == null) reactions - target else reactions + (target to choice)
                        reactingTo = null
                    },
                    accessibilityPrefix = "chat.reactions",
                )
            }
        }
    }
    if (showAddToChat) {
        ModalBottomSheet(onDismissRequest = { showAddToChat = false }) {
            AddToChatSheet(
                // The shipped Android app has no camera flow; none is invented here.
                showsCamera = false,
                browserAvailable = browserAvailable,
                thinking = thinking,
                onThinkingChange = { thinking = it },
                onPhotos = { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onFiles = { files.launch(arrayOf("image/*")) },
                onCloudBrowser = {
                    if (ComposerAttachment.CloudBrowser !in attachments) attachments = attachments + ComposerAttachment.CloudBrowser
                    showAddToChat = false
                },
                onDone = { showAddToChat = false },
                accessibilityPrefix = "catalog.addToChat",
            )
        }
    }
}

/**
 * ConnectorCard specimen (twin of the iOS `CatalogConnectorCardGroup`): every state from the picker.
 * Authorize and Retry move to Connecting only — no successful authorization is simulated.
 */
@Composable
private fun CatalogConnectorCardGroup() {
    val states = listOf(ConnectorCardState.Authorize, ConnectorCardState.Connecting, ConnectorCardState.Added, ConnectorCardState.Error())
    val labels = listOf("Authorize", "Connecting", "Added", "Error")
    var index by rememberSaveable { mutableIntStateOf(0) }
    CatalogGroup("Connector card") {
        SegmentedPicker(states.indices.toList(), index, { index = it },
            { labels[it] }, tag = { "catalog.card.connector.state.${labels[it]}" })
        ConnectorCard(
            ConnectorCardModel(ConnectorProvider.Gmail, "Search, read, draft, and manage email.", states[index]),
            onAuthorize = { index = 1 }, onRetry = { index = 1 },
            accessibilityPrefix = "catalog.card.connector",
        )
    }
}

/**
 * LoginCard specimen: Entry opens the native Add login form in a sheet. Fields live only in this page's
 * state and are cleared on Save or dismiss — nothing is stored or sent; Save only flips the card to Saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogLoginCardGroup() {
    var state by rememberSaveable { mutableStateOf(LoginCardState.Entry) }
    var showsChevron by rememberSaveable { mutableStateOf(true) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    val closeForm = { showForm = false; username = ""; password = "" }
    CatalogGroup("Login card") {
        SegmentedPicker(LoginCardState.entries, state, { state = it }, { it.name }, tag = { "catalog.card.login.state.${it.name}" })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chevron on button", style = RemTypography.footnote, color = RemColors.current.labelPrimary, modifier = Modifier.weight(1f))
            Switch(checked = showsChevron, onCheckedChange = { showsChevron = it }, modifier = Modifier.testTag("catalog.card.login.chevron"))
        }
        LoginCard(
            LoginCardModel("GitHub login details", "github.com", state),
            onAddLogin = { note = null; showForm = true },
            onOpenSaved = { note = "Saved login details open in Settings → Cloud browser in the app." },
            showsChevron = showsChevron,
            accessibilityPrefix = "catalog.card.login",
            leading = { LoginSiteMark(LoginSiteBrand.GitHub) },
        )
        note?.let {
            Text(it, style = RemTypography.footnote, color = RemColors.current.labelSecondary, modifier = Modifier.testTag("catalog.card.login.note"))
        }
    }
    if (showForm) {
        ModalBottomSheet(onDismissRequest = closeForm, modifier = Modifier.testTag("catalog.card.login.form")) {
            Column(Modifier.padding(horizontal = RemSpacing.lg).padding(bottom = RemSpacing.xl), verticalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = closeForm, modifier = Modifier.testTag("catalog.card.login.form.cancel")) { Text("Cancel") }
                    Text("Add login", style = RemTypography.bodyBold, color = RemColors.current.labelPrimary, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { state = LoginCardState.Saved; closeForm() },
                        enabled = loginFormCanSave(username, password),
                        modifier = Modifier.testTag("catalog.card.login.form.save"),
                    ) { Text("Save") }
                }
                LoginForm("github.com", username, { username = it }, password, { password = it },
                    testTagPrefix = "catalog.card.login.form")
            }
        }
    }
}

/**
 * PermissionCard specimen: the inline card expands and collapses in place. Allow once / Deny resolve and
 * collapse it; Always allow is only a proposal (no grant); Review again returns to Awaiting.
 */
@Composable
private fun CatalogPermissionCardGroup() {
    var state by rememberSaveable { mutableStateOf(PermissionCardState.Awaiting) }
    var expanded by rememberSaveable { mutableStateOf(PermissionCardState.Awaiting.defaultExpanded) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    val resolve = { newState: PermissionCardState -> note = null; state = newState; expanded = newState.defaultExpanded }
    CatalogGroup("Permission card") {
        SegmentedPicker(PermissionCardState.entries, state, { resolve(it) }, { it.name }, tag = { "catalog.card.permission.state.${it.name}" })
        PermissionCard(
            PermissionCardModel(
                title = "Reminder permission", question = "Allow Rem to create this reminder?",
                summary = "One reminder in your Personal list.",
                details = PermissionRequestDetails("Send investor update", "Oct 10, 2026 · 9:00 AM UTC", "Reminders · Personal"),
                state = state, alwaysAllowScope = "create reminders in Personal only.",
            ),
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onAllow = { resolve(PermissionCardState.Allowed) },
            onDeny = { resolve(PermissionCardState.Denied) },
            onAlwaysAllow = { note = "Always allow is a proposal only. No persistent grant exists." },
            onReviewAgain = { resolve(PermissionCardState.Awaiting) },
            accessibilityPrefix = "catalog.card.permission",
        )
        note?.let {
            Text(it, style = RemTypography.footnote, color = RemColors.current.labelSecondary, modifier = Modifier.testTag("catalog.card.permission.note"))
        }
    }
}

@Composable
internal fun CatalogAgent() {
    var browser by rememberSaveable { mutableStateOf(BrowserLiveCardState.Active) }
    CatalogPage {
        CatalogGroup("Status pill") {
            Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                AgentStatusPill("Working")
                AgentStatusPill("Needs you", tone = AgentStatusTone.Attention)
            }
        }
        CatalogGroup("Running task banner") {
            Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                RunningTaskBanner(task = "Browser", status = "Signing in to my.dnb.com")
                RunningTaskBanner(task = "Browser", status = "Needs you · Password rejected", tone = RunningTaskTone.Attention)
            }
        }
        CatalogGroup("Browser card") {
            SegmentedPicker(BrowserLiveCardState.entries, browser, { browser = it }, { it.name },
                tag = { "catalog.browserState.${it.name}" })
            BrowserLiveCard(browser)
        }
        CatalogGroup("Execution trace") {
            ExecutionTrace(
                status = ExecutionTraceStatus.InProgress,
                title = "Build RFE checklist",
                subtitle = "Writing the RFE checklist PDF template",
                timestamp = "10:49pm",
                steps = listOf(
                    ExecutionStep("Launched checklist subagent", "Delegated the checklist prep to a subagent.", ExecutionTraceLane.Main, ExecutionStepStatus.Done),
                    ExecutionStep("Prepared source directories", "The concatenate step returned an incomplete payload.", ExecutionTraceLane.Subagent("01"), ExecutionStepStatus.Failed),
                    ExecutionStep("Found official guidance", "Searched official guidance for the request.", ExecutionTraceLane.Subagent("01"), ExecutionStepStatus.Done),
                ),
                footer = "Working",
            )
        }
        CatalogGroup("Daily brief card") {
            var reading by remember { mutableStateOf(false) }
            DailyBriefCard(
                title = "Your morning brief",
                summary = "Three things need you today: the investor update, a venue reply, and rehearsal timing.",
                counts = BriefCounts(),
                onRead = { reading = !reading },
                isReading = reading,
            )
        }
    }
}

@Composable
internal fun CatalogBrand() {
    var thinking by rememberSaveable { mutableStateOf(false) }
    var added by rememberSaveable { mutableStateOf(false) }
    CatalogPage {
        CatalogGroup("Face mark") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Thinking", Modifier.weight(1f))
                Switch(checked = thinking, onCheckedChange = { thinking = it }, modifier = Modifier.testTag("catalog.faceThinking"))
            }
            RemFaceMark(modifier = Modifier.testTag("catalog.faceMark"), mode = if (thinking) RemFaceMarkMode.Thinking else RemFaceMarkMode.Idle, tint = RemColors.current.brandBlue, size = 96.dp)
        }
        CatalogGroup("App icon") {
            RemAppIcon(size = 64.dp, cornerRadius = RemRadius.large)
        }
        CatalogGroup("Provider marks") {
            Row(Modifier.testTag("catalog.providerMarks"), horizontalArrangement = Arrangement.spacedBy(RemSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                Image(imageVector = RemBrandGlyphs.GoogleG, contentDescription = "Google", modifier = Modifier.size(26.dp))
                ConnectorProvider.entries.forEach { ConnectorProviderMark(it) }
            }
        }
        CatalogGroup("Empty state") {
            if (added) {
                Card {
                    TaskEventRow(kind = TaskEventKind.Task, title = "New task", leading = TaskEventLeading.Schedule, showPills = false)
                }
                TextButton(onClick = { added = false }, modifier = Modifier.testTag("catalog.emptyReset")) { Text("Reset") }
            } else {
                Column(Modifier.fillMaxWidth().heightIn(min = 280.dp)) {
                    RemContentUnavailableView(
                        icon = Icons.Filled.DateRange,
                        title = "No agenda yet",
                        message = "Create a new task or schedule existing ones",
                        actionLabel = "Add New",
                        onAction = { added = true },
                    )
                }
            }
        }
    }
}
