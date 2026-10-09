import SwiftUI
import UIKit
import RemDesignSystem

@main
struct RemSettingsPlaygroundApp: App {
    @Environment(\.dynamicTypeSize) private var systemTypeSize
    var body: some Scene {
        WindowGroup {
            PlaygroundHome()
                .preferredColorScheme(ProcessInfo.processInfo.arguments.contains("--settings-dark") ? .dark : (ProcessInfo.processInfo.arguments.contains("--settings-light") ? .light : nil))
                .dynamicTypeSize(ProcessInfo.processInfo.arguments.contains("--settings-large-text") ? .accessibility3 : systemTypeSize)
        }
    }
}

enum LoadFixture: String, CaseIterable { case success = "Success", slow = "Slow", error = "Error" }

/// The established onboarding order (`docs/playground/expansion-design`, Compose `remOnboardingSteps`):
/// Sign in → Consent → Connectors → Check-in → Voice. Continue / Skip advance to the next step; the
/// last step completes the flow.
enum OnboardingStep: Int, CaseIterable, Hashable {
    case signIn, consent, connectors, checkIn, voice

    var title: String {
        switch self {
        case .signIn: return "Sign in"
        case .consent: return "Privacy"
        case .connectors: return "Connectors"
        case .checkIn: return "Check-in"
        case .voice: return "Voice"
        }
    }

    var identifier: String {
        switch self {
        case .signIn: return "openOnboardingSignIn"
        case .consent: return "openOnboardingConsent"
        case .connectors: return "openOnboardingConnectors"
        case .checkIn: return "openOnboardingCheckIn"
        case .voice: return "openOnboardingVoice"
        }
    }

    var next: OnboardingStep? { OnboardingStep(rawValue: rawValue + 1) }
}

private enum PlaygroundRoute: Hashable {
    case components, controls, rows, agenda, chat, agent, brand, loading
    case settings
    case agendaSuggestions(AgendaSuggestionsFixture)
    case onboarding
    case onboardingStep(OnboardingStep)
    case onboardingComplete(lastAction: String)
}

struct PlaygroundHome: View {
    @State private var path = NavigationPath()
    @State private var fixture = LoadFixture.success
    @State private var agendaFixture = AgendaSuggestionsFixture.loaded

