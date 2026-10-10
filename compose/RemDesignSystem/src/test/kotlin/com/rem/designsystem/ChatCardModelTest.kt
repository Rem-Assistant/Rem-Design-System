package com.rem.designsystem

import androidx.compose.ui.unit.dp
import com.rem.designsystem.chat.ActionReceiptHeight
import com.rem.designsystem.chat.ActionReceiptOutcome
import com.rem.designsystem.chat.ActionReceiptSuccessTintAlpha
import com.rem.designsystem.chat.MessageDraft
import com.rem.designsystem.chat.MessageDraftCardMaxWidth
import com.rem.designsystem.chat.MessageDraftCardState
import com.rem.designsystem.chat.PollCardMarkerWidth
import com.rem.designsystem.chat.PollCardMaxWidth
import com.rem.designsystem.chat.PollCardModel
import com.rem.designsystem.chat.PollCardState
import com.rem.designsystem.chat.PollLetteredOption
import com.rem.designsystem.chat.PollOption
import com.rem.designsystem.chat.PollPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure display rules of the chat cards — PollCard lettering and state, MessageDraftCard receipt mapping,
 * and the Figma names each enum maps to. Paired with the SwiftUI `ChatCardModelTests.swift`.
 */
class ChatCardModelTest {
    private val options = listOf(
        PollOption("notion", "Add Notion"),
        PollOption("later", "Not now"),
        PollOption("ask", "Ask me each time"),
    )

    @Test fun markersFollowListOrderAndExtendPastTheMasterAB() {
        assertEquals(listOf("A", "B", "C"), PollCardModel.lettered(options).map { it.marker })
        assertEquals(listOf("A", "B"), PollCardModel.lettered(options.take(2)).map { it.marker })
        assertEquals(emptyList<PollLetteredOption>(), PollCardModel.lettered(emptyList()))
    }

    @Test fun markerSequenceIsBijectiveBase26() {
        assertEquals("A", PollCardModel.marker(0))
        assertEquals("C", PollCardModel.marker(2))
        assertEquals("Z", PollCardModel.marker(25))
        assertEquals("AA", PollCardModel.marker(26))
        assertEquals("AB", PollCardModel.marker(27))
        assertEquals("ZZ", PollCardModel.marker(701))
        assertEquals("AAA", PollCardModel.marker(702))
    }

    @Test fun selectionResolvesToAnsweredWithOriginalMarker() {
        val state = PollCardModel.state(options, "ask")
        assertEquals(PollCardState.Answered(PollLetteredOption("C", options[2])), state)
        assertEquals("Answered", state.figmaName)
        assertEquals(PollCardState.Answered(PollLetteredOption("B", options[1])), PollCardModel.state(options, "later"))
    }

    @Test fun noOrUnknownSelectionStaysAwaiting() {
        assertEquals(PollCardState.Awaiting, PollCardModel.state(options, null))
        assertEquals(PollCardState.Awaiting, PollCardModel.state(options, "missing"))
        assertEquals(PollCardState.Awaiting, PollCardModel.state(emptyList(), "notion"))
        assertEquals("Awaiting", PollCardState.Awaiting.figmaName)
    }

    @Test fun purposeFigmaNames() {
        assertEquals(listOf("Choice", "Suggestion"), PollPurpose.entries.map { it.figmaName })
    }

    @Test fun draftStateMapsToReceipt() {
        assertEquals(listOf("Review", "Sent", "Unconfirmed"), MessageDraftCardState.entries.map { it.figmaName })
        assertTrue(MessageDraftCardState.Review.showsActions)
        assertNull(MessageDraftCardState.Review.receiptOutcome)
        assertNull(MessageDraftCardState.Review.receiptLabel)
        assertFalse(MessageDraftCardState.Sent.showsActions)
        assertEquals(ActionReceiptOutcome.Confirmed, MessageDraftCardState.Sent.receiptOutcome)
        assertEquals("Sent", MessageDraftCardState.Sent.receiptLabel)
        assertFalse(MessageDraftCardState.Unconfirmed.showsActions)
        assertEquals(ActionReceiptOutcome.Unconfirmed, MessageDraftCardState.Unconfirmed.receiptOutcome)
        assertEquals("Unconfirmed", MessageDraftCardState.Unconfirmed.receiptLabel)
    }

    @Test fun receiptOutcomeFigmaNamesAndMetrics() {
        assertEquals(listOf("Confirmed", "Unconfirmed"), ActionReceiptOutcome.entries.map { it.figmaName })
        assertEquals(48.dp, ActionReceiptHeight)
        assertEquals(0.12f, ActionReceiptSuccessTintAlpha, 0f)
        assertEquals(330.dp, MessageDraftCardMaxWidth)
        assertEquals(330.dp, PollCardMaxWidth)
        assertEquals(20.dp, PollCardMarkerWidth)
    }

    @Test fun draftDefaultsToNewEmailTitle() {
        assertEquals("New Email", MessageDraft("a@example.com", "b@example.com", "S", "B").title)
    }
}
