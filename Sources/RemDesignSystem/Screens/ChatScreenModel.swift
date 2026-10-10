import Foundation

// MARK: - Chat / Inbox presentation contract
//
// The typed boundary between a consuming app and the canonical Chat, Task detail and Inbox
// compositions. The design system **renders** these values and **emits** the actions; it never
// authenticates, sends, persists, executes tools or decides what a receipt means.
//
// Ownership (see `docs/contracts/chat-adapter.md`):
//   • App            — auth, conversations, tool execution, networking, persistence, business state,
//                      and the authoritative delivery / read evidence.
//   • Thin adapter   — maps app state to these values and these actions back to app operations. The
//                      evidence → `MessageBubble.Delivery` mapping lives here, never in the DS.
//   • Design system  — layout, composition and interaction presentation states for the values given.
//
// No SwiftUI here: every rule below is pure and testable without a UI host (it uses only the plain
// value types `ComposerAttachment`, `MessageReaction` and `MessageBubble.Delivery`). Compose twin:
// `screens/ChatScreenModel.kt` (same names, same rules, asserted by `ChatScreenModelTest.kt`).

/// Whether the host currently accepts composer input. `disabled` is an **external** decision (signed
/// out, conversation unavailable, offline…) supplied by the host; the composer never infers it.
public enum ComposerAvailability: Equatable, Sendable {
    case enabled
    /// The host refuses input. `reason` is host copy, exposed to assistive technologies.
    case disabled(reason: String?)

    public var isEnabled: Bool { self == .enabled }
}

/// The host's turn phase. Sending and streaming are separate states with separate meanings; both are
/// cancellable through `ChatComposerAction.cancel`, never through `.send`.
public enum ComposerPhase: Equatable, Sendable {
    /// No turn in flight. Send is available when `ChatComposerState.canSend`.
    case idle
    /// The host has submitted a turn and has not yet started receiving a reply.
    case sending
    /// The host is receiving the reply.
    case streaming

    public var isInFlight: Bool { self != .idle }
}

/// Everything the canonical composer renders, supplied by the host. No field is stored or transmitted
/// by the design system.
public struct ChatComposerState: Equatable, Sendable {
    public var draft: String
    public var placeholder: String
    /// Model trigger label, typically from `ChatModelSelection.triggerLabel(in:)`.
    public var modelLabel: String
    public var attachments: [ComposerAttachment]
    public var availability: ComposerAvailability
    public var phase: ComposerPhase
    public var showsModel: Bool
    /// Voice input is offered only when the host can start it; Speak then emits `.speak`.
    public var voiceAvailable: Bool
    /// Host-owned focus. The composer reports changes through `.focusChanged` and follows this value.
    public var isFocused: Bool

    public init(
        draft: String = "",
        placeholder: String = "Ask anything",
        modelLabel: String = "Auto",
        attachments: [ComposerAttachment] = [],
        availability: ComposerAvailability = .enabled,
        phase: ComposerPhase = .idle,
        showsModel: Bool = true,
        voiceAvailable: Bool = true,
        isFocused: Bool = false
    ) {
        self.draft = draft
        self.placeholder = placeholder
        self.modelLabel = modelLabel
        self.attachments = attachments
        self.availability = availability
        self.phase = phase
        self.showsModel = showsModel
        self.voiceAvailable = voiceAvailable
        self.isFocused = isFocused
    }

    /// True when the draft has visible text.
    public var hasText: Bool { !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    /// Content attachments (images, files) make a message on their own; a capability chip such as
    /// Cloud browser only modifies the next turn and never makes one.
    public var hasContentAttachments: Bool { attachments.contains { $0.kind != .capability } }

    /// The single send rule: enabled, no turn in flight, and text or a content attachment.
    public var canSend: Bool {
        availability.isEnabled && !phase.isInFlight && (hasText || hasContentAttachments)
    }

    /// Cancel is offered whenever a turn is in flight, even if new input is externally disabled.
    public var canCancel: Bool { phase.isInFlight }

    /// Speak is shown only when voice is available, input is enabled and no turn is in flight.
    public var showsSpeak: Bool { voiceAvailable && availability.isEnabled && !phase.isInFlight }

    /// The model trigger is shown disabled (45%) while a turn is in flight or input is disabled.
    public var modelEnabled: Bool { availability.isEnabled && !phase.isInFlight }

    /// What the trailing control does right now: `.send`, `.cancel`, or nothing (disabled).
    public var primaryAction: ChatComposerAction? {
        if canCancel { return .cancel }
        return canSend ? .send : nil
    }

    /// The trailing control's presentation, derived from the rules above.
    public var sendDisplay: ComposerSendDisplay {
        if phase.isInFlight { return .stop }
        return canSend ? .send : .unavailable
    }
}

/// The trailing control's three presentations.
public enum ComposerSendDisplay: Equatable, Sendable {
    /// Grey ↑, disabled.
    case unavailable
    /// Brand-blue ↑, emits `.send`.
    case send
    /// Red ■, emits `.cancel`.
    case stop
}

/// Every interaction the canonical composer reports. The host decides what each one does.
public enum ChatComposerAction: Equatable, Sendable {
    case draftChanged(String)
    case send
    case cancel
    case speak
    case add
    case removeAttachment(id: String)
    case focusChanged(Bool)
}

// MARK: - Transcript

/// One transcript message as the design system renders it. `delivery` is **supplied by the host's
/// adapter** from its own acceptance / acknowledgement evidence; the DS renders it verbatim.
public struct ChatMessageDisplay: Identifiable, Equatable, Sendable {
    public let id: String
    public var role: MessageBubble.Role
    public var text: String
    public var meta: String?
    public var delivery: MessageBubble.Delivery
    public var reaction: MessageReaction?
    /// Offer Try again on a failed message only when the host can actually retry it.
    public var canRetry: Bool
    /// Host-formatted send/receive time shown in the swipe-to-reveal timestamp column (e.g. "10:24").
    /// The DS never reads a clock or formats dates; `nil` shows no timestamp for this message.
    public var time: String?

