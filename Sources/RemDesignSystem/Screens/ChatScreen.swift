import SwiftUI
#if os(iOS)
import UIKit
#endif

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
/// The transcript scrolls **under** the header and the composer dock (`ChatEdgeBackdrop`): each sits on a
/// progressive blur, opaque at the screen edge and fading to clear just past the chrome, and the transcript's
/// top and bottom content insets equal the header and dock heights, so the first and latest messages are
/// never hidden. Hosts wire nothing.
/// The composition also owns **jump to latest** (`ChatJumpToLatest`): a round down-arrow centred above the
/// dock while the person is scrolled more than `ChatJumpToLatest.threshold` above the newest message.
/// Hosts wire nothing for it. The keyboard is dismissed by dragging the transcript down
/// (`.scrollDismissesKeyboard(.interactively)`) or by a downward swipe on the composer (`ChatKeyboardDismiss`).
public struct ChatScreen<Transcript: View>: View {
    private let composerText: String
    private let composerPlaceholder: String
    private let composerState: RemComposerBar.SendState
    private let voiceBar: VoiceBarState?
    private let transcript: () -> Transcript
    private var composition: Composition?
    /// How far the transcript's bottom edge sits below the visible area (jump to latest).
    @State private var distanceFromBottom: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.chatJumpToLatestEvidence) private var jumpEvidence
    /// Edge backdrops: the header's bottom edge, the dock's top edge and the transcript viewport's edges, all
    /// in the composition's coordinate space. Their differences are the transcript's top and bottom insets.
    @State private var headerBottom: CGFloat = 0
    @State private var viewportTop: CGFloat = 0
    @State private var dockTop: CGFloat = 0
    @State private var viewportBottom: CGFloat = 0
    @Environment(\.chatEdgeBackdropEvidence) private var backdropEvidence

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
        let headerInset = ChatEdgeBackdrop.chromeHeight(headerBottom: headerBottom, viewportTop: viewportTop)
        let dockInset = ChatEdgeBackdrop.dockHeight(viewportBottom: viewportBottom, dockTop: dockTop)
        // Edge backdrops: the transcript fills the screen (under the status bar, the dock and the home indicator;
        // above the keyboard) and the header and dock float over it on progressive blurs, so messages scroll
        // behind both instead of clipping at solid bands.
        return ZStack(alignment: .top) {
            // The reader measures the visible transcript area (inside the docked composer's inset), so a
            // short conversation fills it itself, bottom-aligned, instead of relying on the scroll view's
            // default anchor to shift undersized content down. That shift moves the rows on screen but not
            // their accessibility container, leaving the latest rows outside it (unreachable by
            // VoiceOver's scroll-to-visible and UI automation).
            ScrollViewReader { proxy in
                GeometryReader { viewport in
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
                        .frame(
                            maxWidth: .infinity,
                            minHeight: ChatEdgeBackdrop.transcriptMinHeight(
                                viewportHeight: viewport.size.height, topInset: headerInset, bottomInset: dockInset
                            ),
                            alignment: c.emptyState == nil ? .bottom : .top
                        )
                        // Edge backdrops: the content starts below the header and ends above the dock, so the
                        // first and latest messages are fully visible at rest; scrolled, they pass under the blur.
                        .padding(.top, headerInset)
                        .padding(.bottom, dockInset)
                        // Jump to latest: the content's bottom edge (dock inset included) in the scroll view's
                        // own space, so its distance below the visible area is `maxY - viewport height`.
                        .background(GeometryReader { content in
                            Color.clear.preference(
                                key: ChatContentBottomKey.self,
                                value: content.frame(in: .named(ChatScrollIDs.space)).maxY
                            )
                        })
                        .overlay(alignment: .bottom) {
                            Color.clear.frame(height: 1).id(ChatScrollIDs.latest).accessibilityHidden(true)
                        }
                        .overlay(alignment: .top) {
                            if backdropEvidence {
                                Color.clear.frame(height: 1).id(ChatScrollIDs.backdropEvidence)
                                    .padding(.top, ChatScrollIDs.backdropEvidenceOffset)
                                    .accessibilityHidden(true)
                            }
                        }
                    }
                    .coordinateSpace(.named(ChatScrollIDs.space))
                    .scrollDisabled(ProcessInfo.processInfo.arguments.contains("--probe-no-scroll"))  // PROBE only
                    .defaultScrollAnchor(c.emptyState == nil && !jumpEvidence ? .bottom : .top)
                    .scrollDismissesKeyboard(.interactively)
                    .accessibilityIdentifier("chat.content")
                    .onPreferenceChange(ChatContentBottomKey.self) { reported in
                        guard let maxY = reported else { return }
                        distanceFromBottom = ChatEdgeBackdrop.distanceFromLatest(
                            contentBottom: maxY, viewportHeight: viewport.size.height
                        )
                    }
                    .preference(key: ChatViewportTopKey.self, value: viewport.frame(in: .named(ChatScrollIDs.screen)).minY)
                    .preference(key: ChatViewportBottomKey.self, value: viewport.frame(in: .named(ChatScrollIDs.screen)).maxY)
                }
                // Over the transcript, lifted by the dock inset so it never covers the docked composer or the
                // keyboard (the transcript itself extends under the dock).
                // The animation is scoped to the button so it never animates transcript changes.
                .overlay(alignment: .bottom) {
                    let showsJump = c.emptyState == nil
                        && ChatJumpToLatest.isVisible(distanceFromBottom: distanceFromBottom)
                    let jumpMotion = !reduceMotion && !jumpEvidence
                    ZStack {
                        if showsJump {
                            ChatJumpToLatestButton {
                                withAnimation(jumpMotion ? .easeInOut(duration: 0.3) : nil) {
                                    proxy.scrollTo(ChatScrollIDs.latest, anchor: .bottom)
                                }
                            }
                            .transition(
                                jumpMotion
                                    ? AnyTransition.opacity.combined(with: .scale(scale: ChatJumpToLatest.appearScale))
                                    : AnyTransition.identity
                            )
                        }
                    }
                    .padding(.bottom, ChatEdgeBackdrop.jumpButtonBottomPadding(dockHeight: dockInset))
                    .animation(jumpMotion ? .easeOut(duration: ChatJumpToLatest.appearDuration) : nil, value: showsJump)
                }
                .onAppear {
                    guard backdropEvidence else { return }
                    DispatchQueue.main.async { proxy.scrollTo(ChatScrollIDs.backdropEvidence, anchor: .top) }
                }
            }
            .ignoresSafeArea(.container, edges: [.top, .bottom])

            ChatEdgeBackdropView(edge: .top, height: headerInset)
                .ignoresSafeArea(.container, edges: .top)
            ChatEdgeBackdropView(edge: .bottom, height: dockInset)
                .ignoresSafeArea(.container, edges: .bottom)

            ChatHeader(
                name: c.header.name,
                activity: c.header.activity,
                status: c.header.status,
                faceMode: c.header.isWorking ? .thinking : .idle,
                accessibilityPrefix: "chat.header",
                onTap: c.header.showsActivityDetails ? { c.onAction(.activityDetails) } : nil,
                onBack: c.header.showsBack ? { c.onAction(.back) } : nil,
                onOverflow: c.header.showsOverflow ? { c.onAction(.overflow) } : nil,
                onCall: c.header.showsCall ? { c.onAction(.call) } : nil,
                onMenu: c.header.showsSidebar ? { c.onAction(.openSidebar) } : nil
            )
            .padding(.bottom, DesignTokens.Spacing.sm)
            .background(GeometryReader { header in
                Color.clear.preference(
                    key: ChatHeaderBottomKey.self,
                    value: header.frame(in: .named(ChatScrollIDs.screen)).maxY
                )
            })
            // Drawn over the transcript, but still read first.
            .accessibilitySortPriority(1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            dock(c).background(GeometryReader { dock in
                Color.clear.preference(key: ChatDockTopKey.self, value: dock.frame(in: .named(ChatScrollIDs.screen)).minY)
            })
        }
        // Edge backdrops: one space for the header, the dock (outside the inset content) and the viewport.
        .coordinateSpace(.named(ChatScrollIDs.screen))
        .onPreferenceChange(ChatHeaderBottomKey.self) { headerBottom = $0 ?? 0 }
        .onPreferenceChange(ChatViewportTopKey.self) { viewportTop = $0 ?? 0 }
        .onPreferenceChange(ChatDockTopKey.self) { dockTop = $0 ?? 0 }
        .onPreferenceChange(ChatViewportBottomKey.self) { viewportBottom = $0 ?? 0 }
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
    }
}

