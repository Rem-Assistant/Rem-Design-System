#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// iOS render evidence for the May adoption-parity changes, paired by name with the Compose
/// `AdoptionParityEvidence.kt` shots: the TaskEventRow app time column and completed ring, the Schedule
/// sheet's empty states, and the avatar slot on ChatHeader / RemSidebar. (The iOS sheets keep the native
/// `.segmented` picker, so `RemSegmentedPicker-*` is Compose-only.)
@MainActor
final class AdoptionParityRenderSnapshots: XCTestCase {

    func testRenderAdoptionParity() {
        for dark in [false, true] {
            render("TaskEventRow-app-\(dark ? "dark" : "light")", width: 402, height: nil, dark: dark) { taskEventRows }
        }
        for filter in AgendaScheduleFilter.allCases {
            render("AgendaScheduleEmpty-\(filter.rawValue)-light", width: 393, height: 852, dark: false) {
                AgendaScheduleSheet(fixture: AgendaEntryFixture(backlog: []), initialFilter: filter)
            }
        }
        render("ChatHeader-customAvatar-light", width: 402, height: nil, dark: false) { chatHeaders }
        render("Sidebar-customAvatar-light", width: 332, height: 852, dark: false) {
            RemSidebar(SidebarFixtures.sidebar, mainAgentAvatar: sampleMascot) { _ in }
        }
    }

    @ViewBuilder private var taskEventRows: some View {
        VStack(alignment: .leading, spacing: 0) {
            caption("Compact (default, unchanged)")
            TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"])
            caption("App parity (leadingStyle: .app)")
            TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"],
                         leadingStyle: .app)
            TaskEventRow(kind: .event(DesignTokens.Color.systemBlue), title: "Team standup", leading: .time("14:30"),
                         pills: ["Work"], leadingStyle: .app)
            TaskEventRow(kind: .task, title: "Send the venue deposit", leading: .time("8:00 AM"), completed: true,
                         leadingStyle: .app)
            TaskEventRow(kind: .task, title: "Unfiled inbox task", leading: .schedule, showPills: false, leadingStyle: .app)
            TaskEventRow(kind: .task, title: "Anytime task", leading: .clock, showPills: false, leadingStyle: .app)
            TaskEventRow(kind: .task, title: "Unparseable label falls back", leading: .time("Oct 14"), showPills: false,
                         leadingStyle: .app)
            caption("Completed, compact")
            TaskEventRow(kind: .task, title: "Book the rehearsal room", leading: .time("10:00"), completed: true)
        }
        .padding(DesignTokens.Spacing.lg)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var chatHeaders: some View {
        VStack(spacing: DesignTokens.Spacing.xl) {
            caption("Default: the Rem face")
            ChatHeader(activity: "Connected")
            caption("avatar = sample mascot (idle, thinking)")
            ChatHeader(name: "May", activity: "Connected", onOverflow: {}, onMenu: {}, avatar: sampleMascot)
            ChatHeader(name: "May", activity: "Reading the shared notes", faceMode: .thinking, avatar: sampleMascot)
        }
        .padding(.vertical, DesignTokens.Spacing.lg)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(DesignTokens.Typography.caption1)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.xs)
    }

    /// Test-only stand-in for an adopting product's mascot (a gray two-humped figure with black oval eyes,
    /// after May's): it proves the slot, not the art. Thinking glances up. Same drawing as Compose.
    private var sampleMascot: RemAvatarSlot {
        { mode, size in AnyView(SampleMascot(thinking: mode == .thinking).frame(width: size, height: size)) }
    }

    // MARK: - iOS hosting-controller snapshot (mirrors RenderSnapshots.render)

    private func render(_ name: String, width: CGFloat, height: CGFloat?, dark: Bool,
                        @ViewBuilder _ content: () -> some View) {
        let root = content()
            .frame(width: width)
            .environment(\.colorScheme, dark ? .dark : .light)

        let host = UIHostingController(rootView: AnyView(root))
        host.overrideUserInterfaceStyle = dark ? .dark : .light
        if height == nil {
            host.safeAreaRegions = []
        }

        let fitHeight = height
            ?? host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude)).height
        let size = CGSize(width: width, height: fitHeight)

        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.overrideUserInterfaceStyle = dark ? .dark : .light
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.frame = CGRect(origin: .zero, size: size)
        host.view.setNeedsLayout()
        host.view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            host.view.layer.render(in: ctx.cgContext)
        }

        let attachment = XCTAttachment(image: image)
        attachment.name = "\(name).png"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

private struct SampleMascot: View {
    let thinking: Bool

    var body: some View {
        Canvas { context, size in
            let w = size.width, h = size.height
            let gray = Color(red: 0.604, green: 0.604, blue: 0.627)
            for x: CGFloat in [0.32, 0.68] {
                context.fill(Path(ellipseIn: CGRect(x: w * (x - 0.24), y: h * (0.36 - 0.24 * w / h), width: w * 0.48, height: w * 0.48)), with: .color(gray))
            }
            context.fill(Path(roundedRect: CGRect(x: w * 0.08, y: h * 0.36, width: w * 0.84, height: h * 0.52),
                              cornerRadius: w * 0.2), with: .color(gray))
            let eyeY = thinking ? h * 0.46 : h * 0.54
            for x: CGFloat in [0.38, 0.62] {
                context.fill(Path(ellipseIn: CGRect(x: w * x - w * 0.05, y: eyeY - h * 0.08, width: w * 0.1, height: h * 0.16)),
                             with: .color(.black))
            }
        }
        .accessibilityHidden(true)
    }
}
#endif
