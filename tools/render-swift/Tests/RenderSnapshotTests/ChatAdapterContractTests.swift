import XCTest
@testable import RemDesignSystem

// MARK: - Reference app adapter (test-only)
//
// A minimal stand-in for a consuming app and its thin adapter, written against the public DS contract
// only. It exists to prove the boundary in `docs/contracts/chat-adapter.md`: the app owns evidence and
// operations; the adapter maps app state → DS values and DS actions → app operations; the DS renders.
// Nothing here ships in the library. Paired with `ChatAdapterContractTest.kt`.

/// App-side message record. `acceptedAt` is durable host acceptance; `readAcknowledgedAt` is an
/// explicit acknowledgement from the recipient. Both are app evidence, never DS state.
private struct MockAppMessage {
    var id: String
    var fromPerson: Bool
    var body: String
    var acceptedAt: Date?
    var readAcknowledgedAt: Date?
    var sendFailed = false
    var retryable = false
}

private struct MockAppAttachment {
    var id: String
    var name: String
    var isCapability = false
}

/// App-side turn state.
private struct MockAppSession {
    enum Turn { case none, submitted, receiving }
    var signedIn = true
    var draft = ""
    var attachments: [MockAppAttachment] = []
    var turn: Turn = .none
    var voiceReady = true
    var composerFocused = false
}

/// What the app is asked to do. The DS never performs any of these.
private enum MockAppOperation: Equatable {
    case setDraft(String)
    case submit(text: String, attachmentIDs: [String])
    case cancelTurn
    case startVoice
    case presentAddToChat
    case removeAttachment(String)
    case setComposerFocus(Bool)
    case retry(String)
    case presentReactionPicker(String)
    case setReaction(String, emoji: String?)
}

private enum MockChatAdapter {
    static let clock: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    /// Evidence → display. Delivered needs host acceptance; Read additionally needs an explicit
    /// acknowledgement; a failure with no acceptance is Not delivered; otherwise no receipt at all.
    static func delivery(for message: MockAppMessage) -> MessageBubble.Delivery {
        guard message.fromPerson else { return .none }
        guard let accepted = message.acceptedAt else { return message.sendFailed ? .failed : .none }
        let at = clock.string(from: accepted)
        return message.readAcknowledgedAt == nil ? .delivered(at: at) : .read(at: at)
    }

    static func message(_ message: MockAppMessage) -> ChatMessageDisplay {
        let delivery = delivery(for: message)
        return ChatMessageDisplay(
            id: message.id, role: message.fromPerson ? .user : .assistant, text: message.body,
            delivery: delivery, canRetry: delivery == .failed && message.retryable
        )
    }

    static func composer(_ session: MockAppSession) -> ChatComposerState {
        ChatComposerState(
            draft: session.draft,
            attachments: session.attachments.map {
                ComposerAttachment(id: $0.id, title: $0.name, kind: $0.isCapability ? .capability : .image)
            },
            availability: session.signedIn ? .enabled : .disabled(reason: "Sign in to chat"),
            phase: {
                switch session.turn {
                case .none: return .idle
                case .submitted: return .sending
                case .receiving: return .streaming
                }
            }(),
            voiceAvailable: session.voiceReady,
            isFocused: session.composerFocused
        )
    }

    static func operation(for action: ChatComposerAction, in session: MockAppSession) -> MockAppOperation? {
        switch action {
        case .draftChanged(let text): return .setDraft(text)
        case .send:
            // Defence in depth: the adapter re-checks the same rule the DS rendered.
            guard composer(session).canSend else { return nil }
            return .submit(text: session.draft.trimmingCharacters(in: .whitespacesAndNewlines),
                           attachmentIDs: session.attachments.map(\.id))
        case .cancel: return composer(session).canCancel ? .cancelTurn : nil
        case .speak: return .startVoice
        case .add: return .presentAddToChat
        case .removeAttachment(let id): return .removeAttachment(id)
        case .focusChanged(let focused): return .setComposerFocus(focused)
        }
    }

    static func operation(for action: ChatTranscriptAction) -> MockAppOperation {
        switch action {
        case .retry(let id): return .retry(id)
        case .requestReaction(let id): return .presentReactionPicker(id)
        case .react(let id, let reaction): return .setReaction(id, emoji: reaction?.emoji)
        }
    }
}

// MARK: - Contract tests

final class ChatAdapterContractTests: XCTestCase {
    private let accepted = Date(timeIntervalSince1970: 1_791_000_000) // a fixed instant, rendered in UTC

    private func outgoing(accepted: Date? = nil, read: Date? = nil, failed: Bool = false, retryable: Bool = false) -> MockAppMessage {
        MockAppMessage(id: "m1", fromPerson: true, body: "Thanks", acceptedAt: accepted,
                       readAcknowledgedAt: read, sendFailed: failed, retryable: retryable)
    }

    func testNoAcceptanceMeansNoReceipt() {
        XCTAssertEqual(MockChatAdapter.delivery(for: outgoing()), .none)
    }

