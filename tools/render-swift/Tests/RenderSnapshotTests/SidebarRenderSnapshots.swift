#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// Fixtures shared by the sidebar / New agent / profile renders — copy from Figma page `2693:1474`
/// so the iOS and Compose (`SidebarSnapshots.kt`) evidence shows the same content.
enum SidebarFixtures {
    static let sidebar = RemSidebarDisplay(
        mainAgent: .init(id: "rem", name: "Rem", lane: "Daily Brief ready"),
        sideAgents: [
            .init(id: "health", name: "Health", lane: "Appointments & meds"),
            .init(id: "money", name: "Money", lane: "Bills & budget", hasUnread: true),
            .init(id: "travel", name: "Travel planning: Lisbon in May", lane: "Flights, stays"),
            .init(id: "jobs", name: "Job search: recruiter follow-ups and interview prep", lane: "Applications & interviews"),
            .init(id: "home", name: "Home & errands", lane: "Chores & repairs"),
            .init(id: "spanish", name: "Learning Spanish", lane: "Daily practice"),
            .init(id: "gifts", name: "Gift ideas for Mom's birthday", lane: "Gift ideas & reminders"),
        ],
        account: .init(name: "Samuel"),
        selection: .main
    )

    static let calendar = AgentService(id: "gcal", name: "Google Calendar", status: "Connected", glyph: .calendar)

    static let healthProfile = AgentProfileDisplay(
        kind: .side, name: "Health", look: .default, laneSummary: "Appointments and refills",
        handles: "Appointments, prescriptions, insurance claims", services: [calendar]
    )

    static let remProfile = AgentProfileDisplay(
        kind: .main, name: "Rem", laneSummary: "Daily Brief, sees all your lanes",
        handles: "Daily Brief, sees all your lanes"
    )

    static let filledDraft = NewAgentDraft(
        name: "Health", handles: "Appointments, prescriptions, insurance claims", look: .default
    )
}

// Neutral stand-in for the pushed chat: the real chat chassis lives in ChatScreen (not on main).
private func sidebarChatStandIn() -> some View {
    VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
        Image(systemName: "line.3.horizontal")
            .font(.system(size: 19, weight: .medium))
            .frame(width: 44, height: 44)
            .background(DesignTokens.Color.backgroundSecondary, in: Circle())
        Text("Good morning, Samuel")
            .font(DesignTokens.Typography.title3Bold)
        Text("Three things today: the venue replied about Saturday, two threads still need you, and your 2:00 PM moved to 3:00.")
            .font(DesignTokens.Typography.subheadline)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
        Spacer()
    }
    .padding(.horizontal, DesignTokens.Spacing.lg)
    .padding(.top, 60)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .background(DesignTokens.Color.backgroundPrimary)
}

/// iOS render evidence for the sidebar + New agent flow. Same harness as `RenderSnapshots.swift`
/// (UIHostingController + `layer.render`, PNGs emitted as XCTAttachments), kept in its own file so
/// these surfaces don't edit the shared gallery.
@MainActor
final class SidebarRenderSnapshots: XCTestCase {

    func testRenderSidebarAndNewAgentFlow() {
        for dark in [false, true] {
            let suffix = dark ? "dark" : "light"
            render("Sidebar-open-\(suffix)", width: 393, height: 852, dark: dark) {
                RemSidebarContainer(isOpen: .constant(true)) {
                    RemSidebar(SidebarFixtures.sidebar) { _ in }
                } content: {
                    sidebarChatStandIn()
                }
            }
        }
        render("Sidebar-closed-light", width: 393, height: 852, dark: false) {
            RemSidebarContainer(isOpen: .constant(false)) {
                RemSidebar(SidebarFixtures.sidebar) { _ in }
            } content: {
                sidebarChatStandIn()
            }
        }
        render("Sidebar-empty-light", width: 332, height: 852, dark: false) {
            RemSidebar(
                RemSidebarDisplay(mainAgent: SidebarFixtures.sidebar.mainAgent, sideAgents: [],
                                  account: .init(name: "Samuel"))
            ) { _ in }
        }
        render("AccountPill-light", width: 200, height: nil, dark: false) {
            RemAccountPill(.init(name: "Samuel")) {}
                .padding(24)
                .background(DesignTokens.Color.backgroundSecondary)
        }
        render("NewAgent-empty-light", width: 393, height: 852, dark: false) {
            NavigationStack {
                NewAgentSetupScreen(draft: .constant(NewAgentDraft()), focusesNameOnAppear: false) { _ in }
            }
        }
        render("NewAgent-filled-light", width: 393, height: 852, dark: false) {
            NavigationStack {
                NewAgentSetupScreen(draft: .constant(SidebarFixtures.filledDraft), focusesNameOnAppear: false) { _ in }
            }
        }
        render("NewAgent-filled-dark", width: 393, height: 852, dark: true) {
            NavigationStack {
                NewAgentSetupScreen(draft: .constant(SidebarFixtures.filledDraft), focusesNameOnAppear: false) { _ in }
            }
        }
        render("AgentProfile-side-about-light", width: 393, height: 852, dark: false) {
            profile(SidebarFixtures.healthProfile)
        }
        render("AgentProfile-main-about-light", width: 393, height: 852, dark: false) {
            profile(SidebarFixtures.remProfile)
        }
    }

    // The profile header + About tab body, as Agent activity will host them (tab control omitted —
    // it belongs to AgentActivityScreen, see AgentProfileModel.swift).
    private func profile(_ display: AgentProfileDisplay) -> some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.xl) {
                AgentProfileHeader(display)
                AgentProfileAbout(display) { _ in }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, 20)
            .padding(.bottom, 32)
        }
        .background(DesignTokens.Color.backgroundPrimary)
    }

    // MARK: - iOS hosting-controller snapshot (mirrors RenderSnapshots.render)

    private func render(_ name: String, width: CGFloat, height: CGFloat?, dark: Bool,
                        @ViewBuilder _ content: () -> some View) {
        let root = content()
            .frame(width: width)
            .environment(\.colorScheme, dark ? .dark : .light)

        let host = UIHostingController(rootView: AnyView(root))
        host.overrideUserInterfaceStyle = dark ? .dark : .light

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
#endif