    public init(
        id: String, role: MessageBubble.Role, text: String, meta: String? = nil,
        delivery: MessageBubble.Delivery = .none, reaction: MessageReaction? = nil, canRetry: Bool = false,
        time: String? = nil
    ) {
        self.id = id
        self.role = role
        self.text = text
        self.meta = meta
        self.delivery = role == .user ? delivery : .none
        self.reaction = reaction
        self.canRetry = canRetry
        self.time = time
    }
}

/// Interactions on a transcript message.
public enum ChatTranscriptAction: Equatable, Sendable {
    /// Try again on a failed outgoing message (only offered when `canRetry`).
    case retry(messageID: String)
    /// The person asked to react (long press / accessibility "React"); the host presents the picker.
    case requestReaction(messageID: String)
    /// The person chose (or, with `nil`, cleared) a reaction.
    case react(messageID: String, reaction: MessageReaction?)
}

// MARK: - Inbox

/// A task's run state as the **host** reports it. The DS never infers one; `unknown` is the honest
/// state when the host cannot establish an outcome (for example an action whose result was lost).
public enum InboxItemState: String, CaseIterable, Equatable, Sendable {
    /// No run in progress or pending; no status is shown.
    case none
    case loading
    case executing
    case needsApproval
    case blocked
    case unknown
    case completed

    /// The status label shown on the row and carried into the task chat header; `nil` for `.none`.
    public var statusLabel: String? {
        switch self {
        case .none: return nil
        case .loading: return "Loading"
        case .executing: return "Working"
        case .needsApproval: return "Needs approval"
        case .blocked: return "Needs you"
        case .unknown: return "Status unknown"
        case .completed: return "Done"
        }
    }

    /// Approval and blocked states ask for the person; the rest are informational.
    public var needsPerson: Bool { self == .needsApproval || self == .blocked }

    /// The task chat header for this state, so the chat shows the same truth the Inbox row showed.
    public func header(name: String = "Rem") -> ChatHeaderDisplay {
        ChatHeaderDisplay(
            name: name,
            activity: statusLabel ?? "Connected",
            status: needsPerson ? .needsYou : .connected,
            isWorking: self == .executing || self == .loading
        )
    }
}

/// One unfiled Inbox item as rendered by `InboxScreen`.
public struct InboxItemDisplay: Identifiable, Equatable, Sendable {
    public let id: String
    public var title: String
    public var state: InboxItemState

    public init(id: String, title: String, state: InboxItemState = .none) {
        self.id = id
        self.title = title
        self.state = state
    }
}

/// Interactions on the Inbox list. Routing (which task, which screen) stays in the host.
public enum InboxAction: Equatable, Sendable {
    case open(itemID: String)
}

// MARK: - Full-screen composition (Figma shell `2054:21958`, board `2681:21977`)

/// What the shared header (`2054:19725`) renders: one avatar, one compact identity / activity capsule,
/// and the header-owned back and overflow controls. `activity` is host copy for the **agent's**
/// lifecycle; it is not transport or connection evidence.
public struct ChatHeaderDisplay: Equatable, Sendable {
    public var name: String
    public var activity: String
    public var status: ChatHeader.Status
    /// The avatar shows the thinking face while the host reports the agent as working.
    public var isWorking: Bool
    public var showsBack: Bool
    public var showsOverflow: Bool
    /// The capsule disclosure opens the host's agent activity details.
    public var showsActivityDetails: Bool
    /// Top-right call button (WS1e): the host shows it only when it can start an in-app voice session.
    /// Off by default; the DS only emits `.call`, voice admission stays with the host.
    public var showsCall: Bool

    public init(
        name: String = "Rem", activity: String, status: ChatHeader.Status = .connected, isWorking: Bool = false,
        showsBack: Bool = true, showsOverflow: Bool = true, showsActivityDetails: Bool = true, showsCall: Bool = false
    ) {
        self.name = name
        self.activity = activity
        self.status = status
        self.isWorking = isWorking
        self.showsBack = showsBack
        self.showsOverflow = showsOverflow
        self.showsActivityDetails = showsActivityDetails
        self.showsCall = showsCall
    }
}

/// The task-reply context (`2682:22298`): a dismissible accessory **above the same composer**, never a
/// composer variant. `targetID` is the host's selected reply target; task and conversation identifiers
/// stay in the host. Dismissing reports `targetID` so the host clears only that target.
public struct ChatReplyContext: Equatable, Sendable {
    public var targetID: String
    /// "Replying to#2682:0".
    public var title: String
    /// "Reply summary#2682:1".
    public var summary: String

