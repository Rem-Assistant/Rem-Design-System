package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageReactionPicker
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **MessageActionSheet** — Compose sibling of the SwiftUI `MessageActionSheet`: the long-press message
 * sheet, Figma composition `2603:19498` (an assistant message) — the 2 × 6 reaction grid, then the
 * grouped actions [Reply, Mark as unread], [Copy, Select Text], [Report]. What is shown comes from
 * [ChatMessageActionsDisplay], so the person's own messages show only their applicable rows (never
 * Report).
 *
 * Content only, composed from [MessageReactionPicker], [RemSection] and [ListRow]: the host wraps it in
 * a `ModalBottomSheet` (grabber and scrim dimming the chat), handles every [ChatTranscriptAction] it
 * emits, and dismisses the sheet afterwards. Tags default to `message.<id>.actions…`.
 */
@Composable
fun MessageActionSheet(
    display: ChatMessageActionsDisplay,
    onAction: (ChatTranscriptAction) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "message.${display.messageId}.actions",
) {
    val id = display.messageId
    Column(
        // The sheet's drag handle sits above; the reference keeps the grid clear of it.
        modifier = modifier.fillMaxWidth().padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.sm, bottom = RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        MessageReactionPicker(
            selection = display.selection,
            onSelect = { onAction(ChatTranscriptAction.React(id, it)) },
            choices = display.reactions,
            columns = MessageActionSheetMetrics.ReactionColumns,
            accessibilityPrefix = "$accessibilityPrefix.reactions",
            onMore = if (display.showsMoreReactions) ({ onAction(ChatTranscriptAction.RequestMoreReactions(id)) }) else null,
        )
        display.groups.forEach { group ->
            RemSection {
                group.forEachIndexed { index, action ->
                    if (index > 0) {
                        // "Divider · Content inset only": aligned with the row titles, clear of the trailing edge.
                        HorizontalDivider(
                            Modifier.padding(start = MessageActionSheetMetrics.DividerInset, end = RemSpacing.lg),
                            thickness = 0.5.dp,
                            color = RemColors.current.separator,
                        )
                    }
                    ListRow(
                        modifier = Modifier
                            .semantics { role = Role.Button }
                            .testTag("$accessibilityPrefix.${action.key}"),
                        onClick = { onAction(ChatTranscriptAction.MessageAction(id, action)) },
                        leading = {
                            Box(Modifier.size(MessageActionSheetMetrics.Leading), contentAlignment = Alignment.Center) {
                                Icon(action.icon, contentDescription = null, tint = RemColors.current.labelPrimary, modifier = Modifier.size(22.dp))
                            }
                        },
                        content = { ListRowLabel(action.title) },
                        trailing = {},
                    )
                }
            }
        }
    }
}

/** Material twin of each row's SF Symbol (open rows in `docs/contracts/icon-registry.md`). */
private val ChatMessageAction.icon: ImageVector
    get() = when (this) {
        ChatMessageAction.Reply -> Icons.AutoMirrored.Outlined.Reply
        ChatMessageAction.MarkUnread -> Icons.Outlined.ChatBubbleOutline
        ChatMessageAction.Copy -> Icons.Outlined.ContentCopy
        ChatMessageAction.SelectText -> Icons.Outlined.SelectAll
        ChatMessageAction.Report -> Icons.Filled.Flag
    }

internal object MessageActionSheetMetrics {
    /** The reference grid is 2 × 6: eleven reactions and the `+` cell. */
    const val ReactionColumns = 6
    /** Leading symbol frame from the reference rows. */
    val Leading = 29.dp
    val DividerInset = 56.dp
}

@Preview(name = "MessageActionSheet · assistant", showBackground = true)
@Composable
private fun MessageActionSheetAssistantPreview() {
    RemTheme {
        MessageActionSheet(
            ChatMessageActionsDisplay(ChatMessageDisplay("a1", MessageRole.Assistant, "Here’s a clearer introduction.")),
            onAction = {},
            modifier = Modifier.background(RemColors.current.backgroundPrimary),
        )
    }
}

@Preview(name = "MessageActionSheet · own", showBackground = true)
@Composable
private fun MessageActionSheetOwnPreview() {
    RemTheme {
        MessageActionSheet(
            ChatMessageActionsDisplay(ChatMessageDisplay("u1", MessageRole.User, "Can you review it?", reaction = MessageReaction.Heart)),
            onAction = {},
            modifier = Modifier.background(RemColors.current.backgroundPrimary),
        )
    }
}
