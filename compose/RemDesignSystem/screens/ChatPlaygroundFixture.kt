package com.rem.designsystem.screens

import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageRole

// Full-screen Chat / task reply / Inbox fixture (presentation-only) — Compose twin of
// `Screens/ChatPlaygroundFixture.swift`. Feeds the canonical compositions exactly as an app adapter
// would, so the Playground and the app render the same components. It is NOT an app adapter and claims
// no runtime truth: a message sent here carries no receipt; Delivered / Read / Not delivered appear only
// through the explicitly named simulateHost… controls, at the illustrative [FixtureTime]. Immutable:
// every operation returns the next fixture.

/** What the page must do in response to a handled action (presentation, not effects). */
enum class ChatPlaygroundEffect { Exit, PresentHostControls, PresentAddToChat }

data class ChatPlaygroundFixture(
    val entries: List<ChatTranscriptEntry> = PopulatedEntries,
    val composer: ChatComposerFixture = ChatComposerFixture(),
    val replyContext: ChatReplyContext? = null,
    /** The Inbox item this conversation replies to; null for ordinary Chat. */
    val taskId: String? = null,
    val taskState: InboxItemState = InboxItemState.None,
    val note: String? = null,
    /**
     * The outgoing message the current turn belongs to (just sent or retried). Simulated host evidence
     * applies to this message — never to "whichever is latest" — so a retried older message is the one
     * accepted or failed. Null when no turn is in flight.
     */
    val activeOutgoingId: String? = null,
    /** Set while the active turn is a retry, so cancelling it restores Not delivered + Try again. */
    private val retryingId: String? = null,
    private val nextId: Int = 0,
) {
    enum class Conversation(val label: String) { Populated("Populated"), Empty("Empty") }

    val header: ChatHeaderDisplay
        get() = when {
            composer.state.phase.isInFlight -> ChatHeaderDisplay(activity = "Working on your request", isWorking = true)
            taskId == null -> ChatHeaderDisplay(activity = "Connected")
            else -> taskState.header()
        } // No call entry: it would replace overflow, the Playground's route to its host controls.

    val emptyState: ChatEmptyState?
        get() = if (entries.isEmpty()) ChatEmptyState(message = EmptyMessage, starters = Starters) else null

    /** Handle one composition action the way a host would; returns the next fixture and what to present. */
    fun handle(action: ChatScreenAction): Pair<ChatPlaygroundFixture, ChatPlaygroundEffect?> = when (action) {
        ChatScreenAction.Back -> this to ChatPlaygroundEffect.Exit
        ChatScreenAction.Overflow -> this to ChatPlaygroundEffect.PresentHostControls
        ChatScreenAction.ActivityDetails -> copy(note = ActivityNote) to null
        ChatScreenAction.Call -> copy(note = CallNote) to null
        is ChatScreenAction.Starter -> {
            val starter = Starters.firstOrNull { it.id == action.id }
            (if (starter == null) this else copy(composer = composer.apply(ChatComposerAction.DraftChanged(starter.title))).send()) to null
        }
        is ChatScreenAction.DismissReplyContext ->
            (if (replyContext?.targetId == action.targetId) copy(replyContext = null) else this) to null
        is ChatScreenAction.Composer -> when (action.action) {
            ChatComposerAction.Add -> this to ChatPlaygroundEffect.PresentAddToChat
            ChatComposerAction.Send -> send() to null
            ChatComposerAction.Cancel -> {
                val next = composer.apply(ChatComposerAction.Cancel)
                (if (next.state.phase.isInFlight) copy(composer = next) else copy(composer = next).endTurn(cancelled = true)) to null
            }
            else -> copy(composer = composer.apply(action.action)) to null
        }
        is ChatScreenAction.Transcript -> handle(action.action) to null
    }

    private fun handle(action: ChatTranscriptAction): ChatPlaygroundFixture = when (action) {
        is ChatTranscriptAction.Retry -> {
            val m = message(action.messageId)
            // One turn at a time: a retry never switches the active turn mid-flight.
            if (composer.state.phase.isInFlight || m == null || m.delivery != MessageDelivery.Failed || !m.canRetry) this
            else replace(m.copy(delivery = MessageDelivery.None, canRetry = false)).copy(
                composer = composer.select(ChatComposerFixture.Scenario.Sending),
                note = RetryNote,
                activeOutgoingId = m.id,
                retryingId = m.id,
            )
        }
        is ChatTranscriptAction.RequestReaction -> copy(note = ReactionNote)
        is ChatTranscriptAction.React -> message(action.messageId)?.let { replace(it.copy(reaction = action.reaction)) } ?: this
    }

    /** Send: append the outgoing message with no receipt and enter Sending. */
    private fun send(): ChatPlaygroundFixture {
        if (!composer.state.canSend) return this
        val next = composer.apply(ChatComposerAction.Send)
        val text = next.sent.lastOrNull() ?: return this
        val id = nextId + 1
        return copy(
            entries = entries + ChatTranscriptEntry.Message(ChatMessageDisplay("sent.$id", MessageRole.User, text)),
            composer = next.select(ChatComposerFixture.Scenario.Sending),
            note = null,
            activeOutgoingId = "sent.$id",
            nextId = id,
        )
    }

    // Fixture host (stand-ins for evidence an app would receive).

    /**
     * The host accepted the active turn's message: Delivered with the fixture time; reply streams. Where
     * the receipt shows is still decided by [ChatTranscriptRules] (latest outgoing only).
     */
    fun simulateHostAcceptance(): ChatPlaygroundFixture {
        if (composer.state.phase != ComposerPhase.Sending) return this
        val m = activeOutgoing() ?: return this
        if (m.delivery != MessageDelivery.None) return this
        return replace(m.copy(delivery = MessageDelivery.Delivered(FixtureTime)))
            .copy(composer = composer.select(ChatComposerFixture.Scenario.Streaming), retryingId = null)
    }

    /** The recipient explicitly acknowledged the latest outgoing message: Read keeps the delivered time. */
    fun simulateReadAcknowledgement(): ChatPlaygroundFixture {
        val m = latestOutgoing() ?: return this
        val delivered = m.delivery as? MessageDelivery.Delivered ?: return this
        return replace(m.copy(delivery = MessageDelivery.Read(delivered.at)))
    }

    /** The host reported the active turn's message as not delivered (retryable); the turn ends. */
    fun simulateDeliveryFailure(): ChatPlaygroundFixture {
        if (composer.state.phase != ComposerPhase.Sending) return this
        val m = activeOutgoing() ?: return this
        if (m.delivery != MessageDelivery.None) return this
        return replace(m.copy(delivery = MessageDelivery.Failed, canRetry = true))
            .endTurn(cancelled = false).copy(composer = composer.select(ChatComposerFixture.Scenario.Ready))
    }

    /** The reply finished: append a fixture assistant message and return to Ready. */
    fun simulateReplyComplete(): ChatPlaygroundFixture {
        if (composer.state.phase != ComposerPhase.Streaming) return this
        val id = nextId + 1
        return copy(
            entries = entries + ChatTranscriptEntry.Message(
                ChatMessageDisplay("reply.$id", MessageRole.Assistant, "Here is a fixture reply.", meta = "Automatic · Reply complete"),
            ),
            composer = composer.select(ChatComposerFixture.Scenario.Ready),
            activeOutgoingId = null,
            retryingId = null,
            nextId = id,
        )
    }

    fun message(id: String): ChatMessageDisplay? =
        (entries.firstOrNull { it.id == id } as? ChatTranscriptEntry.Message)?.message

    private fun latestOutgoing(): ChatMessageDisplay? = ChatTranscriptRules.latestOutgoingId(entries)?.let(::message)

    private fun activeOutgoing(): ChatMessageDisplay? = activeOutgoingId?.let(::message)

    /** Ends the active turn. A cancelled, not-yet-accepted retry returns to Not delivered + Try again. */
    private fun endTurn(cancelled: Boolean): ChatPlaygroundFixture {
        val retried = retryingId?.let(::message)
        val restored = if (cancelled && retried != null && retried.delivery == MessageDelivery.None) {
            replace(retried.copy(delivery = MessageDelivery.Failed, canRetry = true))
        } else this
        return restored.copy(activeOutgoingId = null, retryingId = null)
    }

    private fun replace(message: ChatMessageDisplay) = copy(
        entries = entries.map { if (it.id == message.id) ChatTranscriptEntry.Message(message) else it },
    )

    companion object {
        /** Illustrative fixture time for simulated host evidence. Not a clock reading. */
        const val FixtureTime = "10:24"
        const val ReplyTitle = "Replying to Rem"
        const val ActivityNote = "Agent activity details open in the app."
        const val CallNote = "The app starts an in-app voice session with Rem."
        const val ReactionNote = "The app presents the reaction picker."
        const val RetryNote = "Retry resubmitted; no receipt until the host accepts it."
        const val EmptyMessage = "Start a conversation with Rem. Plan your day, explore an idea or get a task moving."
        val Starters = listOf(ChatStarter("plan-day", "Help me plan my day"))

        /** Default populated conversation (`2054:21981`) with a consistent sample timeline. */
        val PopulatedEntries: List<ChatTranscriptEntry> = listOf(
            ChatTranscriptEntry.Timestamp("t1", "Today 10:20 AM"),
            ChatTranscriptEntry.Message(ChatMessageDisplay("u1", MessageRole.User, "Help me plan the rest of my day.", delivery = MessageDelivery.Delivered("10:20"), time = "10:20")),
            ChatTranscriptEntry.Message(ChatMessageDisplay("a1", MessageRole.Assistant, "Start with your highest-priority task, then leave time for your next commitment.", time = "10:21")),
            ChatTranscriptEntry.Message(ChatMessageDisplay("u2", MessageRole.User, "Turn this into a reminder for tomorrow.", delivery = MessageDelivery.Delivered(FixtureTime), time = FixtureTime)),
            ChatTranscriptEntry.Message(ChatMessageDisplay("a2", MessageRole.Assistant, "I can help you prepare that reminder.", meta = "Automatic · Reply complete", time = FixtureTime)),
        )

        fun conversation(conversation: Conversation) =
            ChatPlaygroundFixture(entries = if (conversation == Conversation.Empty) emptyList() else PopulatedEntries)

        /** Task chat for an Inbox item: the same composition plus the reply accessory and the item's state. */
        fun taskReply(item: InboxItemDisplay) = ChatPlaygroundFixture(
            entries = listOf(
                ChatTranscriptEntry.Message(ChatMessageDisplay("task.u1", MessageRole.User, "Could you make the next step clearer?", delivery = MessageDelivery.Delivered(FixtureTime))),
                ChatTranscriptEntry.Message(ChatMessageDisplay("task.a1", MessageRole.Assistant, "I'll outline a clear next step for this task.")),
            ),
            composer = ChatComposerFixture(state = ChatComposerState(placeholder = "Write your reply…")),
            replyContext = ChatReplyContext(item.id, ReplyTitle, item.title),
            taskId = item.id,
            taskState = item.state,
        )
    }
}