    public init(targetID: String, title: String, summary: String) {
        self.targetID = targetID
        self.title = title
        self.summary = summary
    }
}

/// One empty-chat starter. Starters are host data; offering one implies nothing about capability —
/// the host lists only what it can actually do.
public struct ChatStarter: Identifiable, Equatable, Sendable {
    public let id: String
    public var title: String

    public init(id: String, title: String) {
        self.id = id
        self.title = title
    }
}

/// The empty conversation (`2054:22089`): a title, a message and starters — **no second face**; the
/// header's avatar is the only identity on screen.
public struct ChatEmptyState: Equatable, Sendable {
    public var title: String
    public var message: String
    public var starters: [ChatStarter]

    public init(title: String = "What can I help with?", message: String, starters: [ChatStarter] = []) {
        self.title = title
        self.message = message
        self.starters = starters
    }
}

/// One transcript entry for `ChatTranscriptList`.
public enum ChatTranscriptEntry: Identifiable, Equatable, Sendable {
    case message(ChatMessageDisplay)
    /// A centred, host-formatted time separator (e.g. "Today 3:25 PM").
    case timestamp(id: String, text: String)

    public var id: String {
        switch self {
        case .message(let message): return message.id
        case .timestamp(let id, _): return id
        }
    }
}

/// Presentation rules for receipts across a transcript. They decide only **where** a host-supplied
/// state is shown, never what it is: the latest outgoing message alone carries its Delivered / Read
/// receipt; older outgoing messages show none; a failure stays visible wherever it is, so it can be
/// retried.
public enum ChatTranscriptRules {
    /// The id of the latest outgoing message, if any.
    public static func latestOutgoingID(in entries: [ChatTranscriptEntry]) -> String? {
        for entry in entries.reversed() {
            if case .message(let message) = entry, message.role == .user { return message.id }
        }
        return nil
    }

    /// The delivery to render for `message` within a transcript whose latest outgoing id is `latest`.
    public static func displayedDelivery(for message: ChatMessageDisplay, latestOutgoingID latest: String?) -> MessageBubble.Delivery {
        guard message.role == .user else { return .none }
        if message.delivery == .failed { return .failed }
        return message.id == latest ? message.delivery : .none
    }
}

/// Swipe-left-to-reveal timestamps (approved Chat requirement WS1d): one shared offset translates every
/// transcript row together while header, composer and keyboard stay fixed. Pure rules so both platforms
/// agree. Thresholds, rubber-band factor and snapback are platform-convention choices marked for review;
/// the reference screenshot does not establish them. Twin of Compose `ChatTimestampReveal`.
public enum ChatTimestampReveal {
    /// Minimum leftward travel before the transcript claims the drag.
    public static let minTravel: CGFloat = 10
    /// Horizontal travel must exceed vertical travel by this ratio, so vertical scrolling stays untouched.
    public static let horizontalRatio: CGFloat = 1.5
    /// Resistance applied to travel beyond the column width (no overshoot with Reduce Motion).
    public static let rubberBand: CGFloat = 0.3
    /// Rows always return when the finger lifts: the reveal is a peek, never a persistent state.
    public static let settled: CGFloat = 0

    /// Whether a drag of (`dx`, `dy`) is a leftward reveal rather than a scroll or a rightward swipe.
    public static func isRevealDrag(dx: CGFloat, dy: CGFloat) -> Bool {
        dx <= -minTravel && abs(dx) > abs(dy) * horizontalRatio
    }

    /// How far rows move left (>= 0) for a drag translation `dx`, given the timestamp `columnWidth`.
    public static func reveal(dx: CGFloat, columnWidth: CGFloat, reduceMotion: Bool) -> CGFloat {
        let travel = max(0, -dx)
        guard travel > columnWidth else { return travel }
        return reduceMotion ? columnWidth : columnWidth + (travel - columnWidth) * rubberBand
    }

    /// Accessible description of a message's time, so timestamps never require the gesture.
    public static func accessibilityTime(_ message: ChatMessageDisplay) -> String? {
        guard let time = message.time else { return nil }
        return message.role == .user ? "Sent at \(time)" : "Received at \(time)"
    }
}

/// Every interaction the full Chat composition reports. Routing and effects stay in the host.
public enum ChatScreenAction: Equatable, Sendable {
    case back
    case overflow
    /// The header call button: start the host's in-app voice session with Rem (never PSTN).
    case call
    /// The header capsule disclosure: open agent activity details.
    case activityDetails
    case starter(id: String)
    /// Clear the reply target `targetID` only.
    case dismissReplyContext(targetID: String)
    case composer(ChatComposerAction)
    case transcript(ChatTranscriptAction)
}
