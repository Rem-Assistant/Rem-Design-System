package com.rem.designsystem

import com.rem.designsystem.chat.AddToChatMaxPhotoSelection
import com.rem.designsystem.chat.ChatModelOption
import com.rem.designsystem.chat.ChatModelProvider
import com.rem.designsystem.chat.ChatModelSelection
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageBubbleGeometry as G
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.ThinkingLevel
import com.rem.designsystem.chat.chatModelTriggerLabel
import org.junit.Assert.assertEquals
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