    var body: some View {
        NavigationStack(path: $path) {
            Form {
                Section("Components") {
                    NavigationLink("Component catalog", value: PlaygroundRoute.components)
                        .accessibilityIdentifier("openComponents")
                }
                Section {
                    NavigationLink("Settings", value: PlaygroundRoute.settings)
                        .accessibilityIdentifier("openSettings")
                    Picker("Settings data", selection: $fixture) {
                        ForEach(LoadFixture.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                    }.pickerStyle(.segmented).accessibilityIdentifier("fixturePicker")
                    NavigationLink("Agenda", value: PlaygroundRoute.agendaSuggestions(agendaFixture))
                        .accessibilityIdentifier("openAgendaSuggestions")
                    Picker("Agenda data", selection: $agendaFixture) {
                        ForEach(AgendaSuggestionsFixture.allCases, id: \.self) { Text($0.title).tag($0) }
                    }.pickerStyle(.segmented).accessibilityIdentifier("agendaFixturePicker")
                    NavigationLink("Onboarding", value: PlaygroundRoute.onboarding)
                        .accessibilityIdentifier("openOnboarding")
                } header: {
                    Text("Screens")
                } footer: {
                    let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.1.0"
                    let sha = Bundle.main.object(forInfoDictionaryKey: "RemPlaygroundSourceSHA") as? String ?? "unversioned"
                    Text("Version \(version) · \(String(sha.prefix(12)))")
                        .accessibilityIdentifier("playground.build")
                }
            }
            .navigationTitle("Rem Playground")
            .navigationDestination(for: PlaygroundRoute.self) { route in
                switch route {
                case .components: ComponentCatalog()
                case .controls: ControlsPreview()
                case .rows: CatalogRows()
                case .agenda: CatalogAgenda()
                case .chat: CatalogChat()
                case .agent: CatalogAgent()
                case .brand: CatalogBrand()
                case .loading: LoadingPreview()
                case .settings: SettingsPreview(fixture: fixture)
                case .agendaSuggestions(let agenda): AgendaSuggestionsPreview(fixture: agenda)
                case .onboarding: OnboardingHub()
                case .onboardingStep(let step):
                    OnboardingStepHost(step: step, advance: { advance(from: step, action: $0) })
                case .onboardingComplete(let lastAction):
                    OnboardingComplete(lastAction: lastAction, onDone: returnToOnboardingHub)
                }
            }
            .navigationDestination(for: SettingsEntryDestination.self) { _ in
                AgentPreview(fixture: fixture)
            }
            .navigationDestination(for: AgentSettingsDestination.self) { route in
                switch route {
                case .pairedDevices: SettingsPairedDevicesScreen()
                case .memory: SettingsMemoryScreen()
                case .models: SettingsModelsScreen()
                case .cloudBrowser: SettingsCloudBrowserScreen()
                case .wallet: SettingsWalletScreen()
                case .voice: SettingsVoiceScreen()
                case .connectors: SettingsConnectorsScreen()
                }
            }
        }
    }

    /// Pushes the next onboarding step, or the completion state after the last one, so native Back
    /// walks the flow in reverse.
    private func advance(from step: OnboardingStep, action: String) {
        if let next = step.next {
            path.append(PlaygroundRoute.onboardingStep(next))
        } else {
            path.append(PlaygroundRoute.onboardingComplete(lastAction: action))
        }
    }

    /// Completion returns to the onboarding hub (the first pushed route).
    private func returnToOnboardingHub() {
        path.removeLast(max(path.count - 1, 0))
    }
}

// MARK: - Components

struct ComponentCatalog: View {
    private static let pages: [(title: String, route: PlaygroundRoute, identifier: String)] = [
        ("Controls", .controls, "openControls"),
        ("Rows", .rows, "openRows"),
        ("Agenda", .agenda, "openCatalogAgenda"),
        ("Chat", .chat, "openChat"),
        ("Agent", .agent, "openAgentCatalog"),
        ("Brand & empty states", .brand, "openBrand"),
        ("Loading", .loading, "openLoading"),
    ]

    var body: some View {
        List {
            ForEach(Self.pages, id: \.identifier) { page in
                NavigationLink(page.title, value: page.route)
                    .accessibilityIdentifier(page.identifier)
            }
        }
        .navigationTitle("Components")
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Skeleton → content for a content load, and an inline progress indicator for an action.
struct LoadingPreview: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var loaded = false
    @State private var attempt = 0
    @State private var refreshing = false

    private let rows = ["Paired devices", "Connectors", "Memory", "Voice"]

    var body: some View {
        List {
            Section {
                if loaded {
                    ForEach(rows, id: \.self) { title in
                        HStack(spacing: DesignTokens.Spacing.md) {
                            ContainedIcon("circle.grid.2x2.fill", fill: .tint(DesignTokens.Color.systemBlue), size: .settings)
                            Text(title)
                        }
                    }
                    .transition(.opacity)
                } else {
                    SkeletonList(rows: rows.count, label: "Loading content")
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                        .accessibilityIdentifier("loading.skeleton")
                        .transition(.opacity)
                }
            }
            Section {
                Button {
                    refreshing = true
                } label: {
                    HStack {
                        Text(refreshing ? "Refreshing" : "Refresh")
                        Spacer()
                        if refreshing { ProgressView() }
                    }
                }
                .disabled(!loaded || refreshing)
                .accessibilityIdentifier("loading.refresh")
                Button("Reload") { attempt += 1 }
                    .disabled(!loaded)
                    .accessibilityIdentifier("loading.reload")
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: loaded)
        .navigationTitle("Loading")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: attempt) {
            loaded = false
            do {
                try await Task.sleep(for: .seconds(3))
                loaded = true
                announce("Content loaded")
            } catch { /* Leaving the screen cancels the load. */ }
        }
        .task(id: refreshing) {
            guard refreshing else { return }
            do {
                try await Task.sleep(for: .seconds(2.5))
                refreshing = false
                announce("Refreshed")
            } catch { refreshing = false }
        }
    }
}

struct ControlsPreview: View {
    private static let buttons: [(title: String, variant: RemButtonVariant)] = [
        ("Rect · Black", .rectBlack), ("Rect · Blue", .rectBlue), ("Rect · Secondary", .rectSecondary),
        ("Rect · Destructive", .rectDestructive), ("Text · Accent", .textAccent),
        ("Text · Destructive", .textDestructive), ("Pill · Secondary", .pillSecondary),
    ]
    @State private var lastButton: String?
    @State private var level = 0.5
    @State private var enabled = true
    @State private var name = "Avery Diaz"
    @State private var draft = ""
    @State private var editing = false
    var body: some View {
        Form {
            Section {
                Toggle("Notifications", isOn: $enabled)
                Button { draft = name; editing = true } label: {
                    HStack { Text("Display name"); Spacer(); Text(name).foregroundStyle(.secondary) }
                }.accessibilityIdentifier("editName")
                HStack {
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .settings)
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .small)
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .large)
                }
            }
            Section("Buttons") {
                // One row per button so each tap reaches only its own action.
                ForEach(Self.buttons, id: \.title) { button in
                    Button(button.title) { lastButton = button.title }.remButton(button.variant)
                        .listRowBackground(Color.clear)
                }
                Button("Disabled") {}.remButton(.rectBlack).disabled(true)
                    .listRowBackground(Color.clear)
                if let lastButton {
                    Text("Tapped \(lastButton)").accessibilityIdentifier("controls.lastButton")
                }
            }
            Section("Slider") {
                RemSlider(value: $level)
                    .accessibilityIdentifier("controls.slider")
                Text("\(Int((level * 100).rounded()))%").foregroundStyle(.secondary)
            }
            Section("Pills") {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    RemPill("3 tasks", kind: .list)
                    RemPill("Standup", kind: .dot(DesignTokens.Color.systemBlue))
                    RemPill("Personal")
                }
            }
        }.navigationTitle("Controls")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $editing) {
                NavigationStack {
                    Form { TextField("Display name", text: $draft).accessibilityIdentifier("nameField") }
                        .navigationTitle("Edit name").navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) { Button("Cancel") { editing = false } }
                            ToolbarItem(placement: .confirmationAction) {
                                Button("Save") { name = draft.trimmingCharacters(in: .whitespacesAndNewlines); editing = false }
                                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            }
                        }
                }
            }
    }
}

