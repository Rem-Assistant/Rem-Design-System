package com.rem.designsystem.screens

import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageRole

// Chat / Inbox presentation contract — Compose twin of `Screens/ChatScreenModel.swift`.
//
// The typed boundary between a consuming app and the canonical Chat, Task detail and Inbox
// compositions. The design system renders these values and emits the actions; it never authenticates,
// sends, persists, executes tools or decides what a receipt means. The app owns the authoritative
// delivery / read evidence; its thin adapter maps that evidence to [MessageDelivery]
// (see `docs/contracts/chat-adapter.md`). No Compose import: every rule is pure and unit-tested by
// `ChatScreenModelTest.kt`, paired with the SwiftUI `ChatScreenModelTests.swift`.

/** Whether the host accepts composer input. [Disabled] is an external host decision, never inferred. */
sealed interface ComposerAvailability {
    data object Enabled : ComposerAvailability
    /** The host refuses input; [reason] is host copy, exposed to assistive technologies. */
    data class Disabled(val reason: String? = null) : ComposerAvailability

    val isEnabled: Boolean get() = this == Enabled
}

/** The host's turn phase. Sending and streaming are separate; both are cancelled through [ChatComposerAction.Cancel]. */
enum class ComposerPhase {
    /** No turn in flight. */
    Idle,
    /** Submitted, no reply received yet. */
    Sending,
    /** Receiving the reply. */
    Streaming;

    val isInFlight: Boolean get() = this != Idle
}

/** The trailing control's three presentations. */
enum class ComposerSendDisplay { Unavailable, Send, Stop }

/** Everything the canonical composer renders, supplied by the host. Nothing is stored or transmitted. */
data class ChatComposerState(
    val draft: String = "",
    val placeholder: String = "Ask anything",
    val modelLabel: String = "Auto",
    val attachments: List<ComposerAttachment> = emptyList(),
    val availability: ComposerAvailability = ComposerAvailability.Enabled,
    val phase: ComposerPhase = ComposerPhase.Idle,
    val showsModel: Boolean = true,
    /** Voice input is offered only when the host can start it; Speak then emits [ChatComposerAction.Speak]. */
    val voiceAvailable: Boolean = true,
    /** Host-owned focus; the composer reports changes through [ChatComposerAction.FocusChanged]. */
    val isFocused: Boolean = false,
) {
    val hasText: Boolean get() = draft.isNotBlank()

    /** Images and files make a message on their own; a capability chip (Cloud browser) never does. */
    val hasContentAttachments: Boolean get() = attachments.any { it.kind != ComposerAttachment.Kind.Capability }

    /** The single send rule: enabled, no turn in flight, and text or a content attachment. */
    val canSend: Boolean get() = availability.isEnabled && !phase.isInFlight && (hasText || hasContentAttachments)

    /** Cancel is offered whenever a turn is in flight, even if new input is externally disabled. */
    val canCancel: Boolean get() = phase.isInFlight

    val showsSpeak: Boolean get() = voiceAvailable && availability.isEnabled && !phase.isInFlight

    /** The model trigger is disabled (45%) while a turn is in flight or input is disabled. */
    val modelEnabled: Boolean get() = availability.isEnabled && !phase.isInFlight

    /** What the trailing control does right now: Send, Cancel, or nothing. */
    val primaryAction: ChatComposerAction?
        get() = when {
            canCancel -> ChatComposerAction.Cancel
            canSend -> ChatComposerAction.Send
            else -> null
        }

    val sendDisplay: ComposerSendDisplay
        get() = when {
            phase.isInFlight -> ComposerSendDisplay.Stop
            canSend -> ComposerSendDisplay.Send
            else -> ComposerSendDisplay.Unavailable
        }
}

/** Every interaction the canonical composer reports. The host decides what each one does. */
sealed interface ChatComposerAction {
    data class DraftChanged(val text: String) : ChatComposerAction
    data object Send : ChatComposerAction
    data object Cancel : ChatComposerAction
    data object Speak : ChatComposerAction
    data object Add : ChatComposerAction
    data class RemoveAttachment(val id: String) : ChatComposerAction
    data class FocusChanged(val focused: Boolean) : ChatComposerAction
}

