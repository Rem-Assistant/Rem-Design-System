import XCTest
@testable import RemDesignSystem

/// Pure display rules of the chat cards — PollCard lettering and state, MessageDraftCard receipt mapping,
/// and the Figma names each enum maps to. Paired with `ChatCardModelTest.kt`; both assert the same values.
final class ChatCardModelTests: XCTestCase {
    private let options = [
        PollOption(id: "notion", label: "Add Notion"),
        PollOption(id: "later", label: "Not now"),
        PollOption(id: "ask", label: "Ask me each time"),
    ]

    func testMarkersFollowListOrderAndExtendPastTheMasterAB() {
        XCTAssertEqual(PollCardModel.lettered(options).map(\.marker), ["A", "B", "C"])
        XCTAssertEqual(PollCardModel.lettered(Array(options.prefix(2))).map(\.marker), ["A", "B"])
        XCTAssertEqual(PollCardModel.lettered([]), [])
    }

    func testMarkerSequenceIsBijectiveBase26() {
        XCTAssertEqual(PollCardModel.marker(at: 0), "A")
        XCTAssertEqual(PollCardModel.marker(at: 2), "C")
        XCTAssertEqual(PollCardModel.marker(at: 25), "Z")
        XCTAssertEqual(PollCardModel.marker(at: 26), "AA")
        XCTAssertEqual(PollCardModel.marker(at: 27), "AB")
        XCTAssertEqual(PollCardModel.marker(at: 701), "ZZ")
        XCTAssertEqual(PollCardModel.marker(at: 702), "AAA")
    }

    func testSelectionResolvesToAnsweredWithOriginalMarker() {
        let state = PollCardModel.state(options: options, selection: "ask")
        XCTAssertEqual(state, .answered(PollLetteredOption(marker: "C", option: options[2])))
        XCTAssertEqual(state.figmaName, "Answered")
        XCTAssertEqual(PollCardModel.state(options: options, selection: "later"),
                       .answered(PollLetteredOption(marker: "B", option: options[1])))
    }

    func testNoOrUnknownSelectionStaysAwaiting() {
        XCTAssertEqual(PollCardModel.state(options: options, selection: nil), .awaiting)
        XCTAssertEqual(PollCardModel.state(options: options, selection: "missing"), .awaiting)
        XCTAssertEqual(PollCardModel.state(options: [], selection: "notion"), .awaiting)
        XCTAssertEqual(PollCardState.awaiting.figmaName, "Awaiting")
    }

    func testPurposeFigmaNames() {
        XCTAssertEqual(PollPurpose.allCases.map(\.figmaName), ["Choice", "Suggestion"])
    }

    func testDraftStateMapsToReceipt() {
        XCTAssertEqual(MessageDraftCardState.allCases.map(\.figmaName), ["Review", "Sent", "Unconfirmed"])
        XCTAssertTrue(MessageDraftCardState.review.showsActions)
        XCTAssertNil(MessageDraftCardState.review.receiptOutcome)
        XCTAssertNil(MessageDraftCardState.review.receiptLabel)
        XCTAssertFalse(MessageDraftCardState.sent.showsActions)
        XCTAssertEqual(MessageDraftCardState.sent.receiptOutcome, .confirmed)
        XCTAssertEqual(MessageDraftCardState.sent.receiptLabel, "Sent")
        XCTAssertFalse(MessageDraftCardState.unconfirmed.showsActions)
        XCTAssertEqual(MessageDraftCardState.unconfirmed.receiptOutcome, .unconfirmed)
        XCTAssertEqual(MessageDraftCardState.unconfirmed.receiptLabel, "Unconfirmed")
    }

    func testReceiptOutcomeFigmaNamesAndMetrics() {
        XCTAssertEqual(ActionReceiptOutcome.allCases.map(\.figmaName), ["Confirmed", "Unconfirmed"])
        XCTAssertEqual(ActionReceipt.height, 48)
        XCTAssertEqual(ActionReceipt.successTintOpacity, 0.12)
        XCTAssertEqual(MessageDraftCard.maxWidth, 330)
        XCTAssertEqual(PollCard.maxWidth, 330)
        XCTAssertEqual(PollCard.markerWidth, 20)
    }

    func testDraftDefaultsToNewEmailTitle() {
        let draft = MessageDraft(from: "a@example.com", to: "b@example.com", subject: "S", body: "B")
        XCTAssertEqual(draft.title, "New Email")
    }
}