/// Scroll identifiers for jump to latest (a generic view cannot hold static stored properties).
private enum ChatScrollIDs {
    static let space = "chat.scroll"
    static let latest = "chat.latest"
    /// The whole composition (edge backdrop measurements).
    static let screen = "chat.screen"
    /// Edge-backdrop evidence: a marker this far below the content top is scrolled to the viewport top.
    static let backdropEvidence = "chat.backdropEvidence"
    static let backdropEvidenceOffset: CGFloat = 96
}

// The four edge measurements below are optional with a nil default, and `reduce` keeps the last *reported*
// value. Siblings that never set a key still take part in the reduction with the default value, so a
// plain `value = nextValue()` let the header and backdrop views (later in the same ZStack) reset the
// viewport edges to 0. The dock inset then measured as 0 and the latest message sat under the composer
// at rest (Shell-chat / ChatComposition-default renders, chat.md rule 12).

/// The header's bottom edge in the composition's coordinate space (header backdrop).
private struct ChatHeaderBottomKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil
    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) { value = nextValue() ?? value }
}

/// The transcript viewport's top edge in the composition's coordinate space: above zero by the status bar,
/// since the transcript extends under it (edge backdrops).
private struct ChatViewportTopKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil
    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) { value = nextValue() ?? value }
}

