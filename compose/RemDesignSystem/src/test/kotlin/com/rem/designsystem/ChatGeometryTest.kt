package com.rem.designsystem

import com.rem.designsystem.chat.AddToChatMaxPhotoSelection
import com.rem.designsystem.chat.ChatModelOption
import com.rem.designsystem.chat.ChatModelProvider
import com.rem.designsystem.chat.ChatModelSelection
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageBubbleGeometry as G
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageReactionBadgeMetrics as T
import com.rem.designsystem.chat.MessageReactionBadgeMetrics.Corner
import com.rem.designsystem.chat.MessageReactionBadgeMetrics.Frame
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.screens.ChatMessageDisplay
import com.rem.designsystem.chat.ThinkingLevel
import com.rem.designsystem.chat.chatModelTriggerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Chat slice's pure contract — responsive bubble geometry, the model trigger label, the reaction set
 * and the Add to Chat limits. Paired with the SwiftUI `ChatGeometryTests.swift`; both assert the same
 * numbers so the two layouts cannot drift silently.
 */
class ChatGeometryTest {
    /** 320dp screen, 16dp transcript gutters: a 288dp row. Accepted Figma fixture (`2659:21942`). */
    private val narrowRow = 320f - 2 * 16f

    @Test fun failedRowAtNarrowWidthReservesFailureAffordanceAndFills() {
        assertEquals(52f, G.FailureReserve)
        val bubble = G.bubbleWidth(idealWidth = 900f, available = narrowRow, failed = true)
        assertEquals(236f, bubble)
        assertEquals(204f, G.textWidth(bubble))
    }

    @Test fun deliveredRowAtNarrowWidthFillsWholeRow() {
        assertEquals(288f, G.bubbleWidth(idealWidth = 900f, available = narrowRow, failed = false))
    }

    @Test fun shortMessageHugsItsText() {
        assertEquals(120f, G.bubbleWidth(idealWidth = 120f, available = 370f, failed = false))
        assertEquals(120f, G.bubbleWidth(idealWidth = 120f, available = narrowRow, failed = true))
    }

    @Test fun wideRowsCapAt320RatherThanStretching() {
        assertEquals(320f, G.bubbleWidth(idealWidth = 900f, available = 1000f, failed = false))
        assertEquals(320f, G.bubbleWidth(idealWidth = 900f, available = 1000f, failed = true))
        assertEquals(288f, G.textWidth(320f))
    }

    @Test fun geometryNeverGoesNegative() {
        assertEquals(0f, G.bubbleLimit(available = 30f, failed = true))
        assertEquals(0f, G.textWidth(10f))
        assertEquals(0f, G.incomingWidth(12f))
    }

    @Test fun incomingKeepsRoomForItsUpperRightReaction() {
        assertEquals(258f, G.incomingWidth(narrowRow))
        assertEquals(320f, G.incomingWidth(1000f))
        assertTrue(G.ReactionOverlap <= G.IncomingTrailingReserve)
    }

    // Tapback reactions (iMessage reference, 2026-10-11). Paired with ChatGeometryTests.swift.

    @Test fun tapbackConstantsMatchTheSwiftTwin() {
        assertEquals(34f, T.Badge)
        assertEquals(2f, T.Ring)
        assertEquals(38f, T.BadgeOuter)
        assertEquals(19f, T.Emoji)
        assertEquals(G.ReactionOverlap, T.Outward)
        assertEquals(20f, T.Lift)
        assertEquals(22f, T.TopReserve)
        assertEquals(T.TopReserve, G.IncomingReactionTop)
        assertEquals(30f, T.StackStep)
        assertEquals(2, T.MaxVisible)
    }