/** The Playground Inbox: host-reported item states and routing into task chat. */
data class InboxPlaygroundFixture(val items: List<InboxItemDisplay> = SeedItems) {
    enum class Content(val label: String) { Items("Items"), Empty("Empty") }

    /** The task chat an Inbox action opens, or null for an unknown id. */
    fun route(action: InboxAction): ChatPlaygroundFixture? = when (action) {
        is InboxAction.Open -> items.firstOrNull { it.id == action.itemId }?.let { ChatPlaygroundFixture.taskReply(it) }
    }

    companion object {
        const val EmptyMessage = "Nothing in your Inbox."
        val SeedItems = listOf(
            InboxItemDisplay("plan-next-step", "Plan the next step"),
            InboxItemDisplay("investor-update", "Draft the investor update", InboxItemState.Executing),
            InboxItemDisplay("venue-booking", "Approve the venue booking", InboxItemState.NeedsApproval),
            InboxItemDisplay("offsite-travel", "Book travel for the offsite", InboxItemState.Blocked),
            InboxItemDisplay("calendar-holds", "Sync calendar holds", InboxItemState.Unknown),
            InboxItemDisplay("weekly-summary", "Send the weekly summary", InboxItemState.Completed),
        )

        fun content(content: Content) = InboxPlaygroundFixture(if (content == Content.Empty) emptyList() else SeedItems)
    }
}
