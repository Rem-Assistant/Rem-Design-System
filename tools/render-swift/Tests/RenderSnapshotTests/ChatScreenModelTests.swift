import XCTest
@testable import RemDesignSystem

/// The Chat / Inbox presentation contract's pure rules (`ChatScreenModel.swift`). Paired with
/// `ChatScreenModelTest.kt`; both assert the same rules so the SwiftUI and Compose composers cannot
/// drift. Native focus, keyboard and tap journeys need separate runtime UI evidence.
final class ChatScreenModelTests: XCTestCase {
    private let photo = ComposerAttachment(id: "photo.0", title: "Photo 1", kind: .image)
    private let file = ComposerAttachment(id: "file.a", title: "notes.png", kind: .file)

    func testHostContentEntryIsNeverAMessage() {
        let entries: [ChatTranscriptEntry] = [
            .message(ChatMessageDisplay(id: "u1", role: .user, text: "Plan my day")),
            .hostContent(id: "proposal.1"),
        ]
        XCTAssertEqual(entries[1].id, "proposal.1")
        XCTAssertEqual(ChatTranscriptRules.latestOutgoingID(in: entries), "u1",
                       "A host card after the latest outgoing message never takes its receipt")
    }

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

    func testModelMenuEnabledOverridesTheDerivedRuleOnlyWhenSet() {
        // nil keeps the derived rule.
        XCTAssertTrue(ChatComposerState().modelEnabled)
        XCTAssertFalse(ChatComposerState(availability: .disabled(reason: nil)).modelEnabled)
        // Input blocked (pending provider evidence, quota out) but the menu stays open to escape to Automatic.
        let escape = ChatComposerState(draft: "Hi", availability: .disabled(reason: "Quota reached"), modelMenuEnabled: true)
        XCTAssertTrue(escape.modelEnabled)
        XCTAssertFalse(escape.canSend)
        XCTAssertFalse(escape.showsSpeak)
        XCTAssertNil(escape.primaryAction)
        // The host closes the menu while preparing / sending even with input enabled.
        XCTAssertFalse(ChatComposerState(modelMenuEnabled: false).modelEnabled)
        XCTAssertTrue(ChatComposerState(phase: .sending, modelMenuEnabled: true).modelEnabled)
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
        XCTAssertFalse(header.showsCall, "call is opt-in: hosts show it only when in-app voice is available")
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

    // MARK: Jump to latest (paired with `ChatScreenModelTest.kt`)

    func testJumpToLatestShowsOnlyBeyondTheThreshold() {
        XCTAssertEqual(ChatJumpToLatest.threshold, 200)
        XCTAssertFalse(ChatJumpToLatest.isVisible(distanceFromBottom: 0), "at the bottom")
        XCTAssertFalse(ChatJumpToLatest.isVisible(distanceFromBottom: -40), "overscrolled past the bottom")
        XCTAssertFalse(ChatJumpToLatest.isVisible(distanceFromBottom: 200), "near the bottom")
        XCTAssertTrue(ChatJumpToLatest.isVisible(distanceFromBottom: 201))
        XCTAssertTrue(ChatJumpToLatest.isVisible(distanceFromBottom: 2_000))
    }

    func testJumpToLatestGeometryAndAccessibilityMatchCompose() {
        XCTAssertEqual(ChatJumpToLatest.diameter, 44, "minimum touch target")
        XCTAssertEqual(ChatJumpToLatest.glyphSize, 17)
        XCTAssertEqual(ChatJumpToLatest.bottomSpacing, 12)
        XCTAssertEqual(ChatJumpToLatest.appearScale, 0.8)
        XCTAssertEqual(ChatJumpToLatest.appearDuration, 0.2)
        XCTAssertEqual(ChatJumpToLatest.accessibilityLabel, "Scroll to latest message")
        XCTAssertEqual(ChatJumpToLatest.accessibilityIdentifier, "chat.jumpToLatest")
    }

    // MARK: Header backdrop (paired with `ChatScreenModelTest.kt`)

    func testHeaderBackdropContentInsetEqualsTheChromeHeight() {
        for chrome: CGFloat in [0, 128, 152, 177.5, 260] {
            XCTAssertEqual(ChatHeaderBackdrop.contentTopInset(chromeHeight: chrome), chrome, "inset == chrome \(chrome)")
        }
        XCTAssertEqual(ChatHeaderBackdrop.contentTopInset(chromeHeight: -4), 0, "never negative")
        // Measured edges: status bar 59 above the safe area (viewport top -59), header bottom at 128.
        XCTAssertEqual(ChatHeaderBackdrop.chromeHeight(headerBottom: 128, viewportTop: -59), 187)
        XCTAssertEqual(ChatHeaderBackdrop.chromeHeight(headerBottom: 128, viewportTop: 0), 128)
        // A short conversation fills exactly the area below the header: inset + min height == viewport.
        XCTAssertEqual(ChatHeaderBackdrop.transcriptMinHeight(viewportHeight: 700, topInset: 187) + 187, 700)
        XCTAssertEqual(ChatHeaderBackdrop.transcriptMinHeight(viewportHeight: 100, topInset: 187), 0)
    }

    func testHeaderBackdropFirstMessageIsFullyVisibleAtTheTop() {
        for chrome: CGFloat in [96, 128, 152, 260] {
            let top = ChatHeaderBackdrop.firstMessageTop(chromeHeight: chrome)
            XCTAssertTrue(ChatHeaderBackdrop.isBelowHeader(rowTop: top, chromeHeight: chrome), "below the header at \(chrome)")
            XCTAssertTrue(ChatHeaderBackdrop.isClearOfBackdrop(rowTop: top, chromeHeight: chrome), "clear of the fade at \(chrome)")
        }
        // Scrolled up by even 1pt from that resting place, the row starts to pass under the fade.
        let chrome: CGFloat = 128
        XCTAssertFalse(ChatHeaderBackdrop.isClearOfBackdrop(rowTop: ChatHeaderBackdrop.firstMessageTop(chromeHeight: chrome) - 1,
                                                            chromeHeight: chrome))
        XCTAssertFalse(ChatHeaderBackdrop.isBelowHeader(rowTop: chrome - 1, chromeHeight: chrome))
    }

    func testHeaderBackdropMaskFadesToClearJustBelowTheHeader() {
        XCTAssertEqual(ChatHeaderBackdrop.fadeHeight, 20)
        XCTAssertEqual(ChatHeaderBackdrop.fadeOverhang, ChatHeaderBackdrop.transcriptTopPadding,
                       "fade overhang == transcript top padding")
        XCTAssertEqual(ChatHeaderBackdrop.transcriptTopPadding, 12)
        XCTAssertEqual(ChatHeaderBackdrop.topOpacity, 1)
        XCTAssertEqual(ChatHeaderBackdrop.bottomOpacity, 0)
        XCTAssertEqual(ChatHeaderBackdrop.hairline, 0.5)
        let chrome: CGFloat = 128
        XCTAssertEqual(ChatHeaderBackdrop.backdropHeight(chromeHeight: chrome), 140)
        let stops = ChatHeaderBackdrop.maskStops(chromeHeight: chrome)
        XCTAssertEqual(stops.count, 3)
        XCTAssertEqual(stops[0].location, 0)
        XCTAssertEqual(stops[0].opacity, 1)
        // The fade starts 8pt (the header's bottom gap) above the header's bottom edge.
        XCTAssertEqual(stops[1].location, (chrome - 8) / 140, accuracy: 1e-6)
        XCTAssertEqual(stops[1].opacity, 1)
        XCTAssertEqual(stops[2].location, 1)
        XCTAssertEqual(stops[2].opacity, 0)
        // With no measured chrome yet, the whole (overhang-high) backdrop is fade.
        XCTAssertEqual(ChatHeaderBackdrop.maskStops(chromeHeight: -12).map { $0.location }, [0, 0, 1])
    }

    // MARK: Long-press message actions (`2603:19498`)

    func testAssistantMessageSheetMatchesTheReferenceGroups() {
        let sheet = ChatMessageActionsDisplay(message: ChatMessageDisplay(id: "a1", role: .assistant, text: "Hi", reaction: .heart))
        XCTAssertEqual(sheet.messageID, "a1")
        XCTAssertEqual(sheet.groups, [[.reply, .markUnread], [.copy, .selectText], [.report]])
        XCTAssertEqual(sheet.selection, .heart, "The current reaction is the picker's selection")
        XCTAssertEqual(sheet.reactions.count, 11, "2 × 6 grid: eleven reactions and the + cell")
        XCTAssertEqual(Array(sheet.reactions.prefix(6)), MessageReaction.standardChoices)
        XCTAssertTrue(sheet.showsMoreReactions)
    }

    func testOwnMessagesNeverOfferReportOrMarkAsUnread() {
        let sheet = ChatMessageActionsDisplay(message: ChatMessageDisplay(id: "u1", role: .user, text: "Hi"))
        XCTAssertEqual(sheet.groups, [[.reply], [.copy, .selectText]])
        XCTAssertFalse(sheet.groups.joined().contains(.report))
    }

    func testUnavailableActionsAreHiddenAndEmptyGroupsDisappear() {
        let groups = ChatMessageActionsDisplay.groups(for: .assistant, available: [.copy, .report])
        XCTAssertEqual(groups, [[.copy], [.report]])
        XCTAssertEqual(ChatMessageActionsDisplay.groups(for: .user, available: [.report]), [], "Report never reaches own messages")
    }

    func testMessageActionTitlesAndSymbolsFollowTheReference() {
        XCTAssertEqual(ChatMessageAction.allCases.map(\.title), ["Reply", "Mark as unread", "Copy", "Select Text", "Report"])
        XCTAssertEqual(ChatMessageAction.allCases.map(\.rawValue), ["reply", "markUnread", "copy", "selectText", "report"],
                       "Identifier segments match the Compose keys")
        XCTAssertEqual(ChatMessageAction.allCases.map(\.systemImage),
                       ["arrowshape.turn.up.left", "message", "doc.on.doc", "selection.pin.in.out", "flag.fill"])
    }
}
