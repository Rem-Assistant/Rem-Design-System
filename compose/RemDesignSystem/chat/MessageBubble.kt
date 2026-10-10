package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography
import kotlin.math.max
import kotlin.math.min

/**
 * Who sent a [MessageBubble] — drives alignment, fill, and text treatment.
 */
enum class MessageRole { User, Assistant }

/**
 * Delivery state of an outgoing [MessageBubble]; ignored for [MessageRole.Assistant]. `at` is the
 * host-formatted delivery time. [Read] keeps the same delivery time and must only follow an explicit
 * acknowledgement. [Failed] shows no timestamp.
 */
sealed interface MessageDelivery {
    data object None : MessageDelivery
    data class Delivered(val at: String) : MessageDelivery
    data class Read(val at: String) : MessageDelivery
    data object Failed : MessageDelivery
}

/**
 * **MessageBubble** — Compose sibling of the SwiftUI [MessageBubble]. A single chat message in one of
 * two roles, mirroring the Figma **MessageBubble** set (`50:7`), the outgoing master (`2000:3605`) and
 * the shipped `ChatMessageViews.swift`:
 *
 * - [MessageRole.User] (outgoing) — a brandBlue rounded bubble, trailing-aligned, labelOnColor text.
 * - [MessageRole.Assistant] (incoming) — unboxed prose, leading-aligned.
 *
 * Geometry is responsive ([MessageBubbleGeometry]): an outgoing bubble hugs its text up to 320dp and
 * fills when the row is narrower; a failed row reserves 52dp on the right for the outside failure
 * control. The optional [reaction] sits at the upper corner toward the conversation centre (outgoing
 * upper-left, incoming upper-right) with a 14dp overlap. A failed message shows an outlined error icon
 * outside the bubble (opening a Try again menu) and "Not delivered" right-aligned beneath, no time.
 *
 * @param meta Optional metadata (e.g. a timestamp) beneath the message, in chatMeta.
 * @param onLongPress Long press (and the accessibility "React" action) — hosts open [MessageReactionPicker].
 */
@Composable
fun MessageBubble(
    text: String,
    role: MessageRole,
    modifier: Modifier = Modifier,
    meta: String? = null,
    delivery: MessageDelivery = MessageDelivery.None,
    reaction: MessageReaction? = null,
    accessibilityPrefix: String = "message",
    onRetry: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    when (role) {
        MessageRole.User -> OutgoingMessage(text, modifier, meta, delivery, reaction, accessibilityPrefix, onRetry, onLongPress)
        MessageRole.Assistant -> IncomingMessage(text, modifier, meta, reaction, accessibilityPrefix, onLongPress)
    }
}

@Composable
private fun OutgoingMessage(
    text: String,
    modifier: Modifier,
    meta: String?,
    delivery: MessageDelivery,
    reaction: MessageReaction?,
    prefix: String,
    onRetry: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
) {
    val colors = RemColors.current
    val failed = delivery == MessageDelivery.Failed
    val reserve = if (failed) MessageBubbleGeometry.FailureReserve.dp else 0.dp
    val receipt = when (delivery) {
        MessageDelivery.None -> null
        is MessageDelivery.Delivered -> "Delivered · ${delivery.at}"
        is MessageDelivery.Read -> "Read · ${delivery.at}"
        MessageDelivery.Failed -> "Not delivered"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (reaction == null) 0.dp else MessageBubbleGeometry.ReactionOverlap.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(
            if (failed) MessageBubbleGeometry.FailureLabelGap.dp else MessageBubbleGeometry.ReceiptGap.dp,
        ),
    ) {
        OutgoingRow(
            failed = failed,
            bubble = {
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.brandBlue, RoundedCornerShape(MessageBubbleGeometry.CornerRadius.dp))
                            .longPress(onLongPress)
                            .testTag("$prefix.bubble")
                            .padding(
                                horizontal = MessageBubbleGeometry.ContentInsetHorizontal.dp,
                                vertical = MessageBubbleGeometry.ContentInsetVertical.dp,
                            ),
                    ) {
                        Text(text = text, style = RemTypography.chatMessage, color = colors.labelOnColor)
                    }
                    if (reaction != null) {
                        MessageReactionBadge(
                            reaction,
                            Modifier
                                .align(Alignment.TopStart)
                                .offset(x = -MessageBubbleGeometry.ReactionOverlap.dp, y = -MessageBubbleGeometry.ReactionOverlap.dp)
                                .testTag("$prefix.reaction"),
                        )
                    }
                }
            },
            failure = { FailureControl(prefix, onRetry) },
        )
        if (receipt != null) {
            Text(
                text = receipt,
                style = RemTypography.chatMeta.copy(fontWeight = FontWeight.SemiBold),
                color = if (failed) colors.systemRed else colors.labelSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(end = reserve).testTag("$prefix.receipt"),
            )
        }
        if (meta != null) {
            Text(
                text = meta, style = RemTypography.chatMeta, color = colors.labelSecondary, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(end = reserve),
            )
        }
    }
}

