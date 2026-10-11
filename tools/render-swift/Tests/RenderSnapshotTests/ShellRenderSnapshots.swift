#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// iOS render evidence for the Playground app shell (Build 9, `docs/playground/route-map.md`): the root
/// conversation with the header sidebar button (`2702:2351`) and the sidebar pushed open over that same
/// conversation (`2702:2479`), light and dark. Pairs with the Compose `Shell-*` shots (`ShellEvidence.kt`).
/// Same harness as `SidebarRenderSnapshots.swift`, in its own file so the shell does not edit the gallery.
@MainActor
final class ShellRenderSnapshots: XCTestCase {

    func testRenderShell() {
        var shell = PlaygroundShellFixture()
        shell.openSidebar()
        let open = shell
        for dark in [false, true] {
            let suffix = dark ? "dark" : "light"
            render("Shell-chat-\(suffix)", width: 402, height: 874, dark: dark) {
                Self.shellRootChat()
            }
            render("Shell-sidebar-\(suffix)", width: 402, height: 874, dark: dark) {
                RemSidebarContainer(isOpen: .constant(open.isSidebarOpen)) {
                    RemSidebar(open.sidebar) { _ in }
                } content: {
                    Self.shellRootChat()
                }
            }
        }
    }

    // A free-standing builder: the sidebar container's content closure escapes, so it must not capture self.
    private static func shellRootChat() -> some View {
        let fixture = ChatPlaygroundFixture.root()
        return ChatScreen(
            header: fixture.header,
            composer: fixture.composer.state,
            replyContext: fixture.replyContext,
            emptyState: fixture.emptyState,
            onAction: { _ in }
        ) {
            ChatTranscriptList(fixture.entries) { _ in }
        }
    }

    // MARK: - iOS hosting-controller snapshot (mirrors RenderSnapshots.render)

    private func render(_ name: String, width: CGFloat, height: CGFloat, dark: Bool,
                        @ViewBuilder _ content: () -> some View) {
        let root = content()
            .frame(width: width)
            .environment(\.colorScheme, dark ? .dark : .light)

        let host = UIHostingController(rootView: AnyView(root))
        host.overrideUserInterfaceStyle = dark ? .dark : .light
        let size = CGSize(width: width, height: height)

        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.overrideUserInterfaceStyle = dark ? .dark : .light
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.frame = CGRect(origin: .zero, size: size)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let capture = {
            host.view.setNeedsLayout()
            host.view.layoutIfNeeded()
            return renderer.image { ctx in host.view.layer.render(in: ctx.cgContext) }
        }
        // Settle on layout, not a fixed sleep: the composition measures its header and dock through
        // preferences and lays out again, so capture until two consecutive frames match (bounded).
        var image = capture()
        for _ in 0..<20 {
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
            let next = capture()
            let settled = next.pngData() == image.pngData()
            image = next
            if settled { break }
        }

        let attachment = XCTAttachment(image: image)
        attachment.name = "\(name).png"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
#endif
