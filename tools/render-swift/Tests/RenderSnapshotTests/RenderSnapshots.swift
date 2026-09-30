#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

// File-scope so the ListRow trailing closures don't capture `self` (escaping closures in a class
// require explicit self otherwise).
private func chevron() -> some View {
    Image(systemName: "chevron.right")
        .font(.system(size: 14, weight: .semibold))
        .foregroundStyle(DesignTokens.Color.labelTertiary)
}

// Faithful iOS screenshot evidence. Runs on an iOS Simulator (via `xcodebuild test`) so the tokens
// resolve to real iOS UIColor semantics — `.systemBackground` is WHITE on iOS (it is grey on macOS,
// which is why the earlier macOS `ImageRenderer` renders looked inverted). Snapshots a real
// `UIHostingController` view hierarchy with `host.view.layer.render(in:)`, which captures
// `ScrollView`/`List` content that `ImageRenderer` cannot — so the ACTUAL `OnboardingConsentTemplate`
// renders faithfully (no scroll-free re-composition). Each PNG is emitted as an `XCTAttachment` and
// extracted from the `.xcresult` afterwards (an env var like SNAPSHOT_OUT_DIR does not cross into the
// simulator process, so the test does not write to disk itself). Consent attachment names are also
// the required keys in `tools/design-sync/manifest.json` and the machine-readable drift report.
@MainActor
final class RenderSnapshots: XCTestCase {

    func testRenderAll() {
        for dark in [false, true] {
            let suffix = dark ? "dark" : "light"
            render("RemButton-\(suffix)", width: 300, height: nil, dark: dark) { buttonGallery }
            render("ContainedIcon-\(suffix)", width: 260, height: nil, dark: dark) { iconRow }
            render("RemFaceMark-\(suffix)", width: 220, height: nil, dark: dark) { faceMark }
            render("ListRow-\(suffix)", width: 380, height: nil, dark: dark) { listRowCard }
            render("Pill-\(suffix)", width: 240, height: nil, dark: dark) { pillGallery }
        }
        render("Consent-default-light", width: 393, height: 852, dark: false) { consentScreen() }
        // Reusable legal content chrome. The host-owned page-sheet container, scrim, and return
        // behavior are verified by the Figma prototype and interaction evidence.
        render("Consent-terms-light", width: 393, height: 852, dark: false) {
            LegalDocumentTemplate(
                title: "Terms of Service",
                sections: LegalDocumentFixtures.terms,
                onClose: {}
            )
        }
        render("Consent-privacy-light", width: 393, height: 852, dark: false) {
            LegalDocumentTemplate(
                title: "Privacy Policy",
                sections: LegalDocumentFixtures.privacy,
                onClose: {}
            )
        }
        // Sign-in states, keyed to pair with the Compose `SignIn-*` shots in the side-by-side table.
        render("SignIn-returning-light", width: 393, height: 852, dark: false) {
            OnboardingSignInTemplate(state: .returning(accountName: "Sam"), onPrimary: {})
        }
        render("SignIn-new-light", width: 393, height: 852, dark: false) {
            OnboardingSignInTemplate(state: .new, onPrimary: {})
        }
        render("SignIn-checking-light", width: 393, height: 852, dark: false) {
            OnboardingSignInTemplate(state: .checking, onPrimary: {})
        }
        render("SignIn-error-dark", width: 393, height: 852, dark: true) {
            OnboardingSignInTemplate(
                state: .error(message: "We couldn't sign you in. Check your connection and try again."),
                onPrimary: {}
            )
        }
        render("SignIn-recovery-light", width: 393, height: 852, dark: false) {
            OnboardingSignInTemplate(
                state: .recovery(message: "Your session expired. Sign in again to pick up where you left off."),
                onPrimary: {}
            )
        }
        // Wave 1 onboarding flows — paired with the Compose `Connectors-*`/`Voice-*`/`AgendaEmpty-*` shots.
        render("Connectors-light", width: 393, height: 852, dark: false) { connectorsScreen() }
        render("Voice-light", width: 393, height: 852, dark: false) { voiceScreen() }
        render("AgendaEmpty-light", width: 393, height: 852, dark: false) {
            RemContentUnavailableView(
                symbol: "calendar.badge.plus",
                title: "No agenda yet",
                message: "Create a new task or schedule existing ones",
                actionLabel: "Add New",
                action: {}
            )
        }
        // Wave 2 core components — paired with the Compose shots of the same names.
        render("DateNavigationHeader-light", width: 402, height: nil, dark: false) { dateNavHeader }
        render("MessageBubble-light", width: 402, height: nil, dark: false) { messageBubbles }
        render("TaskEventRow-light", width: 390, height: nil, dark: false) { taskEventRows }
        render("VoiceBar-light", width: 418, height: nil, dark: false) { voiceBarStack }
    }

    // MARK: - Wave 2 galleries

