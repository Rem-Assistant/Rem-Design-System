package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

// Memory destination for the bounded Settings playground — Figma masters `Screen/Memory`
// (`1833:5096`) and `Screen/Memory summary` (`1865:6193`). Code-only prototype: toggles and the
// composer are in-memory fixture state for the session; no chat generation, memory persistence, or
// backend. The destination owns its scaffold/title and a nested root↔summary stack, and takes a
// standard [onBack] callback for the outer navigation.

/** The three independent Memory control toggles. */
enum class MemoryControl { SearchAndReference, GenerateMemory, SensitiveTopics }

/** One heading + paragraph of the generated summary. */
data class MemorySummarySection(val heading: String, val body: String)

/** Deterministic Memory fixture: the exact source copy and the independent toggle defaults. */
object MemoryContent {
    const val controlsFooter = "Rem can learn from conversations. You stay in control of what it keeps."
    const val overviewFooter = "Open the generated summary to ask Rem to correct, add, or forget something."
    const val summaryMetadata = "Updated just now · Generated from your conversations"
    const val composerPlaceholder = "Ask or update memory"

    /** Exact source defaults: two on, Sensitive topics off. Independent — no mutual exclusion. */
    val toggleDefaults: Map<MemoryControl, Boolean> = mapOf(
        MemoryControl.SearchAndReference to true,
        MemoryControl.GenerateMemory to true,
        MemoryControl.SensitiveTopics to false,
    )

    val summarySections: List<MemorySummarySection> = listOf(
        MemorySummarySection(
            "Overview",
            "You prefer direct, practical help and clear product decisions. Rem should use prior conversations when they are relevant, keep durable preferences current, and avoid treating short-lived details as permanent facts.",
        ),
        MemorySummarySection(
            "How Rem should work",
            "Be resourceful before asking. Use the tools and context already available, show what changed, and keep proposed ideas distinct from behavior that exists in the product.",
        ),
        MemorySummarySection(
            "Current focus",
            "You are simplifying Rem around a user-centered UI layer, connected capabilities, automations, and a memory model that is useful without exposing infrastructure as product.",
        ),
    )

    /**
     * The deterministic, local send boundary: a nonempty draft is "noted" for the session only, a
     * blank draft is a no-op. No generated reply, no attachment flow, no persistence.
     */
    fun composerFeedback(draft: String): String? =
        if (draft.isBlank()) null
        else "Noted in this prototype session. Rem doesn’t reply or change memory here."
}

private enum class MemoryRoute { Root, Summary }

/**
 * Memory destination entry point. Owns its scaffold/title and a nested root↔summary stack. [onBack]
 * leaves the destination (outer navigation is owned by the host).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMemoryScreen(onBack: () -> Unit) {
    val colors = RemColors.current
    var route by rememberSaveable { mutableStateOf(MemoryRoute.Root) }
    val title = if (route == MemoryRoute.Root) "Memory" else "Memory summary"
    val leave = { if (route == MemoryRoute.Summary) route = MemoryRoute.Root else onBack() }

    Scaffold(
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = leave, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (route) {
                MemoryRoute.Root -> MemoryRootContent(openSummary = { route = MemoryRoute.Summary })
                MemoryRoute.Summary -> MemorySummaryContent()
            }
        }
    }
}

@Composable
private fun MemoryRootContent(openSummary: () -> Unit) {
    var controls by remember { mutableStateOf(MemoryContent.toggleDefaults) }
    fun toggle(control: MemoryControl, on: Boolean) { controls = controls + (control to on) }

    Column(
        Modifier
            .testTag("settingsMemory")
            .semantics { contentDescription = PlaygroundMockData.hint }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        RemSection(header = "Memory controls", footer = MemoryContent.controlsFooter, settingsHeader = true) {
            toggleRow(MemoryControl.SearchAndReference, "Search and reference chats", "Use details from past chats", controls, ::toggle, divider = true)
            toggleRow(MemoryControl.GenerateMemory, "Generate memory", "Update the summary from chats", controls, ::toggle, divider = true)
            toggleRow(MemoryControl.SensitiveTopics, "Sensitive topics", "Allow sensitive details", controls, ::toggle)
        }
        RemSection(header = "Overview", footer = MemoryContent.overviewFooter, settingsHeader = true) {
            ListRow(
                modifier = Modifier.testTag("memory.summaryRow"),
                onClick = openSummary,
                leading = {},
                content = { ListRowLabel("Memory summary", "Generated from your conversations") },
                trailing = { DisclosureChevron() },
            )
        }
    }
}

@Composable
private fun toggleRow(
    control: MemoryControl,
    title: String,
    subtitle: String,
    controls: Map<MemoryControl, Boolean>,
    onToggle: (MemoryControl, Boolean) -> Unit,
    divider: Boolean = false,
) {
    val colors = RemColors.current
    val on = controls[control] == true
    ListRow(
        showsDivider = divider,
        leading = {},
        content = { ListRowLabel(title, subtitle) },
        trailing = {
            Switch(
                checked = on,
                onCheckedChange = { onToggle(control, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.systemGreen),
                modifier = Modifier.testTag("memory.toggle.${control.name}").semantics { contentDescription = title },
            )
        },
    )
}

@Composable
private fun MemorySummaryContent() {
    val colors = RemColors.current
    var draft by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.testTag("memorySummary").fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(MemoryContent.summaryMetadata, style = RemTypography.footnote, color = colors.labelSecondary)
            MemoryContent.summarySections.forEach { section ->
                Text(section.heading, style = RemTypography.body, color = colors.labelPrimary, modifier = Modifier.semantics { heading() })
                Text(section.body, style = RemTypography.body, color = colors.labelPrimary)
            }
            feedback?.let {
                Text(it, style = RemTypography.footnote, color = colors.labelSecondary, modifier = Modifier.testTag("memory.composerFeedback"))
            }
        }
        MemoryComposer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                feedback = MemoryContent.composerFeedback(draft)
                draft = ""
            },
        )
    }
}

/**
 * The Memory-specific interactive composer called for by the Settings source contract — plus + send
 * only, no model/Speak slots (the shared [com.rem.designsystem.chat.RemComposerBar] is a static
 * presentational variant that includes those). Runs the deterministic local send boundary.
 */
@Composable
private fun MemoryComposer(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    val colors = RemColors.current
    val canSend = draft.isNotBlank()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .background(colors.backgroundSecondary, RoundedCornerShape(30.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            if (draft.isEmpty()) {
                Text(MemoryContent.composerPlaceholder, style = RemTypography.chatMessage, color = colors.labelTertiary)
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = RemTypography.chatMessage.copy(color = colors.labelPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brandBlue),
                modifier = Modifier.fillMaxWidth().testTag("memory.composerField"),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f))
            Box(
                Modifier
                    .size(32.dp)
                    .background(if (canSend) colors.brandBlue else colors.fillTertiary, CircleShape)
                    .then(if (canSend) Modifier.clickableSend(onSend) else Modifier)
                    .testTag("memory.composerSend")
                    .semantics { contentDescription = "Send" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = if (canSend) colors.labelOnColor else colors.labelSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun Modifier.clickableSend(onSend: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onSend))
