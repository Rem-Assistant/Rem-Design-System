import SwiftUI

/// **RemComposerBar** — the chat / comment composer: a growing text field in a rounded, borderless
/// pill with a bottom control row `[+] · model · field · [Speak] · send`, and an optional attachments
/// strip on top. No border — the shape is the material pill alone.
///
/// Figma canonical: **RemComposerBar** (`53:2`) + the 3-state doc (`527:2` — Idle / Composing / Sending).
/// Shipping source: `Shared/Views/Chat/RemComposerBar.swift` (`SharedRemChatView.composerBar`). The
/// shipping shell injects the leading/trailing/send affordances via `@ViewBuilder` slots; this design-
/// system component renders the **canonical filled arrangement** so the composer reads as one system,
/// with a [SendState] driving the send button. Compose sibling: `chat/RemComposerBar.kt`.
///
/// Chat slice (Figma **Composer** `2071:11555`): the model control is the secondary-pill **Auto**
/// trigger. Pass a `ChatModelMenu` to make it open the runtime-supplied model menu; without one it is
/// a display-only pill. While sending, the trigger is disabled (45%) and Speak is hidden. Attachments
/// are removable chips supplied by the host (`ComposerAttachment`) — a Cloud browser chip is a
/// capability added to the next message, not a browser launch.
///
/// Host hooks (`init(state:modelMenu:accessibilityPrefix:onAction:)`): the host supplies a
/// `ChatComposerState` — draft, availability (externally disabled), phase (sending / streaming), voice
/// availability, focus — and receives typed `ChatComposerAction`s. Send is available for text *or* a
/// content attachment; Stop emits `.cancel`, never `.send`. The visual split between Sending (progress)
/// and Streaming (Stop) in the Figma composer is not drawn yet: both show the red Stop, pending review.
public struct RemComposerBar: View {
    /// Drives the trailing send affordance: idle (grey ↑), active (brandBlue ↑), sending (red ■ abort).
    /// Legacy display state; the `state:onAction:` initializer derives the control from
    /// `ChatComposerState` instead.
    public enum SendState: Equatable {
        case idle
        case active
        case sending
    }

    private let text: String
    private let placeholder: String
    private let model: String
    private let state: SendState
    private let attachments: [ComposerAttachment]
    private let showModel: Bool
    private let showSpeak: Bool
    private var textBinding: Binding<String>?
    private var onSend: (() -> Void)?
    private var onCancel: (() -> Void)?
    private var onSpeak: (() -> Void)?
    private var onAdd: (() -> Void)?
    private var modelMenu: ChatModelMenu?
    private var onRemoveAttachment: ((ComposerAttachment) -> Void)?
    private var accessibilityPrefix = "composer"
    /// Set by the `state:onAction:` initializer: the host-owned state and the typed action sink.
    private var composerState: ChatComposerState?
    private var onAction: ((ChatComposerAction) -> Void)?
    @FocusState private var fieldFocused: Bool

    public init(
        text: String = "",
        placeholder: String = "Ask anything",
        model: String = "Auto",
        state: SendState = .idle,
        showAttachments: Bool = false,
        showModel: Bool = true,
        showSpeak: Bool = true,
        attachments: [ComposerAttachment] = []
    ) {
        self.text = text
        self.placeholder = placeholder
        self.model = model
        self.state = state
        // `showAttachments` is the legacy display flag: it shows the canonical Cloud browser chip.
        self.attachments = attachments.isEmpty && showAttachments ? [.cloudBrowser] : attachments
        self.showModel = showModel
        self.showSpeak = showSpeak
    }

