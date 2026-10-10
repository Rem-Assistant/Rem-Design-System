package com.rem.designsystem

import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ChatMessageDisplay
import com.rem.designsystem.screens.ChatTranscriptAction
import com.rem.designsystem.screens.ComposerAvailability
import com.rem.designsystem.screens.ComposerPhase
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Reference app adapter (test-only) — twin of `ChatAdapterContractTests.swift`. A minimal stand-in for a
// consuming app and its thin adapter, written against the public DS contract only, proving the boundary
// in `docs/contracts/chat-adapter.md`. Nothing here ships in the library.

/** App evidence: [acceptedAt] is durable host acceptance; [readAcknowledgedAt] is explicit acknowledgement. */
private data class MockAppMessage(
    val id: String,
    val fromPerson: Boolean,
    val body: String,
    val acceptedAt: Instant? = null,
    val readAcknowledgedAt: Instant? = null,
    val sendFailed: Boolean = false,
    val retryable: Boolean = false,
)

private data class MockAppAttachment(val id: String, val name: String, val isCapability: Boolean = false)

private data class MockAppSession(
    val signedIn: Boolean = true,
    val draft: String = "",
    val attachments: List<MockAppAttachment> = emptyList(),
    val turn: Turn = Turn.None,
    val voiceReady: Boolean = true,
    val composerFocused: Boolean = false,
) {
    enum class Turn { None, Submitted, Receiving }
}

private sealed interface MockAppOperation {
    data class SetDraft(val text: String) : MockAppOperation
    data class Submit(val text: String, val attachmentIds: List<String>) : MockAppOperation
    data object CancelTurn : MockAppOperation
    data object StartVoice : MockAppOperation
    data object PresentAddToChat : MockAppOperation
    data class RemoveAttachment(val id: String) : MockAppOperation
    data class SetComposerFocus(val focused: Boolean) : MockAppOperation
    data class Retry(val id: String) : MockAppOperation
    data class PresentReactionPicker(val id: String) : MockAppOperation
    data class SetReaction(val id: String, val emoji: String?) : MockAppOperation
}

private object MockChatAdapter {
    val clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC)

    /** Delivered needs host acceptance; Read additionally needs explicit acknowledgement. */
    fun delivery(message: MockAppMessage): MessageDelivery {
        if (!message.fromPerson) return MessageDelivery.None
        val accepted = message.acceptedAt
            ?: return if (message.sendFailed) MessageDelivery.Failed else MessageDelivery.None
        val at = clock.format(accepted)
        return if (message.readAcknowledgedAt == null) MessageDelivery.Delivered(at) else MessageDelivery.Read(at)
    }

    fun message(message: MockAppMessage): ChatMessageDisplay {
        val delivery = delivery(message)
        return ChatMessageDisplay(
            id = message.id,
            role = if (message.fromPerson) MessageRole.User else MessageRole.Assistant,
            text = message.body,
            delivery = delivery,
            canRetry = delivery == MessageDelivery.Failed && message.retryable,
        )
    }

    fun composer(session: MockAppSession) = ChatComposerState(
        draft = session.draft,
        attachments = session.attachments.map {
            ComposerAttachment(it.id, it.name, if (it.isCapability) ComposerAttachment.Kind.Capability else ComposerAttachment.Kind.Image)
        },
        availability = if (session.signedIn) ComposerAvailability.Enabled else ComposerAvailability.Disabled("Sign in to chat"),
        phase = when (session.turn) {
            MockAppSession.Turn.None -> ComposerPhase.Idle
            MockAppSession.Turn.Submitted -> ComposerPhase.Sending
            MockAppSession.Turn.Receiving -> ComposerPhase.Streaming
        },
        voiceAvailable = session.voiceReady,
        isFocused = session.composerFocused,
    )

    fun operation(action: ChatComposerAction, session: MockAppSession): MockAppOperation? = when (action) {
        is ChatComposerAction.DraftChanged -> MockAppOperation.SetDraft(action.text)
        // Defence in depth: the adapter re-checks the same rule the DS rendered.
        ChatComposerAction.Send -> if (composer(session).canSend) {
            MockAppOperation.Submit(session.draft.trim(), session.attachments.map { it.id })
        } else null
        ChatComposerAction.Cancel -> if (composer(session).canCancel) MockAppOperation.CancelTurn else null
        ChatComposerAction.Speak -> MockAppOperation.StartVoice
        ChatComposerAction.Add -> MockAppOperation.PresentAddToChat
        is ChatComposerAction.RemoveAttachment -> MockAppOperation.RemoveAttachment(action.id)
        is ChatComposerAction.FocusChanged -> MockAppOperation.SetComposerFocus(action.focused)
    }

    fun operation(action: ChatTranscriptAction): MockAppOperation = when (action) {
        is ChatTranscriptAction.Retry -> MockAppOperation.Retry(action.messageId)
        is ChatTranscriptAction.RequestReaction -> MockAppOperation.PresentReactionPicker(action.messageId)
        is ChatTranscriptAction.React -> MockAppOperation.SetReaction(action.messageId, action.reaction?.emoji)
    }
}

class ChatAdapterContractTest {
    private val accepted = Instant.ofEpochSecond(1_791_000_000)

    private fun outgoing(accepted: Instant? = null, read: Instant? = null, failed: Boolean = false, retryable: Boolean = false) =
        MockAppMessage("m1", true, "Thanks", accepted, read, failed, retryable)

