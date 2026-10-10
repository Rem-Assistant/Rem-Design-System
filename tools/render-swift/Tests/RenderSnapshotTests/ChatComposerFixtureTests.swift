import XCTest
@testable import RemDesignSystem

/// Presentation-only Playground host for the canonical composer (`ChatComposerFixture.swift`). Paired
/// with `ChatComposerFixtureTest.kt`. Native typing, focus and tap journeys need runtime UI evidence.
final class ChatComposerFixtureTests: XCTestCase {
    private let photo = ComposerAttachment(id: "photo.0", title: "Photo 1", kind: .image)

    func testTypingThenSendRecordsTrimmedTextAndClearsDraft() {
        var fixture = ChatComposerFixture()
        fixture.apply(.draftChanged("  Plan my afternoon "))
        fixture.apply(.send)
        XCTAssertEqual(fixture.sent, ["Plan my afternoon"])
        XCTAssertEqual(fixture.state.draft, "")
    }

    func testSendIsIgnoredWhenTheRuleSaysNo() {
        var fixture = ChatComposerFixture()
        fixture.apply(.send)
        fixture.attach(.cloudBrowser)
        fixture.apply(.send)
        XCTAssertEqual(fixture.sent, [])
    }

    func testAttachmentOnlySendKeepsCapabilityChip() {
        var fixture = ChatComposerFixture()
        fixture.attach(photo)
        fixture.attach(.cloudBrowser)
        fixture.apply(.send)
        XCTAssertEqual(fixture.sent, ["Photo 1"])
        XCTAssertEqual(fixture.state.attachments, [.cloudBrowser])
    }

    func testAttachIsIdempotentAndRemoveDropsById() {
        var fixture = ChatComposerFixture()
        fixture.attach(.cloudBrowser)
        fixture.attach(.cloudBrowser)
        XCTAssertEqual(fixture.state.attachments.count, 1)
        fixture.apply(.removeAttachment(id: ComposerAttachment.cloudBrowser.id))
        XCTAssertTrue(fixture.state.attachments.isEmpty)
    }

    func testScenariosDriveHostStates() {
        var fixture = ChatComposerFixture(state: ChatComposerState(draft: "Hello"))
        fixture.select(.unavailable)
        XCTAssertEqual(fixture.state.availability, .disabled(reason: ChatComposerFixture.unavailableReason))
        XCTAssertFalse(fixture.state.canSend)
        fixture.select(.noVoice)
        XCTAssertFalse(fixture.state.showsSpeak)
        XCTAssertTrue(fixture.state.canSend)
        fixture.select(.streaming)
        XCTAssertEqual(fixture.state.primaryAction, .cancel)
        XCTAssertEqual(fixture.state.draft, "Hello", "Scenarios keep the draft")
    }

    func testStopCancelsBackToReadyAndNeverSends() {
        var fixture = ChatComposerFixture(state: ChatComposerState(draft: "Hello"))
        fixture.select(.sending)
        fixture.apply(.send)
        XCTAssertEqual(fixture.sent, [])
        fixture.apply(.cancel)
        XCTAssertEqual(fixture.scenario, .ready)
        XCTAssertEqual(fixture.state.phase, .idle)
        XCTAssertEqual(fixture.note, ChatComposerFixture.cancelNote)
    }

    func testSpeakOnlyWhenOfferedAndFocusIsRecorded() {
        var fixture = ChatComposerFixture()
        fixture.select(.noVoice)
        fixture.apply(.speak)
        XCTAssertNil(fixture.note)
        fixture.select(.ready)
        fixture.apply(.speak)
        XCTAssertEqual(fixture.note, ChatComposerFixture.speakNote)
        fixture.apply(.focusChanged(true))
        XCTAssertTrue(fixture.state.isFocused)
    }
}
