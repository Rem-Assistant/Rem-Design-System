#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// Native layout check for the composer control row at the 320pt width stress fixture (board
/// `2681:21977`): a 320pt screen leaves a 288pt composer (16pt dock gutters) and a 264pt control row.
/// Add, Auto, Speak and Send must all stay on one line at their intrinsic size — the release render
/// `ChatComposition-taskReply-narrow-light` showed Speak wrapping as "Spea" / "k" before the fix.
///
/// Wrapping is detected by height: one-line Speak fits inside the 44pt row, a wrapped pill does not,
/// so the composer's fitted height at 288pt must equal its height at the canonical 370pt.
@MainActor
final class ComposerNarrowLayoutTests: XCTestCase {
    private static let narrowComposer: CGFloat = 320 - 2 * 16
    private static let canonicalComposer: CGFloat = 402 - 2 * 16

    private func fittedHeight<V: View>(_ view: V, width: CGFloat) -> CGFloat {
        let host = UIHostingController(rootView: view.environment(\.colorScheme, .light))
        return host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude)).height
    }

    private func assertOneLineRow<V: View>(_ view: V, _ name: String, file: StaticString = #filePath, line: UInt = #line) {
        let narrow = fittedHeight(view, width: Self.narrowComposer)
        let canonical = fittedHeight(view, width: Self.canonicalComposer)
        XCTAssertGreaterThan(canonical, 0, name, file: file, line: line)
        XCTAssertEqual(narrow, canonical, accuracy: 0.5,
                       "\(name): the control row grew at 288pt (a control wrapped or was pushed to a new line)",
                       file: file, line: line)
    }

    func testHostDrivenComposerKeepsOneLineControlsAtNarrowWidth() {
        assertOneLineRow(
            RemComposerBar(state: ChatComposerState(placeholder: "Write your reply…"), onAction: { _ in }),
            "Idle task-reply composer"
        )
        assertOneLineRow(
            RemComposerBar(state: ChatComposerState(draft: "Plan the next step"), onAction: { _ in }),
            "Composing"
        )
    }

    func testLongModelNameTruncatesInsteadOfWrappingSpeak() {
        // Host-supplied model names can be long: the model label may truncate, Speak may not wrap.
        let name = "Provider Model With A Very Long Name"
        let providers = [ChatModelProvider(id: "provider-a", name: "Provider A", models: [
            ChatModelOption(id: "long", name: name),
        ])]
        assertOneLineRow(
            RemComposerBar(
                state: ChatComposerState(modelLabel: name),
                modelMenu: ChatModelMenu(providers: providers, selection: .model(id: "long"), onSelect: { _ in }),
                onAction: { _ in }
            ),
            "Composer with a long model name"
        )
    }
}
#endif
