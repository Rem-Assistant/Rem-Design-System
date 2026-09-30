package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.primitives.RemPill
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **TaskDetailScreen** — Compose sibling of the SwiftUI [TaskDetailScreen]. The task / event detail
 * surface: a scrolling body (title · date · meta, then the host-supplied sections — the "Last activity"
 * log, notes, etc.) with a [RemComposerBar] pinned at the bottom as the comment / reply composer. The
 * host supplies the body through [content] and drives the composer through the composer parameters.
 *
 * Chrome comes from the **platform**: the detail is hosted under an app bar (back + "Task" title) drawn
 * by the OS scaffold, so this template never hand-draws a nav bar (a platform-native control), matching
 * [ChatScreen], [AgendaScreen], and [InboxScreen]. Authority: Task detail screen (Figma `299:2`, page
 * "Task & Events") + the shipping `TaskCommentsThread` / `TaskCommentComposer`
 * (`Shared/Views/Tasks/TaskCommentsSection.swift`).
 */
@Composable
fun TaskDetailScreen(
    title: String,
    modifier: Modifier = Modifier,
    dateText: String? = null,
    metaPills: List<String> = emptyList(),
    composerText: String = "",
    composerPlaceholder: String = "Reply or ask Rem…",
    composerState: ComposerSendState = ComposerSendState.Idle,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            Header(title = title, dateText = dateText, metaPills = metaPills)
            content()
        }
        RemComposerBar(
            modifier = Modifier
                .padding(horizontal = RemSpacing.lg)
                .padding(bottom = RemSpacing.md),
            text = composerText,
            placeholder = composerPlaceholder,
            state = composerState,
        )
    }
}

/** Title · date · meta. The meta line pairs the date with any list / folder membership as [RemPill]s. */
@Composable
private fun Header(title: String, dateText: String?, metaPills: List<String>) {
    val colors = RemColors.current
    Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
        Text(text = title, style = RemTypography.title1Bold, color = colors.labelPrimary)
        if (dateText != null || metaPills.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
            ) {
                if (dateText != null) {
                    Text(text = dateText, style = RemTypography.subheadline, color = colors.labelSecondary)
                }
                metaPills.forEach { RemPill(text = it) }
            }
        }
    }
}

// MARK: - Preview

/**
 * Preview body — composes the detail's two host sections from canonical pieces: a plain titled
 * "Last activity" group (matching the shipping `TaskCommentsThread`, which sits on the primary
 * background, not a card) and a grouped [RemSection] "Notes" card (single row → no divider).
 */
@Composable
private fun TaskDetailPreviewBody() {
    val colors = RemColors.current
    Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.lg)) {
        // Last activity
        Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
            Text(
                text = "LAST ACTIVITY",
                style = RemTypography.caption1Bold,
                color = colors.labelSecondary,
            )
            // Activity row: Rem's face-mark avatar · author · body · timestamp + Reply.
            Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.brandBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    RemFaceMark(tint = colors.brandBlue, size = 17.dp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
                ) {
                    Text(text = "Rem", style = RemTypography.caption1Bold, color = colors.labelPrimary)
                    Text(
                        text = "Drafted the investor update and pulled last quarter's metrics — want me to send it?",
                        style = RemTypography.body,
                        color = colors.labelPrimary,
                    )
                    Row(
                        modifier = Modifier.padding(top = RemSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
                    ) {
                        Text(text = "2h ago", style = RemTypography.caption1, color = colors.labelSecondary)
                        Text(text = "Reply", style = RemTypography.caption1Bold, color = colors.brandBlue)
                    }
                }
            }
            // Run now (left) · View history (right) — understated labels, no filled container.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(text = "Run now", style = RemTypography.caption1Bold, color = colors.labelSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "View history", style = RemTypography.caption1Bold, color = colors.labelSecondary)
            }
        }
        // Notes — grouped card, single row → no divider.
        RemSection(header = "Notes") {
            Text(
                text = "Include the updated runway chart and the two hires we closed. Keep it to one screen.",
                style = RemTypography.body,
                color = colors.labelPrimary,
                modifier = Modifier.fillMaxWidth().padding(RemSpacing.md),
            )
        }
    }
}

@Preview(name = "TaskDetailScreen", showBackground = true, widthDp = 402, heightDp = 760)
@Composable
private fun TaskDetailScreenPreview() {
    RemTheme {
        TaskDetailScreen(
            title = "Draft the investor update",
            dateText = "Oct 1 2026",
            metaPills = listOf("Fundraise"),
            composerState = ComposerSendState.Idle,
        ) {
            TaskDetailPreviewBody()
        }
    }
}