    @Test fun outgoingTapbackOverlapsTheTopLeadingCorner() {
        assertEquals(Corner.TopLeading, Corner.of(MessageRole.User))
        val l = T.layout(1, Corner.TopLeading)
        assertEquals(-16f, l.offsetX)
        assertEquals(-22f, l.offsetY)
        assertEquals(38f, l.width)
        assertEquals(47f, l.height)
        assertEquals(listOf(Frame(0f, 0f, 38f)), l.badges)
        // The badge disc in bubble coordinates: 20 of its 34 overlap horizontally, 14 vertically.
        val discLeft = l.offsetX + l.badges[0].x + T.Ring
        val discTop = l.offsetY + l.badges[0].y + T.Ring
        assertEquals(-14f, discLeft)
        assertEquals(-20f, discTop)
        assertEquals(20f, discLeft + T.Badge)
        assertEquals(14f, discTop + T.Badge)
    }

    @Test fun incomingTapbackMirrorsToTheTopTrailingCorner() {
        assertEquals(Corner.TopTrailing, Corner.of(MessageRole.Assistant))
        val out = T.layout(2, Corner.TopLeading)
        val inc = T.layout(2, Corner.TopTrailing)
        assertEquals(16f, inc.offsetX)
        assertEquals(out.offsetY, inc.offsetY)
        assertEquals(out.width, inc.width)
        // Front badge at the outer edge of the stack, the next one 30 further toward the bubble.
        assertEquals(30f, inc.badges[0].x)
        assertEquals(0f, inc.badges[1].x)
        out.badges.zip(inc.badges).forEach { (o, i) -> assertEquals(o.x, inc.width - i.maxX) }
        out.dots.zip(inc.dots).forEach { (o, i) -> assertEquals(o.x, inc.width - i.maxX); assertEquals(o.y, i.y) }
    }

    @Test fun tapbackTailIsTwoShrinkingDotsBelowOutwardOfTheFrontBadge() {
        val l = T.layout(1, Corner.TopLeading)
        assertEquals(listOf(Frame(4f, 30f, 12f), Frame(1f, 39f, 8f)), l.dots)
        val (large, small) = l.dots
        assertTrue("dots trail outward", small.x < large.x && large.x < l.badges[0].x + T.BadgeOuter / 2)
        assertTrue("dots trail downward", small.y > large.y && large.y > T.BadgeOuter / 2)
    }

    @Test fun stackedTapbacksOverlapShowBothEmojiAndCapAtTwo() {
        val two = T.layout(2, Corner.TopLeading)
        assertEquals(listOf(0f, 30f), two.badges.map { it.x })
        // The discs still overlap, so it reads as a stack...
        assertTrue("discs overlap", two.badges[0].maxX - T.Ring > two.badges[1].x + T.Ring)
        // ...but the front badge stops before the back badge's centred emoji begins.
        val backEmojiLeft = two.badges[1].x + (T.BadgeOuter - T.Emoji) / 2
        assertTrue("back emoji fully visible", two.badges[0].maxX <= backEmojiLeft)
        assertEquals(68f, two.width)
        // A chat has two participants: never more than two badges.
        val many = T.layout(5, Corner.TopLeading)
        assertEquals(2, many.badges.size)
        assertEquals(68f, many.width)
        val none = T.layout(0, Corner.TopTrailing)
        assertTrue(none.badges.isEmpty() && none.dots.isEmpty())
    }

    @Test fun tapbackStaysInsideItsReservedClearance() {
        for (corner in Corner.values()) for (count in 1..T.MaxVisible + 1) {
            val l = T.layout(count, corner)
            val frames = l.badges + l.dots
            // Never above the reserved top clearance, so it cannot reach the previous row.
            assertEquals(-T.TopReserve, l.offsetY + frames.minOf { it.y })
            // Never further outward than the incoming row's trailing reserve.
            val outward = if (corner == Corner.TopLeading) -(l.offsetX + frames.minOf { it.x })
            else l.offsetX - l.width + frames.maxOf { it.maxX }
            assertEquals(16f, outward)
            assertTrue(outward <= G.IncomingTrailingReserve)
        }
    }

    @Test fun tapbackAccessibilityNamesTheWholeSet() {
        assertEquals("Reaction: Heart", T.accessibilityLabel(listOf(MessageReaction.Heart)))
        assertEquals(
            "Reactions: Thumbs up, Heart",
            T.accessibilityLabel(listOf(MessageReaction.ThumbsUp, MessageReaction.Heart)),
        )
    }

