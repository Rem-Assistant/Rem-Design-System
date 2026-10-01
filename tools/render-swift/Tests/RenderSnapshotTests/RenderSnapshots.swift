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

// Trailing amount for a wallet transaction row (credits = systemGreen "+", debits = labelPrimary).
// File-scope for the same reason as `chevron()` — it's used inside ListRow escaping trailing closures.
private func walletAmount(_ amount: String, credit: Bool) -> some View {
    Text(amount)
        .font(DesignTokens.Typography.body.weight(.semibold))
        .foregroundStyle(credit ? DesignTokens.Color.systemGreen : DesignTokens.Color.labelPrimary)
        .monospacedDigit()
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
            render("Slider-\(suffix)", width: 320, height: nil, dark: dark) { sliderGallery }
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
        render("ComposerBar-light", width: 420, height: nil, dark: false) { composerBars }
        // Wave 2 screens — components composed into surfaces.
        render("AgendaScreen-light", width: 402, height: 780, dark: false) { agendaScreen() }
        render("InboxScreen-light", width: 402, height: 780, dark: false) { inboxScreen() }
        render("ChatScreen-light", width: 402, height: 820, dark: false) { chatScreen() }
        // Wave 3 screens
        render("TaskDetailScreen-light", width: 402, height: 820, dark: false) { taskDetailScreen() }
        render("SettingsScreen-light", width: 402, height: 820, dark: false) { settingsScreen() }
        // Wave 4 — agent surfaces (product bets) + Wallet proposal
        render("AgentStatusPill-light", width: 320, height: nil, dark: false) { agentStatusPills }
        render("AgentStatusPill-dark", width: 320, height: nil, dark: true) { agentStatusPills }
        render("RunningTaskBanner-light", width: 440, height: nil, dark: false) { runningTaskBanners }
        render("BrowserLiveCard-light", width: 380, height: nil, dark: false) { browserLiveCards }
        render("ExecutionTrace-light", width: 430, height: nil, dark: false) { executionTrace }
        render("WalletScreen-light", width: 402, height: 900, dark: false) { walletScreen() }
    }

    // MARK: - Wave 4 galleries (agent surfaces) + Wallet

    @ViewBuilder private var agentStatusPills: some View {
        VStack(spacing: 14) {
            AgentStatusPill("Working")
            AgentStatusPill("Needs you", tone: .attention)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var runningTaskBanners: some View {
        VStack(spacing: 14) {
            RunningTaskBanner(task: "Browser", status: "Signing in to my.dnb.com")
            RunningTaskBanner(task: "Browser", status: "Needs you · Password rejected", tone: .attention)
        }
        .padding(24)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var browserLiveCards: some View {
        VStack(spacing: 14) {
            BrowserLiveCard(.opening)
            BrowserLiveCard(.active)
            BrowserLiveCard(.ended)
        }
        .padding(24)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    @ViewBuilder private var executionTrace: some View {
        ExecutionTrace(
            status: .inProgress,
            title: "Build RFE checklist",
            subtitle: "Writing the RFE checklist PDF template",
            timestamp: "10:49pm",
            steps: [
                .init(label: "Launched H-1B RFE Checklist Tailoring Subagent",
                      detail: "Delegated the checklist prep to a subagent via artifact.send_input, covering a tailored checklist for USCIS.",
                      lane: .main, status: .done),
                .init(label: "Prepared RFE Checklist Source Directories",
                      detail: "Staged the checklist sources; the concatenate step returned an incomplete JSON payload.",
                      lane: .subagent("01"), status: .failed),
                .init(label: "Found USCIS RFE Official Results",
                      detail: "Web search targeting USCIS official guidance for H-1B Requests for Evidence.",
                      lane: .subagent("01"), status: .done),
                .init(label: "Created index.html source file",
                      detail: "Wrote the RFE checklist HTML template used to render the PDF.",
                      lane: .subagent("01"), status: .done),
            ],
            footer: "Working"
        )
        .padding(24)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    // Wallet proposal body — ported from WalletScreen.swift's preview (its building blocks are
    // preview-private, so the harness reconstructs the same composition from the public API).
    private func walletScreen() -> some View {
        WalletScreen {
            // Balance hero
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xl) {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    HStack(spacing: DesignTokens.Spacing.md) {
                        ContainedIcon("wallet.pass.fill", fill: .tint(DesignTokens.Color.brandBlue))
                        Text("Available balance")
                            .font(DesignTokens.Typography.footnote)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                    }
                    Text("$42.75")
                        .font(DesignTokens.Typography.title1Bold)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text("≈ 1,710 credits remaining")
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                Button(action: {}) { Text("Add funds").frame(maxWidth: .infinity) }
                    .remButton(.rectBlack)
            }
            .padding(DesignTokens.Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DesignTokens.Color.backgroundSecondary)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))

            // Usage summary + budget meter
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                HStack {
                    Text("Spent this period")
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                    Spacer()
                    Text("$18.20")
                        .font(DesignTokens.Typography.body.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .monospacedDigit()
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(DesignTokens.Color.fillTertiary)
                        Capsule().fill(DesignTokens.Color.brandBlue)
                            .frame(width: geo.size.width * (18.20 / 50.00))
                    }
                }
                .frame(height: 8)
                Text("$18.20 of $50.00 monthly budget")
                    .font(DesignTokens.Typography.caption1)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            .padding(DesignTokens.Spacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DesignTokens.Color.backgroundSecondary)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))

            // Recent activity
            RemSection(header: "Recent activity") {
                ListRow("Agent run · Inbox triage", subtitle: "Today, 9:24 AM",
                        leading: { ContainedIcon("bolt.fill", fill: .tint(DesignTokens.Color.systemBlue)) },
                        trailing: { walletAmount("-$0.42", credit: false) })
                Divider().padding(.leading, 60)
                ListRow("Agent run · Draft email reply", subtitle: "Today, 8:10 AM",
                        leading: { ContainedIcon("bolt.fill", fill: .tint(DesignTokens.Color.systemBlue)) },
                        trailing: { walletAmount("-$0.18", credit: false) })
                Divider().padding(.leading, 60)
                ListRow("Top-up", subtitle: "Yesterday",
                        leading: { ContainedIcon("plus.circle.fill", fill: .tint(DesignTokens.Color.systemGreen)) },
                        trailing: { walletAmount("+$20.00", credit: true) })
                Divider().padding(.leading, 60)
                ListRow("Monthly plan credit", subtitle: "Sep 25",
                        leading: { ContainedIcon("arrow.triangle.2.circlepath", fill: .tint(DesignTokens.Color.systemGreen)) },
                        trailing: { walletAmount("+$5.00", credit: true) })
            }

            // Payment method — single-row section (no divider)
            RemSection(header: "Payment") {
                ListRow("Visa ending 4242", subtitle: "Expires 08/27",
                        leading: { ContainedIcon("creditcard.fill", fill: .tint(DesignTokens.Color.systemIndigo)) },
                        trailing: { chevron() })
            }
        }
    }

    private func taskDetailScreen() -> some View {
        TaskDetailScreen(title: "Draft the investor update", dateText: "Oct 1 2026", metaPills: ["Fundraise"], composerState: .idle) {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                Text("LAST ACTIVITY")
                    .font(DesignTokens.Typography.caption1)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                Text("Rem drafted the investor update and pulled last quarter's metrics — want me to send it?")
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                RemSection(header: "Notes") {
                    Text("Add your notes here")
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(DesignTokens.Spacing.md)
                }
            }
        }
    }

    private func settingsScreen() -> some View {
        SettingsScreen {
            RemSection {
                ListRow("Avery Diaz", subtitle: "avery@example.com",
                        leading: { ContainedIcon("person.fill", fill: .tint(DesignTokens.Color.systemIndigo)) },
                        trailing: { EmptyView() })
            }
            RemSection(header: "General") {
                ListRow("Connectors",
                        leading: { ContainedIcon("link", fill: .tint(DesignTokens.Color.systemBlue)) },
                        trailing: { chevron() })
                Divider().padding(.leading, 60)
                ListRow("Voice", subtitle: "Aria",
                        leading: { ContainedIcon("mic.fill", fill: .tint(DesignTokens.Color.systemPurple)) },
                        trailing: { chevron() })
            }
        }
    }

    private func agendaScreen() -> some View {
        AgendaScreen(dateText: "Oct 1 2026", onPrevious: {}, onNext: {}) {
            TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"])
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .event(DesignTokens.Color.systemBlue), title: "Team standup", leading: .time("10:30"), pills: ["Work"])
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .task, title: "Draft the investor update", leading: .time("14:00"), pills: ["Fundraise"])
        }
    }

    private func inboxScreen() -> some View {
        InboxScreen {
            TaskEventRow(kind: .task, title: "Follow up with the Freestyle team", leading: .none, showPills: false)
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .task, title: "Review the Q4 roadmap draft", leading: .none, showPills: false)
            Divider().padding(.leading, 60)
            TaskEventRow(kind: .task, title: "Book the venue for the offsite", leading: .none, showPills: false)
        }
    }

    private func chatScreen() -> some View {
        ChatScreen(composerText: "", composerState: .idle) {
            MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
            MessageBubble(
                "Done — I archived 38 newsletters and snoozed 5 low-priority threads. Want me to draft replies to the two that still need you?",
                role: .assistant
            )
            MessageBubble("Yes, go ahead.", role: .user)
        }
    }

    @ViewBuilder private var composerBars: some View {
        VStack(spacing: 20) {
            RemComposerBar()
            RemComposerBar(text: "Remind me to send the investor update tomorrow", state: .active, showAttachments: true)
            RemComposerBar(text: "Plan the rest of my day", state: .sending)
        }
        .padding(24)
        .background(DesignTokens.Color.backgroundPrimary)
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

    // RemSlider gallery — the wrapped platform slider at a few values.
    @ViewBuilder private var sliderGallery: some View {
        VStack(spacing: 18) {
            RemSlider(value: .constant(0.25))
            RemSlider(value: .constant(0.6))
            RemSlider(value: .constant(0.9))
        }
        .padding(24)
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
