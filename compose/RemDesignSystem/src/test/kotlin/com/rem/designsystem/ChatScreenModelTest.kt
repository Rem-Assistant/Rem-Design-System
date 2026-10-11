package com.rem.designsystem

import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatMessageAction
import com.rem.designsystem.screens.ChatMessageActionsDisplay
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ChatEmptyState
import com.rem.designsystem.screens.ChatHeaderDisplay
import com.rem.designsystem.screens.ChatMessageDisplay
import com.rem.designsystem.screens.ChatReplyContext
import com.rem.designsystem.screens.ChatScreenAction
import com.rem.designsystem.screens.ChatTranscriptEntry
import com.rem.designsystem.screens.ChatTimestampReveal
import com.rem.designsystem.screens.ChatKeyboardDismiss
import com.rem.designsystem.screens.ChatTranscriptKeyboardDismissConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.rem.designsystem.screens.ChatJumpToLatest
import com.rem.designsystem.screens.ChatEdgeBackdrop
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

    @Test fun hostContentEntryIsNeverAMessage() {
        val entries = listOf(
            ChatTranscriptEntry.Message(ChatMessageDisplay("u1", MessageRole.User, "Plan my day")),
            ChatTranscriptEntry.HostContent("proposal.1"),
        )
        assertEquals("proposal.1", entries[1].id)
        assertEquals("A host card after the latest outgoing message never takes its receipt",
            "u1", ChatTranscriptRules.latestOutgoingId(entries))
    }

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

    @Test fun modelMenuEnabledOverridesTheDerivedRuleOnlyWhenSet() {
        // null keeps the derived rule.
        assertTrue(ChatComposerState().modelEnabled)
        assertFalse(ChatComposerState(availability = ComposerAvailability.Disabled(null)).modelEnabled)
        // Input blocked (pending provider evidence, quota out) but the menu stays open to escape to Automatic.
        val escape = ChatComposerState(draft = "Hi", availability = ComposerAvailability.Disabled("Quota reached"), modelMenuEnabled = true)
        assertTrue(escape.modelEnabled)
        assertFalse(escape.canSend)
        assertFalse(escape.showsSpeak)
        assertNull(escape.primaryAction)
        // The host closes the menu while preparing / sending even with input enabled.
        assertFalse(ChatComposerState(modelMenuEnabled = false).modelEnabled)
        assertTrue(ChatComposerState(phase = ComposerPhase.Sending, modelMenuEnabled = true).modelEnabled)
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
        assertFalse("call is opt-in: hosts show it only when in-app voice is available", header.showsCall)
        val context = ChatReplyContext("target-1", "Replying to Rem", "Plan the next step")
        assertEquals(ChatScreenAction.DismissReplyContext("target-1"), ChatScreenAction.DismissReplyContext(context.targetId))
        assertEquals("What can I help with?", ChatEmptyState(message = "m").title)
    }

    // WS1d — swipe left to reveal timestamps (thresholds marked for review).

    @Test fun messageTimeIsHostSuppliedAndOptional() {
        val m = ChatMessageDisplay(id = "u", role = MessageRole.User, text = "Hi", time = "10:24")
        assertEquals("10:24", m.time)
        assertNull(ChatMessageDisplay(id = "a", role = MessageRole.Assistant, text = "Hi").time)
        assertEquals("10:30", m.copy(time = "10:30").time)
        assertFalse(m == m.copy(time = null))
    }

    @Test fun onlyAClearlyHorizontalLeftDragReveals() {
        assertTrue(ChatTimestampReveal.isRevealDrag(dx = -20f, dy = 5f))
        assertFalse("rightward", ChatTimestampReveal.isRevealDrag(dx = 20f, dy = 0f))
        assertFalse("below travel", ChatTimestampReveal.isRevealDrag(dx = -9f, dy = 0f))
        assertFalse("vertical scroll", ChatTimestampReveal.isRevealDrag(dx = -20f, dy = 40f))
        assertFalse("diagonal", ChatTimestampReveal.isRevealDrag(dx = -15f, dy = 12f))
    }

    @Test fun revealTracksTheFingerThenResistsOrClamps() {
        assertEquals(0f, ChatTimestampReveal.reveal(dx = 30f, columnWidth = 60f, reduceMotion = false), 0f)
        assertEquals(40f, ChatTimestampReveal.reveal(dx = -40f, columnWidth = 60f, reduceMotion = false), 0f)
        assertEquals(60f, ChatTimestampReveal.reveal(dx = -60f, columnWidth = 60f, reduceMotion = false), 0f)
        assertEquals(72f, ChatTimestampReveal.reveal(dx = -100f, columnWidth = 60f, reduceMotion = false), 0.001f)
        assertEquals("Reduce Motion clamps", 60f, ChatTimestampReveal.reveal(dx = -100f, columnWidth = 60f, reduceMotion = true), 0f)
        assertEquals(0f, ChatTimestampReveal.SETTLED, 0f)
    }

    // Edge backdrops (paired with `ChatScreenModelTests.swift`).

    @Test fun edgeBackdropContentInsetsEqualTheChromeAndDockHeights() {
        for (h in listOf(0f, 128f, 152f, 177.5f, 260f)) {
            assertEquals("top inset == chrome $h", h, ChatEdgeBackdrop.contentTopInset(h), 0f)
            assertEquals("bottom inset == dock $h", h, ChatEdgeBackdrop.contentBottomInset(h), 0f)
        }
        assertEquals("never negative", 0f, ChatEdgeBackdrop.contentTopInset(-4f), 0f)
        assertEquals("never negative", 0f, ChatEdgeBackdrop.contentBottomInset(-4f), 0f)
    }

    @Test fun edgeBackdropFirstMessageIsFullyVisibleAtTheTop() {
        for (chrome in listOf(96f, 128f, 152f, 260f)) {
            val top = ChatEdgeBackdrop.firstMessageTop(chrome)
            assertTrue("below the header at chrome $chrome", ChatEdgeBackdrop.isBelowHeader(top, chrome))
            assertTrue("clear of the fade at chrome $chrome", ChatEdgeBackdrop.isClearOfBackdrop(top, chrome))
        }
        // A row scrolled up by even 1dp from that resting place starts to pass under the fade.
        val chrome = 128f
        assertFalse(ChatEdgeBackdrop.isClearOfBackdrop(ChatEdgeBackdrop.firstMessageTop(chrome) - 1f, chrome))
        assertFalse(ChatEdgeBackdrop.isBelowHeader(chrome - 1f, chrome))
    }

    @Test fun edgeBackdropLatestMessageIsFullyVisibleAboveTheDock() {
        val viewport = 874f
        // Dock heights: composer alone; with reply accessory; with the IME up (dock + IME).
        for (dock in listOf(96f, 160f, 420f)) {
            val bottom = ChatEdgeBackdrop.latestMessageBottom(viewport, dock)
            assertTrue("above the dock at $dock", bottom <= viewport - dock)
            assertTrue("clear of the fade at $dock", ChatEdgeBackdrop.isClearOfDockBackdrop(bottom, viewport, dock))
            assertFalse("1dp lower passes under the fade", ChatEdgeBackdrop.isClearOfDockBackdrop(bottom + 1f, viewport, dock))
        }
    }

    @Test fun edgeBackdropJumpToLatestHidesAtTheLatestMessageAndClearsTheDock() {
        // At rest at the latest message the scroll sits at its max (dock inset included): distance 0, hidden.
        assertEquals(0f, ChatEdgeBackdrop.distanceFromLatest(maxScroll = 2400f, scroll = 2400f), 0f)
        assertFalse(ChatJumpToLatest.isVisible(ChatEdgeBackdrop.distanceFromLatest(2400f, 2400f)))
        assertFalse(ChatJumpToLatest.isVisible(ChatEdgeBackdrop.distanceFromLatest(2400f, 2200f)))
        assertTrue(ChatJumpToLatest.isVisible(ChatEdgeBackdrop.distanceFromLatest(2400f, 2199f)))
        // The button keeps its 12dp gap above the dock's top edge.
        assertEquals(96f + 12f, ChatEdgeBackdrop.jumpButtonBottomPadding(96f), 0f)
        assertEquals(12f, ChatEdgeBackdrop.jumpButtonBottomPadding(0f), 0f)
    }

    @Test fun edgeBackdropScrimsFadeToClearJustPastTheChrome() {
        assertEquals(20f, ChatEdgeBackdrop.FADE_HEIGHT, 0f)
        assertEquals("fade overhang == transcript padding", ChatEdgeBackdrop.TRANSCRIPT_PADDING, ChatEdgeBackdrop.FADE_OVERHANG, 0f)
        assertEquals(0.85f, ChatEdgeBackdrop.SCRIM_EDGE_ALPHA, 0f)
        assertEquals(0f, ChatEdgeBackdrop.SCRIM_CLEAR_ALPHA, 0f)
        assertEquals(0.5f, ChatEdgeBackdrop.HAIRLINE, 0f)
        // Header: full to 8dp (the header's bottom gap) above its bottom edge, clear 12dp below it.
        val chrome = 128f
        assertEquals(140f, ChatEdgeBackdrop.backdropHeight(chrome), 0f)
        val top = ChatEdgeBackdrop.scrimStops(chrome)
        assertEquals(3, top.size)
        assertEquals(0f to 0.85f, top[0])
        assertEquals((chrome - 8f) / 140f, top[1].first, 1e-6f)
        assertEquals(0.85f, top[1].second, 0f)
        assertEquals(1f to 0f, top[2])
        assertEquals(listOf(0f to 0.85f, 0f to 0.85f, 1f to 0f), ChatEdgeBackdrop.scrimStops(-12f))
        // Dock: clear 12dp above its top edge, full from 8dp (the dock's top padding) below it to the edge.
        val dock = 96f
        assertEquals(108f, ChatEdgeBackdrop.dockBackdropHeight(dock), 0f)
        val bottom = ChatEdgeBackdrop.dockScrimStops(dock)
        assertEquals(3, bottom.size)
        assertEquals(0f to 0f, bottom[0])
        assertEquals(20f / 108f, bottom[1].first, 1e-6f)
        assertEquals(0.85f, bottom[1].second, 0f)
        assertEquals(1f to 0.85f, bottom[2])
        assertEquals(listOf(0f to 0f, 1f to 0.85f, 1f to 0.85f), ChatEdgeBackdrop.dockScrimStops(0f))
    }

    @Test fun edgeBackdropBlurMasksMatchTheScrimsAndTheGlassStaysLegible() {
        assertEquals(20f, ChatEdgeBackdrop.BLUR_RADIUS, 0f)
        assertEquals(0.7f, ChatEdgeBackdrop.BLUR_TINT_ALPHA, 0f)
        assertEquals(0.6f, ChatJumpToLatest.GLASS_TINT_ALPHA, 0f)
        for (h in listOf(0f, 96f, 128f, 187f)) {
            // The blur mask runs 1 -> 0 exactly where the API < 32 scrim runs 0.85 -> 0.
            assertEquals(ChatEdgeBackdrop.maskStops(h).map { it.first }, ChatEdgeBackdrop.scrimStops(h).map { it.first })
            assertEquals(listOf(1f, 1f, 0f), ChatEdgeBackdrop.maskStops(h).map { it.second })
            assertEquals(ChatEdgeBackdrop.dockMaskStops(h).map { it.first }, ChatEdgeBackdrop.dockScrimStops(h).map { it.first })
            assertEquals(listOf(0f, 1f, 1f), ChatEdgeBackdrop.dockMaskStops(h).map { it.second })
        }
    }

    // Jump to latest (paired with `ChatScreenModelTests.swift`).

    @Test fun jumpToLatestShowsOnlyBeyondTheThreshold() {
        assertEquals(200f, ChatJumpToLatest.THRESHOLD, 0f)
        assertFalse("at the bottom", ChatJumpToLatest.isVisible(0f))
        assertFalse("overscrolled past the bottom", ChatJumpToLatest.isVisible(-40f))
        assertFalse("near the bottom", ChatJumpToLatest.isVisible(200f))
        assertTrue(ChatJumpToLatest.isVisible(201f))
        assertTrue(ChatJumpToLatest.isVisible(2_000f))
    }

    @Test fun jumpToLatestGeometryAndAccessibilityMatchSwift() {
        assertEquals("minimum touch target", 44f, ChatJumpToLatest.DIAMETER, 0f)
        assertEquals(12f, ChatJumpToLatest.BOTTOM_SPACING, 0f)
        assertEquals(0.8f, ChatJumpToLatest.APPEAR_SCALE, 0f)
        assertEquals(200, ChatJumpToLatest.APPEAR_MILLIS)
        assertEquals("Scroll to latest message", ChatJumpToLatest.ACCESSIBILITY_LABEL)
        assertEquals("chat.jumpToLatest", ChatJumpToLatest.TEST_TAG)
    }

    // Swipe down to dismiss the keyboard (paired with `ChatScreenModelTests.swift`).

    @Test fun keyboardDismissNeedsAClearDownwardSwipe() {
        assertEquals(24f, ChatKeyboardDismiss.THRESHOLD, 0f)
        assertEquals(1.5f, ChatKeyboardDismiss.VERTICAL_DOMINANCE, 0f)
        assertTrue("straight down at the threshold", ChatKeyboardDismiss.isDismissSwipe(0f, 24f))
        assertTrue(ChatKeyboardDismiss.isDismissSwipe(10f, 60f))
        assertFalse("short of the threshold", ChatKeyboardDismiss.isDismissSwipe(0f, 23.9f))
        assertFalse("upward never dismisses", ChatKeyboardDismiss.isDismissSwipe(0f, -80f))
        assertFalse("no movement", ChatKeyboardDismiss.isDismissSwipe(0f, 0f))
    }

    @Test fun keyboardDismissNeedsVerticalTravelToDominate() {
        assertFalse("diagonal", ChatKeyboardDismiss.isDismissSwipe(40f, 40f))
        assertFalse("exactly at the dominance ratio", ChatKeyboardDismiss.isDismissSwipe(20f, 30f))
        assertTrue("just past the dominance ratio", ChatKeyboardDismiss.isDismissSwipe(20f, 30.1f))
        assertFalse("leftward slip counts the same", ChatKeyboardDismiss.isDismissSwipe(-40f, 50f))
        assertFalse("sideways", ChatKeyboardDismiss.isDismissSwipe(120f, 25f))
    }

    @Test fun keyboardDismissIgnoresPressAndHoldDrags() {
        assertEquals(350L, ChatKeyboardDismiss.HOLD_LIMIT_MILLIS)
        assertTrue(ChatKeyboardDismiss.isSwipeStart(0L))
        assertTrue(ChatKeyboardDismiss.isSwipeStart(349L))
        assertFalse("text selection starts with a hold", ChatKeyboardDismiss.isSwipeStart(350L))
        assertEquals("Dismiss keyboard", ChatKeyboardDismiss.ACCESSIBILITY_ACTION_NAME)
    }

    @Test fun transcriptDragTowardOlderMessagesDismissesOnlyWhileTheImeIsOpen() {
        var ime = true
        var dismissed = 0
        val connection = ChatTranscriptKeyboardDismissConnection(
            density = { 2f }, imeVisible = { ime }, onDismiss = { dismissed++ },
        )
        val user = NestedScrollSource.UserInput
        // 2px per dp: 20 + 20 px = 20dp, short of the threshold; the next 10px reaches 25dp.
        assertEquals("consumes nothing", Offset.Zero, connection.onPreScroll(Offset(0f, 20f), user))
        connection.onPreScroll(Offset(0f, 20f), user)
        assertEquals(0, dismissed)
        connection.onPreScroll(Offset(0f, 10f), user)
        assertEquals(1, dismissed)

        // Toward the latest message (finger up) resets and never dismisses.
        connection.onPreScroll(Offset(0f, 40f), user)
        connection.onPreScroll(Offset(0f, -200f), user)
        connection.onPreScroll(Offset(0f, 40f), user)
        assertEquals(1, dismissed)

        // Programmatic scrolls (jump to latest, following new content) never dismiss.
        connection.onPreScroll(Offset(0f, 400f), NestedScrollSource.SideEffect)
        assertEquals(1, dismissed)

        // IME closed: a drag does nothing.
        ime = false
        connection.onPreScroll(Offset(0f, 400f), user)
        assertEquals(1, dismissed)
    }

    @Test fun timeIsAlwaysAvailableToAssistiveTechnology() {
        assertEquals("Sent at 10:24", ChatTimestampReveal.accessibilityTime(
            ChatMessageDisplay(id = "u", role = MessageRole.User, text = "Hi", time = "10:24")))
        assertEquals("Received at 10:25", ChatTimestampReveal.accessibilityTime(
            ChatMessageDisplay(id = "a", role = MessageRole.Assistant, text = "Hi", time = "10:25")))
        assertNull(ChatTimestampReveal.accessibilityTime(ChatMessageDisplay(id = "x", role = MessageRole.User, text = "Hi")))
    }

    // Long-press message actions (`2603:19498`).

    @Test fun assistantMessageSheetMatchesTheReferenceGroups() {
        val sheet = ChatMessageActionsDisplay(
            ChatMessageDisplay(id = "a1", role = MessageRole.Assistant, text = "Hi", reaction = MessageReaction.Heart),
        )
        assertEquals("a1", sheet.messageId)
        assertEquals(
            listOf(
                listOf(ChatMessageAction.Reply, ChatMessageAction.MarkUnread),
                listOf(ChatMessageAction.Copy, ChatMessageAction.SelectText),
                listOf(ChatMessageAction.Report),
            ),
            sheet.groups,
        )
        assertEquals("The current reaction is the picker's selection", MessageReaction.Heart, sheet.selection)
        assertEquals("2 × 6 grid: eleven reactions and the + cell", 11, sheet.reactions.size)
        assertEquals(MessageReaction.StandardChoices, sheet.reactions.take(6))
        assertTrue(sheet.showsMoreReactions)
    }

    @Test fun ownMessagesNeverOfferReportOrMarkAsUnread() {
        val sheet = ChatMessageActionsDisplay(ChatMessageDisplay(id = "u1", role = MessageRole.User, text = "Hi"))
        assertEquals(listOf(listOf(ChatMessageAction.Reply), listOf(ChatMessageAction.Copy, ChatMessageAction.SelectText)), sheet.groups)
        assertFalse(sheet.groups.flatten().contains(ChatMessageAction.Report))
    }

    @Test fun unavailableActionsAreHiddenAndEmptyGroupsDisappear() {
        assertEquals(
            listOf(listOf(ChatMessageAction.Copy), listOf(ChatMessageAction.Report)),
            ChatMessageActionsDisplay.groups(MessageRole.Assistant, setOf(ChatMessageAction.Copy, ChatMessageAction.Report)),
        )
        assertEquals(
            "Report never reaches own messages",
            emptyList<List<ChatMessageAction>>(),
            ChatMessageActionsDisplay.groups(MessageRole.User, setOf(ChatMessageAction.Report)),
        )
    }

    @Test fun messageActionTitlesAndKeysFollowTheReference() {
        assertEquals(listOf("Reply", "Mark as unread", "Copy", "Select Text", "Report"), ChatMessageAction.entries.map { it.title })
        assertEquals(listOf("reply", "markUnread", "copy", "selectText", "report"), ChatMessageAction.entries.map { it.key })
    }
}