/**
 * One transcript message as rendered. [delivery] is supplied by the host's adapter from its own
 * acceptance / acknowledgement evidence and rendered verbatim; assistant messages never carry one.
 */
class ChatMessageDisplay(
    val id: String,
    val role: MessageRole,
    val text: String,
    val meta: String? = null,
    delivery: MessageDelivery = MessageDelivery.None,
    val reaction: MessageReaction? = null,
    /** Offer Try again on a failed message only when the host can actually retry it. */
    val canRetry: Boolean = false,
    /**
     * Host-formatted send/receive time shown in the swipe-to-reveal timestamp column (e.g. "10:24").
     * The DS never reads a clock or formats dates; null shows no timestamp for this message.
     */
    val time: String? = null,
) {
    val delivery: MessageDelivery = if (role == MessageRole.User) delivery else MessageDelivery.None

    fun copy(
        text: String = this.text,
        meta: String? = this.meta,
        delivery: MessageDelivery = this.delivery,
        reaction: MessageReaction? = this.reaction,
        canRetry: Boolean = this.canRetry,
        time: String? = this.time,
    ) = ChatMessageDisplay(id, role, text, meta, delivery, reaction, canRetry, time)

    override fun equals(other: Any?): Boolean = other is ChatMessageDisplay &&
        id == other.id && role == other.role && text == other.text && meta == other.meta &&
        delivery == other.delivery && reaction == other.reaction && canRetry == other.canRetry && time == other.time

    override fun hashCode(): Int = listOf(id, role, text, meta, delivery, reaction, canRetry, time).hashCode()
}

/** Interactions on a transcript message. */
sealed interface ChatTranscriptAction {
    data class Retry(val messageId: String) : ChatTranscriptAction
    /**
     * Long press (accessibility "Message actions"): the host presents [MessageActionSheet] for the
     * message, built from [ChatMessageActionsDisplay].
     */
    data class RequestActions(val messageId: String) : ChatTranscriptAction
    /** The person chose (or, with null, cleared) a reaction. */
    data class React(val messageId: String, val reaction: MessageReaction?) : ChatTranscriptAction
    /** The sheet's `+` cell: the host presents its full emoji picker. */
    data class RequestMoreReactions(val messageId: String) : ChatTranscriptAction
    /**
     * A sheet row. Presentation only — the host performs it (reply target, unread state, clipboard,
     * text selection, report flow) and dismisses the sheet.
     */
    data class MessageAction(val messageId: String, val action: ChatMessageAction) : ChatTranscriptAction
}

// Long-press message actions (Figma `2603:19498`).

/**
 * One row of the long-press message sheet — twin of SwiftUI `ChatMessageAction`. The DS only reports
 * the choice; the host performs it. [key] is the tag segment ("message.<id>.actions.<key>").
 */
enum class ChatMessageAction(val key: String, val title: String) {
    Reply("reply", "Reply"),
    MarkUnread("markUnread", "Mark as unread"),
    Copy("copy", "Copy"),
    SelectText("selectText", "Select Text"),
    Report("report", "Report"),
}

/**
 * What [MessageActionSheet] renders for one message: the 2 × 6 reaction grid and the grouped rows —
 * twin of SwiftUI `ChatMessageActionsDisplay`. The reference is an assistant message ([Reply, Mark as
 * unread], [Copy, Select Text], [Report]). The person's own messages show only what applies to them:
 * Report is never offered on them, and Mark as unread has no meaning for a message they sent. Rows the
 * host cannot perform are left out through `available`; an emptied group disappears.
 */