    /// Interactive composition of the same canonical pill. Send follows the single rule in
    /// `ChatComposerState.canSend` (text or a content attachment); the parent owns draft state and all
    /// actions. No text is stored or transmitted by this component.
    /// `modelMenu` replaces the display-only model pill with the interactive menu; `attachments` and
    /// `onRemoveAttachment` drive the removable chips. While `.sending`, the red Stop calls `onCancel`
    /// — never `onSend` — and is disabled when no `onCancel` is supplied. `onSpeak` makes Speak a button.
    public init(
        text: Binding<String>, placeholder: String = "Ask anything", model: String = "Auto",
        state: SendState = .idle, showAttachments: Bool = false,
        showModel: Bool = true, showSpeak: Bool = true,
        attachments: [ComposerAttachment] = [],
        modelMenu: ChatModelMenu? = nil,
        accessibilityPrefix: String = "composer", onAdd: (() -> Void)? = nil,
        onRemoveAttachment: ((ComposerAttachment) -> Void)? = nil,
        onSpeak: (() -> Void)? = nil,
        onCancel: (() -> Void)? = nil,
        onSend: @escaping () -> Void
    ) {
        self.init(text: text.wrappedValue, placeholder: placeholder, model: model,
                  state: state, showAttachments: showAttachments, showModel: showModel, showSpeak: showSpeak,
                  attachments: attachments)
        self.textBinding = text
        self.onSend = onSend
        self.onCancel = onCancel
        self.onSpeak = onSpeak
        self.onAdd = onAdd
        self.modelMenu = modelMenu
        self.onRemoveAttachment = onRemoveAttachment
        self.accessibilityPrefix = accessibilityPrefix
    }

    /// The host-driven composer: renders `state` exactly and reports every interaction as a typed
    /// `ChatComposerAction`. Availability, phase, voice and focus are host decisions; the rules that turn
    /// them into the control row live in `ChatComposerState` (send / cancel / speak / model enablement).
    public init(
        state: ChatComposerState,
        modelMenu: ChatModelMenu? = nil,
        accessibilityPrefix: String = "composer",
        onAction: @escaping (ChatComposerAction) -> Void
    ) {
        self.init(text: state.draft, placeholder: state.placeholder, model: state.modelLabel,
                  state: state.phase.isInFlight ? .sending : (state.canSend ? .active : .idle),
                  showModel: state.showsModel, showSpeak: state.voiceAvailable, attachments: state.attachments)
        self.composerState = state
        self.onAction = onAction
        self.modelMenu = modelMenu
        self.accessibilityPrefix = accessibilityPrefix
    }

    // MARK: Resolved presentation (one rule set for every initializer)

    private var currentText: String { textBinding?.wrappedValue ?? text }

    /// The rules applied to whichever initializer was used.
    private var resolved: ChatComposerState {
        if let composerState { return composerState }
        return ChatComposerState(
            draft: currentText, placeholder: placeholder, modelLabel: model, attachments: attachments,
            phase: state == .sending ? .sending : .idle, showsModel: showModel, voiceAvailable: showSpeak
        )
    }

    private var sendDisplay: ComposerSendDisplay {
        // Display-only legacy renders keep their explicit state (fixtures pass `.active` with text).
        if composerState == nil, textBinding == nil {
            switch state {
            case .idle: return .unavailable
            case .active: return .send
            case .sending: return .stop
            }
        }
        return resolved.sendDisplay
    }

    private var isInteractive: Bool { onAction != nil || onSend != nil }

    private var primaryEnabled: Bool {
        if let composerState { return composerState.primaryAction != nil }
        switch sendDisplay {
        case .unavailable: return false
        case .send: return onSend != nil
        case .stop: return onCancel != nil
        }
    }

    private func performPrimary() {
        if let composerState, let onAction {
            if let action = composerState.primaryAction { onAction(action) }
            return
        }
        if sendDisplay == .stop { onCancel?() } else { onSend?() }
    }

    private var inputEnabled: Bool { resolved.availability.isEnabled }

    private var speakAction: (() -> Void)? {
        if let onAction { return { onAction(.speak) } }
        return onSpeak
    }

    private var addAction: (() -> Void)? {
        if let onAction { return { onAction(.add) } }
        return onAdd
    }

