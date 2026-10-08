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
    private let showAttachments: Bool
    private let showModel: Bool
    private let showSpeak: Bool
    private var textBinding: Binding<String>?
    private var onSend: (() -> Void)?
    private var onAdd: (() -> Void)?
    private var accessibilityPrefix = "composer"

    public init(
        text: String = "",
        placeholder: String = "Ask anything",
        model: String = "Auto",
        state: SendState = .idle,
        showAttachments: Bool = false,
        showModel: Bool = true,
        showSpeak: Bool = true
    ) {
        self.text = text
        self.placeholder = placeholder
        self.model = model
        self.state = state
        self.showAttachments = showAttachments
        self.showModel = showModel
        self.showSpeak = showSpeak
    }

    /// Interactive composition of the same canonical pill. Empty text disables Send; the parent
    /// owns draft state and all actions. No text is stored or transmitted by this component.
    public init(
        text: Binding<String>, placeholder: String = "Ask anything", model: String = "Auto",
        state: SendState = .idle, showAttachments: Bool = false,
        showModel: Bool = true, showSpeak: Bool = true,
        accessibilityPrefix: String = "composer", onAdd: (() -> Void)? = nil,
        onSend: @escaping () -> Void
    ) {
        self.init(text: text.wrappedValue, placeholder: placeholder, model: model,
                  state: state, showAttachments: showAttachments, showModel: showModel, showSpeak: showSpeak)
        self.textBinding = text
        self.onSend = onSend
        self.onAdd = onAdd
        self.accessibilityPrefix = accessibilityPrefix
    }

    private var currentText: String { textBinding?.wrappedValue ?? text }
    private var effectiveState: SendState {
        guard textBinding != nil, state != .sending else { return state }
        return currentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .idle : .active
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if showAttachments {
                attachmentsStrip
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let textBinding {
                TextField(placeholder, text: textBinding, axis: .vertical)
                    .font(DesignTokens.Typography.chatMessage)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
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
                if showModel { modelSelector }
                Spacer(minLength: DesignTokens.Spacing.sm)
                if showSpeak { speakPill }
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

    private var modelSelector: some View {
        HStack(spacing: 3) {
            Text(model)
                .font(DesignTokens.Typography.subheadline)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
            Image(systemName: "chevron.up.chevron.down")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelTertiary)
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
        HStack(spacing: DesignTokens.Spacing.sm) {
            // Cloud browser chip
            HStack(spacing: DesignTokens.Spacing.xs) {
                Image(systemName: "globe")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.brandBlue)
                Text("Cloud browser")
                    .font(DesignTokens.Typography.caption1)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Image(systemName: "xmark")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(DesignTokens.Color.labelTertiary)
            }
            .padding(.horizontal, DesignTokens.Spacing.sm)
            .padding(.vertical, 6)
            .background(DesignTokens.Color.backgroundPrimary, in: Capsule())

            // Image thumbnail with remove affordance
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                    .fill(DesignTokens.Color.systemBlue.opacity(0.35))
                    .frame(width: 44, height: 44)
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 15))
                    .foregroundStyle(DesignTokens.Color.labelPrimary, DesignTokens.Color.backgroundPrimary)
                    .offset(x: 5, y: -5)
            }
        }
    }
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
