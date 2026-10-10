import Foundation

// MARK: - Full-screen Chat / task reply / Inbox fixture (presentation-only)
//
// The deterministic, in-memory host behind the Playground's full-screen Chat, task-reply and Inbox
// journeys, mirroring the Settings fixture pattern. It feeds the canonical compositions exactly as an
// app adapter would — `ChatScreen(header:composer:…onAction:transcript:)` and
// `InboxScreen(items:onAction:empty:)` — so the Playground and the app render the same components.
//
// It is NOT an app adapter and claims no runtime truth. A message sent here carries **no receipt**
// (no host acceptance exists). Delivered / Read / Not delivered appear only through the explicitly
// named `simulateHost…` controls, which stand in for evidence an app would receive; their time is the
// illustrative `fixtureTime`. Nothing is sent, stored or routed outside this value.
//
// Compose twin: `screens/ChatPlaygroundFixture.kt`. Tests: `ChatPlaygroundFixtureTests.swift` /
// `ChatPlaygroundFixtureTest.kt`.

/// What the page must do in response to a handled action (presentation, not effects).
public enum ChatPlaygroundEffect: Equatable, Sendable {
    /// Leave the screen (header Back).
    case exit
    /// Present the fixture-host controls (header overflow) — the Playground's stand-in for the app's
    /// overflow menu.
    case presentHostControls
    /// Present Add to Chat.
    case presentAddToChat
}

public struct ChatPlaygroundFixture: Equatable, Sendable {
    public enum Conversation: String, CaseIterable, Sendable {
        case populated = "Populated"
        case empty = "Empty"
    }

    /// Illustrative fixture time for simulated host evidence. Not a clock reading.
    public static let fixtureTime = "10:24"
    public static let replyTitle = "Replying to Rem"
    public static let activityNote = "Agent activity details open in the app."
    public static let reactionNote = "The app presents the reaction picker."
    public static let retryNote = "Retry resubmitted; no receipt until the host accepts it."
    public static let emptyMessage = "Start a conversation with Rem. Plan your day, explore an idea or get a task moving."
    public static let starters = [ChatStarter(id: "plan-day", title: "Help me plan my day")]

    public private(set) var entries: [ChatTranscriptEntry]
    public var composer: ChatComposerFixture
    public private(set) var replyContext: ChatReplyContext?
    /// The Inbox item this conversation replies to; `nil` for ordinary Chat.
    public private(set) var taskID: String?
    public private(set) var taskState: InboxItemState
    public private(set) var note: String?
    /// The outgoing message the current turn belongs to (the one just sent or retried). Simulated host
    /// evidence applies to this message — never to "whichever is latest" — so a retried older message
    /// is the one accepted or failed. `nil` when no turn is in flight.
    public private(set) var activeOutgoingID: String?
    /// Set while the active turn is a retry, so cancelling it restores the message's failure and
    /// Try again instead of stranding it with no receipt and no retry.
    private var retryingID: String?
    private var nextID = 0

    // MARK: Construction

    public init(_ conversation: Conversation = .populated) {
        entries = conversation == .empty ? [] : Self.populatedEntries
        composer = ChatComposerFixture()
        taskState = .none
    }

    /// Task chat for an Inbox item: the same composition plus the reply accessory, and the header
    /// carrying the item's host-reported state.
    public static func taskReply(for item: InboxItemDisplay) -> ChatPlaygroundFixture {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.taskID = item.id
        fixture.taskState = item.state
        fixture.entries = [
            .message(ChatMessageDisplay(id: "task.u1", role: .user, text: "Could you make the next step clearer?",
                                        delivery: .delivered(at: fixtureTime))),
            .message(ChatMessageDisplay(id: "task.a1", role: .assistant, text: "I'll outline a clear next step for this task.")),
        ]
        fixture.replyContext = ChatReplyContext(targetID: item.id, title: replyTitle, summary: item.title)
        fixture.composer = ChatComposerFixture(state: ChatComposerState(placeholder: "Write your reply…"))
        return fixture
    }