data class ChatMessageActionsDisplay(
    val messageId: String,
    val role: MessageRole,
    /** The message's current reaction; choosing it again clears it. */
    val selection: MessageReaction?,
    val reactions: List<MessageReaction>,
    /** The trailing `+` cell, shown only when the host can present a full emoji picker. */
    val showsMoreReactions: Boolean,
    val groups: List<List<ChatMessageAction>>,
) {
    constructor(
        message: ChatMessageDisplay,
        available: Set<ChatMessageAction> = ChatMessageAction.entries.toSet(),
        reactions: List<MessageReaction> = MessageReaction.SheetChoices,
        showsMoreReactions: Boolean = true,
    ) : this(
        messageId = message.id,
        role = message.role,
        selection = message.reaction,
        reactions = reactions,
        showsMoreReactions = showsMoreReactions,
        groups = groups(message.role, available),
    )

    companion object {
        /** The grouped rows for [role], in reference order, keeping only [available] actions. */
        fun groups(role: MessageRole, available: Set<ChatMessageAction>): List<List<ChatMessageAction>> {
            val reference = if (role == MessageRole.Assistant) {
                listOf(
                    listOf(ChatMessageAction.Reply, ChatMessageAction.MarkUnread),
                    listOf(ChatMessageAction.Copy, ChatMessageAction.SelectText),
                    listOf(ChatMessageAction.Report),
                )
            } else {
                listOf(listOf(ChatMessageAction.Reply), listOf(ChatMessageAction.Copy, ChatMessageAction.SelectText))
            }
            return reference.map { group -> group.filter { it in available } }.filter { it.isNotEmpty() }
        }
    }
}

/**
 * A task's run state as the host reports it — twin of SwiftUI `InboxItemState`. The DS never infers one;
 * [Unknown] is the honest state when the host cannot establish an outcome.
 */
enum class InboxItemState {
    None, Loading, Executing, NeedsApproval, Blocked, Unknown, Completed;

    /** The row status label and task chat activity; null for [None]. */
    val statusLabel: String?
        get() = when (this) {
            None -> null
            Loading -> "Loading"
            Executing -> "Working"
            NeedsApproval -> "Needs approval"
            Blocked -> "Needs you"
            Unknown -> "Status unknown"
            Completed -> "Done"
        }

    val needsPerson: Boolean get() = this == NeedsApproval || this == Blocked

    /** The task chat header for this state, so the chat shows the same truth the Inbox row showed. */
    fun header(name: String = "Rem") = ChatHeaderDisplay(
        activity = statusLabel ?: "Connected",
        name = name,
        status = if (needsPerson) ChatHeaderStatus.NeedsYou else ChatHeaderStatus.Connected,
        isWorking = this == Executing || this == Loading,
    )
}

/** One unfiled Inbox item as rendered by [InboxScreen]. */
data class InboxItemDisplay(val id: String, val title: String, val state: InboxItemState = InboxItemState.None)

/** Interactions on the Inbox list. Routing stays in the host. */
sealed interface InboxAction {
    data class Open(val itemId: String) : InboxAction
}

// Full-screen composition (Figma shell `2054:21958`, board `2681:21977`).

/**
 * What the shared header (`2054:19725`) renders: one avatar, one compact identity / activity capsule,
 * and the header-owned back and overflow controls. [activity] is host copy for the agent's lifecycle,
 * not transport or connection evidence.
 */
data class ChatHeaderDisplay(
    val activity: String,
    val name: String = "Rem",
    val status: ChatHeaderStatus = ChatHeaderStatus.Connected,
    /** The avatar shows the thinking face while the host reports the agent as working. */
    val isWorking: Boolean = false,
    val showsBack: Boolean = true,
    val showsOverflow: Boolean = true,
    /** The capsule disclosure opens the host's agent activity details. */
    val showsActivityDetails: Boolean = true,
    /**
     * Top-right call button (WS1e): the host shows it only when it can start an in-app voice session.
     * Off by default; the DS only emits [ChatScreenAction.Call], voice admission stays with the host.
     */
    val showsCall: Boolean = false,
)

/**
 * The task-reply context (`2682:22298`): a dismissible accessory above the same composer, never a
 * composer variant. [targetId] is the host's selected reply target; task and conversation ids stay in
 * the host. Dismiss reports [targetId] so the host clears only that target.
 */
data class ChatReplyContext(
    val targetId: String,
    /** "Replying to#2682:0". */
    val title: String,
    /** "Reply summary#2682:1". */
    val summary: String,
)

/** One empty-chat starter. Host data; offering one implies nothing about capability. */
data class ChatStarter(val id: String, val title: String)

/** The empty conversation (`2054:22089`): title, message, starters — no second face. */
data class ChatEmptyState(
    val message: String,
    val title: String = "What can I help with?",
    val starters: List<ChatStarter> = emptyList(),
)

/** One transcript entry for [ChatTranscriptList]. */
sealed interface ChatTranscriptEntry {
    val id: String