// MARK: - Settings

struct SettingsPreview: View {
    let fixture: LoadFixture
    @State private var sharing = false
    var body: some View {
        SettingsEntryContent(onShare: { sharing = true }, agentRoute: .agentSettings)
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle("Settings").navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $sharing) { PlaygroundShareSheet() }
    }
}

/// The operating system owns recipient selection and sending. Opening this sheet sends nothing.
private struct PlaygroundShareSheet: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: ["Rem — a personal AI assistant."], applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

struct AgentPreview: View {
    let fixture: LoadFixture
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var status = "loading"
    @State private var attempt = 0
    var body: some View {
        // A stable container owns the load task while its loading/ready child changes.
        ZStack {
            if status == "ready" {
                AgentSettingsContent(availableDestinations: [.pairedDevices, .connectors, .cloudBrowser, .memory, .models, .wallet, .voice])
                    .transition(.opacity)
            } else if status == "loading" {
                VStack(spacing: 20) {
                    SkeletonList(rows: 7, label: "Loading agent settings")
                        .accessibilityIdentifier("agentSettings.skeleton")
                    Button("Cancel") { dismiss() }.accessibilityIdentifier("cancelLoad")
                    Spacer(minLength: 0)
                }
                .padding(DesignTokens.Spacing.lg)
                .transition(.opacity)
            } else {
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.orange).font(.largeTitle)
                        .accessibilityHidden(true)
                    Text("Couldn’t load agent settings").font(.headline)
                    Button("Retry") { attempt += 1 }.remButton(.rectBlue).accessibilityIdentifier("retry")
                    Button("Cancel") { dismiss() }.accessibilityIdentifier("cancelLoad")
                }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: status)
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationTitle("Agent settings").navigationBarTitleDisplayMode(.inline)
        .task(id: attempt) {
            // Native pushes can reattach this host's task. Once loaded, keep the
            // source links stable while a child destination is being presented.
            guard status != "ready" else { return }
            status = "loading"
            do {
                try await Task.sleep(for: .seconds(fixture == .slow && attempt == 0 ? 10 : 0.2))
                try Task.checkCancellation()
                status = fixture == .error && attempt == 0 ? "error" : "ready"
                announce(status == "ready" ? "Agent settings loaded" : "Couldn’t load agent settings")
            } catch { /* Navigation cancels this view-owned task. */ }
        }
    }
}

