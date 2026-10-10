import SwiftUI

/// **ChatScreen** — the conversation surface: a scrolling transcript of `MessageBubble`s, an optional
/// `VoiceBar` above the input when voice is live, and a `RemComposerBar` pinned at the bottom. Composed
/// from Wave-2 components; the host supplies the transcript through `transcript`. Chrome comes from the
/// platform, as in the shipping `SharedRemChatView`.
///
/// Figma canonical: Chat screen (`71:533`) + composer states (`527:2`). Source: `SharedRemChatView.swift`.
/// Compose sibling: `screens/ChatScreen.kt`.
///
/// **Full-screen composition** (`init(header:composer:…onAction:transcript:)`) — Figma shell
/// `2054:21958`, acceptance board `2681:21977`: the shared header (`2054:19725`, which owns back and
/// overflow — the platform navigation bar is hidden so no second title row competes), then the Screen
/// state slot — scrolling transcript, or the empty state (`2054:22089`, no second face) — and bottom
/// chrome docking the **one canonical composer** (`2071:11555`) above the keyboard or home indicator,
/// with the optional task-reply accessory (`2682:22298`) above it. Safe areas come from the host; the
/// 320pt board is a width stress fixture only. The host supplies data and handles every
/// `ChatScreenAction`; it owns sending, receipts and routing (`docs/contracts/chat-adapter.md`).
public struct ChatScreen<Transcript: View>: View {
    private let composerText: String
    private let composerPlaceholder: String
    private let composerState: RemComposerBar.SendState
    private let voiceBar: VoiceBarState?
    private let transcript: () -> Transcript
    private var composition: Composition?

    /// The host-driven composition's inputs.
    private struct Composition {
        let header: ChatHeaderDisplay
        let composer: ChatComposerState
        let replyContext: ChatReplyContext?
        let emptyState: ChatEmptyState?
        let modelMenu: ChatModelMenu?
        let onAction: (ChatScreenAction) -> Void
    }

    public init(
        composerText: String = "",
        composerPlaceholder: String = "Ask anything",
        composerState: RemComposerBar.SendState = .idle,
        voiceBar: VoiceBarState? = nil,
        @ViewBuilder transcript: @escaping () -> Transcript
    ) {
        self.composerText = composerText
        self.composerPlaceholder = composerPlaceholder
        self.composerState = composerState
        self.voiceBar = voiceBar
        self.transcript = transcript
    }

    /// The full-screen Chat composition. `transcript` is the rich-content slot: compose
    /// `ChatTranscriptList` and any host cards (tool results, proposals, browser) inside it. When
    /// `emptyState` is non-nil it is shown instead of the transcript. Ordinary and task chat use the
    /// same composition; a task reply only adds `replyContext`.
    public init(
        header: ChatHeaderDisplay,
        composer: ChatComposerState,
        replyContext: ChatReplyContext? = nil,
        emptyState: ChatEmptyState? = nil,
        modelMenu: ChatModelMenu? = nil,
        voiceBar: VoiceBarState? = nil,
        onAction: @escaping (ChatScreenAction) -> Void,
        @ViewBuilder transcript: @escaping () -> Transcript
    ) {
        self.init(composerText: composer.draft, composerPlaceholder: composer.placeholder,
                  voiceBar: voiceBar, transcript: transcript)
        self.composition = Composition(
            header: header, composer: composer, replyContext: replyContext, emptyState: emptyState,
            modelMenu: modelMenu, onAction: onAction
        )
    }

    public var body: some View {
        if let composition {
            composed(composition)
        } else {
            legacy
        }
    }

    // MARK: Legacy display layout (component catalog, render evidence)