    private var removeAction: ((ComposerAttachment) -> Void)? {
        if let onAction { return { onAction(.removeAttachment(id: $0.id)) } }
        return onRemoveAttachment
    }

    private var fieldBinding: Binding<String>? {
        if let composerState, let onAction {
            return Binding(get: { composerState.draft }, set: { onAction(.draftChanged($0)) })
        }
        return textBinding
    }

    private var disabledReason: String? {
        if case .disabled(let reason) = resolved.availability { return reason }
        return nil
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if !resolved.attachments.isEmpty {
                attachmentsStrip
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let fieldBinding {
                TextField(resolved.placeholder, text: fieldBinding, axis: .vertical)
                    .font(DesignTokens.Typography.chatMessage)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .focused($fieldFocused)
                #if os(iOS)
                    // Tap-to-type through SwiftUI's own gesture path. Inside a host ScrollView the
                    // vertical field's text view did not take focus from a tap or a short press
                    // (playground run 37944337913), while SwiftUI controls on the same page did.
                    .simultaneousGesture(TapGesture().onEnded { if inputEnabled { fieldFocused = true } })
                #endif
                    .disabled(!inputEnabled)
                    .accessibilityHint(disabledReason ?? "")
                    .accessibilityIdentifier("\(accessibilityPrefix).composerField")
            } else {
                Text(text.isEmpty ? placeholder : text)
                    .font(DesignTokens.Typography.chatMessage)
                    .foregroundStyle(text.isEmpty ? DesignTokens.Color.labelTertiary : DesignTokens.Color.labelPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }

            // Control row. Add and Send keep 44pt hit frames; those frames already carry the visual gap
            // to their neighbours (17pt glyph / 32pt circle inside 44), so no extra row spacing is
            // added beside them. Speak keeps its one-line intrinsic width; the Spacer and then the
            // model label give way, so Speak never wraps at a 320pt screen (288pt composer, 264pt row).
            HStack(spacing: 0) {
                if let addAction {
                    Button(action: addAction) { addGlyph.frame(minWidth: 44, minHeight: 44) }
                        .buttonStyle(.plain).accessibilityLabel("Add")
                        .disabled(!inputEnabled)
                        .accessibilityIdentifier("\(accessibilityPrefix).composerAdd")
                } else { addGlyph.padding(.trailing, DesignTokens.Spacing.sm) } // display-only: no 44pt frame
                // The model label is host data and may truncate; Speak (layoutPriority 1) never does.
                if resolved.showsModel { modelSelector.disabled(!modelEnabled) }
                Spacer(minLength: DesignTokens.Spacing.sm)
                if showsSpeak {
                    if let speakAction {
                        Button(action: speakAction) { speakPill.frame(minHeight: 44) }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Speak")
                            .accessibilityIdentifier("\(accessibilityPrefix).composerSpeak")
                            .layoutPriority(1) // the HStack reads priority from its direct child
                    } else { speakPill }
                }
                if isInteractive {
                    Button(action: performPrimary) { sendButton.frame(minWidth: 44, minHeight: 44) }
                        .buttonStyle(.plain)
                        .disabled(!primaryEnabled)
                        .accessibilityLabel(sendDisplay == .stop ? "Stop" : "Send")
                        .accessibilityIdentifier("\(accessibilityPrefix).composerSend")
                } else { sendButton.padding(.leading, DesignTokens.Spacing.sm) } // display-only: no 44pt frame
            }
        }
        .padding(DesignTokens.Spacing.md)
        .background(DesignTokens.Color.backgroundSecondary, in: RoundedRectangle(cornerRadius: 30, style: .continuous))
        .onAppear { if let composerState { fieldFocused = composerState.isFocused } }
        .onChange(of: composerState?.isFocused) { _, requested in
            if let requested, requested != fieldFocused { fieldFocused = requested }
        }
        .onChange(of: fieldFocused) { _, focused in
            guard let composerState, let onAction, focused != composerState.isFocused else { return }
            onAction(.focusChanged(focused))
        }
    }

    /// Display-only legacy renders keep showing Speak unless sending; host-driven renders follow
    /// `ChatComposerState.showsSpeak`.
    private var showsSpeak: Bool {
        if let composerState { return composerState.showsSpeak }
        return showSpeak && sendDisplay != .stop
    }

    private var modelEnabled: Bool {
        if let composerState { return composerState.modelEnabled }
        return sendDisplay != .stop
    }

    private var addGlyph: some View {
        Image(systemName: "plus")
            .font(.system(size: 17, weight: .regular))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
    }

    @ViewBuilder
    private var modelSelector: some View {
        if let modelMenu {
            modelMenu
        } else {
            ChatModelTriggerPill(label: resolved.modelLabel)
        }
    }

    private var speakPill: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Image(systemName: "waveform")
                .font(.system(size: 13, weight: .semibold))
            Text("Speak")
                .font(DesignTokens.Typography.subheadline.weight(.semibold))
                .lineLimit(1)
        }
        .foregroundStyle(DesignTokens.Color.labelOnColor)
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, 7)
        .background(DesignTokens.Color.brandBlue, in: Capsule())
        .fixedSize()
        .layoutPriority(1)
    }

    private var sendButton: some View {
        ZStack {
            Circle().fill(sendFill)
            Image(systemName: sendDisplay == .stop ? "stop.fill" : "arrow.up")
                .font(.system(size: sendDisplay == .stop ? 12 : 15, weight: .bold))
                .foregroundStyle(sendForeground)
        }
        .frame(width: 32, height: 32)
    }

    private var sendFill: Color {
        switch sendDisplay {
        case .unavailable: return DesignTokens.Color.fillTertiary
        case .send: return DesignTokens.Color.brandBlue
        case .stop: return DesignTokens.Color.systemRed
        }
    }

    private var sendForeground: Color {
        sendDisplay == .unavailable ? DesignTokens.Color.labelSecondary : DesignTokens.Color.labelOnColor
    }

    private var attachmentsStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(resolved.attachments) { attachment in
                    attachmentChip(attachment)
                }
            }
        }
    }

    private func attachmentChip(_ attachment: ComposerAttachment) -> some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Text(attachment.title)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .lineLimit(1)
            if let removeAction {
                Button { removeAction(attachment) } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                        .frame(width: 20, height: 20)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remove \(attachment.title)")
                .accessibilityIdentifier("\(accessibilityPrefix).removeAttachment.\(attachment.id)")
            } else {
                Image(systemName: "xmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(DesignTokens.Color.brandBlue)
                    .accessibilityHidden(true)
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .padding(.vertical, DesignTokens.Spacing.xs)
        .background(
            DesignTokens.Color.backgroundPrimary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous)
        )
        .accessibilityIdentifier("\(accessibilityPrefix).attachment.\(attachment.id)")
    }
}

/// One item attached to the next message, shown as a removable chip in `RemComposerBar`. The host
/// owns the list (and any picked content); the chip renders only its title.
public struct ComposerAttachment: Hashable, Identifiable, Sendable {
    public enum Kind: Hashable, Sendable {
        /// A capability for the next turn (e.g. Cloud browser) — not content, not an immediate launch.
        case capability
        case image
        case file
    }

    public let id: String
    public let title: String
    public let kind: Kind

    public init(id: String, title: String, kind: Kind) {
        self.id = id
        self.title = title
        self.kind = kind
    }

    /// The Cloud browser capability chip.
    public static let cloudBrowser = ComposerAttachment(id: "cloud-browser", title: "Cloud browser", kind: .capability)
}

#Preview {
    VStack(spacing: 20) {
        RemComposerBar()
        RemComposerBar(text: "Remind me to send the investor update tomorrow", state: .active, showAttachments: true)
        RemComposerBar(text: "Plan the rest of my day", state: .sending)
    }
    .padding(24)
    .background(DesignTokens.Color.backgroundPrimary)
}