    /// Default populated conversation (`2054:21981`), with the sample timeline kept consistent (the
    /// Figma sample's "Today 3:25 PM" vs "10:24" mismatch is not reproduced). Only the latest outgoing
    /// message's receipt is rendered (`ChatTranscriptRules`).
    static let populatedEntries: [ChatTranscriptEntry] = [
        .timestamp(id: "t1", text: "Today 10:20 AM"),
        .message(ChatMessageDisplay(id: "u1", role: .user, text: "Help me plan the rest of my day.",
                                    delivery: .delivered(at: "10:20"))),
        .message(ChatMessageDisplay(id: "a1", role: .assistant,
                                    text: "Start with your highest-priority task, then leave time for your next commitment.")),
        .message(ChatMessageDisplay(id: "u2", role: .user, text: "Turn this into a reminder for tomorrow.",
                                    delivery: .delivered(at: fixtureTime))),
        .message(ChatMessageDisplay(id: "a2", role: .assistant, text: "I can help you prepare that reminder.",
                                    meta: "Automatic · Reply complete")),
    ]

    // MARK: Presentation inputs

    public var header: ChatHeaderDisplay {
        if composer.state.phase.isInFlight {
            return ChatHeaderDisplay(activity: "Working on your request", isWorking: true)
        }
        return taskID == nil ? ChatHeaderDisplay(activity: "Connected") : taskState.header()
    }

    public var emptyState: ChatEmptyState? {
        entries.isEmpty ? ChatEmptyState(message: Self.emptyMessage, starters: Self.starters) : nil
    }

    // MARK: Actions

    /// Handle one composition action the way a host would; returns what the page must present.
    @discardableResult
    public mutating func handle(_ action: ChatScreenAction) -> ChatPlaygroundEffect? {
        switch action {
        case .back: return .exit
        case .overflow: return .presentHostControls
        case .activityDetails: note = Self.activityNote
        case .starter(let id):
            guard let starter = Self.starters.first(where: { $0.id == id }) else { return nil }
            composer.apply(.draftChanged(starter.title))
            send()
        case .dismissReplyContext(let targetID):
            if replyContext?.targetID == targetID { replyContext = nil }
        case .composer(let composerAction):
            switch composerAction {
            case .add: return .presentAddToChat
            case .send: send()
            case .cancel:
                composer.apply(.cancel)
                if !composer.state.phase.isInFlight { endTurn(cancelled: true) }
            default: composer.apply(composerAction)
            }
        case .transcript(let transcriptAction):
            handle(transcriptAction)
        }
        return nil
    }

    private mutating func handle(_ action: ChatTranscriptAction) {
        switch action {
        case .retry(let id):
            // One turn at a time: a retry never switches the active turn mid-flight.
            guard !composer.state.phase.isInFlight,
                  let index = messageIndex(id), case .message(var message) = entries[index],
                  message.delivery == .failed, message.canRetry else { return }
            message.delivery = .none
            message.canRetry = false
            entries[index] = .message(message)
            activeOutgoingID = id
            retryingID = id
            composer.select(.sending)
            note = Self.retryNote
        case .requestReaction:
            note = Self.reactionNote
        case .react(let id, let reaction):
            guard let index = messageIndex(id), case .message(var message) = entries[index] else { return }
            message.reaction = reaction
            entries[index] = .message(message)
        }
    }

    /// Send: append the outgoing message with **no receipt** and enter Sending.
    private mutating func send() {
        guard composer.state.canSend else { return }
        composer.apply(.send)
        guard let text = composer.sent.last else { return }
        nextID += 1
        let id = "sent.\(nextID)"
        entries.append(.message(ChatMessageDisplay(id: id, role: .user, text: text)))
        activeOutgoingID = id
        composer.select(.sending)
        note = nil
    }

    // MARK: Fixture host (stand-ins for evidence an app would receive)

