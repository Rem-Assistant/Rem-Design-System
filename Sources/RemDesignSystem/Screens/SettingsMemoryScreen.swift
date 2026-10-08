import SwiftUI

// Memory destination for the bounded Settings playground — Figma masters
// `Screen/Memory` (`1833:5096`) and `Screen/Memory summary` (`1865:6193`).
//
// Code-only prototype: every state change below is an in-memory fixture mutation that lives for the
// playground session. There is no chat generation, no memory persistence, and no backend. The screen
// exposes a public, zero-argument, deterministic entry point (`SettingsMemoryScreen`) and owns its own
// nested navigation through the host's `NavigationStack` (the root integration owns registering the
// Agent Settings → Memory route; this screen never nests its own navigation container).

/// The three independent Memory control toggles. Figma `1833:5089/5090/5091`.
public enum MemoryControl: String, CaseIterable, Sendable {
    case searchAndReference, generateMemory, sensitiveTopics
}

/// One heading + paragraph of the generated summary. Figma `1865:6185…6190`.
public struct MemorySummarySection: Hashable, Sendable {
    public let heading: String
    public let body: String
    public init(heading: String, body: String) {
        self.heading = heading
        self.body = body
    }
}

/// Deterministic Memory fixture: three independent toggles plus the static summary copy. The toggle
/// defaults are the exact source defaults (two on, Sensitive topics off) and are independent — the
/// source settles no mutual-exclusion rule, so none is invented.
public struct MemoryFixture: Sendable {
    public var searchAndReference: Bool
    public var generateMemory: Bool
    public var sensitiveTopics: Bool

    public init(searchAndReference: Bool = true,
                generateMemory: Bool = true,
                sensitiveTopics: Bool = false) {
        self.searchAndReference = searchAndReference
        self.generateMemory = generateMemory
        self.sensitiveTopics = sensitiveTopics
    }

    public func isOn(_ control: MemoryControl) -> Bool {
        switch control {
        case .searchAndReference: return searchAndReference
        case .generateMemory: return generateMemory
        case .sensitiveTopics: return sensitiveTopics
        }
    }

    public mutating func set(_ control: MemoryControl, _ on: Bool) {
        switch control {
        case .searchAndReference: searchAndReference = on
        case .generateMemory: generateMemory = on
        case .sensitiveTopics: sensitiveTopics = on
        }
    }

    // Exact source copy (`settings-destinations.md` · Memory).
    public static let controlsFooter =
        "Rem can learn from conversations. You stay in control of what it keeps."
    public static let overviewFooter =
        "Open the generated summary to ask Rem to correct, add, or forget something."
    public static let summaryMetadata =
        "Updated just now · Generated from your conversations"
    public static let composerPlaceholder = "Ask or update memory"

    public static let summarySections: [MemorySummarySection] = [
        MemorySummarySection(
            heading: "Overview",
            body: "You prefer direct, practical help and clear product decisions. Rem should use prior conversations when they are relevant, keep durable preferences current, and avoid treating short-lived details as permanent facts."),
        MemorySummarySection(
            heading: "How Rem should work",
            body: "Be resourceful before asking. Use the tools and context already available, show what changed, and keep proposed ideas distinct from behavior that exists in the product."),
        MemorySummarySection(
            heading: "Current focus",
            body: "You are simplifying Rem around a user-centered UI layer, connected capabilities, automations, and a memory model that is useful without exposing infrastructure as product."),
    ]

    /// The deterministic, local send boundary. A nonempty draft is "noted" for the session only;
    /// an empty draft is a no-op. There is deliberately no generated reply, no attachment flow, and
    /// no persistence — the source authors none here.
    public static func composerFeedback(for draft: String) -> String? {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        return "Noted in this prototype session. Rem doesn’t reply or change memory here."
    }
}

private enum MemoryRoute: Hashable { case summary }

/// Memory root (`1833:5096`): a native grouped List of three independent toggle rows plus a
/// navigable "Memory summary" row. Zero-argument, deterministic entry point.
public struct SettingsMemoryScreen: View {
    @State private var fixture = MemoryFixture()

