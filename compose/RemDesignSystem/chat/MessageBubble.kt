package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * Who sent a [MessageBubble] — drives alignment, fill, and text treatment.
 */
enum class MessageRole { User, Assistant }

/**
 * **MessageBubble** — Compose sibling of the SwiftUI [MessageBubble]. A single chat message in one of
 * two roles, mirroring the Figma **MessageBubble** component set (`50:7`) and the shipped
 * `ChatMessageViews.swift`:
 *
 * - [MessageRole.User] (sent) — a brandBlue rounded bubble, trailing-aligned, with labelOnColor
 *   (white) text: the iMessage "sent" treatment the Figma node draws.
 * - [MessageRole.Assistant] (received) — plain text on the surface, leading-aligned, no bubble; reads
 *   as prose/markdown with minimal chrome.
 *
 * Both roles use chatMessage (body, 17sp) and cap their width at [MessageBubbleMaxWidth] so a message
 * never spans the full transcript, leaving a gutter on the opposite edge. Optional [meta] (e.g. a
 * timestamp) sits beneath the message in chatMeta.
 *
 * @param text The message body. Plain text; the assistant role reads as prose.
 * @param role [MessageRole.User] (trailing bubble) or [MessageRole.Assistant] (leading plain text).
 * @param meta Optional metadata (e.g. a timestamp) shown beneath the message, aligned to its edge.
 *   Hidden when `null`, so the default matches the Figma node.
 */
@Composable
fun MessageBubble(
    text: String,
    role: MessageRole,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (role == MessageRole.User) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = MessageBubbleMaxWidth),
            horizontalAlignment = if (role == MessageRole.User) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            when (role) {
                MessageRole.User -> Text(
                    text = text,
                    style = RemTypography.chatMessage,
                    color = colors.labelOnColor,
                    modifier = Modifier
                        .background(colors.brandBlue, RoundedCornerShape(RemRadius.xlarge))
                        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
                )
                MessageRole.Assistant -> Text(
                    text = text,
                    style = RemTypography.chatMessage,
                    color = colors.labelPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RemSpacing.xs),
                )
            }
            if (meta != null) {
                Text(text = meta, style = RemTypography.chatMeta, color = colors.labelSecondary)
            }
        }
    }
}

/**
 * Caps a message so it never spans the full transcript width. A fixed cap (rather than a live
 * fraction of the parent) keeps bubbles readable across phone, tablet, and wide windows, where a
 * fraction would over-stretch. Matches the Figma bubble's max width band (~272dp user / ~330dp
 * assistant) and the SwiftUI `MessageBubbleMetrics.maxWidth`.
 */
private val MessageBubbleMaxWidth = 300.dp

@Preview(name = "MessageBubble", showBackground = true)
@Composable
private fun MessageBubblePreview() {
    RemTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RemColors.current.backgroundPrimary)
                .padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
            MessageBubble(
                "Done — I archived 38 newsletters and snoozed 5 low-priority threads on this Mac.\n\n" +
                    "Want me to draft quick replies to the two that still need you?",
                role = MessageRole.Assistant,
            )
            MessageBubble("Yes, go ahead.", role = MessageRole.User, meta = "9:41 AM")
        }
    }
}
