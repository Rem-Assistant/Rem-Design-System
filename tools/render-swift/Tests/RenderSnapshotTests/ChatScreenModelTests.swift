import XCTest
@testable import RemDesignSystem

/// The Chat / Inbox presentation contract's pure rules (`ChatScreenModel.swift`). Paired with
/// `ChatScreenModelTest.kt`; both assert the same rules so the SwiftUI and Compose composers cannot
/// drift. Native focus, keyboard and tap journeys need separate runtime UI evidence.
final class ChatScreenModelTests: XCTestCase {
    private let photo = ComposerAttachment(id: "photo.0", title: "Photo 1", kind: .image)
    private let file = ComposerAttachment(id: "file.a", title: "notes.png", kind: .file)

    func testEmptyDraftCannotSend() {
        let state = ChatComposerState()
        XCTAssertFalse(state.canSend)
        XCTAssertNil(state.primaryAction)
        XCTAssertEqual(state.sendDisplay, .unavailable)
    }

    func testWhitespaceOnlyDraftCannotSend() {
        XCTAssertFalse(ChatComposerState(draft: "  \n\t ").canSend)
    }

    func testTextDraftSends() {
        let state = ChatComposerState(draft: "Plan my afternoon")
        XCTAssertTrue(state.canSend)
        XCTAssertEqual(state.primaryAction, .send)
        XCTAssertEqual(state.sendDisplay, .send)
    }

    func testAttachmentOnlyMessageSends() {
        XCTAssertTrue(ChatComposerState(attachments: [photo]).canSend)
        XCTAssertTrue(ChatComposerState(attachments: [file]).canSend)
    }

    func testCapabilityChipAloneNeverMakesAMessage() {
        let state = ChatComposerState(attachments: [.cloudBrowser])
        XCTAssertFalse(state.hasContentAttachments)
        XCTAssertFalse(state.canSend)
        XCTAssertTrue(ChatComposerState(draft: "Find a venue", attachments: [.cloudBrowser]).canSend)
    }

    func testExternallyDisabledBlocksSendSpeakAndModel() {
        let state = ChatComposerState(draft: "Hello", attachments: [photo],
                                      availability: .disabled(reason: "Sign in to chat"))
        XCTAssertFalse(state.canSend)
        XCTAssertFalse(state.showsSpeak)
        XCTAssertFalse(state.modelEnabled)
        XCTAssertNil(state.primaryAction)
        XCTAssertEqual(state.sendDisplay, .unavailable)
    }

    func testSendingAndStreamingAreCancelNeverSend() {
        for phase in [ComposerPhase.sending, .streaming] {
            let state = ChatComposerState(draft: "Next question", phase: phase)
            XCTAssertFalse(state.canSend, "\(phase)")
            XCTAssertTrue(state.canCancel, "\(phase)")
            XCTAssertEqual(state.primaryAction, .cancel, "\(phase)")
            XCTAssertEqual(state.sendDisplay, .stop, "\(phase)")
            XCTAssertFalse(state.showsSpeak, "\(phase)")
            XCTAssertFalse(state.modelEnabled, "\(phase)")
        }
    }

    func testCancelStaysAvailableWhenInputIsExternallyDisabledMidTurn() {
        let state = ChatComposerState(availability: .disabled(reason: nil), phase: .streaming)
        XCTAssertEqual(state.primaryAction, .cancel)
    }

    func testSpeakFollowsHostVoiceAvailability() {
        XCTAssertTrue(ChatComposerState(voiceAvailable: true).showsSpeak)
        XCTAssertFalse(ChatComposerState(voiceAvailable: false).showsSpeak)
    }

    func testAssistantMessagesNeverCarryDelivery() {
        let message = ChatMessageDisplay(id: "a", role: .assistant, text: "Done", delivery: .read(at: "10:24"))
        XCTAssertEqual(message.delivery, .none)
        let outgoing = ChatMessageDisplay(id: "u", role: .user, text: "Thanks", delivery: .read(at: "10:24"))
        XCTAssertEqual(outgoing.delivery, .read(at: "10:24"), "The DS renders the supplied state verbatim")
    }

    // MARK: Transcript receipt placement (board `2681:21977`, acceptance `2659:21942`)

    private func user(_ id: String, _ delivery: MessageBubble.Delivery) -> ChatTranscriptEntry {
        .message(ChatMessageDisplay(id: id, role: .user, text: id, delivery: delivery))
    }

