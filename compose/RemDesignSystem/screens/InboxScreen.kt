package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.rem.designsystem.agentsurfaces.AgentStatusPill
import com.rem.designsystem.agentsurfaces.AgentStatusTone
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **InboxScreen** — Compose sibling of the SwiftUI [InboxScreen]. A large "Inbox" title over the
 * unfiled [TaskEventRow]s (no time, pills hidden — not yet scheduled/filed), supplied by the host
 * through [content]. Chrome comes from the OS. Authority: Inbox screen (Figma page "Inbox") +
 * `SharedInboxView.swift`.
 */
@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    title: String = "Inbox",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Text(
            text = title,
            style = RemTypography.largeTitle.copy(fontWeight = FontWeight.Bold),
            color = colors.labelPrimary,
            modifier = Modifier.padding(horizontal = RemSpacing.lg).padding(top = RemSpacing.md, bottom = RemSpacing.sm),
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content()
        }
    }
}

/**
 * The host-driven Inbox — twin of SwiftUI `InboxScreen(items:onAction:empty:)`: rows from [items],
 * the host-reported [InboxItemState] as an [AgentStatusPill] (never inferred), [InboxAction.Open] on tap,
 * [empty] when there are none. Routing stays in the host. Row separators and
 * the empty composition are pending the canonical Inbox review.
 */
@Composable
fun InboxScreen(
    items: List<InboxItemDisplay>,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Inbox",
    empty: @Composable ColumnScope.() -> Unit,
) {
    InboxScreen(modifier = modifier, title = title) {
        if (items.isEmpty()) {
            empty()
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onAction(InboxAction.Open(item.id)) }
                        .testTag("inbox.item.${item.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TaskEventRow(kind = TaskEventKind.Task, title = item.title, leading = TaskEventLeading.None, showPills = false)
                    }
                    item.state.statusLabel?.let { status ->
                        AgentStatusPill(
                            status = status,
                            tone = if (item.state.needsPerson) AgentStatusTone.Attention else AgentStatusTone.Neutral,
                            modifier = Modifier.padding(end = RemSpacing.lg).testTag("inbox.item.${item.id}.status"),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "InboxScreen", showBackground = true, widthDp = 402, heightDp = 700)
@Composable
private fun InboxScreenPreview() {
    RemTheme {
        InboxScreen {
            TaskEventRow(kind = TaskEventKind.Task, title = "Follow up with the Freestyle team", leading = TaskEventLeading.None, showPills = false)
            TaskEventRow(kind = TaskEventKind.Task, title = "Review the Q4 roadmap draft", leading = TaskEventLeading.None, showPills = false)
            TaskEventRow(kind = TaskEventKind.Task, title = "Book the venue for the offsite", leading = TaskEventLeading.None, showPills = false)
        }
    }
}