    private var legacy: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    transcript()
                }
                .padding(DesignTokens.Spacing.lg)
            }
            VStack(spacing: DesignTokens.Spacing.sm) {
                if let voiceBar {
                    VoiceBar(voiceBar)
                }
                RemComposerBar(text: composerText, placeholder: composerPlaceholder, state: composerState)
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.bottom, DesignTokens.Spacing.md)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    // MARK: Full-screen composition

    private func composed(_ c: Composition) -> some View {
        VStack(spacing: 0) {
            ChatHeader(
                name: c.header.name,
                activity: c.header.activity,
                status: c.header.status,
                faceMode: c.header.isWorking ? .thinking : .idle,
                accessibilityPrefix: "chat.header",
                onTap: c.header.showsActivityDetails ? { c.onAction(.activityDetails) } : nil,
                onBack: c.header.showsBack ? { c.onAction(.back) } : nil,
                onOverflow: c.header.showsOverflow ? { c.onAction(.overflow) } : nil,
                onCall: c.header.showsCall ? { c.onAction(.call) } : nil
            )
            .padding(.bottom, DesignTokens.Spacing.sm)

            ScrollView {
                Group {
                    if let empty = c.emptyState {
                        ChatEmptyStateView(empty) { c.onAction(.starter(id: $0)) }
                    } else {
                        VStack(spacing: DesignTokens.Spacing.lg) {
                            transcript()
                        }
                    }
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
                .frame(maxWidth: .infinity)
            }
            .defaultScrollAnchor(c.emptyState == nil ? .bottom : .top)
            .scrollDismissesKeyboard(.interactively)
            .accessibilityIdentifier("chat.content")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .safeAreaInset(edge: .bottom, spacing: 0) { dock(c) }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    #if os(iOS)
        // The shared header owns back / overflow: no second navigation-title row.
        .toolbar(.hidden, for: .navigationBar)
    #endif
    }

    /// Bottom chrome: optional voice bar, optional reply accessory, then the one canonical composer.
    /// Docked by `safeAreaInset`, so it rides above the keyboard or the home indicator.
    private func dock(_ c: Composition) -> some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if let voiceBar {
                VoiceBar(voiceBar)
            }
            if let context = c.replyContext {
                ChatReplyContextAccessory(context, accessibilityPrefix: "chat.replyContext") {
                    c.onAction(.dismissReplyContext(targetID: context.targetID))
                }
            }
            RemComposerBar(state: c.composer, modelMenu: c.modelMenu, accessibilityPrefix: "chat") {
                c.onAction(.composer($0))
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
        .padding(.bottom, DesignTokens.Spacing.sm)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

/// The empty conversation (`2054:22089`): title, message and host-supplied starters as full-width
/// capsules. No face — the header avatar is the only identity on screen.
public struct ChatEmptyStateView: View {
    private let state: ChatEmptyState
    private let onStarter: (String) -> Void

    public init(_ state: ChatEmptyState, onStarter: @escaping (String) -> Void) {
        self.state = state
        self.onStarter = onStarter
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Text(state.title)
                .font(DesignTokens.Typography.title3.weight(.bold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .multilineTextAlignment(.center)
            Text(state.message)
                .font(DesignTokens.Typography.subheadline)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            VStack(spacing: DesignTokens.Spacing.md) {
                ForEach(state.starters) { starter in
                    Button { onStarter(starter.id) } label: {
                        Text(starter.title)
                            .font(DesignTokens.Typography.body.weight(.semibold))
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, DesignTokens.Spacing.lg)
                            .padding(.vertical, DesignTokens.Spacing.md)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(DesignTokens.Color.backgroundPrimary, in: Capsule())
                            .shadow(color: .black.opacity(0.10), radius: 12, x: 0, y: 4)
                            .contentShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("chat.starter.\(starter.id)")
                }
            }
            .padding(.top, DesignTokens.Spacing.sm)
        }
        .padding(.top, DesignTokens.Spacing.xl)
        .frame(maxWidth: .infinity)
    }
}

/// The canonical transcript for host-supplied entries: centred timestamps, `MessageBubble`s, and the
/// receipt placement rule in `ChatTranscriptRules` (only the latest outgoing message carries its
/// Delivered / Read receipt; failures stay visible). Delivery values are rendered as supplied.
public struct ChatTranscriptList: View {
    private let entries: [ChatTranscriptEntry]
    private let onAction: (ChatTranscriptAction) -> Void
    private let hostContent: (String) -> AnyView
    /// Swipe-to-reveal timestamps (WS1d): one shared offset moves every row together.
    @State private var reveal: CGFloat = ChatTimestampReveal.settled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Width of the right-side timestamp column (fits "10:24 AM" at footnote size). Provisional: to be
    /// validated against long localized times and large text sizes; not a specified value.
    static let timeColumnWidth: CGFloat = 64

    /// `hostContent` renders each `.hostContent(id:)` entry in place; hosts without such entries omit it.
    public init(
        _ entries: [ChatTranscriptEntry],
        onAction: @escaping (ChatTranscriptAction) -> Void,
        hostContent: @escaping (String) -> AnyView = { _ in AnyView(EmptyView()) }
    ) {
        self.entries = entries
        self.onAction = onAction
        self.hostContent = hostContent
    }

    public var body: some View {
        let latest = ChatTranscriptRules.latestOutgoingID(in: entries)
        VStack(spacing: DesignTokens.Spacing.lg) {
            ForEach(entries) { entry in
                switch entry {
                case .timestamp(let id, let text):
                    Text(text)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("chat.timestamp.\(id)")
                case .hostContent(let id):
                    hostContent(id)
                        .frame(maxWidth: .infinity, alignment: .leading)
                case .message(let message):
                    MessageBubble(displayed(message, latest: latest), onAction: onAction)
                        .overlay(alignment: Alignment(horizontal: .trailing, vertical: .messageBubbleCenter)) {
                            timeColumn(message)
                        }
                }
            }
        }
        // Rows move together; the header, composer and keyboard live outside the transcript and stay put.
        .offset(x: -reveal)
        .simultaneousGesture(revealGesture)
    }

    /// Right-side time for one message, just past the row's trailing edge so it is off-screen at rest and
    /// slides in, outside the bubble, as the rows move left. Spoken through the bubble instead.
    @ViewBuilder
    private func timeColumn(_ message: ChatMessageDisplay) -> some View {
        if let time = message.time {
            Text(time)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .lineLimit(1)
                .frame(width: Self.timeColumnWidth, alignment: .trailing)
                .offset(x: Self.timeColumnWidth)
                .opacity(min(1, reveal / Self.timeColumnWidth))
                .accessibilityHidden(true)
        }
    }

    /// Leftward horizontal drags only; vertical scrolling keeps working. Rows snap back on release.
    private var revealGesture: some Gesture {
        DragGesture(minimumDistance: ChatTimestampReveal.minTravel)
            .onChanged { value in
                let dx = value.translation.width, dy = value.translation.height
                guard reveal > 0 || ChatTimestampReveal.isRevealDrag(dx: dx, dy: dy) else { return }
                reveal = ChatTimestampReveal.reveal(dx: dx, columnWidth: Self.timeColumnWidth, reduceMotion: reduceMotion)
            }
            .onEnded { _ in
                withAnimation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.85)) {
                    reveal = ChatTimestampReveal.settled
                }
            }
    }

    private func displayed(_ message: ChatMessageDisplay, latest: String?) -> ChatMessageDisplay {
        var shown = message
        shown.delivery = ChatTranscriptRules.displayedDelivery(for: message, latestOutgoingID: latest)
        return shown
    }
}

#Preview {
    ChatScreen(composerText: "", composerState: .idle) {
        MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
        MessageBubble(
            "Done — I archived 38 newsletters and snoozed 5 low-priority threads. Want me to draft replies to the two that still need you?",
            role: .assistant
        )
        MessageBubble("Yes, go ahead.", role: .user)
    }
}

#Preview("Composition · task reply") {
    ChatScreen(
        header: ChatHeaderDisplay(activity: "Connected"),
        composer: ChatComposerState(placeholder: "Write your reply…"),
        replyContext: ChatReplyContext(targetID: "target-1", title: "Replying to Rem", summary: "Plan the next step"),
        onAction: { _ in }
    ) {
        ChatTranscriptList([
            .message(ChatMessageDisplay(id: "u1", role: .user, text: "Could you make the next step clearer?",
                                        delivery: .delivered(at: "10:24"))),
            .message(ChatMessageDisplay(id: "a1", role: .assistant, text: "I'll outline a clear next step for this task.")),
        ]) { _ in }
    }
}

#Preview("Composition · empty") {
    ChatScreen(
        header: ChatHeaderDisplay(activity: "Connected"),
        composer: ChatComposerState(),
        emptyState: ChatEmptyState(
            message: "Start a conversation with Rem. Plan your day, explore an idea or get a task moving.",
            starters: [ChatStarter(id: "plan-day", title: "Help me plan my day")]
        ),
        onAction: { _ in }
    ) { EmptyView() }
}
