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
public struct RemComposerBar: View {
    /// Drives the trailing send affordance: idle (grey ↑), active (brandBlue ↑), sending (red ■ abort).
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
    private var onAdd: (() -> Void)?
    private var modelMenu: ChatModelMenu?
    private var onRemoveAttachment: ((ComposerAttachment) -> Void)?
    private var accessibilityPrefix = "composer"
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

    /// Interactive composition of the same canonical pill. Empty text disables Send; the parent
    /// owns draft state and all actions. No text is stored or transmitted by this component.
    /// `modelMenu` replaces the display-only model pill with the interactive menu; `attachments` and
    /// `onRemoveAttachment` drive the removable chips.
    public init(
        text: Binding<String>, placeholder: String = "Ask anything", model: String = "Auto",
        state: SendState = .idle, showAttachments: Bool = false,
        showModel: Bool = true, showSpeak: Bool = true,
        attachments: [ComposerAttachment] = [],
        modelMenu: ChatModelMenu? = nil,
        accessibilityPrefix: String = "composer", onAdd: (() -> Void)? = nil,
        onRemoveAttachment: ((ComposerAttachment) -> Void)? = nil,
        onSend: @escaping () -> Void
    ) {
        self.init(text: text.wrappedValue, placeholder: placeholder, model: model,
                  state: state, showAttachments: showAttachments, showModel: showModel, showSpeak: showSpeak,
                  attachments: attachments)
        self.textBinding = text
        self.onSend = onSend
        self.onAdd = onAdd
        self.modelMenu = modelMenu
        self.onRemoveAttachment = onRemoveAttachment
        self.accessibilityPrefix = accessibilityPrefix
    }

    private var currentText: String { textBinding?.wrappedValue ?? text }
    private var effectiveState: SendState {
        guard textBinding != nil, state != .sending else { return state }
        return currentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .active
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if !attachments.isEmpty {
                attachmentsStrip
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let textBinding {
                TextField(placeholder, text: textBinding, axis: .vertical)
                    .font(DesignTokens.Typography.chatMessage)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .focused($fieldFocused)
                #if os(iOS)
                    // Tap-to-type through SwiftUI's own gesture path. Inside a host ScrollView the
                    // vertical field's text view did not take focus from a tap or a short press
                    // (playground run 37944337913), while SwiftUI controls on the same page did.
                    .simultaneousGesture(TapGesture().onEnded { fieldFocused = true })
                #endif
                    .accessibilityIdentifier("\(accessibilityPrefix).composerField")
            } else {
                Text(text.isEmpty ? placeholder : text)
                    .font(DesignTokens.Typography.chatMessage)
                    .foregroundStyle(text.isEmpty ? DesignTokens.Color.labelTertiary : DesignTokens.Color.labelPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: DesignTokens.Spacing.sm) {
                if let onAdd {
                    Button(action: onAdd) { addGlyph.frame(minWidth: 44, minHeight: 44) }
                        .buttonStyle(.plain).accessibilityLabel("Add")
                        .accessibilityIdentifier("\(accessibilityPrefix).composerAdd")
                } else { addGlyph }
                if showModel { modelSelector.disabled(effectiveState == .sending) }
                Spacer(minLength: DesignTokens.Spacing.sm)
                if showSpeak && effectiveState != .sending { speakPill }
                if let onSend {
                    Button(action: onSend) { sendButton.frame(minWidth: 44, minHeight: 44) }
                        .buttonStyle(.plain)
                        .disabled(effectiveState == .idle)
                        .accessibilityLabel(effectiveState == .sending ? "Stop" : "Send")
                        .accessibilityIdentifier("\(accessibilityPrefix).composerSend")
                } else { sendButton }
            }
        }
        .padding(DesignTokens.Spacing.md)
        .background(DesignTokens.Color.backgroundSecondary, in: RoundedRectangle(cornerRadius: 30, style: .continuous))
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
            ChatModelTriggerPill(label: model)
        }
    }

    private var speakPill: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Image(systemName: "waveform")
                .font(.system(size: 13, weight: .semibold))
            Text("Speak")
                .font(DesignTokens.Typography.subheadline.weight(.semibold))
        }
        .foregroundStyle(DesignTokens.Color.labelOnColor)
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, 7)
        .background(DesignTokens.Color.brandBlue, in: Capsule())
    }

    private var sendButton: some View {
        ZStack {
            Circle().fill(sendFill)
            Image(systemName: effectiveState == .sending ? "stop.fill" : "arrow.up")
                .font(.system(size: effectiveState == .sending ? 12 : 15, weight: .bold))
                .foregroundStyle(sendForeground)
        }
        .frame(width: 32, height: 32)
    }

    private var sendFill: Color {
        switch effectiveState {
        case .idle: return DesignTokens.Color.fillTertiary
        case .active: return DesignTokens.Color.brandBlue
        case .sending: return DesignTokens.Color.systemRed
        }
    }

    private var sendForeground: Color {
        effectiveState == .idle ? DesignTokens.Color.labelSecondary : DesignTokens.Color.labelOnColor
    }

    private var attachmentsStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(attachments) { attachment in
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
            if let onRemoveAttachment {
                Button { onRemoveAttachment(attachment) } label: {
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
