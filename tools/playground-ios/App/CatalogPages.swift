import SwiftUI
import RemDesignSystem

// Component catalog pages. Each page renders the shared RemDesignSystem components with the states
// and interactions they already support; nothing here adds component behaviour of its own.

/// Shared page shell: a scrolling column on the primary background with a titled group per component.
struct CatalogPage<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xl) { content() }
                .padding(DesignTokens.Spacing.lg)
        }
        .background(DesignTokens.Color.backgroundPrimary)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct CatalogGroup<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(title)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .accessibilityAddTraits(.isHeader)
            content()
        }
    }
}

private extension View {
    func catalogCard() -> some View {
        background(DesignTokens.Color.backgroundSecondary)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    }
}

// MARK: - Rows

struct CatalogRows: View {
    @State private var opened = 0
    @State private var connector: ConnectorRowState = .available
    @State private var connectorOn = true

    var body: some View {
        CatalogPage(title: "Rows") {
            CatalogGroup(title: "List row") {
                ListRow("Terms of Service",
                        subtitle: opened == 0 ? "How Rem accounts, subscriptions, and approved actions work." : "Opened",
                        action: { opened += 1 },
                        leading: { ContainedIcon("doc.text", fill: .subtle) },
                        trailing: { Image(systemName: "chevron.right").foregroundStyle(DesignTokens.Color.labelTertiary) })
                    .catalogCard()
                    .accessibilityIdentifier("catalog.listRow")
            }
            CatalogGroup(title: "Section") {
                RemSection(header: "Notifications", footer: "Applies to this device.") {
                    ListRow("Daily brief", subtitle: "8:00 AM",
                            leading: { ContainedIcon("sun.max.fill", fill: .tint(DesignTokens.Color.systemOrange)) },
                            trailing: { EmptyView() })
                }
            }
            CatalogGroup(title: "Connector row") {
                Picker("Connector state", selection: $connector) {
                    Text("Available").tag(ConnectorRowState.available)
                    Text("Connecting").tag(ConnectorRowState.connecting)
                    Text("Connected").tag(ConnectorRowState.connected)
                    Text("Error").tag(ConnectorRowState.error)
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("catalog.connectorState")
                ConnectorRow("Gmail", state: connector, accessory: accessory) {
                    ConnectorProviderMark(.gmail)
                }
                .catalogCard()
            }
            CatalogGroup(title: "Task and event rows") {
                VStack(spacing: 0) {
                    TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"])
                    Divider().padding(.leading, 60)
                    TaskEventRow(kind: .event(DesignTokens.Color.systemBlue), title: "Team standup", leading: .time("10:30"), pills: ["Work"])
                    Divider().padding(.leading, 60)
                    TaskEventRow(kind: .task, title: "Unfiled inbox task", leading: .schedule, showPills: false)
                }
                .catalogCard()
            }
        }
    }

    /// Each state shows the accessory the component pairs it with; Connect and Retry move through
    /// Connecting to Connected, as a real connection would.
    private var accessory: ConnectorRowAccessory {
        switch connector {
        case .available: return .action("Connect", { connect() })
        case .connecting: return .progress
        case .connected: return .toggle($connectorOn)
        case .error: return .action("Retry", { connect() })
        }
    }

    private func connect() {
        connector = .connecting
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(1.5))
            if connector == .connecting { connector = .connected }
        }
    }
}

// MARK: - Agenda

struct CatalogAgenda: View {
    @State private var dayOffset = 0
    @State private var resolved: [String: String] = [:]
    @State private var remaining = CatalogAgenda.sectionSuggestions
    @State private var expanded = false

    private static let suggestions: [(id: String, action: AgendaSuggestionRow.Action, title: String, metadata: String)] = [
        ("add", .add, "Confirm rehearsal time", "From Slack · Today"),
        ("move", .move, "Reply to the venue", "Due today · Move to 3:00 PM"),
    ]