/** Places the bubble (and, when failed, the outside failure control) using [MessageBubbleGeometry]. */
@Composable
private fun OutgoingRow(failed: Boolean, bubble: @Composable () -> Unit, failure: @Composable () -> Unit) {
    Layout(
        content = {
            bubble()
            if (failed) failure()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val bubbleMeasurable = measurables.first()
        val idealDp = bubbleMeasurable.maxIntrinsicWidth(Constraints.Infinity).toDp().value
        val reserveDp = if (failed) MessageBubbleGeometry.FailureReserve else 0f
        val availableDp = if (constraints.hasBoundedWidth) constraints.maxWidth.toDp().value
        else min(idealDp, MessageBubbleGeometry.MaxWidth) + reserveDp
        val widthPx = MessageBubbleGeometry.bubbleWidth(idealDp, availableDp, failed).dp.roundToPx()
        val bubblePlaceable = bubbleMeasurable.measure(Constraints.fixedWidth(widthPx))
        val side = MessageBubbleGeometry.FailureControl.dp.roundToPx()
        val failurePlaceable = if (failed) measurables.getOrNull(1)?.measure(Constraints.fixed(side, side)) else null
        val rowWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else availableDp.dp.roundToPx()
        val height = max(bubblePlaceable.height, failurePlaceable?.height ?: 0)
        layout(rowWidth, height) {
            val trailing = rowWidth - reserveDp.dp.roundToPx()
            bubblePlaceable.placeRelative(trailing - bubblePlaceable.width, height - bubblePlaceable.height)
            failurePlaceable?.placeRelative(rowWidth - failurePlaceable.width, height - failurePlaceable.height)
        }
    }
}

@Composable
private fun FailureControl(prefix: String, onRetry: (() -> Unit)?) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(MessageBubbleGeometry.FailureControl.dp)
            .then(
                if (onRetry != null) Modifier.clickable(role = Role.Button, onClickLabel = "Open Try again menu") { menuOpen = true }
                else Modifier,
            )
            .semantics { contentDescription = "Delivery failed" }
            .testTag("$prefix.failure"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = RemColors.current.systemRed, modifier = Modifier.size(22.dp))
        if (onRetry != null) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Try again") },
                    onClick = { menuOpen = false; onRetry() },
                    modifier = Modifier.testTag("$prefix.retry"),
                )
            }
        }
    }
}

@Composable
private fun IncomingMessage(
    text: String,
    modifier: Modifier,
    meta: String?,
    reaction: MessageReaction?,
    prefix: String,
    onLongPress: (() -> Unit)?,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (reaction == null) 0.dp else MessageBubbleGeometry.IncomingReactionTop.dp,
                end = MessageBubbleGeometry.IncomingTrailingReserve.dp,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Box(modifier = Modifier.widthIn(max = MessageBubbleGeometry.MaxWidth.dp).fillMaxWidth()) {
            Text(
                text = text,
                style = RemTypography.chatMessage,
                color = colors.labelPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .longPress(onLongPress)
                    .testTag("$prefix.bubble")
                    .padding(vertical = RemSpacing.xs),
            )
            if (reaction != null) {
                MessageReactionBadge(
                    reaction,
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = MessageBubbleGeometry.ReactionOverlap.dp, y = -MessageBubbleGeometry.IncomingReactionTop.dp)
                        .testTag("$prefix.reaction"),
                )
            }
        }
        if (meta != null) Text(text = meta, style = RemTypography.chatMeta, color = colors.labelSecondary)
    }
}

private fun Modifier.longPress(action: (() -> Unit)?): Modifier =
    if (action == null) this
    else this
        .pointerInput(action) { detectTapGestures(onLongPress = { action() }) }
        .semantics { onLongClick(label = "React") { action(); true } }

/**
 * Responsive message geometry in dp, shared with the SwiftUI `MessageBubbleGeometry` and the JVM tests.
 * No value here is a fixed bubble width: widths derive from the space the row actually has.
 */
object MessageBubbleGeometry {
    const val MaxWidth = 320f
    const val ContentInsetHorizontal = 16f
    const val ContentInsetVertical = 12f
    /** Figma outgoing master radius; no 20dp radius token exists. */
    const val CornerRadius = 20f
    const val FailureControl = 44f
    const val FailureGap = 8f
    /** Right-hand space a failed row reserves for the outside failure control: 44 + 8 = 52. */
    const val FailureReserve = FailureControl + FailureGap
    const val FailureLabelGap = 8f
    const val ReceiptGap = 4f
    const val ReactionOverlap = 14f
    const val IncomingReactionTop = 10f
    const val IncomingTrailingReserve = 30f

    /** The widest an outgoing bubble may be in a row [available] dp wide. */
    fun bubbleLimit(available: Float, failed: Boolean): Float =
        max(0f, min(MaxWidth, available - if (failed) FailureReserve else 0f))

    /** Hug the content's ideal width up to the limit; wider content fills the limit and wraps. */
    fun bubbleWidth(idealWidth: Float, available: Float, failed: Boolean): Float =
        min(idealWidth, bubbleLimit(available, failed))

    /** Text width inside an outgoing bubble of [bubbleWidth]. */
    fun textWidth(bubbleWidth: Float): Float = max(0f, bubbleWidth - 2 * ContentInsetHorizontal)

    /** Incoming text block width in a row [available] dp wide. */
    fun incomingWidth(available: Float): Float = max(0f, min(MaxWidth, available - IncomingTrailingReserve))
}

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
            MessageBubble("Can you move the planning sync to Thursday?", role = MessageRole.User, reaction = MessageReaction.Heart)
            MessageBubble(
                "Done — the planning sync is now Thursday at 10:00. I let the two attendees know.",
                role = MessageRole.Assistant,
                reaction = MessageReaction.ThumbsUp,
            )
            MessageBubble("Thanks, that works.", role = MessageRole.User, delivery = MessageDelivery.Read("10:24"))
            MessageBubble("Also share the agenda with the group.", role = MessageRole.User, delivery = MessageDelivery.Failed, onRetry = {})
        }
    }
}