    data class Message(val message: ChatMessageDisplay) : ChatTranscriptEntry {
        override val id: String get() = message.id
    }

    /** A centred, host-formatted time separator (e.g. "Today 3:25 PM"). */
    data class Timestamp(override val id: String, val text: String) : ChatTranscriptEntry

    /**
     * A host-rendered card at this position in the transcript (a proposal with approve buttons, a tool
     * result, the daily brief). The DS draws nothing for it; [ChatTranscriptList]'s `hostContent` renders
     * it by [id]. It carries no receipt and never counts as a message.
     */
    data class HostContent(override val id: String) : ChatTranscriptEntry
}

/**
 * Receipt placement across a transcript — decides only where a host-supplied state is shown, never
 * what it is: the latest outgoing message alone carries its Delivered / Read receipt; failures stay
 * visible so they can be retried.
 */
object ChatTranscriptRules {
    fun latestOutgoingId(entries: List<ChatTranscriptEntry>): String? =
        entries.asReversed().firstNotNullOfOrNull { entry ->
            (entry as? ChatTranscriptEntry.Message)?.message?.takeIf { it.role == MessageRole.User }?.id
        }

    fun displayedDelivery(message: ChatMessageDisplay, latestOutgoingId: String?): MessageDelivery = when {
        message.role != MessageRole.User -> MessageDelivery.None
        message.delivery == MessageDelivery.Failed -> MessageDelivery.Failed
        message.id == latestOutgoingId -> message.delivery
        else -> MessageDelivery.None
    }
}

/**
 * Swipe-left-to-reveal timestamps (approved Chat requirement WS1d): one shared offset translates every
 * transcript row together while header, composer and keyboard stay fixed. Pure rules so both platforms
 * agree. Thresholds, rubber-band factor and snapback are platform-convention choices marked for review;
 * the reference screenshot does not establish them. Twin of SwiftUI `ChatTimestampReveal`.
 */
object ChatTimestampReveal {
    /** Minimum leftward travel before the transcript claims the drag. */
    const val MIN_TRAVEL = 10f
    /** Horizontal travel must exceed vertical travel by this ratio, so vertical scrolling stays untouched. */
    const val HORIZONTAL_RATIO = 1.5f
    /** Resistance applied to travel beyond the column width (no overshoot with Reduce Motion). */
    const val RUBBER_BAND = 0.3f

    /** Whether a drag of ([dx], [dy]) is a leftward reveal rather than a scroll or a rightward swipe. */
    fun isRevealDrag(dx: Float, dy: Float): Boolean =
        dx <= -MIN_TRAVEL && kotlin.math.abs(dx) > kotlin.math.abs(dy) * HORIZONTAL_RATIO

    /** How far rows move left (>= 0) for a drag translation [dx], given the timestamp [columnWidth]. */
    fun reveal(dx: Float, columnWidth: Float, reduceMotion: Boolean): Float {
        val travel = (-dx).coerceAtLeast(0f)
        if (travel <= columnWidth) return travel
        return if (reduceMotion) columnWidth else columnWidth + (travel - columnWidth) * RUBBER_BAND
    }

    /** Rows always return when the finger lifts: the reveal is a peek, never a persistent state. */
    const val SETTLED = 0f

    /** Accessible description of a message including its time, so timestamps never require the gesture. */
    fun accessibilityTime(message: ChatMessageDisplay): String? = message.time?.let {
        if (message.role == MessageRole.User) "Sent at $it" else "Received at $it"
    }
}

/** Every interaction the full Chat composition reports. Routing and effects stay in the host. */
sealed interface ChatScreenAction {
    data object Back : ChatScreenAction
    data object Overflow : ChatScreenAction
    /** The header call button: start the host's in-app voice session with Rem (never PSTN). */
    data object Call : ChatScreenAction
    /** The header capsule disclosure: open agent activity details. */
    data object ActivityDetails : ChatScreenAction
    data class Starter(val id: String) : ChatScreenAction
    /** Clear the reply target [targetId] only. */
    data class DismissReplyContext(val targetId: String) : ChatScreenAction
    data class Composer(val action: ChatComposerAction) : ChatScreenAction
    data class Transcript(val action: ChatTranscriptAction) : ChatScreenAction
}