    func testDeliveredRequiresHostAcceptance() {
        let at = MockChatAdapter.clock.string(from: accepted)
        XCTAssertEqual(MockChatAdapter.delivery(for: outgoing(accepted: accepted)), .delivered(at: at))
    }

    func testReadRequiresExplicitAcknowledgementAndKeepsDeliveredTime() {
        let at = MockChatAdapter.clock.string(from: accepted)
        let read = MockChatAdapter.delivery(for: outgoing(accepted: accepted, read: accepted.addingTimeInterval(600)))
        XCTAssertEqual(read, .read(at: at))
    }

    func testAcknowledgementWithoutAcceptanceNeverShowsRead() {
        XCTAssertEqual(MockChatAdapter.delivery(for: outgoing(read: accepted)), .none)
    }

    func testFailureWithoutAcceptanceIsNotDeliveredAndRetryIsHostGated() {
        XCTAssertEqual(MockChatAdapter.message(outgoing(failed: true)).delivery, .failed)
        XCTAssertFalse(MockChatAdapter.message(outgoing(failed: true)).canRetry)
        XCTAssertTrue(MockChatAdapter.message(outgoing(failed: true, retryable: true)).canRetry)
    }

    func testIncomingMessagesCarryNoReceipt() {
        let incoming = MockAppMessage(id: "a1", fromPerson: false, body: "Done", acceptedAt: accepted, readAcknowledgedAt: accepted)
        XCTAssertEqual(MockChatAdapter.message(incoming).delivery, .none)
    }

    func testSignedOutSessionRendersExternallyDisabledComposer() {
        let state = MockChatAdapter.composer(MockAppSession(signedIn: false, draft: "Hi"))
        XCTAssertEqual(state.availability, .disabled(reason: "Sign in to chat"))
        XCTAssertNil(MockChatAdapter.operation(for: .send, in: MockAppSession(signedIn: false, draft: "Hi")))
    }

    func testSendMapsToSubmitWithTrimmedTextAndAttachments() {
        let session = MockAppSession(draft: "  Plan my afternoon ", attachments: [MockAppAttachment(id: "photo.0", name: "Photo 1")])
        XCTAssertEqual(MockChatAdapter.operation(for: .send, in: session),
                       .submit(text: "Plan my afternoon", attachmentIDs: ["photo.0"]))
    }

    func testAttachmentOnlySendSubmitsEmptyText() {
        let session = MockAppSession(attachments: [MockAppAttachment(id: "photo.0", name: "Photo 1")])
        XCTAssertEqual(MockChatAdapter.operation(for: .send, in: session), .submit(text: "", attachmentIDs: ["photo.0"]))
    }

    func testCapabilityOnlySessionCannotSubmit() {
        let session = MockAppSession(attachments: [MockAppAttachment(id: "cloud-browser", name: "Cloud browser", isCapability: true)])
        XCTAssertNil(MockChatAdapter.operation(for: .send, in: session))
    }

    func testInFlightTurnsMapStopToCancelNotSubmit() {
        for turn in [MockAppSession.Turn.submitted, .receiving] {
            let session = MockAppSession(draft: "More", turn: turn)
            XCTAssertEqual(MockChatAdapter.composer(session).primaryAction, .cancel)
            XCTAssertEqual(MockChatAdapter.operation(for: .cancel, in: session), .cancelTurn)
            XCTAssertNil(MockChatAdapter.operation(for: .send, in: session))
        }
        XCTAssertNil(MockChatAdapter.operation(for: .cancel, in: MockAppSession()))
    }

    func testVoiceAddFocusAndAttachmentActionsRouteToAppOperations() {
        let session = MockAppSession()
        XCTAssertEqual(MockChatAdapter.operation(for: .speak, in: session), .startVoice)
        XCTAssertEqual(MockChatAdapter.operation(for: .add, in: session), .presentAddToChat)
        XCTAssertEqual(MockChatAdapter.operation(for: .focusChanged(true), in: session), .setComposerFocus(true))
        XCTAssertEqual(MockChatAdapter.operation(for: .removeAttachment(id: "photo.0"), in: session), .removeAttachment("photo.0"))
        XCTAssertEqual(MockChatAdapter.operation(for: .draftChanged("a"), in: session), .setDraft("a"))
        XCTAssertTrue(MockChatAdapter.composer(MockAppSession(composerFocused: true)).isFocused)
        XCTAssertFalse(MockChatAdapter.composer(MockAppSession(voiceReady: false)).showsSpeak)
    }

    func testTranscriptActionsRouteToAppOperations() {
        XCTAssertEqual(MockChatAdapter.operation(for: .retry(messageID: "m1")), .retry("m1"))
        XCTAssertEqual(MockChatAdapter.operation(for: .requestReaction(messageID: "m1")), .presentReactionPicker("m1"))
        XCTAssertEqual(MockChatAdapter.operation(for: .react(messageID: "m1", reaction: .heart)), .setReaction("m1", emoji: "❤️"))
        XCTAssertEqual(MockChatAdapter.operation(for: .react(messageID: "m1", reaction: nil)), .setReaction("m1", emoji: nil))
    }
}
