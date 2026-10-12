import SwiftUI

/// **ChatReplyContextAccessory** — the task-reply context shown **above the same `RemComposerBar`**:
/// "Replying to …" and a one-line summary of the reply target, with a dismiss ×. It is an accessory,
/// not a composer variant: it owns no send, model, attachment or voice behaviour.
///
/// Sizing is content-driven: it fills the composer dock's width and hugs its text vertically, with a
/// real 44pt dismiss target. (The Figma source master's final fixed-width / 60pt hugging normalisation
/// was cancelled and is not assumed; the task-screen instance renders at the dock width, 370 × 58 at
/// 402pt.) Dismiss reports the host's reply target only — task and conversation ids stay in the host.
///
/// Figma canonical: **Rem/Chat/Reply context** (`2682:22298`; text properties `Replying to#2682:0`,
/// `Reply summary#2682:1`; task specimen `2682:22305`). Compose sibling: `chat/ChatReplyContextAccessory.kt`.
public struct ChatReplyContextAccessory: View {
    private let context: ChatReplyContext
    private let accessibilityPrefix: String
    private let onDismiss: (() -> Void)?

    public static let dismissTarget: CGFloat = 44

    public init(_ context: ChatReplyContext, accessibilityPrefix: String = "replyContext", onDismiss: (() -> Void)? = nil) {
        self.context = context
        self.accessibilityPrefix = accessibilityPrefix
        self.onDismiss = onDismiss
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            VStack(alignment: .leading, spacing: 2) {
                Text(context.title)
                    .font(DesignTokens.Typography.footnote.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Text(context.summary)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("\(accessibilityPrefix).label")
            if let onDismiss {
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                        .frame(width: Self.dismissTarget, height: Self.dismissTarget)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Dismiss reply context")
                .accessibilityIdentifier("\(accessibilityPrefix).dismiss")
            }
        }
        .padding(.leading, DesignTokens.Spacing.md)
        .padding(.trailing, onDismiss == nil ? DesignTokens.Spacing.md : 0)
        .padding(.vertical, onDismiss == nil ? DesignTokens.Spacing.sm : 0)
        .frame(maxWidth: .infinity, minHeight: Self.dismissTarget)
        .background(
            DesignTokens.Color.backgroundSecondary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
        )
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.sm) {
        ChatReplyContextAccessory(
            ChatReplyContext(targetID: "target-1", title: "Replying to Rem", summary: "Plan the next step"),
            onDismiss: {}
        )
        RemComposerBar(placeholder: "Write your reply…")
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