    @Test fun noAcceptanceMeansNoReceipt() {
        assertEquals(MessageDelivery.None, MockChatAdapter.delivery(outgoing()))
    }

    @Test fun deliveredRequiresHostAcceptance() {
        val at = MockChatAdapter.clock.format(accepted)
        assertEquals(MessageDelivery.Delivered(at), MockChatAdapter.delivery(outgoing(accepted = accepted)))
    }

    @Test fun readRequiresExplicitAcknowledgementAndKeepsDeliveredTime() {
        val at = MockChatAdapter.clock.format(accepted)
        assertEquals(MessageDelivery.Read(at), MockChatAdapter.delivery(outgoing(accepted = accepted, read = accepted.plusSeconds(600))))
    }

    @Test fun acknowledgementWithoutAcceptanceNeverShowsRead() {
        assertEquals(MessageDelivery.None, MockChatAdapter.delivery(outgoing(read = accepted)))
    }

    @Test fun failureWithoutAcceptanceIsNotDeliveredAndRetryIsHostGated() {
        assertEquals(MessageDelivery.Failed, MockChatAdapter.message(outgoing(failed = true)).delivery)
        assertFalse(MockChatAdapter.message(outgoing(failed = true)).canRetry)
        assertTrue(MockChatAdapter.message(outgoing(failed = true, retryable = true)).canRetry)
    }

    @Test fun incomingMessagesCarryNoReceipt() {
        val incoming = MockAppMessage("a1", false, "Done", accepted, accepted)
        assertEquals(MessageDelivery.None, MockChatAdapter.message(incoming).delivery)
    }

    @Test fun signedOutSessionRendersExternallyDisabledComposer() {
        val session = MockAppSession(signedIn = false, draft = "Hi")
        assertEquals(ComposerAvailability.Disabled("Sign in to chat"), MockChatAdapter.composer(session).availability)
        assertNull(MockChatAdapter.operation(ChatComposerAction.Send, session))
    }

    @Test fun sendMapsToSubmitWithTrimmedTextAndAttachments() {
        val session = MockAppSession(draft = "  Plan my afternoon ", attachments = listOf(MockAppAttachment("photo.0", "Photo 1")))
        assertEquals(MockAppOperation.Submit("Plan my afternoon", listOf("photo.0")), MockChatAdapter.operation(ChatComposerAction.Send, session))
    }

    @Test fun attachmentOnlySendSubmitsEmptyText() {
        val session = MockAppSession(attachments = listOf(MockAppAttachment("photo.0", "Photo 1")))
        assertEquals(MockAppOperation.Submit("", listOf("photo.0")), MockChatAdapter.operation(ChatComposerAction.Send, session))
    }

    @Test fun capabilityOnlySessionCannotSubmit() {
        val session = MockAppSession(attachments = listOf(MockAppAttachment("cloud-browser", "Cloud browser", isCapability = true)))
        assertNull(MockChatAdapter.operation(ChatComposerAction.Send, session))
    }

    @Test fun inFlightTurnsMapStopToCancelNotSubmit() {
        for (turn in listOf(MockAppSession.Turn.Submitted, MockAppSession.Turn.Receiving)) {
            val session = MockAppSession(draft = "More", turn = turn)
            assertEquals(ChatComposerAction.Cancel, MockChatAdapter.composer(session).primaryAction)
            assertEquals(MockAppOperation.CancelTurn, MockChatAdapter.operation(ChatComposerAction.Cancel, session))
            assertNull(MockChatAdapter.operation(ChatComposerAction.Send, session))
        }
        assertNull(MockChatAdapter.operation(ChatComposerAction.Cancel, MockAppSession()))
    }

    @Test fun voiceAddFocusAndAttachmentActionsRouteToAppOperations() {
        val session = MockAppSession()
        assertEquals(MockAppOperation.StartVoice, MockChatAdapter.operation(ChatComposerAction.Speak, session))
        assertEquals(MockAppOperation.PresentAddToChat, MockChatAdapter.operation(ChatComposerAction.Add, session))
        assertEquals(MockAppOperation.SetComposerFocus(true), MockChatAdapter.operation(ChatComposerAction.FocusChanged(true), session))
        assertEquals(MockAppOperation.RemoveAttachment("photo.0"), MockChatAdapter.operation(ChatComposerAction.RemoveAttachment("photo.0"), session))
        assertEquals(MockAppOperation.SetDraft("a"), MockChatAdapter.operation(ChatComposerAction.DraftChanged("a"), session))
        assertTrue(MockChatAdapter.composer(MockAppSession(composerFocused = true)).isFocused)
        assertFalse(MockChatAdapter.composer(MockAppSession(voiceReady = false)).showsSpeak)
    }

    @Test fun transcriptActionsRouteToAppOperations() {
        assertEquals(MockAppOperation.Retry("m1"), MockChatAdapter.operation(ChatTranscriptAction.Retry("m1")))
        assertEquals(MockAppOperation.PresentReactionPicker("m1"), MockChatAdapter.operation(ChatTranscriptAction.RequestReaction("m1")))
        assertEquals(MockAppOperation.SetReaction("m1", "❤️"), MockChatAdapter.operation(ChatTranscriptAction.React("m1", MessageReaction.Heart)))
        assertEquals(MockAppOperation.SetReaction("m1", null), MockChatAdapter.operation(ChatTranscriptAction.React("m1", null)))
    }
}
