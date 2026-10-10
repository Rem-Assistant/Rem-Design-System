package com.rem.designsystem

import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ChatEmptyState
import com.rem.designsystem.screens.ChatHeaderDisplay
import com.rem.designsystem.screens.ChatMessageDisplay
import com.rem.designsystem.screens.ChatReplyContext
import com.rem.designsystem.screens.ChatScreenAction
import com.rem.designsystem.screens.ChatTranscriptEntry
import com.rem.designsystem.screens.ChatTranscriptRules
import com.rem.designsystem.screens.ComposerAvailability
import com.rem.designsystem.screens.ComposerPhase
import com.rem.designsystem.screens.ComposerSendDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Chat / Inbox presentation contract's pure rules (`screens/ChatScreenModel.kt`). Paired with the
 * SwiftUI `ChatScreenModelTests.swift`; both assert the same rules so the composers cannot drift.
 */
class ChatScreenModelTest {
    private val photo = ComposerAttachment("photo.0", "Photo 1", ComposerAttachment.Kind.Image)
    private val file = ComposerAttachment("file.a", "notes.png", ComposerAttachment.Kind.File)

    @Test fun emptyDraftCannotSend() {
        val state = ChatComposerState()
        assertFalse(state.canSend)
        assertNull(state.primaryAction)
        assertEquals(ComposerSendDisplay.Unavailable, state.sendDisplay)
    }

    @Test fun whitespaceOnlyDraftCannotSend() {
        assertFalse(ChatComposerState(draft = "  \n\t ").canSend)
    }

    @Test fun textDraftSends() {
        val state = ChatComposerState(draft = "Plan my afternoon")
        assertTrue(state.canSend)
        assertEquals(ChatComposerAction.Send, state.primaryAction)
        assertEquals(ComposerSendDisplay.Send, state.sendDisplay)
    }

    @Test fun attachmentOnlyMessageSends() {
        assertTrue(ChatComposerState(attachments = listOf(photo)).canSend)
        assertTrue(ChatComposerState(attachments = listOf(file)).canSend)
    }

    @Test fun capabilityChipAloneNeverMakesAMessage() {
        val state = ChatComposerState(attachments = listOf(ComposerAttachment.CloudBrowser))
        assertFalse(state.hasContentAttachments)
        assertFalse(state.canSend)
        assertTrue(ChatComposerState(draft = "Find a venue", attachments = listOf(ComposerAttachment.CloudBrowser)).canSend)
    }

    @Test fun externallyDisabledBlocksSendSpeakAndModel() {
        val state = ChatComposerState(
            draft = "Hello", attachments = listOf(photo),
            availability = ComposerAvailability.Disabled("Sign in to chat"),
        )
        assertFalse(state.canSend)
        assertFalse(state.showsSpeak)
        assertFalse(state.modelEnabled)
        assertNull(state.primaryAction)
        assertEquals(ComposerSendDisplay.Unavailable, state.sendDisplay)
    }

    @Test fun sendingAndStreamingAreCancelNeverSend() {
        for (phase in listOf(ComposerPhase.Sending, ComposerPhase.Streaming)) {
            val state = ChatComposerState(draft = "Next question", phase = phase)
            assertFalse("$phase", state.canSend)
            assertTrue("$phase", state.canCancel)
            assertEquals("$phase", ChatComposerAction.Cancel, state.primaryAction)
            assertEquals("$phase", ComposerSendDisplay.Stop, state.sendDisplay)
            assertFalse("$phase", state.showsSpeak)
            assertFalse("$phase", state.modelEnabled)
        }
    }

    @Test fun cancelStaysAvailableWhenInputIsExternallyDisabledMidTurn() {
        val state = ChatComposerState(availability = ComposerAvailability.Disabled(null), phase = ComposerPhase.Streaming)
        assertEquals(ChatComposerAction.Cancel, state.primaryAction)
    }

    @Test fun speakFollowsHostVoiceAvailability() {
        assertTrue(ChatComposerState(voiceAvailable = true).showsSpeak)
        assertFalse(ChatComposerState(voiceAvailable = false).showsSpeak)
    }

    @Test fun assistantMessagesNeverCarryDelivery() {
        val incoming = ChatMessageDisplay("a", MessageRole.Assistant, "Done", delivery = MessageDelivery.Read("10:24"))
        assertEquals(MessageDelivery.None, incoming.delivery)
        val outgoing = ChatMessageDisplay("u", MessageRole.User, "Thanks", delivery = MessageDelivery.Read("10:24"))
        assertEquals("The DS renders the supplied state verbatim", MessageDelivery.Read("10:24"), outgoing.delivery)
    }

    // Transcript receipt placement (board `2681:21977`, acceptance `2659:21942`).

    private fun user(id: String, delivery: MessageDelivery) =
        ChatTranscriptEntry.Message(ChatMessageDisplay(id, MessageRole.User, id, delivery = delivery))

    @Test fun onlyLatestOutgoingCarriesItsReceipt() {
        val entries = listOf(
            ChatTranscriptEntry.Timestamp("t", "Today"),
            user("u1", MessageDelivery.Delivered("10:20")),
            ChatTranscriptEntry.Message(ChatMessageDisplay("a1", MessageRole.Assistant, "ok")),
            user("u2", MessageDelivery.Read("10:24")),
            ChatTranscriptEntry.Message(ChatMessageDisplay("a2", MessageRole.Assistant, "done")),
        )
        val latest = ChatTranscriptRules.latestOutgoingId(entries)
        assertEquals("u2", latest)
        assertEquals(MessageDelivery.None, ChatTranscriptRules.displayedDelivery((entries[1] as ChatTranscriptEntry.Message).message, latest))
        assertEquals(MessageDelivery.Read("10:24"), ChatTranscriptRules.displayedDelivery((entries[3] as ChatTranscriptEntry.Message).message, latest))
    }

    @Test fun failureStaysVisibleOnOlderMessages() {
        val failed = ChatMessageDisplay("u1", MessageRole.User, "x", delivery = MessageDelivery.Failed)
        assertEquals(MessageDelivery.Failed, ChatTranscriptRules.displayedDelivery(failed, "u9"))
    }

    @Test fun noOutgoingMeansNoLatest() {
        assertNull(ChatTranscriptRules.latestOutgoingId(listOf(ChatTranscriptEntry.Timestamp("t", "Today"))))
    }

    @Test fun replyContextAndHeaderDefaults() {
        val header = ChatHeaderDisplay(activity = "Connected")
        assertTrue(header.showsBack && header.showsOverflow && header.showsActivityDetails)
        assertFalse(header.isWorking)
        val context = ChatReplyContext("target-1", "Replying to Rem", "Plan the next step")
        assertEquals(ChatScreenAction.DismissReplyContext("target-1"), ChatScreenAction.DismissReplyContext(context.targetId))
        assertEquals("What can I help with?", ChatEmptyState(message = "m").title)
    }
}