// MARK: - Agenda

struct AgendaSuggestionsPreview: View {
    let fixture: AgendaSuggestionsFixture
    var body: some View {
        AgendaSuggestionsPlaygroundView(fixture: fixture)
            .navigationTitle("Agenda New")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Onboarding

/// Entry points into each established onboarding step; every step continues to the next.
struct OnboardingHub: View {
    var body: some View {
        List {
            ForEach(OnboardingStep.allCases, id: \.self) { step in
                NavigationLink(step.title, value: PlaygroundRoute.onboardingStep(step))
                    .accessibilityIdentifier(step.identifier)
            }
        }
        .navigationTitle("Onboarding")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct OnboardingStepHost: View {
    let step: OnboardingStep
    /// Advances with the action that moved the flow on ("continue" or "skip").
    let advance: (String) -> Void

    var body: some View {
        switch step {
        case .signIn:
            // Auth is unresolved for the playground: both providers advance without signing in.
            OnboardingSignInTemplate(
                state: .new,
                onPrimary: { advance("continue") },
                onGoogle: { advance("continue") }
            )
        case .consent:
            OnboardingConsentStep(onAccept: { advance("continue") })
        case .connectors:
            OnboardingConnectorsStep(onContinue: { advance("continue") }, onSkip: { advance("skip") })
        case .checkIn:
            OnboardingCheckInStep(onContinue: { advance("continue") }, onSkip: { advance("skip") })
        case .voice:
            OnboardingVoicePlaygroundScreen(
                onContinue: { advance("continue") },
                onSkip: { advance("skip") }
            )
        }
    }
}

/// Consent with working Terms / Privacy rows: each opens the shared legal-document sheet. The sheet
/// body reuses the row's own description; real legal copy belongs to the shipping app.
private struct OnboardingConsentStep: View {
    let onAccept: () -> Void
    @State private var document: LegalDocumentRoute?

    private struct LegalDocumentRoute: Identifiable {
        let id: String
        let summary: String
    }

    private static let terms = LegalDocumentRoute(id: "Terms of Service", summary: "How Rem accounts, subscriptions, and approved actions work.")
    private static let privacy = LegalDocumentRoute(id: "Privacy Policy", summary: "What Rem, your gateway, and AI or voice providers process.")

    var body: some View {
        OnboardingConsentTemplate(
            message: "Rem uses your data to answer you and act on the things you ask. You can review or delete it anytime in Settings.",
            legalItems: [
                .init(symbol: "doc.text", title: Self.terms.id, subtitle: Self.terms.summary) { document = Self.terms },
                .init(symbol: "shield", title: Self.privacy.id, subtitle: Self.privacy.summary) { document = Self.privacy },
            ],
            footnote: "By tapping \u{201C}Accept and Continue,\u{201D} you agree to our Terms of Service and Privacy Policy.",
            onPrimary: onAccept
        )
        .sheet(item: $document) { route in
            LegalDocumentTemplate(
                title: route.id,
                sections: [.init(heading: route.id, body: route.summary)],
                onClose: { document = nil }
            )
        }
    }
}

/// Connectors with local connect toggles; Continue and Skip move the flow on.
private struct OnboardingConnectorsStep: View {
    let onContinue: () -> Void
    let onSkip: () -> Void
    @State private var connected: Set<String> = ["Gmail"]

    private static let catalog: [(symbol: String, tint: Color, name: String)] = [
        ("envelope.fill", DesignTokens.Color.systemRed, "Gmail"),
        ("calendar", DesignTokens.Color.systemBlue, "Google Calendar"),
        ("number", DesignTokens.Color.systemPurple, "Slack"),
    ]

    var body: some View {
        OnboardingConnectorsTemplate(
            connectors: Self.catalog.map { item in
                let isConnected = connected.contains(item.name)
                return .init(symbol: item.symbol, tint: item.tint, name: item.name,
                             status: isConnected ? "Connected" : "Not connected",
                             isConnected: isConnected) {
                    if isConnected { connected.remove(item.name) } else { connected.insert(item.name) }
                }
            },
            showSeeMore: false,
            onContinue: onContinue,
            onSkip: onSkip
        )
    }
}

/// Check-in keeps its place in the flow. Its time choices are not settled, so this step only
/// routes Continue / Skip; the native Check-in implementation lives in PR #53.
private struct OnboardingCheckInStep: View {
    let onContinue: () -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Spacer()
            ContainedIcon("sun.horizon.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
                .accessibilityHidden(true)
            Text("Check-in")
                .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
            Text("Check-in times are pending.")
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
            Spacer()
            VStack(spacing: DesignTokens.Spacing.xs) {
                Button(action: onContinue) { Text("Continue").frame(maxWidth: .infinity) }
                    .remPrimaryActionButton()
                    .accessibilityIdentifier("onboardingCheckIn.continue")
                Button("Skip", action: onSkip)
                    .remButton(.textAccent)
                    .accessibilityIdentifier("onboardingCheckIn.skip")
            }
        }
        .multilineTextAlignment(.center)
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.sm)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// The flow's completion state. Done returns to the onboarding hub.
struct OnboardingComplete: View {
    let lastAction: String
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Spacer()
            ContainedIcon("checkmark", fill: .tint(DesignTokens.Color.systemGreen), size: .large)
                .accessibilityHidden(true)
            Text("Onboarding complete")
                .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .accessibilityIdentifier("onboarding.complete")
                .accessibilityValue(lastAction)
            Spacer()
            Button(action: onDone) { Text("Done").frame(maxWidth: .infinity) }
                .remPrimaryActionButton()
                .accessibilityIdentifier("onboarding.done")
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.sm)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationBarBackButtonHidden()
        .onAppear { announce("Onboarding complete") }
    }
}


// MARK: - Skeleton loading

/// Moving highlight for skeleton placeholders. Ported from the shipping app's `ShimmerModifier`
/// (`RemClaw/Shared/Views/DesignTokens.swift`), with one addition: when Reduce Motion is on the
/// placeholder stays static instead of sweeping.
struct ShimmerModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var phase: CGFloat = -1

    func body(content: Content) -> some View {
        content
            .overlay {
                if !reduceMotion {
                    GeometryReader { proxy in
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.3), .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: proxy.size.width)
                        .offset(x: phase * proxy.size.width)
                    }
                    .mask(content)
                    .allowsHitTesting(false)
                }
            }
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) { phase = 1 }
            }
    }
}