/// The transcript viewport's bottom edge in the composition's coordinate space: the screen bottom, or the top
/// of the keyboard (edge backdrops).
private struct ChatViewportBottomKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil
    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) { value = nextValue() ?? value }
}

/// The composer dock's top edge in the composition's coordinate space (edge backdrops).
private struct ChatDockTopKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil
    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) { value = nextValue() ?? value }
}

/// The transcript content's bottom edge in the scroll view's coordinate space (jump to latest). Optional and
/// keeping the last reported value, like the edge keys above: the content's later overlays (the latest-message
/// anchor) never set it, and a plain `value = nextValue()` let them reset it to 0, so the distance from the
/// latest message never passed the threshold and jump to latest never appeared.
private struct ChatContentBottomKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil
    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) { value = nextValue() ?? value }
}

/// Evidence seam (internal, never set by hosts): open the transcript at its oldest message instead of the
/// latest and show jump to latest without motion, so a still render captures the scrolled-up state. Use
/// only with a transcript taller than the screen plus `ChatJumpToLatest.threshold`. Twin of Compose
/// `LocalChatJumpToLatestEvidence`. Not part of the public API.
private struct ChatJumpToLatestEvidenceKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var chatJumpToLatestEvidence: Bool {
        get { self[ChatJumpToLatestEvidenceKey.self] }
        set { self[ChatJumpToLatestEvidenceKey.self] = newValue }
    }
}

/// Evidence seam (internal, never set by hosts): after the transcript opens at its oldest message (combine
/// with `chatJumpToLatestEvidence`), scroll it `ChatScrollIDs.backdropEvidenceOffset` down so a still render
/// shows messages passing under the header and the dock backdrops. Twin of Compose
/// `LocalChatEdgeBackdropEvidence`. Not part of the public API.
private struct ChatEdgeBackdropEvidenceKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var chatEdgeBackdropEvidence: Bool {
        get { self[ChatEdgeBackdropEvidenceKey.self] }
        set { self[ChatEdgeBackdropEvidenceKey.self] = newValue }
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
    #if !os(iOS)
    /// True only while the reveal drag is live. SwiftUI resets a `@GestureState` when the gesture ends
    /// *or is cancelled*, and `onEnded` runs only for the first, so the snap-back keys off this reset.
    @GestureState private var isRevealDragging = false
    #endif
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
        #if os(iOS)
        // The peek is a UIKit pan on the enclosing scroll view, not a SwiftUI `DragGesture` on the rows:
        // on iOS 17 a `DragGesture` attached to ScrollView content, even simultaneously, kept the scroll
        // view's pan from starting for touches that began on (or within touch slop of) a row, so a vertical
        // drag from a bubble scrolled nothing (run 38181697796). See `ChatTimestampRevealPan`.
        .background(
            ChatTimestampRevealPan(
                onChanged: { dx, dy in self.revealChanged(dx: dx, dy: dy) },
                onEnded: { self.settleReveal() }
            )
            .allowsHitTesting(false)
            .accessibilityHidden(true)
        )
        #else
        .simultaneousGesture(revealGesture)
        .onChange(of: isRevealDragging) { _, dragging in
            guard !dragging else { return }
            settleReveal()
        }
        #endif
    }

    /// Applies one reveal drag translation: leftward horizontal drags only, once claimed it follows dx.
    private func revealChanged(dx: CGFloat, dy: CGFloat) {
        guard reveal > 0 || ChatTimestampReveal.isRevealDrag(dx: dx, dy: dy) else { return }
        reveal = ChatTimestampReveal.reveal(dx: dx, columnWidth: Self.timeColumnWidth, reduceMotion: reduceMotion)
    }

    /// Rows always return when the drag ends, is cancelled or fails: the reveal is a peek.
    private func settleReveal() {
        guard reveal != ChatTimestampReveal.settled else { return }
        withAnimation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.85)) {
            reveal = ChatTimestampReveal.settled
        }
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

    #if !os(iOS)
    /// Leftward horizontal drags only; vertical scrolling keeps working. Rows snap back when the drag
    /// ends or is cancelled (see `isRevealDragging`). iOS uses `ChatTimestampRevealPan` instead.
    private var revealGesture: some Gesture {
        DragGesture(minimumDistance: ChatTimestampReveal.minTravel)
            .updating($isRevealDragging) { _, dragging, _ in dragging = true }
            .onChanged { value in
                revealChanged(dx: value.translation.width, dy: value.translation.height)
            }
    }
    #endif

    private func displayed(_ message: ChatMessageDisplay, latest: String?) -> ChatMessageDisplay {
        var shown = message
        shown.delivery = ChatTranscriptRules.displayedDelivery(for: message, latestOutgoingID: latest)
        return shown
    }
}

