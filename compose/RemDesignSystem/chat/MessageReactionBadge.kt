package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * One reaction a person can attach to a chat message — Compose sibling of the SwiftUI
 * `MessageReaction`. Reactions are data: [StandardChoices] is the approved six-choice set (first row of
 * the Figma long-press sheet `2603:19498`), but hosts may supply their own. No persistence is implied.
 */
data class MessageReaction(val emoji: String, val name: String) {
    companion object {
        val ThumbsUp = MessageReaction("👍", "Thumbs up")
        val ThumbsDown = MessageReaction("👎", "Thumbs down")
        val Heart = MessageReaction("❤️", "Heart")
        val Laugh = MessageReaction("😂", "Laugh")
        val Party = MessageReaction("🎉", "Party")
        val Surprised = MessageReaction("😮", "Surprised")

        val StandardChoices: List<MessageReaction> = listOf(ThumbsUp, ThumbsDown, Heart, Laugh, Party, Surprised)
    }
}

/**
 * **MessageReactionBadge** — Compose sibling of the SwiftUI [MessageReactionBadge]: a 28dp circle in the
 * secondary-pill fill (`fillTertiary`) holding a 20sp emoji. Figma: **Rem/Chat/Reaction badge**
 * (`2654:20860`). [MessageBubble] anchors it toward the conversation centre with a 14dp overlap.
 */
@Composable
fun MessageReactionBadge(reaction: MessageReaction, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MessageReactionMetrics.Badge)
            .background(RemColors.current.fillTertiary, CircleShape)
            .clearAndSetSemantics { contentDescription = "Reaction: ${reaction.name}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(reaction.emoji, style = TextStyle(fontSize = 20.sp, lineHeight = 22.sp))
    }
}

/**
 * The approved long-press reaction row: 44dp secondary-pill circles with a 27sp emoji, spread edge to
 * edge. Choosing the current reaction again clears it (`onSelect(null)`). The host owns the selection.
 */
@Composable
fun MessageReactionPicker(
    selection: MessageReaction?,
    onSelect: (MessageReaction?) -> Unit,
    modifier: Modifier = Modifier,
    choices: List<MessageReaction> = MessageReaction.StandardChoices,
    accessibilityPrefix: String = "reactions",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choices.forEachIndexed { index, choice ->
            val selected = choice == selection
            Box(
                modifier = Modifier
                    .size(MessageReactionMetrics.PickerTarget)
                    .background(RemColors.current.fillTertiary, CircleShape)
                    .clickable(role = Role.Button) { onSelect(if (selected) null else choice) }
                    .semantics { contentDescription = choice.name; this.selected = selected }
                    .testTag("$accessibilityPrefix.$index"),
                contentAlignment = Alignment.Center,
            ) {
                Text(choice.emoji, style = TextStyle(fontSize = 27.sp, lineHeight = 30.sp))
            }
        }
    }
}

internal object MessageReactionMetrics {
    val Badge = 28.dp
    val PickerTarget = 44.dp
}

@Preview(name = "MessageReactionBadge", showBackground = true)
@Composable
private fun MessageReactionBadgePreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            MessageReactionBadge(MessageReaction.Heart)
            MessageReactionPicker(selection = MessageReaction.Heart, onSelect = {})
        }
    }
}