    @ViewBuilder private var dateNavHeader: some View {
        DateNavigationHeader(dateText: "Oct 1 2026", onPrevious: {}, onNext: {})
            .padding(.vertical, 24)
            .frame(maxWidth: .infinity)
            .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var messageBubbles: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
            MessageBubble(
                "Done — I archived 38 newsletters and snoozed 5 low-priority threads.\n\nWant me to draft replies to the two that still need you?",
                role: .assistant
            )
            MessageBubble("Yes, go ahead.", role: .user, meta: "9:41 AM")
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var taskEventRows: some View {
        VStack(spacing: 0) {
            TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"])
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .event(DesignTokens.Color.systemBlue), title: "Team standup", leading: .time("10:30"), pills: ["Work"])
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .task, title: "Unfiled inbox task", leading: .schedule, showPills: false)
        }
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
        .padding(24)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var voiceBarStack: some View {
        VStack(spacing: 12) {
            ForEach(VoiceBarState.allCases, id: \.self) { state in
                VoiceBar(state)
            }
        }
        .padding(20)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    // MARK: - Galleries (mirror each component's #Preview)

    @ViewBuilder private var buttonGallery: some View {
        let variants: [(String, RemButtonVariant)] = [
            ("Rect · Black", .rectBlack), ("Rect · Blue", .rectBlue),
            ("Rect · Secondary", .rectSecondary), ("Rect · Destructive", .rectDestructive),
            ("Text · Accent", .textAccent), ("Text · Destructive", .textDestructive),
            ("Pill · Secondary", .pillSecondary),
        ]
        VStack(spacing: 14) {
            ForEach(variants.indices, id: \.self) { i in
                Button(variants[i].0) {}.remButton(variants[i].1)
            }
            Button("Disabled") {}.remButton(.rectBlack).disabled(true)
        }
        .padding(24)
    }

    // The canonical Rem face mark — the real branded scalloped blob + eyes + smile (idle resting
    // frame), shared with the Compose `RemFaceMark`. brandBlue on the primary background, matching the
    // app's brand identity (not a generic system "face" glyph).
    @ViewBuilder private var faceMark: some View {
        RemFaceMark(mode: .idle, tint: DesignTokens.Color.brandBlue, size: 96)
            .padding(40)
            .frame(maxWidth: .infinity)
            .background(DesignTokens.Color.backgroundPrimary)
    }

    // Pill gallery — the three quiet-chip kinds (list badge for tasks, colored dot for events, plain).
    @ViewBuilder private var pillGallery: some View {
        VStack(alignment: .leading, spacing: 10) {
            RemPill("3 tasks", kind: .list)
            RemPill("Standup", kind: .dot(DesignTokens.Color.systemBlue))
            RemPill("Personal")
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var iconRow: some View {
        HStack(spacing: 16) {
            ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
            ContainedIcon("doc.text", fill: .subtle)
            ContainedIcon("shield", fill: .subtle)
        }
        .padding(24)
    }

    @ViewBuilder private var listRowCard: some View {
        VStack(spacing: 0) {
            ListRow("Terms of Service",
                    subtitle: "How Rem accounts, subscriptions, and approved actions work.",
                    action: {},
                    leading: { ContainedIcon("doc.text", fill: .subtle) },
                    trailing: { chevron() })
            Divider().padding(.leading, 60)
            ListRow("Privacy Policy",
                    subtitle: "What Rem, your gateway, and AI or voice providers process.",
                    action: {},
                    leading: { ContainedIcon("shield", fill: .subtle) },
                    trailing: { chevron() })
        }
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
        .padding(24)
    }

    // The REAL onboarding consent template (with its ScrollView) — rendered faithfully now.
    private func consentScreen() -> some View {
        OnboardingConsentTemplate(
            // Canonical consent copy (authority: Compose `ConsentStep.kt`), kept identical on both
            // platforms so the side-by-side evidence compares the same screen.
            message: "Rem uses your data to answer you and act on the things you ask. You can review or delete it anytime in Settings.",
            legalItems: [
                .init(symbol: "doc.text", title: "Terms of Service",
                      subtitle: "How Rem accounts, subscriptions, and approved actions work.", action: {}),
                .init(symbol: "shield", title: "Privacy Policy",
                      subtitle: "What Rem, your gateway, and AI or voice providers process.", action: {}),
            ],
            footnote: "By tapping \u{201C}Accept and Continue,\u{201D} you agree to our Terms of Service and Privacy Policy.",
            onPrimary: {}
        )
    }

    // Connectors — onboarding treatment: hero lockup + grouped card (toggle / Connect) on a white flip
    // bg + Continue/Skip. Brand tiles use system-color tokens (token-bound).
    private func connectorsScreen() -> some View {
        OnboardingConnectorsTemplate(
            connectors: [
                .init(symbol: "envelope.fill", tint: DesignTokens.Color.systemRed, name: "Gmail", status: "Connected", isConnected: true, action: {}),
                .init(symbol: "calendar", tint: DesignTokens.Color.systemBlue, name: "Google Calendar", status: "Not connected", isConnected: false, action: {}),
                .init(symbol: "number", tint: DesignTokens.Color.systemPurple, name: "Slack", status: "Not connected", isConnected: false, action: {}),
            ],
            onContinue: {}, onSkip: {}
        )
    }

    // Voice setup — hero + hear/picker rows + Character & speed sliders + Continue (constant bindings).
    private func voiceScreen() -> some View {
        OnboardingVoiceTemplate(
            selectedVoice: "Aria (Warm)",
            onHearVoice: {},
            onSelectVoice: {},
            speed: .constant(0.45),
            consistency: .constant(0.7),
            likeness: .constant(0.6),
            onContinue: {}
        )
    }

    // MARK: - iOS hosting-controller snapshot

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
        // Give SwiftUI a run-loop tick to commit its first render pass before capturing.
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            // Render the layer tree directly — avoids the headless "render server returned error"
            // that drawHierarchy(afterScreenUpdates:) hits in a logic-test simulator.
            host.view.layer.render(in: ctx.cgContext)
        }

        // The SNAPSHOT_OUT_DIR env doesn't cross into the simulator process, so instead of writing
        // to disk we attach the PNG to the test result; the workflow extracts it from the .xcresult.
        let attachment = XCTAttachment(image: image)
        attachment.name = "\(name).png"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
#endif
