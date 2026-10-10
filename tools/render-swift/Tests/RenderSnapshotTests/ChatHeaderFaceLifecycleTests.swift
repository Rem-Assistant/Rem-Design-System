#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// Diagnostics only (assertions are unchanged): records each turn's ink samples and the failures
/// recorded in this class, and prints them once the test bundle finishes. The render workflow keeps
/// only the last 60 lines of the `xcodebuild` log, where per-test failure messages no longer appear;
/// the bundle-finish report lands inside that window. Output is bounded (a few short lines).
final class FaceInkReport: NSObject, XCTestObservation {
    static let shared = FaceInkReport()
    private static let maxFailures = 6
    private static let maxLength = 360
    private var registered = false
    private var lines: [String] = []
    private var failures: [String] = []

    /// Idempotent; called from the tests on the main thread.
    func register() {
        guard !registered else { return }
        registered = true
        XCTestObservationCenter.shared.addTestObserver(self)
    }

    func note(_ line: String) {
        lines.append(String(line.prefix(Self.maxLength)))
    }

    func testCase(_ testCase: XCTestCase, didRecord issue: XCTIssue) {
        guard testCase is ChatHeaderFaceLifecycleTests, failures.count < Self.maxFailures else { return }
        failures.append(String("\(testCase.name): \(issue.compactDescription)".prefix(Self.maxLength)))
    }

    func testBundleDidFinish(_ testBundle: Bundle) {
        print("[face-ink-report] begin (\(failures.count) failure(s) recorded)")
        lines.forEach { print("[face-ink-report] \($0)") }
        failures.forEach { print("[face-ink-report] failure \($0)") }
        print("[face-ink-report] end")
        fflush(stdout)
    }
}

/// Native lifecycle check for the Chat header face (`ChatScreen` → `ChatHeader` → `RemFaceMark`):
/// when a turn ends, by Stop or by reply complete, the idle face must keep its full outline like the
/// header did before the turn. The Playground journey `testChatScreenSendStopAcceptAndRead` captured
/// an idle header with eyes and smile but no outline after send / stop / accept / read / reply complete,
/// while the never-thinking empty state kept it.
///
/// Drives the real composition with `ChatPlaygroundFixture` in a live window and counts brand-blue
/// (12, 80, 255) pixels in the avatar region of layer renders sampled over more than one 1.6s
/// outline-draw cycle. A sanity phase requires the thinking draw to visibly animate in this harness
/// (a near-complete frame, a mostly undrawn frame and a wide range between them), so the idle
/// assertion cannot pass vacuously. `layer.render(in:)` captures the in-process layer tree, not
/// the render server's presentation; post-transition journey screenshots remain the visual evidence.
@MainActor
final class ChatHeaderFaceLifecycleTests: XCTestCase {
    private final class Model: ObservableObject {
        @Published var fixture = ChatPlaygroundFixture()
    }

    private struct Host: View {
        @ObservedObject var model: Model
        var body: some View {
            ChatScreen(
                header: model.fixture.header,
                composer: model.fixture.composer.state,
                onAction: { model.fixture.handle($0) }
            ) {
                ChatTranscriptList(model.fixture.entries) { model.fixture.handle(.transcript($0)) }
            }
            .environment(\.colorScheme, .light)
        }
    }

    private static let size = CGSize(width: 402, height: 874)
    /// The 64pt avatar is centred at the top of the screen; transcript content starts below this.
    private static let avatarRegion = CGRect(x: 160, y: 0, width: 82, height: 130)

