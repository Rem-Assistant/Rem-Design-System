#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// Native lifecycle check for the Chat header face (`ChatScreen` → `ChatHeader` → `RemFaceMark`):
/// when a turn ends, by Stop or by reply complete, the idle face must keep its full outline like the
/// header did before the turn. The Playground journey `testChatScreenSendStopAcceptAndRead` captured
/// an idle header with eyes and smile but no outline after send / stop / accept / read / reply complete,
/// while the never-thinking empty state kept it.
///
/// Drives the real composition with `ChatPlaygroundFixture` in a live window and counts brand-blue
/// (12, 80, 255) pixels in the avatar region of layer renders sampled over more than one 1.6s
/// outline-draw cycle. A sanity phase requires the thinking draw to visibly animate in this harness,
/// so the idle assertion cannot pass vacuously.
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
        let model = Model()
        let (window, view) = mount(model)
        defer { window.isHidden = true }
        XCTAssertFalse(model.fixture.header.isWorking, file: file, line: line)
        let before = samples(view, seconds: 0.5).min() ?? 0
        XCTAssertGreaterThan(before, 0, "\(name): the idle face renders brand-blue ink", file: file, line: line)

        model.fixture.composer.apply(.draftChanged("Plan my afternoon"))
        model.fixture.handle(.composer(.send))
        XCTAssertTrue(model.fixture.header.isWorking, "\(name): sending shows the thinking face", file: file, line: line)
        let thinking = samples(view, seconds: 1.8)
        XCTAssertLessThan(Double(thinking.min() ?? 0), Double(before) * 0.5,
                          "\(name) sanity: the thinking outline visibly self-draws here (\(thinking), idle \(before))",
                          file: file, line: line)

        endTurn(&model.fixture)
        XCTAssertFalse(model.fixture.header.isWorking, "\(name): the turn ended", file: file, line: line)
        let after = samples(view, seconds: 2.0)
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
