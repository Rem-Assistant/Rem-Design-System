import SwiftUI

/// One reaction a person can attach to a chat message: the emoji glyph plus its spoken name.
///
/// Reactions are **data**, not design: the approved long-press picker shows `standardChoices`, but a
/// host may supply its own list. This type carries no persistence — where a chosen reaction is stored
/// (if anywhere) is the host's decision. See `docs/contracts/chat.md`.
public struct MessageReaction: Hashable, Identifiable, Sendable {
    public let emoji: String
    public let name: String
    public var id: String { emoji }

    public init(emoji: String, name: String) {
        self.emoji = emoji
        self.name = name
    }

    public static let thumbsUp = MessageReaction(emoji: "👍", name: "Thumbs up")
    public static let thumbsDown = MessageReaction(emoji: "👎", name: "Thumbs down")
    public static let heart = MessageReaction(emoji: "❤️", name: "Heart")
    public static let laugh = MessageReaction(emoji: "😂", name: "Laugh")
    public static let party = MessageReaction(emoji: "🎉", name: "Party")
    public static let surprised = MessageReaction(emoji: "😮", name: "Surprised")

    /// The approved six-choice set — the first row of the Figma long-press sheet (`2603:19498`).
    public static let standardChoices: [MessageReaction] = [thumbsUp, thumbsDown, heart, laugh, party, surprised]
}

/// **MessageReactionBadge** — the reaction shown on a message: a 28pt circle in the Rem secondary
/// pill fill (`fillTertiary`) holding a 20pt emoji.
///
/// Figma canonical: **Rem/Chat/Reaction badge** (`2654:20860`). The badge is independent of delivery
/// state; `MessageBubble` anchors it at the upper corner toward the conversation centre (outgoing
/// upper-left, incoming upper-right) with a 14pt overlap. Compose sibling: `chat/MessageReactionBadge.kt`.
public struct MessageReactionBadge: View {
    private let reaction: MessageReaction

    public init(_ reaction: MessageReaction) {
        self.reaction = reaction
    }

    public var body: some View {
        Text(reaction.emoji)
            .font(.system(size: MessageReactionMetrics.badgeEmoji))
            .frame(width: MessageReactionMetrics.badge, height: MessageReactionMetrics.badge)
            .background(DesignTokens.Color.fillTertiary, in: Circle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Reaction: \(reaction.name)")
    }
}

/// The approved long-press reaction row: 44pt secondary-pill circles with a 27pt emoji, spread
/// edge to edge. Selecting the current reaction again clears it (`onSelect(nil)`). The host owns the
/// selection and decides how it is stored; the Playground keeps it in local fixture state only.
public struct MessageReactionPicker: View {
    private let choices: [MessageReaction]
    private let selection: MessageReaction?
    private let accessibilityPrefix: String
    private let onSelect: (MessageReaction?) -> Void

    public init(
        choices: [MessageReaction] = MessageReaction.standardChoices,
        selection: MessageReaction?,
        accessibilityPrefix: String = "reactions",
        onSelect: @escaping (MessageReaction?) -> Void
    ) {
        self.choices = choices
        self.selection = selection
        self.accessibilityPrefix = accessibilityPrefix
        self.onSelect = onSelect
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(choices.enumerated()), id: \.element.id) { index, choice in
                if index > 0 { Spacer(minLength: DesignTokens.Spacing.xs) }
                Button {
                    onSelect(choice == selection ? nil : choice)
                } label: {
                    Text(choice.emoji)
                        .font(.system(size: MessageReactionMetrics.pickerEmoji))
                        .frame(width: MessageReactionMetrics.pickerTarget, height: MessageReactionMetrics.pickerTarget)
                        .background(DesignTokens.Color.fillTertiary, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(choice.name)
                .accessibilityAddTraits(choice == selection ? .isSelected : [])
                .accessibilityIdentifier("\(accessibilityPrefix).\(index)")
            }
        }
        .frame(maxWidth: .infinity)
    }
}

enum MessageReactionMetrics {
    static let badge: CGFloat = 28
    static let badgeEmoji: CGFloat = 20
    static let pickerTarget: CGFloat = 44
    static let pickerEmoji: CGFloat = 27
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.xl) {
        MessageReactionBadge(.heart)
        MessageReactionPicker(selection: .heart) { _ in }
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