    private func mount(_ model: Model) -> (UIWindow, UIView) {
        let host = UIHostingController(rootView: Host(model: model))
        host.overrideUserInterfaceStyle = .light
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.size))
        window.overrideUserInterfaceStyle = .light
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.frame = window.bounds
        host.view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(0.3))
        return (window, host.view)
    }

    /// Brand-blue pixels inside the avatar region of a layer render of `view`.
    private func faceInk(_ view: UIView) -> Int {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        let image = UIGraphicsImageRenderer(size: view.bounds.size, format: format).image { ctx in
            view.layer.render(in: ctx.cgContext)
        }
        guard let cg = image.cgImage, let data = cg.dataProvider?.data, let bytes = CFDataGetBytePtr(data) else { return 0 }
        let bpp = cg.bitsPerPixel / 8, row = cg.bytesPerRow
        let blueFirst = cg.bitmapInfo.contains(.byteOrder32Little) // BGRA
        let region = Self.avatarRegion.intersection(CGRect(x: 0, y: 0, width: cg.width, height: cg.height))
        var count = 0
        for y in Int(region.minY)..<Int(region.maxY) {
            for x in Int(region.minX)..<Int(region.maxX) {
                let p = bytes + y * row + x * bpp
                let r = Int(blueFirst ? p[2] : p[0]), b = Int(blueFirst ? p[0] : p[2])
                if b > 150 && b - r > 120 { count += 1 }
            }
        }
        return count
    }

    /// Ink counts sampled every 0.1s for `seconds` while the run loop drives SwiftUI animation.
    private func samples(_ view: UIView, seconds: Double) -> [Int] {
        stride(from: 0.0, to: seconds, by: 0.1).map { _ in
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
            return faceInk(view)
        }
    }

    /// Idle → send (thinking) → `endTurn` → idle; the idle face after the turn matches the one before.
    private func assertOutlineSurvivesTurn(_ name: String, endTurn: (inout ChatPlaygroundFixture) -> Void,
                                           file: StaticString = #filePath, line: UInt = #line) {
        FaceInkReport.shared.register()
        let model = Model()
        let (window, view) = mount(model)
        defer { window.isHidden = true }
        XCTAssertFalse(model.fixture.header.isWorking, file: file, line: line)
        let beforeSamples = samples(view, seconds: 0.5)
        let before = beforeSamples.min() ?? 0
        XCTAssertGreaterThan(before, 0, "\(name): the idle face renders brand-blue ink", file: file, line: line)

        model.fixture.composer.apply(.draftChanged("Plan my afternoon"))
        model.fixture.handle(.composer(.send))
        XCTAssertTrue(model.fixture.header.isWorking, "\(name): sending shows the thinking face", file: file, line: line)
        // Sanity: the self-draw must be observable in this harness, not a static frame. Thinking shows
        // the outline only (no eyes or smile), drawn 0→1 every 1.6s at 1.25× the idle pen (0.075 vs
        // 0.06 of the size). The outline is roughly 80% of the idle ink, so a complete thinking outline
        // is about 1.0 × idle and one ~70% drawn is about 0.7 × idle. Over the 0.1s samples (> one
        // cycle) require (a) a near-complete draw, max ≥ 0.7 × idle; (b) an early, mostly undrawn
        // frame, min < 0.5 × idle; and (c) a swing of at least 0.4 × idle between them. A static frame
        // (empty, partial or full) has max == min and cannot meet both (a) and (b); (c) also rules out
        // a small jitter straddling the 0.5–0.7 band. The idle bound below is unchanged.
        let thinking = samples(view, seconds: 1.8)
        let low = Double(thinking.min() ?? 0), high = Double(thinking.max() ?? 0), idle = Double(before)
        XCTAssertGreaterThanOrEqual(high, idle * 0.7,
                                    "\(name) sanity: a near-complete thinking outline is observed (\(thinking), idle \(before))",
                                    file: file, line: line)
        XCTAssertLessThan(low, idle * 0.5,
                          "\(name) sanity: an early, mostly undrawn thinking frame is observed (\(thinking), idle \(before))",
                          file: file, line: line)
        XCTAssertGreaterThanOrEqual(high - low, idle * 0.4,
                                    "\(name) sanity: the thinking outline visibly self-draws here (\(thinking), idle \(before))",
                                    file: file, line: line)

        endTurn(&model.fixture)
        XCTAssertFalse(model.fixture.header.isWorking, "\(name): the turn ended", file: file, line: line)
        let after = samples(view, seconds: 2.0)
        FaceInkReport.shared.note("\(name): before=\(beforeSamples) thinking=\(thinking) after=\(after)")
        XCTAssertGreaterThanOrEqual(Double(after.min() ?? 0), Double(before) * 0.8,
                                    "\(name): the idle face keeps its full outline (\(after), before the turn \(before))",
                                    file: file, line: line)
    }

    func testOutlineSurvivesStop() {
        assertOutlineSurvivesTurn("Stop") { $0.handle(.composer(.cancel)) }
    }

    func testOutlineSurvivesReplyComplete() {
        assertOutlineSurvivesTurn("Reply complete") { fixture in
            fixture.simulateHostAcceptance()
            fixture.simulateReplyComplete()
        }
    }
}
#endif
