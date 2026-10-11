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

    // Tapback reactions (iMessage reference, 2026-10-11). Paired with ChatGeometryTest.kt.

    private typealias T = MessageReactionBadgeMetrics

    func testTapbackConstantsMatchTheComposeTwin() {
        XCTAssertEqual(T.badge, 34)
        XCTAssertEqual(T.ring, 2)
        XCTAssertEqual(T.badgeOuter, 38)
        XCTAssertEqual(T.emoji, 19)
        XCTAssertEqual(T.outward, G.reactionOverlap)
        XCTAssertEqual(T.lift, 20)
        XCTAssertEqual(T.topReserve, 22)
        XCTAssertEqual(G.incomingReactionTop, T.topReserve)
        XCTAssertEqual(T.stackStep, 20)
        XCTAssertEqual(T.maxVisible, 3)
    }

    func testOutgoingTapbackOverlapsTheTopLeadingCorner() {
        XCTAssertEqual(T.Corner(role: .user), .topLeading)
        let l = T.layout(count: 1, corner: .topLeading)
        XCTAssertEqual(l.offsetFromCorner, CGSize(width: -16, height: -22))
        XCTAssertEqual(l.size, CGSize(width: 38, height: 47))
        XCTAssertEqual(l.badges, [CGRect(x: 0, y: 0, width: 38, height: 38)])
        // The badge disc in bubble coordinates: 20 of its 34 overlap horizontally, 14 vertically.
        let discLeft = l.offsetFromCorner.width + l.badges[0].minX + T.ring
        let discTop = l.offsetFromCorner.height + l.badges[0].minY + T.ring
        XCTAssertEqual(discLeft, -14)
        XCTAssertEqual(discTop, -20)
        XCTAssertEqual(discLeft + T.badge, 20)
        XCTAssertEqual(discTop + T.badge, 14)
    }

    func testIncomingTapbackMirrorsToTheTopTrailingCorner() {
        XCTAssertEqual(T.Corner(role: .assistant), .topTrailing)
        let out = T.layout(count: 2, corner: .topLeading)
        let inc = T.layout(count: 2, corner: .topTrailing)
        XCTAssertEqual(inc.offsetFromCorner, CGSize(width: 16, height: out.offsetFromCorner.height))
        XCTAssertEqual(inc.size, out.size)
        // Front badge at the outer edge of the stack, the next one 20 further toward the bubble.
        XCTAssertEqual(inc.badges.map(\.minX), [20, 0])
        for (o, i) in zip(out.badges + out.dots, inc.badges + inc.dots) {
            XCTAssertEqual(o.minX, inc.size.width - i.maxX)
            XCTAssertEqual(o.minY, i.minY)
        }
    }

    func testTapbackTailIsTwoShrinkingDotsBelowOutwardOfTheFrontBadge() {
        let l = T.layout(count: 1, corner: .topLeading)
        XCTAssertEqual(l.dots, [CGRect(x: 4, y: 30, width: 12, height: 12), CGRect(x: 1, y: 39, width: 8, height: 8)])
        let large = l.dots[0], small = l.dots[1]
        XCTAssertTrue(small.minX < large.minX && large.minX < l.badges[0].midX, "dots trail outward")
        XCTAssertTrue(small.minY > large.minY && large.minY > T.badgeOuter / 2, "dots trail downward")
    }

    func testStackedTapbacksOverlapAndCapAtThree() {
        let two = T.layout(count: 2, corner: .topLeading)
        XCTAssertEqual(two.badges.map(\.minX), [0, 20])
        XCTAssertGreaterThan(two.badges[0].maxX, two.badges[1].minX, "each badge overlaps the previous one")
        XCTAssertEqual(two.size.width, 58)
        let many = T.layout(count: 5, corner: .topLeading)
        XCTAssertEqual(many.badges.count, 3)
        XCTAssertEqual(many.size.width, 78)
        let none = T.layout(count: 0, corner: .topTrailing)
        XCTAssertTrue(none.badges.isEmpty && none.dots.isEmpty)
    }

    func testTapbackStaysInsideItsReservedClearance() {
        for corner in [T.Corner.topLeading, .topTrailing] {
            for count in 1...3 {
                let l = T.layout(count: count, corner: corner)
                let frames = l.badges + l.dots
                // Never above the reserved top clearance, so it cannot reach the previous row.
                XCTAssertEqual(l.offsetFromCorner.height + frames.map(\.minY).min()!, -T.topReserve)
                // Never further outward than the incoming row's trailing reserve.
                let outward = corner == .topLeading
                    ? -(l.offsetFromCorner.width + frames.map(\.minX).min()!)
                    : l.offsetFromCorner.width - l.size.width + frames.map(\.maxX).max()!
                XCTAssertEqual(outward, 16)
                XCTAssertLessThanOrEqual(outward, G.incomingTrailingReserve)
            }
        }
    }

    func testTapbackAccessibilityNamesTheWholeSet() {
        XCTAssertEqual(T.accessibilityLabel(for: [.heart]), "Reaction: Heart")
        XCTAssertEqual(T.accessibilityLabel(for: [.thumbsUp, .heart]), "Reactions: Thumbs up, Heart")
    }

    func testDisplayReactionsPutTheOwnReactionInFrontAndDefaultToNoExtras() {
        var message = ChatMessageDisplay(id: "m", role: .user, text: "Hi", reaction: .heart)
        XCTAssertEqual(message.additionalReactions, [])
        XCTAssertEqual(message.reactions, [.heart])
        let plain = message
        message.additionalReactions = [.thumbsUp]
        XCTAssertEqual(message.reactions, [.heart, .thumbsUp])
        XCTAssertNotEqual(plain, message)
        message.reaction = nil
        XCTAssertEqual(message.reactions, [.thumbsUp])
        XCTAssertEqual(T.stack(primary: .thumbsUp, additional: [.thumbsUp]), [.thumbsUp, .thumbsUp])
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

    func testModelTriggerPrefersTheHostLabelForAnUnlistedSelection() {
        let providers = [
            ChatModelProvider(id: "p", name: "Provider", models: [ChatModelOption(id: "m1", name: "Model One")]),
        ]
        let byok = ChatModelSelection.model(id: "anthropic/claude-sonnet-4-5")
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: byok, providers: providers, fallbackLabel: "Claude Sonnet 4.5"),
                       "Claude Sonnet 4.5")
        // A listed model keeps its catalog name; Automatic keeps the Automatic label.
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .model(id: "m1"), providers: providers, fallbackLabel: "Other"), "Model One")
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: .automatic, providers: providers, fallbackLabel: "Other"), "Auto")
        // Blank or Automatic-label fallbacks never disguise the selection: the raw id is shown.
        for fallback in [nil, "", "  ", "Auto"] as [String?] {
            XCTAssertEqual(ChatModelMenu.triggerLabel(for: byok, providers: providers, fallbackLabel: fallback),
                           "anthropic/claude-sonnet-4-5", "\(String(describing: fallback))")
        }
        XCTAssertEqual(ChatModelMenu.triggerLabel(for: byok, providers: [], automaticLabel: "Automatic", fallbackLabel: "Auto"), "Auto")
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