    var body: some View {
        CatalogPage(title: "Agenda") {
            CatalogGroup(title: "Date navigation") {
                DateNavigationHeader(dateText: dateText,
                                     onPrevious: { dayOffset -= 1 },
                                     onNext: { dayOffset += 1 })
                    .frame(maxWidth: .infinity)
            }
            CatalogGroup(title: "Suggestion rows") {
                VStack(spacing: 0) {
                    ForEach(Self.suggestions, id: \.id) { item in
                        if let outcome = resolved[item.id] {
                            ListRow(item.title, subtitle: outcome, leading: { EmptyView() }, trailing: { EmptyView() })
                        } else {
                            AgendaSuggestionRow(
                                action: item.action, title: item.title, metadata: item.metadata,
                                acceptIdentifier: "catalog.suggestion.accept.\(item.id)",
                                dismissIdentifier: "catalog.suggestion.dismiss.\(item.id)",
                                onAccept: { resolved[item.id] = item.action == .add ? "Added" : "Moved" },
                                onDismiss: { resolved[item.id] = "Dismissed" }
                            )
                        }
                    }
                }
                .catalogCard()
                if !resolved.isEmpty {
                    Button("Restore") { resolved = [:] }
                        .remButton(.textAccent)
                        .accessibilityIdentifier("catalog.suggestion.restore")
                }
            }
            CatalogGroup(title: "Suggestion section") {
                SuggestionSection(
                    suggestions: remaining,
                    inlineLimit: expanded ? remaining.count : SuggestionSection.defaultInlineLimit,
                    onAccept: { accepted in remaining.removeAll { $0.id == accepted.id } },
                    onDismiss: { dismissed in remaining.removeAll { $0.id == dismissed.id } },
                    onSeeMore: { expanded = true }
                )
                if remaining.count < Self.sectionSuggestions.count || expanded {
                    Button("Reset") { remaining = Self.sectionSuggestions; expanded = false }
                        .remButton(.textAccent)
                        .accessibilityIdentifier("catalog.section.reset")
                }
            }
        }
    }

    // The same four suggestions as the Compose catalog: three inline, one behind See more.
    private static let sectionSuggestions: [AgendaSuggestionItem] = [
        AgendaSuggestionItem(id: "testflight", action: .add, title: "Set up the TestFlight pipeline", metadata: "From Granola · 8h ago"),
        AgendaSuggestionItem(id: "compose", action: .add, title: "Confirm the Compose UI renders", metadata: "From Granola · 8h ago"),
        AgendaSuggestionItem(id: "deposit", action: .move, title: "Reply to the venue about the deposit", metadata: "Overdue 3d"),
        AgendaSuggestionItem(id: "offsite", action: .add, title: "Book the venue for the offsite", metadata: "From Slack · Today"),
    ]

    private var dateText: String {
        var base = DateComponents()
        base.year = 2026; base.month = 10; base.day = 1
        let calendar = Calendar(identifier: .gregorian)
        let date = calendar.date(byAdding: .day, value: dayOffset, to: calendar.date(from: base)!)!
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMM d yyyy"
        return formatter.string(from: date)
    }
}

// MARK: - Chat

struct CatalogChat: View {
    @State private var draft = ""
    @State private var sent: [String] = []
    @State private var voice: VoiceBarState = .listening

    var body: some View {
        CatalogPage(title: "Chat") {
            CatalogGroup(title: "Message bubbles") {
                VStack(spacing: DesignTokens.Spacing.md) {
                    MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
                    MessageBubble("Done — I archived 38 newsletters and snoozed 5 low-priority threads.", role: .assistant)
                    ForEach(sent.indices, id: \.self) { index in
                        MessageBubble(sent[index], role: .user, meta: "Now")
                    }
                }
            }
            CatalogGroup(title: "Composer") {
                RemComposerBar(
                    text: $draft,
                    state: draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .active,
                    accessibilityPrefix: "catalog",
                    onSend: send
                )
            }
            CatalogGroup(title: "Voice bar") {
                Picker("Voice state", selection: $voice) {
                    ForEach(VoiceBarState.allCases, id: \.self) { state in
                        Text(state.rawValue.capitalized).tag(state)
                    }
                }
                .pickerStyle(.menu)
                .accessibilityIdentifier("catalog.voiceState")
                VoiceBar(voice)
            }
        }
    }