#if os(iOS)
/// The swipe-left time peek's recognizer (WS1d, iOS). A zero-size, non-interactive probe finds the
/// `UIScrollView` that hosts the transcript and adds one `UIPanGestureRecognizer` to it, so the rows carry
/// no SwiftUI gesture and every vertical drag — on a bubble, beside it, on a timestamp or in the gutter —
/// is the scroll view's own pan.
/// - **Begins** only when the drag is predominantly horizontal and leftward (`ChatTimestampReveal`
///   `horizontalRatio`, judged on velocity, else translation); any other drag fails it at once and the
///   scroll view never waits on it.
/// - **Recognizes simultaneously** with the scroll view's pan and SwiftUI's own recognizers, cancels and
///   delays no touches, so long presses, buttons and scrolling are untouched (a long press already fails
///   once the finger travels 10pt).
/// - Reports translation while live, and `onEnded` on end, cancel or failure, so rows always snap back.
/// - No enclosing scroll view (a bare `ChatTranscriptList` outside one): no peek; times stay accessible
///   through the bubble.
struct ChatTimestampRevealPan: UIViewRepresentable {
    let onChanged: (CGFloat, CGFloat) -> Void
    let onEnded: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> Probe {
        let probe = Probe()
        probe.isUserInteractionEnabled = false
        probe.isAccessibilityElement = false
        probe.backgroundColor = .clear
        probe.coordinator = context.coordinator
        return probe
    }

    func updateUIView(_ uiView: Probe, context: Context) {
        context.coordinator.parent = self
    }

    static func dismantleUIView(_ uiView: Probe, coordinator: Coordinator) {
        coordinator.attach(to: nil)
    }

    /// Re-resolves the host scroll view whenever it is inserted into, or removed from, a hierarchy.
    final class Probe: UIView {
        weak var coordinator: Coordinator?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            coordinator?.attach(to: window == nil ? nil : enclosingScrollView())
        }

        override func didMoveToSuperview() {
            super.didMoveToSuperview()
            coordinator?.attach(to: window == nil ? nil : enclosingScrollView())
        }

        private func enclosingScrollView() -> UIScrollView? {
            var view = superview
            while let current = view {
                if let scrollView = current as? UIScrollView { return scrollView }
                view = current.superview
            }
            return nil
        }
    }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var parent: ChatTimestampRevealPan
        private weak var host: UIScrollView?
        private var pan: UIPanGestureRecognizer?

        init(_ parent: ChatTimestampRevealPan) {
            self.parent = parent
        }

        func attach(to scrollView: UIScrollView?) {
            guard scrollView !== host || (scrollView != nil && pan == nil) else { return }
            if let pan {
                let live = pan.state == .began || pan.state == .changed
                pan.view?.removeGestureRecognizer(pan)
                self.pan = nil
                if live { parent.onEnded() }
            }
            host = scrollView
            guard let scrollView else { return }
            let recognizer = UIPanGestureRecognizer(target: self, action: #selector(handle(_:)))
            recognizer.delegate = self
            recognizer.cancelsTouchesInView = false
            recognizer.delaysTouchesBegan = false
            recognizer.delaysTouchesEnded = false
            recognizer.maximumNumberOfTouches = 1
            scrollView.addGestureRecognizer(recognizer)
            pan = recognizer
        }

        @objc private func handle(_ recognizer: UIPanGestureRecognizer) {
            switch recognizer.state {
            case .began, .changed:
                let t = recognizer.translation(in: Self.fixedSpace(of: recognizer))
                parent.onChanged(t.x, t.y)
            case .ended, .cancelled, .failed:
                parent.onEnded()
            default:
                break
            }
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let pan = gestureRecognizer as? UIPanGestureRecognizer else { return false }
            let space = Self.fixedSpace(of: pan)
            var motion = pan.velocity(in: space)
            if motion == .zero { motion = pan.translation(in: space) }
            return motion.x < 0 && abs(motion.x) > abs(motion.y) * ChatTimestampReveal.horizontalRatio
        }

        /// The scroll view's superview: unlike the scroll view's own bounds it does not move while the
        /// transcript scrolls, so the translation is the finger's travel on screen.
        private static func fixedSpace(of recognizer: UIGestureRecognizer) -> UIView? {
            recognizer.view?.superview ?? recognizer.view
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            true
        }
    }
}
#endif

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