    /// The host accepted the active turn's message: Delivered with the fixture time; reply streams.
    /// Where the receipt shows is still decided by `ChatTranscriptRules` (latest outgoing only).
    public mutating func simulateHostAcceptance() {
        guard composer.state.phase == .sending, let index = activeOutgoingIndex,
              case .message(var message) = entries[index], message.delivery == .none else { return }
        message.delivery = .delivered(at: Self.fixtureTime)
        entries[index] = .message(message)
        retryingID = nil
        composer.select(.streaming)
    }

    /// The recipient explicitly acknowledged the latest outgoing message: Read keeps the delivered time.
    public mutating func simulateReadAcknowledgement() {
        guard let index = latestOutgoingIndex, case .message(var message) = entries[index],
              case .delivered(let at) = message.delivery else { return }
        message.delivery = .read(at: at)
        entries[index] = .message(message)
    }

    /// The host reported the active turn's message as not delivered (retryable); the turn ends.
    public mutating func simulateDeliveryFailure() {
        guard composer.state.phase == .sending, let index = activeOutgoingIndex,
              case .message(var message) = entries[index], message.delivery == .none else { return }
        message.delivery = .failed
        message.canRetry = true
        entries[index] = .message(message)
        endTurn(cancelled: false)
        composer.select(.ready)
    }

    /// The reply finished: append a fixture assistant message and return to Ready.
    public mutating func simulateReplyComplete() {
        guard composer.state.phase == .streaming else { return }
        nextID += 1
        entries.append(.message(ChatMessageDisplay(id: "reply.\(nextID)", role: .assistant,
                                                   text: "Here is a fixture reply.", meta: "Automatic · Reply complete")))
        endTurn(cancelled: false)
        composer.select(.ready)
    }

    // MARK: Helpers

    /// Ends the active turn. A cancelled, not-yet-accepted retry returns to Not delivered + Try again.
    private mutating func endTurn(cancelled: Bool) {
        if cancelled, let id = retryingID, let index = messageIndex(id), case .message(var message) = entries[index],
           message.delivery == .none {
            message.delivery = .failed
            message.canRetry = true
            entries[index] = .message(message)
        }
        activeOutgoingID = nil
        retryingID = nil
    }

    private var activeOutgoingIndex: Int? {
        activeOutgoingID.flatMap { messageIndex($0) }
    }

    private var latestOutgoingIndex: Int? {
        guard let id = ChatTranscriptRules.latestOutgoingID(in: entries) else { return nil }
        return messageIndex(id)
    }

    private func messageIndex(_ id: String) -> Int? {
        entries.firstIndex { $0.id == id }
    }

    /// The message with `id`, if present.
    public func message(_ id: String) -> ChatMessageDisplay? {
        guard let index = messageIndex(id), case .message(let message) = entries[index] else { return nil }
        return message
    }
}

/// The Playground Inbox: host-reported item states and routing into task chat.
public struct InboxPlaygroundFixture: Equatable, Sendable {
    public enum Content: String, CaseIterable, Sendable {
        case items = "Items"
        case empty = "Empty"
    }

    public static let emptyMessage = "Nothing in your Inbox."
    public static let seedItems: [InboxItemDisplay] = [
        InboxItemDisplay(id: "plan-next-step", title: "Plan the next step"),
        InboxItemDisplay(id: "investor-update", title: "Draft the investor update", state: .executing),
        InboxItemDisplay(id: "venue-booking", title: "Approve the venue booking", state: .needsApproval),
        InboxItemDisplay(id: "offsite-travel", title: "Book travel for the offsite", state: .blocked),
        InboxItemDisplay(id: "calendar-holds", title: "Sync calendar holds", state: .unknown),
        InboxItemDisplay(id: "weekly-summary", title: "Send the weekly summary", state: .completed),
    ]

    public private(set) var items: [InboxItemDisplay]

    public init(_ content: Content = .items) {
        items = content == .empty ? [] : Self.seedItems
    }

    /// The task chat an Inbox action opens, or `nil` for an unknown id.
    public func route(_ action: InboxAction) -> ChatPlaygroundFixture? {
        switch action {
        case .open(let id):
            guard let item = items.first(where: { $0.id == id }) else { return nil }
            return .taskReply(for: item)
        }
    }
}
