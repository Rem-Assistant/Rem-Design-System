package com.rem.designsystem

import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.screens.AgentActivityCurrent
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatMessageAction
import com.rem.designsystem.screens.ChatMessageActionsDisplay
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

    /**
     * Release-review regression: fail A, send + accept + complete B, then retry A. Host evidence must
     * apply to A (the active turn), not to B (the latest outgoing), and the turn must complete.
     */
    @Test fun retryOfOlderFailedMessageCompletesItsOwnTurn() {
        var fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("A")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val a = fixture.latest()
        fixture = fixture.simulateDeliveryFailure()
        assertEquals(MessageDelivery.Failed, fixture.message(a)!!.delivery)

        fixture = fixture.act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("B")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val b = fixture.latest()
        assertTrue(a != b)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(a)))
        assertEquals("Retry never switches the active turn mid-flight", b, fixture.activeOutgoingId)
        assertEquals(MessageDelivery.Failed, fixture.message(a)!!.delivery)
        fixture = fixture.simulateHostAcceptance().simulateReplyComplete()
        assertEquals(MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(b)!!.delivery)
        assertNull(fixture.activeOutgoingId)

        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(a)))
        assertEquals(a, fixture.activeOutgoingId)
        assertEquals(ComposerPhase.Sending, fixture.composer.state.phase)
        fixture = fixture.simulateHostAcceptance()
        assertEquals("Acceptance applies to A", MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(a)!!.delivery)
        assertEquals("B is untouched", MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(b)!!.delivery)
        assertEquals(ComposerPhase.Streaming, fixture.composer.state.phase)
        val before = fixture.entries.size
        fixture = fixture.simulateReplyComplete()
        assertEquals(before + 1, fixture.entries.size)
        assertEquals(ComposerPhase.Idle, fixture.composer.state.phase)
        assertNull(fixture.activeOutgoingId)

        // Latest-only receipt display is unchanged: B is still the latest outgoing message.
        val latest = ChatTranscriptRules.latestOutgoingId(fixture.entries)
        assertEquals(b, latest)
        assertEquals(MessageDelivery.None, ChatTranscriptRules.displayedDelivery(fixture.message(a)!!, latest))
    }

    @Test fun retryFailureAppliesToTheRetriedMessage() {
        var fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("A")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val a = fixture.latest()
        fixture = fixture.simulateDeliveryFailure()
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("B")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val b = fixture.latest()
        fixture = fixture.simulateHostAcceptance().simulateReplyComplete()
            .act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(a)))
            .simulateDeliveryFailure()
        assertEquals(MessageDelivery.Failed, fixture.message(a)!!.delivery)
        assertTrue(fixture.message(a)!!.canRetry)
        assertEquals(MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(b)!!.delivery)
        assertEquals(ComposerPhase.Idle, fixture.composer.state.phase)
    }

    @Test fun cancelledRetryRestoresNotDeliveredAndTryAgain() {
        var fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("A")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val a = fixture.activeOutgoingId!!
        fixture = fixture.simulateDeliveryFailure()
            .act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(a)))
            .act(ChatScreenAction.Composer(ChatComposerAction.Cancel))
        assertNull(fixture.activeOutgoingId)
        assertEquals("A cancelled retry is still not delivered", MessageDelivery.Failed, fixture.message(a)!!.delivery)
        assertTrue("and can be retried again", fixture.message(a)!!.canRetry)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.Retry(a))).simulateHostAcceptance()
        assertEquals(MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message(a)!!.delivery)
    }

    @Test fun cancelEndsTheActiveTurnWithoutAReceipt() {
        var fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty)
            .act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("A")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val a = fixture.activeOutgoingId!!
        fixture = fixture.act(ChatScreenAction.Composer(ChatComposerAction.Cancel))
        assertNull(fixture.activeOutgoingId)
        fixture = fixture.simulateHostAcceptance()
        assertEquals("No acceptance after cancel", MessageDelivery.None, fixture.message(a)!!.delivery)
    }

    @Test fun headerAndAddEffects() {
        val fixture = ChatPlaygroundFixture()
        assertEquals(ChatPlaygroundEffect.Exit, fixture.handle(ChatScreenAction.Back).second)
        assertEquals(ChatPlaygroundEffect.PresentHostControls, fixture.handle(ChatScreenAction.Overflow).second)
        assertEquals(ChatPlaygroundEffect.PresentAddToChat, fixture.handle(ChatScreenAction.Composer(ChatComposerAction.Add)).second)
        val (opened, effect) = fixture.handle(ChatScreenAction.ActivityDetails)
        assertEquals("The header identity opens Agent activity", ChatPlaygroundEffect.PresentActivity, effect)
        assertEquals("Opening activity changes nothing in the conversation", fixture, opened)
        assertNull(opened.note)
    }

    @Test fun activityScreenCarriesTheHeadersCurrentState() {
        var fixture = ChatPlaygroundFixture()
        assertEquals(AgentActivityCurrent.of(fixture.header), fixture.activity.display.current)
        fixture = fixture.act(ChatScreenAction.Composer(ChatComposerAction.DraftChanged("Plan my afternoon")))
            .act(ChatScreenAction.Composer(ChatComposerAction.Send))
        val current = fixture.activity.display.current
        assertEquals("Opened mid-turn, it shows the live state", "Working on your request", current.activity)
        assertTrue(current.isWorking)
        val task = InboxPlaygroundFixture().route(InboxAction.Open("venue-booking"))!!
        assertEquals(task.header.activity, task.activity.display.current.activity)
        assertEquals(ChatHeaderStatus.NeedsYou, task.activity.display.current.status)
    }

    @Test fun longPressPresentsTheMessageActionSheetForThatMessage() {
        val fixture = ChatPlaygroundFixture()
        val (same, effect) = fixture.handle(ChatScreenAction.Transcript(ChatTranscriptAction.RequestActions("a1")))
        assertEquals(ChatPlaygroundEffect.PresentMessageActions(ChatMessageActionsDisplay(fixture.message("a1")!!)), effect)
        assertEquals("Presenting the sheet changes nothing else", fixture, same)
        val own = fixture.handle(ChatScreenAction.Transcript(ChatTranscriptAction.RequestActions("u1"))).second
        assertEquals(
            "No Report on the person's own message",
            listOf(listOf(ChatMessageAction.Reply), listOf(ChatMessageAction.Copy, ChatMessageAction.SelectText)),
            (own as ChatPlaygroundEffect.PresentMessageActions).display.groups,
        )
        assertNull(fixture.handle(ChatScreenAction.Transcript(ChatTranscriptAction.RequestActions("missing"))).second)
    }

    @Test fun sheetReactionAndReplyChangeOnlyPresentation() {
        var fixture = ChatPlaygroundFixture().act(ChatScreenAction.Transcript(ChatTranscriptAction.React("a1", MessageReaction.Fire)))
        assertEquals(MessageReaction.Fire, fixture.message("a1")!!.reaction)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.MessageAction("a1", ChatMessageAction.Reply)))
        assertEquals(ChatReplyContext("a1", ChatPlaygroundFixture.ReplyTitle, fixture.message("a1")!!.text), fixture.replyContext)
        fixture = fixture.act(ChatScreenAction.Transcript(ChatTranscriptAction.MessageAction("u2", ChatMessageAction.Reply)))
        assertEquals(ChatPlaygroundFixture.OwnReplyTitle, fixture.replyContext!!.title)
        assertEquals("u2", fixture.replyContext!!.targetId)
        assertEquals("Receipts untouched", MessageDelivery.Delivered(ChatPlaygroundFixture.FixtureTime), fixture.message("u2")!!.delivery)
    }

    @Test fun hostOwnedSheetActionsSendNothing() {
        val notes = listOf(
            ChatTranscriptAction.RequestMoreReactions("a1") to ChatPlaygroundFixture.MoreReactionsNote,
            ChatTranscriptAction.MessageAction("a1", ChatMessageAction.MarkUnread) to ChatPlaygroundFixture.MarkUnreadNote,
            ChatTranscriptAction.MessageAction("a1", ChatMessageAction.Copy) to ChatPlaygroundFixture.CopyNote,
            ChatTranscriptAction.MessageAction("a1", ChatMessageAction.SelectText) to ChatPlaygroundFixture.SelectTextNote,
            ChatTranscriptAction.MessageAction("a1", ChatMessageAction.Report) to ChatPlaygroundFixture.ReportNote,
        )
        for ((action, note) in notes) {
            val before = ChatPlaygroundFixture()
            val (after, effect) = before.handle(ChatScreenAction.Transcript(action))
            assertNull(effect)
            assertEquals(note, after.note)
            assertEquals(before.entries, after.entries)
            assertEquals(before.composer, after.composer)
        }
    }

    @Test fun callButtonIsAnInAppVoiceEntryThatDialsNothing() {
        val fixture = ChatPlaygroundFixture()
        assertFalse("call would replace overflow, the Playground's host-controls route", fixture.header.showsCall)
        val (called, effect) = fixture.handle(ChatScreenAction.Call)
        assertNull("no presentation, transcript or composer change", effect)
        assertEquals(ChatPlaygroundFixture.CallNote, called.note)
        assertEquals(fixture.entries, called.entries)
        assertEquals(fixture.composer, called.composer)
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
