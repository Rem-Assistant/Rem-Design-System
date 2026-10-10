import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
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
        // Like ChatScreen: scrolling above the keyboard keeps it up (only a drag into it dismisses),
        // so a person can scroll a mid-page composer's controls into view while typing.
        .scrollDismissesKeyboard(.interactively)
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
            // The row specimen stands alone (dashed outline, 4pt apart), as in region frame 2336:19714:
            // no catalog card around it.
            CatalogGroup(title: "Suggestion rows") {
                VStack(spacing: DesignTokens.Spacing.xs) {
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

/// Neutral fictional Chat fixture. Paired with `ChatFixture` in the Android `CatalogPages.kt`: keep the
/// copy, ids and catalog identical. Providers and models are placeholders, never a production catalog;
/// reactions, delivery and attachments live only in this page's local state.
enum ChatFixture {
    static let outgoing = "Can you move the planning sync to Thursday?"
    static let incoming = "Done \u{2014} the planning sync is now Thursday at 10:00, and both attendees have the update."
    static let readMessage = "Thanks, that works."
    static let failedMessage = "Please share the agenda with the group as well."
    /// Illustrative only: the fixture's delivery time, kept when the message is marked Read.
    static let deliveredAt = "10:24"
    static let activities: [(text: String, status: ChatHeader.Status)] = [
        ("Connected", .connected),
        ("Reading the shared notes", .connected),
        ("Needs you", .needsYou),
    ]
    static let providers = [
        ChatModelProvider(id: "provider-a", name: "Provider A", models: [
            ChatModelOption(id: "model-a1", name: "Model A1"),
            ChatModelOption(id: "model-a2", name: "Model A2"),
        ]),
        ChatModelProvider(id: "provider-b", name: "Provider B", models: [
            ChatModelOption(id: "model-b1", name: "Model B1"),
        ]),
    ]
    static let manageModelsNote = "Manage Models opens model settings in the app."
    static let cameraNote = "Camera opens the system camera in the app."
}

private struct ReactionTarget: Identifiable { let id: String }

struct CatalogChat: View {
    @State private var draft = ""
    @State private var sent: [String] = []
    @State private var voice: VoiceBarState = .listening
    @State private var activity = 0
    @State private var reactions: [String: MessageReaction] = ["chat.incoming": .thumbsUp]
    @State private var reactingTo: ReactionTarget?
    @State private var failedDelivered = false
    @State private var model: ChatModelSelection = .automatic
    @State private var attachments: [ComposerAttachment] = []
    @State private var showAddToChat = false
    @State private var browserAvailable = true
    @State private var thinking: ThinkingLevel = .medium
    @State private var showPhotos = false
    @State private var photoItems: [PhotosPickerItem] = []
    @State private var showFiles = false
    @State private var feedback: String?
    @State private var headerCall = false

    var body: some View {
        CatalogPage(title: "Chat") {
            CatalogGroup(title: "Header") {
                // The trailing slot holds one action: the in-app call entry replaces More (WS1e).
                ChatHeader(
                    activity: ChatFixture.activities[activity].text,
                    status: ChatFixture.activities[activity].status,
                    accessibilityPrefix: "chat.header",
                    onOverflow: {},
                    onCall: headerCall ? {} : nil
                )
                Picker("Trailing action", selection: $headerCall) {
                    Text("More").tag(false)
                    Text("Call").tag(true)
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("chat.headerTrailing")
                Picker("Agent activity", selection: $activity) {
                    ForEach(ChatFixture.activities.indices, id: \.self) { index in
                        Text(ChatFixture.activities[index].text).tag(index)
                    }
                }
                .pickerStyle(.menu)
                .accessibilityIdentifier("chat.headerActivity")
            }
            CatalogGroup(title: "Message bubbles") {
                VStack(spacing: DesignTokens.Spacing.md) {
                    message(ChatFixture.outgoing, role: .user, id: "chat.outgoing")
                    message(ChatFixture.incoming, role: .assistant, id: "chat.incoming")
                    message(ChatFixture.readMessage, role: .user, id: "chat.read", delivery: .read(at: ChatFixture.deliveredAt))
                    message(
                        ChatFixture.failedMessage, role: .user, id: "chat.failed",
                        delivery: failedDelivered ? .delivered(at: ChatFixture.deliveredAt) : .failed
                    )
                    ForEach(sent.indices, id: \.self) { index in
                        message(sent[index], role: .user, id: "chat.sent.\(index)")
                    }
                }
            }
            CatalogGroup(title: "Composer") {
                RemComposerBar(
                    text: $draft,
                    state: draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .active,
                    attachments: attachments,
                    modelMenu: ChatModelMenu(
                        providers: ChatFixture.providers, selection: model, accessibilityPrefix: "catalog",
                        onSelect: { model = $0 },
                        onManageModels: { feedback = ChatFixture.manageModelsNote }
                    ),
                    accessibilityPrefix: "catalog",
                    onAdd: { showAddToChat = true },
                    onRemoveAttachment: { removed in attachments.removeAll { $0.id == removed.id } },
                    onSend: send
                )
                Toggle("Cloud browser available", isOn: $browserAvailable)
                    .font(DesignTokens.Typography.footnote)
                    .accessibilityIdentifier("chat.browserAvailable")
                if let feedback {
                    Text(feedback)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .accessibilityIdentifier("chat.feedback")
                }
            }
            CatalogGroup(title: "Composer \u{00B7} sending") {
                RemComposerBar(text: "Plan the rest of my day", state: .sending)
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
        .sheet(item: $reactingTo) { target in
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                Text("Reactions")
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                MessageReactionPicker(selection: reactions[target.id], accessibilityPrefix: "chat.reactions") { choice in
                    reactions[target.id] = choice
                    reactingTo = nil
                }
            }
            .padding(DesignTokens.Spacing.lg)
            .presentationDetents([.height(140)])
        }
        .sheet(isPresented: $showAddToChat) {
            AddToChatSheet(
                showsCamera: UIImagePickerController.isSourceTypeAvailable(.camera),
                browserAvailable: browserAvailable,
                thinking: $thinking,
                accessibilityPrefix: "catalog.addToChat",
                onCamera: { feedback = ChatFixture.cameraNote; showAddToChat = false },
                onPhotos: { showPhotos = true },
                onFiles: { showFiles = true },
                onCloudBrowser: {
                    if !attachments.contains(.cloudBrowser) { attachments.append(.cloudBrowser) }
                    showAddToChat = false
                },
                onDone: { showAddToChat = false }
            )
            .photosPicker(
                isPresented: $showPhotos, selection: $photoItems,
                maxSelectionCount: AddToChatSheet.maxPhotoSelection, matching: .images
            )
            .fileImporter(isPresented: $showFiles, allowedContentTypes: [.image], allowsMultipleSelection: true) { result in
                if case .success(let urls) = result {
                    attachments += urls.map { ComposerAttachment(id: "file.\($0.lastPathComponent)", title: $0.lastPathComponent, kind: .file) }
                    showAddToChat = false
                }
            }
            .presentationDetents([.medium])
        }
        .onChange(of: photoItems) { _, items in
            guard !items.isEmpty else { return }
            // Only the count is used: picked photos stay on the device and are never read here.
            attachments.removeAll { $0.kind == .image }
            attachments += items.indices.map { ComposerAttachment(id: "photo.\($0)", title: "Photo \($0 + 1)", kind: .image) }
            photoItems = []
            showAddToChat = false
        }
    }

    private func message(
        _ text: String, role: MessageBubble.Role, id: String, delivery: MessageBubble.Delivery = .none
    ) -> some View {
        MessageBubble(
            text, role: role, delivery: delivery, reaction: reactions[id], accessibilityPrefix: id,
            onRetry: { failedDelivered = true },
            onLongPress: { reactingTo = ReactionTarget(id: id) }
        )
    }

    /// Same rule as the composer: text, or a content attachment alone (fixture chips; nothing is read).
    private func send() {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = attachments.filter { $0.kind != .capability }
        guard !text.isEmpty || !content.isEmpty else { return }
        sent.append(text.isEmpty ? content.map(\.title).joined(separator: ", ") : text)
        draft = ""
        attachments.removeAll { $0.kind != .capability }
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

// MARK: - Full-screen Chat, task reply and Inbox (Playground 7 candidate)

/// Full-screen canonical Chat driven by the DS `ChatPlaygroundFixture`, exactly as an app adapter would
/// drive it. The header owns Back (exit) and overflow, which opens the **fixture host** controls — the
/// stand-ins for host evidence (acceptance, read acknowledgement, failure, reply). Nothing leaves the
/// page: no message is sent and no receipt exists without one of those explicit fixture controls.
struct PlaygroundChatScreen: View {
    @Environment(\.dismiss) private var dismiss
    @State private var fixture: ChatPlaygroundFixture
    @State private var model: ChatModelSelection = .automatic
    @State private var thinking: ThinkingLevel = .medium
    @State private var showHostControls = false
    @State private var showAddToChat = false
    @State private var showPhotos = false
    @State private var photoItems: [PhotosPickerItem] = []
    @State private var showFiles = false

    init(fixture: ChatPlaygroundFixture) {
        _fixture = State(initialValue: fixture)
    }

    var body: some View {
        ChatScreen(
            header: fixture.header,
            composer: fixture.composer.state,
            replyContext: fixture.replyContext,
            emptyState: fixture.emptyState,
            modelMenu: ChatModelMenu(
                providers: ChatFixture.providers, selection: model, accessibilityPrefix: "chat",
                onSelect: { model = $0 },
                onManageModels: { fixture.composer.show(note: ChatFixture.manageModelsNote) }
            ),
            onAction: handle
        ) {
            ChatTranscriptList(fixture.entries) { handle(.transcript($0)) }
            if let note = fixture.note ?? fixture.composer.note {
                Text(note)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityIdentifier("chat.fixtureNote")
            }
        }
        .confirmationDialog("Fixture host", isPresented: $showHostControls, titleVisibility: .visible) {
            Button("Host accepted the message") { fixture.simulateHostAcceptance() }
                .accessibilityIdentifier("chat.host.accept")
            Button("Recipient acknowledged (Read)") { fixture.simulateReadAcknowledgement() }
                .accessibilityIdentifier("chat.host.read")
            Button("Host reported not delivered") { fixture.simulateDeliveryFailure() }
                .accessibilityIdentifier("chat.host.fail")
            Button("Reply complete") { fixture.simulateReplyComplete() }
                .accessibilityIdentifier("chat.host.reply")
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Stand-ins for evidence the app receives from its runtime. Fixture only.")
        }
        .sheet(isPresented: $showAddToChat) {
            // Real system pickers, as in the Chat catalog: PhotosPicker runs out of process and needs no
            // library permission; the document picker grants access only to what the person picks.
            AddToChatSheet(
                showsCamera: UIImagePickerController.isSourceTypeAvailable(.camera),
                browserAvailable: true,
                thinking: $thinking,
                accessibilityPrefix: "chat.addToChat",
                onCamera: { fixture.composer.show(note: ChatFixture.cameraNote); showAddToChat = false },
                onPhotos: { showPhotos = true },
                onFiles: { showFiles = true },
                onCloudBrowser: { attach(.cloudBrowser) },
                onDone: { showAddToChat = false }
            )
            .photosPicker(
                isPresented: $showPhotos, selection: $photoItems,
                maxSelectionCount: AddToChatSheet.maxPhotoSelection, matching: .images
            )
            .fileImporter(isPresented: $showFiles, allowedContentTypes: [.image], allowsMultipleSelection: true) { result in
                // Only the file name is used: the file is never opened, read or uploaded.
                if case .success(let urls) = result, !urls.isEmpty {
                    fixture.composer.attachPickedFiles(named: urls.map(\.lastPathComponent))
                    showAddToChat = false
                }
            }
            .presentationDetents([.medium])
        }
        .onChange(of: photoItems) { _, items in
            guard !items.isEmpty else { return }
            // Only the count is used: picked photos stay on the device and are never read here.
            fixture.composer.attachPickedPhotos(count: items.count)
            photoItems = []
            showAddToChat = false
        }
    }

    private func handle(_ action: ChatScreenAction) {
        switch fixture.handle(action) {
        case .exit?: dismiss()
        case .presentHostControls?: showHostControls = true
        case .presentAddToChat?: showAddToChat = true
        case nil: break
        }
    }

    private func attach(_ attachment: ComposerAttachment) {
        fixture.composer.attach(attachment)
        showAddToChat = false
    }
}

/// The unified Inbox fixture: host-reported item states; tapping an item opens its task reply chat.
struct PlaygroundInboxScreen: View {
    let inbox: InboxPlaygroundFixture
    let open: (String) -> Void

    var body: some View {
        InboxScreen(items: inbox.items, onAction: { action in
            if case .open(let id) = action { open(id) }
        }) {
            Text(InboxPlaygroundFixture.emptyMessage)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .frame(maxWidth: .infinity)
                .padding(.top, DesignTokens.Spacing.xl)
                .accessibilityIdentifier("inbox.empty")
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}