    func testOnlyLatestOutgoingCarriesItsReceipt() {
        let entries: [ChatTranscriptEntry] = [
            .timestamp(id: "t", text: "Today"),
            user("u1", .delivered(at: "10:20")),
            .message(ChatMessageDisplay(id: "a1", role: .assistant, text: "ok")),
            user("u2", .read(at: "10:24")),
            .message(ChatMessageDisplay(id: "a2", role: .assistant, text: "done")),
        ]
        let latest = ChatTranscriptRules.latestOutgoingID(in: entries)
        XCTAssertEqual(latest, "u2")
        guard case .message(let older) = entries[1], case .message(let newest) = entries[3] else { return XCTFail() }
        XCTAssertEqual(ChatTranscriptRules.displayedDelivery(for: older, latestOutgoingID: latest), .none)
        XCTAssertEqual(ChatTranscriptRules.displayedDelivery(for: newest, latestOutgoingID: latest), .read(at: "10:24"))
    }

    func testFailureStaysVisibleOnOlderMessages() {
        let failed = ChatMessageDisplay(id: "u1", role: .user, text: "x", delivery: .failed)
        XCTAssertEqual(ChatTranscriptRules.displayedDelivery(for: failed, latestOutgoingID: "u9"), .failed)
    }

    func testNoOutgoingMeansNoLatest() {
        XCTAssertNil(ChatTranscriptRules.latestOutgoingID(in: [.timestamp(id: "t", text: "Today")]))
    }

    func testReplyContextAndHeaderDefaults() {
        let header = ChatHeaderDisplay(activity: "Connected")
        XCTAssertTrue(header.showsBack && header.showsOverflow && header.showsActivityDetails)
        XCTAssertFalse(header.isWorking)
        let context = ChatReplyContext(targetID: "target-1", title: "Replying to Rem", summary: "Plan the next step")
        XCTAssertEqual(ChatScreenAction.dismissReplyContext(targetID: context.targetID), .dismissReplyContext(targetID: "target-1"))
        XCTAssertEqual(ChatEmptyState(message: "m").title, "What can I help with?")
    }

    // MARK: WS1d — swipe left to reveal timestamps (thresholds marked for review)

    func testMessageTimeIsHostSuppliedAndOptional() {
        var message = ChatMessageDisplay(id: "u", role: .user, text: "Hi", time: "10:24")
        XCTAssertEqual(message.time, "10:24")
        XCTAssertNil(ChatMessageDisplay(id: "a", role: .assistant, text: "Hi").time)
        let original = message
        message.time = nil
        XCTAssertNotEqual(message, original)
    }

    func testOnlyAClearlyHorizontalLeftDragReveals() {
        XCTAssertTrue(ChatTimestampReveal.isRevealDrag(dx: -20, dy: 5))
        XCTAssertFalse(ChatTimestampReveal.isRevealDrag(dx: 20, dy: 0), "rightward")
        XCTAssertFalse(ChatTimestampReveal.isRevealDrag(dx: -9, dy: 0), "below travel")
        XCTAssertFalse(ChatTimestampReveal.isRevealDrag(dx: -20, dy: 40), "vertical scroll")
        XCTAssertFalse(ChatTimestampReveal.isRevealDrag(dx: -15, dy: 12), "diagonal")
    }

    func testRevealTracksTheFingerThenResistsOrClamps() {
        XCTAssertEqual(ChatTimestampReveal.reveal(dx: 30, columnWidth: 60, reduceMotion: false), 0)
        XCTAssertEqual(ChatTimestampReveal.reveal(dx: -40, columnWidth: 60, reduceMotion: false), 40)
        XCTAssertEqual(ChatTimestampReveal.reveal(dx: -60, columnWidth: 60, reduceMotion: false), 60)
        XCTAssertEqual(ChatTimestampReveal.reveal(dx: -100, columnWidth: 60, reduceMotion: false), 72, accuracy: 0.001)
        XCTAssertEqual(ChatTimestampReveal.reveal(dx: -100, columnWidth: 60, reduceMotion: true), 60, "Reduce Motion clamps")
        XCTAssertEqual(ChatTimestampReveal.settled, 0)
    }

    func testTimeIsAlwaysAvailableToAssistiveTechnology() {
        XCTAssertEqual(ChatTimestampReveal.accessibilityTime(ChatMessageDisplay(id: "u", role: .user, text: "Hi", time: "10:24")),
                       "Sent at 10:24")
        XCTAssertEqual(ChatTimestampReveal.accessibilityTime(ChatMessageDisplay(id: "a", role: .assistant, text: "Hi", time: "10:25")),
                       "Received at 10:25")
        XCTAssertNil(ChatTimestampReveal.accessibilityTime(ChatMessageDisplay(id: "x", role: .user, text: "Hi")))
    }
}
