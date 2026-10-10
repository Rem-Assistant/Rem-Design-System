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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
        val Fire = MessageReaction("🔥", "Fire")
        val Eyes = MessageReaction("👀", "Eyes")
        val Thanks = MessageReaction("🙏", "Thanks")
        val Crying = MessageReaction("😢", "Crying")
        val Hundred = MessageReaction("💯", "Hundred")

        val StandardChoices: List<MessageReaction> = listOf(ThumbsUp, ThumbsDown, Heart, Laugh, Party, Surprised)

        /** The long-press sheet's 2 × 6 grid: the standard row, then five more before the `+` cell. */
        val SheetChoices: List<MessageReaction> = StandardChoices + listOf(Fire, Eyes, Thanks, Crying, Hundred)
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
 *
 * With [columns] the choices wrap into rows of that many cells, 12dp apart — the sheet's 2 × 6 grid.
 * [onMore] appends the trailing `+` cell (brand-blue glyph) that asks the host for its full picker;
 * without it no `+` is drawn.
 */
@Composable
fun MessageReactionPicker(
    selection: MessageReaction?,
    onSelect: (MessageReaction?) -> Unit,
    modifier: Modifier = Modifier,
    choices: List<MessageReaction> = MessageReaction.StandardChoices,
    columns: Int? = null,
    accessibilityPrefix: String = "reactions",
    onMore: (() -> Unit)? = null,
) {
    // A cell is a reaction's index in [choices], or -1 for the `+` cell.
    val cells = choices.indices.toList() + if (onMore != null) listOf(MoreCell) else emptyList()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MessageReactionMetrics.GridRowGap),
    ) {
        cells.chunked((columns ?: cells.size).coerceAtLeast(1)).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { index ->
                    if (index == MoreCell) MoreReactionsCell("$accessibilityPrefix.more") { onMore?.invoke() }
                    else ReactionCell(choices[index], choices[index] == selection, "$accessibilityPrefix.$index", onSelect)
                }
            }
        }
    }
}

private const val MoreCell = -1

@Composable
private fun ReactionCell(choice: MessageReaction, selected: Boolean, tag: String, onSelect: (MessageReaction?) -> Unit) {
    Box(
        modifier = Modifier
            .size(MessageReactionMetrics.PickerTarget)
            .background(RemColors.current.fillTertiary, CircleShape)
            .clickable(role = Role.Button) { onSelect(if (selected) null else choice) }
            .semantics { contentDescription = choice.name; this.selected = selected }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(choice.emoji, style = TextStyle(fontSize = 27.sp, lineHeight = 30.sp))
    }
}

@Composable
private fun MoreReactionsCell(tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(MessageReactionMetrics.PickerTarget)
            .background(RemColors.current.fillTertiary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "More reactions" }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = RemColors.current.brandBlue, modifier = Modifier.size(20.dp))
    }
}

internal object MessageReactionMetrics {
    val Badge = 28.dp
    val PickerTarget = 44.dp
    /** Vertical gap between grid rows in the long-press sheet. */
    val GridRowGap = RemSpacing.md
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
            MessageReactionPicker(
                selection = MessageReaction.Heart, onSelect = {}, choices = MessageReaction.SheetChoices, columns = 6, onMore = {},
            )
        }
    }
}
