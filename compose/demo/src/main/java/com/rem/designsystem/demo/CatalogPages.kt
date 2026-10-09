package com.rem.designsystem.demo

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
import androidx.compose.material3.FilterChip
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
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.VoiceBar
import com.rem.designsystem.chat.VoiceBarState
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
                ListRow(title = "Daily brief", subtitle = "8:00 AM")
            }
        }
        CatalogGroup("Connector row") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectorRowState.entries.forEach { state ->
                    FilterChip(selected = connector == state, onClick = { connector = state }, label = { Text(state.name) })
                }
            }
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
        CatalogGroup("Suggestion rows") {
            Card {
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
        // Compose-only: the SwiftUI sibling (SharedSuggestionSection) lives in the app, not the design system.
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

@Composable
internal fun CatalogChat() {
    var draft by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(listOf<String>()) }
    var voice by rememberSaveable { mutableStateOf(VoiceBarState.Listening) }
    val send = {
        val text = draft.trim()
        if (text.isNotEmpty()) { sent = sent + text; draft = "" }
    }
    CatalogPage {
        CatalogGroup("Message bubbles") {
            Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
                MessageBubble("Done — I archived 38 newsletters and snoozed 5 low-priority threads.", role = MessageRole.Assistant)
                sent.forEach { MessageBubble(it, role = MessageRole.User, meta = "Now") }
            }
        }
        CatalogGroup("Composer") {
            RemComposerBar(
                text = draft,
                state = if (draft.isBlank()) ComposerSendState.Idle else ComposerSendState.Active,
                onTextChange = { draft = it },
                onSend = send,
                accessibilityPrefix = "catalog",
            )
        }
        CatalogGroup("Voice bar") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceBarState.entries.forEach { state ->
                    FilterChip(selected = voice == state, onClick = { voice = state }, label = { Text(state.name) })
                }
            }
            VoiceBar(state = voice)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowserLiveCardState.entries.forEach { state ->
                    FilterChip(selected = browser == state, onClick = { browser = state }, label = { Text(state.name) },
                        modifier = Modifier.testTag("catalog.browserState.${state.name}"))
                }
            }
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
        // Compose-only component: there is no SwiftUI DailyBriefCard.
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
            RemFaceMark(mode = if (thinking) RemFaceMarkMode.Thinking else RemFaceMarkMode.Idle, tint = RemColors.current.brandBlue, size = 96.dp)
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