    @Test fun displayReactionsPutTheOwnReactionInFrontAndDefaultToNoExtras() {
        val plain = ChatMessageDisplay(id = "m", role = MessageRole.User, text = "Hi", reaction = MessageReaction.Heart)
        assertEquals(emptyList<MessageReaction>(), plain.additionalReactions)
        assertEquals(listOf(MessageReaction.Heart), plain.reactions)
        val stacked = plain.copy(additionalReactions = listOf(MessageReaction.ThumbsUp))
        assertEquals(listOf(MessageReaction.Heart, MessageReaction.ThumbsUp), stacked.reactions)
        assertNotEquals(plain, stacked)
        assertEquals(listOf(MessageReaction.ThumbsUp), stacked.copy(reaction = null).reactions)
        assertEquals(listOf(MessageReaction.ThumbsUp, MessageReaction.ThumbsUp),
            T.stack(MessageReaction.ThumbsUp, listOf(MessageReaction.ThumbsUp)))
    }

    @Test fun modelTriggerReadsAutoForAutomaticAndTheModelNameOtherwise() {
        val providers = listOf(ChatModelProvider("p", "Provider", listOf(ChatModelOption("m1", "Model One"))))
        assertEquals("Auto", chatModelTriggerLabel(ChatModelSelection.Automatic, providers))
        assertEquals("Model One", chatModelTriggerLabel(ChatModelSelection.Model("m1"), providers))
        // An id the runtime catalog does not contain is shown as-is, never disguised as Automatic.
        assertEquals("gone", chatModelTriggerLabel(ChatModelSelection.Model("gone"), providers))
        assertEquals("Auto", chatModelTriggerLabel(ChatModelSelection.Automatic, emptyList()))
    }

    @Test fun modelTriggerPrefersTheHostLabelForAnUnlistedSelection() {
        val providers = listOf(ChatModelProvider("p", "Provider", listOf(ChatModelOption("m1", "Model One"))))
        val byok = ChatModelSelection.Model("anthropic/claude-sonnet-4-5")
        assertEquals("Claude Sonnet 4.5", chatModelTriggerLabel(byok, providers, fallbackLabel = "Claude Sonnet 4.5"))
        // A listed model keeps its catalog name; Automatic keeps the Automatic label.
        assertEquals("Model One", chatModelTriggerLabel(ChatModelSelection.Model("m1"), providers, fallbackLabel = "Other"))
        assertEquals("Auto", chatModelTriggerLabel(ChatModelSelection.Automatic, providers, fallbackLabel = "Other"))
        // Blank or Automatic-label fallbacks never disguise the selection: the raw id is shown.
        for (fallback in listOf(null, "", "  ", "Auto")) {
            assertEquals("$fallback", "anthropic/claude-sonnet-4-5", chatModelTriggerLabel(byok, providers, fallbackLabel = fallback))
        }
        assertEquals("Auto", chatModelTriggerLabel(byok, emptyList(), automaticLabel = "Automatic", fallbackLabel = "Auto"))
    }

    @Test fun standardReactionsAreTheApprovedSixInOrder() {
        assertEquals(
            listOf("👍", "👎", "❤️", "😂", "🎉", "😮"),
            MessageReaction.StandardChoices.map { it.emoji },
        )
        assertEquals(6, MessageReaction.StandardChoices.map { it.emoji }.toSet().size)
    }

    @Test fun addToChatLimitsAndThinkingLevels() {
        assertEquals(4, AddToChatMaxPhotoSelection)
        assertEquals(listOf("Off", "Low", "Medium", "High"), ThinkingLevel.entries.map { it.title })
    }

    @Test fun legacyAttachmentFlagShowsTheCloudBrowserCapabilityChip() {
        assertEquals(ComposerAttachment.Kind.Capability, ComposerAttachment.CloudBrowser.kind)
        assertEquals("Cloud browser", ComposerAttachment.CloudBrowser.title)
    }
}
