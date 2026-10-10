import XCTest
@testable import RemDesignSystem

/// Full-screen Chat / task reply / Inbox fixture (`ChatPlaygroundFixture.swift`). Paired with
/// `ChatPlaygroundFixtureTest.kt`. Proves the fixture never fabricates receipts and routes Inbox state
/// truthfully; native journeys are covered separately by the Playground UI tests.
final class ChatPlaygroundFixtureTests: XCTestCase {
    func testPopulatedShowsOnlyTheLatestReceiptAndAConsistentTimeline() {
        let fixture = ChatPlaygroundFixture(.populated)
        XCTAssertNil(fixture.emptyState)
        XCTAssertEqual(ChatTranscriptRules.latestOutgoingID(in: fixture.entries), "u2")
        let older = fixture.message("u1")!
        XCTAssertEqual(ChatTranscriptRules.displayedDelivery(for: older, latestOutgoingID: "u2"), .none)
        XCTAssertEqual(fixture.message("u2")?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime))
        XCTAssertEqual(fixture.header.activity, "Connected")
    }

    func testEmptyHasStartersAndNoTranscript() {
        let fixture = ChatPlaygroundFixture(.empty)
        XCTAssertEqual(fixture.emptyState?.starters, ChatPlaygroundFixture.starters)
        XCTAssertTrue(fixture.entries.isEmpty)
    }

    func testStarterSendsWithoutAReceipt() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.starter(id: "plan-day"))
        XCTAssertNil(fixture.emptyState)
        guard case .message(let sent)? = fixture.entries.last else { return XCTFail() }
        XCTAssertEqual(sent.text, "Help me plan my day")
        XCTAssertEqual(sent.delivery, .none, "No host acceptance, no receipt")
        XCTAssertEqual(fixture.composer.state.phase, .sending)
        XCTAssertTrue(fixture.header.isWorking)
    }

    func testSendStopAndHostAcceptanceJourney() {
        var fixture = ChatPlaygroundFixture(.populated)
        fixture.handle(.composer(.draftChanged("Plan my afternoon")))
        fixture.handle(.composer(.send))
        let id = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        XCTAssertEqual(fixture.message(id)?.delivery, MessageBubble.Delivery.none)
        XCTAssertEqual(fixture.composer.state.primaryAction, .cancel)
        fixture.handle(.composer(.cancel))
        XCTAssertEqual(fixture.composer.state.phase, .idle)
        XCTAssertEqual(fixture.message(id)?.delivery, MessageBubble.Delivery.none, "Stop never fabricates a receipt")

        fixture.handle(.composer(.draftChanged("Again")))
        fixture.handle(.composer(.send))
        fixture.simulateHostAcceptance()
        let again = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        XCTAssertEqual(fixture.message(again)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime))
        XCTAssertEqual(fixture.composer.state.phase, .streaming)
        fixture.simulateReadAcknowledgement()
        XCTAssertEqual(fixture.message(again)?.delivery, .read(at: ChatPlaygroundFixture.fixtureTime))
        let before = fixture.entries.count
        fixture.simulateReplyComplete()
        XCTAssertEqual(fixture.entries.count, before + 1)
        XCTAssertEqual(fixture.composer.state.phase, .idle)
    }

    func testReadNeverAppearsWithoutDelivery() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("Hi")))
        fixture.handle(.composer(.send))
        fixture.simulateReadAcknowledgement()
        let id = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        XCTAssertEqual(fixture.message(id)?.delivery, MessageBubble.Delivery.none)
    }

    func testFailureAndRetryJourney() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("Share the agenda")))
        fixture.handle(.composer(.send))
        fixture.simulateDeliveryFailure()
        let id = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        XCTAssertEqual(fixture.message(id)?.delivery, .failed)
        XCTAssertEqual(fixture.message(id)?.canRetry, true)
        XCTAssertEqual(fixture.composer.state.phase, .idle)
        fixture.handle(.transcript(.retry(messageID: id)))
        XCTAssertEqual(fixture.message(id)?.delivery, MessageBubble.Delivery.none, "Retry waits for host acceptance")
        XCTAssertEqual(fixture.note, ChatPlaygroundFixture.retryNote)
        fixture.handle(.transcript(.retry(messageID: id)))
        XCTAssertEqual(fixture.message(id)?.delivery, MessageBubble.Delivery.none, "Retry is not offered twice")
    }

    /// Release-review regression: fail A, send + accept + complete B, then retry A. Host evidence must
    /// apply to A (the active turn), not to B (the latest outgoing), and the turn must complete.
    func testRetryOfOlderFailedMessageCompletesItsOwnTurn() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("A")))
        fixture.handle(.composer(.send))
        let a = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        fixture.simulateDeliveryFailure()
        XCTAssertEqual(fixture.message(a)?.delivery, .failed)

        fixture.handle(.composer(.draftChanged("B")))
        fixture.handle(.composer(.send))
        let b = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        XCTAssertNotEqual(a, b)
        fixture.handle(.transcript(.retry(messageID: a)))
        XCTAssertEqual(fixture.activeOutgoingID, b, "Retry never switches the active turn mid-flight")
        XCTAssertEqual(fixture.message(a)?.delivery, .failed)
        fixture.simulateHostAcceptance()
        fixture.simulateReplyComplete()
        XCTAssertEqual(fixture.message(b)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime))
        XCTAssertNil(fixture.activeOutgoingID)

        fixture.handle(.transcript(.retry(messageID: a)))
        XCTAssertEqual(fixture.activeOutgoingID, a)
        XCTAssertEqual(fixture.composer.state.phase, .sending)
        fixture.simulateHostAcceptance()
        XCTAssertEqual(fixture.message(a)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime), "Acceptance applies to A")
        XCTAssertEqual(fixture.message(b)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime), "B is untouched")
        XCTAssertEqual(fixture.composer.state.phase, .streaming)
        let before = fixture.entries.count
        fixture.simulateReplyComplete()
        XCTAssertEqual(fixture.entries.count, before + 1)
        XCTAssertEqual(fixture.composer.state.phase, .idle)
        XCTAssertNil(fixture.activeOutgoingID)

        // Latest-only receipt display is unchanged: B is still the latest outgoing message.
        let latest = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)
        XCTAssertEqual(latest, b)
        XCTAssertEqual(ChatTranscriptRules.displayedDelivery(for: fixture.message(a)!, latestOutgoingID: latest),
                       MessageBubble.Delivery.none)
    }

    func testRetryFailureAppliesToTheRetriedMessage() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("A")))
        fixture.handle(.composer(.send))
        let a = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        fixture.simulateDeliveryFailure()
        fixture.handle(.composer(.draftChanged("B")))
        fixture.handle(.composer(.send))
        let b = ChatTranscriptRules.latestOutgoingID(in: fixture.entries)!
        fixture.simulateHostAcceptance()
        fixture.simulateReplyComplete()
        fixture.handle(.transcript(.retry(messageID: a)))
        fixture.simulateDeliveryFailure()
        XCTAssertEqual(fixture.message(a)?.delivery, .failed)
        XCTAssertEqual(fixture.message(a)?.canRetry, true)
        XCTAssertEqual(fixture.message(b)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime))
        XCTAssertEqual(fixture.composer.state.phase, .idle)
    }

    func testCancelledRetryRestoresNotDeliveredAndTryAgain() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("A")))
        fixture.handle(.composer(.send))
        let a = fixture.activeOutgoingID!
        fixture.simulateDeliveryFailure()
        fixture.handle(.transcript(.retry(messageID: a)))
        fixture.handle(.composer(.cancel))
        XCTAssertNil(fixture.activeOutgoingID)
        XCTAssertEqual(fixture.message(a)?.delivery, .failed, "A cancelled retry is still not delivered")
        XCTAssertEqual(fixture.message(a)?.canRetry, true, "and can be retried again")
        fixture.handle(.transcript(.retry(messageID: a)))
        fixture.simulateHostAcceptance()
        XCTAssertEqual(fixture.message(a)?.delivery, .delivered(at: ChatPlaygroundFixture.fixtureTime))
    }

    func testCancelEndsTheActiveTurnWithoutAReceipt() {
        var fixture = ChatPlaygroundFixture(.empty)
        fixture.handle(.composer(.draftChanged("A")))
        fixture.handle(.composer(.send))
        let a = fixture.activeOutgoingID
        fixture.handle(.composer(.cancel))
        XCTAssertNil(fixture.activeOutgoingID)
        fixture.simulateHostAcceptance()
        XCTAssertEqual(fixture.message(a!)?.delivery, MessageBubble.Delivery.none, "No acceptance after cancel")
    }

    func testHeaderAndAddEffects() {
        var fixture = ChatPlaygroundFixture()
        XCTAssertEqual(fixture.handle(.back), .exit)
        XCTAssertEqual(fixture.handle(.overflow), .presentHostControls)
        XCTAssertEqual(fixture.handle(.composer(.add)), .presentAddToChat)
        XCTAssertNil(fixture.handle(.activityDetails))
        XCTAssertEqual(fixture.note, ChatPlaygroundFixture.activityNote)
    }

    func testCallButtonIsAnInAppVoiceEntryThatDialsNothing() {
        var fixture = ChatPlaygroundFixture()
        XCTAssertFalse(fixture.header.showsCall, "call would replace overflow, the Playground's host-controls route")
        let before = fixture
        XCTAssertNil(fixture.handle(.call), "no presentation")
        XCTAssertEqual(fixture.note, ChatPlaygroundFixture.callNote)
        XCTAssertEqual(fixture.entries, before.entries)
        XCTAssertEqual(fixture.composer, before.composer)
    }

    func testInboxRoutesEachStateIntoTaskReplyWithTheSameTruth() {
        let inbox = InboxPlaygroundFixture()
        for item in inbox.items {
            let chat = inbox.route(.open(itemID: item.id))!
            XCTAssertEqual(chat.taskID, item.id)
            XCTAssertEqual(chat.replyContext, ChatReplyContext(targetID: item.id, title: ChatPlaygroundFixture.replyTitle, summary: item.title))
            XCTAssertEqual(chat.header, item.state.header())
            XCTAssertEqual(chat.composer.state.placeholder, "Write your reply…")
        }
        XCTAssertNil(inbox.route(.open(itemID: "missing")))
        XCTAssertTrue(InboxPlaygroundFixture(.empty).items.isEmpty)
    }

    func testInboxStatesMapToLabelsAndAttention() {
        XCTAssertNil(InboxItemState.none.statusLabel)
        XCTAssertEqual(InboxItemState.unknown.statusLabel, "Status unknown")
        XCTAssertTrue(InboxItemState.needsApproval.needsPerson)
        XCTAssertTrue(InboxItemState.blocked.needsPerson)
        XCTAssertFalse(InboxItemState.completed.needsPerson)
        XCTAssertEqual(InboxItemState.blocked.header().status, .needsYou)
        XCTAssertTrue(InboxItemState.executing.header().isWorking)
        XCTAssertEqual(InboxItemState.none.header().activity, "Connected")
    }

    func testDismissClearsOnlyTheMatchingTarget() {
        var chat = InboxPlaygroundFixture().route(.open(itemID: "venue-booking"))!
        chat.handle(.dismissReplyContext(targetID: "other"))
        XCTAssertNotNil(chat.replyContext)
        chat.handle(.dismissReplyContext(targetID: "venue-booking"))
        XCTAssertNil(chat.replyContext)
        XCTAssertEqual(chat.taskID, "venue-booking", "Task identity survives dismissing the reply target")
    }
}
