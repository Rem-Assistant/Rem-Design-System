package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
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
 * **MessageReactionBadge** — Compose sibling of the SwiftUI [MessageReactionBadge]: one tapback circle,
 * mirroring the iMessage tapback reference Samuel shared (2026-10-11). A 34dp **solid**
 * `backgroundSecondary` disc with a 19sp emoji, inside a 2dp `backgroundPrimary` ring that cuts it out of
 * whatever it overlaps; both fills are opaque. Figma: **Rem/Chat/Reaction badge** (`2654:20860`).
 * [MessageBubble] places a [MessageReactionStack] (badge, thought-bubble tail, stacked badges) instead.
 */
@Composable
fun MessageReactionBadge(reaction: MessageReaction, modifier: Modifier = Modifier) {
    ReactionDisc(
        MessageReactionBadgeMetrics.Badge,
        modifier.clearAndSetSemantics { contentDescription = MessageReactionBadgeMetrics.accessibilityLabel(listOf(reaction)) },
    ) { ReactionEmoji(reaction) }
}

@Composable
private fun ReactionEmoji(reaction: MessageReaction) {
    Text(reaction.emoji, style = TextStyle(fontSize = MessageReactionBadgeMetrics.Emoji.sp, lineHeight = 22.sp))
}

/** A solid disc of [diameter] dp inside the opaque page-background ring; its size is the ring's outer edge. */
@Composable
private fun ReactionDisc(diameter: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val colors = RemColors.current
    Box(
        modifier = modifier
            .size((diameter + 2 * MessageReactionBadgeMetrics.Ring).dp)
            .background(colors.backgroundPrimary, CircleShape)
            .padding(MessageReactionBadgeMetrics.Ring.dp)
            .background(colors.backgroundSecondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The reaction set on a message — twin of SwiftUI `MessageReactionStack`: the front badge at the
 * bubble's corner with its two-dot tail, up to two more badges stacked behind it toward the bubble's
 * interior. One semantics node for the whole set. Its own size is [MessageReactionBadgeMetrics.layout];
 * the caller places it at the corner.
 */
@Composable
internal fun MessageReactionStack(
    reactions: List<MessageReaction>,
    corner: MessageReactionBadgeMetrics.Corner,
    modifier: Modifier = Modifier,
) {
    val layout = MessageReactionBadgeMetrics.layout(reactions.size, corner)
    Box(
        modifier = modifier
            .size(layout.width.dp, layout.height.dp)
            .clearAndSetSemantics { contentDescription = MessageReactionBadgeMetrics.accessibilityLabel(reactions) },
    ) {
        layout.dots.forEach { dot ->
            ReactionDisc(dot.size - 2 * MessageReactionBadgeMetrics.Ring, Modifier.offset(dot.x.dp, dot.y.dp))
        }
        // Back to front: the corner badge (index 0) is drawn last, on top.
        reactions.take(MessageReactionBadgeMetrics.MaxVisible).withIndex().reversed().forEach { (index, reaction) ->
            val frame = layout.badges[index]
            ReactionDisc(MessageReactionBadgeMetrics.Badge, Modifier.offset(frame.x.dp, frame.y.dp)) { ReactionEmoji(reaction) }
        }
    }
}

/**
 * Tapback geometry (dp), identical to the SwiftUI `MessageReactionBadgeMetrics` and asserted by
 * `ChatGeometryTest` / `ChatGeometryTests`. Frames are in the stack's own coordinates (origin top-left);
 * [Layout.offsetX] / [Layout.offsetY] move that origin from the bubble's anchoring top corner.
 */
object MessageReactionBadgeMetrics {
    /** Which bubble corner the set anchors to: outgoing → top-start, incoming → top-end. */
    enum class Corner {
        TopLeading, TopTrailing;

        companion object {
            fun of(role: MessageRole): Corner = if (role == MessageRole.User) TopLeading else TopTrailing
        }
    }

    /** A circle's outer (ring) frame: top-left and diameter. */
    data class Frame(val x: Float, val y: Float, val size: Float) {
        val maxX: Float get() = x + size
        val maxY: Float get() = y + size
    }

    data class Layout(
        val width: Float,
        val height: Float,
        /** Front badge first. */
        val badges: List<Frame>,
        /** Large then small tail dot; empty when there is no reaction. */
        val dots: List<Frame>,
        val offsetX: Float,
        val offsetY: Float,
    )

    const val Badge = 34f
    const val Ring = 2f
    const val BadgeOuter = Badge + 2 * Ring
    const val Emoji = 19f
    /** How far the badge disc extends outward past the bubble's side edge (20 of its 34 overlap). */
    const val Outward = MessageBubbleGeometry.ReactionOverlap
    /** How far the badge disc rises above the bubble's top edge (14 of its 34 overlap). */
    const val Lift = 20f
    /** Top clearance a reacted message reserves so the set never reaches the row above. */
    const val TopReserve = Lift + Ring
    /** 60% of a badge: each stacked badge overlaps the previous one. */
    const val StackStep = 20f
    const val MaxVisible = 3
    const val TailLarge = 8f
    const val TailSmall = 4f
    /** Dot centres in the front badge disc's coordinates (outgoing orientation; mirrored for incoming). */
    const val TailLargeCenterX = 8f
    const val TailLargeCenterY = 34f
    const val TailSmallCenterX = 3f
    const val TailSmallCenterY = 41f

    fun layout(count: Int, corner: Corner): Layout {
        val visible = count.coerceIn(0, MaxVisible)
        if (visible == 0) return Layout(0f, 0f, emptyList(), emptyList(), 0f, 0f)
        fun dot(cx: Float, cy: Float, diameter: Float): Frame {
            val outer = diameter + 2 * Ring
            return Frame(cx + Ring - outer / 2, cy + Ring - outer / 2, outer)
        }
        val badges = (0 until visible).map { Frame(it * StackStep, 0f, BadgeOuter) }
        val dots = listOf(dot(TailLargeCenterX, TailLargeCenterY, TailLarge), dot(TailSmallCenterX, TailSmallCenterY, TailSmall))
        val width = BadgeOuter + (visible - 1) * StackStep
        val height = (badges + dots).maxOf { it.maxY }
        val reach = Outward + Ring
        return when (corner) {
            Corner.TopLeading -> Layout(width, height, badges, dots, -reach, -TopReserve)
            Corner.TopTrailing -> {
                fun mirror(f: Frame) = f.copy(x = width - f.maxX)
                Layout(width, height, badges.map(::mirror), dots.map(::mirror), reach, -TopReserve)
            }
        }
    }

    /** "Reaction: Heart" for one reaction; "Reactions: Thumbs up, Heart" for several. */
    fun accessibilityLabel(reactions: List<MessageReaction>): String =
        if (reactions.size == 1) "Reaction: ${reactions[0].name}"
        else "Reactions: ${reactions.joinToString(", ") { it.name }}"

    /** The set a message shows: its own reaction (the picker selection) in front, then any others. */
    fun stack(primary: MessageReaction?, additional: List<MessageReaction>): List<MessageReaction> =
        listOfNotNull(primary) + additional
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
            MessageBubble(
                "Sounds good", role = MessageRole.User,
                reaction = MessageReaction.ThumbsUp, additionalReactions = listOf(MessageReaction.ThumbsUp),
            )
            MessageReactionPicker(selection = MessageReaction.Heart, onSelect = {})
            MessageReactionPicker(
                selection = MessageReaction.Heart, onSelect = {}, choices = MessageReaction.SheetChoices, columns = 6, onMore = {},
            )
        }
    }
}