extension View {
    /// Applies the skeleton shimmer (static under Reduce Motion).
    func shimmering() -> some View { modifier(ShimmerModifier()) }
}

/// One rounded placeholder bar, in the skeleton fill used by the shipping app's row skeletons.
struct SkeletonBlock: View {
    private let width: CGFloat?
    private let height: CGFloat
    private let cornerRadius: CGFloat

    init(width: CGFloat? = nil, height: CGFloat = 14, cornerRadius: CGFloat = 4) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(DesignTokens.Color.labelSecondary.opacity(0.2))
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: .leading)
    }
}

/// Grouped-list skeleton: an icon tile plus a title bar per row, inside one rounded section.
/// Exposed to assistive technologies as a single element carrying `label`, so VoiceOver reads the
/// loading state once rather than every placeholder bar.
struct SkeletonList: View {
    private let rows: Int
    private let label: String

    init(rows: Int = 4, label: String) {
        self.rows = rows
        self.label = label
    }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<rows, id: \.self) { index in
                HStack(spacing: DesignTokens.Spacing.md) {
                    SkeletonBlock(width: 30, height: 30, cornerRadius: 7)
                    SkeletonBlock(width: index.isMultiple(of: 2) ? 160 : 120)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .frame(minHeight: 52)
                if index < rows - 1 {
                    Divider().padding(.leading, DesignTokens.Spacing.lg + 30 + DesignTokens.Spacing.md)
                }
            }
        }
        .background(DesignTokens.Color.backgroundSecondary, in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium))
        .shimmering()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

/// Posts a polite VoiceOver/assistive announcement (for example, "Agent settings loaded") when a
/// loading state resolves without moving focus.
@MainActor
func announce(_ message: String) {
    UIAccessibility.post(notification: .announcement, argument: message)
}