    private func send() {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        sent.append(text)
        draft = ""
    }
}

// MARK: - Agent

struct CatalogAgent: View {
    @State private var browser: BrowserLiveCardState = .active
    @State private var reading = false

    var body: some View {
        CatalogPage(title: "Agent") {
            CatalogGroup(title: "Status pill") {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    AgentStatusPill("Working")
                    AgentStatusPill("Needs you", tone: .attention)
                }
            }
            CatalogGroup(title: "Running task banner") {
                VStack(spacing: DesignTokens.Spacing.sm) {
                    RunningTaskBanner(task: "Browser", status: "Signing in to my.dnb.com")
                    RunningTaskBanner(task: "Browser", status: "Needs you · Password rejected", tone: .attention)
                }
            }
            CatalogGroup(title: "Browser card") {
                Picker("Browser state", selection: $browser) {
                    ForEach(BrowserLiveCardState.allCases, id: \.self) { state in
                        Text(state.rawValue.capitalized).tag(state)
                    }
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("catalog.browserState")
                BrowserLiveCard(browser)
            }
            CatalogGroup(title: "Execution trace") {
                ExecutionTrace(
                    status: .inProgress,
                    title: "Build RFE checklist",
                    subtitle: "Writing the RFE checklist PDF template",
                    timestamp: "10:49pm",
                    steps: [
                        .init(label: "Launched checklist subagent", detail: "Delegated the checklist prep to a subagent.",
                              lane: .main, status: .done),
                        .init(label: "Prepared source directories", detail: "The concatenate step returned an incomplete payload.",
                              lane: .subagent("01"), status: .failed),
                        .init(label: "Found official guidance", detail: "Searched official guidance for the request.",
                              lane: .subagent("01"), status: .done),
                    ],
                    footer: "Working"
                )
            }
            CatalogGroup(title: "Daily brief card") {
                DailyBriefCard(
                    title: "Your morning brief",
                    summary: "Three things need you today: the investor update, a venue reply, and rehearsal timing.",
                    onTap: {},
                    onRead: { reading.toggle() },
                    isReading: reading
                )
            }
        }
    }
}

// MARK: - Brand and empty states

struct CatalogBrand: View {
    @State private var thinking = false
    @State private var added = false

    var body: some View {
        CatalogPage(title: "Brand & empty states") {
            CatalogGroup(title: "Face mark") {
                Toggle("Thinking", isOn: $thinking)
                    .accessibilityIdentifier("catalog.faceThinking")
                RemFaceMark(mode: thinking ? .thinking : .idle, tint: DesignTokens.Color.brandBlue, size: 96)
                    .frame(maxWidth: .infinity)
            }
            CatalogGroup(title: "App icon") {
                RemAppIcon(size: 64, cornerRadius: DesignTokens.CornerRadius.large)
            }
            CatalogGroup(title: "Provider marks") {
                HStack(spacing: DesignTokens.Spacing.md) {
                    RemGoogleGlyph(size: 26)
                    ForEach(ConnectorProvider.allCases) { provider in
                        ConnectorProviderMark(provider)
                    }
                }
                // Each mark hides itself from VoiceOver; expose the row as one labelled element.
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Google, " + ConnectorProvider.allCases.map(\.title).joined(separator: ", "))
                .accessibilityIdentifier("catalog.providerMarks")
            }
            CatalogGroup(title: "Empty state") {
                if added {
                    TaskEventRow(kind: .task, title: "New task", leading: .schedule, showPills: false)
                        .catalogCard()
                    Button("Reset") { added = false }
                        .remButton(.textAccent)
                        .accessibilityIdentifier("catalog.emptyReset")
                } else {
                    RemContentUnavailableView(
                        symbol: "calendar.badge.plus",
                        title: "No agenda yet",
                        message: "Create a new task or schedule existing ones",
                        actionLabel: "Add New",
                        action: { added = true }
                    )
                    .frame(minHeight: 280)
                }
            }
        }
    }
}
