import SwiftUI

/// **MessageBubble** — a single chat message in the conversation transcript, in one of two roles.
///
/// The two roles read deliberately differently, mirroring the Figma **MessageBubble** component set
/// (`50:7`, page "Chat components") and the shipped `Shared/Views/Chat/ChatMessageViews.swift`:
///
/// - `.user` (sent) — a `brandBlue` rounded bubble, trailing-aligned, with `labelOnColor` (white)
///   text: the iMessage "sent" treatment the Figma node draws.
/// - `.assistant` (received) — plain text on the surface, leading-aligned, no bubble. The assistant
///   reads as prose/markdown with minimal chrome.
///
/// Both roles use `chatMessage` (body, 17pt) and cap their width via `MessageBubbleMetrics.maxWidth`
/// so a message never spans the full transcript, leaving a gutter on the opposite edge. An optional
/// `meta` line (e.g. a timestamp) sits beneath the message in `chatMeta`.
///
/// Compose sibling: `MessageBubble` in `chat/MessageBubble.kt`.
public struct MessageBubble: View {
    /// Who sent the message. Drives alignment, fill, and text treatment.
    public enum Role: Equatable {
        case user
        case assistant
    }

    private let text: String
    private let role: Role
    private let meta: String?

    /// - Parameters:
    ///   - text: The message body. Plain text; the assistant role reads as prose.
    ///   - role: `.user` (trailing bubble) or `.assistant` (leading plain text).
    ///   - meta: Optional metadata (e.g. a timestamp) shown beneath the message in `chatMeta`,
    ///     aligned to the message's edge. Hidden when `nil`, so the default matches the Figma node.
    public init(_ text: String, role: Role, meta: String? = nil) {
        self.text = text
        self.role = role
        self.meta = meta
    }

    public var body: some View {
        HStack(spacing: 0) {
            if role == .user {
                Spacer(minLength: DesignTokens.Spacing.xl)
            }

            VStack(alignment: edge, spacing: DesignTokens.Spacing.xs) {
                messageContent
                if let meta {
                    Text(meta)
                        .font(DesignTokens.Typography.chatMeta)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
            }
            .frame(maxWidth: MessageBubbleMetrics.maxWidth, alignment: frameAlignment)

            if role == .assistant {
                Spacer(minLength: DesignTokens.Spacing.xl)
            }
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var messageContent: some View {
        switch role {
        case .user:
            Text(text)
                .font(DesignTokens.Typography.chatMessage)
                .foregroundStyle(DesignTokens.Color.labelOnColor)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
                .background(
                    DesignTokens.Color.brandBlue,
                    in: RoundedRectangle(
                        cornerRadius: DesignTokens.CornerRadius.xlarge,
                        style: .continuous
                    )
                )
        case .assistant:
            Text(text)
                .font(DesignTokens.Typography.chatMessage)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .padding(.vertical, DesignTokens.Spacing.xs)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var edge: HorizontalAlignment {
        role == .user ? .trailing : .leading
    }

    private var frameAlignment: Alignment {
        role == .user ? .trailing : .leading
    }
}

private enum MessageBubbleMetrics {
    /// Caps a message so it never spans the full transcript width. A fixed cap (rather than a live
    /// percentage of the container) keeps bubbles readable on iPhone, iPad, and the wide Mac window,
    /// where a percentage would over-stretch. ~78% of a standard iPhone content width; matches the
    /// Figma bubble's max width band (~272pt user / ~330pt assistant).
    static let maxWidth: CGFloat = 300
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.lg) {
        MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
        MessageBubble(
            "Done — I archived 38 newsletters and snoozed 5 low-priority threads on this Mac.\n\nWant me to draft quick replies to the two that still need you?",
            role: .assistant
        )
        MessageBubble("Yes, go ahead.", role: .user, meta: "9:41 AM")
    }
    .padding(DesignTokens.Spacing.lg)
    .frame(maxWidth: .infinity)
    .background(DesignTokens.Color.backgroundPrimary)
}
