package com.rem.designsystem

import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatPlaygroundEffect
import com.rem.designsystem.screens.ChatPlaygroundFixture
import com.rem.designsystem.screens.ChatReplyContext
import com.rem.designsystem.screens.ChatScreenAction
import com.rem.designsystem.screens.ChatTranscriptAction
import com.rem.designsystem.screens.ChatTranscriptEntry
import com.rem.designsystem.screens.ChatTranscriptRules
import com.rem.designsystem.screens.ComposerPhase
import com.rem.designsystem.screens.InboxAction
import com.rem.designsystem.screens.InboxItemState
import com.rem.designsystem.screens.InboxPlaygroundFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Twin of `ChatPlaygroundFixtureTests.swift`: no fabricated receipts, truthful Inbox routing. */
class ChatPlaygroundFixtureTest {
    private fun ChatPlaygroundFixture.act(action: ChatScreenAction) = handle(action).first
    private fun ChatPlaygroundFixture.latest() = ChatTranscriptRules.latestOutgoingId(entries)!!

    @Test fun populatedShowsOnlyTheLatestReceiptAndAConsistentTimeline() {
        val fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Populated)
        assertNull(fixture.emptyState)
        assertEquals("u2", fixture.latest())
        assertEquals(MessageDelivery.None, ChatTranscriptRules.displayedDelivery(fixture.message("u1")!!, "u2"))
        assertEquals(MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message("u2")!!.delivery)
        assertEquals("Connected", fixture.header.activity)
    }

    @Test fun emptyHasStartersAndNoTranscript() {
        val fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
        assertEquals(ChatPlaygroundFixture.Starters, fixture.emptyState!!.starters)
        assertTrue(fixture.entries.isEmpty())
    }

    @Test fun starterSendsWithoutAReceipt() {
        val fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty).act(ChatScreenAction.Starter("plan-day"))
        assertNull(fixture.emptyState)
        val sent = (fixture.entries.last() as ChatTranscriptEntry.Message).message
        assertEquals("Help me plan my day", sent.text)
        assertEquals("No host acceptance, no receipt", MessageDelivery.None, sent.delivery)
        assertEquals(ComposerPhase.Sending, fixture.composer.state.phase)
        assertTrue(fixture.header.isWorking)
    }

    @Test fun sendStopAndHostAcceptanceJourney() {
        var fixture = ChatPlaygroundFixture()
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("Plan my afternoon")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val id = fixture.latest()
        assertEquals(MessageDelivery.None, fixture.message(id)!!.delivery)
        assertEquals(ChatComposerAction.Cancel, fixture.composer.state.primaryAction)
        fixture = fixture.act(ChatScreenAction.Composer(ChatComposerAction.Cancel))
        assertEquals(ComposerPhase.Idle, fixture.composer.state.phase)
        assertEquals("Stop never fabricates a receipt", MessageDelivery.None, fixture.message(id)!!.delivery)

        fixture = fixture.act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("Again")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
            .simulateHostAcceptance()
        val again = fixture.latest()
        assertEquals(MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(again)!!.delivery)
        assertEquals(ComposerPhase.Streaming, fixture.composer.state.phase)
        fixture = fixture.simulateReadAcknowledgement()
        assertEquals(MessageDelivery.Read(ChatPlaygroundFixture.FixtureTime), fixture.message(again)!!.delivery)
        val before = fixture.entries.size
        fixture = fixture.simulateReplyComplete()
        assertEquals(before + 1, fixture.entries.size)
        assertEquals(ComposerPhase.Idle, fixture.composer.state.phase)
    }

    @Test fun readNeverAppearsWithoutDelivery() {
        val fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("Hi")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
            .simulateReadAcknowledgement()
        assertEquals(MessageDelivery.None, fixture.message(fixture.latest())!!.delivery)
    }

    @Test fun failureAndRetryJourney() {
        var fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("Share the agenda")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
            .simulateDeliveryFailure()
        val id = fixture.latest()
        assertEquals(MessageDelivery.Failed, fixture.message(id)!!.delivery)
        assertTrue(fixture.message(id)!!.canRetry)
        assertEquals(ComposerPhase.Idle, fixture.composer.state.phase)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(id)))
        assertEquals("Retry waits for host acceptance", MessageDelivery.None, fixture.message(id)!!.delivery)
        assertEquals(ChatPlaygroundFixture.RetryNote, fixture.note)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(id)))
        assertEquals("Retry is not offered twice", MessageDelivery.None, fixture.message(id)!!.delivery)
    }

    @Test fun headerAndAddEffects() {
        val fixture = ChatPlaygroundFixture()
        assertEquals(ChatPlaygroundEffect.Exit, fixture.handle(ChatScreenAction.Back).second)
        assertEquals(ChatPlaygroundEffect.PresentHostControls, fixture.handle(ChatScreenAction.Overflow).second)
        assertEquals(ChatPlaygroundEffect.PresentAddToChat, fixture.handle(ChatScreenAction.Composer(ChatComposerAction.Add)).second)
        val (noted, effect) = fixture.handle(ChatScreenAction.ActivityDetails)
        assertNull(effect)
        assertEquals(ChatPlaygroundFixture.ActivityNote, noted.note)
    }

    @Test fun inboxRoutesEachStateIntoTaskReplyWithTheSameTruth() {
        val inbox = InboxPlaygroundFixture()
        for (item in inbox.items) {
            val chat = inbox.route(InboxAction.Open(item.id))!!
            assertEquals(item.id, chat.taskId)
            assertEquals(ChatReplyContext(item.id, ChatPlaygroundFixture.ReplyTitle, item.title), chat.replyContext)
            assertEquals(item.state.header(), chat.header)
            assertEquals("Write your reply…", chat.composer.state.placeholder)
        }
        assertNull(inbox.route(InboxAction.Open("missing")))
        assertTrue(InboxPlaygroundFixture.content(InboxPlaygroundFixture.Content.Empty).items.isEmpty())
    }

    @Test fun inboxStatesMapToLabelsAndAttention() {
        assertNull(InboxItemState.None.statusLabel)
        assertEquals("Status unknown", InboxItemState.Unknown.statusLabel)
        assertTrue(InboxItemState.NeedsApproval.needsPerson)
        assertTrue(InboxItemState.Blocked.needsPerson)
        assertFalse(InboxItemState.Completed.needsPerson)
        assertEquals(ChatHeaderStatus.NeedsYou, InboxItemState.Blocked.header().status)
        assertTrue(InboxItemState.Executing.header().isWorking)
        assertEquals("Connected", InboxItemState.None.header().activity)
    }

    @Test fun dismissClearsOnlyTheMatchingTarget() {
        var chat = InboxPlaygroundFixture().route(InboxAction.Open("venue-booking"))!!
        chat = chat.act(ChatScreenAction.DismissReplyContext("other"))
        assertNotNull(chat.replyContext)
        chat = chat.act(ChatScreenAction.DismissReplyContext("venue-booking"))
        assertNull(chat.replyContext)
        assertEquals("Task identity survives dismissing the reply target", "venue-booking", chat.taskId)
    }
}
