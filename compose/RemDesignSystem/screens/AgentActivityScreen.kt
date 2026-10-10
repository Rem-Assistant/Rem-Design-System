package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.primitives.RemContentUnavailableView
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.RemSectionStyle
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **AgentActivityScreen** — Compose sibling of the SwiftUI `AgentActivityScreen`: agent detail reached
 * from the agent identity in Chat. The Rem face with the agent's **current** activity, an Activity /
 * Approvals Material 3 single-choice segmented button row, and the day-grouped Activity timeline.
 *
 * Figma master: **Rem/Chat/Agent activity** (`2002:76914`). The screen owns its top app bar (title
 * "Agent activity", Back to Chat); [onBack] returns to Chat. As with [ChatScreen], the host routes
 * system Back (the Playground registers a `BackHandler` with the same effect). The current state
 * comes only from [AgentActivityDisplay.current]; timeline rows never change it. Rows have no verified
 * destination, so they are not clickable, and Approvals shows a labeled data gap rather than invented
 * approvals. Kept distinct from agent settings, card outcomes and voice state.
 *
 * Composed from [RemFaceMark], [RemSection] (Plain, Settings header), the [ListRow] content slot +
 * [ListRowLabel] and [RemContentUnavailableView]. Inputs: `AgentActivityModel.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentActivityScreen(
    display: AgentActivityDisplay,
    selectedTab: AgentActivityTab,
    onSelectTab: (AgentActivityTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Scaffold(
        modifier = modifier,
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        AgentActivityDisplay.Title, style = RemTypography.bodyBold,
                        modifier = Modifier.testTag("agentActivity.title").semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("agentActivity.back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to ${AgentActivityDisplay.BackTitle}")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            Identity(display.current)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("agentActivity.tabs")) {
                AgentActivityTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = tab == selectedTab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AgentActivityTab.entries.size),
                        modifier = Modifier.testTag("agentActivity.tab.${tab.label}"),
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            when (selectedTab) {
                AgentActivityTab.Activity -> Timeline(display)
                AgentActivityTab.Approvals -> Unavailable(
                    "agentActivity.approvals.gap",
                    Icons.Outlined.VerifiedUser,
                    AgentActivityDisplay.ApprovalsGapTitle,
                    AgentActivityDisplay.ApprovalsGapMessage,
                )
            }
        }
    }
}

/** The current state only: face, name, status dot and the host's current activity. */
@Composable
private fun Identity(current: AgentActivityCurrent) {
    val colors = RemColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = RemSpacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "${current.name}, ${current.activity}" }
            .testTag("agentActivity.identity"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Box(
            Modifier.size(96.dp).background(colors.backgroundSecondary, CircleShape).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            RemFaceMark(mode = if (current.isWorking) RemFaceMarkMode.Thinking else RemFaceMarkMode.Idle, tint = colors.brandBlue, size = 72.dp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
            // Title 2 / Emphasized (22/28): no title2 token exists, so it extends the title3Bold token.
            Text(current.name, style = RemTypography.title3Bold.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.35.sp), color = colors.labelPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(6.dp).background(
                        if (current.status == ChatHeaderStatus.Connected) colors.systemGreen else colors.systemOrange,
                        CircleShape,
                    ),
                )
                Text(current.activity, style = RemTypography.subheadline, color = colors.labelPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Timeline(display: AgentActivityDisplay) {
    val days = display.visibleDays
    if (days.isEmpty()) {
        Unavailable("agentActivity.empty", Icons.Outlined.Schedule, AgentActivityDisplay.ActivityEmptyTitle, AgentActivityDisplay.ActivityEmptyMessage)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.lg)) {
        days.forEach { day ->
            RemSection(
                modifier = Modifier.testTag("agentActivity.day.${day.id}"),
                header = day.title,
                style = RemSectionStyle.Plain,
                settingsHeader = true,
            ) {
                day.events.forEach { event -> EventRow(event) }
            }
        }
    }
}

/** A timeline row: not clickable — no verified destination exists. */
@Composable
private fun EventRow(event: AgentActivityEvent) {
    val colors = RemColors.current
    ListRow(
        modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("agentActivity.event.${event.id}"),
        leading = {
            Box(
                Modifier.size(29.dp).background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.medium)).clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                RemFaceMark(tint = colors.brandBlue, size = 18.dp)
            }
        },
        content = { ListRowLabel(event.title, event.summary) },
        trailing = {},
    )
}

@Composable
private fun Unavailable(tag: String, icon: ImageVector, title: String, message: String) {
    // RemContentUnavailableView fills its parent; bound it inside the scrolling column.
    Box(
        Modifier.fillMaxWidth().heightIn(min = 240.dp).semantics(mergeDescendants = true) {}.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        RemContentUnavailableView(icon = icon, title = title, message = message)
    }
}