    public init() {}

    public var body: some View {
        List {
            Section {
                toggleRow(.searchAndReference, "Search and reference chats", "Use details from past chats")
                toggleRow(.generateMemory, "Generate memory", "Update the summary from chats")
                toggleRow(.sensitiveTopics, "Sensitive topics", "Allow sensitive details")
            } header: {
                Text("Memory controls")
            } footer: {
                Text(MemoryFixture.controlsFooter)
            }

            Section {
                NavigationLink(value: MemoryRoute.summary) {
                    ListRowLabel("Memory summary", subtitle: "Generated from your conversations")
                }
                .accessibilityIdentifier("memory.summaryRow")
            } header: {
                Text("Overview")
            } footer: {
                Text(MemoryFixture.overviewFooter)
            }
        }
        .settingsDestinationList()
        .navigationTitle("Memory")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .accessibilityIdentifier("settingsMemory")
        .accessibilityHint(PlaygroundMockData.hint)
        .navigationDestination(for: MemoryRoute.self) { route in
            switch route {
            case .summary: MemorySummaryView()
            }
        }
    }

    private func toggleRow(_ control: MemoryControl, _ title: String, _ subtitle: String) -> some View {
        Toggle(isOn: Binding(
            get: { fixture.isOn(control) },
            set: { fixture.set(control, $0) }
        )) {
            ListRowLabel(title, subtitle: subtitle)
        }
        .tint(DesignTokens.Color.systemGreen)
        .accessibilityIdentifier("memory.toggle.\(control.rawValue)")
    }
}

/// Memory summary (`1865:6193`): scrolling freeform summary with a simple interactive composer pinned
/// below. The composer is the Memory-specific interactive variant called for by the Settings source
/// contract (the shared `RemComposerBar` is a static presentational component with model/Speak slots);
/// it shows only the plus + send affordances and runs the deterministic local send boundary.
private struct MemorySummaryView: View {
    @State private var draft = ""
    @State private var feedback: String?

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                    Text(MemoryFixture.summaryMetadata)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                    ForEach(MemoryFixture.summarySections, id: \.self) { section in
                        Text(section.heading)
                            .font(DesignTokens.Typography.body)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .accessibilityAddTraits(.isHeader)
                        Text(section.body)
                            .font(DesignTokens.Typography.body)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    if let feedback {
                        Text(feedback)
                            .font(DesignTokens.Typography.footnote)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .accessibilityIdentifier("memory.composerFeedback")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(DesignTokens.Spacing.lg)
            }
            composer
        }
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationTitle("Memory summary")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .accessibilityIdentifier("memorySummary")
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var composer: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            TextField(MemoryFixture.composerPlaceholder, text: $draft, axis: .vertical)
                .font(DesignTokens.Typography.chatMessage)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .accessibilityIdentifier("memory.composerField")

            HStack(spacing: DesignTokens.Spacing.sm) {
                Image(systemName: "plus")
                    .font(.system(size: 17, weight: .regular))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                Spacer(minLength: DesignTokens.Spacing.sm)
                Button(action: send) {
                    ZStack {
                        Circle().fill(canSend ? DesignTokens.Color.brandBlue : DesignTokens.Color.fillTertiary)
                        Image(systemName: "arrow.up")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(canSend ? DesignTokens.Color.labelOnColor : DesignTokens.Color.labelSecondary)
                    }
                    .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
                .disabled(!canSend)
                .accessibilityLabel("Send")
                .accessibilityIdentifier("memory.composerSend")
            }
        }
        .padding(DesignTokens.Spacing.md)
        .background(DesignTokens.Color.backgroundSecondary,
                    in: RoundedRectangle(cornerRadius: 30, style: .continuous))
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.sm)
    }

    private func send() {
        feedback = MemoryFixture.composerFeedback(for: draft)
        draft = ""
    }
}

#if DEBUG
#Preview("Memory") {
    NavigationStack { SettingsMemoryScreen() }
}
#Preview("Memory summary") {
    NavigationStack { MemorySummaryView() }
}
#endif
