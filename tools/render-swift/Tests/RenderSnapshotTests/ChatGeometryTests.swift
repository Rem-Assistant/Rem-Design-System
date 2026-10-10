import XCTest
@testable import RemDesignSystem

/// The Chat slice's pure contract — responsive bubble geometry, the model trigger label, the reaction
/// set and the Add to Chat limits. Paired with `ChatGeometryTest.kt`; both assert the same numbers so
/// the SwiftUI and Compose layouts cannot drift silently.
final class ChatGeometryTests: XCTestCase {
    private typealias G = MessageBubbleGeometry

    /// 320pt screen, 16pt transcript gutters: a 288pt row. Accepted Figma fixture (`2659:21942`).
    private let narrowRow: CGFloat = 320 - 2 * 16

    func testFailedRowAtNarrowWidthReservesFailureAffordanceAndFills() {
        XCTAssertEqual(G.failureReserve, 52)
        let bubble = G.bubbleWidth(idealWidth: 900, available: narrowRow, failed: true)
        XCTAssertEqual(bubble, 236)
        XCTAssertEqual(G.textWidth(bubbleWidth: bubble), 204)
    }

    func testDeliveredRowAtNarrowWidthFillsWholeRow() {
        XCTAssertEqual(G.bubbleWidth(idealWidth: 900, available: narrowRow, failed: false), 288)
    }

    func testShortMessageHugsItsText() {
        XCTAssertEqual(G.bubbleWidth(idealWidth: 120, available: 370, failed: false), 120)
        XCTAssertEqual(G.bubbleWidth(idealWidth: 120, available: narrowRow, failed: true), 120)
    }

    func testWideRowsCapAt320RatherThanStretching() {
        XCTAssertEqual(G.bubbleWidth(idealWidth: 900, available: 1_000, failed: false), 320)
        XCTAssertEqual(G.bubbleWidth(idealWidth: 900, available: 1_000, failed: true), 320)
        XCTAssertEqual(G.textWidth(bubbleWidth: 320), 288)
    }

    func testGeometryNeverGoesNegative() {
        XCTAssertEqual(G.bubbleLimit(available: 30, failed: true), 0)
        XCTAssertEqual(G.textWidth(bubbleWidth: 10), 0)
        XCTAssertEqual(G.incomingWidth(available: 12), 0)
    }

    func testIncomingKeepsRoomForItsUpperRightReaction() {
        XCTAssertEqual(G.incomingWidth(available: narrowRow), 258)
        XCTAssertEqual(G.incomingWidth(available: 1_000), 320)
        XCTAssertLessThanOrEqual(G.reactionOverlap, G.incomingTrailingReserve)
    }

    func testModelTriggerReadsAutoForAutomaticAndTheModelNameOtherwise() {
        let providers = [
            ChatModelProvider(id: "p", name: "Provider", models: [ChatModelOption(id: "m1", name: "Model One")]),
        ]
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .automatic, providers: providers), "Auto")
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .model(id: "m1"), providers: providers), "Model One")
        // An id the runtime catalog does not contain is shown as-is, never disguised as Automatic.
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .model(id: "gone"), providers: providers), "gone")
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .automatic, providers: []), "Auto")
    }

    func testStandardReactionsAreTheApprovedSixInOrder() {
        XCTAssertEqual(MessageReaction.standardChoices.map(\.emoji), ["👍", "👎", "❤️", "😂", "🎉", "😮"])
        XCTAssertEqual(Set(MessageReaction.standardChoices.map(\.id)).count, 6)
    }

    func testAddToChatLimitsAndThinkingLevels() {
        XCTAssertEqual(AddToChatSheet.maxPhotoSelection, 4)
        XCTAssertEqual(ThinkingLevel.allCases.map(\.title), ["Off", "Low", "Medium", "High"])
    }

    func testLegacyAttachmentFlagShowsTheCloudBrowserCapabilityChip() {
        XCTAssertEqual(ComposerAttachment.cloudBrowser.kind, .capability)
        XCTAssertEqual(ComposerAttachment.cloudBrowser.title, "Cloud browser")
    }
}
